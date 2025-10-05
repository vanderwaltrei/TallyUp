package za.ac.iie.TallyUp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.ActivityMainBinding
import za.ac.iie.TallyUp.ui.DashboardFragment
import za.ac.iie.TallyUp.ui.TransactionsFragment
import za.ac.iie.TallyUp.ui.budget.BudgetDashboardFragment
import za.ac.iie.TallyUp.ui.BudgetFragment
import za.ac.iie.TallyUp.ui.GoalsFragment
import za.ac.iie.TallyUp.ui.ProfileFragment
import za.ac.iie.TallyUp.ui.LoginFragment
import android.content.Context
import za.ac.iie.TallyUp.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()
        setupFAB()

        // Load default fragment
        if (savedInstanceState == null) {
            if (userIsLoggedIn()) {
                // If logged in, show dashboard
                loadFragment(DashboardFragment())
            } else {
                // If not logged in, show login screen
                loadFragment(LoginFragment())
            }
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
            // Load TransactionsFragment when FAB is clicked
            loadFragment(TransactionsFragment())

            // Optional: Also update the bottom navigation to show Transactions as selected
            binding.bottomNavigation.selectedItemId = R.id.navigation_transactions

            // Optional: Update toolbar title
            supportActionBar?.title = "Transactions"
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun userIsLoggedIn(): Boolean {
        val prefs = getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)
        return email != null
    }
}