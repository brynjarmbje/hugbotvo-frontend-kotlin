package com.mytestwork2.fragments

import android.app.AlertDialog
import android.os.Bundle
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

    private lateinit var schoolNameText: TextView
    private lateinit var childrenRecyclerView: RecyclerView
    private lateinit var adminsRecyclerView: RecyclerView
    private lateinit var logoutButton: Button

    // New buttons for additional functionalities
    private lateinit var addChildButton: Button
    private lateinit var deleteChildButton: Button
    private lateinit var addAdminButton: Button
    private lateinit var deleteAdminButton: Button
    private lateinit var changePasswordButton: Button

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
        return inflater.inflate(R.layout.fragment_supervisor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        schoolNameText = view.findViewById(R.id.schoolNameText)
        childrenRecyclerView = view.findViewById(R.id.childrenRecyclerView)
        adminsRecyclerView = view.findViewById(R.id.adminsRecyclerView)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Bind new buttons (ensure these IDs exist in your fragment_supervisor.xml)
        addChildButton = view.findViewById(R.id.addChildButton)
        deleteChildButton = view.findViewById(R.id.deleteChildButton)
        addAdminButton = view.findViewById(R.id.addAdminButton)
        deleteAdminButton = view.findViewById(R.id.deleteAdminButton)
        changePasswordButton = view.findViewById(R.id.changePasswordButton)

        childrenRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adminsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        logoutButton.setOnClickListener {
            Log.d("SupervisorFragment", "Logout clicked")
            findNavController().navigate(R.id.action_supervisorFragment_to_loginFragment)
        }

        // Set up extra functionality
        addChildButton.setOnClickListener { promptAddChild() }
        deleteChildButton.setOnClickListener { promptDeleteChild() }
        addAdminButton.setOnClickListener { promptAddAdmin() }
        deleteAdminButton.setOnClickListener { promptDeleteAdmin() }
        changePasswordButton.setOnClickListener { promptChangePassword() }

        fetchSupervisorDashboard()
    }

    private fun fetchSupervisorDashboard() {
        lifecycleScope.launch {
            try {
                val response: SupervisorDashboardResponse = apiService.getSupervisorDashboard(adminId)
                Log.d("SupervisorFragment", "Dashboard response: $response")

                // Update school name
                schoolNameText.text = "School: ${response.schoolName ?: "No school returned"}"

                // Update children and admins lists
                childrenList = response.children ?: emptyList()
                adminsList = response.admins ?: emptyList()

                // Set up RecyclerView adapters
                childrenRecyclerView.adapter = ChildrenAdapter(childrenList) { child ->
                    Log.d("SupervisorFragment", "Child clicked: ${child.name}")
                    // Optionally handle child click events here.
                }
                adminsRecyclerView.adapter = AdminsAdapter(adminsList) { admin ->
                    Log.d("SupervisorFragment", "Admin clicked: ${admin.username}")
                    // Optionally handle admin click events here.
                }
            } catch (e: Exception) {
                Log.e("SupervisorFragment", "Error loading dashboard: ${e.message}", e)
                schoolNameText.text = "Failed to load supervisor dashboard."
            }
        }
    }

    // --- Functions for Additional Actions ---

    private fun promptAddChild() {
        val input = EditText(requireContext())
        input.hint = "Enter child name"
        AlertDialog.Builder(requireContext())
            .setTitle("Add Child")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val childName = input.text.toString().trim()
                if (childName.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Assuming createChild takes a query parameter adminId and a Child body
                            val newChild = apiService.createChild(adminId, Child(name = childName))
                            Toast.makeText(requireContext(), "Child added", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // refresh data
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

    private fun promptDeleteChild() {
        if (childrenList.isEmpty()) {
            Toast.makeText(requireContext(), "No children available", Toast.LENGTH_SHORT).show()
            return
        }
        // Build an array of child names.
        val childNames = childrenList.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Child to Delete")
            .setItems(childNames) { dialog, which ->
                // 'which' is the selected index; get the corresponding child.
                val childToDelete = childrenList[which]
                lifecycleScope.launch {
                    try {
                        apiService.deleteChild(childToDelete.id!!, adminId)
                        Toast.makeText(requireContext(), "Child deleted", Toast.LENGTH_SHORT).show()
                        fetchSupervisorDashboard() // Refresh dashboard
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting child: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun promptAddAdmin() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val usernameInput = EditText(requireContext())
        usernameInput.hint = "Enter admin username"
        val passwordInput = EditText(requireContext())
        passwordInput.hint = "Enter admin password"
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
                            // Assuming createAdmin takes a query parameter adminId and an Admin object in the body
                            val newAdmin = apiService.createAdmin(adminId, Admin(username = username, password = password))
                            Toast.makeText(requireContext(), "Admin added", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // refresh data
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

    private fun promptDeleteAdmin() {
        if (adminsList.isEmpty()) {
            Toast.makeText(requireContext(), "No admins available", Toast.LENGTH_SHORT).show()
            return
        }
        // Build an array of admin usernames.
        val adminNames = adminsList.map { it.username }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Admin to Delete")
            .setItems(adminNames) { dialog, which ->
                // 'which' is the selected index; get the corresponding admin.
                val adminToDelete = adminsList[which]
                lifecycleScope.launch {
                    try {
                        val response = apiService.deleteAdmin(adminToDelete.id!!, adminId)
                        if (response.isSuccessful) {
                            // 2xx, including 204
                            Toast.makeText(requireContext(), "Admin deleted", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard()
                        } else {
                            // 4xx/5xx
                            Toast.makeText(requireContext(),
                                "Error deleting admin: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting admin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun promptChangePassword() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val targetAdminIdInput = EditText(requireContext())
        targetAdminIdInput.hint = "Enter admin ID"
        val newPasswordInput = EditText(requireContext())
        newPasswordInput.hint = "Enter new password"
        layout.addView(targetAdminIdInput)
        layout.addView(newPasswordInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Admin Password")
            .setView(layout)
            .setPositiveButton("Change") { dialog, _ ->
                val adminIdStr = targetAdminIdInput.text.toString().trim()
                val newPassword = newPasswordInput.text.toString().trim()
                val targetAdminId = adminIdStr.toLongOrNull()
                if (targetAdminId != null && newPassword.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Assuming changeAdminPassword takes query parameters: adminId (supervisor), target admin id, and newPassword
                            apiService.changeAdminPassword(adminId, targetAdminId, newPassword)
                            Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                            fetchSupervisorDashboard() // refresh if needed
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error changing password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
