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

    fun sendContributionReminderNotification(
        context: Context,
        memberName: String,
        monthYear: String,
        amount: Double,
        notificationId: Int = NOTIFICATION_ID_BASE + 1
    ) {
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
        } catch (_: SecurityException) {
            // Handled gracefully if permission denied
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
