package za.ac.iie.TallyUp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.ActivityMainBinding
import za.ac.iie.TallyUp.ui.DashboardFragment
import za.ac.iie.TallyUp.ui.TransactionsFragment
import za.ac.iie.TallyUp.ui.budget.BudgetDashboardFragment
import za.ac.iie.TallyUp.ui.GoalsFragment
import za.ac.iie.TallyUp.ui.ProfileFragment

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
            loadFragment(DashboardFragment())
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
                    loadFragment(BudgetDashboardFragment())
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
            // Open Add Transaction screen (to be implemented)
            android.widget.Toast.makeText(this, "Add Transaction clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}