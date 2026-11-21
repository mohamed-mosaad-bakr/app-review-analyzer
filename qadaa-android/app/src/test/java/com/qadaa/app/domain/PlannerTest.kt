package com.qadaa.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class PlannerTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val yearEnd = LocalDate.of(2026, 12, 31)

    @Test fun chosenDaysAreTheShortestAvailable() {
        val plan = Planner.plan(start, yearEnd, 16, emptySet(), emptySet())
        assertEquals(16, plan.chosen.size)
        val maxChosen = plan.chosen.maxOf { it.minutes }
        val chosenDates = plan.chosen.map { it.date }.toSet()
        val others = plan.ranked.filter { it.date !in chosenDates }
        assertTrue("every chosen day must be <= every other day", others.all { it.minutes >= maxChosen })
        // Shortest fasts in Egypt are around the winter solstice.
        assertTrue(plan.chosen.all { it.date.monthValue == 12 || it.date.monthValue == 1 })
    }

    @Test fun rankedIsSortedByDurationThenDate() {
        val ranked = Planner.plan(start, yearEnd, 0, emptySet(), emptySet()).ranked
        ranked.zipWithNext().forEach { (a, b) ->
            assertTrue(a.minutes < b.minutes || (a.minutes == b.minutes && a.date < b.date))
        }
    }

    @Test fun chosenShownInCalendarOrderAndBackupsFollowRanking() {
        val plan = Planner.plan(start, yearEnd, 10, emptySet(), emptySet(), backupCount = 5)
        assertEquals(plan.chosen.sortedBy { it.date }, plan.chosen)
        assertEquals(plan.ranked.drop(10).take(5), plan.backups)
    }

    @Test fun completedAndUnavailableDaysAreSkipped() {
        val first = Planner.plan(start, yearEnd, 3, emptySet(), emptySet()).chosen.map { it.date }
        val plan = Planner.plan(start, yearEnd, 3, setOf(first[0]), setOf(first[1]))
        val dates = plan.ranked.map { it.date }
        assertTrue(first[0] !in dates && first[1] !in dates)
        assertEquals(3, plan.chosen.size)
    }

    @Test fun ramadanAndEidDaysAreNeverChosen() {
        val plan = Planner.plan(start, yearEnd, 400, emptySet(), emptySet())
        assertTrue(plan.ranked.none { HijriRules.blockReason(it.date) != null })
        // 2026: Ramadan ~ Feb 18 – Mar 19 (Umm al-Qura), Eid al-Fitr ~ Mar 20.
        val dates = plan.ranked.map { it.date }.toSet()
        assertTrue(LocalDate.of(2026, 3, 1) !in dates)
        // Ramadan (29 or 30) + Eid al-Fitr (1) + Eid al-Adha & Tashriq (4)
        assertTrue(plan.blockedByCalendar.values.sum() in 34..35)
        assertEquals(1, plan.blockedByCalendar[BlockReason.EID_AL_FITR])
        assertEquals(4, plan.blockedByCalendar[BlockReason.EID_AL_ADHA])
        assertTrue(plan.shortage > 0)
    }

    @Test fun windowAndCountEdgeCases() {
        assertTrue(Planner.plan(start, start.minusDays(1), 5, emptySet(), emptySet()).ranked.isEmpty())
        val small = Planner.plan(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), 5, emptySet(), emptySet())
        assertEquals(3, small.chosen.size); assertEquals(2, small.shortage)
        assertTrue(Planner.plan(start, yearEnd, 0, emptySet(), emptySet()).chosen.isEmpty())
    }

    @Test fun defaultDeadlineIsDayBeforeNextRamadan() {
        val d = HijriRules.defaultDeadline(LocalDate.of(2026, 11, 21))
        assertEquals(null, HijriRules.blockReason(d))
        assertEquals(BlockReason.RAMADAN, HijriRules.blockReason(d.plusDays(1)))
        assertTrue(ChronoUnit.DAYS.between(LocalDate.of(2026, 11, 21), d) in 60..120)
    }
}

class PrayerCalcTest {
    private fun near(expected: String, actual: LocalTime, tol: Int = 5) {
        val e = LocalTime.parse(expected)
        val diff = abs(ChronoUnit.MINUTES.between(e, actual))
        assertTrue("expected ~$expected got $actual", diff <= tol)
    }

    // Reference values: Egyptian General Authority of Survey timetable for Cairo (rounded).
    @Test fun cairoWinter() {
        val t = PrayerCalc.times(LocalDate.of(2025, 12, 21), Cities.byId("cairo"))!!
        near("05:13", t.fajr); near("17:00", t.maghrib)
    }

    @Test fun cairoSummerUsesDaylightSaving() {
        val t = PrayerCalc.times(LocalDate.of(2025, 6, 21), Cities.byId("cairo"))!!
        near("04:08", t.fajr); near("19:59", t.maghrib)
    }

    @Test fun durationMatchesTimes() {
        val t = PrayerCalc.times(LocalDate.of(2026, 3, 1), Cities.default)!!
        val diff = ChronoUnit.MINUTES.between(t.fajr, t.maghrib).toInt()
        assertTrue(abs(diff - t.fastingMinutes) <= 1)
    }

    @Test fun unknownCityFallsBackToDamietta() {
        assertEquals("damietta", Cities.byId("nope").id)
        assertNull(Cities.all.firstOrNull { it.id == "nope" })
    }
}
