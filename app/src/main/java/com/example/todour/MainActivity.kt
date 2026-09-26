package com.example.todour

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && viewModel.vaultUri != null) {
            NotificationHelper.showQuickCaptureNotification(this)
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.selectVaultUri(it)
            requestNotificationPermissionAndShow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)

        setContent {
            TodourTheme {
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

        if (viewModel.vaultUri != null) {
            requestNotificationPermissionAndShow()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotes()
    }

    private fun requestNotificationPermissionAndShow() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationHelper.showQuickCaptureNotification(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodourApp(viewModel: MainViewModel, onPickFolder: () -> Unit) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var menuForItemId by remember { mutableStateOf<String?>(null) }

    if (showAddTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Mit szeretnél létrehozni?") },
            text = { Text("„${viewModel.query.take(60)}”") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.add(viewModel.query)
                    selectedItem = viewModel.items.firstOrNull()
                    showAddTypeDialog = false
                }) { Text("Új jegyzet") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addJournalEntry(viewModel.query)
                    viewModel.query = ""
                    showAddTypeDialog = false
                }) { Text("Napló bejegyzés") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todour", style = MaterialTheme.typography.titleLarge) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                        placeholder = { Text("Jegyzet / Keresés...", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            if (viewModel.query.isNotBlank()) showAddTypeDialog = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hozzáadás",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                ) {
                    items(
                        items = viewModel.filteredItems,
                        key = { it.id }
                    ) { item ->
                        val isSelected = item.id == selectedItem?.id
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .combinedClickable(
                                        onClick = { selectedItem = item },
                                        onLongClick = { menuForItemId = item.id }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = item.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuForItemId == item.id,
                                onDismissRequest = { menuForItemId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Törlés") },
                                    onClick = {
                                        viewModel.remove(item)
                                        if (selectedItem?.id == item.id) selectedItem = null
                                        menuForItemId = null
                                    }
                                )
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
                            .weight(0.45f),
                        placeholder = { Text("Írd ide a jegyzetet...") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f),
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
