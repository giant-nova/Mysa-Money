// FinancialCoachViewModel.kt
package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.data.AppDatabase
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
import com.giantnovadevs.mysamoney.ads.AdManager

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

    private val adManager = AdManager(app)

    // This state tells the UI to show the "watch ad" dialog
    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog = _showAdDialog.asStateFlow()

    // This tracks how many free messages the user has
    private val _messageCredits = MutableStateFlow(1) // Start with 1 free credit
    val messageCredits = _messageCredits.asStateFlow()

    private var isUserPro = false // New variable

    // New function to accept Pro status from UI
    fun setUserProStatus(isPro: Boolean) {
        isUserPro = isPro
    }

    init {
        // Pre-load an ad when the ViewModel is created
        adManager.loadRewardedAd()
    }

    fun askQuestion(question: String) {
        // --- Check for credits ---
        if (!isUserPro && _messageCredits.value <= 0) {
            _showAdDialog.value = true // Show the "watch ad" dialog
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Consume a credit
            _messageCredits.value = _messageCredits.value - 1
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
            You are "Mysa Money Coach," a friendly and helpful financial assistant.
            Provide insights based *only* on the data below. Be concise.

            Data:
            $dataSummary

            Question: "$question"
        """.trimIndent()
    }

    fun showRewardAd(activity: Activity) {
        _showAdDialog.value = false // Close the dialog
        adManager.showRewardedAd(activity) {
            // This is the onRewardEarned callback!
            // Give the user 3 new credits.
            _messageCredits.value = _messageCredits.value + 3
            // Pre-load the next ad
            adManager.loadRewardedAd()
        }
    }

    /**
     * Called when the user closes the "watch ad" dialog.
     */
    fun dismissAdDialog() {
        _showAdDialog.value = false
    }
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}