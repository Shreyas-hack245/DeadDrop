package com.example.domain.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class LatLon(val latitude: Double, val longitude: Double)

/**
 * Small, dependency-free geodesy helpers (pure Kotlin, unit-testable on the JVM).
 */
object GeoMath {

    /**
     * Earth radius used by [com.example.data.location.LocationTracker.distanceMeters] (haversine).
     * Using the same radius here means a drop placed N metres away is also *displayed* as N metres away.
     */
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Metres covered by one degree of latitude (constant on a sphere). */
    private const val METERS_PER_DEGREE_LAT = EARTH_RADIUS_METERS * PI / 180.0

    /**
     * Returns the point that lies [distanceMeters] away from ([latitude], [longitude]) in the direction
     * [bearingDegrees] (0 = north, 90 = east, clockwise).
     *
     * Metres are converted to degrees with the local scale: one degree of longitude is shorter than one
     * degree of latitude by cos(latitude), so the east/west component is divided by that factor.
     * Accurate to well under 0.5 % for the few-kilometre distances used for starter drops.
     */
    fun offset(
        latitude: Double,
        longitude: Double,
        distanceMeters: Double,
        bearingDegrees: Double
    ): LatLon {
        val bearing = Math.toRadians(bearingDegrees)
        val northMeters = distanceMeters * cos(bearing)
        val eastMeters = distanceMeters * sin(bearing)

        val metersPerDegreeLon = (METERS_PER_DEGREE_LAT * cos(Math.toRadians(latitude)))
            .coerceAtLeast(1.0) // avoid division by ~0 at the poles

        val newLat = (latitude + northMeters / METERS_PER_DEGREE_LAT).coerceIn(-90.0, 90.0)
        val newLon = normalizeLongitude(longitude + eastMeters / metersPerDegreeLon)
        return LatLon(newLat, newLon)
    }

    /** Initial compass bearing (0..360, 0 = north, clockwise) from point 1 to point 2. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
    }

    /** "N", "NE", "E"… for a bearing in degrees. */
    fun compassLabel(degrees: Double): String {
        val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[(((degrees % 360.0) + 360.0) % 360.0 / 45.0 + 0.5).toInt() % 8]
    }

    /** Smallest signed angle (-180..180) to turn from heading [from] to bearing [to]; positive = turn right. */
    fun relativeAngle(from: Double, to: Double): Double = ((to - from + 540.0) % 360.0) - 180.0

    /** Wraps a longitude into [-180, 180). */
    fun normalizeLongitude(longitude: Double): Double {
        var lon = (longitude + 180.0) % 360.0
        if (lon < 0) lon += 360.0
        return lon - 180.0
    }
}
