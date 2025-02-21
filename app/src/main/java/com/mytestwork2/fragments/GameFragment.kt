package com.mytestwork2.fragments

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private var gameType: String? = null

    private lateinit var apiService: ApiService
    private var gameData: GameData? = null

    private lateinit var backButton: Button
    private lateinit var audioButton: Button
    private lateinit var optionsContainer: LinearLayout
    private lateinit var headerText: TextView

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
        headerText = view.findViewById(R.id.gameHeader)
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
                headerText.text = gameData?.message ?: "Game Loaded"
                setupOptionButtons(gameData?.optionIds ?: emptyList())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load the game: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("GameFragment", "Error fetching game: ${e.message}", e)
            }
        }
    }

    private fun setupOptionButtons(optionIds: List<Int>) {
        optionsContainer.removeAllViews()

        // Get fixed height and margin in pixels.
        val fixedHeight = resources.getDimensionPixelSize(R.dimen.option_fixed_height)
        val margin = resources.getDimensionPixelSize(R.dimen.option_margin)

        // Define layout parameters with fixed height and match_parent width.
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            fixedHeight
        ).apply {
            setMargins(margin, margin, margin, margin)
        }

        for (id in optionIds) {
            // Create an ImageButton for each option.
            val imageButton = ImageButton(requireContext()).apply {
                layoutParams = buttonParams
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = null  // Remove default background for a clean image.
                setOnClickListener { handleOptionPress(id, this) }
            }
            // Construct the image URL from your backend.
            val imageUrl = "${RetrofitClient.instance.baseUrl()}getImage?id=${id}&adminId=$adminId&childId=$childId"
            Log.d("GameFragment", "Loading image from URL: $imageUrl")
            // Load image using Glide.
            Glide.with(this)
                .load(imageUrl)
                .into(imageButton)

            // Add the button to the container.
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
            Toast.makeText(requireContext(), "Correct!", Toast.LENGTH_SHORT).show()
            // Instead of shifting layout, show a dialog for next question
            AlertDialog.Builder(requireContext())
                .setTitle("Húrra!")
                .setMessage("Finnum annan staf!")
                .setPositiveButton("Next") { dialog, _ ->
                    dialog.dismiss()
                    // Reset state and fetch a new game question
                    selectedOption = null
                    fetchGame()
                }
                .show()
        } else {
            // Wrong answer: provide feedback (e.g., via Toast)
            Toast.makeText(requireContext(), "Wrong answer, try again!", Toast.LENGTH_SHORT).show()
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
