package com.naveenapps.expensemanager.feature.transaction.chat

import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
private val SuccessColor = Color(0xFF3ECF8E)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    appComposeNavigator: AppComposeNavigator = koinInject()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    
    val lastMessageTextLength = messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(messages.size, lastMessageTextLength) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                onBackClick = { appComposeNavigator.popBackStack() }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(
                        message = message,
                        viewModel = viewModel
                    )
                }

                // Intro hints if only welcome message (placed below the welcome message)
                if (messages.size == 1 && !messages[0].isUser) {
                    item {
                        HintChips(onHintClick = { text -> viewModel.sendMessage(text, null) })
                    }
                }
            }

            ChatInputArea(
                isLoading = isLoading,
                onSendMessage = { text, image ->
                    viewModel.sendMessage(text, image)
                }
            )
        }
    }
}

@Composable
private fun ChatTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Assistant", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Gemini", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HintChips(onHintClick: (String) -> Unit) {
    val hints = listOf("5 000 pharmacie Wave", "Créer une catégorie", "Scanner un ticket")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        hints.forEach { hint ->
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onHintClick(hint) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(hint, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    viewModel: ChatViewModel
) {
    if (message.isUser) {
        UserMessage(message)
    } else {
        AiMessage(message, viewModel)
    }
}

@Composable
private fun UserMessage(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            message.imageBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            if (!message.text.isNullOrBlank()) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun AiMessage(message: ChatMessage, viewModel: ChatViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFF3B5BDB), Color(0xFF7048E8))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("M3", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            if (message.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    strokeWidth = 2.dp
                )
                return@Column
            }

            if (message.text == "Transaction enregistrée avec succès !" || message.text?.contains("avec succès") == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(message.text, color = SuccessColor, fontSize = 15.sp)
                }
                return@Column
            }

            if (!message.text.isNullOrBlank()) {
                val color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                val annotatedText = formatMarkdown(message.text)
                
                Row(verticalAlignment = Alignment.Top) {
                    if (message.isError) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = annotatedText,
                        color = color,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Proposal Cards
            message.proposedTransaction?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                EditableTransactionProposalCard(
                    proposal = proposed,
                    onConfirm = { edited -> viewModel.confirmTransaction(message.id, edited) },
                    onReject = { viewModel.rejectTransaction(message.id) }
                )
            }

            message.proposedCategory?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                EditableCategoryProposalCard(
                    proposal = proposed,
                    onConfirm = { edited -> viewModel.confirmCategory(message.id, edited) },
                    onReject = { viewModel.rejectCategory(message.id) }
                )
            }

            message.proposedAccount?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                EditableAccountProposalCard(
                    proposal = proposed,
                    onConfirm = { edited -> viewModel.confirmAccount(message.id, edited) },
                    onReject = { viewModel.rejectAccount(message.id) }
                )
            }
            
            message.proposedShoppingList?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "LISTE DE COURSES",
                    mainText = proposed.name,
                    items = proposed.items.map { "Article" to it },
                    onConfirm = { viewModel.confirmShoppingList(message.id, proposed) },
                    onReject = { viewModel.rejectShoppingList(message.id) }
                )
            }

            message.proposedSavingsGoal?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "OBJECTIF ÉPARGNE",
                    mainText = proposed.name,
                    items = listOf("Cible" to proposed.targetAmount.toString()),
                    onConfirm = { viewModel.confirmSavingsGoal(message.id, proposed) },
                    onReject = { viewModel.rejectSavingsGoal(message.id) }
                )
            }

            message.proposedBudget?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                val typeText = if (proposed.goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME) {
                    "Revenu"
                } else {
                    "Dépense"
                }
                GenericProposalCard(
                    title = "BUDGET",
                    mainText = "Budget Mensuel ($typeText)",
                    items = listOf("Montant" to proposed.amount.toString()),
                    onConfirm = { viewModel.confirmBudget(message.id, proposed) },
                    onReject = { viewModel.rejectBudget(message.id) }
                )
            }

            message.proposedDebt?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                val dirText = if (proposed.direction.name == "LENT") "Prêt à (Il me doit)" else "Emprunt de (Je lui dois)"
                GenericProposalCard(
                    title = "DETTE",
                    mainText = proposed.personName,
                    items = listOf("Type" to dirText, "Montant" to proposed.amount.toString()),
                    onConfirm = { viewModel.confirmDebt(message.id, proposed) },
                    onReject = { viewModel.rejectDebt(message.id) }
                )
            }

            message.proposedRecurring?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "RÉCURRENT",
                    mainText = proposed.name,
                    items = listOf("Montant" to proposed.amount.toString(), "Type" to proposed.type.name),
                    onConfirm = { viewModel.confirmRecurring(message.id, proposed) },
                    onReject = { viewModel.rejectRecurring(message.id) }
                )
            }
        }
    }
}

@Composable
private fun EditableTransactionProposalCard(
    proposal: ProposedTransaction,
    onConfirm: (ProposedTransaction) -> Unit,
    onReject: () -> Unit
) {
    var amountText by remember { mutableStateOf(proposal.amount.toString()) }
    var categoryText by remember { mutableStateOf(proposal.categoryName) }
    var accountText by remember { mutableStateOf(proposal.accountName ?: "") }
    var noteText by remember { mutableStateOf(proposal.note) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DÉPENSE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Montant") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                label = { Text("Catégorie") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = accountText,
                onValueChange = { accountText = it },
                label = { Text("Compte") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    val updated = proposal.copy(
                        amount = amountText.toDoubleOrNull() ?: proposal.amount,
                        categoryName = categoryText.ifBlank { proposal.categoryName },
                        accountName = accountText.ifBlank { proposal.accountName },
                        note = noteText.ifBlank { proposal.note }
                    )
                    onConfirm(updated)
                }) {
                    Text("Enregistrer", color = SuccessColor)
                }
            }
        }
    }
}

@Composable
private fun EditableCategoryProposalCard(
    proposal: ProposedCategory,
    onConfirm: (ProposedCategory) -> Unit,
    onReject: () -> Unit
) {
    var nameText by remember { mutableStateOf(proposal.name) }
    var typeText by remember { mutableStateOf(proposal.type) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CATÉGORIE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = typeText,
                onValueChange = { typeText = it },
                label = { Text("Type") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onConfirm(proposal.copy(name = nameText.ifBlank { proposal.name }, type = typeText.ifBlank { proposal.type })) }) {
                    Text("Enregistrer", color = SuccessColor)
                }
            }
        }
    }
}

@Composable
private fun EditableAccountProposalCard(
    proposal: ProposedAccount,
    onConfirm: (ProposedAccount) -> Unit,
    onReject: () -> Unit
) {
    var nameText by remember { mutableStateOf(proposal.name) }
    var typeText by remember { mutableStateOf(proposal.type.name) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("COMPTE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = typeText,
                onValueChange = { typeText = it },
                label = { Text("Type") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    val resolvedType = when (typeText.uppercase()) {
                        "CREDIT" -> com.naveenapps.expensemanager.core.model.AccountType.CREDIT
                        "MOBILE_MONEY" -> com.naveenapps.expensemanager.core.model.AccountType.MOBILE_MONEY
                        else -> com.naveenapps.expensemanager.core.model.AccountType.REGULAR
                    }
                    onConfirm(proposal.copy(name = nameText.ifBlank { proposal.name }, type = resolvedType))
                }) {
                    Text("Enregistrer", color = SuccessColor)
                }
            }
        }
    }
}

@Composable
private fun TransactionProposalCard(
    amount: Double,
    categoryName: String,
    accountName: String?,
    note: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DÉPENSE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))

            Text(if (amount % 1.0 == 0.0) "${amount.toLong()}" else "$amount", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            ProposalRow("Catégorie", categoryName)
            if (accountName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ProposalRow("Compte", accountName)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ProposalRow("Note", note.ifBlank { "Aucune" })

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onConfirm) {
                    Text("Enregistrer", color = SuccessColor)
                }
            }
        }
    }
}

@Composable
private fun GenericProposalCard(
    title: String,
    mainText: String,
    items: List<Pair<String, String>>,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(mainText, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEachIndexed { index, pair ->
                ProposalRow(pair.first, pair.second)
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onConfirm) {
                    Text("Enregistrer", color = SuccessColor)
                }
            }
        }
    }
}

@Composable
private fun ProposalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatInputArea(
    isLoading: Boolean,
    onSendMessage: (String, Bitmap?) -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempUri?.let { uri ->
                selectedImage = com.naveenapps.expensemanager.core.common.utils.ImageUtils.getResizedBitmap(context, uri)
            }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImage = com.naveenapps.expensemanager.core.common.utils.ImageUtils.getResizedBitmap(context, it)
        }
    }
    
    var isListening by remember { mutableStateOf(false) }
    var listeningBaseText by remember { mutableStateOf("") }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    val speechRecognizer = remember { android.speech.SpeechRecognizer.createSpeechRecognizer(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            listeningBaseText = inputText
            speechRecognizer.startListening(intent)
            isListening = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = rmsdB.coerceIn(0f, 10f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false; rmsLevel = 0f }
            override fun onError(error: Int) { isListening = false; rmsLevel = 0f }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    inputText = if (listeningBaseText.isEmpty()) text else "$listeningBaseText $text"
                }
                isListening = false
                rmsLevel = 0f
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0]
                    inputText = if (listeningBaseText.isEmpty()) partial else "$listeningBaseText $partial"
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        selectedImage?.let {
            Box(modifier = Modifier.padding(bottom = 12.dp)) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = { selectedImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Supprimer", modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showAttachmentMenu by remember { mutableStateOf(false) }
            
            Box {
                IconButton(onClick = { showAttachmentMenu = true }, enabled = !isLoading) {
                    Icon(Icons.Default.Add, contentDescription = "Joindre", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                DropdownMenu(
                    expanded = showAttachmentMenu,
                    onDismissRequest = { showAttachmentMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Caméra", color = MaterialTheme.colorScheme.onBackground) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) },
                        onClick = {
                            showAttachmentMenu = false
                            val file = java.io.File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            tempUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Photos", color = MaterialTheme.colorScheme.onBackground) },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) },
                        onClick = {
                            showAttachmentMenu = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            }
            
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                    innerTextField()
                },
                enabled = !isLoading
            )
            
            // Cancel button — visible only while listening
            if (isListening) {
                IconButton(
                    onClick = {
                        speechRecognizer.cancel()
                        inputText = listeningBaseText
                        isListening = false
                        rmsLevel = 0f
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Annuler dictée", tint = MaterialTheme.colorScheme.error)
                }
            }

            // Mic button with animated volume ring
            val animatedRms by animateFloatAsState(targetValue = rmsLevel, label = "rms")
            val micRingColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            speechRecognizer.stopListening()
                            isListening = false
                            rmsLevel = 0f
                        } else {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                }
                                listeningBaseText = inputText
                                speechRecognizer.startListening(intent)
                                isListening = true
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = if (isListening) {
                        Modifier.drawBehind {
                            val radius = size.minDimension / 2f + (animatedRms / 10f) * 16.dp.toPx()
                            drawCircle(color = micRingColor, radius = radius, style = Stroke(width = 3.dp.toPx()))
                        }
                    } else Modifier
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Vocal", tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val isSendEnabled = !isLoading && (inputText.isNotBlank() || selectedImage != null)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = isSendEnabled) {
                        onSendMessage(inputText, selectedImage)
                        inputText = ""
                        selectedImage = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, 
                    contentDescription = "Envoyer", 
                    tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun formatMarkdown(text: String): AnnotatedString {
    val textWithBullets = text.replace(Regex("(?m)^\\*\\s"), "• ")
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = Regex("(\\*\\*(.*?)\\*\\*)|(`(.*?)`)")
        val matches = regex.findAll(textWithBullets)

        for (match in matches) {
            append(textWithBullets.substring(currentIndex, match.range.first))
            val value = match.value
            when {
                value.startsWith("**") && value.endsWith("**") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(value.removeSurrounding("**"))
                    }
                }
                value.startsWith("`") && value.endsWith("`") -> {
                    withStyle(style = SpanStyle(background = surfaceVariantColor, fontFamily = FontFamily.Monospace)) {
                        append(value.removeSurrounding("`"))
                    }
                }
            }
            currentIndex = match.range.last + 1
        }
        append(textWithBullets.substring(currentIndex))
    }
}
