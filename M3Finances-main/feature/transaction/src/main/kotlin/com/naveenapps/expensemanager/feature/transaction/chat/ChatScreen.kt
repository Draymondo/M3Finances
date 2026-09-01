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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

// Theme colors hardcoded for dark minimalist style
private val BgColor = Color(0xFF0C0C0E)
private val SurfaceColor = Color(0xFF141416)
private val UserBubbleColor = Color(0xFF2A2A30)
private val BorderColor = Color.White.copy(alpha = 0.07f)
private val TextPrimary = Color(0xFFECECEF)
private val TextSecondary = Color(0xFF8E8E96)
private val TextTertiary = Color(0xFF5C5C66)
private val SuccessColor = Color(0xFF3ECF8E)
private val ErrorColor = Color(0xFFF07178)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    appComposeNavigator: AppComposeNavigator = koinInject()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
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
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BgColor)
                .imePadding()
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
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Assistant", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Gemini", color = TextSecondary, fontSize = 12.sp)
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
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable { onHintClick(hint) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(hint, color = TextPrimary, fontSize = 14.sp)
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
                    color = UserBubbleColor,
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
                    color = TextPrimary,
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
                    color = TextSecondary,
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
                val color = if (message.isError) ErrorColor else TextPrimary
                val annotatedText = formatMarkdown(message.text)
                
                Row(verticalAlignment = Alignment.Top) {
                    if (message.isError) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
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
                    onConfirm = { edited -> viewModel.confirmTransaction(edited) },
                    onReject = { viewModel.rejectTransaction() }
                )
            }

            message.proposedCategory?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                EditableCategoryProposalCard(
                    proposal = proposed,
                    onConfirm = { edited -> viewModel.confirmCategory(edited) },
                    onReject = { viewModel.rejectCategory() }
                )
            }

            message.proposedAccount?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                EditableAccountProposalCard(
                    proposal = proposed,
                    onConfirm = { edited -> viewModel.confirmAccount(edited) },
                    onReject = { viewModel.rejectAccount() }
                )
            }
            
            message.proposedShoppingList?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "LISTE DE COURSES",
                    mainText = proposed.name,
                    items = proposed.items.map { "Article" to it },
                    onConfirm = { viewModel.confirmShoppingList(proposed) },
                    onReject = { viewModel.rejectShoppingList() }
                )
            }

            message.proposedSavingsGoal?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "OBJECTIF ÉPARGNE",
                    mainText = proposed.name,
                    items = listOf("Cible" to proposed.targetAmount.toString()),
                    onConfirm = { viewModel.confirmSavingsGoal(proposed) },
                    onReject = { viewModel.rejectSavingsGoal() }
                )
            }

            message.proposedBudget?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "BUDGET",
                    mainText = "Budget Mensuel",
                    items = listOf("Montant" to proposed.amount.toString()),
                    onConfirm = { viewModel.confirmBudget(proposed) },
                    onReject = { viewModel.rejectBudget() }
                )
            }

            message.proposedDebt?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                val dirText = if (proposed.direction.name == "LENT") "Prêt à (Il me doit)" else "Emprunt de (Je lui dois)"
                GenericProposalCard(
                    title = "DETTE",
                    mainText = proposed.personName,
                    items = listOf("Type" to dirText, "Montant" to proposed.amount.toString()),
                    onConfirm = { viewModel.confirmDebt(proposed) },
                    onReject = { viewModel.rejectDebt() }
                )
            }

            message.proposedRecurring?.let { proposed ->
                Spacer(modifier = Modifier.height(12.dp))
                GenericProposalCard(
                    title = "RÉCURRENT",
                    mainText = proposed.name,
                    items = listOf("Montant" to proposed.amount.toString(), "Type" to proposed.type.name),
                    onConfirm = { viewModel.confirmRecurring(proposed) },
                    onReject = { viewModel.rejectRecurring() }
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
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DÉPENSE", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Montant") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CATÉGORIE", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("COMPTE", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                    focusedBorderColor = BorderColor,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DÉPENSE", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))

            Text(if (amount % 1.0 == 0.0) "${amount.toLong()}" else "$amount", fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            ProposalRow("Catégorie", categoryName)
            if (accountName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ProposalRow("Compte", accountName)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ProposalRow("Note", note.ifBlank { "Aucune" })

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(mainText, fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEachIndexed { index, pair ->
                ProposalRow(pair.first, pair.second)
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onReject) {
                    Text("Annuler", color = TextSecondary)
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
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatInputArea(
    isLoading: Boolean,
    onSendMessage: (String, Bitmap?) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
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
    val speechRecognizer = remember { android.speech.SpeechRecognizer.createSpeechRecognizer(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            speechRecognizer.startListening(intent)
            isListening = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    inputText = if (inputText.isEmpty()) text else "$inputText $text"
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
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
            .background(BgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.ime)
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
                .background(SurfaceColor, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showAttachmentMenu by remember { mutableStateOf(false) }
            
            Box {
                IconButton(onClick = { showAttachmentMenu = true }, enabled = !isLoading) {
                    Icon(Icons.Default.Add, contentDescription = "Joindre", tint = TextSecondary)
                }
                
                DropdownMenu(
                    expanded = showAttachmentMenu,
                    onDismissRequest = { showAttachmentMenu = false },
                    modifier = Modifier.background(SurfaceColor),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Caméra", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextPrimary) },
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
                        text = { Text("Photos", color = TextPrimary) },
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
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(TextPrimary),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text("Message...", color = TextSecondary, fontSize = 15.sp)
                    }
                    innerTextField()
                },
                enabled = !isLoading
            )
            
            IconButton(
                onClick = {
                    if (isListening) {
                        speechRecognizer.stopListening()
                        isListening = false
                    } else {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechRecognizer.startListening(intent)
                            isListening = true
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Vocal", tint = if (isListening) ErrorColor else TextSecondary)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (!isLoading && (inputText.isNotBlank() || selectedImage != null)) Color.White else Color(0xFF333333))
                    .clickable(enabled = !isLoading && (inputText.isNotBlank() || selectedImage != null)) {
                        onSendMessage(inputText, selectedImage)
                        inputText = ""
                        selectedImage = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color(0xFF0C0C0E), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

fun formatMarkdown(text: String): AnnotatedString {
    val textWithBullets = text.replace(Regex("(?m)^\\*\\s"), "• ")
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
                    withStyle(style = SpanStyle(background = Color(0xFF2A2A30), fontFamily = FontFamily.Monospace)) {
                        append(value.removeSurrounding("`"))
                    }
                }
            }
            currentIndex = match.range.last + 1
        }
        append(textWithBullets.substring(currentIndex))
    }
}
