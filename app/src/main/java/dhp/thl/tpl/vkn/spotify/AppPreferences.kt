package dhp.thl.tpl.vkn.spotify

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_IS_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SETUP_COMPLETED, value).apply()

    var webWrapperPackage: String
        get() = prefs.getString(KEY_WRAPPER_PACKAGE, DEFAULT_WRAPPER_PACKAGE) ?: DEFAULT_WRAPPER_PACKAGE
        set(value) = prefs.edit().putString(KEY_WRAPPER_PACKAGE, value.trim()).apply()

    var spotifyPackage: String
        get() = prefs.getString(KEY_SPOTIFY_PACKAGE, DEFAULT_SPOTIFY_PACKAGE) ?: DEFAULT_SPOTIFY_PACKAGE
        set(value) = prefs.edit().putString(KEY_SPOTIFY_PACKAGE, value.trim()).apply()

    var showToasts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOASTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TOASTS, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    companion object {
        private const val PREFS_NAME = "dhp_spotify_prefs"
        private const val KEY_IS_SETUP_COMPLETED = "key_is_setup_completed"
        private const val KEY_WRAPPER_PACKAGE = "key_wrapper_package"
        private const val KEY_SPOTIFY_PACKAGE = "key_spotify_package"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_SHOW_TOASTS = "key_show_toasts"

        const val DEFAULT_WRAPPER_PACKAGE = "com.spotiduck.music"
        const val DEFAULT_SPOTIFY_PACKAGE = "com.spotify.music"
        const val PRESET_CHROME_PACKAGE = "com.android.chrome"
    }
}
