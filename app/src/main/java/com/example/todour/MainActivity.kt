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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Kereső + hozzáadás ---
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
                onClick = {
                    viewModel.add(viewModel.query)
                    // Az újonnan létrehozott jegyzet automatikusan megnyílik szerkesztésre
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

        // --- Lista (felső, kisebb rész) ---
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
                        Text(
                            text = item.text,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
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

        // --- Szerkesztő (alsó, nagyobb rész) - itt jelenik meg a kiválasztott jegyzet ---
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
