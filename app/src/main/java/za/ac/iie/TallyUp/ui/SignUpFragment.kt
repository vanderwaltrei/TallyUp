package za.ac.iie.TallyUp.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import za.ac.iie.TallyUp.utils.AchievementManager

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private val firebaseRepo = FirebaseRepository()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── View references ──────────────────────────────────────────────────
        val characterContainer   = view.findViewById<LinearLayout>(R.id.character_container)
        val welcomeTitle         = view.findViewById<TextView>(R.id.welcome_title)
        val welcomeSubtitle      = view.findViewById<TextView>(R.id.welcome_subtitle)
        val formCard             = view.findViewById<View>(R.id.form_card)

        val firstNameInput       = view.findViewById<TextInputEditText>(R.id.first_name_input)
        val lastNameInput        = view.findViewById<TextInputEditText>(R.id.last_name_input)
        val emailInput           = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput        = view.findViewById<TextInputEditText>(R.id.password_input)
        val confirmInput         = view.findViewById<TextInputEditText>(R.id.confirm_password_input)
        val createButton         = view.findViewById<Button>(R.id.create_account_button)
        val loginLink            = view.findViewById<TextView>(R.id.login_link)

        // ── Entrance animations ──────────────────────────────────────────────
        playEntranceAnimations(characterContainer, welcomeTitle, welcomeSubtitle, formCard)

        // ── Input validation watcher (your original logic, preserved exactly) ─
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

        listOf(firstNameInput, lastNameInput, emailInput,
            passwordInput, confirmInput).forEach { it.addTextChangedListener(watcher) }

        // ── Navigation: Login tab ────────────────────────────────────────────
        loginLink.setOnClickListener {
            animateButtonPress(it) {
                Log.d("SignUpFragment", "Login link clicked - navigating to LoginFragment")
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        R.anim.fade_slide_up
                    )
                    .replace(R.id.fragment_container, LoginFragment())
                    .addToBackStack("signup_to_login")
                    .commit()
            }
        }

        // ── Create account ───────────────────────────────────────────────────
        createButton.setOnClickListener {
            animateButtonPress(it) {
                val email           = emailInput.text.toString().trim()
                val password        = passwordInput.text.toString().trim()
                val firstName       = firstNameInput.text.toString().trim()
                val lastName        = lastNameInput.text.toString().trim()
                val confirmPassword = confirmInput.text.toString().trim()

                // Final safety checks (your original logic, preserved exactly)
                if (email.isEmpty() || password.isEmpty() ||
                    firstName.isEmpty() || lastName.isEmpty()) {
                    shakeView(formCard)
                    Toast.makeText(requireContext(),
                        "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@animateButtonPress
                }

                if (!isValidEmail(email)) {
                    shakeView(emailInput)
                    Toast.makeText(requireContext(),
                        "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@animateButtonPress
                }

                if (password != confirmPassword) {
                    shakeView(confirmInput)
                    shakeView(passwordInput)
                    Toast.makeText(requireContext(),
                        "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@animateButtonPress
                }

                if (password.length < 6) {
                    shakeView(passwordInput)
                    Toast.makeText(requireContext(),
                        "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@animateButtonPress
                }

                // Disable button to prevent double clicks
                createButton.isEnabled = false
                createButton.text = "Creating Account..."

                lifecycleScope.launch {
                    try {
                        Log.d("SignUpFragment", "Attempting Firebase signup for email: $email")

                        val result = firebaseRepo.signUp(email, password, firstName, lastName)

                        result.onSuccess { userId ->
                            Log.d("SignUpFragment", "User created successfully: $userId")

                            // Clear old cached data for clean state
                            try {
                                val appRepository = AppRepository(requireContext())
                                appRepository.clearUserData()
                                Log.d("SignUpFragment", "✅ Old cache cleared")
                            } catch (e: Exception) {
                                Log.e("SignUpFragment", "⚠️ Cache clear warning: ${e.message}")
                            }

                            // Save credentials to SharedPreferences
                            val prefs = requireContext()
                                .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                            prefs.edit {
                                putString("loggedInEmail", email)
                                putString("userId", userId)
                                putString("userFirstName", firstName)
                            }
                            Log.d("SignUpFragment", "✅ Saved userId: $userId")

                            // Initialize achievements
                            try {
                                AchievementManager.initializeAchievements(requireContext(), userId)
                                Log.d("SignUpFragment", "✅ Achievements initialized")
                            } catch (e: Exception) {
                                Log.e("SignUpFragment", "❌ Achievements error: ${e.message}", e)
                            }

                            // Set initial coins
                            try {
                                za.ac.iie.TallyUp.utils.CharacterManager
                                    .setCoins(requireContext(), 200)
                                Log.d("SignUpFragment", "✅ Initial coins set: 200")
                            } catch (e: Exception) {
                                Log.e("SignUpFragment", "❌ Coins error: ${e.message}")
                            }

                            // Initialize default categories
                            initializeDefaultCategories(userId)

                            Toast.makeText(
                                requireContext(),
                                "Account created! Welcome, $firstName! 🎉",
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

                            createButton.isEnabled = true
                            createButton.text = "Create Account"
                            shakeView(formCard)
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        Log.e("SignUpFragment", "Unexpected error: ${e.message}", e)
                        requireActivity().runOnUiThread {
                            createButton.isEnabled = true
                            createButton.text = "Create Account"
                        }
                        shakeView(formCard)
                        Toast.makeText(requireContext(),
                            "Error creating account: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ── Animation helpers ────────────────────────────────────────────────────

    private fun playEntranceAnimations(
        character: View?,
        title: View?,
        subtitle: View?,
        card: View?
    ) {
        listOf(title, subtitle, card).forEach { it?.alpha = 0f }

        character?.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.character_bounce_in)
        )

        title?.postDelayed({
            title.alpha = 1f
            title.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 200)

        subtitle?.postDelayed({
            subtitle.alpha = 1f
            subtitle.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 320)

        card?.postDelayed({
            card.alpha = 1f
            card.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 440)
    }

    private fun shakeView(view: View?) {
        view?.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        )
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f).setDuration(80)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f).setDuration(80)
        val scaleUpX   = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f).setDuration(150)
        val scaleUpY   = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f).setDuration(150)

        scaleUpX.interpolator = OvershootInterpolator(2f)
        scaleUpY.interpolator = OvershootInterpolator(2f)

        val pressDown = AnimatorSet().apply { playTogether(scaleDownX, scaleDownY) }
        val pressUp   = AnimatorSet().apply { playTogether(scaleUpX, scaleUpY) }

        AnimatorSet().apply { playSequentially(pressDown, pressUp); start() }
        view.postDelayed(action, 80)
    }

    // ── Business logic helpers (your original code, preserved exactly) ───────

    private suspend fun initializeDefaultCategories(userId: String) {
        try {
            Log.d("SignUpFragment", "Initializing default categories for user: $userId")

            val defaultCategories = listOf(
                Category(name = "Food",       type = "Expense", color = "#FFB085", userId = userId),
                Category(name = "Transport",  type = "Expense", color = "#A3D5FF", userId = userId),
                Category(name = "Books",      type = "Expense", color = "#B2E2B2", userId = userId),
                Category(name = "Fun",        type = "Expense", color = "#FFF4A3", userId = userId),
                Category(name = "Shopping",   type = "Expense", color = "#FFB6C1", userId = userId),
                Category(name = "Other",      type = "Expense", color = "#E0E0E0", userId = userId),
                Category(name = "Salary",     type = "Income",  color = "#D1B3FF", userId = userId),
                Category(name = "Gift",       type = "Income",  color = "#D1B3FF", userId = userId),
                Category(name = "Freelance",  type = "Income",  color = "#D1B3FF", userId = userId),
                Category(name = "Allowance",  type = "Income",  color = "#D1B3FF", userId = userId)
            )

            defaultCategories.forEach { category ->
                val result = firebaseRepo.addCategory(category)
                result.onFailure { error ->
                    Log.e("SignUpFragment",
                        "Error adding category ${category.name}: ${error.message}")
                }
            }

            Log.d("SignUpFragment", "✅ Default categories initialized")
        } catch (e: Exception) {
            Log.e("SignUpFragment", "❌ Categories error: ${e.message}", e)
        }
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}