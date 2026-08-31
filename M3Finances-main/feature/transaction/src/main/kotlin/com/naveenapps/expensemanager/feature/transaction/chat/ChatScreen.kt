package com.naveenapps.expensemanager.feature.transaction.chat

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.feature.transaction.R
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                title = "Assistant",
                navigationIcon = null,
                navigationBackClick = {}
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(
                        message = message,
                        onConfirmTransaction = { viewModel.confirmTransaction(it) },
                        onRejectTransaction = { viewModel.rejectTransaction() },
                        onConfirmCategory = { viewModel.confirmCategory(it) },
                        onRejectCategory = { viewModel.rejectCategory() }
                    )
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
private fun ChatMessageItem(
    message: ChatMessage,
    onConfirmTransaction: (ProposedTransaction) -> Unit,
    onRejectTransaction: () -> Unit,
    onConfirmCategory: (ProposedCategory) -> Unit,
    onRejectCategory: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 0.dp,
                    bottomEnd = if (message.isUser) 0.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else if (message.isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (message.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    message.imageBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                    
                    if (message.isError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = message.text ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(text = message.text ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    message.proposedTransaction?.let { proposed ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Montant: ${proposed.amount}", style = MaterialTheme.typography.bodySmall)
                        Text("Categorie: ${proposed.categoryName}", style = MaterialTheme.typography.bodySmall)
                        Text("Note: ${proposed.note}", style = MaterialTheme.typography.bodySmall)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = onRejectTransaction) {
                                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { onConfirmTransaction(proposed) }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirmer", tint = Color(0xFF4CAF50))
                            }
                        }
                    }

                    message.proposedCategory?.let { proposed ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nom: ${proposed.name}", style = MaterialTheme.typography.bodySmall)
                        Text("Type: ${proposed.type}", style = MaterialTheme.typography.bodySmall)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = onRejectCategory) {
                                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { onConfirmCategory(proposed) }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirmer", tint = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
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
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                selectedImage = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            selectedImage = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
        }
    }
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = data?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                inputText = (inputText + " " + spokenText).trim()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        selectedImage?.let {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
                IconButton(
                    onClick = { selectedImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Supprimer l'image", modifier = Modifier.size(16.dp))
                }
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            var showAttachmentMenu by remember { mutableStateOf(false) }
            
            Box {
                IconButton(onClick = { showAttachmentMenu = true }, enabled = !isLoading) {
                    Icon(Icons.Default.Add, contentDescription = "Joindre")
                }
                
                DropdownMenu(
                    expanded = showAttachmentMenu,
                    onDismissRequest = { showAttachmentMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Caméra") },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
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
                        text = { Text("Photos") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = {
                            showAttachmentMenu = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Fichiers") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            showAttachmentMenu = false
                            galleryLauncher.launch("*/*") // Reuse the same launcher to decode stream
                        }
                    )
                }
            }
            
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tapez un message...") },
                enabled = !isLoading,
                maxLines = 3
            )
            
            IconButton(
                onClick = {
                    onSendMessage(inputText, selectedImage)
                    inputText = ""
                    selectedImage = null
                },
                enabled = !isLoading && (inputText.isNotBlank() || selectedImage != null)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
            }
        }
    }
}

