package com.example.worktime.domain.model

data class DailyActivity(
    val name: String,
    val startMinute: Int,
    val endMinute: Int
) {
    companion object {
        private val ALL_ACTIVITIES = listOf(
            DailyActivity("qc", 390, 510),
            DailyActivity("xs", 390, 510),
            DailyActivity("tj", 390, 510),
            DailyActivity("cf", 390, 510),
            DailyActivity("cf", 630, 750),
            DailyActivity("cf", 990, 1110),
            DailyActivity("sj", 750, 870),
            DailyActivity("qc", 870, 990),
            DailyActivity("tj", 1110, 1230),
            DailyActivity("xs", 1230, 1350),
            DailyActivity("sj", 1350, 30)
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
