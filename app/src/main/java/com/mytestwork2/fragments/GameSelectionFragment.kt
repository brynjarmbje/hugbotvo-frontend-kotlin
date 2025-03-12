package com.mytestwork2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.GameSelectionAdapter
import com.mytestwork2.R
import com.mytestwork2.models.GameOption

class GameSelectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var playerHeader: TextView

    // The adminId, childId and childName are passed as arguments.
    private var adminId: Long? = null
    private var childId: String? = null
    private var childName: String? = null // player's name

    // Simulate available games with points.
    private val availableGames = mutableListOf<GameOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the adminId, childId and childName from the fragment arguments.
        arguments?.let {
            adminId = it.getLong("adminId")
            childId = it.getString("childId")
            childName = it.getString("childName") // retrieve child's name
        }
        // Simulate the points for each game option.
        // You might later replace these values with a real API call.
        availableGames.add(GameOption(id = 1, name = "Stafir", points = 27))
        availableGames.add(GameOption(id = 2, name = "Tölur", points = 35))
        availableGames.add(GameOption(id = 3, name = "Staðsetning", points = 0))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.gameRecyclerView)
        backButton = view.findViewById(R.id.backButton)
        playerHeader = view.findViewById(R.id.playerHeader)

        // Calculate total points from all games.
        val totalPoints = availableGames.sumOf { it.points }
        // Update the header with the player's name and total points.
        playerHeader.text = "${childName ?: "Child $childId"}! Þú ert með $totalPoints stig! Hvað viltu læra!?"

        // Set up RecyclerView with a LinearLayoutManager and our adapter.
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = GameSelectionAdapter(availableGames) { selectedGame ->
            Log.d("GameSelectionFragment", "Selected game: ${selectedGame.id}")
            // Navigate to GameFragment with parameters.
            val bundle = Bundle().apply {
                putLong("adminId", adminId!!)
                putString("childId", childId)
                putString("childName", childName) // pass child's name
                putInt("gameType", selectedGame.id)
            }
            findNavController().navigate(R.id.action_gameSelectionFragment_to_gameFragment, bundle)
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
