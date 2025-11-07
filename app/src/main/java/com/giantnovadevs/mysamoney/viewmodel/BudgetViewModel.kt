package com.giantnovadevs.mysamoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).budgetDao()
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    // Helper to get the current month as "YYYY-MM"
    private fun getCurrentMonthYear(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    // This state holds the currently viewed month, e.g., "2025-10"
    // For now, it just defaults to the current month.
    val selectedMonthYear = MutableStateFlow(getCurrentMonthYear())

    fun getMonthYearDisplay(): String {
        return YearMonth.parse(selectedMonthYear.value, monthFormatter)
            .format(displayFormatter)
    }

    /**
     * Moves the selected month to the next month.
     */
    fun nextMonth() {
        val current = YearMonth.parse(selectedMonthYear.value, monthFormatter)
        selectedMonthYear.value = current.plusMonths(1).format(monthFormatter)
    }

    /**
     * Moves the selected month to the previous month.
     */
    fun prevMonth() {
        val current = YearMonth.parse(selectedMonthYear.value, monthFormatter)
        selectedMonthYear.value = current.minusMonths(1).format(monthFormatter)
    }

    // This is a "hot flow" that "reacts" to changes in selectedMonthYear
    // If you change selectedMonthYear, this flow automatically re-queries the DB
    val budgetsForSelectedMonth = selectedMonthYear.flatMapLatest { monthYear ->
        dao.getBudgetsForMonth(monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Sets or updates a budget for a given category in the currently selected month.
     */
    fun setBudget(categoryId: Int, amount: Double) {
        viewModelScope.launch {
            val budget = Budget(
                categoryId = categoryId,
                monthYear = selectedMonthYear.value, // "2025-10"
                amount = amount
            )
            dao.upsert(budget)
        }
    }
}