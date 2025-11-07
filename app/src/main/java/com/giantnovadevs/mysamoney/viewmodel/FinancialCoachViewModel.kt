package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.ads.AdManager
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

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean
)
data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>, val role: String = "user")
data class Part(val text: String)
data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?, @SerializedName("finishReason") val finishReason: String?)


class FinancialCoachViewModel(app: Application) : AndroidViewModel(app) {

    // Add a Log Tag
    private val TAG = "FinancialCoachVM"

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

    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog = _showAdDialog.asStateFlow()

    private val _messageCredits = MutableStateFlow(1)
    val messageCredits = _messageCredits.asStateFlow()

    private var isUserPro = false

    fun setUserProStatus(isPro: Boolean) {
        isUserPro = isPro
    }

    init {
        adManager.loadRewardedAd()
    }


    fun askQuestion(question: String) {
        if (!isUserPro && _messageCredits.value <= 0) {
            _showAdDialog.value = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // This is the bug fix: only subtract if not Pro
            if (!isUserPro) {
                _messageCredits.value = _messageCredits.value - 1
            }
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

                // Use the correct, working model URL
                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

                Log.d(TAG, "--- askQuestion ---")
                Log.d(TAG, "Request URL: $url")
                Log.d(TAG, "Request JSON: $json")

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() // Read body once

                if (!response.isSuccessful) {
                    Log.e(TAG, "Response Error: $responseBody")
                    throw Exception("HTTP ${response.code}: ${response.message} \n $responseBody")
                }

                Log.d(TAG, "Response Success: $responseBody")
                val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                val aiText = geminiResponse.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: "No response from AI."

                _uiState.value = _uiState.value + ChatMessage(aiText, isFromUser = false)
            } catch (e: Exception) {
                Log.e(TAG, "askQuestion failed", e)
                // --- ✅ FIX 1: Show a friendly error message ---
                _uiState.value = _uiState.value + ChatMessage("uh oh...Its not you, its us. Please try again later.", isFromUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ... (listAvailableModels is unchanged) ...
    fun listAvailableModels() {
        _uiState.value = _uiState.value + ChatMessage("Checking available models... see Logcat.", false)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1/models?key=${BuildConfig.GEMINI_API_KEY}"

                Log.d(TAG, "--- listAvailableModels ---")
                Log.d(TAG, "Request URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "listModels Error: $responseBody")
                    // Show the error in the chat
                    _uiState.value = _uiState.value + ChatMessage("Error listing models: ${response.message}\n${responseBody}", false)
                } else {
                    Log.i(TAG, "--- AVAILABLE MODELS ---")
                    Log.i(TAG, responseBody ?: "Empty response")
                    Log.i(TAG, "--- END OF MODELS ---")
                    _uiState.value = _uiState.value + ChatMessage("Model list logged to Logcat.", false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "listModels failed", e)
                _uiState.value = _uiState.value + ChatMessage("Error: ${e.message}", false)
            }
        }
    }

    private suspend fun buildContextPrompt(question: String): String {
        // ... (This function is unchanged)
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
                appendLine("- ₹${expense.amount} on '$categoryName' (${expense.note ?: "no note"}) on ${formatDate(expense.date)}")            }
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
        _showAdDialog.value = false
        adManager.showRewardedAd(activity) {
            _messageCredits.value = _messageCredits.value + 3
            adManager.loadRewardedAd()
        }
    }

    fun dismissAdDialog() {
        _showAdDialog.value = false
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}