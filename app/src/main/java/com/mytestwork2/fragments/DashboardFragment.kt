package com.mytestwork2.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
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
    private lateinit var changeChildButton: MaterialButton

    // For the drawer menu
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

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
        drawerLayout = view.findViewById(R.id.drawerLayout)
        navigationView = view.findViewById(R.id.navigationView)
        toolbar = view.findViewById(R.id.toolbar)

        childrenRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Modify toolbar appearance
        toolbar.title = ""
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        toolbar.elevation = 0f

        // Setup toolbar and drawer toggle
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            requireActivity(),
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Handle navigation view menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_change_group -> {
                    // Navigate to ChildSelectionFragment
                    val bundle = Bundle().apply { putLong("adminId", adminId) }
                    findNavController().navigate(R.id.action_dashboardFragment_to_childSelectionFragment, bundle)
                }
                R.id.nav_logout -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_loginFragment)
                }
                R.id.nav_credits -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_creditsFragment)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchDashboard()

        changeChildButton.setOnClickListener {
            // Also navigate to child selection if "Change Group" is tapped
            val bundle = Bundle().apply { putLong("adminId", adminId) }
            findNavController().navigate(R.id.action_dashboardFragment_to_childSelectionFragment, bundle)
        }
    }

    private fun fetchDashboard() {
        lifecycleScope.launch {
            try {
                val response: AdminDashboardResponse = apiService.getAdminDashboard(adminId)
                Log.d("DashboardFragment", "Dashboard response: $response")
                schoolText.text = "${response.schoolName ?: "No school returned"}"

                // Get managed children
                var fetchedChildren = response.managedChildren ?: emptyList()

                // Optionally filter by a passed "selectedChildren" argument if needed.
                val selectedChildrenJson = arguments?.getString("selectedChildren")
                if (!selectedChildrenJson.isNullOrEmpty()) {
                    val selectedIds = mutableSetOf<Long>()
                    val jsonArray = org.json.JSONArray(selectedChildrenJson)
                    for (i in 0 until jsonArray.length()) {
                        selectedIds.add(jsonArray.getLong(i))
                    }
                    fetchedChildren = fetchedChildren.filter { child -> selectedIds.contains(child.id) }
                }

                childrenList = fetchedChildren

                // Set up RecyclerView adapter
                val adapter = ChildrenAdapter(childrenList) { child ->
                    Log.d("DashboardFragment", "Clicked on child: ${child.name}")
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
