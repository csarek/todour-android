package com.example.todour

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class QuickCaptureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val text = results.getCharSequence(NotificationHelper.KEY_QUICK_NOTE_INPUT)?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            appendToInbox(context, text)
        }

        // Az értesítés mezőjének "visszaállítása", hogy azonnal újra írható legyen
        NotificationHelper.showQuickCaptureNotification(context)
    }

    private fun appendToInbox(context: Context, text: String) {
        val prefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
        val vaultUriString = prefs.getString("vault_uri", null) ?: return
        val vaultUri = Uri.parse(vaultUriString)

        val root = DocumentFile.fromTreeUri(context, vaultUri) ?: return
        val inboxFile = root.findFile("INBOX.md") ?: root.createFile("text/markdown", "INBOX.md") ?: return

        val previousContent = try {
            context.contentResolver.openInputStream(inboxFile.uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        val newContent = if (previousContent.isBlank()) "- $text" else "$previousContent\n- $text"

        try {
            context.contentResolver.openOutputStream(inboxFile.uri, "wt")?.use { output ->
                OutputStreamWriter(output).use { it.write(newContent) }
            }
        } catch (e: Exception) { }
    }
}
