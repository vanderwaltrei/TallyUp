@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentDashboardBinding
import za.ac.iie.TallyUp.models.AppState
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.utils.CharacterManager
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.GoalDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appState: AppState
    private lateinit var goalDatabase: GoalDatabase
    private lateinit var appDatabase: AppDatabase
    private var goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    companion object {
        private const val TAG = "DashboardFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        goalDatabase = GoalDatabase.getDatabase(requireContext())
        appDatabase = AppDatabase.getDatabase(requireContext())
        appState = repository.loadAppState()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGoalRecyclerView()
        setupUI()
        setupQuickActions()
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        loadRecentTransactions()
    }

    private fun setupGoalRecyclerView() {
        goalAdapter = GoalAdapter(
            goalsList,
            onAddMoneyClicked = { navigateToGoalsFragment() },
            onCompleteGoalClicked = { navigateToGoalsFragment() },
            onEditGoalClicked = { navigateToGoalsFragment() },
            onDeleteGoalClicked = { navigateToGoalsFragment() }
        )

        binding.goalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.goalsRecyclerView.adapter = goalAdapter
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentTransactions() {
        val userId = getCurrentUserId()

        lifecycleScope.launch {
            try {
                val allTransactions = withContext(Dispatchers.IO) {
                    appDatabase.transactionDao().getTransactionsForUser(userId)
                }

                val recentTransactions = allTransactions
                    .sortedByDescending { it.date }
                    .take(3)

                if (recentTransactions.isNotEmpty()) {
                    binding.noTransactionsText.visibility = View.GONE
                    binding.recentTransactionsContainer.visibility = View.VISIBLE
                    binding.recentTransactionsContainer.removeAllViews()

                    recentTransactions.forEach { transaction ->
                        val itemView = layoutInflater.inflate(
                            R.layout.item_transaction,
                            binding.recentTransactionsContainer,
                            false
                        )

                        itemView.findViewById<TextView>(R.id.transaction_description).text =
                            "${transaction.category} - ${transaction.type}"

                        itemView.findViewById<TextView>(R.id.transaction_date).text =
                            formatDate(transaction.date)

                        itemView.findViewById<TextView>(R.id.transaction_amount).text =
                            "R ${"%.2f".format(transaction.amount)}"

                        itemView.findViewById<TextView>(R.id.photo_status).text =
                            if (transaction.photoUris.isNotEmpty()) "Photos attached" else "No photos"

                        binding.recentTransactionsContainer.addView(itemView)
                    }
                } else {
                    binding.noTransactionsText.visibility = View.VISIBLE
                    binding.recentTransactionsContainer.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent transactions", e)
                binding.noTransactionsText.visibility = View.VISIBLE
                binding.recentTransactionsContainer.visibility = View.GONE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadGoalsFromDatabase() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.IO).launch {
            val goalsFromDb = goalDatabase.goalDao().getGoalsByUser(userId)
            goalsList.clear()
            goalsList.addAll(goalsFromDb.take(2))

            withContext(Dispatchers.Main) {
                goalAdapter.notifyDataSetChanged()
                updateGoalsVisibility()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadTransactionsAndUpdateBudget() {
        val userId = getCurrentUserId()
        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                appDatabase.transactionDao().getTransactionsForUser(userId)
            }

            val totalExpenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
            val monthlyBudget = appState.budgetCategories.sumOf { it.budgeted }
            val availableToSpend = monthlyBudget - totalExpenses

            val progressPercentage =
                if (monthlyBudget > 0) ((totalExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100) else 0

            updateBudgetUI(availableToSpend, progressPercentage)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBudgetUI(available: Double, progress: Int) {
        binding.availableAmount.text = "R${"%.2f".format(available)}"
        binding.progressText.text = "$progress%"

        val context = requireContext()
        when {
            progress < 60 -> {
                binding.statusText.text = "On Track!"
                binding.statusText.setBackgroundColor(context.getColor(R.color.success_light))
                binding.statusText.setTextColor(context.getColor(R.color.success))
            }
            progress < 80 -> {
                binding.statusText.text = "Watch It!"
                binding.statusText.setBackgroundColor(context.getColor(R.color.warning_light))
                binding.statusText.setTextColor(context.getColor(R.color.warning))
            }
            else -> {
                binding.statusText.text = "Almost There!"
                binding.statusText.setBackgroundColor(context.getColor(R.color.error))
                binding.statusText.setTextColor(context.getColor(R.color.error))
            }
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: ""
    }

    private fun updateGoalsVisibility() {
        binding.noGoalsText.visibility = if (goalsList.isEmpty()) View.VISIBLE else View.GONE
        binding.goalsRecyclerView.visibility = if (goalsList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupQuickActions() {
        binding.budgetDashboardCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BudgetFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.insightsCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InsightsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.goalsSection.setOnClickListener {
            navigateToGoalsFragment()
        }
    }

    private fun navigateToGoalsFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GoalsFragment())
            .addToBackStack(null)
            .commit()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val firstName = prefs.getString("userFirstName", "User")
        val characterName = CharacterManager.getCharacterName(requireContext())

        binding.welcomeText.text = "Hey $firstName! $characterName is here to help!"
        binding.characterImage.setImageResource(
            CharacterManager.getCharacterDrawable(requireContext())
        )
        binding.coinsText.text = CharacterManager.getCoins(requireContext()).toString()
    }

    override fun onResume() {
        super.onResume()
        loadGoalsFromDatabase()
        loadTransactionsAndUpdateBudget()
        loadRecentTransactions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
