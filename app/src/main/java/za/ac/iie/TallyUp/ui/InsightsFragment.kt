package za.ac.iie.TallyUp.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.FragmentInsightsBinding

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupClickListeners()
        loadData()
    }

    private fun setupViews() {
        // Setup tab layout
        binding.analysisTabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Handle tab selection - you can update the chart/data here
                when (tab?.position) {
                    0 -> showLast7DaysData()
                    1 -> showTrendData()
                    2 -> showDailyData()
                    3 -> showCategoriesData()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.addFirstTransactionBtn.setOnClickListener {
            // Navigate to add transaction screen
            // For now, just hide the no data card when clicked
            binding.noDataCard.visibility = View.GONE
        }
    }

    private fun loadData() {
        // Load data from repository
        val hasTransactions = false // Replace with actual data check

        if (hasTransactions) {
            binding.noDataCard.visibility = View.GONE
            // Load and display actual data
        } else {
            binding.noDataCard.visibility = View.VISIBLE
        }
    }

    private fun showLast7DaysData() {
        // Implement last 7 days data display
    }

    private fun showTrendData() {
        // Implement trend data display
    }

    private fun showDailyData() {
        // Implement daily data display
    }

    private fun showCategoriesData() {
        // Implement categories data display
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}