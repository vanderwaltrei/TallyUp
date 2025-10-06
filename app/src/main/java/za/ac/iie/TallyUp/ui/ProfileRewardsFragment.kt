@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.FragmentProfileRewardsBinding

@Suppress("RedundantOverride")
class ProfileRewardsFragment : Fragment() {

    private var _binding: FragmentProfileRewardsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Rewards and achievements logic will go here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}