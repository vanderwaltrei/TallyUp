package za.ac.iie.TallyUp.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import za.ac.iie.TallyUp.utils.AchievementManager

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val firebaseRepo = FirebaseRepository()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── View references ──────────────────────────────────────────────────
        val characterContainer  = view.findViewById<LinearLayout>(R.id.character_container)
        val welcomeTitle        = view.findViewById<TextView>(R.id.welcome_title)
        val welcomeSubtitle     = view.findViewById<TextView>(R.id.welcome_subtitle)
        val formCard            = view.findViewById<View>(R.id.form_card)
        val socialSection       = view.findViewById<View>(R.id.social_section)

        val emailInputLayout    = view.findViewById<TextInputLayout>(R.id.email_input_layout)
        val passwordInputLayout = view.findViewById<TextInputLayout>(R.id.password_input_layout)
        val emailInput          = view.findViewById<TextInputEditText>(R.id.email_input)
        val passwordInput       = view.findViewById<TextInputEditText>(R.id.password_input)
        val loginButton         = view.findViewById<Button>(R.id.login_button)
        val errorText           = view.findViewById<TextView>(R.id.error_text)
        val progressBar         = view.findViewById<ProgressBar>(R.id.progress_indicator)
        val forgotPasswordText  = view.findViewById<TextView>(R.id.forgot_password_text)
        val signUpTab           = view.findViewById<TextView>(R.id.sign_up_button)
        val googleButton        = view.findViewById<MaterialButton>(R.id.google_login_button)
        val facebookButton      = view.findViewById<MaterialButton>(R.id.facebook_login_button)

        // ── Entrance animations ──────────────────────────────────────────────
        playEntranceAnimations(
            characterContainer, welcomeTitle, welcomeSubtitle, formCard, socialSection
        )

        // ── Navigation ───────────────────────────────────────────────────────
        signUpTab.setOnClickListener {
            animateButtonPress(it) { navigateToSignUp() }
        }

        forgotPasswordText.setOnClickListener {
            navigateToForgotPassword()
        }

        googleButton.setOnClickListener {
            animateButtonPress(it) {
                Toast.makeText(requireContext(), "Google sign-in coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        facebookButton.setOnClickListener {
            animateButtonPress(it) {
                Toast.makeText(requireContext(), "Facebook sign-in coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Login logic ──────────────────────────────────────────────────────
        loginButton.setOnClickListener {
            animateButtonPress(it) {
                val email    = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    showError(errorText, emailInputLayout, passwordInputLayout,
                        getString(R.string.error_empty_fields))
                    return@animateButtonPress
                }

                if (!isValidEmail(email)) {
                    showError(errorText, emailInputLayout, passwordInputLayout,
                        "Please enter a valid email address")
                    return@animateButtonPress
                }

                loginButton.isEnabled  = false
                progressBar.visibility = View.VISIBLE
                errorText.visibility   = View.GONE

                lifecycleScope.launch {
                    try {
                        val result = firebaseRepo.login(email, password)

                        loginButton.isEnabled  = true
                        progressBar.visibility = View.GONE

                        result.onSuccess { userId ->
                            Log.d("LoginFragment", "✅ Login successful for userId: $userId")

                            val profileResult = firebaseRepo.getUserProfile()

                            profileResult.onSuccess { profile ->
                                val firstName = profile["firstName"] as? String ?: "User"

                                val prefs = requireContext()
                                    .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                                prefs.edit {
                                    putString("loggedInEmail", email)
                                    putString("userId", userId)
                                    putString("userFirstName", firstName)
                                }
                                Log.d("LoginFragment", "✅ Saved login data to SharedPreferences")

                                initAchievements(userId)
                                Toast.makeText(requireContext(),
                                    "Welcome back, $firstName!", Toast.LENGTH_SHORT).show()
                                navigateToDashboard()

                            }.onFailure {
                                val prefs = requireContext()
                                    .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                                prefs.edit {
                                    putString("loggedInEmail", email)
                                    putString("userId", userId)
                                }
                                initAchievements(userId)
                                Toast.makeText(requireContext(),
                                    "Welcome back!", Toast.LENGTH_SHORT).show()
                                navigateToDashboard()
                            }

                        }.onFailure { error ->
                            val message = when {
                                error.message?.contains("password") == true       ->
                                    "Invalid email or password"
                                error.message?.contains("user-not-found") == true ->
                                    "Account not found"
                                error.message?.contains("network") == true        ->
                                    "Network error. Please try again."
                                else -> "Login failed. Please try again."
                            }
                            showError(errorText, emailInputLayout, passwordInputLayout, message)
                        }

                    } catch (e: Exception) {
                        loginButton.isEnabled  = true
                        progressBar.visibility = View.GONE
                        showError(errorText, emailInputLayout, passwordInputLayout,
                            "Login failed. Please try again.")
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
        card: View?,
        social: View?
    ) {
        // Hide all elements before animating them in
        listOf(title, subtitle, card, social).forEach { it?.alpha = 0f }

        // 1. Character bounces in immediately
        character?.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.character_bounce_in)
        )

        // 2. Title fades + slides up after 200ms
        title?.postDelayed({
            title.alpha = 1f
            title.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 200)

        // 3. Subtitle after 320ms
        subtitle?.postDelayed({
            subtitle.alpha = 1f
            subtitle.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 320)

        // 4. Form card after 440ms
        card?.postDelayed({
            card.alpha = 1f
            card.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 440)

        // 5. Social section after 580ms
        social?.postDelayed({
            social.alpha = 1f
            social.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_up)
            )
        }, 580)
    }

    private fun showError(
        errorText: TextView,
        field1: View,
        field2: View,
        message: String
    ) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
        errorText.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
        field1.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
        field2.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
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

    // ── Business logic helpers ───────────────────────────────────────────────

    private fun initAchievements(userId: String) {
        lifecycleScope.launch {
            try {
                val existing = AchievementManager.getAllAchievements(requireContext(), userId)
                if (existing.isEmpty()) {
                    AchievementManager.initializeAchievements(requireContext(), userId)
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Achievements error: ${e.message}", e)
            }
        }
    }

    private fun navigateToDashboard() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardFragment())
            .commit()
    }

    private fun navigateToSignUp() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_slide_up,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, SignUpFragment())
            .addToBackStack("login_to_signup")
            .commit()
    }

    private fun navigateToForgotPassword() {
        Toast.makeText(requireContext(), "Password reset coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}