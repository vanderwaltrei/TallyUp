package za.ac.iie.TallyUp.models

data class BudgetCategory(
    val name: String,
    var budgeted: Double = 0.0,   // what user planned to spend
    var amount: Double = 0.0,     // what system calculates or actual allocation
    var spent: Double = 0.0,
    val isCustom: Boolean = false
)
