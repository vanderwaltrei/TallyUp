package za.ac.iie.TallyUp.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.models.BudgetCategory

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ============= USER MANAGEMENT =============

    suspend fun signUp(email: String, password: String, firstName: String, lastName: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")

            // Save user profile to Firestore
            val userProfile = hashMapOf(
                "email" to email,
                "firstName" to firstName,
                "lastName" to lastName,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .set(userProfile)
                .await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserProfile(): Result<Map<String, Any>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val data = document.data ?: throw Exception("User profile not found")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= TRANSACTIONS =============

    suspend fun addTransaction(transaction: Transaction): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val transactionData = hashMapOf(
                "amount" to transaction.amount,
                "type" to transaction.type,
                "category" to transaction.category,
                "description" to (transaction.description ?: ""),
                "photoUris" to transaction.photoUris,
                "date" to transaction.date,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection("transactions")
                .add(transactionData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactions(): Result<List<Transaction>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val snapshot = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    Transaction(
                        id = 0, // Not used with Firebase
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = doc.getString("type") ?: "Expense",
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description"),
                        photoUris = (doc.get("photoUris") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        date = doc.getLong("date") ?: 0L,
                        userId = userId
                    )
                } catch (e: Exception) {
                    null // Skip malformed transactions
                }
            }

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= CATEGORIES =============

    suspend fun addCategory(category: Category): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val categoryData = hashMapOf(
                "name" to category.name,
                "type" to category.type,
                "color" to category.color,
                "userId" to userId
            )

            val docRef = firestore.collection("categories")
                .add(categoryData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<List<Category>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val snapshot = firestore.collection("categories")
                .whereIn("userId", listOf(userId, "default"))
                .get()
                .await()

            val categories = snapshot.documents.mapNotNull { doc ->
                try {
                    Category(
                        id = 0, // Not used with Firebase
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "Expense",
                        color = doc.getString("color") ?: "#E0E0E0",
                        userId = doc.getString("userId") ?: "default"
                    )
                } catch (e: Exception) {
                    null // Skip malformed categories
                }
            }

            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= GOALS =============

    suspend fun addGoal(goal: Goal): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val goalData = hashMapOf(
                "name" to goal.name,
                "description" to goal.description,
                "target" to goal.target,
                "minimum" to goal.minimum,
                "current" to goal.current,
                "deadline" to goal.deadline,
                "createdAt" to goal.createdAt,
                "userId" to userId
            )

            val docRef = firestore.collection("goals")
                .add(goalData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goalId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("goals")
                .document(goalId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return try {
            firestore.collection("goals")
                .document(goalId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoals(): Result<List<Goal>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val snapshot = firestore.collection("goals")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val goals = snapshot.documents.mapNotNull { doc ->
                try {
                    Goal(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        target = doc.getDouble("target") ?: 0.0,
                        minimum = doc.getDouble("minimum") ?: 0.0,
                        current = doc.getDouble("current") ?: 0.0,
                        deadline = doc.getString("deadline") ?: "",
                        createdAt = doc.getString("createdAt") ?: "",
                        userId = userId
                    )
                } catch (e: Exception) {
                    null // Skip malformed goals
                }
            }

            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= BUDGET CATEGORIES =============

    suspend fun saveBudgetCategories(categories: List<BudgetCategory>): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            // Convert BudgetCategory objects to maps
            val categoriesData = categories.map { category ->
                hashMapOf(
                    "name" to category.name,
                    "budgeted" to category.budgeted,
                    "spent" to category.spent
                )
            }

            val budgetData = hashMapOf(
                "categories" to categoriesData,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("budgets")
                .document(userId)
                .set(budgetData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBudgetCategories(): Result<List<BudgetCategory>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No user logged in")

            val document = firestore.collection("budgets")
                .document(userId)
                .get()
                .await()

            val categoriesData = document.get("categories") as? List<*> ?: emptyList<Any>()

            val categories = categoriesData.mapNotNull { item ->
                try {
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    BudgetCategory(
                        name = map["name"] as? String ?: "",
                        budgeted = (map["budgeted"] as? Number)?.toDouble() ?: 0.0,
                        spent = (map["spent"] as? Number)?.toDouble() ?: 0.0
                    )
                } catch (e: Exception) {
                    null // Skip malformed budget categories
                }
            }

            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}