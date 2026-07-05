package com.example.worktime.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WidgetPinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorktimeWidgetUpdater.requestRefresh(context)
        Toast.makeText(context, "小组件已添加到桌面", Toast.LENGTH_SHORT).show()
    }
}
