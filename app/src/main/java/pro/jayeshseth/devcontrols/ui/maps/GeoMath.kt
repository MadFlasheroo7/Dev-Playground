package pro.jayeshseth.devcontrols.ui.maps

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Shared geodesy helpers (haversine distance, initial bearing, midpoint, DMS). */
object GeoMath {
    private const val R = 6371000.0 // earth radius m

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dp = Math.toRadians(lat2 - lat1); val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2); val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun midpoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Pair<Double, Double> {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2); val dl = Math.toRadians(lon2 - lon1)
        val bx = cos(p2) * cos(dl); val by = cos(p2) * sin(dl)
        val p3 = atan2(sin(p1) + sin(p2), sqrt((cos(p1) + bx) * (cos(p1) + bx) + by * by))
        val l3 = Math.toRadians(lon1) + atan2(by, cos(p1) + bx)
        return Math.toDegrees(p3) to ((Math.toDegrees(l3) + 540) % 360 - 180)
    }

    fun toDms(deg: Double, lat: Boolean): String {
        val hemi = if (lat) (if (deg >= 0) "N" else "S") else (if (deg >= 0) "E" else "W")
        val a = abs(deg); val d = a.toInt(); val m = ((a - d) * 60).toInt(); val s = ((a - d) * 60 - m) * 60
        return "%d°%02d'%05.2f\"%s".format(d, m, s, hemi)
    }

    fun fmtDist(m: Double): String = if (m < 1000) "%.0f m".format(m) else "%.2f km".format(m / 1000)
}
