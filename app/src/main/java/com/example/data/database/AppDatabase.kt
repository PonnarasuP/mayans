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

            // 1. Initial Config
            configDao.insertOrUpdate(
                AppConfig(
                    id = 1,
                    upiId = "mayanwelfare@okhdfcbank",
                    upiName = "MAYAN's Well Fare",
                    monthlyAmount = 500.0,
                    adminPin = "1234",
                    fundTitle = "MAYAN's Well Fare",
                    contactPhone = "+91 98401 23456",
                    reminderDayOfMonth = 1,
                    autoNotifyMissed = true,
                    adminName = "Arthi",
                    adminEmail = "arthi.eaglenewz@gmail.com"
                )
            )

            // 2. Members
            val members = listOf(
                Member(1, "Arthi (Admin)", "9840123456", "arthi.eaglenewz@gmail.com", "ADMIN"),
                Member(2, "Sundaram K.", "9840234567", "sundaram@example.com", "MEMBER"),
                Member(3, "Karthik Raja", "9840345678", "karthik@example.com", "MEMBER"),
                Member(4, "Priya Natarajan", "9840456789", "priya@example.com", "MEMBER"),
                Member(5, "Saravanan V.", "9840567890", "saravanan@example.com", "MEMBER"),
                Member(6, "Divya Bharathi", "9840678901", "divya@example.com", "MEMBER"),
                Member(7, "Vignesh Kumar", "9840789012", "vignesh@example.com", "MEMBER"),
                Member(8, "Anand Chandran", "9840890123", "anand@example.com", "MEMBER"),
                Member(9, "Meenakshi S.", "9840901234", "meenakshi@example.com", "MEMBER"),
                Member(10, "Rajesh Kannan", "9840012345", "rajesh@example.com", "MEMBER")
            )
            memberDao.insertMembers(members)

            val currentMonth = "2026-09"
            val prevMonth = "2026-08"

            // 3. Current Month Contributions
            val currentContributions = listOf(
                Contribution(
                    memberId = 1,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PAID,
                    paymentMethod = Contribution.METHOD_UPI,
                    transactionRef = "UPI/260901001",
                    paidDate = System.currentTimeMillis() - 86400000L * 25,
                    verifiedByAdmin = true,
                    verifiedDate = System.currentTimeMillis() - 86400000L * 25,
                    remarks = "Self contribution"
                ),
                Contribution(
                    memberId = 2,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PAID,
                    paymentMethod = Contribution.METHOD_UPI,
                    transactionRef = "UPI/260902441",
                    paidDate = System.currentTimeMillis() - 86400000L * 24,
                    verifiedByAdmin = true,
                    verifiedDate = System.currentTimeMillis() - 86400000L * 24,
                    remarks = "GPay verified"
                ),
                Contribution(
                    memberId = 3,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PAID,
                    paymentMethod = Contribution.METHOD_UPI,
                    transactionRef = "UPI/260904812",
                    paidDate = System.currentTimeMillis() - 86400000L * 22,
                    verifiedByAdmin = true,
                    verifiedDate = System.currentTimeMillis() - 86400000L * 22,
                    remarks = "PhonePe verified"
                ),
                Contribution(
                    memberId = 4,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_CASH_PENDING,
                    paymentMethod = Contribution.METHOD_CASH,
                    transactionRef = "Handed to Mayan at community hall",
                    paidDate = System.currentTimeMillis() - 86400000L * 2,
                    verifiedByAdmin = false,
                    remarks = "Member submitted cash payment notice"
                ),
                Contribution(
                    memberId = 5,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PAID,
                    paymentMethod = Contribution.METHOD_UPI,
                    transactionRef = "UPI/260909931",
                    paidDate = System.currentTimeMillis() - 86400000L * 15,
                    verifiedByAdmin = true,
                    verifiedDate = System.currentTimeMillis() - 86400000L * 15,
                    remarks = "Paytm verified"
                ),
                Contribution(
                    memberId = 6,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_CASH_PENDING,
                    paymentMethod = Contribution.METHOD_CASH,
                    transactionRef = "Given in envelope",
                    paidDate = System.currentTimeMillis() - 86400000L * 1,
                    verifiedByAdmin = false,
                    remarks = "Cash awaiting admin verification"
                ),
                Contribution(
                    memberId = 7,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_OVERDUE,
                    paymentMethod = Contribution.METHOD_NONE,
                    transactionRef = "",
                    paidDate = null,
                    verifiedByAdmin = false,
                    remarks = "Missed beginning of month"
                ),
                Contribution(
                    memberId = 8,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_OVERDUE,
                    paymentMethod = Contribution.METHOD_NONE,
                    transactionRef = "",
                    paidDate = null,
                    verifiedByAdmin = false,
                    remarks = "Reminder sent"
                ),
                Contribution(
                    memberId = 9,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PENDING,
                    paymentMethod = Contribution.METHOD_NONE,
                    transactionRef = "",
                    paidDate = null,
                    verifiedByAdmin = false
                ),
                Contribution(
                    memberId = 10,
                    monthYear = currentMonth,
                    amount = 500.0,
                    status = Contribution.STATUS_PENDING,
                    paymentMethod = Contribution.METHOD_NONE,
                    transactionRef = "",
                    paidDate = null,
                    verifiedByAdmin = false
                )
            )
            contributionDao.insertContributions(currentContributions)

            // 4. Previous Month Contributions (August)
            val prevContributions = members.mapIndexed { index, member ->
                Contribution(
                    memberId = member.id,
                    monthYear = prevMonth,
                    amount = 500.0,
                    status = if (index < 9) Contribution.STATUS_PAID else Contribution.STATUS_OVERDUE,
                    paymentMethod = if (index % 3 == 0) Contribution.METHOD_CASH else Contribution.METHOD_UPI,
                    transactionRef = if (index % 3 == 0) "Cash receipt #$index" else "UPI/2608100$index",
                    paidDate = if (index < 9) System.currentTimeMillis() - 86400000L * (45 - index) else null,
                    verifiedByAdmin = index < 9,
                    verifiedDate = if (index < 9) System.currentTimeMillis() - 86400000L * (45 - index) else null,
                    remarks = if (index < 9) "Cleared" else "Unpaid"
                )
            }
            contributionDao.insertContributions(prevContributions)

            // 5. Initial Audit Log
            auditLogDao.insertLog(
                AuditLog(
                    action = "SYSTEM_INITIALIZATION",
                    description = "MAYAN's Well Fare contribution fund tracking initialized for 10 members (₹500/month).",
                    performedByRole = "SYSTEM"
                )
            )
        }
    }
}
