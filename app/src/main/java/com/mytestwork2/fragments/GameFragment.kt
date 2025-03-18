package com.mytestwork2.fragments

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import com.mytestwork2.R
import com.mytestwork2.models.GameData
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private var adminId: Long? = null
    private var childId: String? = null
    private var childName: String? = null
    private var gameType: Int = 0

    private lateinit var apiService: ApiService
    private var gameData: GameData? = null

    // Session-related state variables
    private var sessionId: Long? = null
    private var currentLevel: Int = 0
    private var totalGamePoints: Int = 0

    private lateinit var backButton: Button
    private lateinit var audioButton: Button
    private lateinit var audioButtonContainer: MaterialButton
    private lateinit var optionsContainer: LinearLayout
    private lateinit var instructionText: TextView
    private lateinit var playerInfoTextView: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var questionMediaPlayer: MediaPlayer? = null // Add this variable
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var incorrectSoundId: Int = 0
    private var buttonClickSoundId: Int = 0

    private var selectedOption: Int? = null

    // Using viewLifecycleOwner.lifecycleScope for coroutine work
    private val fragmentScope get() = viewLifecycleOwner.lifecycleScope
    private lateinit var loadingAnimation: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminId = arguments?.getLong("adminId")
        childId = arguments?.getString("childId")
        childName = arguments?.getString("childName")
        gameType = arguments?.getInt("gameType") ?: 0
        Log.d("GameFragment", "Parameters: adminId=$adminId, childId=$childId, childName=$childName, gameType=$gameType")
        apiService = RetrofitClient.instance.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingAnimation = view.findViewById(R.id.loadingAnimation)

        // Initialize views
        instructionText = view.findViewById(R.id.gameInstruction)
        backButton = view.findViewById(R.id.backButton)
        audioButtonContainer = view.findViewById(R.id.audioButtonContainer)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        playerInfoTextView = view.findViewById(R.id.playerInfoTextView)

        // Initialize SoundPool for sound effects
        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        correctSoundId = soundPool?.load(requireContext(), R.raw.correct_answer, 1) ?: 0
        incorrectSoundId = soundPool?.load(requireContext(), R.raw.incorrect_answer, 1) ?: 0
        buttonClickSoundId = soundPool?.load(requireContext(), R.raw.button_click, 1) ?: 0

        // Play background music at low volume
        playBackgroundMusic()

        // Set click listeners
        backButton.setOnClickListener {
            playBounceAnimation(it)
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            endCurrentSession {
                findNavController().popBackStack()
            }
        }

        audioButtonContainer.setOnClickListener {
            // Load bounce animation and set its listener.
            val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
            bounceAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Optionally do something when the bounce starts.
                }
                override fun onAnimationEnd(animation: Animation?) {
                    // When bounce ends, start pulse animation.
                    val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
                    audioButtonContainer.startAnimation(pulseAnimation)
                }
                override fun onAnimationRepeat(animation: Animation?) {
                    // Not used.
                }
            })
            audioButtonContainer.startAnimation(bounceAnimation)
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            playCorrectAudio()
        }


        // Add pulse animation to the audiobutton button
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        audioButtonContainer.startAnimation(pulseAnimation)

        // Start a new game session when the view is ready
        startGameSession()
    }

    private fun showLoadingAnimation() {
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
    }

    private fun hideLoadingAnimation() {
        loadingAnimation.cancelAnimation()
        loadingAnimation.visibility = View.GONE
    }

    private fun playBounceAnimation(view: View) {
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
        view.startAnimation(animation)
    }

    private fun playBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.1f, 0.1f)
        mediaPlayer?.start()
    }

    private fun startGameSession() {
        fragmentScope.launch {
            try {
                val gameId = gameType
                val sessionResponse = apiService.startSession(
                    adminId!!,
                    childId!!.toLong(),
                    gameId
                )
                sessionId = sessionResponse.sessionId

                // Get overall points using the new endpoint.
                val pointsResponse = apiService.getChildPointsByGameType(childId!!.toLong(), gameType)
                totalGamePoints = pointsResponse.points

                Log.d("GameFragment", "Session started: $sessionId, TotalGamePoints: $totalGamePoints")
                updatePlayerInfo()
                updateBackgroundAnimation()
                showLoadingAnimation()
                fetchGame()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("GameFragment", "Error starting session: ${e.message}", e)
            }
        }
    }

    private val gameDataCache = mutableMapOf<String, GameData>() // Cache for game data

    private fun fetchGame() {
        val cacheKey = "$adminId-$childId-$gameType"
        gameDataCache.remove(cacheKey) // Clear the cache for this key

        fragmentScope.launch {
            try {
                val response = apiService.getGame(adminId!!, childId!!.toLong(), gameType)
                gameData = response
                gameDataCache[cacheKey] = response // Cache the new response
                when (gameType) {
                    1 -> instructionText.text = "Ýttu á stafinn sem þú heyrir í!"
                    2 -> instructionText.text = "Ýttu á töluna sem þú heyrir í!"
                    3 -> instructionText.text = "Ýttu á dýrið á rétta staðinn!"
                }
                setupOptionButtons(gameData?.optionIds ?: emptyList())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load the game: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("GameFragment", "Error fetching game: ${e.message}", e)
            }
            finally {
                // Always hide the animation, even if there's an error.
                hideLoadingAnimation()
            }
        }
    }

    private fun setupOptionButtons(optionIds: List<Int>) {
        // First, clear any running animations on existing children.
        for (i in 0 until optionsContainer.childCount) {
            val child = optionsContainer.getChildAt(i)
            child.clearAnimation()
        }
        // Now remove all old views.
        optionsContainer.removeAllViews()
        val screenHeight = resources.displayMetrics.heightPixels
        val imageSize = (screenHeight * 0.2).toInt()  // 20% of screen width
        val margin = resources.getDimensionPixelSize(R.dimen.option_margin)
        val buttonParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
            setMargins(margin, margin, margin, margin)
        }

        optionIds.forEach { id ->
            // Create a MaterialButton using a themed context with your custom style.
            val button = MaterialButton(ContextThemeWrapper(requireContext(), R.style.OptionButtonStyle)).apply {
                layoutParams = buttonParams
                text = ""  // We only show the image as an icon
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START // With no text, the icon will be centered.
                iconPadding = 0
                iconTint = null  // Avoid tinting the loaded image
                // Set a placeholder icon (same as your previous placeholder)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.team)
                iconSize = (imageSize * 0.8).toInt()
            }

            button.rippleColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.neon_blue))
            val newElevation = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,  // adjust this value as needed
                resources.displayMetrics
            )
            button.elevation = newElevation


            // Build the image URL as before.
            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=$id&adminId=$adminId&childId=$childId"

            // Create a Target and assign it to a variable so it's not garbage collected.
            val target = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    button.icon = BitmapDrawable(resources, bitmap)
                    // Optionally clear the tag after loading
                    button.tag = null
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    button.icon = ContextCompat.getDrawable(requireContext(), R.drawable.paper)
                    button.tag = null
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    button.icon = ContextCompat.getDrawable(requireContext(), R.drawable.team)
                }
            }
            // Store the target as a tag on the button to keep a strong reference.
            button.tag = target

            Picasso.get()
                .load(imageUrl)
                .resize(imageSize, imageSize)
                .centerCrop()
                .into(target)

            val sideSlide = AnimationUtils.loadAnimation(requireContext(), R.anim.side_slide)
            button.startAnimation(sideSlide)

            button.setOnClickListener {
                soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
                handleOptionPress(id, button)
                val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
                button.startAnimation(bounceAnimation)
            }

            optionsContainer.addView(button)
        }

    }

    private fun showCustomToast(message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, requireView().findViewById(R.id.toast_layout))
        val text = layout.findViewById<TextView>(R.id.toast_message)
        text.text = message

        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    private fun showCustomAlertDialog(title: String, message: String, buttonText: String) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
        val dialogButton = dialogView.findViewById<Button>(R.id.dialog_button)

        dialogTitle.text = title
        dialogMessage.text = message
        dialogButton.text = buttonText

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true) // or false if you want to force them to tap Næsti
            .create()

        // If the user presses the dialog button:
        dialogButton.setOnClickListener {
            dialog.dismiss()
        }

        // If the user taps outside or presses back to dismiss the dialog:
        dialog.setOnDismissListener {
            // Always fetch the next question once the dialog goes away.
            selectedOption = null
            showLoadingAnimation()
            fetchGame()
        }

        dialog.show()
    }

    private fun handleOptionPress(id: Int, view: View) {
        playLetterAudio(id)
        selectedOption = id
        if (gameData == null || sessionId == null) return

        val isCorrect = id == gameData!!.correctId

        fragmentScope.launch {
            try {
                val gameId = gameType
                val response = apiService.recordAnswer(
                    adminId!!,
                    childId!!.toLong(),
                    gameId,
                    sessionId!!,
                    gameData!!.correctId, // The question's correct ID
                    id,                  // The option chosen
                    gameData!!.correctId,
                    isCorrect
                )
                currentLevel = response.currentSessionPoints
                totalGamePoints = response.totalGamePoints
                Log.d("GameFragment", "Session updated: Level: $currentLevel, TotalGamePoints: $totalGamePoints")
                updatePlayerInfo()
                updateBackgroundAnimation()
            } catch (e: Exception) {
                Log.e("GameFragment", "Error recording answer: ${e.message}", e)
            }
        }

        if (isCorrect) {
            soundPool?.play(correctSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            val anim = ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply { duration = 200; fillAfter = true }
            view.startAnimation(anim)
            showCustomToast("🌟 Rétt! Þú ert frábær! 🌟") // Custom toast for correct answer
            showCustomAlertDialog("Húrra!", "Þú fannst stafinn! Förum í næsta áfanga! 🚀", "Næsti!")
        } else {
            soundPool?.play(incorrectSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            showCustomToast("🌈 Næstum því! Reyndu aftur! 🌈") // Custom toast for incorrect answer
        }
    }

    // Update header with child's name and overall points.
    private fun updatePlayerInfo() {
        val nameToShow = childName ?: "Child $childId"
        playerInfoTextView.text = "Hæ, $nameToShow! Þú ert með $totalGamePoints stig!"
    }

    private fun playLetterAudio(id: Int) {
        val audioUrl = "${RetrofitClient.instance.baseUrl()}playAudio?id=$id&adminId=$adminId&childId=$childId"
        playQuestionAudio(audioUrl)
    }

    private fun playCorrectAudio() {
        if (gameData == null) return
        val audioUrl = "${RetrofitClient.instance.baseUrl()}playAudio?id=${gameData!!.correctId}&adminId=$adminId&childId=$childId"
        playQuestionAudio(audioUrl)
    }

    private fun playQuestionAudio(audioUrl: String) {
        // Pause background music
        mediaPlayer?.pause()

        // Release any existing question MediaPlayer
        questionMediaPlayer?.release()

        // Initialize and play the question audio
        questionMediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { mp ->
                mp.setVolume(0.5f, 0.5f) // Lower the volume of question audio
                mp.start()
            }
            setOnCompletionListener {
                // Resume background music when question audio finishes
                mediaPlayer?.start()
            }
            setOnErrorListener { mp, what, extra ->
                Log.e("GameFragment", "Question audio error: what=$what, extra=$extra")
                // Resume background music on error
                mediaPlayer?.start()
                true
            }
            prepareAsync()
        }
    }

    private fun updateBackgroundAnimation() {
        val backgroundAnimationView = requireView().findViewById<LottieAnimationView>(R.id.backgroundAnimation)
        when (totalGamePoints) {
            in 0..19 -> backgroundAnimationView.setAnimation(R.raw.mountain_background)
            in 20..39 -> backgroundAnimationView.setAnimation(R.raw.mountain_background_sunny)
            in 40..59 -> backgroundAnimationView.setAnimation(R.raw.mountain_background_green)
            else -> backgroundAnimationView.setAnimation(R.raw.mountain_background_blue)
        }
        backgroundAnimationView.playAnimation()
    }

    // Helper to end the current session via the API.
    private fun endCurrentSession(onComplete: (() -> Unit)? = null) {
        sessionId?.let { sid ->
            lifecycleScope.launch {
                try {
                    val response = apiService.endSession(
                        adminId!!,
                        childId!!.toLong(),
                        gameType,
                        sid
                    )
                    Log.d("GameFragment", "Session $sid ended successfully")
                    sessionId = null
                } catch (e: Exception) {
                    Log.e("GameFragment", "Error ending session: ${e.message}", e)
                } finally {
                    onComplete?.invoke()
                }
            }
        } ?: onComplete?.invoke()
    }

    override fun onPause() {
        super.onPause()
        // End session when fragment is paused
        endCurrentSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        questionMediaPlayer?.release()
        soundPool?.release()
        // Ensure session is ended when fragment is destroyed
        endCurrentSession()
    }
}