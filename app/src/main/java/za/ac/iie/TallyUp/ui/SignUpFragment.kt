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
import za.ac.iie.TallyUp.data.DatabaseProvider
import za.ac.iie.TallyUp.data.User

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

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

        // Enable button only when all fields are filled and passwords match
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

                // Visual feedback for password match
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

        // Create account logic
        createButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val confirmPassword = confirmInput.text.toString().trim()

            // Validate inputs
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

            // Disable button during processing
            createButton.isEnabled = false
            createButton.text = "Creating Account..."

            val db = DatabaseProvider.getDatabase(requireContext())

            lifecycleScope.launch {
                try {
                    Log.d("SignUpFragment", "Checking if email exists: $email")
                    val existingUser = db.userDao().getUserByEmail(email)

                    if (existingUser != null) {
                        Log.d("SignUpFragment", "Email already exists: $email")
                        Toast.makeText(requireContext(), "Email already exists. Please use a different email.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("SignUpFragment", "Creating new user: $email")
                        val newUser = User(
                            email = email,
                            password = password,
                            firstName = firstName,
                            lastName = lastName
                        )

                        db.userDao().insertUser(newUser)
                        Log.d("SignUpFragment", "User inserted successfully")

                        // Save email for profile access
                        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                        prefs.edit {
                            putString("loggedInEmail", email)
                        }
                        Log.d("SignUpFragment", "Preferences saved for email: $email")

                        Toast.makeText(requireContext(), "Account created successfully! Welcome, $firstName!", Toast.LENGTH_SHORT).show()

                        //Navigate to Start Tutorial page instead of Dashboard
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, StartTutorialFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                } catch (e: Exception) {
                    Log.e("SignUpFragment", "Sign up error: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error creating account: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    // Re-enable button
                    requireActivity().runOnUiThread {
                        createButton.isEnabled = true
                        createButton.text = "Create Account"
                    }
                }
            }
        }

        // Setup login link
        loginLink.setOnClickListener {
            Log.d("SignUpFragment", "Login link clicked - navigating to LoginFragment")
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack("signup_to_login")
                .commit()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}