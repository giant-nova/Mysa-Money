package com.giantnovadevs.mysamoney.worker

import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.data.Frequency
import com.giantnovadevs.mysamoney.data.RecurringExpense
import java.time.LocalDate
import java.time.ZoneId
import androidx.room.withTransaction

class ExpenseWorkerRepository(private val db: AppDatabase) {

    /**
     * This is the main function the worker will call.
     * It finds all due expenses, adds them as regular expenses,
     * and updates their next due date.
     */
    suspend fun processDueExpenses(): List<RecurringExpense> {
        // Get today's date at the start of the day
        val todayTimestamp = getStartOfTodayTimestamp()

        // 1. Find all recurring expenses that are due on or before today
        val dueExpenses = db.recurringExpenseDao().getDueExpenses(todayTimestamp)

        if (dueExpenses.isEmpty()) {
            return emptyList()
        }

        // 2. Process each due expense
        val newExpensesToInsert = mutableListOf<Expense>()
        val updatedRecurringExpenses = mutableListOf<RecurringExpense>()

        dueExpenses.forEach { recurringExpense ->
            // Add a new item to the regular 'expenses' table
            newExpensesToInsert.add(
                Expense(
                    amount = recurringExpense.amount,
                    categoryId = recurringExpense.categoryId,
                    note = recurringExpense.note,
                    date = recurringExpense.nextDueDate
                    // We skipped the 'accountId' feature, so this is correct
                )
            )

            // 3. Calculate the *next* due date
            val nextDueDate = calculateNextDueDate(
                lastDueDate = recurringExpense.nextDueDate,
                frequency = recurringExpense.frequency
            )

            // 4. Update the recurring expense with its new due date
            updatedRecurringExpenses.add(
                recurringExpense.copy(nextDueDate = nextDueDate)
            )
        }

        // 5. Save all changes in a single database transaction
        // This ensures if one part fails, nothing is saved (all or nothing)
        db.withTransaction {
            db.expenseDao().insertAll(newExpensesToInsert) // Use insertAll for efficiency
            db.recurringExpenseDao().upsertAll(updatedRecurringExpenses) // Use upsertAll
        }

        return dueExpenses
    }

    /**
     * Calculates the next due date based on the last one and the frequency.
     */
    private fun calculateNextDueDate(lastDueDate: Long, frequency: Frequency): Long {
        val lastDate = LocalDate.ofEpochDay(lastDueDate / (1000 * 60 * 60 * 24))

        val nextDate = when (frequency) {
            Frequency.WEEKLY -> lastDate.plusWeeks(1)
            Frequency.MONTHLY -> lastDate.plusMonths(1)
            Frequency.YEARLY -> lastDate.plusYears(1)
        }

        // Convert back to timestamp
        return nextDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
    }

    private fun getStartOfTodayTimestamp(): Long {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
    }
}

// We need to add `insertAll` and `upsertAll` to our DAOs