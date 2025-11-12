package com.giantnovadevs.mysamoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Income
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.map

class IncomeViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).incomeDao()

    /**
     * A flow of all incomes, ordered by date.
     */
    val allIncomes = dao.getAllIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * A flow that calculates the total income for the current calendar month.
     */
    val monthlyTotalIncome: StateFlow<Double> = allIncomes.map { list ->
        val now = LocalDate.now()
        // ✅ CORRECTED CODE
        val zoneId = ZoneId.systemDefault()
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay(zoneId).toEpochSecond() * 1000
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59).atZone(zoneId).toEpochSecond() * 1000

        list.filter { it.date in startOfMonth..endOfMonth }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    /**
     * Adds a new income record.
     */
    fun addIncome(amount: Double, note: String, date: LocalDate) {
        viewModelScope.launch {
            // Convert LocalDate to a timestamp
            val timestamp = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val income = Income(
                amount = amount,
                note = note,
                date = timestamp
            )
            dao.insert(income)
        }
    }

    /**
     * Deletes an income record.
     */
    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            dao.delete(income)
        }
    }
}