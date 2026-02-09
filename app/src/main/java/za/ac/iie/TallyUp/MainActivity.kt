@file:Suppress("PackageName")

package za.ac.iie.TallyUp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.ActivityMainBinding
import za.ac.iie.TallyUp.ui.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        setupBottomNavigation()
        setupFAB()

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigationVisibility()
        }

        if (savedInstanceState == null) {
            if (userIsLoggedIn()) {
                loadFragment(DashboardFragment())
            } else {
                loadFragment(LoginFragment())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tallyup_channel",
                "TallyUp Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_transactions -> {
                    loadFragment(TransactionsFragment())
                    true
                }
                R.id.navigation_goals -> {
                    loadFragment(GoalsFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            loadFragment(AddTransactionFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateNavigationVisibility() {
        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        val fragmentName = currentFragment?.javaClass?.simpleName ?: ""

        // Hide bottom nav for auth/tutorial screens
        val hideBottomNav = fragmentName in listOf(
            "LoginFragment",
            "SignUpFragment",
            "StartTutorialFragment",
            "Question1TutorialFragment",
            "Question2TutorialFragment",
            "ChooseCharacterTutorialFragment"
        )

        binding.bottomNavigation.visibility =
            if (hideBottomNav) View.GONE else View.VISIBLE

        // 🔥 FAB ONLY on Dashboard
        binding.fabAddTransaction.visibility =
            if (fragmentName == "DashboardFragment") View.VISIBLE else View.GONE
    }

    private fun userIsLoggedIn(): Boolean {
        val prefs = getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInEmail", null) != null
    }
}
