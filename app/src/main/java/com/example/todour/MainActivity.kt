package com.example.todour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
            Button(onClick = { viewModel.add(viewModel.query) }) {
                Text("Hozzáadás")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.remove(item) }) {
                            Text("✕")
                        }
                    }
                }
            }
        }
    }
}
