package com.example.worktime.domain.model

import java.time.LocalTime

data class SleepSchedule(
    val workStartHour: Int,
    val workStartMinute: Int,
    val workEndHour: Int,
    val workEndMinute: Int,
    val sleepSlots: List<SleepSlot>,
    val noSleepThisRound: Boolean = false
) {
    val totalSleepMinutes: Int
        get() = sleepSlots.sumOf { it.durationMinutes }

    val totalSleepHoursString: String
        get() {
            val hours = totalSleepMinutes / 60
            val minutes = totalSleepMinutes % 60
            return if (minutes > 0) {
                val decimal = (minutes * 10 / 60)
                "${hours}.$decimal"
            } else {
                "$hours"
            }
        }

    companion object {
        private const val WORK_DURATION_MINUTES = 2 * 60 // 工作2小时

        fun calculate(workStartHour: Int, workStartMinute: Int = 0): SleepSchedule? {
            val workStartMinutes = workStartHour * 60 + workStartMinute
            val workEndMinutes = workStartMinutes + WORK_DURATION_MINUTES
            val workEndHour = workEndMinutes / 60
            val workEndMinute = workEndMinutes % 60

            val sleepSlots = when (workStartMinutes) {
                toMinutes(12, 30) -> listOf(slot(20, 30, 22, 0))
                toMinutes(22, 30) -> listOf(slot(0, 45, 7, 45))
                toMinutes(8, 30) -> emptyList()
                toMinutes(18, 30) -> listOf(slot(21, 0, 4, 0))
                toMinutes(4, 30) -> listOf(slot(7, 0, 10, 0))
                toMinutes(14, 30) -> listOf(slot(22, 0, 23, 30))
                toMinutes(0, 30) -> listOf(slot(2, 45, 9, 45))
                toMinutes(10, 30) -> emptyList()
                toMinutes(20, 30) -> listOf(slot(22, 45, 5, 45))
                toMinutes(6, 30) -> listOf(slot(9, 0, 12, 0))
                toMinutes(16, 30) -> listOf(slot(19, 0, 2, 0))
                toMinutes(2, 30) -> listOf(slot(5, 0, 8, 0))
                else -> return null
            }

            return SleepSchedule(
                workStartHour = workStartHour,
                workStartMinute = workStartMinute,
                workEndHour = workEndHour,
                workEndMinute = workEndMinute,
                sleepSlots = sleepSlots,
                noSleepThisRound = sleepSlots.isEmpty()
            )
        }

        private fun toMinutes(hour: Int, minute: Int): Int = hour * 60 + minute

        private fun slot(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): SleepSlot {
            return SleepSlot(
                startTime = LocalTime.of(startHour, startMinute),
                endTime = LocalTime.of(endHour, endMinute),
                type = SleepType.MAIN,
                isPreviousDay = false
            )
        }
    }
}
