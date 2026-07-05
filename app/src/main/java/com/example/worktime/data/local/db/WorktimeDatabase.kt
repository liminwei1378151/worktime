package com.example.worktime.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.worktime.data.local.dao.AlarmMappingDao
import com.example.worktime.data.local.dao.PlanHistoryDao
import com.example.worktime.data.local.dao.PlanHistoryEventDao
import com.example.worktime.data.local.dao.ShiftEventDao
import com.example.worktime.data.local.dao.ShiftPlanDao
import com.example.worktime.data.local.entity.AlarmMappingEntity
import com.example.worktime.data.local.entity.PlanHistoryEntity
import com.example.worktime.data.local.entity.PlanHistoryEventEntity
import com.example.worktime.data.local.entity.ShiftEventEntity
import com.example.worktime.data.local.entity.ShiftPlanEntity

@Database(
    entities = [
        ShiftPlanEntity::class, ShiftEventEntity::class, AlarmMappingEntity::class,
        PlanHistoryEntity::class, PlanHistoryEventEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class WorktimeDatabase : RoomDatabase() {

    abstract fun shiftPlanDao(): ShiftPlanDao
    abstract fun shiftEventDao(): ShiftEventDao
    abstract fun alarmMappingDao(): AlarmMappingDao
    abstract fun planHistoryDao(): PlanHistoryDao
    abstract fun planHistoryEventDao(): PlanHistoryEventDao

    companion object {
        private const val DB_NAME = "worktime.db"

        fun create(context: Context): WorktimeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WorktimeDatabase::class.java,
                DB_NAME
            ).addMigrations(
                object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE shift_plan ADD COLUMN endAtEpochMillis INTEGER")
                    }
                },
                object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE shift_plan RENAME TO shift_plan_old")
                        database.execSQL("""CREATE TABLE shift_plan (
                            id INTEGER PRIMARY KEY,
                            totalDays INTEGER NOT NULL,
                            workDurationMinutes INTEGER NOT NULL,
                            restDurationMinutes INTEGER NOT NULL,
                            firstWorkStartAtEpochMillis INTEGER NOT NULL,
                            silentRemindBeforeMinutes INTEGER NOT NULL DEFAULT 15,
                            departRemindBeforeMinutes INTEGER NOT NULL DEFAULT 30,
                            endAtEpochMillis INTEGER,
                            updatedAtEpochMillis INTEGER NOT NULL
                        )""")
                        database.execSQL("""INSERT INTO shift_plan 
                            (id, totalDays, workDurationMinutes, restDurationMinutes, 
                            firstWorkStartAtEpochMillis, silentRemindBeforeMinutes, departRemindBeforeMinutes, 
                            endAtEpochMillis, updatedAtEpochMillis)
                            SELECT id, totalDays, workDurationMinutes, restDurationMinutes, 
                            firstWorkStartAtEpochMillis, 15, 30, endAtEpochMillis, updatedAtEpochMillis 
                            FROM shift_plan_old""")
                        database.execSQL("DROP TABLE shift_plan_old")

                        database.execSQL("ALTER TABLE shift_event RENAME TO shift_event_old")
                        database.execSQL("""CREATE TABLE shift_event (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            planId INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            startAtEpochMillis INTEGER NOT NULL,
                            endAtEpochMillis INTEGER NOT NULL,
                            alarmTriggerAtEpochMillis INTEGER,
                            silentAlarmTriggerAtEpochMillis INTEGER,
                            departAlarmTriggerAtEpochMillis INTEGER,
                            FOREIGN KEY(planId) REFERENCES shift_plan(id) ON DELETE CASCADE
                        )""")
                        database.execSQL("""INSERT INTO shift_event 
                            (id, planId, type, startAtEpochMillis, endAtEpochMillis, alarmTriggerAtEpochMillis, 
                            silentAlarmTriggerAtEpochMillis, departAlarmTriggerAtEpochMillis)
                            SELECT id, planId, type, startAtEpochMillis, endAtEpochMillis, alarmTriggerAtEpochMillis, 
                            alarmTriggerAtEpochMillis, alarmTriggerAtEpochMillis 
                            FROM shift_event_old""")
                        database.execSQL("DROP TABLE shift_event_old")

                        database.execSQL("ALTER TABLE alarm_mapping RENAME TO alarm_mapping_old")
                        database.execSQL("""CREATE TABLE alarm_mapping (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            planId INTEGER NOT NULL,
                            eventId INTEGER NOT NULL,
                            requestCode INTEGER NOT NULL,
                            alarmType TEXT NOT NULL,
                            workStartAtEpochMillis INTEGER NOT NULL,
                            triggerAtEpochMillis INTEGER NOT NULL,
                            createdAtEpochMillis INTEGER NOT NULL,
                            FOREIGN KEY(planId) REFERENCES shift_plan(id) ON DELETE CASCADE,
                            FOREIGN KEY(eventId) REFERENCES shift_event(id) ON DELETE CASCADE
                        )""")
                        database.execSQL("""INSERT INTO alarm_mapping 
                            (id, planId, eventId, requestCode, alarmType, workStartAtEpochMillis, 
                            triggerAtEpochMillis, createdAtEpochMillis)
                            SELECT id, planId, eventId, requestCode, 'SILENT', workStartAtEpochMillis, 
                            triggerAtEpochMillis, createdAtEpochMillis 
                            FROM alarm_mapping_old""")
                        database.execSQL("DROP TABLE alarm_mapping_old")
                    }
                },
                object : androidx.room.migration.Migration(3, 4) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("CREATE INDEX IF NOT EXISTS index_alarm_mapping_requestCodeAlarmType ON alarm_mapping(requestCode, alarmType)")
                    }
                },
                object : androidx.room.migration.Migration(4, 5) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE shift_plan RENAME TO shift_plan_old")
                        database.execSQL("""CREATE TABLE shift_plan (
                            id INTEGER PRIMARY KEY,
                            totalDays INTEGER NOT NULL,
                            firstWorkStartAtEpochMillis INTEGER NOT NULL,
                            silentRemindBeforeMinutes INTEGER NOT NULL DEFAULT 15,
                            departRemindBeforeMinutes INTEGER NOT NULL DEFAULT 30,
                            updatedAtEpochMillis INTEGER NOT NULL
                        )""")
                        database.execSQL("""INSERT INTO shift_plan 
                            (id, totalDays, firstWorkStartAtEpochMillis, silentRemindBeforeMinutes, departRemindBeforeMinutes, updatedAtEpochMillis)
                            SELECT id, totalDays, firstWorkStartAtEpochMillis, silentRemindBeforeMinutes, departRemindBeforeMinutes, updatedAtEpochMillis
                            FROM shift_plan_old""")
                        database.execSQL("DROP TABLE shift_plan_old")
                    }
                },
                object : androidx.room.migration.Migration(5, 6) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("""CREATE TABLE IF NOT EXISTS plan_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            totalDays INTEGER NOT NULL,
                            firstWorkStartAtEpochMillis INTEGER NOT NULL,
                            silentRemindBeforeMinutes INTEGER NOT NULL,
                            departRemindBeforeMinutes INTEGER NOT NULL,
                            updatedAtEpochMillis INTEGER NOT NULL,
                            savedAtEpochMillis INTEGER NOT NULL
                        )""")
                        database.execSQL("""CREATE TABLE IF NOT EXISTS plan_history_event (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            historyId INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            startAtEpochMillis INTEGER NOT NULL,
                            endAtEpochMillis INTEGER NOT NULL,
                            alarmTriggerAtEpochMillis INTEGER,
                            silentAlarmTriggerAtEpochMillis INTEGER,
                            departAlarmTriggerAtEpochMillis INTEGER,
                            FOREIGN KEY(historyId) REFERENCES plan_history(id) ON DELETE CASCADE
                        )""")
                        database.execSQL("CREATE INDEX IF NOT EXISTS index_plan_history_event_historyId ON plan_history_event(historyId)")
                    }
                }
            ).build()
        }
    }
}
