package com.giantnovadevs.mysamoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Frequency
import com.giantnovadevs.mysamoney.data.RecurringExpense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).recurringExpenseDao()

    /**
     * A state flow of all recurring expenses, ordered by the next due date.
     * This is for the UI to display in a list.
     */
    val recurringExpenses = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    /**
     * Calculates the first valid due date that is on or after today.
     */
    private fun calculateFirstDueDate(startDate: LocalDate, frequency: Frequency): LocalDate {
        var nextDate = startDate
        val today = LocalDate.now()

        // If the start date is already today or in the future, use it
        if (!nextDate.isBefore(today)) {
            return nextDate
        }

        // If the start date is in the past, loop forward until
        // we find the next due date that is on or after today.
        while (nextDate.isBefore(today)) {
            nextDate = when (frequency) {
                Frequency.WEEKLY -> nextDate.plusWeeks(1)
                Frequency.MONTHLY -> nextDate.plusMonths(1)
                Frequency.YEARLY -> nextDate.plusYears(1)
            }
        }
        return nextDate
    }


    /**
     * Adds a new recurring expense.
     * The `nextDueDate` is the same as the `startDate` for a new expense.
     */
    fun addRecurringExpense(
        amount: Double,
        categoryId: Int,
        note: String?,
        frequency: Frequency,
        startDate: LocalDate // This is the "anchor date" the user picked
    ) {
        viewModelScope.launch {
            // 1. Calculate the *true* next due date
            val nextDueDate = calculateFirstDueDate(startDate, frequency)

            // 2. Convert to timestamps
            val startTimestamp = startDate.atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC) * 1000
            val nextDueDateTimestamp = nextDueDate.atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC) * 1000

            val expense = RecurringExpense(
                amount = amount,
                categoryId = categoryId,
                note = note,
                frequency = frequency,
                startDate = startTimestamp, // We still save the *original* start date
                nextDueDate = nextDueDateTimestamp // We save the calculated *future* date
            )
            dao.upsert(expense)
        }
    }

    /**
     * Deletes a recurring expense.
     */
    fun deleteRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            dao.delete(expense)
        }
    }
}