package za.ac.iie.TallyUp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.databinding.FragmentAddTransactionBinding
import za.ac.iie.TallyUp.models.TransactionViewModel
import za.ac.iie.TallyUp.models.TransactionViewModelFactory
import java.io.File

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private var selectedCategoryName: String? = null
    private var selectedType: String = "Expense"
    private val selectedPhotoUris = mutableListOf<String>()
    private lateinit var adapter: CategoryAdapter
    private var cameraPhotoUri: Uri? = null

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val data = result.data
                val newUris = mutableListOf<String>()

                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri.toString()
                        newUris.add(uri)
                    }
                } ?: data?.data?.let { uri ->
                    newUris.add(uri.toString())
                } ?: cameraPhotoUri?.let { uri ->
                    newUris.add(uri.toString())
                    cameraPhotoUri = null // Clear after use
                }

                val remainingSlots = 3 - selectedPhotoUris.size
                selectedPhotoUris.addAll(newUris.take(remainingSlots))

                Toast.makeText(requireContext(), "${selectedPhotoUris.size} photo(s) attached", Toast.LENGTH_SHORT).show()

                // Update preview strip
                val previewStrip = binding.photoPreview
                val photoViews = listOf(binding.photo1, binding.photo2, binding.photo3)

                photoViews.forEachIndexed { index, imageView ->
                    if (index < selectedPhotoUris.size) {
                        // Safe: URIs come from system picker or camera
                        imageView.setImageURI(selectedPhotoUris[index].toUri())
                        imageView.visibility = View.VISIBLE
                    } else {
                        imageView.visibility = View.GONE
                    }
                }

                previewStrip.visibility = if (selectedPhotoUris.isNotEmpty()) View.VISIBLE else View.GONE
            }

            android.app.Activity.RESULT_CANCELED -> {
                Toast.makeText(requireContext(), "No photos selected", Toast.LENGTH_SHORT).show()
            }
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

        binding.transactionTypeGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.expense_button -> {
                        selectedType = "Expense"
                        Toast.makeText(requireContext(), "Expense selected", Toast.LENGTH_SHORT).show()
                    }
                    R.id.income_button -> {
                        selectedType = "Income"
                        Toast.makeText(requireContext(), "Income selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.transactionTypeGroup.check(R.id.expense_button)


        binding.photoUploadButton.setOnClickListener {
            // Step 1: Create a file to store the camera photo
            val photoFile = File(
                requireContext().getExternalFilesDir("Pictures"),
                "photo_${System.currentTimeMillis()}.jpg"
            )

            // Create a URI for the file using FileProvider
            cameraPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "za.ac.iie.TallyUp.fileprovider", // must match your manifest
                photoFile
            )

            // Create the camera intent and pass the URI
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // ✅ Added for safety
            }

            // Create the gallery intent
            val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Combine both intents into a chooser
            val chooser = Intent.createChooser(galleryIntent, "Select or capture photo")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

            // Launch the chooser
            photoPickerLauncher.launch(chooser)
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

            //  Manual fragment back navigation
            requireActivity().supportFragmentManager.popBackStack()
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