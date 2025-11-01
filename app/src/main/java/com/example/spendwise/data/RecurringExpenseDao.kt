package com.example.spendwise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Upsert
    suspend fun upsert(expense: RecurringExpense)

    @Upsert
    suspend fun upsertAll(expenses: List<RecurringExpense>)

    @Delete
    suspend fun delete(expense: RecurringExpense)

    /**
     * Gets all recurring expenses, ordered by the next due date.
     * This is for the UI to display them.
     */
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<RecurringExpense>>

    /**
     * Gets all expenses that are due to be processed *right now*.
     * This is NOT a Flow. It's a one-time list for our background job.
     * @param todayTimestamp The current day's timestamp.
     */
    @Query("SELECT * FROM recurring_expenses WHERE nextDueDate <= :todayTimestamp")
    suspend fun getDueExpenses(todayTimestamp: Long): List<RecurringExpense>
}