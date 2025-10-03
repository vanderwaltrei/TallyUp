package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import za.ac.iie.TallyUp.databinding.FragmentGoalsBinding
import za.ac.iie.TallyUp.R

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)

        // ---- Initial visibility ----
        binding.emptyState.visibility = View.VISIBLE
        binding.createGoalPage.visibility = View.GONE

        // ---- Button: "Create Your First Goal" ----
        binding.createFirstGoalButton.setOnClickListener {
            binding.emptyState.visibility = View.GONE
            binding.createGoalPage.visibility = View.VISIBLE
        }

        // ---- Header back button ----
        binding.backButton.setOnClickListener {
            if (binding.createGoalPage.visibility == View.VISIBLE) {
                // If "Create Goal" page is open, go back to empty state
                binding.createGoalPage.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            } else {
                // If already in empty state, switch to Home tab
                val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.navigation_home // your Home tab ID
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}