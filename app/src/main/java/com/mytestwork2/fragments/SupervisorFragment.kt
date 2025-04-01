package com.mytestwork2.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mytestwork2.AdminsAdapter
import com.mytestwork2.ChildrenAdapter
import com.mytestwork2.R
import com.mytestwork2.SessionAdapter
import com.mytestwork2.models.Admin
import com.mytestwork2.models.Child
import com.mytestwork2.models.SessionSummary
import com.mytestwork2.models.SupervisorDashboardResponse
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class SupervisorFragment : Fragment() {

    private var adminId: Long = 0
    private var childId: Long = 0
    private var gameId: Int = 0

    // Header to display school name or dashboard title.
    private lateinit var headerTitle: TextView
    // RecyclerViews for Admins and Children.
    private lateinit var adminsRecyclerView: RecyclerView
    private lateinit var childrenRecyclerView: RecyclerView
    // Action buttons for adding new entries.
    private lateinit var addAdminButton: MaterialButton
    private lateinit var addChildButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private lateinit var apiService: ApiService

    private var childrenList: List<Child> = emptyList()
    private var adminsList: List<Admin> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminId = arguments?.getLong("adminId") ?: 0L
        apiService = RetrofitClient.instance.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate your updated supervisor layout (ensure the IDs match the ones below)
        return inflater.inflate(R.layout.fragment_supervisor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bind views from the new layout.
        headerTitle = view.findViewById(R.id.headerTitle)
        adminsRecyclerView = view.findViewById(R.id.adminsRecyclerView)
        childrenRecyclerView = view.findViewById(R.id.childrenRecyclerView)
        addAdminButton = view.findViewById(R.id.addAdminButton)
        addChildButton = view.findViewById(R.id.addChildButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Set up RecyclerViews with LinearLayoutManager.
        adminsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        childrenRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        logoutButton.setOnClickListener {
            Log.d("SupervisorFragment", "Logout clicked")
            findNavController().navigate(R.id.action_supervisorFragment_to_loginFragment)
        }

        addAdminButton.setOnClickListener { promptAddAdmin() }
        addChildButton.setOnClickListener { promptAddChild() }

        fetchSupervisorDashboard()
    }

    private fun fetchSupervisorDashboard() {
        lifecycleScope.launch {
            try {
                val response: SupervisorDashboardResponse = apiService.getSupervisorDashboard(adminId)
                Log.d("SupervisorFragment", "Dashboard response: $response")

                // Update header text with the school name.
                headerTitle.text = "${response.schoolName ?: "Enginn skóli fannst"}"

                // Update lists.
                childrenList = response.children ?: emptyList()
                adminsList = response.admins ?: emptyList()

                // Set up RecyclerView adapters.
                // Each adapter's item click now triggers a pop-up dialog with further details/actions.
                adminsRecyclerView.adapter = AdminsAdapter(adminsList) { admin ->
                    Log.d("SupervisorFragment", "Admin clicked: ${admin.username}")
                    promptAdminPopup(admin)
                }
                childrenRecyclerView.adapter = ChildrenAdapter(childrenList) { child ->
                    Log.d("SupervisorFragment", "Child clicked: ${child.name}")
                    promptChildPopup(child)
                }
            } catch (e: Exception) {
                Log.e("SupervisorFragment", "Error loading dashboard: ${e.message}", e)
                headerTitle.text = "Failed to load supervisor dashboard."
            }
        }
    }

    // --- Prompt Methods for Additional Actions ---

    private fun promptAddChild() {
        val input = EditText(requireContext()).apply { hint = "Skrifaðu nafn barns" }
        AlertDialog.Builder(requireContext())
            .setTitle("Skrá barn")
            .setView(input)
            .setPositiveButton("Skra") { dialog, _ ->
                val childName = input.text.toString().trim()
                if (childName.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Call your API to create a child (adjust as needed).
                            val newChild = apiService.createChild(adminId, Child(name = childName))
                            Toast.makeText(requireContext(), "Barn skráð", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // Refresh data.
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Villa við skráningu barns: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hætta við") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun promptAddAdmin() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val usernameInput = EditText(requireContext()).apply { hint = "Nafn kennarans" }
        val passwordInput = EditText(requireContext()).apply { hint = "Lykilorð kennarans" }
        layout.addView(usernameInput)
        layout.addView(passwordInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Skrá kennara")
            .setView(layout)
            .setPositiveButton("Skrá") { dialog, _ ->
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Call your API to create an admin (adjust as needed).
                            val newAdmin = apiService.createAdmin(adminId, Admin(username = username, password = password))
                            Toast.makeText(requireContext(), "Kennara bætt við", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // Refresh data.
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Það mistókst að bæta við kennara: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hætta við") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Pop-up for admin details (using admin_popup.xml)
    private fun promptAdminPopup(admin: Admin) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.admin_popup, null)
        builder.setView(view)
        val dialog = builder.create()

        view.findViewById<TextView>(R.id.adminPopupTitle).text = admin.username

        // In your promptAdminPopup(admin: Admin) function:
        view.findViewById<MaterialButton>(R.id.changePasswordButton).setOnClickListener {
            // Create an input field for the new password.
            val passwordInput = EditText(requireContext()).apply {
                hint = "Skrifaðu nýtt lykilorð"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Breyta lykilorði")
                .setView(passwordInput)
                .setPositiveButton("Breyta") { dialog, _ ->
                    val newPassword = passwordInput.text.toString().trim()
                    if (newPassword.isNotEmpty()) {
                        lifecycleScope.launch {
                            try {
                                // Call the API to change password.
                                val response = apiService.changeAdminPassword(adminId, admin.id!!, newPassword)
                                if (response.isSuccessful) {
                                    Toast.makeText(requireContext(), "Lykilorði breytt", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Villa við breytingu lykilorðs: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Lykilorð má ekki vera tómt", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Hætta við") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        view.findViewById<MaterialButton>(R.id.deleteAdminPopupButton).setOnClickListener {
            showDeleteConfirmationDialog("admin", admin.id!!)
        }
        view.findViewById<MaterialButton>(R.id.closeAdminPopupButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun promptChildPopup(child: Child) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.child_popup, null)
        builder.setView(view)
        val dialog = builder.create()

        // Update title with the child's name
        view.findViewById<TextView>(R.id.childPopupTitle).text = child.name

        view.findViewById<MaterialButton>(R.id.deleteChildPopupButton).setOnClickListener {
            showDeleteConfirmationDialog("child", child.id!!)
        }
        view.findViewById<MaterialButton>(R.id.closeChildPopupButton).setOnClickListener {
            dialog.dismiss()
        }

        // Fetch and display child points
        lifecycleScope.launch {
            try {
                // Call the endpoint that returns a map of category names to points.
                val pointsMap = apiService.getAllChildPoints(child.id!!)
                val defaultCategories = listOf("Letters", "Numbers", "Locate")
                val categoryMap = mapOf(
                    "Letters" to "Stafir",
                    "Numbers" to "Tölur",
                    "Locate" to "Staðsetning"
                )
                val details = StringBuilder("Stig í leikjum:\n")
                defaultCategories.forEach { category ->
                    val icelandicName = categoryMap[category] ?: category
                    val points = pointsMap[category] ?: 0
                    details.append("$icelandicName: $points\n")
                }
                view.findViewById<TextView>(R.id.childPopupContent).text = details.toString()
            } catch (e: Exception) {
                Log.e("ChildPopup", "Error fetching child points: ${e.message}", e)
                view.findViewById<TextView>(R.id.childPopupContent).text = "Failed to load details."
            }
        }

        // Set up click listeners for the game buttons:
        view.findViewById<MaterialButton>(R.id.buttonLetters).setOnClickListener {
            promptLatestSessionsPopup(child, gameId = 1, gameName = "Stafir")
        }
        view.findViewById<MaterialButton>(R.id.buttonNumbers).setOnClickListener {
            promptLatestSessionsPopup(child, gameId = 2, gameName = "Tölur")
        }
        view.findViewById<MaterialButton>(R.id.buttonLocate).setOnClickListener {
            promptLatestSessionsPopup(child, gameId = 3, gameName = "Staðsetning")
        }

        // Optionally, if you want to show a default session summary in this popup, you can leave childPopupSessions as is.
        // Otherwise, you may choose to hide or remove it.

        dialog.show()
    }

    private fun promptLatestSessionsPopup(child: Child, gameId: Int, gameName: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.simple_popup, null)
        builder.setView(view)
        val dialog = builder.create()

        view.findViewById<TextView>(R.id.popupTitle).text = "Síðustu $gameName leikir"
        val recyclerView = view.findViewById<RecyclerView>(R.id.sessionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<MaterialButton>(R.id.closePopupButton).setOnClickListener {
            dialog.dismiss()
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getLatestSessions(adminId, child.id!!, gameId)
                val sessions = response.sessions
                // Set up the RecyclerView with the sessions
                recyclerView.adapter = SessionAdapter(sessions)
            } catch (e: Exception) {
                Log.e("LatestSessionsPopup", "Error fetching session details: ${e.message}", e)
                // Optionally, show an error message in a TextView if you prefer.
            }
        }

        dialog.show()
    }

    private fun buildSessionDetailsString(sessions: List<SessionSummary>): String {
        if (sessions.isEmpty()) return "Engir leikir til staðar."

        val builder = StringBuilder()
        sessions.forEach { session ->
            // You may customize the details you want to show.
            builder.append("Stig: ${session.points}\n")
            builder.append("Rétt svör: ${session.correctAnswers}\n")
            builder.append("Upphaf: ${session.startTime}\n")
            builder.append("Lok: ${session.endTime ?: "Í gangi"}\n")
            builder.append("-------------------\n")
        }
        return builder.toString()
    }

    // Mapping function for gameId to name:
    private fun getGameName(gameId: Int): String {
        return when (gameId) {
            1 -> "Stafir"
            2 -> "Tölur"
            3 -> "Staðsetning"
            else -> "Óþekktur leikur"
        }
    }

    // Confirmation dialog for deletion (using confirm_delete_popup.xml)
    private fun showDeleteConfirmationDialog(type: String, targetId: Long) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.confirm_delete_popup, null)
        builder.setView(view)
        val dialog = builder.create()
        view.findViewById<MaterialButton>(R.id.confirmDeleteYesButton).setOnClickListener {
            lifecycleScope.launch {
                try {
                    if (type == "admin") {
                        val response = apiService.deleteAdmin(targetId, adminId)
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Admin deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Error deleting admin: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } else if (type == "child") {
                        apiService.deleteChild(targetId, adminId)
                        Toast.makeText(requireContext(), "Child deleted", Toast.LENGTH_SHORT).show()
                    }
                    fetchSupervisorDashboard()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        view.findViewById<MaterialButton>(R.id.confirmDeleteNoButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
