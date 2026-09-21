package com.example.todour

import android.app.Application
import android.content.Context        
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    init {
        dateFormatPattern = prefs.getString("journal_date_format", "yyyy_MM_dd") ?: "yyyy_MM_dd"
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

    fun loadNotes() {
        val uri = vaultUri ?: return
        val context = getApplication<Application>()
        isLoading = true
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                val root = DocumentFile.fromTreeUri(context, uri)
                val result = mutableListOf<Item>()
                if (root != null) collectNotes(context, root, result)
                result
            }
            items.clear()
            items.addAll(found)
            isLoading = false
        }
    }

    private fun collectNotes(context: Context, dir: DocumentFile, out: MutableList<Item>) {
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                collectNotes(context, file, out)
            } else {
                val name = file.name ?: return@forEach
                if (name.endsWith(".txt", true) || name.endsWith(".md", true)) {
                    val content = try {
                        context.contentResolver.openInputStream(file.uri)?.use { input ->
                            BufferedReader(InputStreamReader(input)).readText()
                        } ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                    out.add(Item(id = file.uri.toString(), name = name, text = content))
                }
            }
        }
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

    // Új jegyzet -> pages/ mappa
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
        items.add(0, Item(id = newFile.uri.toString(), name = fileName, text = text.trim()))
        query = ""
    }

    // Napló bejegyzés -> journals/ mappa, mai dátummal, hozzáfűzve ha már létezik
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
        val updatedItem = Item(id = targetUri.toString(), name = fileName, text = newContent)
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

            }
        }
}
