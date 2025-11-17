@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import za.ac.iie.TallyUp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = ProfilePagerAdapter(this)
        binding.profileViewPager.adapter = adapter

        TabLayoutMediator(binding.profileTabs, binding.profileViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Profile"
                1 -> "Character"
                2 -> "Rewards"
                3 -> "Settings"
                else -> null
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ProfilePagerAdapter(fragment: Fragment) :
        androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProfileMainFragment()
                1 -> ProfileCharacterFragment()
                2 -> ProfileRewardsFragment()
                3 -> ProfileSettingsFragment()
                else -> ProfileMainFragment()
            }
        }
    }
}