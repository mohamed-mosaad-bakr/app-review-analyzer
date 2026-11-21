package com.qadaa.app.domain

import java.time.LocalDate

data class DayPlan(val times: DayTimes) {
    val date: LocalDate get() = times.date
    val minutes: Int get() = times.fastingMinutes
}

data class PlanResult(
    /** Every usable day in the window, shortest fast first (ties: earlier date first). */
    val ranked: List<DayPlan>,
    /** The [needed] shortest days, shown in calendar order. */
    val chosen: List<DayPlan>,
    val backups: List<DayPlan>,
    val needed: Int,
    val blockedByCalendar: Map<BlockReason, Int>,
) {
    val shortage: Int get() = (needed - chosen.size).coerceAtLeast(0)
}

object Planner {
    fun plan(
        start: LocalDate,
        deadline: LocalDate,
        needed: Int,
        completed: Set<LocalDate>,
        unavailable: Set<LocalDate>,
        city: City = Cities.default,
        backupCount: Int = 8,
    ): PlanResult {
        val blocked = mutableMapOf<BlockReason, Int>()
        val all = mutableListOf<DayPlan>()
        var d = start
        while (!d.isAfter(deadline)) {
            val reason = HijriRules.blockReason(d)
            when {
                reason != null -> blocked[reason] = (blocked[reason] ?: 0) + 1
                d in completed || d in unavailable -> Unit
                else -> PrayerCalc.times(d, city)?.takeIf { it.fastingMinutes > 0 }?.let { all += DayPlan(it) }
            }
            d = d.plusDays(1)
        }
        val ranked = all.sortedWith(compareBy<DayPlan> { it.minutes }.thenBy { it.date })
        val n = needed.coerceAtLeast(0)
        return PlanResult(
            ranked = ranked,
            chosen = ranked.take(n).sortedBy { it.date },
            backups = ranked.drop(n).take(backupCount),
            needed = n,
            blockedByCalendar = blocked,
        )
    }
}
