package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.databinding.FragmentTransactionsBinding

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase

    companion object {
        private const val TAG = "TransactionsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupSpinners()
        loadTransactions()
        debugCheckAllTransactions() // Debug method to check all transactions in database
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupSpinners() {
        // TODO: Implement spinner functionality for filtering
        // For now, just hide them since they're not implemented
        binding.timePeriodSpinner.visibility = View.GONE
        binding.typeFilterSpinner.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Button to open AddTransactionFragment
        binding.addFirstTransactionButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack("transactions_to_add")
                .commit()
        }

        // Back button to go to Home tab safely
        binding.backButton.setOnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_home
        }
    }

    private fun loadTransactions() {
        val userId = getCurrentUserId()
        Log.d(TAG, "Loading transactions for user: $userId")

        lifecycleScope.launch {
            try {
                val transactions = appDatabase.transactionDao().getTransactionsForUser(userId)
                Log.d(TAG, "Database query completed. Found ${transactions.size} transactions for user $userId")

                // Debug: Print all transactions for this user
                transactions.forEachIndexed { index, transaction ->
                    Log.d(TAG, "User Transaction $index: " +
                            "ID=${transaction.id}, " +
                            "Type=${transaction.type}, " +
                            "Amount=${transaction.amount}, " +
                            "Category=${transaction.category}, " +
                            "Date=${transaction.date}, " +
                            "Description=${transaction.description}")
                }

                requireActivity().runOnUiThread {
                    if (transactions.isNotEmpty()) {
                        // Show transactions and hide empty state
                        binding.transactionsRecyclerView.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                        transactionAdapter.submitList(transactions)

                        Log.d(TAG, "UI Updated: Showing ${transactions.size} transactions")
                    } else {
                        // Show empty state
                        binding.transactionsRecyclerView.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        Log.d(TAG, "UI Updated: Showing empty state (no transactions found for user $userId)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions: ${e.message}", e)
                requireActivity().runOnUiThread {
                    binding.transactionsRecyclerView.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    Log.e(TAG, "Error UI: Showing empty state due to error")
                }
            }
        }
    }

    private fun debugCheckAllTransactions() {
        lifecycleScope.launch {
            try {
                val allTransactions = appDatabase.transactionDao().getAllTransactions()
                Log.d(TAG, "=== DEBUG: ALL TRANSACTIONS IN DATABASE ===")
                Log.d(TAG, "Total transactions in DB: ${allTransactions.size}")
                allTransactions.forEachIndexed { index, transaction ->
                    Log.d(TAG, "DB Transaction $index: " +
                            "ID=${transaction.id}, " +
                            "Type=${transaction.type}, " +
                            "Amount=${transaction.amount}, " +
                            "Category=${transaction.category}, " +
                            "User=${transaction.userId}, " +
                            "Date=${transaction.date}, " +
                            "Description=${transaction.description}")
                }
                Log.d(TAG, "============================================")

                // Also check current user ID
                val currentUserId = getCurrentUserId()
                Log.d(TAG, "Current user ID from prefs: $currentUserId")

                val userTransactions = allTransactions.filter { it.userId == currentUserId }
                Log.d(TAG, "Transactions matching current user: ${userTransactions.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error in debugCheckAllTransactions: ${e.message}")
            }
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("loggedInEmail", "") ?: "default"
        Log.d(TAG, "Retrieved user ID from SharedPreferences: $userId")
        return userId
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing transactions")
        // Refresh transactions when returning to this fragment
        loadTransactions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}