package com.example.todour

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)

    val items = mutableStateListOf<Item>()
    var query by mutableStateOf("")
    var vaultUri by mutableStateOf<Uri?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var dateFormatPattern by mutableStateOf("yyyy_MM_dd")
        private set
    var isDarkTheme by mutableStateOf(false)
        private set

    init {
        dateFormatPattern = prefs.getString("journal_date_format", "yyyy_MM_dd") ?: "yyyy_MM_dd"
        isDarkTheme = prefs.getBoolean("dark_theme", false)
        prefs.getString("vault_uri", null)?.let { saved ->
            vaultUri = Uri.parse(saved)
            loadNotes()
        }
    }

    fun selectVaultUri(uri: Uri) {
        vaultUri = uri
        prefs.edit().putString("vault_uri", uri.toString()).apply()
        loadNotes()
    }

    fun setDateFormat(pattern: String) {
        dateFormatPattern = pattern
        prefs.edit().putString("journal_date_format", pattern).apply()
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        prefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
    }

    fun getOrOpenTodayNote(): Item? {
        val uri = vaultUri ?: return null
        val context = getApplication<Application>()
        val root = DocumentFile.fromTreeUri(context, uri) ?: return null
        val journalsFolder = getOrCreateFolder(root, "journals") ?: return null

        val sdf = SimpleDateFormat(dateFormatPattern, Locale.getDefault())
        val fileName = sdf.format(Date()) + ".md"
        val existing = journalsFolder.findFile(fileName)
        val targetFile = existing ?: journalsFolder.createFile("text/markdown", fileName)
        val targetUri = targetFile?.uri ?: return null

        val content = readFileContent(context, targetUri)
        val item = Item(id = targetUri.toString(), name = fileName, text = content, folder = "journals")

        val existingIndex = items.indexOfFirst { it.id == item.id }
        if (existingIndex != -1) items[existingIndex] = item else items.add(0, item)

        return item
    }

    fun loadNotes() {
        val uri = vaultUri ?: return
        val context = getApplication<Application>()
        isLoading = true
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                collectNotesFast(context, uri)
            }
            items.clear()
            items.addAll(found)
            isLoading = false
        }
    }

    private suspend fun collectNotesFast(context: Context, treeUri: Uri): List<Item> = coroutineScope {
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

    private fun getOrCreateFolder(root: DocumentFile, name: String): DocumentFile? {
        return root.findFile(name)?.takeIf { it.isDirectory } ?: root.createDirectory(name)
    }

    private fun readFileContent(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun writeFileContent(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                OutputStreamWriter(output).use { it.write(content) }
            }
        } catch (e: Exception) { }
    }

    fun add(text: String) {
        val uri = vaultUri ?: return
        if (text.isBlank()) return
        val context = getApplication<Application>()
        val root = DocumentFile.fromTreeUri(context, uri) ?: return
        val pagesFolder = getOrCreateFolder(root, "pages") ?: return
        val fileName = text.trim().take(40)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".md"
        val newFile = pagesFolder.createFile("text/markdown", fileName) ?: return
        writeFileContent(context, newFile.uri, text.trim())
        items.add(0, Item(id = newFile.uri.toString(), name = fileName, text = text.trim(), folder = "pages"))
        query = ""
    }

    fun addJournalEntry(text: String) {
        val uri = vaultUri ?: return
        if (text.isBlank()) return
        val context = getApplication<Application>()
        val root = DocumentFile.fromTreeUri(context, uri) ?: return
        val journalsFolder = getOrCreateFolder(root, "journals") ?: return

        val sdf = SimpleDateFormat(dateFormatPattern, Locale.getDefault())
        val fileName = sdf.format(Date()) + ".md"
        val existing = journalsFolder.findFile(fileName)

        val previousContent = existing?.let { readFileContent(context, it.uri) } ?: ""
        val newContent = if (previousContent.isBlank()) text.trim()
            else "$previousContent\n\n${text.trim()}"

        val targetFile = existing ?: journalsFolder.createFile("text/markdown", fileName)
        val targetUri = targetFile?.uri ?: return
        writeFileContent(context, targetUri, newContent)

        val existingIndex = items.indexOfFirst { it.id == targetUri.toString() }
        val updatedItem = Item(id = targetUri.toString(), name = fileName, text = newContent, folder = "journals")
        if (existingIndex != -1) items[existingIndex] = updatedItem else items.add(0, updatedItem)
    }

    fun remove(item: Item) {
        val context = getApplication<Application>()
        try {
            DocumentFile.fromSingleUri(context, Uri.parse(item.id))?.delete()
        } catch (e: Exception) { }
        items.removeAll { it.id == item.id }
    }

    fun update(item: Item, newText: String) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items[index] = item.copy(text = newText)
        }
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            writeFileContent(context, Uri.parse(item.id), newText)
        }
    }

    val filteredItems: List<Item>
        get() {
            val q = query.trim()
            if (q.isBlank()) return items
            return items
                .filter { it.name.contains(q, ignoreCase = true) || it.text.contains(q, ignoreCase = true) }
                .sortedByDescending { if (it.name.contains(q, ignoreCase = true)) 1 else 0 }
        }

    data class FocusEntry(
        val itemId: String,
        val itemName: String,
        val line: String,
        val date: java.time.LocalDate
    )

    fun focusEntries(): List<FocusEntry> {
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

    data class LineEntry(val itemId: String, val itemName: String, val line: String)

    fun todoWaitEntries(): List<LineEntry> {
        val result = mutableListOf<LineEntry>()
        items.forEach { item ->
            item.text.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("TODO") || trimmed.startsWith("WAIT")) {
                    result.add(LineEntry(item.id, item.name, trimmed))
                }
            }
        }
        return result
    }

    data class DueEntry(
        val itemId: String,
        val itemName: String,
        val line: String,
        val date: java.time.LocalDate
    )

    fun dueList(): List<DueEntry> {
        val regex = Regex("""due:(\d{4}-\d{2}-\d{2})""")
        val result = mutableListOf<DueEntry>()
        items.forEach { item ->
            item.text.lines().forEach { line ->
                val match = regex.find(line)
                if (match != null) {
                    val date = try {
                        java.time.LocalDate.parse(match.groupValues[1])
                    } catch (e: Exception) {
                        null
                    }
                    if (date != null) {
                        result.add(DueEntry(item.id, item.name, line.trim(), date))
                    }
                }
            }
        }
        return result.sortedBy { it.date }
    }

    data class TagGroup(val tag: String, val items: List<Pair<String, String>>)

    private fun extractGroups(regex: Regex): List<TagGroup> {
        val map = linkedMapOf<String, MutableSet<Pair<String, String>>>()
        items.forEach { item ->
            regex.findAll(item.text).forEach { m ->
                val key = m.groupValues[1]
                map.getOrPut(key) { mutableSetOf() }.add(item.id to item.name)
            }
        }
        return map.entries
            .sortedBy { it.key.lowercase(Locale.getDefault()) }
            .map { (tag, set) -> TagGroup(tag, set.sortedBy { it.second }) }
    }

    fun tagIndex(): List<TagGroup> = extractGroups(Regex("#([\\p{L}\\p{N}_-]+)"))

    fun contextIndex(): List<TagGroup> = extractGroups(Regex("@([\\p{L}\\p{N}_-]+)"))

    fun journalChronological(): List<Item> =
        items.filter { it.folder == "journals" }.sortedByDescending { it.name }
}
