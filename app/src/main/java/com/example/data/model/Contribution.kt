package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contributions",
    indices = [
        Index(value = ["memberId", "monthYear"], unique = true),
        Index(value = ["monthYear"]),
        Index(value = ["status"])
    ]
)
data class Contribution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val monthYear: String, // e.g. "2026-09"
    val amount: Double = 500.0,
    val status: String = STATUS_PENDING, // PAID, PENDING, CASH_PENDING_VERIFICATION, OVERDUE
    val paymentMethod: String = METHOD_NONE, // UPI, CASH, NONE
    val transactionRef: String = "", // UPI UTR or receipt reference
    val paidDate: Long? = null,
    val verifiedByAdmin: Boolean = false,
    val verifiedDate: Long? = null,
    val remarks: String = ""
) {
    companion object {
        const val STATUS_PAID = "PAID"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CASH_PENDING = "CASH_PENDING_VERIFICATION"
        const val STATUS_OVERDUE = "OVERDUE"

        const val METHOD_UPI = "UPI"
        const val METHOD_CASH = "CASH"
        const val METHOD_NONE = "NONE"
    }
}
