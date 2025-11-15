package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.ads.AdManager
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.data.ChatMessageEntity
import com.giantnovadevs.mysamoney.data.PreferencesManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val expenseDao = AppDatabase.getInstance(app).expenseDao()
    private val incomeDao = AppDatabase.getInstance(app).incomeDao()
    private val categoryDao = AppDatabase.getInstance(app).categoryDao()
    private val preferencesManager = PreferencesManager(app)
    private val chatDao = AppDatabase.getInstance(app).chatDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getChatHistory()
        .map { entityList ->
            entityList.map { ChatMessage(it.message, it.isFromUser) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _dashboardInsight = MutableStateFlow<String?>(null)
    val dashboardInsight: StateFlow<String?> = _dashboardInsight.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val adManager = AdManager(app)

    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog = _showAdDialog.asStateFlow()

    val messageCredits: StateFlow<Int> = preferencesManager.messageCredits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private var isUserPro = false

    fun setUserProStatus(isPro: Boolean) {
        isUserPro = isPro
    }

    init {
        adManager.loadRewardedAd()
        viewModelScope.launch {
            if (chatDao.getChatHistory().firstOrNull().isNullOrEmpty()) {
                saveMessageToDb("Hello! I'm your financial coach. Ask me anything about your spending habits or incomes.", false)
            }
        }
    }
    private suspend fun saveMessageToDb(message: String, isFromUser: Boolean) {
        val entity = ChatMessageEntity(
            message = message,
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(entity)
    }

    private fun updateMessageCredits(newCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.saveMessageCredits(newCount)
        }
    }

    fun askQuestion(question: String) {
        val currentCredits = messageCredits.value
        if (!isUserPro && currentCredits <= 0) {
            _showAdDialog.value = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            if (!isUserPro) {
                updateMessageCredits(currentCredits - 1)
            }
            saveMessageToDb(question, true)

            try {
                val prompt = buildContextPrompt(question)
                val requestBody = GeminiRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(prompt)))
                    )
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message} \n $responseBody")
                }

                val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                val aiText = geminiResponse.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: "No response from AI."

                saveMessageToDb(aiText, false)
            } catch (e: Exception) {
                val errorMessage = "uh oh...Its not you, its us. Please try again later."
                saveMessageToDb(errorMessage, false)
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
            updateMessageCredits(messageCredits.value + 3)
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

    fun getDashboardInsight() {
        // Don't run if we're already loading or have an insight
        if (_isLoading.value || _dashboardInsight.value != null) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true // Use the main loading spinner
            try {
                // 1. Build the data context
                val dataContext = buildContextPrompt(question = "") // Pass empty question

                // 2. Create a specific, one-sentence prompt
                val insightPrompt = """
                    You are "Mysa Money Coach." Based *only* on the data below, provide one single, interesting, and concise insight for the user's dashboard.
                    - Be friendly and start with a greeting (e.g., "Hi there!").
                    - Keep it to one or two short sentences.
                    - If there is no data, just say: "Start logging your expenses and incomes to get personalized insights!"
                    - Do not ask a question. Just provide the insight.

                    Data:
                    $dataContext
                """.trimIndent()

                // 3. Build the request
                val requestBody = GeminiRequest(contents = listOf(Content(parts = listOf(Part(insightPrompt)))))
                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message} \n $responseBody")
                }

                val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                val aiText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                // 4. Set the state
                _dashboardInsight.value = aiText ?: "Tap to get your first insight!"

            } catch (e: Exception) {
                _dashboardInsight.value = "Couldn't load insight. Tap to retry."
            } finally {
                _isLoading.value = false
            }
        }
    }
}