package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reelflow_player_prefs"
)

enum class AspectResizeOption(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
}

data class PlayerPreferences(
    val themeMode: ThemeMode = ThemeMode.CINEMA_DARK,
    val defaultSpeed: Float = 1.0f,
    val doubleTapSeekSeconds: Int = 10,
    val autoPlayNext: Boolean = true,
    val resumePlayback: Boolean = true,
    val defaultAspectMode: AspectResizeOption = AspectResizeOption.FIT,
)

class PlayerPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DOUBLE_TAP_SEEK_SEC = intPreferencesKey("double_tap_seek_sec")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DEFAULT_ASPECT_MODE = stringPreferencesKey("default_aspect_mode")
    }

    val preferences: Flow<PlayerPreferences> = context.playerPrefsDataStore.data.map { prefs ->
        PlayerPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.CINEMA_DARK,
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            doubleTapSeekSeconds = prefs[Keys.DOUBLE_TAP_SEEK_SEC] ?: 10,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
            defaultAspectMode = prefs[Keys.DEFAULT_ASPECT_MODE]?.let {
                runCatching { AspectResizeOption.valueOf(it) }.getOrNull()
            } ?: AspectResizeOption.FIT,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.playerPrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.playerPrefsDataStore.edit { it[Keys.DEFAULT_SPEED] = speed }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.playerPrefsDataStore.edit { it[Keys.DOUBLE_TAP_SEEK_SEC] = seconds }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.playerPrefsDataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.playerPrefsDataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setDefaultAspectMode(mode: AspectResizeOption) {
        context.playerPrefsDataStore.edit { it[Keys.DEFAULT_ASPECT_MODE] = mode.name }
    }
}
