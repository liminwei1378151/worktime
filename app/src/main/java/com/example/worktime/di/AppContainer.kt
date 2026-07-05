package com.example.worktime.di

import android.content.Context
import com.example.worktime.alarm.manager.WorkAlarmScheduler
import com.example.worktime.data.local.db.WorktimeDatabase
import com.example.worktime.data.local.settings.SettingsStore
import com.example.worktime.data.repository.WorktimeRepository
import com.example.worktime.domain.scheduler.ShiftScheduler
import com.example.worktime.widget.WorktimeWidgetRefresher

object AppContainer {

    @Volatile
    private var database: WorktimeDatabase? = null

    @Volatile
    private var repository: WorktimeRepository? = null

    @Volatile
    private var settingsStore: SettingsStore? = null

    @Volatile
    private var alarmScheduler: WorkAlarmScheduler? = null

    @Volatile
    private var widgetRefresher: WorktimeWidgetRefresher? = null

    fun repository(context: Context): WorktimeRepository {
        return repository ?: synchronized(this) {
            repository ?: WorktimeRepository(
                database = database(context),
                shiftScheduler = ShiftScheduler()
            ).also { repository = it }
        }
    }

    fun settingsStore(context: Context): SettingsStore {
        return settingsStore ?: synchronized(this) {
            settingsStore ?: SettingsStore(context.applicationContext).also { settingsStore = it }
        }
    }

    fun alarmScheduler(context: Context): WorkAlarmScheduler {
        return alarmScheduler ?: synchronized(this) {
            alarmScheduler ?: WorkAlarmScheduler(context.applicationContext)
                .also { alarmScheduler = it }
        }
    }

    fun widgetRefresher(context: Context): WorktimeWidgetRefresher {
        return widgetRefresher ?: synchronized(this) {
            widgetRefresher ?: WorktimeWidgetRefresher(context.applicationContext)
                .also { widgetRefresher = it }
        }
    }

    private fun database(context: Context): WorktimeDatabase {
        return database ?: synchronized(this) {
            database ?: WorktimeDatabase.create(context.applicationContext).also { database = it }
        }
    }
}
