package za.ac.iie.TallyUp.ui

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
import za.ac.iie.TallyUp.data.DatabaseProvider

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_indicator)
        val signUpText = view.findViewById<TextView>(R.id.sign_up_text)

        // SIMPLE CLICK LISTENER - Make entire TextView clickable
        signUpText.setOnClickListener {
            Log.d("LoginFragment", "Sign up text clicked - navigating to SignUpFragment")
            navigateToSignUp()
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Check for empty fields
            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = getString(R.string.error_empty_fields)
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Validate email format
            if (!isValidEmail(email)) {
                errorText.text = "Please enter a valid email address"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Disable login button to prevent multiple taps
            loginButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE

            // Access to RoomDB
            val db = DatabaseProvider.getDatabase(requireContext())

            // SINGLE database query with proper error handling
            lifecycleScope.launch {
                try {
                    Log.d("LoginFragment", "Attempting login for email: $email")
                    val user = db.userDao().login(email, password)

                    // Re-enable login button after query finishes
                    loginButton.isEnabled = true
                    progressBar.visibility = View.GONE

                    if (user != null) {
                        // Save email for profile access
                        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                        prefs.edit {
                            putString("loggedInEmail", email)
                        }
                        Log.d("LoginFragment", "Login successful, saved email to prefs: $email")

                        Toast.makeText(requireContext(), "Welcome back, ${user.firstName}!", Toast.LENGTH_SHORT).show()

                        // Navigate to dashboard
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, DashboardFragment())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        // Login failed - show error
                        errorText.text = getString(R.string.error_invalid_credentials)
                        errorText.visibility = View.VISIBLE
                        Log.d("LoginFragment", "Login failed - invalid credentials for email: $email")
                        Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Handle any database errors
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