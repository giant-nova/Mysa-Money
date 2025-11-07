package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.ChatMessage
import com.giantnovadevs.mysamoney.viewmodel.FinancialCoachViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCoachScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel,
    viewModel: FinancialCoachViewModel
) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var userQuestion by remember { mutableStateOf("") }

    val isPro by proViewModel.isProUser.collectAsState()
    val showAdDialog by viewModel.showAdDialog.collectAsState()
    val messageCredits by viewModel.messageCredits.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isPro) {
        viewModel.setUserProStatus(isPro)
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Financial Coach")
                        // --- ✅ FIX 2: Add "Pro" Badge ---
                        if (isPro) {
                            Badge(
                                modifier = Modifier.padding(start = 8.dp),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text("PRO", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.listAvailableModels() }) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "List Models",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // --- ✅ FIX 3: New Bottom Bar layout ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Show "thinking..." text when loading
                AnimatedVisibility(visible = isLoading) {
                    Text(
                        text = "Mysa Money Coach is thinking...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                    )
                }

                // The Chat Input Box
                ChatInputBar(
                    text = userQuestion,
                    onTextChange = { userQuestion = it },
                    isLoading = isLoading,
                    onSend = {
                        if (userQuestion.isNotBlank()) {
                            viewModel.askQuestion(userQuestion)
                            userQuestion = ""
                            focusManager.clearFocus()
                        }
                    },
                    credits = messageCredits,
                    isPro = isPro // Pass isPro state
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(chatHistory) { message ->
                MessageBubble(message = message)
            }

            // --- ✅ FIX 3: Removed the loading spinner item from here ---
        }
    }

    // "Watch Ad" Dialog
    if (showAdDialog && !isPro) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdDialog() },
            title = { Text("You're out of free messages") },
            text = { Text("Watch a short ad to get 3 more message credits, or upgrade to Pro for unlimited access.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.showRewardAd(activity)
                        }
                    }
                ) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAdDialog() }) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

/**
 * A single chat bubble
 */
@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalAlignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = horizontalAlignment
    ) {
        Text(
            text = message.message,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * The text field and send button (MODIFIED)
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit,
    credits: Int,
    isPro: Boolean // ✅ Added isPro parameter
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            // --- ✅ FIX 4: Hide credit text if user is Pro ---
            if (!isPro) {
                Text(
                    text = "Free messages remaining: $credits",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask a question...") },
                trailingIcon = {
                    IconButton(onClick = onSend, enabled = !isLoading && text.isNotBlank()) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
        }
    }
}