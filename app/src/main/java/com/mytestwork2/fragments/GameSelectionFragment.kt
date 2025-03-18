package com.mytestwork2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.GameSelectionAdapter
import com.mytestwork2.R
import com.mytestwork2.models.GameOption
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class GameSelectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var playerHeader: TextView

    // The adminId, childId and childName are passed as arguments.
    private var adminId: Long? = null
    private var childId: String? = null
    private var childName: String? = null // player's name

    // List of available games updated from the backend
    private val availableGames = mutableListOf<GameOption>()

    // Create an instance of your API service
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            adminId = it.getLong("adminId")
            childId = it.getString("childId")
            childName = it.getString("childName")
        }
        apiService = RetrofitClient.instance.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.gameRecyclerView)
        backButton = view.findViewById(R.id.backButton)
        playerHeader = view.findViewById(R.id.playerHeader)

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Instead of hardcoding points, fetch the real points from the backend.
        fetchAndDisplayChildPoints()
    }

    private fun fetchAndDisplayChildPoints() {
        lifecycleScope.launch {
            try {
                // Game types: 1 = Letters, 2 = Numbers, 3 = Locate
                val gameTypes = listOf(1, 2, 3)
                availableGames.clear()
                var totalPoints = 0

                // Loop over each game type to fetch the child's points.
                for (gameType in gameTypes) {
                    // Call the endpoint that returns points for the child in a specific game type.
                    val response = apiService.getChildPointsByGameType(childId!!.toLong(), gameType)
                    val points = response.points
                    totalPoints += points

                    // Map gameType to a display name (adjust as needed)
                    val gameName = when (gameType) {
                        1 -> "Stafir"
                        2 -> "Tölur"
                        3 -> "Staðsetning"
                        else -> "Unknown"
                    }
                    availableGames.add(GameOption(id = gameType, name = gameName, points = points))
                }
                // After adding the existing game types, add the new Shake Challenge.
                availableGames.add(GameOption(id = 4, name = "Hrista!"))

                // Update the header with the child's name and total points.
                playerHeader.text = "${childName ?: "Child $childId"}! Þú ert með $totalPoints stig! Hvað viltu læra!?"

                // Set up RecyclerView adapter using the updated availableGames list.
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = GameSelectionAdapter(availableGames) { selectedGame ->
                    if (selectedGame.id == 4) {
                        // Navigate to the ShakeGameFragment for the Shake Challenge.
                        val bundle = Bundle().apply {
                            putLong("adminId", adminId!!)
                            putString("childId", childId)
                            putString("childName", childName)
                        }
                        findNavController().navigate(R.id.action_gameSelectionFragment_to_shakeGameFragment, bundle)
                    } else {
                        // Navigate to the regular GameFragment for other game types.
                        val bundle = Bundle().apply {
                            putLong("adminId", adminId!!)
                            putString("childId", childId)
                            putString("childName", childName)
                            putInt("gameType", selectedGame.id)
                        }
                        findNavController().navigate(R.id.action_gameSelectionFragment_to_gameFragment, bundle)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading child points: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("GameSelectionFragment", "Error fetching child points", e)
            }
        }
    }
}

