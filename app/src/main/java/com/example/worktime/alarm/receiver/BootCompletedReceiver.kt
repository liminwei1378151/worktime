package com.example.worktime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.worktime.alarm.notification.WorkNotificationHelper
import com.example.worktime.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        WorkNotificationHelper.ensureChannel(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val scheduler = AppContainer.alarmScheduler(context)
                val repository = AppContainer.repository(context)
                val now = System.currentTimeMillis()
                val mappings = repository.getAlarmMappings()
                mappings.filter { it.triggerAtEpochMillis >= now }.forEach { mapping ->
                    scheduler.schedule(
                        requestCode = mapping.requestCode,
                        triggerAtEpochMillis = mapping.triggerAtEpochMillis,
                        workStartAtEpochMillis = mapping.workStartAtEpochMillis
                    )
                }
            }
            pendingResult.finish()
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
