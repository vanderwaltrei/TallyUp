package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.DatabaseProvider
import za.ac.iie.TallyUp.databinding.FragmentProfileMainBinding
import za.ac.iie.TallyUp.utils.CharacterManager

class ProfileMainFragment : Fragment() {

    private var _binding: FragmentProfileMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set character image dynamically using CharacterManager
        val characterDrawable = CharacterManager.getCharacterDrawable(requireContext())
        binding.characterDisplay.setImageResource(characterDrawable)

        // Set coins dynamically using CharacterManager
        val coins = CharacterManager.getCoins(requireContext())
        binding.coinsCount.text = coins.toString()

        loadUserData()

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

                    // Save first name for welcome messages in other fragments
                    prefs.edit().putString("userFirstName", user.firstName).apply()
                } else {
                    binding.userName.text = getString(R.string.user_not_found)
                }
            }
        } else {
            binding.userName.text = getString(R.string.no_user_logged_in)
        }
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
                        db.userDao().updateUser(updatedUser)

                        @SuppressLint("SetTextI18n")
                        binding.userName.text = "$firstName $lastName"

                        // Update the stored first name
                        prefs.edit().putString("userFirstName", firstName).apply()

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.profile_updated_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter both first and last name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "No user logged in",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}