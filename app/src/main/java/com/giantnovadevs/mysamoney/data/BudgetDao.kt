package com.giantnovadevs.mysamoney.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    /**
     * @Upsert will Insert or Replace a budget.
     * This is perfect for setting/updating a budget.
     */
    @Upsert
    suspend fun upsert(budget: Budget)

    /**
     * Gets all budgets for a specific month.
     * e.g., "2025-10"
     */
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>

    /**
     * Gets a single budget for a specific category and month.
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear")
    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<Budget?>
}