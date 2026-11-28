package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.StarterPlacement
import com.example.domain.model.StarterWorldSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.deadDropDataStore: DataStore<Preferences> by preferencesDataStore(name = "deaddrop_prefs")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val showStarterDrops: Boolean = true,
    val preferredMapLayer: String = "ARCANE_DARK"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_STARTER_DROPS = booleanPreferencesKey("show_starter_drops")
        val PREFERRED_MAP_LAYER = stringPreferencesKey("preferred_map_layer")
    }

    val preferencesFlow: Flow<UserPreferences> = context.deadDropDataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            showStarterDrops = prefs[Keys.SHOW_STARTER_DROPS] ?: true,
            preferredMapLayer = prefs[Keys.PREFERRED_MAP_LAYER] ?: "ARCANE_DARK"
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.deadDropDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.deadDropDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setShowStarterDrops(show: Boolean) {
        context.deadDropDataStore.edit { it[Keys.SHOW_STARTER_DROPS] = show }
    }

    suspend fun setPreferredMapLayer(layer: String) {
        context.deadDropDataStore.edit { it[Keys.PREFERRED_MAP_LAYER] = layer }
    }

    // --- Per-user starter world (coordinates of the starter drops + the base location used) ---

    private fun starterWorldKey(userId: String) = stringPreferencesKey("starter_world_v1_$userId")

    /** Returns the saved starter world for [userId], or null if none exists or it cannot be read. */
    suspend fun loadStarterWorld(userId: String): StarterWorldSnapshot? {
        val raw: String? = try {
            context.deadDropDataStore.data.first()[starterWorldKey(userId)]
        } catch (_: IOException) {
            null
        }
        if (raw == null) return null
        return decodeStarterWorld(raw)
    }

    suspend fun saveStarterWorld(userId: String, snapshot: StarterWorldSnapshot) {
        context.deadDropDataStore.edit { it[starterWorldKey(userId)] = encodeStarterWorld(snapshot) }
    }

    suspend fun clearStarterWorld(userId: String) {
        context.deadDropDataStore.edit { it.remove(starterWorldKey(userId)) }
    }

    private fun encodeStarterWorld(snapshot: StarterWorldSnapshot): String = JSONObject().apply {
        put("version", snapshot.version)
        put("baseLat", snapshot.baseLatitude)
        put("baseLon", snapshot.baseLongitude)
        put("seed", snapshot.seed)
        put("generatedAt", snapshot.generatedAtMillis)
        put(
            "drops",
            JSONArray().apply {
                snapshot.placements.forEach { p ->
                    put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("lat", p.latitude)
                            put("lon", p.longitude)
                        }
                    )
                }
            }
        )
    }.toString()

    private fun decodeStarterWorld(raw: String): StarterWorldSnapshot? = try {
        val json = JSONObject(raw)
        val drops = json.getJSONArray("drops")
        val placements = (0 until drops.length()).map { i ->
            val d = drops.getJSONObject(i)
            StarterPlacement(d.getString("id"), d.getDouble("lat"), d.getDouble("lon"))
        }
        StarterWorldSnapshot(
            baseLatitude = json.getDouble("baseLat"),
            baseLongitude = json.getDouble("baseLon"),
            seed = json.getLong("seed"),
            generatedAtMillis = json.getLong("generatedAt"),
            placements = placements,
            version = json.getInt("version")
        )
    } catch (_: Exception) {
        null // corrupt/old payload: treated as "no saved world" and regenerated
    }
}
