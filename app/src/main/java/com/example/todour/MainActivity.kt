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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingOpenItemId by mutableStateOf<String?>(null)

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
        pendingOpenItemId = intent?.getStringExtra("open_item_id")

        setContent {
            TodourTheme(darkTheme = viewModel.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodourApp(
                        viewModel = viewModel,
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        openItemId = pendingOpenItemId,
                        onOpenItemHandled = { pendingOpenItemId = null }
                    )
                }
            }
        }

        if (viewModel.vaultUri != null) {
            requestNotificationPermissionAndShow()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenItemId = intent.getStringExtra("open_item_id")
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotes()
        WidgetUpdater.requestUpdate(this)
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
fun TodourApp(
    viewModel: MainViewModel,
    onPickFolder: () -> Unit,
    openItemId: String? = null,
    onOpenItemHandled: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var menuForItemId by remember { mutableStateOf<String?>(null) }

    var showFocusDialog by remember { mutableStateOf(false) }
    var showTodoWaitDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showDueDialog by remember { mutableStateOf(false) }
    var showContextsDialog by remember { mutableStateOf(false) }
    var showJournalListDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Widgetről érkező megnyitási kérés kezelése
    LaunchedEffect(openItemId, viewModel.items.size) {
        if (openItemId != null) {
            val found = viewModel.items.find { it.id == openItemId }
            if (found != null) {
                selectedItem = found
                onOpenItemHandled()
            }
        }
    }

    if (showAddTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Mit szeretnél létrehozni?") },
            text = { Text("„${viewModel.query.take(60)}”") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.add(viewModel.query)
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Todour",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("📅 Ma") },
                    selected = false,
                    onClick = {
                        viewModel.getOrOpenTodayNote { item -> item?.let { selectedItem = it } }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("📥 Inbox") },
                    selected = false,
                    onClick = {
                        viewModel.getOrOpenInbox { item -> item?.let { selectedItem = it } }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("🎯 Fókusz") },
                    selected = false,
                    onClick = {
                        showFocusDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("☑️ TODO / WAIT") },
                    selected = false,
                    onClick = {
                        showTodoWaitDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("# Címkék") },
                    selected = false,
                    onClick = {
                        showTagsDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("⏳ Határidők") },
                    selected = false,
                    onClick = {
                        showDueDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("@ Kontextusok") },
                    selected = false,
                    onClick = {
                        showContextsDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("🗓️ Napló (időrend)") },
                    selected = false,
                    onClick = {
                        showJournalListDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Todour", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Beállítások")
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
                            DropdownMenuItem(
                                text = { Text(if (viewModel.isDarkTheme) "Világos téma" else "Sötét téma") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleTheme()
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
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
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
                                        .padding(vertical = 1.dp)
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
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
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

    // ---- Fókusz dialógus ----
    if (showFocusDialog) {
        val entries = viewModel.focusEntries()
        val today = java.time.LocalDate.now()
        val overdue = entries.filter { it.date.isBefore(today) }
        val dueToday = entries.filter { it.date.isEqual(today) }
        val upcoming = entries.filter { it.date.isAfter(today) }

        AlertDialog(
            onDismissRequest = { showFocusDialog = false },
            title = { Text("Fókusz") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (entries.isEmpty()) {
                        Text("Nincs due: dátumos bejegyzés a következő 3 napban.")
                    } else {
                        if (overdue.isNotEmpty()) {
                            Text("⏰ Lejárt", style = MaterialTheme.typography.labelSmall)
                            overdue.forEach { e ->
                                Text(
                                    "• ${e.line}  (${e.itemName})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.items.find { it.id == e.itemId }?.let { selectedItem = it }
                                            showFocusDialog = false
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (dueToday.isNotEmpty()) {
                            Text("📌 Ma", style = MaterialTheme.typography.labelSmall)
                            dueToday.forEach { e ->
                                Text(
                                    "• ${e.line}  (${e.itemName})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.items.find { it.id == e.itemId }?.let { selectedItem = it }
                                            showFocusDialog = false
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (upcoming.isNotEmpty()) {
                            Text("🔜 Következő 3 nap", style = MaterialTheme.typography.labelSmall)
                            upcoming.forEach { e ->
                                Text(
                                    "• ${e.line}  (${e.itemName})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.items.find { it.id == e.itemId }?.let { selectedItem = it }
                                            showFocusDialog = false
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFocusDialog = false }) { Text("Bezár") }
            }
        )
    }

    // ---- TODO / WAIT dialógus ----
    if (showTodoWaitDialog) {
        val entries = viewModel.todoWaitEntries()
        AlertDialog(
            onDismissRequest = { showTodoWaitDialog = false },
            title = { Text("TODO / WAIT") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (entries.isEmpty()) {
                        Text("Nincs TODO/WAIT bejegyzés.")
                    } else {
                        entries.forEach { e ->
                            Text(
                                "• ${e.line}  (${e.itemName})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.items.find { it.id == e.itemId }?.let { selectedItem = it }
                                        showTodoWaitDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTodoWaitDialog = false }) { Text("Bezár") }
            }
        )
    }

    // ---- Címkék dialógus ----
    if (showTagsDialog) {
        val groups = viewModel.tagIndex()
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            title = { Text("Címkék") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (groups.isEmpty()) {
                        Text("Nincs #cimke a jegyzetekben.")
                    } else {
                        groups.forEach { g ->
                            Text(
                                "#${g.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            g.items.forEach { (id, name) ->
                                Text(
                                    "    • $name",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.items.find { it.id == id }?.let { selectedItem = it }
                                            showTagsDialog = false
                                        }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagsDialog = false }) { Text("Bezár") }
            }
        )
    }

    // ---- Határidők (teljes lista) dialógus ----
    if (showDueDialog) {
        val entries = viewModel.dueList()
        val today = java.time.LocalDate.now()
        AlertDialog(
            onDismissRequest = { showDueDialog = false },
            title = { Text("Határidők (összes)") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (entries.isEmpty()) {
                        Text("Nincs due: bejegyzés.")
                    } else {
                        entries.forEach { e ->
                            val marker = when {
                                e.date.isBefore(today) -> "⏰ "
                                e.date.isEqual(today) -> "📌 "
                                else -> ""
                            }
                            Text(
                                "$marker${e.line}  (${e.itemName})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.items.find { it.id == e.itemId }?.let { selectedItem = it }
                                        showDueDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDueDialog = false }) { Text("Bezár") }
            }
        )
    }

    // ---- Kontextusok dialógus ----
    if (showContextsDialog) {
        val groups = viewModel.contextIndex()
        AlertDialog(
            onDismissRequest = { showContextsDialog = false },
            title = { Text("Kontextusok") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (groups.isEmpty()) {
                        Text("Nincs @kontextus a jegyzetekben.")
                    } else {
                        groups.forEach { g ->
                            Text(
                                "@${g.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            g.items.forEach { (id, name) ->
                                Text(
                                    "    • $name",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.items.find { it.id == id }?.let { selectedItem = it }
                                            showContextsDialog = false
                                        }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextsDialog = false }) { Text("Bezár") }
            }
        )
    }

    // ---- Napló időrendi dialógus ----
    if (showJournalListDialog) {
        val journalItems = viewModel.journalChronological()
        AlertDialog(
            onDismissRequest = { showJournalListDialog = false },
            title = { Text("Napló (időrend)") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (journalItems.isEmpty()) {
                        Text("Még nincs naplóbejegyzés.")
                    } else {
                        journalItems.forEach { j ->
                            Text(
                                j.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItem = j
                                        showJournalListDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJournalListDialog = false }) { Text("Bezár") }
            }
        )
    }
}
