package com.example.worktime.alarm.manager

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.lang.SecurityException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WorkAlarmScheduler(
    private val context: Context
) {
    enum class ScheduleResult {
        CREATED_SILENT,
        OPENED_CLOCK_UI,
        OPENED_CLOCK_APP,
        NO_CLOCK_APP,
        FAILED
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun schedule(
        requestCode: Int,
        triggerAtEpochMillis: Long,
        workStartAtEpochMillis: Long,
        vibrate: Boolean = true,
        label: String = ""
    ): ScheduleResult {
        val triggerDateTime = Instant.ofEpochMilli(triggerAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val alarmLabel = buildLabel(triggerAtEpochMillis, workStartAtEpochMillis, requestCode, label)
        val baseIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, triggerDateTime.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, triggerDateTime.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, alarmLabel)
            putExtra(AlarmClock.EXTRA_VIBRATE, vibrate)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val silentIntent = Intent(baseIntent).apply {
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        if (startActivitySafely(silentIntent)) {
            return ScheduleResult.CREATED_SILENT
        }

        val interactiveIntent = Intent(baseIntent).apply {
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        if (startActivitySafely(interactiveIntent)) {
            return ScheduleResult.OPENED_CLOCK_UI
        }

        val showAlarmsIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startActivitySafely(showAlarmsIntent)) {
            return ScheduleResult.OPENED_CLOCK_APP
        }

        if (openKnownClockApp()) {
            return ScheduleResult.OPENED_CLOCK_APP
        }

        return ScheduleResult.NO_CLOCK_APP
    }

    fun cancel(
        requestCode: Int,
        triggerAtEpochMillis: Long,
        workStartAtEpochMillis: Long,
        label: String = ""
    ): Boolean {
        val alarmLabel = buildLabel(triggerAtEpochMillis, workStartAtEpochMillis, requestCode, label)
        val dismissByLabel = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, alarmLabel)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startActivitySafely(dismissByLabel)
    }

    private fun startActivitySafely(intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent)
            true
        }.recoverCatching {
            if (it is SecurityException) throw it
            false
        }.getOrElse { false }
    }

    private fun openKnownClockApp(): Boolean {
        val packageManager = context.packageManager
        val knownClockPackages = listOf(
            "com.coloros.alarmclock",       // OPPO
            "com.heytap.alarmclock",        // OPPO/realme
            "com.oplus.deskclock",          // OPlus
            "com.huawei.deskclock",         // Huawei
            "com.hihonor.deskclock",        // Honor
            "com.android.deskclock",        // AOSP / Pixel-like
            "com.miui.clock",               // Xiaomi
            "com.vivo.alarmclock",          // vivo
            "com.sec.android.app.clockpackage" // Samsung
        )
        for (pkg in knownClockPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg) ?: continue
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startActivitySafely(launchIntent)) return true
        }
        return false
    }

    private fun buildLabel(
        triggerAtEpochMillis: Long,
        workStartAtEpochMillis: Long,
        requestCode: Int,
        extraLabel: String
    ): String {
        val trigger = formatter.format(
            Instant.ofEpochMilli(triggerAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
        val workStart = formatter.format(
            Instant.ofEpochMilli(workStartAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
        val labelPart = if (extraLabel.isNotEmpty()) "$extraLabel " else ""
        return "倒班#$labelPart#$requestCode 提醒:$trigger 上班:$workStart"
    }
}
