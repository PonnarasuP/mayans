package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val role: String = "MEMBER", // "ADMIN" or "MEMBER"
    val joinDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
) {
    val isAdmin: Boolean get() = role.equals("ADMIN", ignoreCase = true)
}
