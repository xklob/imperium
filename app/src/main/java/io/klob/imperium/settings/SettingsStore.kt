package io.klob.imperium.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class ScrollBarSide(val key: String) {
    LEFT("left"),
    RIGHT("right");

    companion object {
        fun fromKey(key: String?): ScrollBarSide =
            entries.firstOrNull { it.key == key } ?: RIGHT
    }
}

data class ImperiumSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pointerSpeed: Float = 1.5f,
    val pointerAcceleration: Boolean = true,
    val scrollSpeed: Float = 1.0f,
    val naturalScroll: Boolean = false,
    val scrollBarSide: ScrollBarSide = ScrollBarSide.RIGHT,
    val scrollBarWidthDp: Int = SettingsStore.DEFAULT_SCROLL_WIDTH_DP,
    val hapticsEnabled: Boolean = true,
    val autoReconnect: Boolean = true,
    val lastHostAddress: String? = null,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ImperiumSettings> = _settings.asStateFlow()

    private fun load(): ImperiumSettings = ImperiumSettings(
        themeMode = ThemeMode.fromKey(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.key)),
        pointerSpeed = prefs.getFloat(KEY_POINTER_SPEED, 1.5f).coerceIn(MIN_POINTER_SPEED, MAX_POINTER_SPEED),
        pointerAcceleration = prefs.getBoolean(KEY_POINTER_ACCEL, true),
        scrollSpeed = prefs.getFloat(KEY_SCROLL_SPEED, 1.0f).coerceIn(MIN_SCROLL_SPEED, MAX_SCROLL_SPEED),
        naturalScroll = prefs.getBoolean(KEY_NATURAL_SCROLL, false),
        scrollBarSide = ScrollBarSide.fromKey(prefs.getString(KEY_SCROLL_SIDE, ScrollBarSide.RIGHT.key)),
        scrollBarWidthDp = prefs.getInt(KEY_SCROLL_WIDTH_DP, DEFAULT_SCROLL_WIDTH_DP)
            .coerceIn(MIN_SCROLL_WIDTH_DP, MAX_SCROLL_WIDTH_DP),
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
        lastHostAddress = prefs.getString(KEY_LAST_HOST, null),
    )

    fun update(transform: (ImperiumSettings) -> ImperiumSettings) {
        val next = transform(_settings.value).let {
            it.copy(
                pointerSpeed = it.pointerSpeed.coerceIn(MIN_POINTER_SPEED, MAX_POINTER_SPEED),
                scrollSpeed = it.scrollSpeed.coerceIn(MIN_SCROLL_SPEED, MAX_SCROLL_SPEED),
                scrollBarWidthDp = it.scrollBarWidthDp.coerceIn(MIN_SCROLL_WIDTH_DP, MAX_SCROLL_WIDTH_DP),
            )
        }
        _settings.value = next
        prefs.edit {
            putString(KEY_THEME, next.themeMode.key)
            putFloat(KEY_POINTER_SPEED, next.pointerSpeed)
            putBoolean(KEY_POINTER_ACCEL, next.pointerAcceleration)
            putFloat(KEY_SCROLL_SPEED, next.scrollSpeed)
            putBoolean(KEY_NATURAL_SCROLL, next.naturalScroll)
            putString(KEY_SCROLL_SIDE, next.scrollBarSide.key)
            putInt(KEY_SCROLL_WIDTH_DP, next.scrollBarWidthDp)
            putBoolean(KEY_HAPTICS, next.hapticsEnabled)
            putBoolean(KEY_AUTO_RECONNECT, next.autoReconnect)
            if (next.lastHostAddress != null) {
                putString(KEY_LAST_HOST, next.lastHostAddress)
            } else {
                remove(KEY_LAST_HOST)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "imperium_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_POINTER_SPEED = "pointer_speed"
        private const val KEY_POINTER_ACCEL = "pointer_accel"
        private const val KEY_SCROLL_SPEED = "scroll_speed"
        private const val KEY_NATURAL_SCROLL = "natural_scroll"
        private const val KEY_SCROLL_SIDE = "scroll_bar_side"
        private const val KEY_SCROLL_WIDTH_DP = "scroll_bar_width_dp"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_LAST_HOST = "last_host_address"

        const val MIN_POINTER_SPEED = 0.25f
        const val MAX_POINTER_SPEED = 4.0f
        const val MIN_SCROLL_SPEED = 0.25f
        const val MAX_SCROLL_SPEED = 3.0f
        const val MIN_SCROLL_WIDTH_DP = 44
        const val MAX_SCROLL_WIDTH_DP = 120
        const val DEFAULT_SCROLL_WIDTH_DP = 70
    }
}
