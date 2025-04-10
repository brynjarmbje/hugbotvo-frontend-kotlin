package com.mytestwork2.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class GameAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "GameAudioManager"
        private const val NORMAL_VOLUME = 0.7f
        private const val DUCKING_VOLUME = 0.5f
        private const val QUESTION_VOLUME = 0.9f
    }

    // Audio players
    private var backgroundPlayer: MediaPlayer? = null
    private var questionPlayer: MediaPlayer? = null

    // Background music resource ID
    private var backgroundMusicResId: Int = 0

    // Audio focus management
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = AtomicBoolean(false)

    // Audio cache
    private val audioCache = mutableMapOf<Long, String>()

    // State tracking
    private var isBackgroundMusicInitialized = false
    private var isBackgroundMusicPlaying = false

    // Audio focus listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus LOSS - pausing background music")
                pauseBackgroundMusic()
                hasAudioFocus.set(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT - pausing background music")
                pauseBackgroundMusic()
                hasAudioFocus.set(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT_CAN_DUCK - ducking background music")
                duckBackgroundMusic()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus GAIN - restoring background music")
                restoreBackgroundMusic()
                hasAudioFocus.set(true)
            }
        }
    }

    /**
     * Initialize and start background music
     */
    fun initBackgroundMusic(resourceId: Int) {
        Log.d(TAG, "Initializing background music with resource ID: $resourceId")
        backgroundMusicResId = resourceId

        // Create the background player if it doesn't exist
        if (backgroundPlayer == null) {
            try {
                backgroundPlayer = MediaPlayer.create(context, resourceId)
                if (backgroundPlayer != null) {
                    backgroundPlayer?.apply {
                        isLooping = true
                        setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
                        setOnErrorListener { _, what, extra ->
                            Log.e(TAG, "Background player error: what=$what, extra=$extra")
                            isBackgroundMusicPlaying = false
                            isBackgroundMusicInitialized = false
                            // Schedule a retry
                            Handler(Looper.getMainLooper()).postDelayed({
                                initBackgroundMusic(resourceId)
                            }, 1000)
                            true
                        }
                        setOnCompletionListener {
                            isBackgroundMusicPlaying = false
                            // Should not happen with looping enabled, but just in case
                            if (isBackgroundMusicInitialized) {
                                start()
                                isBackgroundMusicPlaying = true
                            }
                        }

                        // Start playing
                        start()
                        isBackgroundMusicPlaying = true
                        isBackgroundMusicInitialized = true
                        Log.d(TAG, "Background music started successfully")
                    }

                    // Request audio focus for background playback
                    requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN)
                } else {
                    Log.e(TAG, "Failed to create background music player")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing background music: ${e.message}", e)
            }
        } else {
            // Player already exists, just start it if not playing
            if (!isBackgroundMusicPlaying) {
                try {
                    backgroundPlayer?.start()
                    isBackgroundMusicPlaying = true
                    Log.d(TAG, "Resumed existing background music")
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming background music: ${e.message}", e)
                    // If there was an error, try to recreate the player
                    releaseBackgroundPlayer()
                    initBackgroundMusic(resourceId)
                }
            }
        }
    }

    /**
     * Play question audio from a URL
     */
    fun playQuestionAudio(audioUrl: String, onCompletion: () -> Unit = {}) {
        Log.d(TAG, "Playing question audio from URL: $audioUrl")

        // Make sure background music is initialized
        if (!isBackgroundMusicInitialized && backgroundMusicResId != 0) {
            initBackgroundMusic(backgroundMusicResId)
        }

        // Duck background music - with extra safety checks
        duckBackgroundMusic()

        // Request audio focus
        requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)

        // Release any existing question player
        releaseQuestionPlayer()

        try {
            // Create and configure new player
            questionPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                setOnPreparedListener { mp ->
                    // Double-check ducking before starting playback
                    duckBackgroundMusic()
                    mp.start()
                    Log.d(TAG, "Question audio started playing")
                }

                setOnCompletionListener {
                    Log.d(TAG, "Question audio completed")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                    release()
                    questionPlayer = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Question audio error: what=$what, extra=$extra")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                    release()
                    questionPlayer = null
                    true
                }

                try {
                    setDataSource(audioUrl)
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up question audio: ${e.message}")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating question player: ${e.message}", e)
            restoreBackgroundMusic()
            onCompletion()
        }

        // Safety timeout to ensure background music is restored
        Handler(Looper.getMainLooper()).postDelayed({
            if (questionPlayer == null || questionPlayer?.isPlaying != true) {
                Log.d(TAG, "Safety check: Ensuring background music is restored")
                restoreBackgroundMusic()
            }
        }, 10000) // 10 seconds should be enough for most audio clips
    }

    /**
     * Play question audio from Base64 encoded string
     */
    fun playAudioFromBase64(audioBase64: String, onCompletion: () -> Unit = {}) {
        Log.d(TAG, "Playing audio from Base64")

        // Make sure background music is initialized
        if (!isBackgroundMusicInitialized && backgroundMusicResId != 0) {
            initBackgroundMusic(backgroundMusicResId)
        }

        // Duck background music - with extra safety checks
        duckBackgroundMusic()

        // Request audio focus
        requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)

        // Release any existing question player
        releaseQuestionPlayer()

        try {
            // Decode the Base64 audio data
            val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)

            // Create a temporary file to play the audio
            val tempFile = File.createTempFile("audio", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            tempFile.writeBytes(audioBytes)

            // Create and configure new player
            questionPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                setOnPreparedListener { mp ->
                    // Double-check ducking before starting playback
                    duckBackgroundMusic()
                    mp.start()
                    Log.d(TAG, "Base64 audio started playing")
                }

                setOnCompletionListener {
                    Log.d(TAG, "Base64 audio completed")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                    release()
                    questionPlayer = null
                    tempFile.delete()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Base64 audio error: what=$what, extra=$extra")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                    release()
                    questionPlayer = null
                    tempFile.delete()
                    true
                }

                try {
                    setDataSource(tempFile.path)
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up Base64 audio: ${e.message}")
                    restoreBackgroundMusic()
                    abandonTransientAudioFocus()
                    onCompletion()
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Base64 audio: ${e.message}")
            restoreBackgroundMusic()
            abandonTransientAudioFocus()
            onCompletion()
        }

        // Safety timeout to ensure background music is restored
        Handler(Looper.getMainLooper()).postDelayed({
            if (questionPlayer == null || questionPlayer?.isPlaying != true) {
                Log.d(TAG, "Safety check: Ensuring background music is restored")
                restoreBackgroundMusic()
            }
        }, 10000) // 10 seconds should be enough for most audio clips
    }

    /**
     * Pause background music
     */
    private fun pauseBackgroundMusic() {
        try {
            backgroundPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isBackgroundMusicPlaying = false
                    Log.d(TAG, "Background music paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing background music: ${e.message}", e)
        }
    }

    /**
     * Duck background music volume
     */
    private fun duckBackgroundMusic() {
        try {
            backgroundPlayer?.let { player ->
                player.setVolume(DUCKING_VOLUME, DUCKING_VOLUME)
                Log.d(TAG, "Background music volume set to $DUCKING_VOLUME")
            } ?: run {
                Log.d(TAG, "Cannot duck: backgroundPlayer is null, trying to initialize")
                if (backgroundMusicResId != 0) {
                    initBackgroundMusic(backgroundMusicResId)
                    // Try ducking again after initialization
                    backgroundPlayer?.setVolume(DUCKING_VOLUME, DUCKING_VOLUME)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ducking background music: ${e.message}", e)
        }
    }

    /**
     * Restore background music to normal volume and resume if paused
     */
    private fun restoreBackgroundMusic() {
        try {
            backgroundPlayer?.let { player ->
                player.setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
                Log.d(TAG, "Background music volume restored to $NORMAL_VOLUME")

                if (!player.isPlaying && isBackgroundMusicInitialized) {
                    player.start()
                    isBackgroundMusicPlaying = true
                    Log.d(TAG, "Background music playback resumed")
                }
            } ?: run {
                Log.d(TAG, "Cannot restore: backgroundPlayer is null, trying to initialize")
                if (backgroundMusicResId != 0) {
                    initBackgroundMusic(backgroundMusicResId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring background music: ${e.message}", e)
            // If there was an error, try to recreate the player
            if (backgroundMusicResId != 0) {
                releaseBackgroundPlayer()
                initBackgroundMusic(backgroundMusicResId)
            }
        }
    }

    /**
     * Add audio to cache
     */
    fun cacheAudio(id: Long, audioBase64: String) {
        audioCache[id] = audioBase64
    }

    /**
     * Get audio from cache
     */
    fun getAudioFromCache(id: Long): String? {
        return audioCache[id]
    }

    /**
     * Request audio focus
     */
    private fun requestAudioFocus(focusGain: Int): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android Oreo and above
            val focusRequest = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = focusRequest
            systemAudioManager.requestAudioFocus(focusRequest)
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            systemAudioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                focusGain
            )
        }

        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus.set(granted)
        return granted
    }

    /**
     * Abandon transient audio focus
     */
    private fun abandonTransientAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { systemAudioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    /**
     * Release background player
     */
    private fun releaseBackgroundPlayer() {
        try {
            backgroundPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
                Log.d(TAG, "Background player released")
            }
            backgroundPlayer = null
            isBackgroundMusicPlaying = false
            isBackgroundMusicInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing background player: ${e.message}", e)
            backgroundPlayer = null
            isBackgroundMusicPlaying = false
            isBackgroundMusicInitialized = false
        }
    }

    /**
     * Release question player
     */
    private fun releaseQuestionPlayer() {
        try {
            questionPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
                Log.d(TAG, "Question player released")
            }
            questionPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing question player: ${e.message}", e)
            questionPlayer = null
        }
    }

    /**
     * Clean up resources
     */
    fun release() {
        releaseBackgroundPlayer()
        releaseQuestionPlayer()
        abandonTransientAudioFocus()
        audioFocusRequest = null
        audioCache.clear()
        backgroundMusicResId = 0
        isBackgroundMusicInitialized = false
        isBackgroundMusicPlaying = false
    }

    /**
     * Debug method to check current state
     */
    fun logCurrentState() {
        Log.d(TAG, "Current state: " +
                "backgroundPlayer=${backgroundPlayer != null}, " +
                "isInitialized=$isBackgroundMusicInitialized, " +
                "isPlaying=$isBackgroundMusicPlaying, " +
                "resourceId=$backgroundMusicResId")
    }
}
