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
                    modelName = "gemini-3.1-flash-lite",
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
                    - "category": suggest ONE global category from this exact list that best matches the whole receipt: 
                      Food, Transportation, Shopping, Health, Entertainment, Utilities, Leisure, Clothing, Education, Salary, Gift, Coupons
                      Return null if unsure.
                    - "items": a JSON array of individual items found on the receipt. For each item, include:
                        - "name": the product name as a string
                        - "amount": the price of the item as a number (double)
                        - "category": suggest ONE category from the exact list above for this specific item.
                    
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
                    
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                val parsed = Gson().fromJson(cleanJson, GeminiReceiptResponse::class.java)

                val parsedDate = parsed.date?.let { dateStr ->
                    val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
                    var date: java.util.Date? = null
                    for (format in formats) {
                        try {
                            date = SimpleDateFormat(format, Locale.US).parse(dateStr)
                            if (date != null) break
                        } catch (e: Exception) { }
                    }
                    date
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
        @SerializedName("date") val date: String? = null,
        @SerializedName("category") val category: String? = null,
        @SerializedName("transaction_id") val transactionId: String? = null,
    )

    private fun isExplicitWaveTransfer(notificationText: String): Boolean {
        val text = notificationText.lowercase(Locale.US)
        val transferWords = listOf(
            "envoyer",
            "envoyé",
            "vous avez envoyé",
            "a envoyé"
        )
        return transferWords.any { text.contains(it) }
    }

    companion object {
        fun resolveWaveFee(
            notificationText: String,
            parsedType: String?,
            parsedFee: Double?,
            amount: Double? = null,
        ): Double? {
            if (parsedFee != null) {
                return if (parsedFee > 0.0) parsedFee else null
            }

            if (parsedType?.uppercase(Locale.US) != "TRANSFER") {
                return null
            }

            if (!isExplicitWaveTransfer(notificationText)) {
                return null
            }

            val safeAmount = amount ?: return null
            return safeAmount * 0.01
        }

        private fun isExplicitWaveTransfer(notificationText: String): Boolean {
            val text = notificationText.lowercase(Locale.US)
            val transferWords = listOf(
                "envoyer",
                "envoyé",
                "vous avez envoyé",
                "a envoyé"
            )
            return transferWords.any { text.contains(it) }
        }
    }

    private fun resolveWaveFee(
        notificationText: String,
        parsedType: String?,
        parsedFee: Double?,
        amount: Double?
    ): Double? {
        if (parsedFee != null) {
            return if (parsedFee > 0.0) parsedFee else null
        }

        if (parsedType?.uppercase(Locale.US) != "TRANSFER") {
            return null
        }

        if (!isExplicitWaveTransfer(notificationText)) {
            return null
        }

        val safeAmount = amount ?: return null
        return safeAmount * 0.01
    }

    override suspend fun parseWaveNotification(
        notificationText: String,
        source: com.naveenapps.expensemanager.core.model.TransactionSource,
        apiKey: String
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.1-flash-lite",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = """
                    Parse this Wave (mobile money) notification in French: "$notificationText"
                    
                    Important rules:
                    1. If the notification explicitly mentions a fee amount, use that exact fee value.
                    2. Otherwise, apply 1% only when the text clearly indicates a transfer sent by the user (for example: "vous avez envoyé", "transfert", "transfer", "envoyer").
                    3. Do not apply 1% to payments, purchases, or other non-transfer transactions.
                    4. If there is no fee and it is not a clear transfer, return "fee": null.
                    
                    Return a JSON object with exactly these fields:
                    - "type": "EXPENSE", "INCOME", or "TRANSFER"
                    - "amount": the transaction amount as a number (double), excluding fees
                    - "fee": the fee amount as a number (double) or null if none
                    - "merchant": the name of the recipient, sender, or merchant
                    - "date": the date and time from the notification if present, in "yyyy-MM-dd HH:mm" format, or null if not found
                    - "category": suggest ONE global category (e.g., Food, Transportation, Shopping, Health, Entertainment, Utilities, Leisure, Clothing, Education, Salary, Gift, Coupons)
                    - "transaction_id": the unique transaction ID / reference provided in the notification as a string, or null if not found
                    
                    Example response: {"type": "EXPENSE", "amount": 1000, "fee": 10, "merchant": "Pharmacie de la Paix", "date": "2023-10-27 14:30", "category": "Health", "transaction_id": "CI123456789"}
                """.trimIndent()

                val response = model.generateContent(prompt)
                val responseText = response.text
                    ?: return@withContext Resource.Error(Exception("Réponse vide de Gemini"))
                    
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                val parsed = Gson().fromJson(cleanJson, GeminiWaveResponse::class.java)
                
                if (parsed.amount == null) {
                    return@withContext Resource.Error(Exception("Montant introuvable dans la notification"))
                }

                val resolvedFee = resolveWaveFee(
                    notificationText = notificationText,
                    parsedType = parsed.type,
                    parsedFee = parsed.fee,
                    amount = parsed.amount,
                )

                val transactionType = when (parsed.type?.uppercase()) {
                    "INCOME" -> com.naveenapps.expensemanager.core.model.TransactionType.INCOME
                    "TRANSFER" -> com.naveenapps.expensemanager.core.model.TransactionType.TRANSFER
                    else -> com.naveenapps.expensemanager.core.model.TransactionType.EXPENSE
                }
                
                val parsedDate = parsed.date?.let { dateStr ->
                    try {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(dateStr)
                    } catch (e: Exception) {
                        null
                    }
                } ?: java.util.Date()

                val filledFieldCount = listOfNotNull(
                    parsed.merchant?.takeIf { it.isNotBlank() },
                    parsed.date,
                    parsed.category,
                ).size
                val confidence = (0.4f + filledFieldCount * 0.2f).coerceAtMost(1f)

                // Deduplication strategy: Use extracted transaction_id, or fallback to hash of key fields, or fallback to UUID
                val generatedId = parsed.transactionId 
                    ?: "${parsed.amount}_${parsed.merchant}_${parsed.date}".hashCode().toString().takeIf { parsed.merchant != null } 
                    ?: java.util.UUID.randomUUID().toString()

                Resource.Success(
                    com.naveenapps.expensemanager.core.model.PendingTransaction(
                        id = generatedId,
                        amount = parsed.amount,
                        fee = resolvedFee,
                        merchant = parsed.merchant,
                        date = parsedDate,
                        transactionType = transactionType,
                        suggestedCategory = parsed.category,
                        rawNotification = notificationText,
                        source = source,
                        confidence = confidence
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
                    modelName = "gemini-3.1-flash-lite",
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
        
    override suspend fun generateMonthlyReport(transactionsInfo: String, apiKey: String): Resource<String> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.1-flash-lite",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.4f
                    },
                    systemInstruction = content {
                        text("""
                            Tu es un coach financier personnel chaleureux, bienveillant et intelligent, intégré à une application mobile de gestion de budget. 
                            Ton utilisateur s'appelle Ray. Sa devise principale est le franc CFA (FCFA).
                            Tu aides Ray à suivre son argent sans jamais le juger. Ton ton est naturel, clair, encourageant et tu tutoies Ray.
                            
                            Ton objectif : Faire un bilan de ses finances sur la période indiquée dans les données fournies ci-dessous.
                            - La période peut être une semaine, un mois, une année ou une plage personnalisée — utilise EXACTEMENT la période mentionnée dans les données, ne suppose JAMAIS que c'est un mois par défaut.
                            - Analyse les données fournies (les dépenses par catégorie, le budget, etc.).
                            - N'invente JAMAIS une catégorie, une habitude de dépense ou un exemple ("boissons", "loisirs", "petits plaisirs"...) qui n'apparaît PAS explicitement dans la liste de catégories fournie. La liste donnée est exhaustive : si une catégorie n'y figure pas, Ray n'a rien dépensé dedans sur cette période, ne la mentionne sous aucun prétexte, même comme conseil générique.
                            - Base tes remarques et ton conseil uniquement sur les catégories et montants réellement listés.
                            - Rédige un court paragraphe (3 à 4 phrases max) résumant la situation.
                            - Donne un conseil pratique et encourageant.
                            - Reste concis et va à l'essentiel.
                            - N'utilise PAS de format Markdown compliqué, juste du texte simple avec éventuellement quelques emojis.
                        """.trimIndent())
                    }
                )

                val prompt = """
                    Voici les données financières de Ray pour la période indiquée ci-dessous :
                    
                    $transactionsInfo
                    
                    Fais-lui un bilan personnalisé.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val text = response.text ?: throw Exception("Empty response from Gemini")
                
                Resource.Success(text)
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }
}

