package com.example.dynamiclock.service

import android.app.Notification
import android.app.WallpaperManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SpotifyNotificationListener : NotificationListenerService() {

    @Volatile private var lastSong: String? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaCallback: MediaController.Callback? = null
    private var activeController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private var originalWallpaper: Bitmap? = null
    private var wallpaperChanged = false
    private var renderScript: RenderScript? = null
    @Volatile private var wallpaperGeneration = 0  // increments each song change

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("DynamicLock", "Listener connected")
        renderScript = RenderScript.create(this)  // create once, reuse
        saveOriginalWallpaper()
        setupMediaSessionListener()
    }

    private fun saveOriginalWallpaper() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this) ?: return
            val homeDrawable = wallpaperManager.drawable
            if (homeDrawable is android.graphics.drawable.BitmapDrawable) {
                val bmp = homeDrawable.bitmap
                if (bmp != null) {
                    originalWallpaper = bmp
                    Log.d("DynamicLock", "Original wallpaper saved successfully")
                    return
                }
            }
            Log.w("DynamicLock", "No original wallpaper found")
        } catch (e: Exception) {
            Log.e("DynamicLock", "saveOriginalWallpaper failed: ${e.message}")
        }
    }

    private fun setupMediaSessionListener() {
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, SpotifyNotificationListener::class.java)

        mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
            Log.d("DynamicLock", "Active sessions changed: ${controllers?.size}")
            // Debounce — only process the last call in a burst of rapid calls
            handler.removeCallbacksAndMessages("attach")
            handler.postAtTime({
                attachToSpotifyController(controllers)
            }, "attach", android.os.SystemClock.uptimeMillis() + 300)
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
            if (wallpaperChanged) {
                Log.d("DynamicLock", "Resetting wallpaper — Spotify gone")
                resetWallpaper()
            }
            return
        }

        val playbackState = spotifyController.playbackState?.state
        Log.d("DynamicLock", "Spotify found, playback state: $playbackState")

        if (playbackState != PlaybackState.STATE_PLAYING) {
            Log.d("DynamicLock", "Spotify is not playing (state: $playbackState), ignoring ghost session")
            return
        }

        Log.d("DynamicLock", "Attached to Spotify media session")
        activeController = spotifyController

        mediaCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                metadata?.let { processMetadata(it) }
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                super.onPlaybackStateChanged(state)
                if (state == null) return

                when (state.state) {
                    PlaybackState.STATE_PLAYING -> {
                        Log.d("DynamicLock", "Spotify is playing")
                        activeController?.metadata?.let { processMetadata(it) }
                    }
                    PlaybackState.STATE_STOPPED,
                    PlaybackState.STATE_NONE -> {
                        Log.d("DynamicLock", "Spotify stopped, resetting wallpaper")
                        if (wallpaperChanged) resetWallpaper()
                        lastSong = null
                    }
                    PlaybackState.STATE_PAUSED -> {
                        Log.d("DynamicLock", "Spotify paused, keeping wallpaper")
                    }
                    else -> {}
                }
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                Log.d("DynamicLock", "Spotify session destroyed")
                if (wallpaperChanged) resetWallpaper()
                activeController = null
                lastSong = null
            }
        }

        activeController?.registerCallback(mediaCallback!!)
        spotifyController.metadata?.let { processMetadata(it) }
    }

    private fun getHighResAlbumArt(title: String?): Bitmap? {
        try {
            val activeNotifications = activeNotifications ?: return null
            for (sbn in activeNotifications) {
                if (sbn.packageName != "com.spotify.music") continue
                val notification = sbn.notification ?: continue
                val extras = notification.extras ?: continue

                val largeIcon = notification.getLargeIcon()
                if (largeIcon != null) {
                    val bitmap = largeIcon.loadDrawable(this)
                    if (bitmap is android.graphics.drawable.BitmapDrawable) {
                        val bmp = bitmap.bitmap
                        if (bmp != null && bmp.width >= 300) {
                            Log.d("DynamicLock", "Got high res art from notification icon: ${bmp.width}x${bmp.height}")
                            return bmp
                        }
                    }
                }

                val picture = extras.get(Notification.EXTRA_PICTURE)
                if (picture is Bitmap) {
                    Log.d("DynamicLock", "Got high res art from notification picture: ${picture.width}x${picture.height}")
                    return picture
                }
            }
        } catch (e: Exception) {
            Log.e("DynamicLock", "getHighResAlbumArt failed: ${e.message}")
        }
        return null
    }

    private fun processMetadata(metadata: MediaMetadata) {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val currentSong = "$title-$artist"

        if (currentSong == lastSong) return
        lastSong = currentSong
        wallpaperGeneration++
        val myGeneration = wallpaperGeneration

        // Cancel any pending work for previous song
        handler.removeCallbacksAndMessages(null)

        Log.d("DynamicLock", "Song detected: $title - $artist (generation $myGeneration)")

        // Wait 600ms for Spotify to fully update album art before reading it
        // Spotify sends title first, then art in a second metadata update
        handler.postDelayed({
            if (wallpaperGeneration != myGeneration) {
                Log.d("DynamicLock", "Generation $myGeneration skipped — newer song arrived")
                return@postDelayed
            }
            fetchArtAndSetWallpaper(myGeneration, attempt = 0)
        }, 600)
    }

    private fun fetchArtAndSetWallpaper(generation: Int, attempt: Int) {
        if (wallpaperGeneration != generation) return

        // Always read from the live controller — never stale passed-in metadata
        val freshMetadata = activeController?.metadata
        if (freshMetadata == null) {
            Log.w("DynamicLock", "No fresh metadata available")
            return
        }

        // Try notification art first (can be higher res)
        val highResBitmap = getHighResAlbumArt(null)
        if (highResBitmap != null) {
            Log.d("DynamicLock", "Using notification art: ${highResBitmap.width}x${highResBitmap.height}")
            setLockWallpaper(highResBitmap, generation)
            return
        }

        // Try metadata art
        val bitmap = freshMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: freshMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        if (bitmap != null) {
            Log.d("DynamicLock", "Using metadata art: ${bitmap.width}x${bitmap.height}")
            setLockWallpaper(bitmap, generation)
        } else if (attempt < 5) {
            Log.w("DynamicLock", "Art not ready, retry ${attempt + 1}/5 in 500ms")
            handler.postDelayed({
                fetchArtAndSetWallpaper(generation, attempt + 1)
            }, 500)
        } else {
            Log.e("DynamicLock", "Art unavailable after 5 retries")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.spotify.music") return
        Log.d("DynamicLock", "Spotify notification removed from bar")
        if (wallpaperChanged) {
            Log.d("DynamicLock", "Spotify notification cleared, resetting wallpaper")
            resetWallpaper()
            lastSong = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.spotify.music") return
        // Debounce — only trigger re-attach once per burst of notifications
        handler.removeCallbacksAndMessages("notify")
        handler.postAtTime({
            val componentName = ComponentName(this, SpotifyNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            // Only re-attach if we don't already have an active controller
            if (activeController == null) {
                Log.d("DynamicLock", "No active controller, re-attaching from notification")
                attachToSpotifyController(controllers)
            }
        }, "notify", android.os.SystemClock.uptimeMillis() + 500)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacksAndMessages(null)
        renderScript?.destroy()
        renderScript = null
        activeController?.let {
            mediaCallback?.let { cb -> it.unregisterCallback(cb) }
        }
    }

    // Blur using reused RenderScript instance
    private fun blurBitmap(source: Bitmap, radius: Float = 25f): Bitmap {
        // Aggressively scale down — we work at 1/8 size for blur background
        val scaledWidth = (source.width / 8).coerceAtLeast(1)
        val scaledHeight = (source.height / 8).coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, false)
        val blurredBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)

        try {
            val rs = renderScript ?: RenderScript.create(this)
            val input = Allocation.createFromBitmap(rs, blurredBitmap)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(radius.coerceIn(1f, 25f))
            script.setInput(input)
            script.forEach(output)
            output.copyTo(blurredBitmap)
            input.destroy()
            output.destroy()
            script.destroy()
            Log.d("DynamicLock", "Blur applied")
        } catch (e: Exception) {
            Log.e("DynamicLock", "Blur failed: ${e.message}")
        }

        return blurredBitmap
    }

    // Compose at HALF screen resolution — Android scales wallpaper anyway
    private fun composeWallpaper(albumArt: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap {
        // Work at half resolution — massive performance gain
        val targetWidth = screenWidth / 2
        val targetHeight = screenHeight / 2

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        // Background: blur then scale to half screen
        val blurred = blurBitmap(albumArt)
        val bgBitmap = Bitmap.createScaledBitmap(blurred, targetWidth, targetHeight, true)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw blurred background
        canvas.drawBitmap(bgBitmap, 0f, 0f, paint)

        // Dark overlay for depth
        paint.color = android.graphics.Color.argb(120, 0, 0, 0)
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), paint)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true

        // Album art: 80% of width, centered
        val artSize = (targetWidth * 0.80f).toInt()
        val left = (targetWidth - artSize) / 2f
        val top = (targetHeight - artSize) / 2f

        // Draw shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            setShadowLayer(16f, 0f, 4f, android.graphics.Color.argb(160, 0, 0, 0))
        }
        canvas.drawRoundRect(RectF(left, top, left + artSize, top + artSize), 24f, 24f, shadowPaint)

        // Draw album art with rounded corners directly — no intermediate bitmap
        val srcRect = Rect(0, 0, albumArt.width, albumArt.height)
        val dstRectF = RectF(left, top, left + artSize, top + artSize)
        canvas.save()
        canvas.clipRect(dstRectF)
        canvas.drawBitmap(albumArt, srcRect, dstRectF, paint)
        canvas.restore()

        Log.d("DynamicLock", "Wallpaper composed at ${targetWidth}x${targetHeight}")
        return output
    }

    private fun setLockWallpaper(bitmap: Bitmap, generation: Int) {
        // Run heavy image processing on a background thread
        Thread {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)

                val wallpaperManager = WallpaperManager.getInstance(this)
                val displayMetrics = resources.displayMetrics

                // Compose wallpaper (heavy work happens here)
                val finalBitmap = composeWallpaper(
                    bitmap,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )

                // CRITICAL: Only apply if we are still the latest song
                if (wallpaperGeneration != generation) {
                    Log.d("DynamicLock", "Generation $generation is stale (current: $wallpaperGeneration), skipping")
                    return@Thread
                }

                val result = wallpaperManager.setBitmap(
                    finalBitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )

                if (result > 0) {
                    wallpaperChanged = true
                    Log.d("DynamicLock", "Lock screen wallpaper updated successfully (generation $generation)")
                } else {
                    Log.w("DynamicLock", "Lock screen wallpaper set failed, result: $result")
                }

            } catch (e: Exception) {
                Log.e("DynamicLock", "Wallpaper failed: ${e.message}")
            }
        }.start()
    }

    private fun resetWallpaper() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)

            if (originalWallpaper != null) {
                wallpaperManager.setBitmap(
                    originalWallpaper!!,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )
                Log.d("DynamicLock", "Original wallpaper restored successfully")
            } else {
                wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
                Log.d("DynamicLock", "No original wallpaper, cleared lock screen")
            }

            wallpaperChanged = false
            lastSong = null

        } catch (e: Exception) {
            Log.e("DynamicLock", "Failed to reset wallpaper: ${e.message}")
        }
    }
}