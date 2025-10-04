package za.ac.iie.TallyUp.ui
import android.content.Context
import za.ac.iie.TallyUp.data.DatabaseProvider
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import androidx.core.content.edit
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.text.TextPaint
import androidx.core.content.ContextCompat

class LoginFragment : Fragment(R.layout.fragment_login) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_indicator)

        val signUpText = view.findViewById<TextView>(R.id.sign_up_text)
        val fullText = getString(R.string.don_t_have_an_account_yet_sign_up)
        val start = fullText.indexOf("Sign up")
        val end = start + "Sign up".length

        if (start >= 0) {
            val spannable = SpannableString(fullText)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SignUpFragment())
                        .addToBackStack(null)
                        .commit()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.accent)
                    ds.isUnderlineText = true
                }
            }

            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.accent)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            signUpText.text = spannable
            signUpText.movementMethod = LinkMovementMethod.getInstance()
            signUpText.highlightColor = Color.TRANSPARENT
        } else {
            signUpText.text = fullText // fallback if "Sign up" not found
        }


        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // 🛡️ Check for empty fields
            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = getString(R.string.error_empty_fields)
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            //Disable login button to prevent multiple taps
            loginButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE

            //Access to RoomDB
            val db = DatabaseProvider.getDatabase(requireContext())

            lifecycleScope.launch {
                val user = db.userDao().login(email, password)

                if (user != null) {
                    // Save email for profile access
                    val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                    prefs.edit { putString("loggedInEmail", email) }

                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }

            // Query the database in the background
            lifecycleScope.launch {
                val user = db.userDao().login(email, password)

                // Re-enable login button after query finishes
                loginButton.isEnabled = true
                progressBar.visibility = View.GONE

                if (user != null) {
                    // Login success — navigate to home
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment()) // or HomeFragment()
                        .addToBackStack(null)
                        .commit()
                } else {
                    // Login failed — show error
                    errorText.text = getString(R.string.error_invalid_credentials)
                    errorText.visibility = View.VISIBLE
                }
            }



        }
    }
}
