@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.utils.AchievementManager

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private val firebaseRepo = FirebaseRepository()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firstNameInput = view.findViewById<TextInputEditText>(R.id.first_name_input)
        val lastNameInput = view.findViewById<TextInputEditText>(R.id.last_name_input)
        val emailInput = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val confirmInput = view.findViewById<TextInputEditText>(R.id.confirm_password_input)
        val createButton = view.findViewById<Button>(R.id.create_account_button)
        val loginLink = view.findViewById<TextView>(R.id.login_link)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val allFilled = listOf(
                    firstNameInput.text,
                    lastNameInput.text,
                    emailInput.text,
                    passwordInput.text,
                    confirmInput.text
                ).all { !it.isNullOrBlank() }

                val passwordsMatch = passwordInput.text.toString() == confirmInput.text.toString()
                createButton.isEnabled = allFilled && passwordsMatch

                if (confirmInput.text?.isNotEmpty() == true && !passwordsMatch) {
                    confirmInput.error = "Passwords do not match"
                } else {
                    confirmInput.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        firstNameInput.addTextChangedListener(watcher)
        lastNameInput.addTextChangedListener(watcher)
        emailInput.addTextChangedListener(watcher)
        passwordInput.addTextChangedListener(watcher)
        confirmInput.addTextChangedListener(watcher)

        createButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val confirmPassword = confirmInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createButton.isEnabled = false
            createButton.text = "Creating Account..."

            lifecycleScope.launch {
                try {
                    Log.d("SignUpFragment", "Attempting Firebase signup for email: $email")
                    val result = firebaseRepo.signUp(email, password, firstName, lastName)

                    result.onSuccess { userId ->
                        Log.d("SignUpFragment", "User created successfully with userId: $userId")

                        // ✅ CRITICAL FIX: Save userId to SharedPreferences FIRST
                        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                        prefs.edit {
                            putString("loggedInEmail", email)
                            putString("userId", userId)
                            putString("userFirstName", firstName)
                        }

                        Log.d("SignUpFragment", "✅ Saved userId to SharedPreferences: $userId")

                        // ✅ NOW initialize achievements (after userId is saved)
                        try {
                            AchievementManager.initializeAchievements(requireContext(), userId)
                            Log.d("SignUpFragment", "✅ Achievements initialized for user: $userId")
                        } catch (e: Exception) {
                            Log.e("SignUpFragment", "❌ Error initializing achievements: ${e.message}", e)
                            // Don't fail signup if achievements fail
                        }

                        // Set initial coins
                        za.ac.iie.TallyUp.utils.CharacterManager.setCoins(requireContext(), 200)
                        Log.d("SignUpFragment", "✅ Set initial coins: 200")

                        // Initialize default categories
                        initializeDefaultCategories(userId)

                        Toast.makeText(
                            requireContext(),
                            "Account created successfully! Welcome, $firstName!",
                            Toast.LENGTH_SHORT
                        ).show()

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, StartTutorialFragment())
                            .commit()

                    }.onFailure { error ->
                        Log.e("SignUpFragment", "Sign up error: ${error.message}", error)

                        val errorMessage = when {
                            error.message?.contains("already in use") == true ->
                                "Email already exists. Please use a different email."
                            error.message?.contains("network") == true ->
                                "Network error. Please check your connection."
                            error.message?.contains("weak-password") == true ->
                                "Password is too weak. Please use a stronger password."
                            else -> "Error creating account: ${error.message}"
                        }

                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("SignUpFragment", "Unexpected sign up error: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Error creating account: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    requireActivity().runOnUiThread {
                        createButton.isEnabled = true
                        createButton.text = "Create Account"
                    }
                }
            }
        }

        loginLink.setOnClickListener {
            Log.d("SignUpFragment", "Login link clicked - navigating to LoginFragment")
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack("signup_to_login")
                .commit()
        }
    }

    private suspend fun initializeDefaultCategories(userId: String) {
        try {
            Log.d("SignUpFragment", "Initializing default categories for user: $userId")

            val defaultCategories = listOf(
                Category(name = "Food", type = "Expense", color = "#FFB085", userId = userId),
                Category(name = "Transport", type = "Expense", color = "#A3D5FF", userId = userId),
                Category(name = "Books", type = "Expense", color = "#B2E2B2", userId = userId),
                Category(name = "Fun", type = "Expense", color = "#FFF4A3", userId = userId),
                Category(name = "Shopping", type = "Expense", color = "#FFB6C1", userId = userId),
                Category(name = "Other", type = "Expense", color = "#E0E0E0", userId = userId),
                Category(name = "Salary", type = "Income", color = "#D1B3FF", userId = userId),
                Category(name = "Gift", type = "Income", color = "#D1B3FF", userId = userId),
                Category(name = "Freelance", type = "Income", color = "#D1B3FF", userId = userId),
                Category(name = "Allowance", type = "Income", color = "#D1B3FF", userId = userId)
            )

            defaultCategories.forEach { category ->
                val result = firebaseRepo.addCategory(category)
                result.onFailure { error ->
                    Log.e("SignUpFragment", "Error adding category ${category.name}: ${error.message}")
                }
            }

            Log.d("SignUpFragment", "Default categories initialized successfully")
        } catch (e: Exception) {
            Log.e("SignUpFragment", "Error initializing default categories: ${e.message}", e)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}