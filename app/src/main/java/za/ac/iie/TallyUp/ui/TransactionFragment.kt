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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.FragmentTransactionsBinding
import za.ac.iie.TallyUp.firebase.FirebaseRepository
import java.util.*

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase
    private val firebaseRepo = FirebaseRepository()

    private var allTransactions = listOf<Transaction>()
    private var currentTypeFilter = "All"
    private var currentTimeFilter = "All"
    private var currentCategoryFilter = "All"

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
                currentTypeFilter = parent.getItemAtPosition(position).toString()
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentTimeFilter = parent.getItemAtPosition(position).toString()
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Category spinner setup
        lifecycleScope.launch {
            try {
                // ✅ FIXED: Read categories from local Room database
                val categories = withContext(Dispatchers.IO) {
                    appDatabase.categoryDao().getCategoriesForUser(getCurrentUserId())
                }

                val categoryNames = categories.map { it.name }.distinct()
                val categoryOptions = listOf("All") + categoryNames

                val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryOptions)
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.categoryFilterSpinner.adapter = categoryAdapter

                binding.categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        currentCategoryFilter = parent.getItemAtPosition(position).toString()
                        applyFilters()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        binding.addFirstTransactionButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack("transactions_to_add")
                .commit()
        }

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
                // ✅ FIXED: Read from local Room database instead of Firebase
                val transactions = withContext(Dispatchers.IO) {
                    appDatabase.transactionDao().getTransactionsForUser(userId)
                }

                Log.d(TAG, "Loaded ${transactions.size} transactions from Room")
                allTransactions = transactions
                applyFilters()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions from Room: ${e.message}")
                binding.transactionsRecyclerView.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun applyFilters() {
        var filtered = allTransactions

        // Type filter
        if (currentTypeFilter != "All") {
            filtered = filtered.filter { it.type == currentTypeFilter }
        }

        // Time filter
        filtered = when (currentTimeFilter) {
            "Today" -> filtered.filter { isToday(it.date) }
            "This Week" -> filtered.filter { isThisWeek(it.date) }
            "This Month" -> filtered.filter { isThisMonth(it.date) }
            else -> filtered
        }

        // Category filter
        if (currentCategoryFilter != "All") {
            filtered = filtered.filter { it.category == currentCategoryFilter }
        }

        Log.d(TAG, "Filtered transactions: ${filtered.size}")

        if (filtered.isNotEmpty()) {
            binding.transactionsRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            transactionAdapter.submitList(filtered)
        } else {
            binding.transactionsRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        }
    }

    private fun isToday(date: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == transactionDate.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == transactionDate.get(Calendar.MONTH)
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: "default"
        Log.d(TAG, "Retrieved user ID from SharedPreferences: $userId")
        return userId
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing transactions")
        loadTransactions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}