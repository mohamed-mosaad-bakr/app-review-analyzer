package com.qadaa.app.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Why a day cannot be used for qadaa fasting. */
enum class BlockReason(val labelAr: String) {
    RAMADAN("رمضان"),
    EID_AL_FITR("عيد الفطر"),
    EID_AL_ADHA("عيد الأضحى وأيام التشريق"),
}

/**
 * Hijri dates come from java.time's Umm al-Qura calendar (computed, not sighted). Egypt's Dar
 * al-Ifta announces months by sighting, so the real date can differ by a day — estimated.
 */
object HijriRules {
    private fun hijri(date: LocalDate): Pair<Int, Int>? = runCatching {
        val h = HijrahDate.from(date)
        h.get(ChronoField.MONTH_OF_YEAR) to h.get(ChronoField.DAY_OF_MONTH)
    }.getOrNull()

    fun blockReason(date: LocalDate): BlockReason? {
        val (month, day) = hijri(date) ?: return null
        return when {
            month == 9 -> BlockReason.RAMADAN
            month == 10 && day == 1 -> BlockReason.EID_AL_FITR
            month == 12 && day in 10..13 -> BlockReason.EID_AL_ADHA
            else -> null
        }
    }

    /** First day of the next Ramadan strictly after [from]'s current Ramadan (if inside one). */
    fun nextRamadanStart(from: LocalDate): LocalDate? {
        var d = from
        var insideCurrent = hijri(d)?.first == 9
        repeat(400) {
            val h = hijri(d) ?: return null
            if (h.first == 9 && !insideCurrent) return d
            if (h.first != 9) insideCurrent = false
            d = d.plusDays(1)
        }
        return null
    }

    /** Default deadline: the day before the next (estimated) Ramadan. */
    fun defaultDeadline(today: LocalDate): LocalDate =
        nextRamadanStart(today)?.minusDays(1) ?: today.plusDays(100)
}
