package com.naveenapps.expensemanager.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.InvalidStateException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestTimeoutException
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.naveenapps.expensemanager.core.model.NotificationProcessingException
import com.naveenapps.expensemanager.core.model.ProcessingErrorType
import com.google.gson.annotations.SerializedName
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.PendingTransactionDeduplication
import com.naveenapps.expensemanager.core.model.ReceiptData
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import kotlinx.coroutines.withContext
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        private const val NOTIFICATION_DATE_FORMAT = "yyyy-MM-dd HH:mm"
        private val NOTIFICATION_DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""")

        /** Placeholders usuels d'un modèle de langage, comparés en minuscules. */
        private val PLACEHOLDER_TEXTS = setOf(
            "n/a",
            "na",
            "null",
            "nil",
            "none",
            "unknown",
            "inconnu",
            "inconnue",
            "undefined",
            "not specified",
            "non spécifié",
            "non specifie",
            "-",
            "?",
        )

        fun parseNotificationDate(
            dateString: String?,
            timeZone: TimeZone = TimeZone.getDefault(),
        ): Date? {
            if (dateString.isNullOrBlank() || !NOTIFICATION_DATE_PATTERN.matches(dateString)) return null

            val formatter = SimpleDateFormat(NOTIFICATION_DATE_FORMAT, Locale.US).apply {
                isLenient = false
                this.timeZone = timeZone
            }
            val position = ParsePosition(0)
            val parsedDate = formatter.parse(dateString, position) ?: return null
            return parsedDate.takeIf { position.errorIndex == -1 && position.index == dateString.length }
        }

        /**
         * Valide le montant renvoyé par Gemini (chantier 3J).
         *
         * Un montant absent, nul, négatif, infini ou `NaN` est refusé : la réponse est alors
         * rejetée comme invalide, jamais convertie silencieusement en `0`.
         */
        fun parseAmount(amount: Double?): Double? =
            amount?.takeIf { it.isFinite() && it > 0.0 }

        /**
         * Un frais absent est légitime (le fournisseur peut ne pas en fournir). Un frais présent
         * doit être fini et positif ou nul : un frais négatif est une incohérence déterministe
         * et invalide la réponse, au lieu d'être silencieusement converti en `null`.
         */
        fun isFeeValid(fee: Double?): Boolean = fee == null || (fee.isFinite() && fee >= 0.0)

        /**
         * Normalise un champ texte facultatif renvoyé par Gemini.
         *
         * `null`, une chaîne vide/blanche et les placeholders évidents générés par un modèle
         * ("N/A", "null", "unknown"...) deviennent `null`. Le champ reste facultatif : aucune
         * transaction n'est rejetée pour cette seule raison.
         */
        fun normalizeOptionalText(value: String?): String? {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            return trimmed.takeUnless { it.lowercase(Locale.ROOT) in PLACEHOLDER_TEXTS }
        }

        fun parseTransactionType(type: String?): TransactionType? = when (type?.uppercase(Locale.ROOT)) {
            "INCOME" -> TransactionType.INCOME
            "EXPENSE" -> TransactionType.EXPENSE
            "TRANSFER" -> TransactionType.TRANSFER
            else -> null
        }

        /**
         * Classifie uniquement les exceptions réellement exposées par le SDK Gemini.
         * Les erreurs non reconnues restent UNKNOWN afin de conserver un retry borné,
         * sans les présenter comme temporaires avec certitude.
         */
        fun classifyGeminiException(exception: Exception): NotificationProcessingException {
            val type = when (exception) {
                is RequestTimeoutException,
                is ServerException,
                is QuotaExceededException,
                -> ProcessingErrorType.TEMPORARY

                is InvalidAPIKeyException,
                is UnsupportedUserLocationException,
                is PromptBlockedException,
                is SerializationException,
                is InvalidStateException,
                -> ProcessingErrorType.PERMANENT

                else -> ProcessingErrorType.UNKNOWN
            }
            return NotificationProcessingException(
                type = type,
                message = exception.message ?: "Erreur Gemini inconnue",
                cause = exception,
            )
        }

        fun getProviderContext(source: TransactionSource): String = when (source) {
            TransactionSource.WAVE -> "Wave (mobile money)"
            TransactionSource.ORANGE_MONEY -> "Orange Money"
            TransactionSource.MTN_MOMO -> "MTN MoMo"
            TransactionSource.MOOV_MONEY -> "Moov Money"
            TransactionSource.DJAMO -> "Djamo"
            TransactionSource.PAYPAL -> "PayPal"
            TransactionSource.SMS,
            TransactionSource.UNKNOWN,
            TransactionSource.SCHEDULED -> "financial or mobile money"
        }

        fun buildNotificationPrompt(
            notificationText: String,
            source: TransactionSource,
        ): String {
            val providerContext = getProviderContext(source)
            return """
                Parse this $providerContext notification in French: "$notificationText"

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
        }

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
        source: TransactionSource,
        apiKey: String,
    ): Resource<PendingTransaction> =
        withContext(dispatchers.io) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.1-flash-lite",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    },
                )

                val prompt = buildNotificationPrompt(notificationText, source)

                val response = model.generateContent(prompt)
                val responseText = response.text
                    ?: return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Réponse vide de Gemini",
                        )
                    )
                    
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                val parsed = try {
                    Gson().fromJson(cleanJson, GeminiWaveResponse::class.java)
                } catch (exception: JsonParseException) {
                    return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Réponse JSON Gemini invalide",
                            exception,
                        )
                    )
                } ?: return@withContext Resource.Error(
                    NotificationProcessingException(
                        ProcessingErrorType.PERMANENT,
                        "Réponse JSON Gemini invalide",
                    )
                )
                
                val amount = parseAmount(parsed.amount)
                    ?: return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Montant invalide dans la réponse de l'IA",
                        )
                    )

                if (!isFeeValid(parsed.fee)) {
                    return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Frais invalides dans la réponse de l'IA",
                        )
                    )
                }

                val resolvedFee = resolveWaveFee(
                    notificationText = notificationText,
                    parsedType = parsed.type,
                    parsedFee = parsed.fee,
                    amount = amount,
                )

                val transactionType = parseTransactionType(parsed.type)
                    ?: return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Type de transaction absent ou invalide",
                        )
                    )

                val parsedDate = parseNotificationDate(parsed.date)
                    ?: return@withContext Resource.Error(
                        NotificationProcessingException(
                            ProcessingErrorType.PERMANENT,
                            "Date de transaction absente ou invalide",
                        )
                    )

                // Le merchant et la catégorie restent facultatifs dans le modèle métier, mais une
                // valeur vide ou un placeholder généré par le modèle ("N/A", "null", ...) ne doit
                // pas atteindre Room : elle polluerait notamment la clé de déduplication (3D).
                val merchant = normalizeOptionalText(parsed.merchant)
                val category = normalizeOptionalText(parsed.category)

                val filledFieldCount = listOfNotNull(
                    merchant,
                    parsed.date,
                    category,
                ).size
                val confidence = (0.4f + filledFieldCount * 0.2f).coerceAtMost(1f)

                val providerReference = parsed.transactionId
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                val deduplicationKey = PendingTransactionDeduplication.computeDeduplicationKey(
                    source = source,
                    type = transactionType,
                    amount = amount,
                    senderOrReceiver = merchant,
                    fee = resolvedFee,
                )
                // L'identifiant de la ligne est technique et propre à cet événement. La référence
                // fournisseur et la clé canonique portent, elles, l'identité financière et
                // permettent d'ignorer une notification rejouée sans écraser les choix utilisateur.
                val generatedId = java.util.UUID.randomUUID().toString()

                Resource.Success(
                    com.naveenapps.expensemanager.core.model.PendingTransaction(
                        id = generatedId,
                        amount = amount,
                        fee = resolvedFee,
                        merchant = merchant,
                        date = parsedDate,
                        transactionType = transactionType,
                        suggestedCategory = category,
                        rawNotification = notificationText,
                        source = source,
                        confidence = confidence,
                        providerReference = providerReference,
                        deduplicationKey = deduplicationKey,
                    )
                )
            } catch (e: Exception) {
                Resource.Error(classifyGeminiException(e))
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

