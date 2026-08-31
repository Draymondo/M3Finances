package com.naveenapps.expensemanager.feature.transaction.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.AddCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.matchCategory
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

import com.naveenapps.expensemanager.core.domain.usecase.account.AddAccountUseCase
import com.naveenapps.expensemanager.core.model.Account

import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListItemUseCase
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.ShoppingListItem

import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.AddSavingsGoalUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.AddBudgetUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.AddDebtUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.AddRecurringTransactionUseCase
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val accountRepository: AccountRepository,
    private val addShoppingListUseCase: AddShoppingListUseCase,
    private val addShoppingListItemUseCase: AddShoppingListItemUseCase,
    private val addSavingsGoalUseCase: AddSavingsGoalUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
    private val addDebtUseCase: AddDebtUseCase,
    private val addRecurringTransactionUseCase: AddRecurringTransactionUseCase
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
                text("""
                    Assistant financier francais. Reponses courtes.
                    Pour creer une transaction simple: TRANSACTION|montant|categorie|note
                    Pour creer une transaction scindee (plusieurs articles): TRANSACTION_SPLIT|montant_total|note_globale|montant1|categorie1|note1|montant2|categorie2|note2...
                    Pour creer une categorie: CATEGORY|nom|EXPENSE ou CATEGORY|nom|INCOME
                    Pour creer un compte: ACCOUNT|nom|REGULAR ou ACCOUNT|nom|CREDIT ou ACCOUNT|nom|MOBILE_MONEY
                    Pour creer une liste de courses: SHOPPING_LIST|nom de la liste|article1|article2|article3...
                    Pour creer un objectif d'epargne: SAVINGS_GOAL|nom|montant_cible
                    Pour creer un budget mensuel global: BUDGET|montant
                    Pour creer une dette/emprunt: DEBT|nom_de_la_personne|montant|LENT (si on m'a emprunte) ou BORROWED (si j'ai emprunte)
                    Pour creer une transaction recurrente (abonnement): RECURRING|nom|montant|EXPENSE ou RECURRING|nom|montant|INCOME
                    
                    Use ONLY these exact categories when proposing a transaction: Food, Transportation, Shopping, Health, Entertainment, Utilities, Leisure, Clothing, Education, Salary, Gift, Coupons.
                    If none match, fallback to Other or propose to create a new one.
                """.trimIndent())
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

                if (finalText.contains("TRANSACTION|") || finalText.contains("TRANSACTION_SPLIT|")) {
                    val isSplit = finalText.contains("TRANSACTION_SPLIT|")
                    val command = if (isSplit) "TRANSACTION_SPLIT|" else "TRANSACTION|"
                    val commandStr = finalText.substringAfter(command).substringBefore("\n").replace("`", "").trim()
                    val parts = commandStr.split("|")
                    if (parts.size >= 3) {
                        val amount = parts[0].trim().toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            updateMessage(streamId) {
                                it.copy(
                                    text = "Erreur : Le montant de la transaction doit être supérieur à 0.",
                                    isError = true
                                )
                            }
                        } else {
                            val category = if (isSplit) "Multiple" else parts[1].trim()
                            val note = if (isSplit) parts[1].trim() else parts.drop(2).joinToString("|").trim()
                            
                            val splitItems = mutableListOf<ProposedSplitItem>()
                            if (isSplit) {
                                var i = 2
                                while (i + 2 < parts.size) {
                                    val itemAmount = parts[i].trim().toDoubleOrNull() ?: 0.0
                                    val itemCat = parts[i+1].trim()
                                    val itemNote = parts[i+2].trim()
                                    if (itemAmount > 0.0) {
                                        splitItems.add(ProposedSplitItem(itemAmount, itemCat, itemNote))
                                    }
                                    i += 3
                                }
                            }

                            // Select best account
                            val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                            val isMobileMoney = note.contains("Wave", ignoreCase = true) ||
                                                note.contains("Orange", ignoreCase = true) ||
                                                note.contains("MTN", ignoreCase = true) ||
                                                note.contains("MoMo", ignoreCase = true)
                                                
                            var selectedAccount = if (isMobileMoney) {
                                accounts.find {
                                    it.name.contains("Wave", ignoreCase = true) ||
                                    it.name.contains("Orange", ignoreCase = true) ||
                                    it.name.contains("MTN", ignoreCase = true) ||
                                    it.name.contains("MoMo", ignoreCase = true)
                                }
                            } else null
                            
                            // fallback to REGULAR, then non-DEBT/SAVINGS, then first
                            if (selectedAccount == null) {
                                selectedAccount = accounts.firstOrNull { it.type == com.naveenapps.expensemanager.core.model.AccountType.REGULAR }
                                    ?: accounts.firstOrNull { 
                                        it.type != com.naveenapps.expensemanager.core.model.AccountType.CREDIT && 
                                        it.type != com.naveenapps.expensemanager.core.model.AccountType.DEBT &&
                                        it.type != com.naveenapps.expensemanager.core.model.AccountType.SAVINGS_GOAL 
                                    }
                                    ?: accounts.firstOrNull()
                            }

                            if (selectedAccount == null) {
                                updateMessage(streamId) {
                                    it.copy(
                                        text = "Erreur : Aucun compte disponible pour enregistrer la transaction.",
                                        isError = true
                                    )
                                }
                            } else {
                                updateMessage(streamId) {
                                    it.copy(
                                        text = if (isSplit) "J'ai préparé cette transaction scindée :" else "J'ai préparé cette transaction :",
                                        proposedTransaction = ProposedTransaction(
                                            amount = if (isSplit && splitItems.isNotEmpty()) splitItems.sumOf { it.amount } else amount,
                                            categoryName = category,
                                            note = note,
                                            accountId = selectedAccount.id,
                                            accountName = selectedAccount.name,
                                            splitItems = if (isSplit && splitItems.isNotEmpty()) splitItems else null
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (finalText.contains("CATEGORY|")) {
                    val commandStr = finalText.substringAfter("CATEGORY|").substringBefore("\n").replace("`", "").trim()
                    val parts = commandStr.split("|")
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
                } else if (finalText.contains("ACCOUNT|")) {
                    val commandStr = finalText.substringAfter("ACCOUNT|").substringBefore("\n").replace("`", "").trim()
                    val parts = commandStr.split("|")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val typeStr = parts[1].trim().uppercase()
                        
                        val type = when (typeStr) {
                            "CREDIT" -> com.naveenapps.expensemanager.core.model.AccountType.CREDIT
                            "MOBILE_MONEY" -> com.naveenapps.expensemanager.core.model.AccountType.MOBILE_MONEY
                            else -> com.naveenapps.expensemanager.core.model.AccountType.REGULAR
                        }

                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouveau compte proposé :",
                                proposedAccount = ProposedAccount(
                                    name = name,
                                    type = type
                                )
                            )
                        }
                    }
                } else if (finalText.contains("SHOPPING_LIST|")) {
                    val commandStr = finalText.substringAfter("SHOPPING_LIST|").substringBefore("\n").replace("`", "").trim()
                    val parts = commandStr.split("|")
                    if (parts.isNotEmpty()) {
                        val name = parts[0].trim()
                        val items = parts.drop(1).map { it.trim() }.filter { it.isNotBlank() }

                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouvelle liste de courses proposée :",
                                proposedShoppingList = ProposedShoppingList(
                                    name = name,
                                    items = items
                                )
                            )
                        }
                    }
                } else if (finalText.contains("SAVINGS_GOAL|")) {
                    val parts = finalText.substringAfter("SAVINGS_GOAL|").substringBefore("\n").replace("`", "").trim().split("|")
                    if (parts.size >= 2) {
                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouvel objectif d'épargne proposé :",
                                proposedSavingsGoal = ProposedSavingsGoal(
                                    name = parts[0].trim(),
                                    targetAmount = parts[1].trim().toDoubleOrNull() ?: 0.0
                                )
                            )
                        }
                    }
                } else if (finalText.contains("BUDGET|")) {
                    val parts = finalText.substringAfter("BUDGET|").substringBefore("\n").replace("`", "").trim().split("|")
                    if (parts.isNotEmpty()) {
                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouveau budget global proposé :",
                                proposedBudget = ProposedBudget(
                                    amount = parts[0].trim().toDoubleOrNull() ?: 0.0
                                )
                            )
                        }
                    }
                } else if (finalText.contains("DEBT|")) {
                    val parts = finalText.substringAfter("DEBT|").substringBefore("\n").replace("`", "").trim().split("|")
                    if (parts.size >= 3) {
                        val dirStr = parts[2].trim()
                        val dir = if (dirStr.equals("LENT", ignoreCase = true)) DebtDirection.LENT else DebtDirection.BORROWED
                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouvelle dette/emprunt proposé(e) :",
                                proposedDebt = ProposedDebt(
                                    personName = parts[0].trim(),
                                    amount = parts[1].trim().toDoubleOrNull() ?: 0.0,
                                    direction = dir
                                )
                            )
                        }
                    }
                } else if (finalText.contains("RECURRING|")) {
                    val parts = finalText.substringAfter("RECURRING|").substringBefore("\n").replace("`", "").trim().split("|")
                    if (parts.size >= 3) {
                        val typeStr = parts[2].trim()
                        val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                        updateMessage(streamId) {
                            it.copy(
                                text = "Nouvelle transaction récurrente proposée :",
                                proposedRecurring = ProposedRecurring(
                                    name = parts[0].trim(),
                                    amount = parts[1].trim().toDoubleOrNull() ?: 0.0,
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
                val category = categories.matchCategory(proposed.categoryName) 
                    ?: if (proposed.splitItems?.isNotEmpty() == true) categories.firstOrNull { it.type == CategoryType.EXPENSE } else null

                val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                val account = accounts.find { it.id == proposed.accountId } 
                    ?: accounts.firstOrNull { it.type == com.naveenapps.expensemanager.core.model.AccountType.REGULAR } 
                    ?: accounts.firstOrNull()

                if (category == null || account == null) {
                    addMessage(ChatMessage(isUser = false, text = "Erreur : Compte ou catégorie introuvable.", isError = true))
                    return@launch
                }

                val transactionId = UUID.randomUUID().toString()
                
                val splitItems = proposed.splitItems?.mapNotNull { item ->
                    val itemCat = categories.matchCategory(item.categoryName) ?: category
                    com.naveenapps.expensemanager.core.model.TransactionSplitItem(
                        id = UUID.randomUUID().toString(),
                        transactionId = transactionId,
                        categoryId = itemCat.id,
                        amount = Amount(item.amount),
                        notes = item.note.ifBlank { null },
                        category = itemCat
                    )
                } ?: emptyList()

                val transaction = Transaction(
                    id = transactionId,
                    notes = proposed.note,
                    categoryId = category.id,
                    fromAccountId = account.id,
                    toAccountId = null,
                    type = if (category.type == CategoryType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME,
                    amount = Amount(proposed.amount),
                    imagePath = "",
                    createdOn = Date(),
                    updatedOn = Date(),
                    splitItems = splitItems
                )

                val result = addTransactionUseCase.invoke(transaction)
                if (result is com.naveenapps.expensemanager.core.model.Resource.Success) {
                    addMessage(ChatMessage(isUser = false, text = "Transaction enregistrée avec succès !"))
                } else if (result is com.naveenapps.expensemanager.core.model.Resource.Error) {
                    val errorMsg = result.exception.message ?: "Erreur inconnue"
                    addMessage(ChatMessage(isUser = false, text = "Échec de l'enregistrement : $errorMsg", isError = true))
                }

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

    fun confirmAccount(proposed: ProposedAccount) {
        viewModelScope.launch {
            try {
                val account = Account(
                    id = UUID.randomUUID().toString(),
                    name = proposed.name,
                    type = proposed.type,
                    storedIcon = StoredIcon("account_balance", "#455A64"),
                    createdOn = Date(),
                    updatedOn = Date(),
                    amount = 0.0,
                    creditLimit = 0.0
                )

                addAccountUseCase.invoke(account)
                addMessage(ChatMessage(isUser = false, text = "Compte \"${proposed.name}\" créé avec succès !"))

            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création du compte.", isError = true))
            }
        }
    }

    fun confirmShoppingList(proposed: ProposedShoppingList) {
        viewModelScope.launch {
            try {
                // Find a default account and category
                val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                val categories = getAllCategoryUseCase.invoke().firstOrNull() ?: emptyList()
                
                val defaultAccount = accounts.firstOrNull { it.type == AccountType.REGULAR } ?: accounts.firstOrNull()
                val defaultCategory = categories.firstOrNull { it.type == CategoryType.EXPENSE } ?: categories.firstOrNull()
                
                if (defaultAccount == null || defaultCategory == null) {
                    addMessage(ChatMessage(isUser = false, text = "Impossible de créer la liste: aucun compte ou catégorie par défaut.", isError = true))
                    return@launch
                }

                val listId = UUID.randomUUID().toString()
                val shoppingList = ShoppingList(
                    id = listId,
                    name = proposed.name,
                    categoryId = defaultCategory.id,
                    accountId = defaultAccount.id,
                    createdOn = Date(),
                    updatedOn = Date()
                )

                val listResult = addShoppingListUseCase.invoke(shoppingList)
                if (listResult is com.naveenapps.expensemanager.core.model.Resource.Success) {
                    // Add items
                    var itemsAdded = 0
                    proposed.items.forEach { itemName ->
                        val item = ShoppingListItem(
                            id = UUID.randomUUID().toString(),
                            shoppingListId = listId,
                            name = itemName,
                            price = 0.0, // Default price 0 until modified or checked off
                            createdOn = Date()
                        )
                        val itemResult = addShoppingListItemUseCase.invoke(item)
                        if (itemResult is com.naveenapps.expensemanager.core.model.Resource.Success) {
                            itemsAdded++
                        }
                    }
                    
                    addMessage(ChatMessage(isUser = false, text = "Liste \"${proposed.name}\" créée avec $itemsAdded articles !"))
                } else {
                    addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création de la liste.", isError = true))
                }
            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création de la liste.", isError = true))
            }
        }
    }

    fun confirmSavingsGoal(proposed: ProposedSavingsGoal) {
        viewModelScope.launch {
            try {
                val goal = SavingsGoal(
                    id = UUID.randomUUID().toString(),
                    accountId = UUID.randomUUID().toString(),
                    name = proposed.name,
                    targetAmount = proposed.targetAmount,
                    targetDate = null,
                    notes = "",
                    isAchieved = false,
                    createdOn = Date(),
                    updatedOn = Date()
                )
                addSavingsGoalUseCase.invoke(goal, 0.0, null)
                addMessage(ChatMessage(isUser = false, text = "Objectif d'épargne \"${proposed.name}\" créé avec succès !"))
            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création de l'objectif.", isError = true))
            }
        }
    }
    
    fun confirmBudget(proposed: ProposedBudget) {
        viewModelScope.launch {
            try {
                val budget = Budget(
                    id = UUID.randomUUID().toString(),
                    amount = proposed.amount,
                    selectedMonth = Date().toMonthAndYearKey(),
                    periodType = BudgetPeriod.MONTHLY,
                    categories = emptyList(),
                    accounts = emptyList(),
                    isAllAccountsSelected = true,
                    isAllCategoriesSelected = true,
                    createdOn = Date(),
                    updatedOn = Date()
                )
                addBudgetUseCase.invoke(budget)
                addMessage(ChatMessage(isUser = false, text = "Budget mensuel global de ${proposed.amount} créé avec succès !"))
            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création du budget.", isError = true))
            }
        }
    }
    
    fun confirmDebt(proposed: ProposedDebt) {
        viewModelScope.launch {
            try {
                val debt = Debt(
                    id = UUID.randomUUID().toString(),
                    accountId = UUID.randomUUID().toString(),
                    personName = proposed.personName,
                    direction = proposed.direction,
                    dueDate = null,
                    notes = "",
                    isSettled = false,
                    createdOn = Date(),
                    updatedOn = Date()
                )
                addDebtUseCase.invoke(debt, proposed.amount, null)
                addMessage(ChatMessage(isUser = false, text = "Dette pour \"${proposed.personName}\" créée avec succès !"))
            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création de la dette.", isError = true))
            }
        }
    }
    
    fun confirmRecurring(proposed: ProposedRecurring) {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                val categories = getAllCategoryUseCase.invoke().firstOrNull() ?: emptyList()
                val defaultAccount = accounts.firstOrNull { it.type == AccountType.REGULAR } ?: accounts.firstOrNull()
                val defaultCategory = categories.firstOrNull { it.type == if(proposed.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE } ?: categories.firstOrNull()
                
                if (defaultAccount == null || defaultCategory == null) {
                    addMessage(ChatMessage(isUser = false, text = "Impossible de créer la transaction récurrente: compte ou catégorie manquants.", isError = true))
                    return@launch
                }
                
                val recurring = RecurringTransaction(
                    id = UUID.randomUUID().toString(),
                    notes = proposed.name,
                    categoryId = defaultCategory.id,
                    fromAccountId = defaultAccount.id,
                    toAccountId = null,
                    amount = Amount(proposed.amount),
                    type = proposed.type,
                    frequency = RecurrenceFrequency.MONTHLY,
                    interval = 1,
                    startDate = Date(),
                    endDate = null,
                    nextOccurrenceDate = Date(),
                    isActive = true,
                    createdOn = Date(),
                    updatedOn = Date()
                )
                addRecurringTransactionUseCase.invoke(recurring)
                addMessage(ChatMessage(isUser = false, text = "Abonnement/Récurrent \"${proposed.name}\" créé avec succès !"))
            } catch (e: Exception) {
                addMessage(ChatMessage(isUser = false, text = "Erreur lors de la création de la transaction récurrente.", isError = true))
            }
        }
    }

    fun rejectCategory() {
        addMessage(ChatMessage(isUser = false, text = "Création de catégorie annulée."))
    }
    
    fun rejectAccount() {
        addMessage(ChatMessage(isUser = false, text = "Création de compte annulée."))
    }
    
    fun rejectShoppingList() {
        addMessage(ChatMessage(isUser = false, text = "Création de liste de courses annulée."))
    }
    
    fun rejectSavingsGoal() {
        addMessage(ChatMessage(isUser = false, text = "Création d'objectif d'épargne annulée."))
    }
    
    fun rejectBudget() {
        addMessage(ChatMessage(isUser = false, text = "Création de budget annulée."))
    }
    
    fun rejectDebt() {
        addMessage(ChatMessage(isUser = false, text = "Création de dette annulée."))
    }
    
    fun rejectRecurring() {
        addMessage(ChatMessage(isUser = false, text = "Création de transaction récurrente annulée."))
    }
}
