package com.giantnovadevs.mysamoney.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>


    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getByCategory(categoryId: Int): Flow<List<Expense>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Delete
    suspend fun delete(expense: Expense)


    @Query("DELETE FROM expenses")
    suspend fun deleteAll()


    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun totalInRange(start: Long, end: Long): Double?


    @Query("""
        SELECT categoryId, SUM(amount) as total
        FROM expenses
        WHERE date BETWEEN :start AND :end
        GROUP BY categoryId
    """)
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE date > :timestamp")
    suspend fun getExpensesAfter(timestamp: Long): List<Expense>

}