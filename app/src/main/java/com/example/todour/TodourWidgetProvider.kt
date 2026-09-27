package com.example.todour

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodourWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PREV || intent.action == ACTION_NEXT) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (widgetId != -1) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getInt(keyFor(widgetId), 0)
                val delta = if (intent.action == ACTION_NEXT) 1 else -1
                val newIndex = ((current + delta) % QUERIES.size + QUERIES.size) % QUERIES.size
                prefs.edit().putInt(keyFor(widgetId), newIndex).apply()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val manager = AppWidgetManager.getInstance(context)
                        updateSingleWidget(context, manager, widgetId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_PREV = "com.example.todour.WIDGET_PREV"
        const val ACTION_NEXT = "com.example.todour.WIDGET_NEXT"
        private const val PREFS_NAME = "todour_widget_prefs"

        // (belső kulcs, megjelenített cím)
        val QUERIES = listOf(
            "focus" to "🎯 Fókusz",
            "todo" to "☑️ TODO / WAIT",
            "tags" to "# Címkék",
            "due" to "⏳ Határidők",
            "contexts" to "@ Kontextusok",
            "journal" to "🗓️ Napló"
        )

        private fun keyFor(widgetId: Int) = "query_index_$widgetId"

        suspend fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            appWidgetIds.forEach { widgetId ->
                updateSingleWidget(context, appWidgetManager, widgetId)
            }
        }

        suspend fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val widgetPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val index = widgetPrefs.getInt(keyFor(widgetId), 0).coerceIn(0, QUERIES.size - 1)
            val (queryKey, queryLabel) = QUERIES[index]

            val vaultPrefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
            val vaultUriString = vaultPrefs.getString("vault_uri", null)

            val statusText = if (vaultUriString == null) {
                "Nincs mappa kiválasztva.\nNyisd meg az appot a beállításhoz."
            } else {
                try {
                    val uri = Uri.parse(vaultUriString)
                    val items = VaultRepository.scanVault(context, uri)
                    VaultRepository.renderQuery(queryKey, items)
                } catch (e: Exception) {
                    "Nem sikerült betölteni a jegyzeteket."
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_todour)
            views.setTextViewText(R.id.widget_query_label, queryLabel)
            views.setTextViewText(R.id.widget_status, statusText)

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_status, launchPendingIntent)

            views.setOnClickPendingIntent(R.id.widget_prev, navPendingIntent(context, widgetId, ACTION_PREV))
            views.setOnClickPendingIntent(R.id.widget_next, navPendingIntent(context, widgetId, ACTION_NEXT))

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun navPendingIntent(context: Context, widgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, TodourWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val requestCode = widgetId * 10 + (if (action == ACTION_NEXT) 1 else 2)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
