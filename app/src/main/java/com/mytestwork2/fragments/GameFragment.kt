package com.mytestwork2.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
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
import androidx.core.graphics.drawable.toDrawable
import com.mytestwork2.audio.GameAudioManager

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
    private lateinit var audioButtonContainer: MaterialButton
    private lateinit var optionsContainer: LinearLayout
    private lateinit var instructionText: TextView
    private lateinit var playerInfoTextView: TextView
    private lateinit var audioManager: GameAudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var correctSoundIdDing: Int = 0
    private var incorrectSoundId: Int = 0
    private var buttonClickSoundId: Int = 0

    private val NORMAL_VOLUME = 0.7f    // Normal background music volume

    private var selectedOption: Int? = null
    private var imagesToLoad: Int = 0

    // Using viewLifecycleOwner.lifecycleScope for coroutine work
    private val fragmentScope get() = viewLifecycleOwner.lifecycleScope
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var dimOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep your original parameter initialization
        adminId = arguments?.getLong("adminId")
        childId = arguments?.getString("childId")
        childName = arguments?.getString("childName")
        gameType = arguments?.getInt("gameType") ?: 0
        Log.d("GameFragment", "Parameters: adminId=$adminId, childId=$childId, childName=$childName, gameType=$gameType")

        // Keep your API service initialization
        apiService = RetrofitClient.instance.create(ApiService::class.java)

        // Initialize the new audio manager
        audioManager = GameAudioManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingAnimation = view.findViewById(R.id.loadingAnimation)
        dimOverlay = view.findViewById(R.id.dimOverlay)

        // Initialize views
        instructionText = view.findViewById(R.id.gameInstruction)
        backButton = view.findViewById(R.id.backButton)
        audioButtonContainer = view.findViewById(R.id.audioButtonContainer)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        playerInfoTextView = view.findViewById(R.id.playerInfoTextView)

        // Initialize SoundPool for sound effects
        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        correctSoundId = soundPool?.load(requireContext(), R.raw.correct_answer, 1) ?: 0
        correctSoundIdDing = soundPool?.load(requireContext(), R.raw.correct_ding, 1) ?: 0
        incorrectSoundId = soundPool?.load(requireContext(), R.raw.incorrect_answer, 1) ?: 0
        buttonClickSoundId = soundPool?.load(requireContext(), R.raw.button_click, 1) ?: 0

        audioManager = GameAudioManager(requireContext())
        audioManager.initBackgroundMusic(R.raw.background_music)

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

            if (gameData == null) {
                Log.d("GameFragment", "Cannot play audio yet - game data not loaded")
                // Show a toast or some feedback
                Toast.makeText(requireContext(), "Game data is loading...", Toast.LENGTH_SHORT).show()
            } else {
                playCorrectAudio()
            }
        }

        // Add pulse animation to the audiobutton button
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        audioButtonContainer.startAnimation(pulseAnimation)

        // Start a new game session when the view is ready
        startGameSession()
    }

    // When you receive audio data from the API, cache it
    private fun cacheAudioFromResponse(questionId: Long, audioBase64: String) {
        audioManager.cacheAudio(questionId, audioBase64)
    }


    private fun showLoadingAnimation() {
        dimOverlay.visibility = View.VISIBLE
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
    }

    private fun hideLoadingAnimation() {
        loadingAnimation.cancelAnimation()
        loadingAnimation.visibility = View.GONE
        dimOverlay.visibility = View.GONE
    }

    private fun playBounceAnimation(view: View) {
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
        view.startAnimation(animation)
    }

    private fun playBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
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

    @SuppressLint("SetTextI18n")
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
        }
    }

    private fun setupOptionButtons(optionIds: List<Int>) {
        // Clear existing views & animations.
        for (i in 0 until optionsContainer.childCount) {
            optionsContainer.getChildAt(i).clearAnimation()
        }
        optionsContainer.removeAllViews()
        val screenHeight = resources.displayMetrics.heightPixels
        val imageSize = (screenHeight * 0.2).toInt()  // 20% of screen width
        val margin = resources.getDimensionPixelSize(R.dimen.option_margin)
        val buttonParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
            setMargins(margin, margin, margin, margin)
        }

        // Set the counter.
        imagesToLoad = optionIds.size

        optionIds.forEach { id ->
            val button = MaterialButton(ContextThemeWrapper(requireContext(), R.style.OptionButtonStyle)).apply {
                layoutParams = buttonParams
                text = ""
                // Center the icon.
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                gravity = Gravity.CENTER
                iconPadding = 0
                iconTint = null  // ensures the original icon colors are used
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.team)
                iconSize = (imageSize * 0.8).toInt()
            }

            // Remove any hard-coded ripple if needed:
            // button.rippleColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.neon_blue))
            val newElevation = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                resources.displayMetrics
            )
            button.elevation = newElevation

            val imageUrl = getImageUrl(id.toLong())
            val target = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    button.icon = bitmap.toDrawable(resources)
                    button.tag = null
                    decrementImageCounter()  // Decrement counter on successful load.
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    button.icon = ContextCompat.getDrawable(requireContext(), R.drawable.paper)
                    button.tag = null
                    decrementImageCounter()  // Also decrement on failure.
                }
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    button.icon = ContextCompat.getDrawable(requireContext(), R.drawable.team)
                }
            }
            button.tag = target

            Picasso.get()
                .load(imageUrl)
                .resize(imageSize, imageSize)
                .centerCrop()
                .error(R.drawable.paper)
                .placeholder(R.drawable.team)
                .into(target)

            val sideSlide = AnimationUtils.loadAnimation(requireContext(), R.anim.side_slide)
            button.startAnimation(sideSlide)

            button.setOnClickListener {
                handleOptionPress(id, button)
            }

            optionsContainer.addView(button)
        }
    }

    private fun decrementImageCounter() {
        imagesToLoad--
        if (imagesToLoad <= 0) {
            hideLoadingAnimation()
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

    private fun showCustomAlertDialog(
        title: String,
        message: String,
        buttonText: String,
        correctAnswerDrawable: Drawable? = null
    ) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
        val dialogButton = dialogView.findViewById<Button>(R.id.dialog_button)
        val correctAnswerImage = dialogView.findViewById<ImageView>(R.id.correctAnswerImage)

        dialogTitle.text = title
        dialogMessage.text = message
        dialogButton.text = buttonText

        // Show the correct answer image if provided.
        if (correctAnswerDrawable != null) {
            correctAnswerImage.setImageDrawable(correctAnswerDrawable)
            correctAnswerImage.visibility = View.VISIBLE
        } else {
            correctAnswerImage.visibility = View.GONE
        }

        // Optionally add a pulse animation to the button
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        dialogButton.startAnimation(pulseAnimation)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogButton.setOnClickListener {
            val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
            dialogButton.startAnimation(bounceAnimation)
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            dialog.dismiss()
        }

        // Combine necessary actions in one dismiss listener.
        dialog.setOnDismissListener {
            selectedOption = null
            showLoadingAnimation()
            fetchGame()
        }
        dialog.show()
    }

    private fun handleOptionPress(id: Int, view: View) {
        selectedOption = id
        if (gameData == null || sessionId == null) return

        val isCorrect = id == gameData!!.correctId

        if (isCorrect) {
            // Capture the drawable from the button BEFORE starting the animation.
            val correctDrawable = if (view is MaterialButton && view.icon != null) {
                view.icon.constantState?.newDrawable()?.mutate()
            } else null

            // Then apply the blue tint.
            val blueColor = ContextCompat.getColor(requireContext(), R.color.neon_blue)
            if (view is MaterialButton) {
                view.backgroundTintList = ColorStateList.valueOf(blueColor)
            }

            // Start the vertical bounce animation.
            val correctAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.vertical_bounce)
            view.startAnimation(correctAnim)

            // Play sounds, disable the button, etc.
            soundPool?.play(correctSoundIdDing, 1.0f, 1.0f, 0, 0, 1.0f)
            soundPool?.play(correctSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            showCustomToast("🌟 Rétt! Þú ert frábær! 🌟")

            // Show the dialog with the captured drawable.
            showCustomAlertDialog(
                "Húrra!",
                "Þú fannst stafinn! Förum í næstu spurningu! 🚀",
                "Næsta!",
                correctAnswerDrawable = correctDrawable
            )
        } else {
            // Incorrect answer: set red tint and perform horizontal shake.
            if (view is MaterialButton) {
                val redColor = ContextCompat.getColor(requireContext(), R.color.button_background)
                view.backgroundTintList = ColorStateList.valueOf(redColor)
            }

            val shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.horizontal_bounce)
            shakeAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationEnd(animation: Animation?) {
                    view.postDelayed({
                        if (view is MaterialButton) {
                            val defaultColor = ContextCompat.getColor(requireContext(), R.color.neon_blue)
                            view.backgroundTintList = ColorStateList.valueOf(defaultColor)
                        }
                    }, 500)
                }
            })
            view.startAnimation(shakeAnim)
            soundPool?.play(incorrectSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            showCustomToast("🌈 Næstum því! Reyndu aftur! 🌈")
        }

        // Process the backend answer recording.
        fragmentScope.launch {
            try {
                val gameId = gameType
                val response = apiService.recordAnswer(
                    adminId!!,
                    childId!!.toLong(),
                    gameId,
                    sessionId!!,
                    gameData!!.correctId,
                    id,
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
    }

    // Update header with child's name and overall points.
    @SuppressLint("SetTextI18n")
    private fun updatePlayerInfo() {
        val nameToShow = childName ?: "Child $childId"
        playerInfoTextView.text = "Hæ, $nameToShow! Þú ert með $totalGamePoints stig!"
    }

    private fun playLetterAudio(id: Int) {
        // First check if we have this audio in our cache
        val cachedAudio = audioManager.getAudioFromCache(id.toLong())
        if (cachedAudio != null) {
            Log.d("GameFragment", "Playing letter audio from cache for ID: $id")
            audioManager.playAudioFromBase64(cachedAudio)
            return
        }

        // Fallback to the URL method
        val audioUrl = getAudioUrl(id.toLong())
        Log.d("GameFragment", "Playing letter audio from URL for ID: $id")
        audioManager.playQuestionAudio(audioUrl)
    }


    private fun playCorrectAudio() {
        if (gameData == null) {
            Log.e("GameFragment", "Cannot play correct audio: gameData is null")
            return
        }

        val correctId = gameData!!.correctId.toLong()

        // First check if we have this audio in our cache
        val cachedAudio = audioManager.getAudioFromCache(correctId)
        if (!cachedAudio.isNullOrEmpty()) {
            Log.d("GameFragment", "Playing correct audio from cache")
            audioManager.playAudioFromBase64(cachedAudio)
            return
        }

        // Fallback to the URL method
        val audioUrl = getAudioUrl(correctId)
        Log.d("GameFragment", "Playing correct audio from URL: $audioUrl")
        audioManager.playQuestionAudio(audioUrl)
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

        // Release audio resources
        audioManager.release()
        soundPool?.release()

        // Ensure session is ended when fragment is destroyed
        endCurrentSession()
    }


    // For images
    private fun getImageUrl(id: Long): String {
        val url = "${RetrofitClient.baseUrl()}api/questions/$id/image"
        Log.d("GameFragment", "⭐ Generated image URL: $url")
        return url
    }

    // For audio
    private fun getAudioUrl(id: Long): String {
        val url = "${RetrofitClient.baseUrl()}api/questions/$id/audio"
        Log.d("GameFragment", "⭐ Generated audio URL: $url")
        return url
    }


}