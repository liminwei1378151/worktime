package com.example.worktime.domain.scheduler

import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import java.time.Duration
import kotlin.math.min

class ShiftScheduler {

    fun generate(plan: ShiftPlan): List<ShiftEvent> {
        val error = plan.validate()
        require(error == null) { error ?: "排班方案参数不合法" }

        val result = mutableListOf<ShiftEvent>()
        val cutoff = plan.calculateEndAtEpochMillis()
        val workDurationMillis = Duration.ofMinutes(plan.workDurationMinutes.toLong()).toMillis()
        val restDurationMillis = Duration.ofMinutes(plan.restDurationMinutes.toLong()).toMillis()

        var cursor = plan.firstWorkStartAtEpochMillis
        var currentType = ShiftEventType.WORK

        while (cursor < cutoff) {
            val segmentDuration = if (currentType == ShiftEventType.WORK) {
                workDurationMillis
            } else {
                restDurationMillis
            }
            val end = min(cursor + segmentDuration, cutoff)

            result += ShiftEvent(
                planId = plan.id,
                type = currentType,
                startAtEpochMillis = cursor,
                endAtEpochMillis = end
            )

            cursor = end
            currentType = if (currentType == ShiftEventType.WORK) {
                ShiftEventType.REST
            } else {
                ShiftEventType.WORK
            }
        }

        return result
    }
}
