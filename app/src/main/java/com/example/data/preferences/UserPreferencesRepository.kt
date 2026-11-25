package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
