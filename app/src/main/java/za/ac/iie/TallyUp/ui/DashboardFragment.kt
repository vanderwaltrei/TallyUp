package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentDashboardBinding
import za.ac.iie.TallyUp.models.AppState
import za.ac.iie.TallyUp.models.CharacterType
import za.ac.iie.TallyUp.models.Mood
import za.ac.iie.TallyUp.data.AppRepository
import za.ac.iie.TallyUp.ui.budget.BudgetDashboardFragment
import za.ac.iie.TallyUp.ui.insights.InsightsFragment

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var appState: AppState

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        repository = AppRepository(requireContext())
        appState = repository.loadAppState()

        setupUI()
        setupQuickActions()
        return binding.root
    }

    private fun setupQuickActions() {
        binding.budgetDashboardCard.setOnClickListener {
            navigateToBudgetDashboard()
        }

        binding.insightsCard.setOnClickListener {
            navigateToInsights()
        }
    }

    private fun navigateToInsights() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, InsightsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToBudgetDashboard() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, BudgetDashboardFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupUI() {
        // Welcome message
        binding.welcomeText.text = "Hey ${appState.user?.firstName ?: "there"}!"

        // Available to spend
        val totalBudget = appState.budgetCategories.sumOf { it.budgeted }
        val totalSpent = appState.budgetCategories.sumOf { it.spent }
        val availableToSpend = totalBudget - totalSpent

        binding.availableAmount.text = "R${"%.2f".format(availableToSpend)}"
        binding.availableSubtitle.text = "Available to Spend"

        // Progress circle (simplified)
        val spentPercentage =
            if (totalBudget > 0.0) ((totalSpent / totalBudget) * 100.0).toInt() else 0
        binding.progressText.text = "${spentPercentage}%"

        // Status indicator
        val statusText = when {
            spentPercentage < 60 -> "On Track!"
            spentPercentage < 80 -> "Watch It!"
            else -> "Almost There!"
        }
        binding.statusText.text = statusText

        // Recent transactions
        val recentTransactions = appState.transactions.take(3)
        binding.recentSection.visibility = if (recentTransactions.isEmpty()) View.GONE else View.VISIBLE

        // Goals
        val goals = appState.goals.take(2)
        binding.goalsSection.visibility = if (goals.isEmpty()) View.GONE else View.VISIBLE

        // Character display
        appState.user?.character?.let { character ->
            val characterRes = if (character.type == CharacterType.FEMALE) {
                R.drawable.character_female
            } else {
                R.drawable.character_male
            }
            binding.characterImage.setImageResource(characterRes)

            binding.moodIndicator.visibility =
                if (character.mood == Mood.SAD) View.VISIBLE else View.GONE

            binding.coinsText.text = "${appState.user?.coins ?: 0}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
