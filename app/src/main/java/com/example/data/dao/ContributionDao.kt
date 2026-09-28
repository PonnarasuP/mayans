package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Contribution
import com.example.data.model.MemberWithContribution
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {

    @Query("""
        SELECT 
            m.id AS id,
            m.name AS name,
            m.phone AS phone,
            m.email AS email,
            m.role AS role,
            m.joinDate AS joinDate,
            m.isActive AS isActive,
            m.notes AS notes,
            c.id AS contributionId,
            c.monthYear AS monthYear,
            c.amount AS amount,
            c.status AS status,
            c.paymentMethod AS paymentMethod,
            c.transactionRef AS transactionRef,
            c.paidDate AS paidDate,
            c.verifiedByAdmin AS verifiedByAdmin,
            c.remarks AS remarks
        FROM members m
        LEFT JOIN contributions c ON m.id = c.memberId AND c.monthYear = :monthYear
        WHERE m.isActive = 1
        ORDER BY m.name ASC
    """)
    fun getMembersWithContributionForMonth(monthYear: String): Flow<List<MemberWithContribution>>

    @Query("""
        SELECT 
            m.id AS id,
            m.name AS name,
            m.phone AS phone,
            m.email AS email,
            m.role AS role,
            m.joinDate AS joinDate,
            m.isActive AS isActive,
            m.notes AS notes,
            c.id AS contributionId,
            c.monthYear AS monthYear,
            c.amount AS amount,
            c.status AS status,
            c.paymentMethod AS paymentMethod,
            c.transactionRef AS transactionRef,
            c.paidDate AS paidDate,
            c.verifiedByAdmin AS verifiedByAdmin,
            c.remarks AS remarks
        FROM members m
        INNER JOIN contributions c ON m.id = c.memberId
        WHERE c.status = 'CASH_PENDING_VERIFICATION'
        ORDER BY c.paidDate DESC
    """)
    fun getCashPendingVerifications(): Flow<List<MemberWithContribution>>

    @Query("SELECT * FROM contributions WHERE memberId = :memberId AND monthYear = :monthYear LIMIT 1")
    suspend fun getContributionForMemberMonth(memberId: Long, monthYear: String): Contribution?

    @Query("SELECT * FROM contributions WHERE id = :id LIMIT 1")
    suspend fun getContributionById(id: Long): Contribution?

    @Query("SELECT * FROM contributions WHERE memberId = :memberId ORDER BY monthYear DESC")
    fun getContributionsForMember(memberId: Long): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions WHERE monthYear = :monthYear")
    fun getContributionsForMonth(monthYear: String): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions ORDER BY paidDate DESC, id DESC")
    fun getAllContributions(): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions ORDER BY paidDate DESC, id DESC")
    suspend fun getAllContributionsSync(): List<Contribution>

    @Query("SELECT DISTINCT monthYear FROM contributions ORDER BY monthYear DESC")
    fun getAllTrackedMonths(): Flow<List<String>>

    @Query("SELECT SUM(amount) FROM contributions WHERE status = 'PAID'")
    fun getTotalCollectedOverall(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM contributions WHERE monthYear = :monthYear AND status = 'PAID'")
    fun getTotalCollectedForMonth(monthYear: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: Contribution): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<Contribution>)

    @Update
    suspend fun updateContribution(contribution: Contribution)

    @Query("UPDATE contributions SET status = :status, verifiedByAdmin = :verified, verifiedDate = :verifiedDate WHERE id = :id")
    suspend fun updateVerificationStatus(id: Long, status: String, verified: Boolean, verifiedDate: Long?)

    @Query("DELETE FROM contributions WHERE id = :id")
    suspend fun deleteContributionById(id: Long)
}
