package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.ChatMessage
import com.giantnovadevs.mysamoney.viewmodel.FinancialCoachViewModel
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCoachScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val viewModel: FinancialCoachViewModel = viewModel()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var userQuestion by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val showAdDialog by viewModel.showAdDialog.collectAsState()
    val messageCredits by viewModel.messageCredits.collectAsState()
    val context = LocalContext.current // We need this to get the Activity

    // Automatically scroll to the bottom when a new message appears
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Coach") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // The bottom bar is our text input field
        bottomBar = {
            ChatInputBar(
                text = userQuestion,
                onTextChange = { userQuestion = it },
                isLoading = isLoading,
                onSend = {
                    if (userQuestion.isNotBlank()) {
                        viewModel.askQuestion(userQuestion)
                        userQuestion = "" // Clear the text field
                        focusManager.clearFocus() // Hide keyboard
                    }
                },
                credits = messageCredits
            )
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

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

    }
    if (showAdDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdDialog() },
            title = { Text("You're out of free messages") },
            text = { Text("Watch a short ad to get 3 more message credits, or upgrade to Pro for unlimited access.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Find the activity and show the ad
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
                .fillMaxWidth(0.85f) // Max 85% of screen width
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * The text field and send button at the bottom
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit,
    credits: Int
) {
    Surface(
        tonalElevation = 4.dp // Add shadow to separate from content
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Show remaining credits
            Text(
                text = "Free messages remaining: $credits",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
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