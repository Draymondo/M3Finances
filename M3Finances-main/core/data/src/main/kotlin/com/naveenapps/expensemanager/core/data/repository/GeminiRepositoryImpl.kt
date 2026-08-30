package com.naveenapps.expensemanager.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.model.ReceiptData
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GeminiRepositoryImpl(
    private val dispatchers: AppCoroutineDispatchers,
) : GeminiRepository {

    private data class GeminiReceiptResponse(
        @SerializedName("amount") val amount: Double? = null,
        @SerializedName("merchant_name") val merchantName: String? = null,
        @SerializedName("date") val date: String? = null,
        @SerializedName("category") val category: String? = null,
    )

    override suspend fun scanReceipt(imageBytes: ByteArray, apiKey: String): Resource<ReceiptData> =
        withContext(dispatchers.io) {
            try {
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: return@withContext Resource.Error(Exception("Impossible de décoder l'image"))

                val model = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = """
                    Analyze this receipt image and extract the following information.
                    Return ONLY a valid JSON object with exactly these fields:
                    - "amount": the total amount as a number (double), or null if not found
                    - "merchant_name": the store/merchant name as a string, or null if not found
                    - "date": the transaction date in YYYY-MM-DD format as a string, or null if not found
                    - "category": suggest ONE category from this list that best matches: 
                      Food, Transport, Shopping, Health, Entertainment, Bills, Education, Travel, Other
                      Return null if unsure.
                    
                    Example response: {"amount": 42.50, "merchant_name": "Carrefour", "date": "2024-01-15", "category": "Food"}
                """.trimIndent()

                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val responseText = response.text
                    ?: return@withContext Resource.Error(Exception("Réponse vide de Gemini"))

                val parsed = Gson().fromJson(responseText, GeminiReceiptResponse::class.java)

                val parsedDate = parsed.date?.let { dateStr ->
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                    } catch (e: Exception) {
                        null
                    }
                }

                Resource.Success(
                    ReceiptData(
                        amount = parsed.amount,
                        merchantName = parsed.merchantName,
                        date = parsedDate,
                        suggestedCategory = parsed.category,
                    )
                )
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }
}

