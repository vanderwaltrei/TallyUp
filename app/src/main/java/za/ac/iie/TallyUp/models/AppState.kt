package za.ac.iie.TallyUp.models

data class AppState(
    val transactions: List<Transaction> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val budgetCycle: String = "Every Month",
    val monthlyIncome: Double = 600.0,
    val budgetCategories: List<BudgetCategory> = listOf(
        BudgetCategory("Food", 200.0),
        BudgetCategory("Transport", 80.0),
        BudgetCategory("Books", 100.0),
        BudgetCategory("Fun", 120.0),
        BudgetCategory("Shopping", 80.0),
        BudgetCategory("Other", 20.0)
    ),
    var user: User? = null,
    val isAuthenticated: Boolean = false
)