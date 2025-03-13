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
import com.mytestwork2.models.Admin
import com.mytestwork2.models.Child
import com.mytestwork2.models.SupervisorDashboardResponse
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class SupervisorFragment : Fragment() {

    private var adminId: Long = 0

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
                headerTitle.text = "${response.schoolName ?: "No school returned"}"

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
        val input = EditText(requireContext()).apply { hint = "Enter child name" }
        AlertDialog.Builder(requireContext())
            .setTitle("Add Child")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val childName = input.text.toString().trim()
                if (childName.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Call your API to create a child (adjust as needed).
                            val newChild = apiService.createChild(adminId, Child(name = childName))
                            Toast.makeText(requireContext(), "Child added", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // Refresh data.
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error adding child: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun promptAddAdmin() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val usernameInput = EditText(requireContext()).apply { hint = "Enter admin username" }
        val passwordInput = EditText(requireContext()).apply { hint = "Enter admin password" }
        layout.addView(usernameInput)
        layout.addView(passwordInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Admin")
            .setView(layout)
            .setPositiveButton("Add") { dialog, _ ->
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Call your API to create an admin (adjust as needed).
                            val newAdmin = apiService.createAdmin(adminId, Admin(username = username, password = password))
                            Toast.makeText(requireContext(), "Admin added", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // Refresh data.
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error adding admin: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Pop-up for admin details (using admin_popup.xml)
    private fun promptAdminPopup(admin: Admin) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.admin_popup, null)
        builder.setView(view)
        val dialog = builder.create()

        // In your promptAdminPopup(admin: Admin) function:
        view.findViewById<MaterialButton>(R.id.changePasswordButton).setOnClickListener {
            // Create an input field for the new password.
            val passwordInput = EditText(requireContext()).apply {
                hint = "Enter new password"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(passwordInput)
                .setPositiveButton("Change") { dialog, _ ->
                    val newPassword = passwordInput.text.toString().trim()
                    if (newPassword.isNotEmpty()) {
                        lifecycleScope.launch {
                            try {
                                // Call the API to change password.
                                val response = apiService.changeAdminPassword(adminId, admin.id!!, newPassword)
                                if (response.isSuccessful) {
                                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Error changing password: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        view.findViewById<MaterialButton>(R.id.deleteAdminPopupButton).setOnClickListener {
            showDeleteConfirmationDialog("admin", admin.id!!)
        }
        view.findViewById<MaterialButton>(R.id.closeAdminPopupButton).setOnClickListener {
            dialog.dismiss()
        }
        // Set filler details (if desired).
        // view.findViewById<TextView>(R.id.adminPopupContent).text = "Filler admin details..."

        dialog.show()
    }

    // Pop-up for child details (using child_popup.xml)
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

        // Define the default categories for games
        val defaultCategories = listOf("Letters", "Numbers", "Locate")

        lifecycleScope.launch {
            try {
                // Call the endpoint that returns a map of category names to points.
                val pointsMap = apiService.getAllChildPoints(child.id!!)

                // Build a string showing points per category.
                val details = StringBuilder("Points by Game:\n")
                defaultCategories.forEach { category ->
                    // If the category doesn't exist in the map, default to 0.
                    val points = pointsMap[category] ?: 0
                    details.append("$category: $points\n")
                }

                view.findViewById<TextView>(R.id.childPopupContent).text = details.toString()
            } catch (e: Exception) {
                Log.e("ChildPopup", "Error fetching child points: ${e.message}", e)
                view.findViewById<TextView>(R.id.childPopupContent).text = "Failed to load details."
            }
        }

        dialog.show()
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
