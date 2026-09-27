package com.example.todour

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object VaultRepository {

    data class FocusEntry(
        val itemId: String,
        val itemName: String,
        val line: String,
        val date: java.time.LocalDate
    )

    suspend fun scanVault(context: Context, treeUri: Uri): List<Item> = coroutineScope {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val dirsToProcess = ArrayDeque<Pair<String, String>>()
        dirsToProcess.add(rootDocId to "")

        val fileDocs = mutableListOf<Triple<String, String, String>>()

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        while (dirsToProcess.isNotEmpty()) {
            val (currentDocId, topFolder) = dirsToProcess.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)

            try {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex) ?: continue
                        val mime = cursor.getString(mimeIndex)

                        if (name.startsWith(".")) continue

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            val childTopFolder = if (topFolder.isEmpty()) name else topFolder
                            dirsToProcess.add(docId to childTopFolder)
                        } else if (name.endsWith(".md", true) || name.endsWith(".txt", true)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            fileDocs.add(Triple(docUri.toString(), name, topFolder))
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }

        val semaphore = Semaphore(8)
        fileDocs.map { (uriString, name, topFolder) ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val content = readFileContent(context, Uri.parse(uriString))
                    Item(id = uriString, name = name, text = content, folder = topFolder)
                }
            }
        }.awaitAll()
    }

    fun readFileContent(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun writeFileContent(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                OutputStreamWriter(output).use { it.write(content) }
            }
        } catch (e: Exception) { }
    }

    fun getOrCreateFolder(root: DocumentFile, name: String): DocumentFile? {
        return root.findFile(name)?.takeIf { it.isDirectory } ?: root.createDirectory(name)
    }

    fun computeFocusEntries(items: List<Item>): List<FocusEntry> {
        val today = java.time.LocalDate.now()
        val limit = today.plusDays(3)
        val regex = Regex("""due:(\d{4}-\d{2}-\d{2})""")
        val result = mutableListOf<FocusEntry>()

        items.forEach { item ->
            item.text.lines().forEach { line ->
                val match = regex.find(line)
                if (match != null) {
                    val date = try {
                        java.time.LocalDate.parse(match.groupValues[1])
                    } catch (e: Exception) {
                        null
                    }
                    if (date != null && !date.isAfter(limit)) {
                        result.add(FocusEntry(item.id, item.name, line.trim(), date))
                    }
                }
            }
        }
        return result.sortedBy { it.date }
    }
}
