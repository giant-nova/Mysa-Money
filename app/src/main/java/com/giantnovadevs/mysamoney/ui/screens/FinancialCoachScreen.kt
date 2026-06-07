package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val ChatUserBubble = Color(0xFF007AFF) // Classic Blue
private val ChatCoachBubble = Color(0xFFE9E9EB) // Light Grey
private val TextUser = Color.White
private val TextCoach = Color.Black

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

    val isPro by proViewModel.isProUser.collectAsState()
    val showAdDialog by viewModel.showAdDialog.collectAsState()
    val messageCredits by viewModel.messageCredits.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Scroll to bottom when new message arrives
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty() || isLoading) {
            listState.animateScrollToItem((chatHistory.size + (if(isLoading) 1 else 0)) - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Financial Coach",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (isPro) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) { Text("PRO", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                onSend = { text ->
                    viewModel.askQuestion(text)
                    focusManager.clearFocus()
                },
                isLoading = isLoading,
                credits = messageCredits,
                isPro = isPro
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Message
            if (chatHistory.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Ask me anything about your finances!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(chatHistory) { index, message ->
                MessageBubble(message = message)
                if (!isPro && (index + 1) % 5 == 0) {
                    AdMobBanner(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            }

            // Typing Indicator
            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }
    }

    // Ad Dialog (Keep existing logic)
    if (showAdDialog && !isPro) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdDialog() },
            title = { Text("Out of free messages") },
            text = { Text("Watch a short ad to get 3 more credits, or upgrade to Pro for unlimited chats.") },
            confirmButton = {
                Button(onClick = { (context as? Activity)?.let { viewModel.showRewardAd(it) } }) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAdDialog() }) { Text("Later") }
            }
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser

    // ✅ FIX: Use Alignment.End and Alignment.Start
    val alignment = if (isUser) Alignment.End else Alignment.Start

    // Define colors based on sender
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else ChatCoachBubble
    val textColor = if (isUser) Color.White else TextCoach

    // Bubble shape logic (tail position)
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) // User: Bottom-Right sharp
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp) // Coach: Bottom-Left sharp
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment // Now this matches the required type
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                // Ensure text wraps correctly
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .background(ChatCoachBubble)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "Thinking",
            style = MaterialTheme.typography.bodyMedium,
            color = TextCoach.copy(alpha = 0.7f)
        )
        Text(
            "...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextCoach.copy(alpha = alpha),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChatInputBar(
    onSend: (String) -> Unit,
    isLoading: Boolean,
    credits: Int,
    isPro: Boolean
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(Color.White) // Input area background
            .padding(16.dp)
    ) {
        // Credits Indicator
        if (!isPro) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$credits free messages remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Modern Pill Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(AppBackground)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        "Ask your financial coach...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank() && !isLoading) {
                                onSend(text)
                                text = ""
                            }
                        }
                    ),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = !isLoading && text.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}