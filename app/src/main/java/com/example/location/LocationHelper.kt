package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.domain.model.CityLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    fun hasFineLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentCity(context: Context): CityLocation? {
        if (!hasLocationPermission(context)) return null
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = if (hasFineLocationPermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val fusedLocation: Location? = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                fusedClient.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (cont.isActive) cont.resume(loc)
                        } else {
                            fusedClient.lastLocation
                                .addOnSuccessListener { last ->
                                    if (cont.isActive) cont.resume(last)
                                }
                                .addOnFailureListener {
                                    if (cont.isActive) cont.resume(null)
                                }
                        }
                    }
                    .addOnFailureListener {
                        fusedClient.lastLocation
                            .addOnSuccessListener { last ->
                                if (cont.isActive) cont.resume(last)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(null)
                            }
                    }
            }
        }

        val location = fusedLocation ?: readBestLocationManagerFallback(context) ?: return null

        val lat = location.latitude
        val lon = location.longitude
        val tzId = TimeZone.getDefault().id

        val (cityAr, countryAr) = reverseGeocode(context, lat, lon, Locale("ar"))
        val (cityEn, countryEn) = reverseGeocode(context, lat, lon, Locale.ENGLISH)

        return CityLocation(
            id = "gps_${String.format(Locale.US, "%.4f_%.4f", lat, lon)}",
            nameAr = cityAr ?: cityEn ?: "موقعي الحالي",
            nameEn = cityEn ?: cityAr ?: "Current Location",
            countryAr = countryAr ?: countryEn ?: "",
            countryEn = countryEn ?: countryAr ?: "",
            latitude = lat,
            longitude = lon,
            timezoneId = tzId
        )
    }

    @SuppressLint("MissingPermission")
    private fun readBestLocationManagerFallback(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            val candidate = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || candidate.accuracy < best.accuracy || candidate.time > best.time + 120_000L) {
                best = candidate
            }
        }
        return best
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        lon: Double,
        locale: Locale
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, locale)
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            val country = addr?.countryName
            city to country
        } catch (_: Exception) {
            null to null
        }
    }
}
