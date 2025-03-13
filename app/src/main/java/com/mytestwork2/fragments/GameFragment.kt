package com.mytestwork2.fragments

import android.app.AlertDialog
import android.media.MediaPlayer
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
import com.bumptech.glide.Glide
import com.mytestwork2.R
import com.mytestwork2.models.GameData
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private var adminId: Long? = null
    private var childId: String? = null
    private var childName: String? = null // player's name
    private var gameType: Int = 0

    private lateinit var apiService: ApiService
    private var gameData: GameData? = null

    // New session-related state variables
    private var sessionId: Long? = null
    private var currentLevel: Int = 0  // now represents the child's current level for the session
    private var totalGamePoints: Int = 0

    private lateinit var backButton: Button
    private lateinit var audioButton: Button
    private lateinit var optionsContainer: LinearLayout
    private lateinit var instructionText: TextView
    private lateinit var playerInfoTextView: TextView  // displays player's name and level

    private var mediaPlayer: MediaPlayer? = null
    private var selectedOption: Int? = null

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
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        instructionText = view.findViewById(R.id.gameInstruction)
        backButton = view.findViewById(R.id.backButton)
        audioButton = view.findViewById(R.id.audioButton)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        playerInfoTextView = view.findViewById(R.id.playerInfoTextView)

        backButton.setOnClickListener { findNavController().popBackStack() }
        audioButton.setOnClickListener { playCorrectAudio() }

        // Start a new game session when the view is ready
        startGameSession()
    }

    private fun startGameSession() {
        lifecycleScope.launch {
            try {
                // Use gameType as gameId (e.g., 1 for letters, etc.)
                val gameId = gameType
                // Start a new session
                val sessionResponse = apiService.startSession(
                    adminId!!,
                    childId!!.toLong(),
                    gameId
                )
                sessionId = sessionResponse.sessionId

                // Get the child’s overall points for this game type from the new endpoint.
                val pointsResponse = apiService.getChildPointsByGameType(childId!!.toLong(), gameType)
                totalGamePoints = pointsResponse.points

                Log.d("GameFragment", "Session started: $sessionId, TotalGamePoints: $totalGamePoints")
                updatePlayerInfo()
                // Now fetch a game question. (The backend will use the child’s points from its record.)
                fetchGame()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("GameFragment", "Error starting session: ${e.message}", e)
            }
        }
    }


    private fun fetchGame() {
        lifecycleScope.launch {
            try {
                // Call backend endpoint to fetch a game question with points filtering.
                val response = apiService.getGame(
                    adminId!!,
                    childId!!.toLong(),
                    gameType
                )
                gameData = response
                when (gameType) {
                    1 -> instructionText.text = "Ýttu á stafinn sem þú heyrir í!"
                    2 -> instructionText.text = "Ýttu á töluna sem þú heyrir í!"
                    3  -> instructionText.text = "Ýttu á dýrið á rétta staðinn!"
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
                setOnClickListener { handleOptionPress(id, this) }
            }
            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=${id}&adminId=$adminId&childId=$childId"
            Log.d("GameFragment", "Loading image from URL: $imageUrl")
            Glide.with(this).load(imageUrl).into(imageButton)
            optionsContainer.addView(imageButton)
        }
    }

    private fun handleOptionPress(id: Int, view: View) {
        playLetterAudio(id)
        selectedOption = id
        if (gameData == null || sessionId == null) return

        val isCorrect = id == gameData!!.correctId

        // Always call recordAnswer with the proper flag
        lifecycleScope.launch {
            try {
                val gameId = when (gameType) {
                    1 -> 1
                    2 -> 2
                    3 -> 3
                    else -> 0
                }
                val response = apiService.recordAnswer(
                    adminId!!,
                    childId!!.toLong(),
                    gameId,
                    sessionId!!,
                    gameData!!.correctId, // The question's correct ID
                    id,                  // The option chosen by the child
                    gameData!!.correctId,
                    isCorrect
                )
                // Update session level and overall points from the response.
                currentLevel = response.currentSessionPoints
                totalGamePoints = response.totalGamePoints
                Log.d("GameFragment", "Session updated: Level: $currentLevel, TotalGamePoints: $totalGamePoints")
                updatePlayerInfo()
            } catch (e: Exception) {
                Log.e("GameFragment", "Error recording answer: ${e.message}", e)
            }
        }

        // Display feedback based on the answer
        if (isCorrect) {
            // Correct answer: animate and show positive feedback.
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
            // Incorrect answer: show a toast prompting to try again.
            Toast.makeText(requireContext(), "Næstum því! Reyndu aftur :)", Toast.LENGTH_SHORT).show()
        }
    }

    // Update player info header with child name, level, and overall points.
    private fun updatePlayerInfo() {
        val nameToShow = childName ?: "Child $childId"
        playerInfoTextView.text = "Player: $nameToShow | Total Points: $totalGamePoints"
    }


    private fun playCorrectAudio() {
        if (gameData == null) return
        val url = "${RetrofitClient.instance.baseUrl()}playAudio?id=${gameData!!.correctId}&adminId=$adminId&childId=$childId"
        Log.d("GameFragment", "Playing correct audio from URL: $url")
        playAudio(url)
    }

    private fun playLetterAudio(id: Int) {
        val url = "${RetrofitClient.instance.baseUrl()}playAudio?id=$id&adminId=$adminId&childId=$childId"
        playAudio(url)
    }

    private fun playAudio(url: String) {
        try {
            mediaPlayer?.reset() ?: run { mediaPlayer = MediaPlayer() }
            mediaPlayer?.apply {
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
