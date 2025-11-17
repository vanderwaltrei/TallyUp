@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import za.ac.iie.TallyUp.utils.AchievementManager

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val firebaseRepo = FirebaseRepository()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_indicator)
        val signUpText = view.findViewById<TextView>(R.id.sign_up_text)

        signUpText.setOnClickListener {
            Log.d("LoginFragment", "Sign up text clicked - navigating to SignUpFragment")
            navigateToSignUp()
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = getString(R.string.error_empty_fields)
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                errorText.text = "Please enter a valid email address"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE

            lifecycleScope.launch {
                try {
                    Log.d("LoginFragment", "Attempting Firebase login for email: $email")

                    val result = firebaseRepo.login(email, password)

                    loginButton.isEnabled = true
                    progressBar.visibility = View.GONE

                    result.onSuccess { userId ->
                        Log.d("LoginFragment", "✅ Login successful for userId: $userId")

                        // Get user profile to display first name
                        val profileResult = firebaseRepo.getUserProfile()

                        profileResult.onSuccess { profile ->
                            val firstName = profile["firstName"] as? String ?: "User"

                            // Save credentials for session management
                            val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                            prefs.edit {
                                putString("loggedInEmail", email)
                                putString("userId", userId)
                                putString("userFirstName", firstName)
                            }
                            Log.d("LoginFragment", "✅ Saved login data to SharedPreferences")

                            // ✅ CRITICAL: Initialize achievements for existing users who might not have them
                            lifecycleScope.launch {
                                try {
                                    Log.d("LoginFragment", "🎯 Checking/initializing achievements for userId: $userId")

                                    // Check if user has achievements
                                    val existingAchievements = AchievementManager.getAllAchievements(requireContext(), userId)

                                    if (existingAchievements.isEmpty()) {
                                        Log.d("LoginFragment", "⚠️ No achievements found, initializing...")
                                        AchievementManager.initializeAchievements(requireContext(), userId)
                                        Log.d("LoginFragment", "✅ Achievements initialized on login")
                                    } else {
                                        Log.d("LoginFragment", "✅ User already has ${existingAchievements.size} achievements")
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginFragment", "❌ Error checking/initializing achievements: ${e.message}", e)
                                    // Don't fail login if achievements fail
                                }
                            }

                            Toast.makeText(requireContext(), "Welcome back, $firstName!", Toast.LENGTH_SHORT).show()

                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, DashboardFragment())
                                .commit()

                        }.onFailure { profileError ->
                            Log.e("LoginFragment", "Failed to get profile: ${profileError.message}")

                            val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                            prefs.edit {
                                putString("loggedInEmail", email)
                                putString("userId", userId)
                            }

                            // Still initialize achievements
                            lifecycleScope.launch {
                                try {
                                    val existingAchievements = AchievementManager.getAllAchievements(requireContext(), userId)
                                    if (existingAchievements.isEmpty()) {
                                        AchievementManager.initializeAchievements(requireContext(), userId)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginFragment", "Error initializing achievements: ${e.message}")
                                }
                            }

                            Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()

                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, DashboardFragment())
                                .commit()
                        }
                    }.onFailure { error ->
                        errorText.text = when {
                            error.message?.contains("password") == true -> "Invalid email or password"
                            error.message?.contains("network") == true -> "Network error. Please check your connection."
                            else -> "Login failed. Please try again."
                        }
                        errorText.visibility = View.VISIBLE
                        Log.d("LoginFragment", "Login failed: ${error.message}")
                        Toast.makeText(requireContext(), "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Login error: ${e.message}", e)
                    loginButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    errorText.text = "Login failed. Please try again."
                    errorText.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToSignUp() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SignUpFragment())
            .addToBackStack("login_to_signup")
            .commit()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}