package com.example.worktime.domain.model

data class DailyActivity(
    val name: String,
    val startMinute: Int,
    val endMinute: Int
) {
    companion object {
        private val ALL_ACTIVITIES = listOf(
            DailyActivity("qc", 360, 480),
            DailyActivity("tj", 360, 480),
            DailyActivity("cf", 360, 480),
            DailyActivity("cf", 600, 720),
            DailyActivity("sj", 720, 840),
            DailyActivity("qc", 840, 960),
            DailyActivity("cf", 960, 1080),
            DailyActivity("tj", 1080, 1200),
            DailyActivity("xs", 1320, 1440),
            DailyActivity("sj", 1320, 1440)
        )

        fun getActivitiesForWork(startHour: Int, startMinute: Int, workDurationMinutes: Int = 120): List<String> {
            val workStart = startHour * 60 + startMinute
            val workEnd = workStart + workDurationMinutes

            return ALL_ACTIVITIES
                .filter { activity ->
                    if (activity.endMinute <= activity.startMinute) {
                        val actEnd = activity.endMinute + 1440
                        val wEnd = if (workEnd <= workStart) workEnd + 1440 else workEnd
                        activity.startMinute == workStart && actEnd == wEnd
                    } else {
                        activity.startMinute == workStart && activity.endMinute == workEnd
                    }
                }
                .map { it.name }
                .distinct()
        }
    }
}
