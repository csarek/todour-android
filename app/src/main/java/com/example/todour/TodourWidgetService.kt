package com.example.todour

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking

class TodourWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return TodourRemoteViewsFactory(applicationContext, widgetId)
    }
}

class TodourRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var entries: List<Pair<String, String>> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        entries = runBlocking {
            val widgetPrefs = context.getSharedPreferences("todour_widget_prefs", Context.MODE_PRIVATE)
            val index = widgetPrefs.getInt("query_index_$widgetId", 0)
                .coerceIn(0, TodourWidgetProvider.QUERIES.size - 1)
            val queryKey = TodourWidgetProvider.QUERIES[index].first

            val vaultPrefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
            val vaultUriString = vaultPrefs.getString("vault_uri", null)

            if (vaultUriString == null) {
                emptyList()
            } else {
                try {
                    val uri = Uri.parse(vaultUriString)
                    val items = VaultRepository.scanVault(context, uri)
                    VaultRepository.computeEntries(queryKey, items)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun onDestroy() {
        entries = emptyList()
    }

    override fun getCount(): Int = entries.size

    override fun getViewAt(position: Int): RemoteViews {
        val (itemId, displayText) = entries[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_list_item)
        rv.setTextViewText(R.id.list_item_text, displayText)
        val fillInIntent = Intent().apply {
            putExtra("open_item_id", itemId)
        }
        rv.setOnClickFillInIntent(R.id.list_item_text, fillInIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
