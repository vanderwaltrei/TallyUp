@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentProfileSettingsBinding

class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup logout functionality
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    @SuppressLint("UseKtx")
    private fun logoutUser() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("loggedInEmail").apply()

        // Navigate to login screen
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}