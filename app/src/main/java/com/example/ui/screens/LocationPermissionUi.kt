package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.LocationPermissionStatus
import com.example.ui.LocationUiState
import com.example.ui.theme.AmberForge
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.ArcaneSurface
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Walks up ContextWrappers to find the hosting Activity (needed for shouldShowRequestPermissionRationale). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Opens this app's system settings page (used for "don't ask again" denials). */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Rationale shown after sign-in (and on later launches) while location permission is not granted.
 * When the permission is permanently denied the primary action becomes "Open settings".
 */
@Composable
fun LocationRationaleDialog(
    permanentlyDenied: Boolean,
    onEnable: () -> Unit,
    onNotNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        containerColor = ArcaneSurface,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = DiscoveryCyan) },
        title = {
            Text(
                text = "DeadDrop needs your location to show treasures near you",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = if (permanentlyDenied) {
                    "Location access was turned off for DeadDrop. Open settings and allow Location " +
                        "to see drops around you."
                } else {
                    "Your position is used to place starter drops around you, show how far each " +
                        "treasure is, and unlock drops when you get close."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onEnable,
                modifier = Modifier.testTag("location_enable_button")
            ) {
                Text(
                    text = if (permanentlyDenied) "Open settings" else "Enable location",
                    color = AmethystGlow
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.testTag("location_not_now_button")
            ) {
                Text("Not now", color = TextSecondary)
            }
        }
    )
}

/** Small pill shown while the first location fix is pending. */
@Composable
fun LocationLoadingChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("location_loading_chip"),
        shape = RoundedCornerShape(999.dp),
        color = MidnightIndigo.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ArcaneOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = DiscoveryCyan,
                strokeWidth = 2.dp
            )
            Text(
                text = "Finding your location…",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

/**
 * Explore-screen banner explaining why the map is not centred on the user, with a single action.
 * Returns without composing anything when nothing needs attention.
 */
@Composable
fun LocationStatusBanner(
    state: LocationUiState,
    onEnableLocation: () -> Unit,
    onTurnOnGps: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class BannerContent(val text: String, val action: String, val onAction: () -> Unit)

    val content: BannerContent? = when {
        state.permission == LocationPermissionStatus.UNKNOWN -> null
        !state.isPermissionGranted -> BannerContent(
            text = "Enable location to see treasures near you",
            action = if (state.permission == LocationPermissionStatus.PERMANENTLY_DENIED) "Settings" else "Enable",
            onAction = onEnableLocation
        )
        !state.gpsEnabled && !state.hasFix -> BannerContent(
            text = "Location services are off on this device",
            action = "Turn on",
            onAction = onTurnOnGps
        )
        !state.gpsEnabled -> BannerContent(
            text = "Location services are off — your position may be out of date",
            action = "Turn on",
            onAction = onTurnOnGps
        )
        state.timedOut && !state.hasFix -> BannerContent(
            text = "We couldn't find you yet. Try stepping outside or check your signal.",
            action = "Retry",
            onAction = onRetry
        )
        else -> null
    }
    if (content == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("location_status_banner"),
        shape = RoundedCornerShape(16.dp),
        color = MidnightIndigo.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, AmberForge.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOff,
                contentDescription = null,
                tint = AmberForge,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = content.text,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = content.onAction,
                modifier = Modifier.testTag("location_banner_action")
            ) {
                Text(content.action, color = DiscoveryCyan)
            }
        }
    }
}
