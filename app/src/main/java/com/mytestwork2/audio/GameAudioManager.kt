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
        private const val DUCKING_VOLUME = 0.2f
        private const val QUESTION_VOLUME = 0.9f
    }

    // Audio players
    private var backgroundPlayer: MediaPlayer? = null
    private var questionPlayer: MediaPlayer? = null

    // Audio focus management
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = AtomicBoolean(false)

    // Audio cache
    private val audioCache = mutableMapOf<Long, String>()

    // Audio focus listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus LOSS - pausing background music")
                backgroundPlayer?.pause()
                hasAudioFocus.set(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT - pausing background music")
                backgroundPlayer?.pause()
                hasAudioFocus.set(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT_CAN_DUCK - ducking background music")
                backgroundPlayer?.setVolume(DUCKING_VOLUME, DUCKING_VOLUME)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus GAIN - restoring background music")
                backgroundPlayer?.setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
                if (backgroundPlayer?.isPlaying == false) {
                    backgroundPlayer?.start()
                }
                hasAudioFocus.set(true)
            }
        }
    }

    /**
     * Initialize and start background music
     */
    fun initBackgroundMusic(resourceId: Int) {
        // Release any existing player
        releaseBackgroundPlayer()

        // Create and configure new player
        backgroundPlayer = MediaPlayer.create(context, resourceId).apply {
            isLooping = true
            setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
            start()
        }

        // Request audio focus for background playback
        requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN)
    }

    /**
     * Play question audio from a URL
     */
    fun playQuestionAudio(audioUrl: String, onCompletion: () -> Unit = {}) {
        // Request transient audio focus
        if (requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)) {
            // Release any existing question player
            releaseQuestionPlayer()

            // Create and configure new player
            questionPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                setOnPreparedListener { mp ->
                    mp.start()
                    Log.d(TAG, "Question audio started playing")
                }

                setOnCompletionListener {
                    Log.d(TAG, "Question audio completed")
                    abandonTransientAudioFocus()
                    onCompletion()
                    release()
                    questionPlayer = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Question audio error: what=$what, extra=$extra")
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
                    abandonTransientAudioFocus()
                    onCompletion()
                }
            }
        } else {
            // Failed to get audio focus
            Log.d(TAG, "Failed to get audio focus for question audio")
            onCompletion()
        }
    }

    /**
     * Play question audio from Base64 encoded string
     */
    fun playAudioFromBase64(audioBase64: String, onCompletion: () -> Unit = {}) {
        // Request transient audio focus
        if (requestAudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)) {
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
                        mp.start()
                        Log.d(TAG, "Base64 audio started playing")
                    }

                    setOnCompletionListener {
                        Log.d(TAG, "Base64 audio completed")
                        abandonTransientAudioFocus()
                        onCompletion()
                        release()
                        questionPlayer = null
                        tempFile.delete()
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Base64 audio error: what=$what, extra=$extra")
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
                        abandonTransientAudioFocus()
                        onCompletion()
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Base64 audio: ${e.message}")
                abandonTransientAudioFocus()
                onCompletion()
            }
        } else {
            // Failed to get audio focus
            Log.d(TAG, "Failed to get audio focus for Base64 audio")
            onCompletion()
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
        backgroundPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        backgroundPlayer = null
    }

    /**
     * Release question player
     */
    private fun releaseQuestionPlayer() {
        questionPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        questionPlayer = null
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
    }
}
