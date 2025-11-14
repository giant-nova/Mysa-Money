package com.giantnovadevs.mysamoney.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.CategoryTotal
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.utils.FileExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.TreeMap
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).expenseDao()

    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addExpense(expense: Expense) = viewModelScope.launch { dao.insert(expense) }

    fun deleteExpense(expense: Expense) = viewModelScope.launch { dao.delete(expense) }

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    val selectedMonthYear = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))


    fun getExpenseById(id: Int): Flow<Expense?> {
        return dao.getExpenseById(id)
    }

    // ✅ ADD THIS
    fun updateExpense(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        dao.update(expense)
    }
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
    @OptIn(ExperimentalCoroutinesApi::class)
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
    private val categoryDao = AppDatabase.getInstance(app).categoryDao()
    // ...

    // --- ✅ REPLACE your old exportCSV function with this ---

    /**
     * Exports the user's data to a file in their "Download" folder.
     * @param format "CSV", "PDF", or "Excel"
     * @param onComplete Callback with (success, filePath/message)
     */
    fun exportExpenses(format: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get all the data
                val allExpenses = expenses.value // Get current expenses
                val allCategories = categoryDao.getAllCategoriesList() // Get categories

                // 2. Define file properties
                val timestamp = System.currentTimeMillis()
                val mimeType: String
                val fileName: String

                when (format) {
                    "CSV" -> {
                        fileName = "MysaMoney_Export_$timestamp.csv"
                        mimeType = "text/csv"
                    }
                    "PDF" -> {
                        fileName = "MysaMoney_Export_$timestamp.pdf"
                        mimeType = "application/pdf"
                    }
                    else -> {
                        // We'll stub Excel for now
                        onComplete(false, "Excel format not yet supported")
                        return@launch
                    }
                }

                // 3. Get the correct file output stream
                val helper = FileExportHelper(getApplication())
                val outputStream = helper.getOutputStream(fileName, mimeType)

                if (outputStream == null) {
                    throw IOException("Failed to create output stream.")
                }

                // 4. Write the content
                outputStream.use { stream ->
                    when (format) {
                        "CSV" -> writeCsv(stream, allExpenses, allCategories)
                        "PDF" -> writePdf(stream, allExpenses, allCategories)
                    }
                }

                // 5. Report success
                withContext(Dispatchers.Main) {
                    onComplete(true, "Saved to Download/$fileName")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, e.message)
                }
            }
        }
    }

    /**
     * Helper: Writes the CSV data to the output stream.
     */
    private fun writeCsv(stream: OutputStream, expenses: List<Expense>, categories: List<Category>) {
        val writer = stream.bufferedWriter()
        // Header
        writer.write("ID,Date,Amount,Category,Note")
        writer.newLine()

        // Rows
        val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        expenses.forEach { expense ->
            val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
            val note = expense.note?.replace(",", "") ?: "" // Remove commas from note
            val date = df.format(Date(expense.date))
            writer.write("${expense.id},$date,${expense.amount},\"$categoryName\",\"$note\"")
            writer.newLine()
        }
        writer.flush()
    }

    /**
     * Placeholder: Writes PDF data.
     * (We'll build this logic in the next step)
     */
    /**
     * Helper: Writes the PDF data to the output stream using iText7.
     */
    private fun writePdf(stream: OutputStream, expenses: List<Expense>, categories: List<Category>) {
        // 1. Initialize the PDF writer and document
        val pdfWriter = PdfWriter(stream)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // 2. Add a Title
        document.add(
            Paragraph("Mysa Money Expense Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(18f)
                .setMarginBottom(20f)
        )

        // 3. Create a Table with 4 columns
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1.5f, 2f, 3f, 1.5f)))
        table.setWidth(UnitValue.createPercentValue(100f)) // Full width

        // 4. Add Table Headers
        table.addHeaderCell(Cell().add(Paragraph("Date").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Category").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Note").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT)))

        // 5. Add Data Rows
        val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        var total = 0.0

        for (expense in expenses) {
            val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
            val note = expense.note ?: ""
            val date = df.format(Date(expense.date))
            total += expense.amount

            table.addCell(Cell().add(Paragraph(date)))
            table.addCell(Cell().add(Paragraph(categoryName)))
            table.addCell(Cell().add(Paragraph(note)))
            table.addCell(Cell().add(Paragraph("₹%.2f".format(expense.amount)).setTextAlignment(TextAlignment.RIGHT)))
        }

        // 6. Add a Total Row at the bottom
        table.addCell(Cell(1, 3).add(Paragraph("Total").setBold().setTextAlignment(TextAlignment.RIGHT)))
        table.addCell(Cell().add(Paragraph("₹%.2f".format(total)).setBold().setTextAlignment(TextAlignment.RIGHT)))

        // 7. Add the table to the document
        document.add(table)

        // 8. Close the document (this is what saves it)
        document.close()
    }
}