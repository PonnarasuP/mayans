package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID = "mayan_welfare_reminders"
    const val CHANNEL_NAME = "MAYAN's Welfare Reminders"
    const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Monthly contribution reminders and payment verification notifications"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private const val PREFS_NAME = "welfare_prefs"
    private const val KEY_DEVICE_MEMBER_ID = "device_member_id"
    private const val KEY_DEVICE_MEMBER_NAME = "device_member_name"

    fun setDeviceMember(context: Context, memberId: Long, memberName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_DEVICE_MEMBER_ID, memberId)
            .putString(KEY_DEVICE_MEMBER_NAME, memberName)
            .apply()
    }

    fun getDeviceMemberId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_DEVICE_MEMBER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun getDeviceMemberName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_MEMBER_NAME, null)
    }

    /**
     * Ensures that members only receive their OWN notification and never see others' notifications on their phone.
     */
    fun shouldShowNotificationForMember(
        context: Context,
        targetMemberId: Long?,
        targetMemberName: String?
    ): Boolean {
        val deviceId = getDeviceMemberId(context)
        val deviceName = getDeviceMemberName(context)

        // If device has a configured member identity, strictly match
        if (deviceId != null && targetMemberId != null) {
            return deviceId == targetMemberId
        }
        if (!deviceName.isNullOrBlank() && !targetMemberName.isNullOrBlank()) {
            return deviceName.equals(targetMemberName, ignoreCase = true)
        }
        // If device is not assigned to a specific member, do not leak others' notifications
        return targetMemberId == null
    }

    fun sendContributionReminderNotification(
        context: Context,
        memberName: String,
        monthYear: String,
        amount: Double,
        targetMemberId: Long? = null,
        notificationId: Int = NOTIFICATION_ID_BASE + 1,
        forceShow: Boolean = false
    ): Boolean {
        // Enforce strict privacy rule: member shall only receive their own notification
        if (!forceShow && targetMemberId != null) {
            if (!shouldShowNotificationForMember(context, targetMemberId, memberName)) {
                return false
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MAYAN's Well Fare • Due Reminder")
            .setContentText("Dear $memberName, ₹${amount.toInt()} monthly contribution for $monthYear is due. Pay now via UPI.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Dear $memberName,\nYour monthly contribution of ₹${amount.toInt()} for MAYAN's Well Fare ($monthYear) is pending. Please contribute to keep our welfare goals on track. Thank you!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendCashVerificationNotification(
        context: Context,
        memberName: String,
        amount: Double,
        isApproved: Boolean,
        notificationId: Int = NOTIFICATION_ID_BASE + 200
    ) {
        createNotificationChannel(context)
        val title = if (isApproved) "Payment Verified!" else "Cash Payment Update"
        val message = if (isApproved) {
            "Admin has verified cash payment of ₹${amount.toInt()} from $memberName."
        } else {
            "Cash payment claim of ₹${amount.toInt()} from $memberName was rejected or marked unpaid."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
