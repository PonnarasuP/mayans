package com.example.data.model

import androidx.room.Embedded

data class MemberWithContribution(
    @Embedded val member: Member,
    val contributionId: Long? = null,
    val monthYear: String? = null,
    val amount: Double? = null,
    val status: String? = null, // null means not yet created/pending
    val paymentMethod: String? = null,
    val transactionRef: String? = null,
    val paidDate: Long? = null,
    val verifiedByAdmin: Boolean = false,
    val remarks: String? = null
) {
    val displayStatus: String
        get() = status ?: Contribution.STATUS_PENDING

    val isPaid: Boolean
        get() = displayStatus == Contribution.STATUS_PAID

    val isCashPending: Boolean
        get() = displayStatus == Contribution.STATUS_CASH_PENDING

    val isOverdue: Boolean
        get() = displayStatus == Contribution.STATUS_OVERDUE
}
