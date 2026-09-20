package com.example.todour

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val items = mutableStateListOf<Item>()
    var query by mutableStateOf("")

    fun add(text: String) {
        if (text.isNotBlank()) {
            items.add(0, Item(text = text.trim()))
            query = ""
        }
    }

    fun remove(item: Item) {
        items.remove(item)
    }

    fun update(item: Item, newText: String) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items[index] = item.copy(text = newText)
        }
    }

    val filteredItems: List<Item>
        get() {
            if (query.isBlank()) return items
            return items.filter { it.text.contains(query, ignoreCase = true) }
        }
}
