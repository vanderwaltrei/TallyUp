package za.ac.iie.TallyUp.models

data class BudgetCategory(
    val name: String,
    var budgeted: Double,
    var spent: Double = 0.0,
    val isCustom: Boolean = false
)