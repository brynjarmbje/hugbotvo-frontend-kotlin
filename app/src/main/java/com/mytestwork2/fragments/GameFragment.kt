package com.mytestwork2.fragments

import android.app.AlertDialog
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.squareup.picasso.Picasso
import com.mytestwork2.R
import com.mytestwork2.models.GameData
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import com.mytestwork2.transformations.RoundedCornersTransformation
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
    private lateinit var audioButtonContainer: FrameLayout
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
            playBounceAnimation(it)
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            playCorrectAudio()
        }

        // Add pulse animation to the cloud button
        val cloudAnimation = view.findViewById<LottieAnimationView>(R.id.cloudAnimation)
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        cloudAnimation.startAnimation(pulseAnimation)

        // Start a new game session when the view is ready
        startGameSession()
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
        }
    }

    private fun setupOptionButtons(optionIds: List<Int>) {
        optionsContainer.removeAllViews()
        val imageSize = resources.getDimensionPixelSize(R.dimen.option_fixed_height)
        val margin = resources.getDimensionPixelSize(R.dimen.option_margin)
        val buttonParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
            setMargins(margin, margin, margin, margin)
        }

        // Define the corner radius for the images (e.g., 24dp)
        val cornerRadius = resources.getDimension(R.dimen.option_corner_radius)

        optionIds.forEach { id ->
            // Create a FrameLayout to hold the background and the image
            val frameLayout = FrameLayout(requireContext()).apply {
                layoutParams = buttonParams
                isClickable = true
                isFocusable = true
                foreground = requireContext().obtainStyledAttributes(
                    intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                ).getDrawable(0) // Ripple effect for click feedback
                background = ContextCompat.getDrawable(requireContext(), R.drawable.option_background) // Static background
            }

            // Add ImageView for the option
            val imageView = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true // Ensure the image respects the rounded corners
            }
            frameLayout.addView(imageView)

            // Load the image using Picasso with rounded corners
            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=$id&adminId=$adminId&childId=$childId"
            Picasso.get()
                .load(imageUrl)
                .resize(imageSize, imageSize)
                .centerCrop()
                .transform(RoundedCornersTransformation(cornerRadius)) // Apply rounded corners
                .placeholder(R.drawable.team) // Add a placeholder image
                .error(R.drawable.paper) // Add an error image
                .into(imageView)

            // Set click listener
            frameLayout.setOnClickListener {
                soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
                handleOptionPress(id, frameLayout)
            }

            // Add the FrameLayout to the options container
            optionsContainer.addView(frameLayout)
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
            .create()

        dialogButton.setOnClickListener {
            dialog.dismiss()
            selectedOption = null
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

    private fun playAudio(url: String) {
        try {
            mediaPlayer?.release() // Release any existing MediaPlayer instance
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp -> mp.start() }
                setOnErrorListener { mp, what, extra ->
                    Log.e("GameFragment", "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Audio error", Toast.LENGTH_SHORT).show()
            Log.e("GameFragment", "Audio error: ${e.message}", e)
        }
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
