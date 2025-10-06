package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.View
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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.data.Category
import androidx.recyclerview.widget.GridLayoutManager
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.models.TransactionViewModel
import za.ac.iie.TallyUp.models.TransactionViewModelFactory
import androidx.lifecycle.ViewModelProvider

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Temporary default type  Income/Expense toggle
    private var selectedType: String = "Expense"
    private var selectedCategory: String? = null
    private var selectedDate: Long? = null
    private val selectedPhotoUris = mutableListOf<Uri>()
    private var cameraPhotoUri: Uri? = null

    companion object {
        private const val REQUEST_GALLERY = 1001
        private const val REQUEST_CAMERA = 1002
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao()
        val factory = TransactionViewModelFactory(transactionDao)
        val transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

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
        binding.photoUploadButton.setOnClickListener {
            showPhotoSourceDialog()
        }

        binding.photoPreview.setOnClickListener {
            showPhotoSourceDialog()
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

        loadCategories()

        val currentUserId = getCurrentUserId()

        // Save button logic
        binding.saveButton.setOnClickListener {
            val amount = binding.amountInput.text.toString().toDoubleOrNull()
            val rawDescription = binding.descriptionInput.text?.toString()?.trim() ?: ""
            val finalDescription = if (rawDescription.isBlank()) null else rawDescription
            val date = selectedDate ?: System.currentTimeMillis()

            if (amount == null || selectedCategory.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter amount and select category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            transactionViewModel.addTransaction(
                type = selectedType,
                amount = amount,
                category = selectedCategory!!,
                description = finalDescription,
                photoUris = selectedPhotoUris.map { it.toString() },
                date = Date(date),
                userId = currentUserId
            )

            Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        val uri = when (requestCode) {
            REQUEST_GALLERY -> data?.data
            REQUEST_CAMERA -> cameraPhotoUri
            else -> null
        }

        uri?.let {
            if (selectedPhotoUris.size >= 3) {
                Toast.makeText(requireContext(), "Maximum 3 photos allowed", Toast.LENGTH_SHORT).show()
            } else {
                selectedPhotoUris.add(it)
                updatePhotoPreview()
            }
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

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Receipt Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        cameraPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }


    private fun updatePhotoPreview() {
        val views = listOf(binding.photo1, binding.photo2, binding.photo3)
        val isFull = selectedPhotoUris.size >= 3
        binding.photoUploadButton.isEnabled = !isFull
        binding.photoPreview.isClickable = !isFull

        if (selectedPhotoUris.isEmpty()) {
            binding.photoPreview.visibility = View.GONE
        } else {
            binding.photoPreview.visibility = View.VISIBLE
            views.forEachIndexed { index, imageView ->
                if (index < selectedPhotoUris.size) {
                    imageView.setImageURI(selectedPhotoUris[index])
                    imageView.visibility = View.VISIBLE

                    imageView.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Remove Photo")
                            .setMessage("Are you sure you want to remove this photo?")
                            .setPositiveButton("Remove") { _, _ ->
                                selectedPhotoUris.removeAt(index)
                                updatePhotoPreview()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                } else {
                    imageView.visibility = View.GONE
                }
            }
        }
    }

    private fun loadCategories() {
        val db = DatabaseProvider.getDatabase(requireContext())
        val currentUserId = getCurrentUserId() // ⬅️ Add this line

        lifecycleScope.launch {
            val categories = db.categoryDao().getCategoriesByType(selectedType)

            val allItems = categories + Category(
                name = "Add New",
                type = selectedType,
                color = "#A3D5FF",
                userId = currentUserId // ⬅️ Include userId here
            )

            binding.categoryGrid.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.categoryGrid.adapter = CategoryAdapter(
                allItems,
                onCategorySelected = { category ->
                    selectedCategory = category.name
                },
                onAddNewClicked = {
                    showAddCategoryDialog()
                }
            )
        }
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_add_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.categoryNameInput)
        val swatchGrid = dialogView.findViewById<RecyclerView>(R.id.colorSwatchGrid)

        val pastelColors = listOf(
            "#FFB085", "#A3D5FF", "#B2E2B2", "#FFF4A3",
            "#FFB6C1", "#D1B3FF", "#E0E0E0"
        )

        var selectedColor = pastelColors.first()

        swatchGrid.layoutManager = GridLayoutManager(requireContext(), 4)
        swatchGrid.adapter = SwatchAdapter(pastelColors) { color ->
            selectedColor = color
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()

                if (name.isNotEmpty()) {
                    val currentUserId = getCurrentUserId()

                    val newCategory = Category(
                        name = name,
                        type = selectedType,
                        color = selectedColor,
                        userId = currentUserId
                    )

                    val db = DatabaseProvider.getDatabase(requireContext())
                    lifecycleScope.launch {
                        db.categoryDao().insertCategory(newCategory)
                        Toast.makeText(requireContext(), "Category added!", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
