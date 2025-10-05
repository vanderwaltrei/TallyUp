package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.DatabaseProvider
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.FragmentAddTransactionBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*
import za.ac.iie.TallyUp.R


class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Temporary default type  Income/Expense toggle
    private var selectedType: String = "Expense"
    private var selectedCategory: String? = null
    private var selectedDate: Long? = null
    private var selectedPhotoUri: Uri? = null

    companion object {
        private const val REQUEST_PHOTO = 1001
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set default date to today
        selectedDate = System.currentTimeMillis()
        binding.dateDisplay.text = formatDate(selectedDate!!)

        // Date picker logic
        binding.dateDisplay.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select transaction date")
                .setSelection(selectedDate)
                .build()

            picker.show(parentFragmentManager, "datePicker")
            picker.addOnPositiveButtonClickListener {
                selectedDate = it
                binding.dateDisplay.text = formatDate(it)
            }
        }

        // Back button
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Photo picker
        binding.photoPreview.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_PHOTO)
        }

        // Income/Expense toggle
        binding.incomeButton.setOnClickListener {
            selectedType = "Income"
            updateTypeUI()
        }

        binding.expenseButton.setOnClickListener {
            selectedType = "Expense"
            updateTypeUI()
        }

        // Category selection (example: grid buttons)
        binding.categoryFood.setOnClickListener {
            selectedCategory = "Food"
        }

        binding.categoryTransport.setOnClickListener {
            selectedCategory = "Transport"
        }

        // Save button logic
        binding.saveButton.setOnClickListener {
            val amount = binding.amountInput.text.toString().toDoubleOrNull()
            val description = binding.descriptionInput.text.toString().trim()
            val photoUri = selectedPhotoUri?.toString()
            val date = selectedDate ?: System.currentTimeMillis()

            if (amount == null || selectedCategory.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter amount and select category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = Transaction(
                amount = amount,
                type = selectedType,
                category = selectedCategory!!,
                description = if (description.isEmpty()) null else description,
                photoUri = photoUri,
                date = date
            )

            val db = DatabaseProvider.getDatabase(requireContext())
            lifecycleScope.launch {
                db.transactionDao().insertTransaction(transaction)
                Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            selectedPhotoUri = data?.data
            binding.photoPreview.setImageURI(selectedPhotoUri)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun updateTypeUI() {
        val activeBg = requireContext().getColor(R.color.success)
        val inactiveBg = requireContext().getColor(R.color.background)
        val activeText = requireContext().getColor(R.color.background) // match background
        val defaultText = requireContext().getColor(R.color.foreground) // default text color

        if (selectedType == "Income") {
            binding.incomeButton.setBackgroundColor(activeBg)
            binding.incomeButton.setTextColor(activeText)

            binding.expenseButton.setBackgroundColor(inactiveBg)
            binding.expenseButton.setTextColor(defaultText)
        } else {
            binding.expenseButton.setBackgroundColor(activeBg)
            binding.expenseButton.setTextColor(activeText)

            binding.incomeButton.setBackgroundColor(inactiveBg)
            binding.incomeButton.setTextColor(defaultText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
