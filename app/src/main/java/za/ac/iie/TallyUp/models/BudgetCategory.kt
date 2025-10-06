package za.ac.iie.TallyUp.models

data class BudgetCategory(
    val name: String,
    val budgeted: Double,  // Renamed from 'amount' to match DashboardFragment usage
    val spent: Double = 0.0  // Add this property for DashboardFragment
) {
    // Add compatibility property if needed
    val amount: Double
        get() = budgeted
}