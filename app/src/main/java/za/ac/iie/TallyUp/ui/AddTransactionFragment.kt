package za.ac.iie.TallyUp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.databinding.FragmentAddTransactionBinding
import za.ac.iie.TallyUp.models.TransactionViewModel
import za.ac.iie.TallyUp.models.TransactionViewModelFactory

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private var selectedCategoryName: String? = null
    private var selectedType: String = "Expense"
    private val selectedPhotoUris = mutableListOf<String>()
    private lateinit var adapter: CategoryAdapter

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedPhotoUris.clear()
            val data = result.data

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri.toString()
                    selectedPhotoUris.add(uri)
                }
            } ?: data?.data?.let { uri ->
                selectedPhotoUris.add(uri.toString())
            }

            Toast.makeText(requireContext(), "${selectedPhotoUris.size} photo(s) selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao()
        val factory = TransactionViewModelFactory(transactionDao)
        transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUserId = getCurrentUserId()
        setupCategoryGrid(currentUserId)

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.expenseButton.setOnClickListener {
            selectedType = "Expense"
            Toast.makeText(requireContext(), "Expense selected", Toast.LENGTH_SHORT).show()
        }

        binding.incomeButton.setOnClickListener {
            selectedType = "Income"
            Toast.makeText(requireContext(), "Income selected", Toast.LENGTH_SHORT).show()
        }

        binding.photoUploadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            photoPickerLauncher.launch(intent)
        }

        binding.saveButton.setOnClickListener {
            val amountText = binding.amountInput.text.toString()
            val descriptionText = binding.descriptionInput.text.toString().trim()
            val amount = amountText.toDoubleOrNull()
            val description = if (descriptionText.isBlank()) null else descriptionText
            val type = selectedType
            val selectedCategory = selectedCategoryName ?: ""
            val photoUris = selectedPhotoUris.toList()
            val selectedDate = System.currentTimeMillis()

            if (amount == null || selectedCategory.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a valid amount and category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            transactionViewModel.addTransaction(
                type = type,
                amount = amount,
                category = selectedCategory,
                description = description,
                photoUris = photoUris,
                date = selectedDate,
                userId = currentUserId
            )

            Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun setupCategoryGrid(currentUserId: String) {
        val categoryDao = AppDatabase.getDatabase(requireContext()).categoryDao()

        lifecycleScope.launch {
            val categories = categoryDao.getCategoriesForUser(currentUserId).toMutableList()
            categories.add(Category(name = "Add New", type = "Expense", color = "#E0E0E0", userId = currentUserId))

            adapter = CategoryAdapter(
                categories,
                onCategorySelected = { selectedCategory ->
                    selectedCategoryName = selectedCategory.name
                },
                onAddNewClicked = {
                    AddCategoryDialogFragment(
                        userId = currentUserId,
                        defaultType = selectedType, // ← pass the current toggle value
                        onCategoryCreated = { newCategory ->
                            lifecycleScope.launch {
                                categoryDao.insertCategory(newCategory)
                                val updatedCategories = adapter.categories.toMutableList()
                                updatedCategories.add(updatedCategories.size - 1, newCategory)
                                adapter.updateCategories(updatedCategories)
                                selectedCategoryName = newCategory.name
                            }
                        }
                    ).show(parentFragmentManager, "AddCategoryDialog")
                }
            )

            binding.categoryGrid.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.categoryGrid.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}