package com.example.todour

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val componentName = ComponentName(appContext, TodourWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(componentName)
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            TodourWidgetProvider.updateAllWidgets(appContext, manager, ids)
        }
    }
}
