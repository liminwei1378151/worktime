package com.example.worktime.domain.planner

import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class MealType(val label: String, val baseTime: LocalTime) {
    BREAKFAST("早餐", LocalTime.of(8, 0)),
    LUNCH("午饭", LocalTime.of(12, 0)),
    DINNER("晚饭", LocalTime.of(18, 0))
}

data class MealPlan(
    val type: MealType,
    val scheduledAtEpochMillis: Long,
    val note: String
)

data class SupplementPlan(
    val name: String,
    val scheduledAtEpochMillis: Long,
    val note: String
)

data class SleepPlan(
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val note: String
)

data class DailyRoutinePlan(
    val meals: List<MealPlan>,
    val supplements: List<SupplementPlan>,
    val sleeps: List<SleepPlan>
)

object DailyRoutinePlanner {
    private val workSupplementOffset: Duration = Duration.ofMinutes(25)
    private val workWakeBuffer: Duration = Duration.ofMinutes(90)
    private val postWorkCooldown: Duration = Duration.ofMinutes(30)
    private val mealDuration: Duration = Duration.ofMinutes(45)
    private val minimumSleep: Duration = Duration.ofMinutes(120)
    private val mealSupplementOffset: Duration = Duration.ofMinutes(10)
    private val mealMedicineOffset: Duration = Duration.ofMinutes(15)
    private val afternoonSnackTime: LocalTime = LocalTime.of(15, 30)

    fun planForDate(
        date: LocalDate,
        events: List<ShiftEvent>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyRoutinePlan {
        val workEvents = events.filter { it.type == ShiftEventType.WORK }.sortedBy { it.startAtEpochMillis }
        val restEvents = events.filter { it.type == ShiftEventType.REST }.sortedBy { it.startAtEpochMillis }
        val meals = mealsForDate(date, workEvents, zoneId)
        val supplements = supplementsForDate(date, meals, workEvents, zoneId)
        val sleeps = sleepsForDate(date, meals, workEvents, restEvents, zoneId)
        return DailyRoutinePlan(meals = meals, supplements = supplements, sleeps = sleeps)
    }

    private fun mealsForDate(
        date: LocalDate,
        workEvents: List<ShiftEvent>,
        zoneId: ZoneId
    ): List<MealPlan> {
        return MealType.entries.map { mealType ->
            val adjusted = workEvents.any { event ->
                val start = event.startAtEpochMillis.toLocalDateTime(zoneId)
                start.toLocalDate() == date && start.toLocalTime() == mealType.baseTime
            }
            val scheduledTime = if (adjusted) mealType.baseTime.minusMinutes(40) else mealType.baseTime
            val note = if (adjusted) "因上班提前40分钟" else "按固定时间"
            MealPlan(
                type = mealType,
                scheduledAtEpochMillis = LocalDateTime.of(date, scheduledTime).toEpochMillis(zoneId),
                note = note
            )
        }
    }

    private fun supplementsForDate(
        date: LocalDate,
        meals: List<MealPlan>,
        workEvents: List<ShiftEvent>,
        zoneId: ZoneId
    ): List<SupplementPlan> {
        val supplements = mutableListOf<SupplementPlan>()
        val breakfast = meals.first { it.type == MealType.BREAKFAST }
        val lunch = meals.first { it.type == MealType.LUNCH }
        val dinner = meals.first { it.type == MealType.DINNER }

        supplements += SupplementPlan("纯牛奶", breakfast.scheduledAtEpochMillis, "早餐时一起吃")
        supplements += SupplementPlan("坚果", breakfast.scheduledAtEpochMillis, "早餐时一起吃")
        supplements += SupplementPlan("复合VB", breakfast.scheduledAtEpochMillis + mealSupplementOffset.toMillis(), "早餐后补充")
        supplements += SupplementPlan("VD", breakfast.scheduledAtEpochMillis + mealSupplementOffset.toMillis(), "早餐后配坚果/牛奶")
        supplements += SupplementPlan("护肝片", lunch.scheduledAtEpochMillis + mealMedicineOffset.toMillis(), "午饭后")
        supplements += SupplementPlan("VC", dinner.scheduledAtEpochMillis + mealMedicineOffset.toMillis(), "晚饭后")

        val workRelatedSupplements = workEvents.flatMap { workEvent ->
            val plannedAt = workEvent.startAtEpochMillis - workSupplementOffset.toMillis()
            listOf(
                SupplementPlan("党参生脉饮", plannedAt, "上班前25分钟"),
                SupplementPlan("红枣", plannedAt, "上班前垫一口")
            )
        }.filter { it.scheduledAtEpochMillis.toLocalDate(zoneId) == date }

        if (workRelatedSupplements.isEmpty()) {
            supplements += SupplementPlan(
                name = "红枣",
                scheduledAtEpochMillis = LocalDateTime.of(date, afternoonSnackTime).toEpochMillis(zoneId),
                note = "下午加餐"
            )
        } else {
            supplements += workRelatedSupplements
        }

        return supplements.sortedBy { it.scheduledAtEpochMillis }
    }

    private fun sleepsForDate(
        date: LocalDate,
        meals: List<MealPlan>,
        workEvents: List<ShiftEvent>,
        restEvents: List<ShiftEvent>,
        zoneId: ZoneId
    ): List<SleepPlan> {
        val mealBlocks = buildMealBlocks(date.minusDays(1), date.plusDays(1), workEvents, zoneId)
        return restEvents.mapNotNull { restEvent ->
            val restStart = restEvent.startAtEpochMillis + postWorkCooldown.toMillis()
            val nextWork = workEvents.firstOrNull { it.startAtEpochMillis >= restEvent.endAtEpochMillis }
            val wakeDeadline = minOf(
                restEvent.endAtEpochMillis,
                nextWork?.startAtEpochMillis?.minus(workWakeBuffer.toMillis()) ?: restEvent.endAtEpochMillis
            )

            if (wakeDeadline - restStart < minimumSleep.toMillis()) {
                return@mapNotNull null
            }

            val segments = subtractBlocks(
                windowStart = restStart,
                windowEnd = wakeDeadline,
                blocks = mealBlocks
            )
            val longest = segments.maxByOrNull { it.second - it.first } ?: return@mapNotNull null
            if (longest.second - longest.first < minimumSleep.toMillis()) {
                return@mapNotNull null
            }

            val note = if (longest.second - longest.first >= Duration.ofHours(5).toMillis()) {
                "主睡眠，尽量睡够"
            } else {
                "补觉，优先恢复精神"
            }

            SleepPlan(
                startAtEpochMillis = longest.first,
                endAtEpochMillis = longest.second,
                note = note
            )
        }.filter { sleep ->
            val sleepStartDate = sleep.startAtEpochMillis.toLocalDate(zoneId)
            val sleepEndDate = sleep.endAtEpochMillis.toLocalDate(zoneId)
            sleepStartDate == date || sleepEndDate == date
        }.sortedBy { it.startAtEpochMillis }
    }

    private fun buildMealBlocks(
        startDate: LocalDate,
        endDate: LocalDate,
        workEvents: List<ShiftEvent>,
        zoneId: ZoneId
    ): List<Pair<Long, Long>> {
        val blocks = mutableListOf<Pair<Long, Long>>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            mealsForDate(cursor, workEvents, zoneId).forEach { meal ->
                blocks += meal.scheduledAtEpochMillis to (meal.scheduledAtEpochMillis + mealDuration.toMillis())
            }
            cursor = cursor.plusDays(1)
        }
        return blocks.sortedBy { it.first }
    }

    private fun subtractBlocks(
        windowStart: Long,
        windowEnd: Long,
        blocks: List<Pair<Long, Long>>
    ): List<Pair<Long, Long>> {
        var segments = listOf(windowStart to windowEnd)
        blocks.forEach { (blockStart, blockEnd) ->
            segments = segments.flatMap { (segmentStart, segmentEnd) ->
                if (blockEnd <= segmentStart || blockStart >= segmentEnd) {
                    listOf(segmentStart to segmentEnd)
                } else {
                    buildList {
                        if (blockStart > segmentStart) add(segmentStart to blockStart)
                        if (blockEnd < segmentEnd) add(blockEnd to segmentEnd)
                    }
                }
            }
        }
        return segments.filter { it.second > it.first }
    }

    private fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }

    private fun LocalDateTime.toEpochMillis(zoneId: ZoneId): Long {
        return atZone(zoneId).toInstant().toEpochMilli()
    }
}
