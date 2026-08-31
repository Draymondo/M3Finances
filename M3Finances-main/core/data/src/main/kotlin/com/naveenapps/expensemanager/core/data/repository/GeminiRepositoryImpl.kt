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

import com.naveenapps.expensemanager.core.model.ReceiptItemData

class GeminiRepositoryImpl(
    private val dispatchers: AppCoroutineDispatchers,
) : GeminiRepository {

    private data class GeminiReceiptItemResponse(
        @SerializedName("name") val name: String? = null,
        @SerializedName("amount") val amount: Double? = null,
        @SerializedName("category") val category: String? = null,
    )

    private data class GeminiReceiptResponse(
        @SerializedName("amount") val amount: Double? = null,
        @SerializedName("merchant_name") val merchantName: String? = null,
        @SerializedName("date") val date: String? = null,
        @SerializedName("category") val category: String? = null,
        @SerializedName("items") val items: List<GeminiReceiptItemResponse>? = null,
    )

    override suspend fun scanReceipt(imageBytes: ByteArray, apiKey: String): Resource<ReceiptData> =
        withContext(dispatchers.io) {
            try {
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: return@withContext Resource.Error(Exception("Impossible de décoder l'image"))

                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
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
                    - "category": suggest ONE global category from this list that best matches the whole receipt: 
                      Food, Transport, Shopping, Health, Entertainment, Bills, Education, Travel, Other
                      Return null if unsure.
                    - "items": a JSON array of individual items found on the receipt. For each item, include:
                        - "name": the product name as a string
                        - "amount": the price of the item as a number (double)
                        - "category": suggest ONE category from the list above for this specific item.
                    
                    Example response: {"amount": 42.50, "merchant_name": "Carrefour", "date": "2024-01-15", "category": "Food", "items": [{"name": "Milk", "amount": 2.50, "category": "Food"}, {"name": "Magazine", "amount": 5.00, "category": "Entertainment"}]}
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

                val parsedItems = parsed.items?.map {
                    ReceiptItemData(
                        name = it.name,
                        amount = it.amount,
                        suggestedCategory = it.category
                    )
                }

                Resource.Success(
                    ReceiptData(
                        amount = parsed.amount,
                        merchantName = parsed.merchantName,
                        date = parsedDate,
                        suggestedCategory = parsed.category,
                        items = parsedItems,
                    )
                )
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }

    private data class GeminiWaveResponse(
        @SerializedName("type") val type: String? = null,
        @SerializedName("amount") val amount: Double? = null,
        @SerializedName("fee") val fee: Double? = null,
        @SerializedName("merchant") val merchant: String? = null,
        @SerializedName("category") val category: String? = null,
    )

    override suspend fun parseWaveNotification(
        notificationText: String,
        apiKey: String
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = """
                    Parse this Wave (mobile money) notification in French: "$notificationText"
                    
                    Important rule: Wave usually charges 1% fee for transfers. If it's a transfer and no fee is explicitly mentioned, calculate fee as 1% of the amount. If explicitly mentioned, use that.
                    
                    Return a JSON object with exactly these fields:
                    - "type": "EXPENSE", "INCOME", or "TRANSFER"
                    - "amount": the transaction amount as a number (double), excluding fees
                    - "fee": the fee amount as a number (double) or null if none
                    - "merchant": the name of the recipient, sender, or merchant
                    - "category": suggest ONE global category (e.g., Food, Transport, Shopping, Health, Entertainment, Bills, Education, Travel, Family, Personal, Other)
                    
                    Example response: {"type": "EXPENSE", "amount": 1000, "fee": 10, "merchant": "Pharmacie de la Paix", "category": "Health"}
                """.trimIndent()

                val response = model.generateContent(prompt)
                val responseText = response.text
                    ?: return@withContext Resource.Error(Exception("Réponse vide de Gemini"))

                val parsed = Gson().fromJson(responseText, GeminiWaveResponse::class.java)
                
                if (parsed.amount == null) {
                    return@withContext Resource.Error(Exception("Montant introuvable dans la notification"))
                }

                val transactionType = when (parsed.type?.uppercase()) {
                    "INCOME" -> com.naveenapps.expensemanager.core.model.TransactionType.INCOME
                    "TRANSFER" -> com.naveenapps.expensemanager.core.model.TransactionType.TRANSFER
                    else -> com.naveenapps.expensemanager.core.model.TransactionType.EXPENSE
                }

                Resource.Success(
                    com.naveenapps.expensemanager.core.model.PendingTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        amount = parsed.amount,
                        fee = parsed.fee,
                        merchant = parsed.merchant,
                        date = java.util.Date(),
                        transactionType = transactionType,
                        suggestedCategory = parsed.category,
                        rawNotification = notificationText
                    )
                )
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }

    private data class SuggestCategoryResponse(
        @SerializedName("category") val category: String
    )

    override suspend fun suggestCategory(note: String, categories: List<String>, apiKey: String): Resource<String> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = """
                    You are a financial categorization assistant. Given the following transaction note: "$note",
                    select the ONE most appropriate category from this exact list of available categories:
                    [${categories.joinToString(", ")}].
                    
                    Return ONLY a valid JSON object with exactly this field:
                    - "category": the selected category name as a string.
                    
                    If none match well, choose the one that makes the most general sense, or pick the first one.
                    Do NOT invent new categories.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val json = response.text ?: throw Exception("Empty response from Gemini")
                val cleanJson = json.replace("```json", "").replace("```", "").trim()
                
                val parsed = Gson().fromJson(cleanJson, SuggestCategoryResponse::class.java)
                
                Resource.Success(parsed.category)
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }

    private data class InsightsResponse(
        @SerializedName("insights") val insights: String
    )

    override suspend fun generateFinancialInsights(dataJson: String, apiKey: String): Resource<String> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = """
                    You are a helpful personal finance assistant. I will provide you with JSON data representing 
                    the user's financial activity for the current period (income, expenses, top categories).
                    
                    Based on this data, write a short, friendly, and personalized financial insight or advice.
                    - It must be written in FRENCH.
                    - It must be 2 or 3 short sentences maximum.
                    - Highlight a positive trend, a warning about high spending, or a general encouraging remark.
                    
                    Return ONLY a valid JSON object with exactly this field:
                    - "insights": the generated text.
                    
                    Here is the data:
                    $dataJson
                """.trimIndent()

                val response = model.generateContent(prompt)
                val json = response.text ?: throw Exception("Empty response from Gemini")
                val cleanJson = json.replace("```json", "").replace("```", "").trim()
                
                val parsed = Gson().fromJson(cleanJson, InsightsResponse::class.java)
                
                Resource.Success(parsed.insights)
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }
}

