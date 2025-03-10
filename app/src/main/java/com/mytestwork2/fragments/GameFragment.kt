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
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.mytestwork2.R
import com.mytestwork2.models.GameData
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private var adminId: Long? = null
    private var childId: String? = null
    private var gameType: String? = null

    private lateinit var apiService: ApiService
    private var gameData: GameData? = null

    private lateinit var backButton: Button
    private lateinit var audioButton: Button
    private lateinit var optionsContainer: LinearLayout
    private lateinit var instructionText: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var selectedOption: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminId = arguments?.getLong("adminId")
        childId = arguments?.getString("childId")
        gameType = arguments?.getString("gameType")
        Log.d("GameFragment", "Parameters: adminId=$adminId, childId=$childId, gameType=$gameType")
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

        backButton.setOnClickListener { findNavController().popBackStack() }
        audioButton.setOnClickListener { playCorrectAudio() }

        fetchGame()
    }

    private fun fetchGame() {
        lifecycleScope.launch {
            try {
                // Call backend endpoint: /api/admins/{adminId}/children/{childId}/games?gameType=...
                val response = apiService.getGame(
                    adminId!!.toLong(),
                    childId!!.toLong(),
                    gameType!!
                )
                gameData = response
                when (gameType) {
                    "letters" -> {
                        instructionText.text = "Ýttu á stafinn sem þú heyrir í!"
                    }
                    "numbers" -> {
                        instructionText.text = "Ýttu á töluna sem þú heyrir í!"
                    }
                    "locate" -> {
                        instructionText.text = "Ýttu á dýrið á rétta staðnum!"
                    }
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
        // Load desired square size
        val imageSize = resources.getDimensionPixelSize(R.dimen.option_fixed_height)
        val fixedHeight = resources.getDimensionPixelSize(R.dimen.option_fixed_height)
        val margin = resources.getDimensionPixelSize(R.dimen.option_margin)

        // Create LayoutParams for a square (width = height = imageSize)
        val buttonParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
            setMargins(margin, margin, margin, margin)
        }

        optionIds.forEach { id ->

            val imageButton = ImageButton(requireContext()).apply {
                layoutParams = buttonParams
                // Make the image fill the rounded shape if desired:
                scaleType = ImageView.ScaleType.CENTER_CROP

                // 1) Set a background shape with rounded corners
                background = ContextCompat.getDrawable(context, R.drawable.bg_rounded)
                // 2) Tell the system to use that background for the outline
                outlineProvider = ViewOutlineProvider.BACKGROUND
                // 3) Clip the image to match the background’s round corners
                clipToOutline = true
                // 4) Elevation for shadow
                elevation = 8f

                // Enable ripple effect
                isClickable = true
                isFocusable = true
                foreground = requireContext().obtainStyledAttributes(
                    intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                ).getDrawable(0)

                // Remove default button background/tint
                // If the above shape is enough for a "clean" look, you can skip setting background = null
                // background = null

                setOnClickListener { handleOptionPress(id, this) }
            }

            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=${id}&adminId=$adminId&childId=$childId"
            Log.d("GameFragment", "Loading image from URL: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .into(imageButton)

            optionsContainer.addView(imageButton)
        }
    }



    private fun handleOptionPress(id: Int, view: View) {
        playLetterAudio(id)
        selectedOption = id
        if (gameData == null) return
        if (id == gameData!!.correctId) {
            // Correct answer: apply a scale animation for feedback
            val anim = ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            )
            anim.duration = 200
            anim.fillAfter = true
            view.startAnimation(anim)
            Toast.makeText(requireContext(), "Rétt!", Toast.LENGTH_SHORT).show()
            // Instead of shifting layout, show a dialog for next question
            AlertDialog.Builder(requireContext())
                .setTitle("Húrra!")
                .setMessage("Finnum annan staf!")
                .setPositiveButton("Næsti!") { dialog, _ ->
                    dialog.dismiss()
                    // Reset state and fetch a new game question
                    selectedOption = null
                    fetchGame()
                }
                .show()
        } else {
            // Wrong answer: provide feedback (e.g., via Toast)
            Toast.makeText(requireContext(), "Næstum því! Reyndu aftur :)", Toast.LENGTH_SHORT).show()
        }
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
            // Create or reset the MediaPlayer
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            } else {
                mediaPlayer?.reset()
            }
            mediaPlayer?.apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    Log.d("GameFragment", "Audio is prepared; starting playback")
                    mp.start()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("GameFragment", "MediaPlayer error: what=$what, extra=$extra")
                    true  // indicate we handled the error
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
