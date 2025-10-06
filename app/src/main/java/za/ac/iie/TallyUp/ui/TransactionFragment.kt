@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.databinding.FragmentTransactionsBinding
import za.ac.iie.TallyUp.models.TransactionViewModel
import za.ac.iie.TallyUp.models.TransactionViewModelFactory

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase
    private lateinit var transactionViewModel: TransactionViewModel

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

        val transactionDao = appDatabase.transactionDao()
        val factory = TransactionViewModelFactory(transactionDao)
        transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        setupSpinners()
        loadTransactions()
        debugCheckAllTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupSpinners() {
        val typeOptions = listOf("All", "Income", "Expense")
        val timeOptions = listOf("All", "Today", "This Week", "This Month")

        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.typeFilterSpinner.adapter = typeAdapter

        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeOptions)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.timePeriodSpinner.adapter = timeAdapter

        binding.timePeriodSpinner.visibility = View.VISIBLE
        binding.typeFilterSpinner.visibility = View.VISIBLE
        binding.categoryFilterSpinner.visibility = View.VISIBLE

        binding.typeFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = parent.getItemAtPosition(position).toString()
                transactionViewModel.setTypeFilter(selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTime = parent.getItemAtPosition(position).toString()
                transactionViewModel.setTimeFilter(selectedTime)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Category spinner setup
        val categoryDao = appDatabase.categoryDao()
        lifecycleScope.launch {
            val userId = getCurrentUserId()
            val categories = categoryDao.getCategoriesForUser(userId).map { it.name }
            val categoryOptions = listOf("All") + categories

            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryOptions)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categoryFilterSpinner.adapter = categoryAdapter

            binding.categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedCategory = parent.getItemAtPosition(position).toString()
                    transactionViewModel.setCategoryFilter(selectedCategory)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
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

        transactionViewModel.loadTransactionsForUser(userId)

        transactionViewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            Log.d(TAG, "Filtered transactions received: ${transactions.size}")

            if (transactions.isNotEmpty()) {
                binding.transactionsRecyclerView.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                transactionAdapter.submitList(transactions)
                Log.d(TAG, "UI Updated: Showing ${transactions.size} filtered transactions")
            } else {
                binding.transactionsRecyclerView.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                Log.d(TAG, "UI Updated: Showing empty state (no filtered transactions)")
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