package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.FragmentDashboardBinding
import za.ac.iie.TallyUp.model.AppState
import za.ac.iie.TallyUp.data.AppRepository

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
        return binding.root
    }

    private fun setupUI() {
        // Welcome message
        binding.welcomeText.text = "Hey ${appState.user?.firstName ?: "there"}!"

        // Available to spend
        val totalBudget = appState.budgetCategories.sumOf { it.budgeted }
        val totalSpent = appState.budgetCategories.sumOf { it.spent }
        val availableToSpend = totalBudget - totalSpent

        binding.availableAmount.text = "$${"%.2f".format(availableToSpend)}"
        binding.availableSubtitle.text = "Available to Spend"

        // Progress circle (simplified)
        val spentPercentage = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
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
        if (recentTransactions.isEmpty()) {
            binding.recentSection.visibility = View.GONE
        } else {
            binding.recentSection.visibility = View.VISIBLE
            // Setup recycler view for transactions (to be implemented)
        }

        // Goals
        val goals = appState.goals.take(2)
        if (goals.isEmpty()) {
            binding.goalsSection.visibility = View.GONE
        } else {
            binding.goalsSection.visibility = View.VISIBLE
            // Setup recycler view for goals (to be implemented)
        }

        // Character display
        appState.user?.character?.let { character ->
            val characterRes = if (character.type == za.ac.iie.TallyUp.model.CharacterType.FEMALE) {
                za.ac.iie.TallyUp.R.drawable.character_female
            } else {
                za.ac.iie.TallyUp.R.drawable.character_male
            }
            binding.characterImage.setImageResource(characterRes)

            if (character.mood == za.ac.iie.TallyUp.model.Mood.SAD) {
                binding.moodIndicator.visibility = View.VISIBLE
            } else {
                binding.moodIndicator.visibility = View.GONE
            }

            binding.coinsText.text = "${appState.user?.coins ?: 0}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}