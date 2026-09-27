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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickCaptureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val target = intent.getStringExtra(NotificationHelper.EXTRA_TARGET) ?: return
        val results = RemoteInput.getResultsFromIntent(intent) ?: return

        val key = if (target == NotificationHelper.TARGET_INBOX)
            NotificationHelper.KEY_INBOX_INPUT else NotificationHelper.KEY_JOURNAL_INPUT
        val text = results.getCharSequence(key)?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            if (target == NotificationHelper.TARGET_INBOX) {
                appendToInbox(context, text)
            } else {
                appendToJournal(context, text)
            }
        }

        NotificationHelper.showQuickCaptureNotification(context)
    }

    private fun readContent(context: Context, uri: Uri): String = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: ""
    } catch (e: Exception) { "" }

    private fun writeContent(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                OutputStreamWriter(output).use { it.write(content) }
            }
        } catch (e: Exception) { }
    }

    private fun appendToInbox(context: Context, text: String) {
        val prefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
        val vaultUriString = prefs.getString("vault_uri", null) ?: return
        val root = DocumentFile.fromTreeUri(context, Uri.parse(vaultUriString)) ?: return
        val inboxFile = root.findFile("INBOX.md") ?: root.createFile("text/markdown", "INBOX.md") ?: return

        val previous = readContent(context, inboxFile.uri)
        val newContent = if (previous.isBlank()) "- $text" else "$previous\n- $text"
        writeContent(context, inboxFile.uri, newContent)
    }

    private fun appendToJournal(context: Context, text: String) {
        val prefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
        val vaultUriString = prefs.getString("vault_uri", null) ?: return
        val pattern = prefs.getString("journal_date_format", "yyyy_MM_dd") ?: "yyyy_MM_dd"
        val root = DocumentFile.fromTreeUri(context, Uri.parse(vaultUriString)) ?: return
        val journalsFolder = root.findFile("journals")?.takeIf { it.isDirectory }
            ?: root.createDirectory("journals") ?: return

        val fileName = SimpleDateFormat(pattern, Locale.getDefault()).format(Date()) + ".md"
        val existing = journalsFolder.findFile(fileName)
        val previous = existing?.let { readContent(context, it.uri) } ?: ""
        val newContent = if (previous.isBlank()) text else "$previous\n\n$text"

        val targetFile = existing ?: journalsFolder.createFile("text/markdown", fileName)
        val targetUri = targetFile?.uri ?: return
        writeContent(context, targetUri, newContent)
    }
}
