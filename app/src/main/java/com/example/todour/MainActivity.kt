package com.example.todour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodourApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TodourApp(viewModel: MainViewModel) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    val current = selectedItem
    if (current != null) {
        EditNoteScreen(
            item = current,
            onSave = { newText ->
                viewModel.update(current, newText)
                selectedItem = null
            },
            onBack = { selectedItem = null }
        )
    } else {
        NoteListScreen(
            viewModel = viewModel,
            onItemClick = { item -> selectedItem = item }
        )
    }
}

@Composable
fun NoteListScreen(viewModel: MainViewModel, onItemClick: (Item) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                onClick = { viewModel.add(viewModel.query) },
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

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = viewModel.filteredItems,
                key = { it.id }
            ) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(onClick = { viewModel.remove(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Törlés")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(item: Item, onSave: (String) -> Unit, onBack: () -> Unit) {
    var text by remember { mutableStateOf(item.text) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Jegyzet szerkesztése") },
            navigationIcon = {
                IconButton(onClick = { onSave(text) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Vissza / Mentés")
                }
            }
        )
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            placeholder = { Text("Írd ide a jegyzetet...") }
        )
    }
}
