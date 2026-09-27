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

    companion object {
        suspend fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences("todour_prefs", Context.MODE_PRIVATE)
            val vaultUriString = prefs.getString("vault_uri", null)

            val statusText: String = if (vaultUriString == null) {
                "Nincs mappa kiválasztva.\nNyisd meg az appot a beállításhoz."
            } else {
                try {
                    val uri = Uri.parse(vaultUriString)
                    val items = VaultRepository.scanVault(context, uri)
                    val focusEntries = VaultRepository.computeFocusEntries(items)
                    if (focusEntries.isEmpty()) {
                        "🎯 Fókusz: nincs aktuális határidő."
                    } else {
                        val lines = focusEntries.take(5).map { "• ${it.line}" }
                        val extra = focusEntries.size - lines.size
                        val suffix = if (extra > 0) "\n… és $extra további" else ""
                        "🎯 Fókusz (${focusEntries.size}):\n" + lines.joinToString("\n") + suffix
                    }
                } catch (e: Exception) {
                    "Nem sikerült betölteni a jegyzeteket."
                }
            }

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_todour)
                views.setTextViewText(R.id.widget_status, statusText)
                views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_status, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
