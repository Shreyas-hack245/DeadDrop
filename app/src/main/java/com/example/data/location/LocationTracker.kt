package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val isGpsLive: Boolean = false,
    /** Direction of travel in degrees from true north (GPS course), if the fix carries one. */
    val courseDegrees: Float? = null,
    /** Ground speed in metres per second, if known. */
    val speedMps: Float? = null
)

internal fun android.location.Location.toGeoPoint() = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
    isGpsLive = true,
    courseDegrees = if (hasBearing()) bearing else null,
    speedMps = if (hasSpeed()) speed else null
)

/** Result of asking Google Play services whether device location settings (GPS) are usable. */
sealed interface LocationSettingsStatus {
    /** Location services are on and satisfy a high-accuracy request. */
    data object Satisfied : LocationSettingsStatus

    /** Location services are off; launching [resolution] shows the system "Turn on location" dialog. */
    data class Resolvable(val resolution: PendingIntent) : LocationSettingsStatus

    /** The check failed in a way the user cannot fix from a dialog (e.g. Play services missing). */
    data object Unavailable : LocationSettingsStatus
}

class LocationTracker(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * One-shot position: first a fresh high-accuracy fix (up to [timeoutMs]), then, if that fails or
     * times out, the cached last known location. Returns null when permission is missing or neither works.
     */
    suspend fun getCurrentLocationOrNull(timeoutMs: Long = CURRENT_FIX_TIMEOUT_MS): GeoPoint? {
        if (!hasLocationPermission()) return null
        val fresh = withTimeoutOrNull(timeoutMs) { highAccuracyFixOrNull(timeoutMs) }
        return fresh ?: lastKnownOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun highAccuracyFixOrNull(timeoutMs: Long): GeoPoint? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(timeoutMs)
                    .build()
                fusedClient.getCurrentLocation(request, cts.token)
                    .addOnSuccessListener { loc ->
                        cont.resume(loc?.toGeoPoint())
                    }
                    .addOnFailureListener { cont.resume(null) }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownOrNull(): GeoPoint? =
        suspendCancellableCoroutine { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        cont.resume(loc?.toGeoPoint())
                    }
                    .addOnFailureListener { cont.resume(null) }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }

    /**
     * Asks Play services (SettingsClient + LocationSettingsRequest) whether device location is switched on.
     * When it is off the result carries the PendingIntent for the standard "Turn on location" dialog.
     */
    suspend fun checkLocationSettings(): LocationSettingsStatus =
        suspendCancellableCoroutine { cont ->
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3500L).build()
            val settingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .setAlwaysShow(true)
                .build()
            LocationServices.getSettingsClient(context.applicationContext)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener { cont.resume(LocationSettingsStatus.Satisfied) }
                .addOnFailureListener { error ->
                    cont.resume(
                        if (error is ResolvableApiException) {
                            LocationSettingsStatus.Resolvable(error.resolution)
                        } else {
                            LocationSettingsStatus.Unavailable
                        }
                    )
                }
        }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMs: Long = 3500L): Flow<GeoPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(2000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc.toGeoPoint())
            }
        }
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    companion object {
        private const val CURRENT_FIX_TIMEOUT_MS = 8_000L

        // Default fallback anchor (San Francisco Civic Center) when GPS is unavailable or permission not yet granted
        val DEFAULT_ANCHOR = GeoPoint(
            latitude = 37.7793,
            longitude = -122.4193,
            accuracyMeters = null,
            isGpsLive = false
        )

        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadiusMeters = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusMeters * c
        }

        fun formatDistance(meters: Double): String = when {
            meters < 1000.0 -> "${meters.toInt()}m"
            else -> String.format("%.1f km", meters / 1000.0)
        }
    }
}
