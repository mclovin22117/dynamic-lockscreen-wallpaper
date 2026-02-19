package com.example.dynamiclock.service

import android.app.WallpaperManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SpotifyNotificationListener : NotificationListenerService() {

    private var lastSong: String? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaCallback: MediaController.Callback? = null
    private var activeController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("DynamicLock", "Listener connected")
        setupMediaSessionListener()
    }

    private fun setupMediaSessionListener() {
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, SpotifyNotificationListener::class.java)

        mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
            Log.d("DynamicLock", "Active sessions changed: ${controllers?.size}")

            val spotifyStillActive = controllers?.any {
                it.packageName == "com.spotify.music"
            } == true

            if (!spotifyStillActive) {
                Log.d("DynamicLock", "Spotify session ended, resetting wallpaper")
                resetWallpaper()
            }

            attachToSpotifyController(controllers)
        }, componentName)

        val currentControllers = mediaSessionManager?.getActiveSessions(componentName)
        attachToSpotifyController(currentControllers)
    }

    private fun attachToSpotifyController(controllers: List<MediaController>?) {
        activeController?.let { controller ->
            mediaCallback?.let { cb -> controller.unregisterCallback(cb) }
        }
        activeController = null

        val spotifyController = controllers?.find {
            it.packageName == "com.spotify.music"
        }

        if (spotifyController == null) {
            Log.d("DynamicLock", "No Spotify media session found")
            return
        }

        Log.d("DynamicLock", "Attached to Spotify media session")
        activeController = spotifyController

        mediaCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                metadata?.let { processMetadata(it) }
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                Log.d("DynamicLock", "Spotify session destroyed, resetting wallpaper")
                resetWallpaper()
                activeController = null
                lastSong = null
            }
        }

        activeController?.registerCallback(mediaCallback!!)
        spotifyController.metadata?.let { processMetadata(it) }
    }

    private fun processMetadata(metadata: MediaMetadata, retryCount: Int = 0) {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val currentSong = "$title-$artist"

        if (currentSong == lastSong) return
        lastSong = currentSong

        Log.d("DynamicLock", "Song: $title - $artist")

        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        if (bitmap != null) {
            Log.d("DynamicLock", "Album art found: ${bitmap.width}x${bitmap.height}")
            setLockWallpaper(bitmap)
        } else if (retryCount < 5) {
            Log.w("DynamicLock", "No album art yet, retrying in 1s (attempt ${retryCount + 1}/5)")
            handler.postDelayed({
                lastSong = null
                activeController?.metadata?.let {
                    processMetadata(it, retryCount + 1)
                }
            }, 1000)
        } else {
            Log.e("DynamicLock", "Album art not available after 5 retries")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.spotify.music") return
        Log.d("DynamicLock", "Spotify notification removed")

        // Check if any spotify session is still active
        val componentName = ComponentName(this, SpotifyNotificationListener::class.java)
        val controllers = mediaSessionManager?.getActiveSessions(componentName)
        val spotifyStillActive = controllers?.any {
            it.packageName == "com.spotify.music"
        } == true

        if (!spotifyStillActive) {
            Log.d("DynamicLock", "Spotify fully closed, resetting wallpaper")
            resetWallpaper()
            lastSong = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.spotify.music") return
        val componentName = ComponentName(this, SpotifyNotificationListener::class.java)
        val controllers = mediaSessionManager?.getActiveSessions(componentName)
        attachToSpotifyController(controllers)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacksAndMessages(null)
        activeController?.let {
            mediaCallback?.let { cb -> it.unregisterCallback(cb) }
        }
    }

    private fun resetWallpaper() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)

            // Create a solid black bitmap same size as screen
            val displayMetrics = resources.displayMetrics
            val blackBitmap = Bitmap.createBitmap(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            // Fill with black
            blackBitmap.eraseColor(android.graphics.Color.BLACK)

            wallpaperManager.setBitmap(
                blackBitmap,
                null,
                true,
                WallpaperManager.FLAG_LOCK
            )

            // Now clear it — this time it replaces cache with black first
            wallpaperManager.clear(WallpaperManager.FLAG_LOCK)

            Log.d("DynamicLock", "Lock screen wallpaper reset successfully")
        } catch (e: Exception) {
            Log.e("DynamicLock", "Failed to reset wallpaper: ${e.message}")
        }
    }

    private fun setLockWallpaper(bitmap: Bitmap) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val displayMetrics = resources.displayMetrics

            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                true
            )

            val result = wallpaperManager.setBitmap(
                scaledBitmap,
                null,
                true,
                WallpaperManager.FLAG_LOCK
            )

            if (result > 0) {
                Log.d("DynamicLock", "Lock screen wallpaper updated successfully")
            } else {
                Log.w("DynamicLock", "Lock screen wallpaper set failed, result: $result")
            }

        } catch (e: Exception) {
            Log.e("DynamicLock", "Wallpaper failed: ${e.message}")
        }
    }
}