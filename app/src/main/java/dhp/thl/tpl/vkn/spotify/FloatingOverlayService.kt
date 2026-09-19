package dhp.thl.tpl.vkn.spotify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var appPreferences: AppPreferences

    companion object {
        const val ACTION_SHOW_OVERLAY = "dhp.thl.tpl.vkn.spotify.ACTION_SHOW_OVERLAY"
        const val ACTION_STOP_OVERLAY = "dhp.thl.tpl.vkn.spotify.ACTION_STOP_OVERLAY"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dhp_spotify_overlay_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            setupOverlayView()
        }

        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_floating_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        isMoving = true
                    }

                    if (isMoving) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        // User tapped without dragging
                        val closeButton = overlayView?.findViewById<View>(R.id.btn_close_overlay)
                        if (isPointInsideView(closeButton, event.rawX, event.rawY)) {
                            stopSelf()
                        } else {
                            triggerSpotifySync()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(overlayView, params)
    }

    private fun isPointInsideView(view: View?, rawX: Float, rawY: Float): Boolean {
        if (view == null) return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return rawX >= x && rawX <= (x + view.width) &&
               rawY >= y && rawY <= (y + view.height)
    }

    private fun triggerSpotifySync() {
        val spotifyPackage = appPreferences.spotifyPackage
        val launchIntent = packageManager.getLaunchIntentForPackage(spotifyPackage)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
            if (appPreferences.showToasts) Toast.makeText(this, getString(R.string.toast_syncing), Toast.LENGTH_SHORT).show()
        } else {
            if (appPreferences.showToasts) Toast.makeText(this, getString(R.string.toast_spotify_not_found), Toast.LENGTH_LONG).show()
        }

        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Overlay Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows overlay status while selecting songs in web player"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pStopIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_desc))
            .setSmallIcon(R.drawable.ic_spotify)
            .addAction(R.drawable.ic_close, getString(R.string.overlay_dismiss), pStopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}
