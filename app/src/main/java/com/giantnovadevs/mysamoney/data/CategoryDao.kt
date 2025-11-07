package com.giantnovadevs.mysamoney.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<Category>>


    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<Category>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long


    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}