package com.mytestwork2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mytestwork2.R
import com.mytestwork2.models.LoginRequest
import com.mytestwork2.models.LoginResponse
import com.mytestwork2.network.ApiService
import com.mytestwork2.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private val apiService: ApiService by lazy {
        RetrofitClient.instance.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usernameInput = view.findViewById(R.id.username_input)
        passwordInput = view.findViewById(R.id.password_input)
        loginButton = view.findViewById(R.id.login_button)
        progressBar = view.findViewById(R.id.progress_bar)
        errorText = view.findViewById(R.id.error_text)

        loginButton.setOnClickListener {
            handleLogin()
        }
    }

    private suspend fun warmUpBackend() {
        try {
            // Call the ping endpoint
            val response = apiService.ping()
            if (response.isSuccessful) {
                Log.d("LoginFragment", "Backend warmed up: ${response.body()}")
            } else {
                Log.e("LoginFragment", "Ping failed with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Ping error: ${e.message}")
        }
    }

    private fun handleLogin() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter credentials", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // First, warm up the backend.
                warmUpBackend()

                val response = apiService.login(LoginRequest(username, password))
                if (response.isSupervisor) {
                    // Navigate to the Supervisor Dashboard
                    val bundle = Bundle().apply {
                        putLong("adminId", response.adminId)
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_supervisorFragment, bundle)
                } else {
                    // Navigate to the regular Dashboard
                    val bundle = Bundle().apply {
                        putLong("adminId", response.adminId)
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment, bundle)
                }                      } catch (e: Exception) {
                e.printStackTrace()
                errorText.text = "Innskráning mistókst. Vinsamlegast reyndu aftur"
                errorText.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}
