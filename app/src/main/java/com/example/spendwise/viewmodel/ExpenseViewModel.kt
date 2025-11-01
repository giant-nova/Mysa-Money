package com.example.spendwise.viewmodel


import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.spendwise.data.AppDatabase
import com.example.spendwise.data.CategoryTotal
import com.example.spendwise.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.TreeMap

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).expenseDao()

    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addExpense(expense: Expense) = viewModelScope.launch { dao.insert(expense) }

    fun deleteExpense(expense: Expense) = viewModelScope.launch { dao.delete(expense) }

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    val selectedMonthYear = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))

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
    /**
     * A flow that reacts to the selectedMonthYear changing.
     * It calculates the start/end of that month and gets the
     * spending totals for each category.
     */
    val categoryTotalsForSelectedMonth: StateFlow<List<CategoryTotal>> = selectedMonthYear.flatMapLatest { monthYear ->
        // Parse "YYYY-MM" string
        val yearMonth = YearMonth.parse(monthYear)

        // Get start and end of the month in milliseconds
        val startOfMonth = yearMonth.atDay(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC) * 1000

        dao.getCategoryTotals(startOfMonth, endOfMonth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today total (computed from current in-memory flow snapshot)
    fun getTodayTotal(): Double {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
        val start = today.timeInMillis
        val end = start + 86_400_000L
        return expenses.value.filter { it.date in start..end }.sumOf { it.amount }
    }


    // Weekly total as StateFlow
    val weeklyTotal: StateFlow<Double> = expenses.map { list ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
// set to the start of current week (Monday)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val diff = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
        cal.add(Calendar.DAY_OF_YEAR, diff)
        val start = cal.timeInMillis
        val end = start + 7 * 86_400_000L
        list.filter { it.date in start..end }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val monthlyTotal: StateFlow<Double> = expenses.map { list ->
        val now = LocalDate.now()
        // ✅ CORRECTED CODE
        val zoneId = ZoneId.systemDefault()
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay(zoneId).toEpochSecond() * 1000
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59).atZone(zoneId).toEpochSecond() * 1000

        list.filter { it.date in startOfMonth..endOfMonth }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category totals for dashboard (map of categoryId -> sum)
    suspend fun totalsByCategory() {
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
        val start = now.timeInMillis - 6 * 86_400_000L // last 7 days
        val end = now.timeInMillis + 86_400_000L
        return dao.getAllExpenses().let { /* not used here - fetch from expenses snapshot */ }
    }


    /**
     * A flow that provides total spending for each of the last 7 days.
     * The key is the LocalDate, the value is the total amount.
     * TreeMap is used to keep the dates sorted.
     */
    val dailySpendingLast7Days: StateFlow<Map<LocalDate, Double>> = expenses.map { list ->
        // Create a map for the last 7 days, initialized to 0.0
        val dailyTotals = TreeMap<LocalDate, Double>()
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            dailyTotals[today.minusDays(i.toLong())] = 0.0
        }

        // Get the timestamp for 7 days ago
        val sevenDaysAgo = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

        // Filter expenses and sum them up
        list.filter { it.date >= sevenDaysAgo }
            .forEach { expense ->
                val date = LocalDate.ofEpochDay(expense.date / (1000 * 60 * 60 * 24))
                if (dailyTotals.containsKey(date)) {
                    dailyTotals[date] = (dailyTotals[date] ?: 0.0) + expense.amount
                }
            }
        dailyTotals
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // CSV export — callback invoked on Main thread
    fun exportCSV(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "SpendWise_${System.currentTimeMillis()}.csv")
                FileWriter(file).use { writer ->
                    writer.append("Amount,CategoryId,Date,Note\n")
                    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    expenses.value.forEach { e ->
                        writer.append("${e.amount},${e.id},${df.format(Date(e.date))},${e.note ?: ""}\n")
                    }
                }
                withContext(Dispatchers.Main) { onComplete(true, file.absolutePath) }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) { onComplete(false, ex.message) }
            }
        }
    }
}