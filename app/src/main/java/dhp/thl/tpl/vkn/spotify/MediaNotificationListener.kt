package dhp.thl.tpl.vkn.spotify

import android.service.notification.NotificationListenerService

// Stub — exists only so MediaSessionManager.getActiveSessions() can be called
// with our ComponentName to detect active media sessions (e.g. Spotify paused).
// Does not intercept or read any notification content.
class MediaNotificationListener : NotificationListenerService()
