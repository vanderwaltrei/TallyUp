package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.databinding.FragmentAddTransactionBinding
import za.ac.iie.TallyUp.R
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button → go back to previous screen
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Toggle buttons for Expense / Income
        binding.expenseButton.setOnClickListener {
            Toast.makeText(requireContext(), "Expense selected", Toast.LENGTH_SHORT).show()
        }

        binding.incomeButton.setOnClickListener {
            Toast.makeText(requireContext(), "Income selected", Toast.LENGTH_SHORT).show()
        }

        // Upload photo button
        binding.photoUploadButton.setOnClickListener {
            Toast.makeText(requireContext(), "Upload photo clicked", Toast.LENGTH_SHORT).show()
        }

        // Save transaction button
        binding.saveButton.setOnClickListener {
            val amount = binding.amountInput.text.toString()
            val description = binding.descriptionInput.text.toString()

            if (amount.isEmpty() || amount == "0") {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show()

            // Go back to previous screen
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
