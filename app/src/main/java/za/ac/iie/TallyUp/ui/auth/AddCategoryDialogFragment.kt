@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.databinding.DialogueAddCategoryBinding
import za.ac.iie.TallyUp.ui.SwatchAdapter

class AddCategoryDialogFragment(
    private val userId: String,
    private val defaultType: String,
    private val onCategoryCreated: (Category) -> Unit
) : DialogFragment() {

    private var _binding: DialogueAddCategoryBinding? = null
    private val binding get() = _binding!!
    private var selectedColor: String = "#E0E0E0" // Default swatch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogueAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swatchColors = listOf(
            "#FFB085", "#A3D5FF", "#B2E2B2", "#FFF4A3",
            "#FFB6C1", "#D1B3FF", "#E0E0E0", "#FFD700"
        )

        val swatchAdapter = SwatchAdapter(swatchColors) { color ->
            selectedColor = color
        }

        binding.colorSwatchGrid.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.colorSwatchGrid.adapter = swatchAdapter

        binding.categoryNameInput.setOnEditorActionListener { _, _, _ ->
            createCategory()
            true
        }

        binding.createCategoryButton.setOnClickListener {
            createCategory()
        }
    }

    private fun createCategory() {
        val name = binding.categoryNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        // Create new category with proper defaults
        val newCategory = Category(
            name = name,
            type = defaultType, // Use the passed type (Income/Expense)
            color = selectedColor,
            userId = userId
        )

        onCategoryCreated(newCategory)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}