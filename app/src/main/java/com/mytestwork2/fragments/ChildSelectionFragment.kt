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
import com.mytestwork2.ChildSelectionAdapter
import com.mytestwork2.R
import com.mytestwork2.models.Child
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONArray

class ChildSelectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var headerText: TextView

    private val apiService: ApiService by lazy {
        RetrofitClient.instance.create(ApiService::class.java)
    }
    private var adminId: Long = 0
    private var allChildren: List<Child> = emptyList()
    private var selectedChildren: MutableSet<Long> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminId = arguments?.getLong("adminId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_child_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headerText = view.findViewById(R.id.header)
        recyclerView = view.findViewById(R.id.childrenRecyclerView)
        saveButton = view.findViewById(R.id.saveButton)
        backButton = view.findViewById(R.id.backButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchChildren()

        saveButton.setOnClickListener {
            handleSave()
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun fetchChildren() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        lifecycleScope.launch {
            try {
                // Fetch the children that are already in the admin's group.
                val managedChildren = apiService.getAllChildrenInAdminGroup(adminId)
                // Fetch the children that are not managed by the admin.
                val unmanagedChildren = apiService.getUnmanagedChildren(adminId)
                // Combine both lists.
                allChildren = managedChildren + unmanagedChildren

                // Initialize selectedChildren with IDs of managed children.
                selectedChildren = managedChildren.mapNotNull { it.id }.toMutableSet()

                Log.d("ChildSelectionFragment", "Fetched children: $allChildren")
                recyclerView.adapter = ChildSelectionAdapter(allChildren, selectedChildren) { childId ->
                    toggleSelection(childId)
                }
            } catch (e: Exception) {
                Log.e("ChildSelectionFragment", "Error fetching children: ${e.message}", e)
                errorText.text = "Failed to load children."
                errorText.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun toggleSelection(childId: Long) {
        if (selectedChildren.contains(childId)) {
            selectedChildren.remove(childId)
        } else {
            selectedChildren.add(childId)
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun handleSave() {
        if (selectedChildren.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Ekkert valið")
                .setMessage("Vinsamlegast veldu allavega einn í hópinn.")
                .setPositiveButton("Allt í lagi", null)
                .show()
            return
        }

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                // 1) Clear the entire group.
                val clearResponse = apiService.clearGroup(adminId)
                if (!clearResponse.isSuccessful) {
                    throw Exception("Failed to clear group: ${clearResponse.code()}")
                }

                // 2) Add all selected children to the group.
                selectedChildren.forEach { childId ->
                    apiService.addChildToGroup(adminId, childId)
                }

                Toast.makeText(requireContext(), "Hópi breytt", Toast.LENGTH_SHORT).show()

                val bundle = Bundle().apply {
                    putLong("adminId", adminId)
                }
                findNavController().navigate(R.id.action_childSelectionFragment_to_dashboardFragment, bundle)
            } catch (e: Exception) {
                Log.e("ChildSelectionFragment", "Error saving group: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to update group", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

}
