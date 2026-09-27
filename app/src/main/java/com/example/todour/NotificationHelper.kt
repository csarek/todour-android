package com.example.todour

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

object NotificationHelper {

    const val CHANNEL_ID = "todour_quick_capture"
    const val NOTIFICATION_ID = 1001
    const val KEY_INBOX_INPUT = "key_inbox_input"
    const val KEY_JOURNAL_INPUT = "key_journal_input"
    const val EXTRA_TARGET = "extra_target"
    const val TARGET_INBOX = "inbox"
    const val TARGET_JOURNAL = "journal"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gyors jegyzet",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Gyorsrögzítő"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun showQuickCaptureNotification(context: Context) {
        fun buildAction(target: String, remoteKey: String, label: String): NotificationCompat.Action {
            val remoteInput = RemoteInput.Builder(remoteKey).setLabel(label).build()
            val intent = Intent(context, QuickCaptureReceiver::class.java).apply {
                putExtra(EXTRA_TARGET, target)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                target.hashCode(),
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationCompat.Action.Builder(0, label, pendingIntent)
                .addRemoteInput(remoteInput)
                .build()
        }

        val inboxAction = buildAction(TARGET_INBOX, KEY_INBOX_INPUT, "Inbox")
        val journalAction = buildAction(TARGET_JOURNAL, KEY_JOURNAL_INPUT, "Napló")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Todour")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setSortKey("0")
            .addAction(inboxAction)
            .addAction(journalAction)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
