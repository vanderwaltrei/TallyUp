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

        createNotificationChannel() // ✅ Step 3 added here

        setupToolbar()
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

    // ✅ Step 3: Create the notification channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tallyup_channel",
                "TallyUp Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled notifications"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "TallyUp"
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
                R.id.navigation_budget -> {
                    loadFragment(BudgetFragment())
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
            supportActionBar?.title = "Add Transaction"
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateNavigationVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val shouldHideNavigation = shouldHideNavigationForFragment(currentFragment)

        if (shouldHideNavigation) {
            hideNavigation()
        } else {
            showNavigation()
        }
    }

    private fun shouldHideNavigationForFragment(fragment: Fragment?): Boolean {
        val fragmentsToHideNav = listOf(
            "LoginFragment",
            "SignUpFragment",
            "StartTutorialFragment",
            "Question1TutorialFragment",
            "Question2TutorialFragment",
            "ChooseCharacterTutorialFragment"
        )

        val fragmentClassName = fragment?.javaClass?.simpleName ?: ""
        return fragmentsToHideNav.contains(fragmentClassName)
    }

    private fun hideNavigation() {
        binding.bottomNavigation.visibility = View.GONE
        binding.fabAddTransaction.visibility = View.GONE
    }

    private fun showNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fabAddTransaction.visibility = View.VISIBLE
    }

    private fun userIsLoggedIn(): Boolean {
        val prefs = getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)
        return email != null
    }
}