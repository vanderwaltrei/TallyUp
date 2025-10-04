package za.ac.iie.TallyUp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.DatabaseProvider
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

        // Load user data
        loadUserData()

        // Setup ViewPager with tabs
        setupViewPager()

        // Setup save button
        binding.saveProfileButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadUserData() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)

        if (email != null) {
            val db = DatabaseProvider.getDatabase(requireContext())

            lifecycleScope.launch {
                val user = db.userDao().getUserByEmail(email)

                if (user != null) {
                    val fullName = "${user.firstName} ${user.lastName}"
                    binding.userName.text = fullName
                    binding.firstNameInput.setText(user.firstName)
                    binding.lastNameInput.setText(user.lastName)
                } else {
                    binding.userName.text = getString(R.string.user_not_found)
                }
            }
        } else {
            binding.userName.text = getString(R.string.no_user_logged_in)
        }
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

    private fun saveProfileChanges() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)

        if (email != null) {
            val db = DatabaseProvider.getDatabase(requireContext())
            val firstName = binding.firstNameInput.text.toString().trim()
            val lastName = binding.lastNameInput.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                lifecycleScope.launch {
                    val user = db.userDao().getUserByEmail(email)
                    user?.let {
                        val updatedUser = it.copy(firstName = firstName, lastName = lastName)
                        db.userDao().insertUser(updatedUser) // This will update due to same primary key

                        // Update displayed name
                        binding.userName.text = "$firstName $lastName"

                        // Show success message
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Profile updated successfully!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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