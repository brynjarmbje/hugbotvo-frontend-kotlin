package com.mytestwork2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.ChildrenAdapter
import com.mytestwork2.R
import com.mytestwork2.models.AdminDashboardResponse
import com.mytestwork2.models.Child
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var schoolText: TextView
    private lateinit var childrenRecyclerView: RecyclerView
    private lateinit var changeChildButton: Button

    private lateinit var apiService: ApiService
    private var adminId: Long = 0
    private var childrenList: List<Child> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve adminId from arguments
        adminId = arguments?.getLong("adminId") ?: 0L
        apiService = RetrofitClient.instance.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        schoolText = view.findViewById(R.id.schoolText)
        childrenRecyclerView = view.findViewById(R.id.childrenRecyclerView)
        changeChildButton = view.findViewById(R.id.changeChildButton)

        childrenRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchDashboard()

        changeChildButton.setOnClickListener {
            // Navigate to child-selection screen if implemented
            Log.d("DashboardFragment", "Change Child Selections clicked")
            val bundle = Bundle().apply {
                putLong("adminId", adminId)  // Ensure adminId is the correct non-zero value!
            }
            findNavController().navigate(R.id.action_dashboardFragment_to_childSelectionFragment, bundle)
        }
    }

    private fun fetchDashboard() {
        lifecycleScope.launch {
            try {
                // Call the backend endpoint that returns the admin dashboard data.
                val response: AdminDashboardResponse = apiService.getAdminDashboard(adminId)
                Log.d("DashboardFragment", "Dashboard response: $response")

                // Update the school name using the response from the backend
                schoolText.text = "School: ${response.schoolName ?: "No school returned"}"

                // Initially use the managedChildren list from the response.
                var fetchedChildren = response.managedChildren ?: emptyList()

                // Check if selectedChildren was passed from ChildSelectionFragment.
                val selectedChildrenJson = arguments?.getString("selectedChildren")
                if (!selectedChildrenJson.isNullOrEmpty()) {
                    // Parse the JSON array of selected child IDs.
                    val selectedIds = mutableSetOf<Long>()
                    val jsonArray = org.json.JSONArray(selectedChildrenJson)
                    for (i in 0 until jsonArray.length()) {
                        selectedIds.add(jsonArray.getLong(i))
                    }
                    // Filter the list to include only children whose id is in selectedIds.
                    fetchedChildren = fetchedChildren.filter { child -> selectedIds.contains(child.id) }
                }

                childrenList = fetchedChildren

                // Set up the RecyclerView adapter with the children list
                val adapter = ChildrenAdapter(childrenList) { child ->
                    Log.d("DashboardFragment", "Clicked on child: ${child.name}")
                    // Navigate to game selection, if applicable.
                    // Example:
                     val bundle = Bundle().apply {
                         putLong("adminId", adminId)
                         putString("childId", child.id.toString())
                     }
                     findNavController().navigate(R.id.action_dashboardFragment_to_gameSelectionFragment, bundle)
                }
                childrenRecyclerView.adapter = adapter

            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error loading dashboard: ${e.message}", e)
                schoolText.text = "Failed to load dashboard."
            }
        }
    }
}
