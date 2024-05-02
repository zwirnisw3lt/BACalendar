package com.example.ba_calander

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import android.util.Log
import android.Manifest

fun checkScheduleExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Request the permission
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}

fun scheduleNotification(context: Context, event: Event) {
    Log.d("Notification", "Scheduling notification for event ${event.title}")

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("events", "Event Notifications", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    checkScheduleExactAlarmPermission(context)

    val notification = NotificationCompat.Builder(context, "events")
        .setContentTitle("Upcoming Event")
        .setContentText("The event ${event.title} is starting in 45 minutes.")
        .setSmallIcon(R.drawable.ic_notification)
        .build()

    val eventTime = Instant.ofEpochSecond(event.start.toLong())
    val now = Instant.now()

    val triggerTime = if (eventTime.isBefore(now)) {
        now
    } else {
        eventTime.minus(45, ChronoUnit.MINUTES)
    }

    Log.d("Notification", "Trigger time: $triggerTime")

    val intent = Intent(context, NotificationPublisher::class.java).apply {
        putExtra(NotificationPublisher.NOTIFICATION_ID, event.title.hashCode())
        putExtra(NotificationPublisher.NOTIFICATION, notification)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        abs(event.title.hashCode()),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    )

    val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerTime.toEpochMilli(), pendingIntent)

    Log.d("Notification", "Notification scheduled")
}