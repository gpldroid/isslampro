package com.isslampro.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.*

/** Kotlin port of the prayer-calc.js astronomy used by the web edition. */
object PrayerCalculator {
    data class Method(val title: String, val fajr: Double, val isha: Double? = null,
                      val ishaMinutes: Int? = null, val adjustments: Map<String, Int> = emptyMap())

    data class Times(val fajr: Instant, val sunrise: Instant, val dhuhr: Instant,
                     val asr: Instant, val maghrib: Instant, val isha: Instant)

    val methods = linkedMapOf(
        "egypt" to Method("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
        "mwl" to Method("رابطة العالم الإسلامي", 18.0, 17.0, adjustments = mapOf("dhuhr" to 1)),
        "makkah" to Method("أم القرى", 18.5, ishaMinutes = 90),
        "karachi" to Method("جامعة العلوم الإسلامية - كراتشي", 18.0, 18.0, adjustments = mapOf("dhuhr" to 1)),
        "dubai" to Method("الإمارات - دبي", 18.2, 18.2, adjustments = mapOf("sunrise" to -3, "dhuhr" to 3, "asr" to 3, "maghrib" to 3)),
        "kuwait" to Method("الكويت", 18.0, 17.5),
        "qatar" to Method("قطر", 18.0, ishaMinutes = 90),
        "algeria" to Method("الجزائر", 18.0, 17.0, adjustments = mapOf("maghrib" to 3)),
        "morocco" to Method("وزارة الأوقاف والشؤون الإسلامية - المغرب", 19.0, 17.0, adjustments = mapOf("maghrib" to 5)),
        "tunisia" to Method("تونس", 18.0, 18.0),
        "turkey" to Method("تركيا - ديانت", 18.0, 17.0, adjustments = mapOf("sunrise" to -7, "dhuhr" to 5, "asr" to 4, "maghrib" to 7)),
        "isna" to Method("أمريكا الشمالية - ISNA", 15.0, 15.0)
    )

    private fun sin(d: Double) = sin(Math.toRadians(d))
    private fun cos(d: Double) = cos(Math.toRadians(d))
    private fun tan(d: Double) = tan(Math.toRadians(d))
    private fun asin(x: Double) = Math.toDegrees(asin(x))
    private fun acos(x: Double) = Math.toDegrees(acos(x))
    private fun atan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun fix(a: Double, b: Double): Double = ((a % b) + b) % b
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun acot(x: Double) = Math.toDegrees(atan(1.0 / x))

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class Sun(val decl: Double, val eqt: Double)
    private fun sunPosition(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g) + 0.02 * sin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2(cos(e) * sin(l), cos(l)) / 15.0
        return Sun(asin(sin(e) * sin(l)), q / 15.0 - fixHour(ra))
    }

    fun calculate(date: LocalDate, latitude: Double, longitude: Double,
                  methodKey: String = "morocco", asrFactor: Double = 1.0, elevation: Double = 0.0): Times {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val method = methods[methodKey] ?: methods.getValue("morocco")
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val riseAngle = 0.833 + 0.0347 * sqrt(max(0.0, elevation))
        fun midday(t: Double) = fixHour(12.0 - sunPosition(jd + t).eqt)
        fun angleTime(angle: Double, t: Double, morning: Boolean = false): Double {
            val decl = sunPosition(jd + t).decl
            val noon = midday(t)
            val arg = (-sin(angle) - sin(decl) * sin(latitude)) / (cos(decl) * cos(latitude))
            if (arg !in -1.0..1.0) return Double.NaN
            val hourAngle = acos(arg) / 15.0
            return noon + if (morning) -hourAngle else hourAngle
        }
        fun asrTime(factor: Double, t: Double): Double {
            val decl = sunPosition(jd + t).decl
            return angleTime(-acot(factor + tan(abs(latitude - decl))), t)
        }
        var t = mapOf("fajr" to 5.0, "sunrise" to 6.0, "dhuhr" to 12.0, "asr" to 13.0, "sunset" to 18.0, "isha" to 18.0)
        repeat(2) {
            val p = t.mapValues { it.value / 24.0 }
            t = mapOf(
                "fajr" to angleTime(method.fajr, p.getValue("fajr"), true),
                "sunrise" to angleTime(riseAngle, p.getValue("sunrise"), true),
                "dhuhr" to midday(p.getValue("dhuhr")),
                "asr" to asrTime(asrFactor, p.getValue("asr")),
                "sunset" to angleTime(riseAngle, p.getValue("sunset")),
                "isha" to (method.isha?.let { angleTime(it, p.getValue("isha")) } ?: 18.0)
            )
        }
        val sunrise = t.getValue("sunrise")
        val sunset = t.getValue("sunset")
        val night = 24.0 - sunset + sunrise
        var fajr = t.getValue("fajr")
        var isha = t.getValue("isha")
        if (!fajr.isFinite() || sunrise - fajr > method.fajr / 60.0 * night) fajr = sunrise - method.fajr / 60.0 * night
        if (method.isha != null && (!isha.isFinite() || isha - sunset > method.isha / 60.0 * night)) isha = sunset + method.isha / 60.0 * night
        val values = mapOf(
            "fajr" to fajr, "sunrise" to sunrise, "dhuhr" to t.getValue("dhuhr"),
            "asr" to t.getValue("asr"), "maghrib" to sunset,
            "isha" to (method.ishaMinutes?.let { sunset + it / 60.0 } ?: isha)
        )
        fun instant(key: String): Instant {
            val utcHours = values.getValue(key) - longitude / 15.0 + (method.adjustments[key] ?: 0) / 60.0
            val millis = date.toEpochDay() * 86_400_000L + (utcHours * 3_600_000.0).roundToLong()
            return Instant.ofEpochMilli(millis / 60_000L * 60_000L)
        }
        return Times(instant("fajr"), instant("sunrise"), instant("dhuhr"), instant("asr"), instant("maghrib"), instant("isha"))
    }

    fun format(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant)
}
