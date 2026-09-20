package com.example.todour

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setVaultUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodourApp(
                        viewModel = viewModel,
                        onPickFolder = { folderPickerLauncher.launch(null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodourApp(viewModel: MainViewModel, onPickFolder: () -> Unit) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showJournalDialog by remember { mutableStateOf(false) }

    if (showJournalDialog) {
        JournalEntryDialog(
            onDismiss = { showJournalDialog = false },
            onConfirm = { text ->
                viewModel.addJournalEntry(text)
                showJournalDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todour") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Jegyzetmappa kiválasztása") },
                            onClick = {
                                menuExpanded = false
                                onPickFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Frissítés") },
                            onClick = {
                                menuExpanded = false
                                viewModel.loadNotes()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Napló dátumformátum: " +
                                        if (viewModel.dateFormatPattern == "yyyy_MM_dd") "2026_09_20" else "2026-09-20"
                                )
                            },
                            onClick = {
                                val next = if (viewModel.dateFormatPattern == "yyyy_MM_dd")
                                    "yyyy-MM-dd" else "yyyy_MM_dd"
                                viewModel.setDateFormat(next)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (viewModel.vaultUri == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Még nincs kiválasztva jegyzetmappa.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onPickFolder) {
                            Text("Mappa kiválasztása")
                        }
                    }
                }
            } else if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = viewModel.query,
                        onValueChange = { viewModel.query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Jegyzet / Keresés...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showJournalDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Napló bejegyzés",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.add(viewModel.query)
                            selectedItem = viewModel.items.firstOrNull()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hozzáadás",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                ) {
                    items(
                        items = viewModel.filteredItems,
                        key = { it.id }
                    ) { item ->
                        val isSelected = item.id == selectedItem?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedItem = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = item.text.take(60).replace("\n", " "),
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.remove(item)
                                    if (selectedItem?.id == item.id) selectedItem = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Törlés")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                val current = selectedItem
                if (current != null) {
                    var editedText by remember(current.id) { mutableStateOf(current.text) }
                    TextField(
                        value = editedText,
                        onValueChange = {
                            editedText = it
                            viewModel.update(current, it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        placeholder = { Text("Írd ide a jegyzetet...") }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Válassz egy jegyzetet a listából, vagy hozz létre újat.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalEntryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Napló bejegyzés") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Mi történt ma?") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Mentés") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Mégse") }
        }
    )
}
