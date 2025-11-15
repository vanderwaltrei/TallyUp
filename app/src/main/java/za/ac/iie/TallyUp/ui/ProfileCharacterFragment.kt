package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.FragmentProfileCharacterBinding

@Suppress("RedundantOverride")
class ProfileCharacterFragment : Fragment() {

    private var _binding: FragmentProfileCharacterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileCharacterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Call your new function
        updateCoinCount()
    }

    @android.annotation.SuppressLint("SetTextI18n")
    private fun updateCoinCount() {
        // Get the actual coin count from your CharacterManager
        val coins = za.ac.iie.TallyUp.utils.CharacterManager.getCoins(requireContext())

        // Set the text of your TextView
        binding.coinsCountText.text = "$coins Coins"
    }

    override fun onResume() {
        super.onResume()
        // Refresh the coin count in case the user earns/spends
        // coins and switches back to this tab.
        if (_binding != null) {
            updateCoinCount()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}