package com.example.worktime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.worktime.MainActivity
import com.example.worktime.R
import com.example.worktime.di.AppContainer
import com.example.worktime.domain.model.DailyActivity
import com.example.worktime.domain.model.ShiftEventType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking

object WorktimeWidgetUpdater {
    const val ACTION_REFRESH_WIDGET = "com.example.worktime.action.REFRESH_WIDGET"

    fun requestRefresh(context: Context) {
        val intent = Intent(context, WorktimeWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        context.sendBroadcast(intent)
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WorktimeWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return

        val display = loadDisplay(context)
        widgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.worktime_widget)
            views.setTextViewText(R.id.widgetLine1, display.line1)
            views.setTextViewText(R.id.widgetLine2, display.line2)
            views.setViewVisibility(R.id.widgetBadgeRow, View.GONE)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun loadDisplay(context: Context): WidgetDisplay = runBlocking {
        val repo = AppContainer.repository(context)
        val now = System.currentTimeMillis()
        val allWorkEvents = repo.getFutureWorkEvents(now - Duration.ofHours(12).toMillis())
            .filter { it.type == ShiftEventType.WORK }
        val workEvents = allWorkEvents.filter { it.startAtEpochMillis >= now }

        if (workEvents.isEmpty()) {
            WidgetDisplay(line1 = "暂无排班", line2 = "暂无排班")
        } else {
            val next = workEvents[0]
            val nextDate = toLocalDate(next.startAtEpochMillis)
            val today = toLocalDate(now)
            val daysDiff = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val nextHour = toLocalTime(next.startAtEpochMillis).hour
            val nextMinute = toLocalTime(next.startAtEpochMillis).minute
            val nextActivities = DailyActivity.getActivitiesForWork(nextHour, nextMinute)
            val nextActivitiesText = if (nextActivities.isNotEmpty()) " ${nextActivities.take(3).joinToString(" ")}" else ""
            
            val line1 = buildString {
                append(relativeDate(daysDiff))
                append(" ")
                append(formatTime(next.startAtEpochMillis))
                append("-")
                append(formatTime(next.endAtEpochMillis))
                append(nextActivitiesText)
            }
            
            val line2 = if (workEvents.size > 1) {
                val nextNext = workEvents[1]
                val nextNextDate = toLocalDate(nextNext.startAtEpochMillis)
                val daysDiff2 = ChronoUnit.DAYS.between(today, nextNextDate).toInt()
                val nextNextHour = toLocalTime(nextNext.startAtEpochMillis).hour
                val nextNextMinute = toLocalTime(nextNext.startAtEpochMillis).minute
                val nextNextActivities = DailyActivity.getActivitiesForWork(nextNextHour, nextNextMinute)
                val nextNextActivitiesText = if (nextNextActivities.isNotEmpty()) " ${nextNextActivities.take(3).joinToString(" ")}" else ""
                buildString {
                    append(relativeDate(daysDiff2))
                    append(" ")
                    append(formatTime(nextNext.startAtEpochMillis))
                    append("-")
                    append(formatTime(nextNext.endAtEpochMillis))
                    append(nextNextActivitiesText)
                }
            } else {
                buildString { append("") }
            }

            WidgetDisplay(line1 = line1, line2 = line2)
        }
    }

    private fun relativeDate(daysDiff: Int): String = when (daysDiff) {
        0 -> "今天"
        1 -> "明天"
        2 -> "后天"
        in 3..4 -> "${daysDiff}天后"
        else -> "${daysDiff}天后"
    }

    private fun formatTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    private fun toLocalTime(epochMillis: Long): java.time.LocalTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
    }

    private fun toLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private data class WidgetDisplay(
        val line1: String,
        val line2: String
    )
}
