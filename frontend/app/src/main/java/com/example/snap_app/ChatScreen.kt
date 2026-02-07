package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data Classes
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var modelLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize GenieService
    val genieService = remember { GenieService(context) }

    // Template responses
    val templates = listOf(
        "Tell me about my workout plan",
        "What should I eat today?",
        "How many calories should I consume?",
        "Give me fitness tips",
        "Track my progress"
    )

    // Initialize model on first composition
    LaunchedEffect(Unit) {
        // Show initial greeting
        messages = listOf(
            ChatMessage(
                content = "Hi! 👋 I'm your AI fitness assistant. I'm here to help you achieve your fitness goals! How can I assist you today?",
                isUser = false
            ),
            ChatMessage(
                content = "Loading AI model... This may take a moment on first launch.",
                isUser = false
            )
        )
        
        // Initialize GenieService in background coroutine
        coroutineScope.launch {
            try {
                val initialized = genieService.initialize()
                if (initialized) {
                    modelLoaded = true
                    // Remove loading message and show ready message
                    messages = messages.dropLast(1) + listOf(
                        ChatMessage(
                            content = "",
                            isUser = false
                        )
                    )
                } else {
                    // Model initialization failed - show error with setup instructions
                    messages = messages.dropLast(1) + listOf(
                        ChatMessage(
                            content = """
                                ❌ Model files not found!
                                
                                📱 To load the model on your phone:
                                1. Connect phone via USB
                                2. Enable USB debugging
                                3. Run: .\push_model_to_device.ps1
                                4. Wait for files to push
                                5. Restart this app
                                
                                See DEVICE_SETUP.md for detailed instructions
                            """.trimIndent(),
                            isUser = false
                        )
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Error loading model: ${e.message}"
                messages = messages.dropLast(1) + listOf(
                    ChatMessage(
                        content = "❌ Error loading AI model. Check logs for details: ${e.message}",
                        isUser = false
                    )
                )
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                text = "AI Assistant 🤖",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonPink,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal fitness companion",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }

                // Typing indicator
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Template responses (quick replies) - only show if model is loaded and we're at the start
            if (messages.size <= 2 && modelLoaded) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(templates) { template ->
                        TemplateChip(
                            text = template,
                            onClick = {
                                if (messageText.isEmpty()) {
                                    messageText = template
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            color = Color.Gray
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E3050),
                        unfocusedContainerColor = Color(0xFF1E3050),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonPink,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank() && modelLoaded) {
                            // Add user message
                            val userMessage = ChatMessage(
                                content = messageText,
                                isUser = true
                            )
                            messages = messages + userMessage

                            val userInput = messageText
                            messageText = ""

                            // Show typing indicator
                            isTyping = true

                            // Get AI response using GenieService
                            coroutineScope.launch {
                                try {
                                    // Call LLM for response
                                    val aiResponse = genieService.generateResponse(userInput)
                                    
                                    isTyping = false

                                    val aiMessage = ChatMessage(
                                        content = aiResponse,
                                        isUser = false
                                    )
                                    messages = messages + aiMessage
                                } catch (e: Exception) {
                                    isTyping = false
                                    errorMessage = "Error generating response: ${e.message}"
                                    
                                    val errorResponse = ChatMessage(
                                        content = "Sorry, I encountered an error while processing your request: ${e.message}",
                                        isUser = false
                                    )
                                    messages = messages + errorResponse
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (messageText.isNotBlank() && modelLoaded) NeonPink else Color.Gray,
                            shape = CircleShape
                        ),
                    enabled = messageText.isNotBlank() && modelLoaded
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isUser) NeonPink else Color(0xFF1E3050)
    val textColor = Color.White

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.isUser) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    MarkdownText(
                        markdown = message.content,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeString,
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun TemplateChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF1E3050),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = NeonPink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .widthIn(max = 80.dp)
            .background(
                color = Color(0xFF1E3050),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by animateFloatAsState(
                targetValue = if ((System.currentTimeMillis() / 400) % 3 == index.toLong()) 1f else 0.3f,
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = Color.White.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun animateFloatAsState(targetValue: Float, label: String): State<Float> {
    return remember { mutableStateOf(targetValue) }
}

@Composable
fun MarkdownText(
    markdown: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    val lines = markdown.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (line in lines) {
            when {
                // Headers
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ").trim(), color),
                        fontSize = fontSize * 1.1f,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        color = color
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ").trim(), color),
                        fontSize = fontSize * 1.2f,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        color = color
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ").trim(), color),
                        fontSize = fontSize * 1.35f,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        color = color
                    )
                }
                // Bullet list items
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val indent = line.length - line.trimStart().length
                    val content = line.trimStart().drop(2)
                    Row(modifier = Modifier.padding(start = (indent * 4).dp)) {
                        Text("•  ", color = NeonPink, fontSize = fontSize, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseInlineMarkdown(content, color),
                            fontSize = fontSize,
                            lineHeight = 20.sp,
                            color = color
                        )
                    }
                }
                // Numbered list items
                line.trimStart().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val indent = line.length - line.trimStart().length
                    val numberEnd = line.trimStart().indexOf(". ")
                    val number = line.trimStart().substring(0, numberEnd + 1)
                    val content = line.trimStart().substring(numberEnd + 2)
                    Row(modifier = Modifier.padding(start = (indent * 4).dp)) {
                        Text("$number ", color = NeonPink, fontSize = fontSize, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseInlineMarkdown(content, color),
                            fontSize = fontSize,
                            lineHeight = 20.sp,
                            color = color
                        )
                    }
                }
                // Code block lines (```...```)
                line.trimStart().startsWith("```") -> {
                    // skip fence markers
                }
                // Empty line -> small spacer
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Regular paragraph
                else -> {
                    Text(
                        text = parseInlineMarkdown(line, color),
                        fontSize = fontSize,
                        lineHeight = 20.sp,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`
 */
fun parseInlineMarkdown(text: String, color: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            when {
                // Bold: **text**
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code: `text`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.White.copy(alpha = 0.1f),
                            color = NeonPink
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}