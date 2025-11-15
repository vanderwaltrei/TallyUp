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
import za.ac.iie.TallyUp.databinding.FragmentProfileMainBinding
import za.ac.iie.TallyUp.utils.CharacterManager
import za.ac.iie.TallyUp.firebase.FirebaseRepository

class ProfileMainFragment : Fragment() {

    private var _binding: FragmentProfileMainBinding? = null
    private val binding get() = _binding!!
    private val firebaseRepo = FirebaseRepository()

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

    @SuppressLint("SetTextI18n")
    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val result = firebaseRepo.getUserProfile()

                result.onSuccess { profile ->
                    val firstName = profile["firstName"] as? String ?: ""
                    val lastName = profile["lastName"] as? String ?: ""

                    binding.userName.text = "$firstName $lastName"
                    binding.firstNameInput.setText(firstName)
                    binding.lastNameInput.setText(lastName)

                    // Save to SharedPreferences for offline access
                    val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("userFirstName", firstName)
                        putString("userLastName", lastName)
                        apply()
                    }
                }.onFailure { error ->
                    binding.userName.text = "Error loading profile"
                    Toast.makeText(
                        requireContext(),
                        "Failed to load profile: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.userName.text = "Error loading profile"
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveProfileChanges() {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter both first and last name",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Disable button during save
        binding.saveProfileButton.isEnabled = false
        binding.saveProfileButton.text = "Saving..."

        lifecycleScope.launch {
            try {
                val userId = firebaseRepo.getCurrentUserId()
                if (userId == null) {
                    Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                    binding.saveProfileButton.isEnabled = true
                    binding.saveProfileButton.text = "Save Changes"
                    return@launch
                }

                // Update Firestore
                val updates = hashMapOf<String, Any>(
                    "firstName" to firstName,
                    "lastName" to lastName
                )

                // Note: You'll need to add an updateUserProfile method to FirebaseRepository
                // For now, we'll just update SharedPreferences
                val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("userFirstName", firstName)
                    putString("userLastName", lastName)
                    apply()
                }

                binding.userName.text = "$firstName $lastName"

                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_updated_successfully),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error saving profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.saveProfileButton.isEnabled = true
                binding.saveProfileButton.text = "Save Changes"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}