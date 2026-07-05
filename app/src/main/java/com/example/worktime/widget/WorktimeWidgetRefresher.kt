package com.example.worktime.widget

import android.content.Context

class WorktimeWidgetRefresher(
    private val context: Context
) {
    fun refresh() {
        WorktimeWidgetUpdater.requestRefresh(context)
    }
}
