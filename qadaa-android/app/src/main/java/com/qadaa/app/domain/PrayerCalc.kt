package com.qadaa.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A location used for the astronomical calculation. */
data class City(val id: String, val nameAr: String, val lat: Double, val lon: Double, val zone: String = "Africa/Cairo")

object Cities {
    val all = listOf(
        City("damietta", "دمياط", 31.4165, 31.8133),
        City("cairo", "القاهرة", 30.0444, 31.2357),
        City("alexandria", "الإسكندرية", 31.2001, 29.9187),
        City("mansoura", "المنصورة", 31.0409, 31.3785),
        City("tanta", "طنطا", 30.7865, 31.0004),
        City("portsaid", "بورسعيد", 31.2653, 32.3019),
        City("ismailia", "الإسماعيلية", 30.5965, 32.2715),
        City("suez", "السويس", 29.9668, 32.5498),
        City("minya", "المنيا", 28.1099, 30.7503),
        City("asyut", "أسيوط", 27.1809, 31.1837),
        City("sohag", "سوهاج", 26.5591, 31.6957),
        City("luxor", "الأقصر", 25.6872, 32.6396),
        City("aswan", "أسوان", 24.0889, 32.8998),
    )
    val default = all.first()
    fun byId(id: String?): City = all.firstOrNull { it.id == id } ?: default
}

/** Estimated Fajr and Maghrib for one day; [fastingMinutes] is Fajr → Maghrib. */
data class DayTimes(val date: LocalDate, val fajr: LocalTime, val maghrib: LocalTime, val fastingMinutes: Int)

/**
 * ESTIMATED prayer times from a solar-position model (NOAA equations), using the method of the
 * Egyptian General Authority of Survey: Fajr when the sun is 19.5° below the horizon, Maghrib at
 * sunset (−0.833°, refraction + solar radius). The UTC offset comes from the device time-zone
 * database for the date, so Egypt's summer time (UTC+3) is handled automatically.
 *
 * These are NOT official timetable values: elevation, local precautionary minutes (ihtiyat) and
 * the model's ~1–2 minute error are not accounted for. The UI labels them as estimates.
 */
object PrayerCalc {
    const val FAJR_ANGLE = 19.5
    private const val SUNSET_ALTITUDE = -0.833

    /** Returns minutes after local midnight, or null if the sun never reaches that altitude. */
    private fun eventLocalMinutes(date: LocalDate, city: City, altitude: Double, morning: Boolean): Double? {
        val offsetMin = ZoneId.of(city.zone).rules.getOffset(date.atTime(12, 0)).totalSeconds / 60.0
        val latR = Math.toRadians(city.lat)
        var utcMin = if (morning) 240.0 else 960.0 // first guess, refined below
        repeat(3) {
            val gamma = 2.0 * Math.PI / 365.0 * (date.dayOfYear - 1 + (utcMin / 60.0 - 12.0) / 24.0)
            val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
            val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
            val c = (sin(Math.toRadians(altitude)) - sin(latR) * sin(decl)) / (cos(latR) * cos(decl))
            if (c < -1.0 || c > 1.0) return null
            val haDeg = Math.toDegrees(acos(c))
            utcMin = 720.0 - 4.0 * (city.lon + if (morning) haDeg else -haDeg) - eqTime
        }
        return utcMin + offsetMin
    }

    private fun toLocalTime(minutes: Double): LocalTime {
        val m = ((minutes.roundToInt() % 1440) + 1440) % 1440
        return LocalTime.of(m / 60, m % 60)
    }

    fun times(date: LocalDate, city: City = Cities.default): DayTimes? {
        val fajr = eventLocalMinutes(date, city, -FAJR_ANGLE, morning = true) ?: return null
        val maghrib = eventLocalMinutes(date, city, SUNSET_ALTITUDE, morning = false) ?: return null
        return DayTimes(date, toLocalTime(fajr), toLocalTime(maghrib), (maghrib - fajr).roundToInt())
    }
}
