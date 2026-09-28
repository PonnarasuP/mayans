package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.Member
import com.example.data.model.MemberWithContribution
import com.example.data.remote.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class WelfareRepository(
    private val db: AppDatabase,
    val syncManager: FirestoreSyncManager? = null
) {

    private val memberDao = db.memberDao()
    private val contributionDao = db.contributionDao()
    private val configDao = db.appConfigDao()
    private val auditLogDao = db.auditLogDao()

    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val activeMembers: Flow<List<Member>> = memberDao.getActiveMembers()
    val activeMemberCount: Flow<Int> = memberDao.getActiveMemberCount()
    val config: Flow<AppConfig?> = configDao.getConfig()
    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getRecentLogs()
    val cashPendingVerifications: Flow<List<MemberWithContribution>> =
        contributionDao.getCashPendingVerifications()
    val allContributions: Flow<List<Contribution>> = contributionDao.getAllContributions()
    val allTrackedMonths: Flow<List<String>> = contributionDao.getAllTrackedMonths()
    val totalCollectedOverall: Flow<Double?> = contributionDao.getTotalCollectedOverall()

    fun getMembersWithContributionForMonth(monthYear: String): Flow<List<MemberWithContribution>> {
        return contributionDao.getMembersWithContributionForMonth(monthYear)
    }

    fun getContributionsForMember(memberId: Long): Flow<List<Contribution>> {
        return contributionDao.getContributionsForMember(memberId)
    }

    suspend fun getMemberById(id: Long): Member? {
        return memberDao.getMemberByIdSync(id)
    }

    suspend fun getConfigSync(): AppConfig {
        return configDao.getConfigSync() ?: AppConfig()
    }

    suspend fun addMember(member: Member): Long {
        val id = memberDao.insertMember(member)
        val savedMember = member.copy(id = id)
        val log = AuditLog(
            action = "MEMBER_ADDED",
            description = "Added member: ${member.name} (${member.phone}), Role: ${member.role}",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushMember(savedMember)
        syncManager?.pushAuditLog(log.copy(id = logId))
        return id
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
        val log = AuditLog(
            action = "MEMBER_UPDATED",
            description = "Updated member details for: ${member.name}",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushMember(member)
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
        val log = AuditLog(
            action = "MEMBER_DELETED",
            description = "Removed member: ${member.name}",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.deleteMemberFromCloud(member.id)
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun recordUpiPayment(
        memberId: Long,
        monthYear: String,
        amount: Double,
        utrNumber: String,
        isAutoApproved: Boolean = true
    ) {
        val existing = contributionDao.getContributionForMemberMonth(memberId, monthYear)
        val member = memberDao.getMemberByIdSync(memberId)
        val memberName = member?.name ?: "Member #$memberId"

        val contribution = existing?.copy(
            amount = amount,
            status = Contribution.STATUS_PAID,
            paymentMethod = Contribution.METHOD_UPI,
            transactionRef = utrNumber.ifBlank { "UPI/AUTOPAY" },
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = isAutoApproved,
            verifiedDate = if (isAutoApproved) System.currentTimeMillis() else null,
            remarks = "UPI payment for $monthYear"
        ) ?: Contribution(
            memberId = memberId,
            monthYear = monthYear,
            amount = amount,
            status = Contribution.STATUS_PAID,
            paymentMethod = Contribution.METHOD_UPI,
            transactionRef = utrNumber.ifBlank { "UPI/AUTOPAY" },
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = isAutoApproved,
            verifiedDate = if (isAutoApproved) System.currentTimeMillis() else null,
            remarks = "UPI payment for $monthYear"
        )

        val contribId = contributionDao.insertContribution(contribution)
        val log = AuditLog(
            action = "UPI_PAYMENT",
            description = "₹$amount paid via UPI by $memberName for $monthYear (Ref: $utrNumber)",
            performedByRole = "MEMBER"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushContribution(contribution.copy(id = contribId))
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun submitCashPayment(
        memberId: Long,
        monthYear: String,
        amount: Double,
        note: String
    ) {
        val existing = contributionDao.getContributionForMemberMonth(memberId, monthYear)
        val member = memberDao.getMemberByIdSync(memberId)
        val memberName = member?.name ?: "Member #$memberId"

        val contribution = existing?.copy(
            amount = amount,
            status = Contribution.STATUS_CASH_PENDING,
            paymentMethod = Contribution.METHOD_CASH,
            transactionRef = note.ifBlank { "Cash handed to admin" },
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = false,
            verifiedDate = null,
            remarks = "Cash submitted, pending admin verification"
        ) ?: Contribution(
            memberId = memberId,
            monthYear = monthYear,
            amount = amount,
            status = Contribution.STATUS_CASH_PENDING,
            paymentMethod = Contribution.METHOD_CASH,
            transactionRef = note.ifBlank { "Cash handed to admin" },
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = false,
            verifiedDate = null,
            remarks = "Cash submitted, pending admin verification"
        )

        val contribId = contributionDao.insertContribution(contribution)
        val log = AuditLog(
            action = "CASH_SUBMITTED",
            description = "Cash payment of ₹$amount submitted by $memberName for $monthYear ($note). Pending verification.",
            performedByRole = "MEMBER"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushContribution(contribution.copy(id = contribId))
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun verifyCashPayment(
        contributionId: Long,
        approved: Boolean,
        adminNotes: String = ""
    ) {
        if (approved) {
            contributionDao.updateVerificationStatus(
                id = contributionId,
                status = Contribution.STATUS_PAID,
                verified = true,
                verifiedDate = System.currentTimeMillis()
            )
            val log = AuditLog(
                action = "CASH_VERIFIED",
                description = "Admin verified & approved cash payment ID #$contributionId. $adminNotes",
                performedByRole = "ADMIN"
            )
            val logId = auditLogDao.insertLog(log)
            val updated = contributionDao.getContributionById(contributionId)
            if (updated != null) {
                syncManager?.pushContribution(updated)
            }
            syncManager?.pushAuditLog(log.copy(id = logId))
        } else {
            contributionDao.updateVerificationStatus(
                id = contributionId,
                status = Contribution.STATUS_PENDING,
                verified = false,
                verifiedDate = null
            )
            val log = AuditLog(
                action = "CASH_REJECTED",
                description = "Admin rejected cash payment claim ID #$contributionId. $adminNotes",
                performedByRole = "ADMIN"
            )
            val logId = auditLogDao.insertLog(log)
            val updated = contributionDao.getContributionById(contributionId)
            if (updated != null) {
                syncManager?.pushContribution(updated)
            }
            syncManager?.pushAuditLog(log.copy(id = logId))
        }
    }

    suspend fun adminRecordPaymentDirect(
        memberId: Long,
        monthYear: String,
        amount: Double,
        method: String,
        ref: String,
        remarks: String
    ) {
        val existing = contributionDao.getContributionForMemberMonth(memberId, monthYear)
        val member = memberDao.getMemberByIdSync(memberId)
        val memberName = member?.name ?: "Member #$memberId"

        val contribution = existing?.copy(
            amount = amount,
            status = Contribution.STATUS_PAID,
            paymentMethod = method,
            transactionRef = ref,
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = true,
            verifiedDate = System.currentTimeMillis(),
            remarks = remarks
        ) ?: Contribution(
            memberId = memberId,
            monthYear = monthYear,
            amount = amount,
            status = Contribution.STATUS_PAID,
            paymentMethod = method,
            transactionRef = ref,
            paidDate = System.currentTimeMillis(),
            verifiedByAdmin = true,
            verifiedDate = System.currentTimeMillis(),
            remarks = remarks
        )

        val contribId = contributionDao.insertContribution(contribution)
        val log = AuditLog(
            action = "ADMIN_RECORD_PAYMENT",
            description = "Admin manually recorded ₹$amount ($method) for $memberName for $monthYear. Verified.",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushContribution(contribution.copy(id = contribId))
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun updateConfig(config: AppConfig, isAdmin: Boolean = true) {
        configDao.insertOrUpdate(config)
        val log = AuditLog(
            action = "CONFIG_UPDATED",
            description = "Welfare fund settings updated. UPI ID: ${config.upiId}, Monthly: ₹${config.monthlyAmount}",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushConfig(config, isAdmin)
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun markMissedContributionsOverdue(monthYear: String) {
        val membersWithStatus = contributionDao.getMembersWithContributionForMonth(monthYear).firstOrNull() ?: emptyList()
        var count = 0
        for (item in membersWithStatus) {
            val status = item.displayStatus
            if (status == Contribution.STATUS_PENDING) {
                val contrib = if (item.contributionId != null) {
                    contributionDao.getContributionForMemberMonth(item.member.id, monthYear)?.copy(
                        status = Contribution.STATUS_OVERDUE
                    )
                } else {
                    Contribution(
                        memberId = item.member.id,
                        monthYear = monthYear,
                        amount = 500.0,
                        status = Contribution.STATUS_OVERDUE,
                        paymentMethod = Contribution.METHOD_NONE,
                        remarks = "Missed contribution beginning of month"
                    )
                }
                if (contrib != null) {
                    val savedId = contributionDao.insertContribution(contrib)
                    syncManager?.pushContribution(contrib.copy(id = savedId))
                    count++
                }
            }
        }
        if (count > 0) {
            val log = AuditLog(
                action = "OVERDUE_FLAGGED",
                description = "Flagged $count pending members as OVERDUE for $monthYear.",
                performedByRole = "SYSTEM"
            )
            val logId = auditLogDao.insertLog(log)
            syncManager?.pushAuditLog(log.copy(id = logId))
        }
    }

    suspend fun logNotificationBroadcast(count: Int, monthYear: String) {
        val log = AuditLog(
            action = "NOTIFICATIONS_BROADCAST",
            description = "Admin dispatched missed dues push reminders to $count members for $monthYear.",
            performedByRole = "ADMIN"
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushAuditLog(log.copy(id = logId))
    }

    suspend fun logCustomActivity(action: String, description: String, role: String = "ADMIN") {
        val log = AuditLog(
            action = action,
            description = description,
            performedByRole = role
        )
        val logId = auditLogDao.insertLog(log)
        syncManager?.pushAuditLog(log.copy(id = logId))
    }
}
