# Spotify Launcher

An Android application designed to bridge web wrappers (like [SpotiDuck](https://github.com/23fpsz/SpotiDuck-Releases) or browser wrappers) with the official Spotify Android app (`com.spotify.music`).

---

## 💡 The Concept & Why It Works

On mobile devices, free Spotify accounts enforce forced shuffle mode and restrict selecting individual songs on demand. However, the Spotify Web Player (`open.spotify.com`) is treated like a desktop environment, where on-demand track selection is unrestricted.

**SpotiSync** pairs the two seamlessly:
1. **Launches SpotiDuck / Web Wrapper**: Displays the desktop-capable web interface where you can browse and choose any track.
2. **Displays a Floating Overlay Pill**: Spawns a lightweight draggable overlay (`SYSTEM_ALERT_WINDOW`) above the screen and shows a guidance toast: *"Pick your song in the web wrapper, then tap the overlay!"*.
3. **One-Tap Handoff**: Once your song is selected, tapping the floating pill immediately opens the official Spotify app (`com.spotify.music`) to take over playback via **Spotify Connect**, giving you native audio quality, lock-screen controls, and system volume integration.

---

## ✨ Features

- **Draggable Floating Overlay**: A clean Spotify-green floating pill that sits above other apps. You can drag it anywhere on your screen or dismiss it with the `✕` button.
- **Configurable Web Wrapper**: Default is set to SpotiDuck (`com.fps23.spotiduck`), but you can configure it to Chrome, Brave, or any custom browser / web wrapper package.
- **App Icon Long-Press Shortcuts**:
  - 🎵 **Launch & Sync**: Directly starts the overlay and launches the web player.
  - ⚙️ **Settings**: Opens the configuration screen.
- **System Settings Integration**: Hooked into Android's `APPLICATION_PREFERENCES` intent. You can access settings directly via:
  `Settings` ➔ `Apps` ➔ `SpotiSync` ➔ `Additional settings in the app / More app settings`.
- **Automatic Fallback**: If the configured wrapper app is not installed, it automatically opens `https://open.spotify.com` in your default web browser.

---

## ⚠️ Required Setup (Do This First)

For the handoff to work correctly, both apps need a one-time configuration:

### 1. Enable Spotify Connect
Open the **official Spotify app** → tap **Home** → tap your profile icon → **Settings** → **Devices** → enable **"Spotify Connect"**. This is what allows the web player to hand off playback to the native app.

### 2. Grant Nearby Devices / Bluetooth Scanning Permission
Spotify Connect uses local network/Bluetooth discovery. Both apps need this permission:
- **Spotify**: Android **Settings** → **Apps** → **Spotify** → **Permissions** → enable **Nearby devices**.
- **SpotiDuck / Web Wrapper**: Android **Settings** → **Apps** → **SpotiDuck** (or your wrapper) → **Permissions** → enable **Nearby devices**.

Without this, Spotify Connect cannot discover the web player session and the handoff will silently fail.

### 3. Prevent SpotiDuck from Being Killed on Swipe
By default, swiping SpotiDuck out of the recent apps list will kill it mid-session. To prevent this:
- Android **Settings** → **Apps** → **SpotiDuck** (or your wrapper) → **Battery** → set to **"Unrestricted"** (or equivalent on your device, e.g. "No restrictions").
- On some OEM devices (Xiaomi, Samsung, etc.) there is an additional **"Allow background activity"** or **"Auto-launch"** toggle — enable it.

This ensures SpotiDuck stays alive while you're browsing for a song, even if you briefly switch apps.

---

## 📱 How It Works (First-Run vs Daily Use)

1. **First Time Opening SpotiSync**:
   - The app automatically detects that setup is incomplete and immediately opens **Settings**.
   - Use the built-in **"Pick App 📱"** button to select your installed web wrapper (e.g., SpotiDuck or Chrome) and confirm the official Spotify app ID (`com.spotify.music`).
   - Tap **"Grant Overlay Permission"** so the floating sync pill can be drawn over the web player.
   - Tap **"Save & Complete Setup"**.

2. **Daily Use (Auto Mode)**:
   - Next time you tap the SpotiSync app icon, **it immediately does its job with zero extra taps**:
     1. Spawns the floating Spotify sync pill.
     2. Shows the guidance toast (*"Pick your song in the web wrapper, then tap the overlay!"*).
     3. Launches SpotiDuck (or your configured wrapper).
   - Once you select your song in SpotiDuck, tap the floating pill to switch to the official Spotify app and sync playback!

3. **Re-configuring Anytime**:
   - **App Shortcut**: Long-press the SpotiSync app icon on your home screen and select **"Settings"**.
   - **System Settings**: Go to Android **Settings** ➔ **Apps** ➔ **SpotiSync** ➔ **Additional settings in the app**.

---

## 🛠️ Project Structure

```
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/spotistarter/app/
│       │   ├── MainActivity.kt           # Main launcher, permission checks & status
│       │   ├── SettingsActivity.kt       # Configures target packages
│       │   ├── FloatingOverlayService.kt # Foreground service managing draggable overlay
│       │   └── AppPreferences.kt         # Persistent preferences storage
│       └── res/
│           ├── drawable/                 # Spotify icons, close icons, pill backgrounds
│           ├── layout/                   # Activity and floating overlay UI layouts
│           ├── mipmap-anydpi-v26/        # Adaptive launcher icons
│           ├── values/                   # Strings, Spotify dark-mode palette & themes
│           └── xml/                      # App shortcuts (shortcuts.xml)
├── gradle/libs.versions.toml             # Gradle version catalog
├── build.gradle.kts                      # Root build configuration
└── settings.gradle.kts                   # Project settings
```

---

## 🚀 Building the Project

### With Android Studio
Open this project folder (`spotify-starter`) directly in Android Studio. Gradle will sync and you can run it on your device or build an APK via:
`Build` ➔ `Build Bundle(s) / APK(s)` ➔ `Build APK(s)`.

### With Gradle CLI
```bash
./gradlew assembleDebug
```
The output APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
