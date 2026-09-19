package dhp.thl.tpl.vkn.spotify

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import dhp.thl.tpl.vkn.spotify.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: AppPreferences

    data class AppItem(val label: String, val packageName: String) {
        override fun toString(): String = "$label ($packageName)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        loadSettings()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateNotifAccessStatus()
    }

    private fun loadSettings() {
        binding.etWrapperPackage.setText(preferences.webWrapperPackage)
        binding.etSpotifyPackage.setText(preferences.spotifyPackage)
        binding.switchShowToasts.isChecked = preferences.showToasts
        binding.switchExcludeRecents.isChecked = preferences.excludeFromRecents
        updateLanguageButtonLabel()

        if (!preferences.isSetupCompleted) {
            binding.cardSetupNotice.visibility = View.VISIBLE
            binding.btnSaveSettings.text = getString(R.string.btn_save_setup)
        } else {
            binding.cardSetupNotice.visibility = View.GONE
            binding.btnSaveSettings.text = getString(R.string.btn_save_changes)
        }
    }

    private fun updatePermissionStatus() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        if (hasOverlay) {
            binding.tvPermStatus.text = getString(R.string.status_granted)
            binding.tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.spotify_green))
            binding.btnSettingsPermission.text = getString(R.string.btn_perm_granted)
        } else {
            binding.tvPermStatus.text = getString(R.string.status_required)
            binding.tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.spotify_grey))
            binding.btnSettingsPermission.text = getString(R.string.btn_grant_overlay_permission)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnPresetSpotiduck.setOnClickListener {
            binding.etWrapperPackage.setText(AppPreferences.DEFAULT_WRAPPER_PACKAGE)
        }

        binding.btnPresetChrome.setOnClickListener {
            binding.etWrapperPackage.setText(AppPreferences.PRESET_CHROME_PACKAGE)
        }

        binding.btnPickWrapperApp.setOnClickListener {
            showInstalledAppPicker { selectedPkg ->
                binding.etWrapperPackage.setText(selectedPkg)
            }
        }

        binding.btnResetSpotify.setOnClickListener {
            binding.etSpotifyPackage.setText(AppPreferences.DEFAULT_SPOTIFY_PACKAGE)
        }

        binding.btnPickSpotifyApp.setOnClickListener {
            showInstalledAppPicker { selectedPkg ->
                binding.etSpotifyPackage.setText(selectedPkg)
            }
        }

        binding.btnSettingsPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.switchShowToasts.isChecked = preferences.showToasts
        binding.switchShowToasts.setOnCheckedChangeListener { _, isChecked ->
            preferences.showToasts = isChecked
        }

        binding.switchExcludeRecents.isChecked = preferences.excludeFromRecents
        binding.switchExcludeRecents.setOnCheckedChangeListener { _, isChecked ->
            preferences.excludeFromRecents = isChecked
        }

        binding.btnSelectLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnSaveSettings.setOnClickListener {
            val wrapperPkg = binding.etWrapperPackage.text.toString().trim()
            val spotifyPkg = binding.etSpotifyPackage.text.toString().trim()

            if (wrapperPkg.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_enter_wrapper_pkg), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (spotifyPkg.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_enter_spotify_pkg), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            preferences.webWrapperPackage = wrapperPkg
            preferences.spotifyPackage = spotifyPkg
            preferences.showToasts = binding.switchShowToasts.isChecked
            preferences.excludeFromRecents = binding.switchExcludeRecents.isChecked
            preferences.isSetupCompleted = true

            Toast.makeText(
                this,
                getString(R.string.toast_setup_saved),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun showInstalledAppPicker(onSelected: (String) -> Unit) {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        val appList = resolveInfos.map {
            AppItem(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label.lowercase() }

        if (appList.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_apps_found), Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            appList
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_select_app))
            .setAdapter(adapter) { _, which ->
                val selected = appList[which]
                onSelected(selected.packageName)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun hasNotificationAccess(): Boolean {
        return try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val cn = ComponentName(this, MediaNotificationListener::class.java)
            msm?.getActiveSessions(cn) != null
        } catch (_: SecurityException) {
            false
        }
    }

    private fun updateNotifAccessStatus() {
        if (hasNotificationAccess()) {
            binding.tvNotifAccessStatus.text = getString(R.string.status_notif_granted)
            binding.tvNotifAccessStatus.setTextColor(ContextCompat.getColor(this, R.color.spotify_green))
            binding.btnNotifAccess.text = getString(R.string.btn_notif_access_granted)
        } else {
            binding.tvNotifAccessStatus.text = getString(R.string.status_notif_required)
            binding.tvNotifAccessStatus.setTextColor(ContextCompat.getColor(this, R.color.spotify_grey))
            binding.btnNotifAccess.text = getString(R.string.btn_grant_notif_access)
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun updateLanguageButtonLabel() {
        val label = when (preferences.appLanguage) {
            "en" -> "🇬🇧 " + getString(R.string.lang_english)
            "vi" -> "🇻🇳 " + getString(R.string.lang_vietnamese)
            else -> "🌐 " + getString(R.string.lang_system_default)
        }
        binding.btnSelectLanguage.text = label
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            "🌐 " + getString(R.string.lang_system_default),
            "🇬🇧 " + getString(R.string.lang_english),
            "🇻🇳 " + getString(R.string.lang_vietnamese)
        )
        val langTags = arrayOf("", "en", "vi")

        val currentTag = preferences.appLanguage
        val selectedIndex = when (currentTag) {
            "en" -> 1
            "vi" -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pref_language_title))
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val chosenTag = langTags[which]
                preferences.appLanguage = chosenTag

                // Apply per-app locale using AppCompatDelegate (supported on all Android versions)
                val appLocale = if (chosenTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(chosenTag)
                }
                AppCompatDelegate.setApplicationLocales(appLocale)

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
