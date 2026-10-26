package com.gumah.nooran.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gumah.nooran.model.Story
import com.gumah.nooran.ui.theme.AmiriFontFamily
import com.gumah.nooran.ui.theme.SoftGold
import kotlinx.coroutines.launch

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialStory: Story,
    currentUserId: String?,
    onDeleteStory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember {
        mutableStateOf(stories.indexOfFirst { it.id == initialStory.id }.coerceAtLeast(0))
    }
    val currentStory = stories.getOrNull(currentIndex) ?: run {
        onDismiss()
        return
    }

    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-advance progress
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value) * 6000L
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = remaining.toInt().coerceAtLeast(100), easing = LinearEasing)
            )
            // Move to next
            if (currentIndex < stories.size - 1) {
                currentIndex++
                progress.snapTo(0f)
            } else {
                onDismiss()
            }
        }
    }

    val bgBrush = remember(currentStory.bgGradient) {
        when (currentStory.bgGradient) {
            "emerald" -> Brush.verticalGradient(listOf(Color(0xFF0B3B24), Color(0xFF04190F)))
            "night" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
            "gold" -> Brush.verticalGradient(listOf(Color(0xFF382D16), Color(0xFF140F05)))
            else -> Brush.verticalGradient(listOf(Color(0xFF14532D), Color(0xFF052E16)))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 3) {
                                // Tap Left (Previous)
                                if (currentIndex > 0) {
                                    currentIndex--
                                    scope.launch { progress.snapTo(0f) }
                                }
                            } else {
                                // Tap Right (Next)
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                    scope.launch { progress.snapTo(0f) }
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Content text centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = currentStory.templateType,
                        color = SoftGold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = currentStory.content,
                    fontFamily = AmiriFontFamily,
                    fontSize = currentStory.textSize.sp,
                    lineHeight = (currentStory.textSize * 1.6f).sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }

            // Top Progress Bars & Header (Status, Author, Close)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Progress Bars Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val segmentProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14532D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentStory.authorAvatarSymbol.ifBlank { "✨" }, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentStory.authorName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentStory.authorUsername}",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row {
                        if (currentStory.userId == currentUserId) {
                            IconButton(onClick = { onDeleteStory(currentStory.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف القصة",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
