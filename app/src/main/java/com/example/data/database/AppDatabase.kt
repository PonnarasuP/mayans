package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppConfigDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.ContributionDao
import com.example.data.dao.MemberDao
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Member::class,
        Contribution::class,
        AppConfig::class,
        AuditLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memberDao(): MemberDao
    abstract fun contributionDao(): ContributionDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mayan_welfare_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val memberDao = database.memberDao()
            val contributionDao = database.contributionDao()
            val configDao = database.appConfigDao()
            val auditLogDao = database.auditLogDao()

            // 1. Initial Config (Clean defaults without hardcoded names or amounts)
            configDao.insertOrUpdate(
                AppConfig(
                    id = 1,
                    upiId = "",
                    upiName = "Welfare Fund",
                    monthlyAmount = 0.0,
                    adminPin = "170588",
                    fundTitle = "Welfare Fund",
                    contactPhone = "",
                    reminderDayOfMonth = 1,
                    autoNotifyMissed = true,
                    adminName = "Admin",
                    adminEmail = ""
                )
            )

            // 2. Initial Audit Log
            auditLogDao.insertLog(
                AuditLog(
                    action = "SYSTEM_INITIALIZATION",
                    description = "Welfare Fund tracking initialized.",
                    performedByRole = "SYSTEM"
                )
            )
        }
    }
}
