package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // e.g. "CASH_VERIFIED", "PAYMENT_RECORDED", "MEMBER_ADDED"
    val description: String,
    val performedByRole: String = "ADMIN"
)
