package com.mytestwork2.fragments

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.mytestwork2.R
import kotlinx.coroutines.*

class ShakeGameFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // UI Elements
    private lateinit var scoreText: TextView
    private lateinit var restartButton: Button
    private lateinit var tilBakaButton: Button
    private lateinit var shakeAnimationView: LottieAnimationView

    // MediaPlayer for shake music
    private var shakeMusicPlayer: MediaPlayer? = null

    // Game logic
    private var isShaking = false
    private var shakeStartTime: Long = 0L
    private var currentShakeDuration: Long = 0L
    private var lastShakeTime: Long = 0L

    // Configuration
    private val shakeThreshold = 12f     // Adjust as needed for sensitivity
    private val pauseThreshold = 1000L     // 1 second pause ends the game

    // Coroutine to check for pause in shaking
    private var pauseCheckJob: Job? = null

    // SharedPreferences for high score storage
    private lateinit var prefs: SharedPreferences

    // Flag to disable sensor events when dialog is open
    private var isDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        prefs = requireContext().getSharedPreferences("shake_game_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shake_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scoreText = view.findViewById(R.id.shakeScoreText)
        restartButton = view.findViewById(R.id.restartButton)
        tilBakaButton = view.findViewById(R.id.tilBakaButton)
        shakeAnimationView = view.findViewById(R.id.shakeAnimationView)

        restartButton.setOnClickListener {
            resetGame()
        }

        tilBakaButton.setOnClickListener {
            stopShakeMusic()
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        resetGame()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        pauseCheckJob?.cancel()
        shakeAnimationView.pauseAnimation()
        stopShakeMusic()
    }

    private fun resetGame() {
        isShaking = false
        shakeStartTime = 0L
        currentShakeDuration = 0L
        lastShakeTime = 0L
        scoreText.text = "Hrista hristaaaa!"
        pauseCheckJob?.cancel()
        shakeAnimationView.pauseAnimation()
        stopShakeMusic()
        isDialogShowing = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // If the dialog is showing, ignore sensor events.
        if (isDialogShowing) return

        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val currentTime = SystemClock.elapsedRealtime()

        if (acceleration > shakeThreshold) {
            // Start the shake animation if not already playing.
            if (!shakeAnimationView.isAnimating) {
                shakeAnimationView.playAnimation()
            }
            // Start the shake music if not already started.
            if (!isShaking) {
                isShaking = true
                shakeStartTime = currentTime
                startShakeMusic()
            }
            lastShakeTime = currentTime

            // Update the current shake duration.
            currentShakeDuration = currentTime - shakeStartTime
            scoreText.text = "Met: ${currentShakeDuration / 1000.0} sekúndur"

            // Cancel any previous pause check.
            pauseCheckJob?.cancel()

            // Start a new pause-check coroutine: if no shake event within pauseThreshold, end game.
            pauseCheckJob = CoroutineScope(Dispatchers.Main).launch {
                delay(pauseThreshold)
                if (SystemClock.elapsedRealtime() - lastShakeTime >= pauseThreshold) {
                    endGame()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed.
    }

    private fun startShakeMusic() {
        if (shakeMusicPlayer == null) {
            shakeMusicPlayer = MediaPlayer.create(requireContext(), R.raw.techno_beat)
            shakeMusicPlayer?.isLooping = true
            shakeMusicPlayer?.setVolume(1.0f, 1.0f)
            shakeMusicPlayer?.start()
        }
    }

    private fun stopShakeMusic() {
        shakeMusicPlayer?.stop()
        shakeMusicPlayer?.release()
        shakeMusicPlayer = null
    }

    private fun endGame() {
        isShaking = false
        isDialogShowing = true
        // Pause the shake animation and stop music.
        shakeAnimationView.pauseAnimation()
        stopShakeMusic()

        // Calculate final score in seconds.
        val finalScoreSec = currentShakeDuration / 1000.0
        // Get current high score.
        val highScore = prefs.getFloat("met", 0f)
        val editor = prefs.edit()
        var messageText = "Þú náðir ${"%.2f".format(finalScoreSec)} sekúndum!"
        if (finalScoreSec > highScore) {
            editor.putFloat("met", finalScoreSec.toFloat()).apply()
            messageText += "\nNýtt met!"
        } else {
            messageText += "\nMet: ${"%.2f".format(highScore)} sekúndur"
        }

        // Inflate custom dialog view.
        val dialogView = layoutInflater.inflate(R.layout.dialog_shake_result, null)
        val highScoreTextView = dialogView.findViewById<TextView>(R.id.highScoreText)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeButton)

        // Set the high score line.
        highScoreTextView.text = "Met: ${"%.2f".format(highScore)} sekúndur"
        // Set the dialog message.
        dialogMessage.text = messageText

        // Build the dialog.
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        positiveButton.setOnClickListener {
            dialog.dismiss()
            isDialogShowing = false
            resetGame()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
            isDialogShowing = false
            findNavController().popBackStack()
        }

        dialog.show()
    }


}
