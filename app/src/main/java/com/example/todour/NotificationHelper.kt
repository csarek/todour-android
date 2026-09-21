package com.example.todour

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID = "todour_quick_capture"
    const val NOTIFICATION_ID = 1001
    const val KEY_QUICK_NOTE_INPUT = "key_quick_note_input"
    const val ACTION_QUICK_CAPTURE = "com.example.todour.ACTION_QUICK_CAPTURE"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gyors jegyzet",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Állandó gyorsrögzítő mező az INBOX-hoz"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun showQuickCaptureNotification(context: Context) {
        val remoteInput = RemoteInput.Builder(KEY_QUICK_NOTE_INPUT)
            .setLabel("Írj ide egy gyors jegyzetet...")
            .build()

        val intent = Intent(context, QuickCaptureReceiver::class.java).apply {
            action = ACTION_QUICK_CAPTURE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "Mentés az INBOX-ba",
            pendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Todour gyorsrögzítő")
            .setContentText("Koppints ide, és írj be egy gondolatot")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(replyAction)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
