// FinancialCoachViewModel.kt
package com.example.spendwise.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.BuildConfig
import com.example.spendwise.data.AppDatabase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean
)

// Gemini REST API models
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

data class Part(val text: String)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    @SerializedName("finishReason") val finishReason: String?
)

class FinancialCoachViewModel(app: Application) : AndroidViewModel(app) {

    private val expenseDao = AppDatabase.getInstance(app).expenseDao()
    private val incomeDao = AppDatabase.getInstance(app).incomeDao()
    private val categoryDao = AppDatabase.getInstance(app).categoryDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val _uiState = MutableStateFlow(
        listOf(
            ChatMessage(
                message = "Hello! I'm your financial coach. Ask me anything about your spending habits or incomes.",
                isFromUser = false
            )
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun askQuestion(question: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _uiState.value = _uiState.value + ChatMessage(question, isFromUser = true)

            try {
                val prompt = buildContextPrompt(question)
                val requestBody = GeminiRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(prompt)))
                    )
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val geminiResponse = gson.fromJson(response.body?.string(), GeminiResponse::class.java)
                val aiText = geminiResponse.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: "No response from AI."

                _uiState.value = _uiState.value + ChatMessage(aiText, isFromUser = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value + ChatMessage("Error: ${e.message}", isFromUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun buildContextPrompt(question: String): String {
        val oneMonthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val expenses = expenseDao.getExpensesAfter(oneMonthAgo)
        val incomes = incomeDao.getAllIncomesAfter(oneMonthAgo)
        val categories = categoryDao.getAllCategoriesList()

        val dataSummary = buildString {
            appendLine("--- User's Financial Data (Last 30 Days) ---")
            appendLine("\n## Incomes:")
            if (incomes.isEmpty()) appendLine("No incomes recorded.")
            else incomes.forEach {
                appendLine("- ₹${it.amount} from '${it.note}' on ${formatDate(it.date)}")
            }

            appendLine("\n## Expenses:")
            if (expenses.isEmpty()) appendLine("No expenses recorded.")
            else expenses.forEach { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
                appendLine("- ₹${expense.amount} on '$categoryName' (${expense.note ?: "no note"}) on ${formatDate(expense.date)}")
            }
            appendLine("--- End of Financial Data ---")
        }

        return """
            You are "SpendWise Coach," a friendly and helpful financial assistant.
            Provide insights based *only* on the data below. Be concise.

            Data:
            $dataSummary

            Question: "$question"
        """.trimIndent()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}