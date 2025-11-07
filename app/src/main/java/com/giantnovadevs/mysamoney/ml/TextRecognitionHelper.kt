package com.giantnovadevs.mysamoney.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TextRecognitionHelper(context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val appContext = context.applicationContext

    /**
     * Analyzes an image from a Uri and returns the largest monetary value found.
     */
    suspend fun analyze(imageUri: Uri): String? {
        val inputImage = try {
            InputImage.fromFilePath(appContext, imageUri)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        // Wait for the ML Kit task to complete
        val result = suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

        return parseResult(result.text)
    }

    /**
     * A simple parser to find the most likely "Total" amount.
     * It looks for the largest number in lines containing keywords.
     */
    private fun parseResult(text: String): String? {
        // Regex to find monetary values (e.g., 12.34, 1,234.56, 150)
        val amountRegex = Regex("""(\d{1,3}(,\d{3})*|\d+)(\.\d{2})?""")

        // Keywords that indicate a total
        val keywords = listOf("total", "amount", "subtotal", "balance", "due")

        var largestAmount = 0.0

        text.lines().forEach { line ->
            val lowerLine = line.lowercase()

            // Check if the line contains any of our keywords
            if (keywords.any { lowerLine.contains(it) }) {

                // Find all numbers in this line
                amountRegex.findAll(line).forEach { matchResult ->
                    val amountString = matchResult.value.replace(",", "")
                    val amount = amountString.toDoubleOrNull()

                    if (amount != null && amount > largestAmount) {
                        largestAmount = amount
                    }
                }
            }
        }

        return if (largestAmount > 0.0) {
            // Return as a simple string, "194.40"
            String.format("%.2f", largestAmount)
        } else {
            null // No amount found
        }
    }
}