package com.naveenapps.expensemanager.feature.transaction.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.naveenapps.expensemanager.core.domain.usecase.category.AddCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var generativeModel: GenerativeModel? = null
    private var chatSession: com.google.ai.client.generativeai.Chat? = null

    init {
        viewModelScope.launch {
            val apiKey = settingsRepository.getGeminiApiKey().firstOrNull() ?: ""
            if (apiKey.isNotEmpty()) {
                setupGenerativeModel(apiKey)
            } else {
                addMessage(
                    ChatMessage(
                        isUser = false,
                        text = "Veuillez configurer votre cle API Gemini dans les parametres.",
                        isError = true
                    )
                )
            }
        }
    }

    private fun setupGenerativeModel(apiKey: String) {
        generativeModel = GenerativeModel(
            modelName = "gemini-3.1-flash-lite",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.0f
            },
            systemInstruction = content {
                text("Assistant financier francais. Reponses courtes. Pour creer une transaction: TRANSACTION|montant|categorie|note. Pour creer une categorie: CATEGORY|nom|EXPENSE ou CATEGORY|nom|INCOME")
            }
        )

        chatSession = generativeModel?.startChat()

        addMessage(
            ChatMessage(
                isUser = false,
                text = "Bonjour ! Comment puis-je vous aider ?"
            )
        )
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun updateMessage(id: String, update: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map { if (it.id == id) update(it) else it }
    }

    fun sendMessage(text: String, imageBitmap: android.graphics.Bitmap? = null) {
        if (text.isBlank() && imageBitmap == null) return

        addMessage(ChatMessage(isUser = true, text = text, imageBitmap = imageBitmap))

        val chat = chatSession ?: run {
            addMessage(ChatMessage(isUser = false, text = "Erreur: Cle API non configuree.", isError = true))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val streamId = UUID.randomUUID().toString()
            addMessage(ChatMessage(id = streamId, isUser = false, text = "", isLoading = true))

            try {
                val inputContent = content {
                    if (imageBitmap != null) image(imageBitmap)
                    if (text.isNotBlank()) text(text)
                }

                val responseStream = chat.sendMessageStream(inputContent)
                val fullText = StringBuilder()

                responseStream.collect { chunk ->
                    val part = chunk.text ?: ""
                    fullText.append(part)
                    updateMessage(streamId) { it.copy(text = fullText.toString(), isLoading = false) }
                }

                val finalText = fullText.toString().trim()

                if (finalText.startsWith("TRANSACTION|")) {
                    val parts = finalText.substringAfter("TRANSACTION|").split("|")
                    if (parts.size >= 3) {
                        val amount = parts[0].trim().toDoubleOrNull() ?: 0.0
                        val category = parts[1].trim()
                        val note = parts.drop(2).joinToString("|").trim()

                        updateMessage(streamId) {
                            it.copy(
                                text = "J'ai prepare cette transaction :",
                                proposedTransaction = ProposedTransaction(
                                    amount = amount,
                                    categoryName = category,
                                    note = note
                                )
                            )
                        }
                    }
                } else if (finalText.startsWith("CATEGORY|")) {
                    val parts = finalText.substringAfter("CATEGORY|").split("|")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val type = parts[1].trim()

                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouvelle categorie proposee :",
                                proposedCategory = ProposedCategory(
                                    name = name,
                                    type = type
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                updateMessage(streamId) {
                    it.copy(text = "Erreur: ${e.localizedMessage}", isError = true, isLoading = false)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmTransaction(proposed: ProposedTransaction) {
        viewModelScope.launch {
            try {
                val categories = getAllCategoryUseCase.invoke().firstOrNull() ?: emptyList()
                val category = categories.find {
                    it.name.equals(proposed.categoryName, ignoreCase = true)
                } ?: categories.firstOrNull { it.type == CategoryType.EXPENSE }

                val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                val account = accounts.firstOrNull { it.type == AccountType.REGULAR } ?: accounts.firstOrNull()

                if (category == null || account == null) {
                    addMessage(ChatMessage(isUser = false, text = "Erreur : Compte ou categorie introuvable.", isError = true))
                    return@launch
                }

                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    notes = proposed.note,
                    categoryId = category.id,
                    fromAccountId = account.id,
                    toAccountId = null,
                    type = if (category.type == CategoryType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME,
                    amount = Amount(proposed.amount),
                    imagePath = "",
                    createdOn = Date(),
                    updatedOn = Date()
                )

                addTransactionUseCase.invoke(transaction)
                addMessage(ChatMessage(isUser = false, text = "Transaction enregistree avec succes !"))

            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de l'enregistrement.", isError = true))
            }
        }
    }

    fun rejectTransaction() {
        addMessage(ChatMessage(isUser = false, text = "Transaction annulee."))
    }

    fun confirmCategory(proposed: ProposedCategory) {
        viewModelScope.launch {
            try {
                val catType = if (proposed.type.equals("INCOME", ignoreCase = true)) CategoryType.INCOME else CategoryType.EXPENSE
                val category = Category(
                    id = UUID.randomUUID().toString(),
                    name = proposed.name,
                    type = catType,
                    storedIcon = StoredIcon("ic_other", "#455A64"),
                    createdOn = Date(),
                    updatedOn = Date()
                )

                addCategoryUseCase.invoke(category)
                addMessage(ChatMessage(isUser = false, text = "Categorie \"${proposed.name}\" creee avec succes !"))

            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la creation.", isError = true))
            }
        }
    }

    fun rejectCategory() {
        addMessage(ChatMessage(isUser = false, text = "Creation de categorie annulee."))
    }
}
