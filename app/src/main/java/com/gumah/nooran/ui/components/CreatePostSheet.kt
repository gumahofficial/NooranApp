package com.gumah.nooran.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gumah.nooran.ui.theme.SoftGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSheet(
    aiLoading: Boolean,
    aiSafetyMsg: String?,
    aiSuggestion: String?,
    onCheckSafety: (String) -> Unit,
    onEnhance: (text: String, action: String) -> Unit,
    onPublishPost: (content: String, tags: List<String>) -> Unit,
    onPublishThread: (tweets: List<String>) -> Unit,
    onPublishPoll: (question: String, options: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf("post") } // post, thread, poll

    // Standard Post state
    var postContent by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }

    // Thread state
    var threadItems by remember { mutableStateOf(listOf("", "")) }

    // Poll state
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("create_post_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type Selector Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("post" to "منشور", "thread" to "سلسلة", "poll" to "استطلاع").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("chip_$type")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                "post" -> {
                    // Standard Post Editor
                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { if (it.length <= 1500) postContent = it },
                        placeholder = { Text("اكتب كلماتٍ تنير القلب... (خاطرة، فائدة، ذكر، تدبر)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                            .testTag("post_content_input"),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 10
                    )

                    // Character counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${postContent.length}/1500",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tags Input
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("وسوم (مفصولة بمسافة مثلاً: تدبر دعاء أثر)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                "thread" -> {
                    // Thread Items
                    Text(
                        text = "إنشاء سلسلة متتالية من المنشورات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    threadItems.forEachIndexed { index, text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}/${threadItems.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, end = 8.dp)
                            )
                            OutlinedTextField(
                                value = text,
                                onValueChange = { newTxt ->
                                    val updated = threadItems.toMutableList()
                                    updated[index] = newTxt
                                    threadItems = updated
                                },
                                placeholder = { Text("المنشور رقم ${index + 1}...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )
                        }
                    }

                    if (threadItems.size < 6) {
                        TextButton(onClick = { threadItems = threadItems + "" }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة جزء للسلسلة")
                        }
                    }
                }

                "poll" -> {
                    // Poll Editor
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        placeholder = { Text("سؤال الاستطلاع (مثلاً: ما الذي تفضل قراءته صباحاً؟)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pollOption1,
                        onValueChange = { pollOption1 = it },
                        placeholder = { Text("الخيار الأول") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pollOption2,
                        onValueChange = { pollOption2 = it },
                        placeholder = { Text("الخيار الثاني") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pollOption3,
                        onValueChange = { pollOption3 = it },
                        placeholder = { Text("الخيار الثالث (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Assistant & Safety Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مساعد نُوران الذكي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val textToCheck = when (selectedType) {
                                    "thread" -> threadItems.joinToString("\n")
                                    "poll" -> pollQuestion
                                    else -> postContent
                                }
                                onCheckSafety(textToCheck)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فحص السكينة والأدب", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (postContent.isNotBlank()) onEnhance(postContent, "polish")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            enabled = postContent.isNotBlank()
                        ) {
                            Text("تحسين الصياغة", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (postContent.isNotBlank()) onEnhance(postContent, "hashtags")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            enabled = postContent.isNotBlank()
                        ) {
                            Text("اقتراح وسوم", fontSize = 11.sp)
                        }
                    }

                    // Display Safety Message
                    AnimatedVisibility(visible = !aiSafetyMsg.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = aiSafetyMsg ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Display AI Suggestion with Replace button
                    AnimatedVisibility(visible = !aiSuggestion.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "اقتراح المساعد:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = aiSuggestion ?: "", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    postContent = aiSuggestion ?: ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("استخدام هذا النص", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        when (selectedType) {
                            "post" -> {
                                if (postContent.isNotBlank()) {
                                    val tags = tagInput.split(" ", "#", "،")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    onPublishPost(postContent, tags)
                                }
                            }
                            "thread" -> {
                                val validTweets = threadItems.filter { it.isNotBlank() }
                                if (validTweets.isNotEmpty()) {
                                    onPublishThread(validTweets)
                                }
                            }
                            "poll" -> {
                                val options = listOf(pollOption1, pollOption2, pollOption3).filter { it.isNotBlank() }
                                if (pollQuestion.isNotBlank() && options.size >= 2) {
                                    onPublishPoll(pollQuestion, options)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(2f)
                        .testTag("publish_post_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("نشر في نُوران", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
