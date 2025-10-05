package za.ac.iie.TallyUp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {

    // For prepopulation on startup
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    // For inserting a category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    // For inserting multiple categories
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    // For filtering by type
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    suspend fun getCategoriesByType(type: String): List<Category>

    // For checking duplicates before adding new category
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name AND type = :type)")
    suspend fun categoryExists(name: String, type: String): Boolean

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesForUser(userId: String): List<Category>
}
