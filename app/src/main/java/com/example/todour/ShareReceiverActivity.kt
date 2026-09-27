package com.example.todour

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            else -> ""
        }

        val prefs = getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
        val vaultUriString = prefs.getString("vault_uri", null)
        val datePattern = prefs.getString("journal_date_format", "yyyy_MM_dd") ?: "yyyy_MM_dd"

        setContent {
            TodourTheme(darkTheme = prefs.getBoolean("dark_theme", false)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShareDialogContent(
                        sharedText = sharedText,
                        hasVault = vaultUriString != null,
                        onSendToInbox = {
                            saveToInbox(vaultUriString, sharedText)
                            Toast.makeText(this, "Elmentve az Inbox-ba", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onSendToJournal = {
                            saveToJournal(vaultUriString, datePattern, sharedText)
                            Toast.makeText(this, "Elmentve a napi jegyzetbe", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveToInbox(vaultUriString: String?, text: String) {
        if (vaultUriString == null || text.isBlank()) return
        val root = DocumentFile.fromTreeUri(this, Uri.parse(vaultUriString)) ?: return
        val inboxFile = root.findFile("INBOX.md") ?: root.createFile("text/markdown", "INBOX.md") ?: return
        val previous = VaultRepository.readFileContent(this, inboxFile.uri)
        val newContent = if (previous.isBlank()) "- ${text.trim()}" else "$previous\n- ${text.trim()}"
        VaultRepository.writeFileContent(this, inboxFile.uri, newContent)
    }

    private fun saveToJournal(vaultUriString: String?, pattern: String, text: String) {
        if (vaultUriString == null || text.isBlank()) return
        val root = DocumentFile.fromTreeUri(this, Uri.parse(vaultUriString)) ?: return
        val journalsFolder = VaultRepository.getOrCreateFolder(root, "journals") ?: return
        val fileName = SimpleDateFormat(pattern, Locale.getDefault()).format(Date()) + ".md"
        val existing = journalsFolder.findFile(fileName)
        val previous = existing?.let { VaultRepository.readFileContent(this, it.uri) } ?: ""
        val newContent = if (previous.isBlank()) text.trim() else "$previous\n\n${text.trim()}"
        val targetFile = existing ?: journalsFolder.createFile("text/markdown", fileName)
        val targetUri = targetFile?.uri ?: return
        VaultRepository.writeFileContent(this, targetUri, newContent)
    }
}

@Composable
fun ShareDialogContent(
    sharedText: String,
    hasVault: Boolean,
    onSendToInbox: () -> Unit,
    onSendToJournal: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Küldés a Todour-ba", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                if (!hasVault) {
                    Text("Előbb válassz jegyzetmappát a Todour appban.")
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                        Text("Bezár")
                    }
                } else {
                    Text(
                        sharedText.take(200),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onSendToInbox) { Text("📥 Inbox") }
                        Button(onClick = onSendToJournal) { Text("📅 Mai napló") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                        Text("Mégse")
                    }
                }
            }
        }
    }
}
