package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.Editable
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.DatabaseProvider
import za.ac.iie.TallyUp.data.User
import androidx.core.content.edit


class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

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

            val db = DatabaseProvider.getDatabase(requireContext())

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)

                if (existingUser != null) {
                    // Toast for duplicate email
                    Toast.makeText(requireContext(), "Email already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName
                    )

                    db.userDao().insertUser(newUser)
                    // Save email for profile access
                    val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                    prefs.edit { putString("loggedInEmail", email) }

                    Toast.makeText(requireContext(), "Account created!", Toast.LENGTH_SHORT).show()

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .addToBackStack(null)
                        .commit()



                    // Toast for success
                    Toast.makeText(requireContext(), "Account created!", Toast.LENGTH_SHORT).show()

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        // Style "Log in" in orange
        val fullText = getString(R.string.already_have_an_account_log_in) // e.g. "Already have an account? Log in"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Log in")
        val end = start + "Log in".length

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.accent)),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        loginLink.text = spannable

        // Link to login screen
        loginLink.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
