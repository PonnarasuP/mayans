package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    val id: Int = 1,
    val upiId: String = "mayanwelfare@okhdfcbank",
    val upiName: String = "MAYAN's Well Fare",
    val monthlyAmount: Double = 500.0,
    val adminPin: String = "1234",
    val fundTitle: String = "MAYAN's Well Fare",
    val contactPhone: String = "+91 98765 43210",
    val reminderDayOfMonth: Int = 1,
    val autoNotifyMissed: Boolean = true,
    val adminName: String = "Arthi",
    val adminEmail: String = "arthi.eaglenewz@gmail.com"
)
