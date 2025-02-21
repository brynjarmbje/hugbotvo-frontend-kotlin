package com.mytestwork2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    // The adminId and childId should be passed as arguments.
    private var adminId: Long? = null
    private var childId: String? = null

    // Define available games similar to your Expo version.
    private val availableGames = listOf(
        GameOption(id = "letters", name = "Letters"),
        GameOption(id = "numbers", name = "Numbers"),
        GameOption(id = "locate", name = "Locate")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the adminId and childId from the fragment arguments.
        arguments?.let {
            adminId = it.getLong("adminId")
            childId = it.getString("childId")
        }
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

        // Set up RecyclerView with a LinearLayoutManager and our adapter.
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = GameSelectionAdapter(availableGames) { selectedGame ->
            Log.d("GameSelectionFragment", "Selected game: ${selectedGame.id}")
            // Navigate to GameFragment with parameters.
            val bundle = Bundle().apply {
                putLong("adminId", adminId!!)
                putString("childId", childId)
                putString("gameType", selectedGame.id)
            }
            findNavController().navigate(R.id.action_gameSelectionFragment_to_gameFragment, bundle)
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
