package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    suspend fun getAllLogsSync(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AuditLog>)
}
