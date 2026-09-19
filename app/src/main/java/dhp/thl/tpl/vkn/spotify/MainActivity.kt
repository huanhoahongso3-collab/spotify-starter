package dhp.thl.tpl.vkn.spotify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        // Apply saved language if specified
        if (preferences.appLanguage.isNotEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(preferences.appLanguage))
        }

        // 1. If user hasn't completed initial setup, route directly to Settings
        if (!preferences.isSetupCompleted || !checkOverlayPermission()) {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
            finish()
            return
        }

        // 2. If Spotify is already running (has a media notification / is active),
        //    go straight to it so the user can continue controlling playback.
        if (isSpotifyRunning()) {
            redirectToSpotify()
            return
        }

        // 3. Otherwise: Auto-launch the web wrapper and overlay to choose a song
        startLaunchFlow()
    }

    private fun isSpotifyRunning(): Boolean {
        val spotifyPkg = preferences.spotifyPackage
        // Primary: check active MediaSessions — works for playing AND paused Spotify.
        // Requires notification access; falls back to isMusicActive if not granted.
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val listenerComponent = ComponentName(this, MediaNotificationListener::class.java)
            val sessions = msm?.getActiveSessions(listenerComponent)
            if (sessions != null) {
                return sessions.any { it.packageName == spotifyPkg }
            }
        } catch (_: SecurityException) {
            // Notification access not granted — fall through to audio check
        }
        // Fallback: audio is actively playing (catches playing-but-no-notification-access case)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.isMusicActive == true
    }

    private fun redirectToSpotify() {
        val spotifyPackage = preferences.spotifyPackage
        val launchIntent = packageManager.getLaunchIntentForPackage(spotifyPackage)

        if (launchIntent != null) {
            if (preferences.showToasts) Toast.makeText(
                this,
                getString(R.string.toast_music_playing_opening_spotify),
                Toast.LENGTH_SHORT
            ).show()
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        } else {
            // If Spotify isn't installed for some reason, fallback to web flow
            startLaunchFlow()
            return
        }
        finish()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun startLaunchFlow() {
        val wrapperPkg = preferences.webWrapperPackage
        val launchIntent = packageManager.getLaunchIntentForPackage(wrapperPkg)

        // 1. Start Floating Overlay Pill Service
        val overlayServiceIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_SHOW_OVERLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayServiceIntent)
        } else {
            startService(overlayServiceIntent)
        }

        // 2. Show Guidance Toast
        if (preferences.showToasts) Toast.makeText(this, getString(R.string.toast_choose_song), Toast.LENGTH_LONG).show()

        // 3. Launch Web Wrapper (e.g. SpotiDuck or fallback to web browser)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (preferences.excludeFromRecents) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(launchIntent)
        } else {
            if (preferences.showToasts) Toast.makeText(
                this,
                getString(R.string.toast_wrapper_not_found, wrapperPkg),
                Toast.LENGTH_SHORT
            ).show()
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (preferences.excludeFromRecents) {
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
            }
            try {
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Finish MainActivity so user lands directly in the web player with overlay
        finish()
    }
}
