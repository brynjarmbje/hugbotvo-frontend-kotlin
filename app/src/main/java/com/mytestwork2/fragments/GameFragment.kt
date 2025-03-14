package com.mytestwork2.fragments

import android.app.AlertDialog
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
        instructionText = view.findViewById(R.id.gameInstruction)
        backButton = view.findViewById(R.id.backButton)
        audioButton = view.findViewById(R.id.audioButton)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        playerInfoTextView = view.findViewById(R.id.playerInfoTextView)

        // Initialize SoundPool for sound effects
        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        correctSoundId = soundPool?.load(requireContext(), R.raw.correct_answer, 1) ?: 0
        incorrectSoundId = soundPool?.load(requireContext(), R.raw.incorrect_answer, 1) ?: 0
        buttonClickSoundId = soundPool?.load(requireContext(), R.raw.button_click, 1) ?: 0

        // Play background music at low volume
        playBackgroundMusic()

        backButton.setOnClickListener {
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            // End current session before navigating back
            endCurrentSession {
                findNavController().popBackStack()
            }
        }
        audioButton.setOnClickListener {
            soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            playCorrectAudio()
        }

        // Start a new game session when the view is ready
        startGameSession()
    }

    private fun playBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.2f, 0.2f)
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
        optionIds.forEach { id ->
            val imageButton = ImageButton(requireContext()).apply {
                layoutParams = buttonParams
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompat.getDrawable(context, R.drawable.bg_rounded)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                elevation = 8f
                isClickable = true
                isFocusable = true
                foreground = requireContext().obtainStyledAttributes(
                    intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                ).getDrawable(0)
                setOnClickListener {
                    soundPool?.play(buttonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
                    handleOptionPress(id, this)
                }
            }
            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=$id&adminId=$adminId&childId=$childId"
            Log.d("GameFragment", "Loading image from URL: $imageUrl")

            // Use Picasso for image loading
            Picasso.get()
                .load(imageUrl)
                .resize(imageSize, imageSize)
                .centerCrop()
                .placeholder(R.drawable.team) // Add a placeholder image
                .error(R.drawable.paper) // Add an error image
                .into(imageButton)

            optionsContainer.addView(imageButton)
        }
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
            Toast.makeText(requireContext(), "Rétt!", Toast.LENGTH_SHORT).show()
            AlertDialog.Builder(requireContext())
                .setTitle("Húrra!")
                .setMessage("Finnum annan staf!")
                .setPositiveButton("Næsti!") { dialog, _ ->
                    dialog.dismiss()
                    selectedOption = null
                    fetchGame()
                }
                .show()
        } else {
            soundPool?.play(incorrectSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
            Toast.makeText(requireContext(), "Næstum því! Reyndu aftur :)", Toast.LENGTH_SHORT).show()
        }
    }

    // Update header with child's name and overall points.
    private fun updatePlayerInfo() {
        val nameToShow = childName ?: "Child $childId"
        playerInfoTextView.text = "Player: $nameToShow | Total Points: $totalGamePoints"
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
