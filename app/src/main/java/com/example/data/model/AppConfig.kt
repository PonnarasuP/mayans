package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    val id: Int = 1,
    val upiId: String = "",
    val upiName: String = "Welfare Fund",
    val monthlyAmount: Double = 0.0,
    val adminPin: String = "170588",
    val fundTitle: String = "Welfare Fund",
    val contactPhone: String = "",
    val reminderDayOfMonth: Int = 1,
    val autoNotifyMissed: Boolean = true,
    val adminName: String = "Admin",
    val adminEmail: String = ""
)

