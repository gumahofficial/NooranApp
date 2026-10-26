package com.gumah.nooran.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gumah.nooran.model.Comment
import com.gumah.nooran.model.Post
import com.gumah.nooran.ui.theme.AmiriFontFamily
import com.gumah.nooran.ui.theme.SoftGold

@Composable
fun PostCard(
    post: Post,
    currentUserId: String?,
    userReaction: String?,
    isBookmarked: Boolean,
    commentsList: List<Comment> = emptyList(),
    onReactionSelected: (String) -> Unit,
    onBookmarkClick: () -> Unit,
    onRepostClick: () -> Unit,
    onQuoteClick: () -> Unit,
    onQuietReadingClick: () -> Unit,
    onShareImageClick: () -> Unit,
    onPollVote: (Int) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    var showReactionsRow by remember { mutableStateOf(false) }
    var showCommentsSection by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    val avatarBg = remember(post.authorAvatarColor) {
        try {
            Color(android.graphics.Color.parseColor(post.authorAvatarColor))
        } catch (_: Exception) {
            Color(0xFF14532D)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Author details & More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Actions (Quiet Reading & Share as Image & Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onQuietReadingClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = "قراءة هادئة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShareImageClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "مشاركة كصورة",
                            tint = SoftGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (post.userId == currentUserId) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "حذف المنشور",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Author Info (RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick() }
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (post.authorVerified) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "موثّق",
                                    tint = SoftGold,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "@${post.authorUsername}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text / Symbol Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorAvatarSymbol.ifBlank { "✨" },
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Thread badge if applicable
            if (post.postType == "thread" && post.threadTotal > 1) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End)
                ) {
                    Text(
                        text = "سلسلة ${post.threadPosition}/${post.threadTotal}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Content Text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Tags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    post.tags.forEach { tag ->
                        Text(
                            text = "#$tag ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Poll Section if type == "poll"
            if (post.postType == "poll" && post.pollOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    if (post.pollQuestion.isNotBlank()) {
                        Text(
                            text = post.pollQuestion,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    post.pollOptions.forEachIndexed { index, option ->
                        val votesForThis = post.pollVotes.getOrNull(index) ?: 0L
                        val percentage = if (post.pollTotalVotes > 0) {
                            ((votesForThis.toFloat() / post.pollTotalVotes.toFloat()) * 100).toInt()
                        } else 0

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onPollVote(index) }
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Text(
                        text = "إجمالي الأصوات: ${post.pollTotalVotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Impact Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Reactions Row Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val activeEmoji = when (userReaction) {
                        "love" -> "❤️"
                        "jazakallah" -> "🤲"
                        "barakallah" -> "🌿"
                        "touched" -> "💚"
                        "useful" -> "💡"
                        "amen" -> "🤍"
                        else -> "🤍"
                    }

                    FilledTonalButton(
                        onClick = { showReactionsRow = !showReactionsRow },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = activeEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showCommentsSection = !showCommentsSection },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "التعليقات",
                                modifier = Modifier.size(18.dp)
                            )
                            if (post.commentsCount > 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "${post.commentsCount}", fontSize = 12.sp)
                            }
                        }
                    }

                    IconButton(onClick = onRepostClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "إعادة نشر",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onQuoteClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = "اقتباس",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onBookmarkClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ",
                            tint = if (isBookmarked) SoftGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Right: Impact Score
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الأثر ${post.impactScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "الأثر",
                            tint = SoftGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Expanded Reactions Picker
            AnimatedVisibility(visible = showReactionsRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val reactions = listOf(
                        "love" to "❤️ أحببته",
                        "jazakallah" to "🤲 جزاك الله خيراً",
                        "barakallah" to "🌿 بارك الله فيك",
                        "touched" to "💚 أثر فيّ",
                        "useful" to "💡 مفيد",
                        "amen" to "🤍 آمين"
                    )

                    reactions.forEach { (type, label) ->
                        Text(
                            text = label.take(2),
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReactionSelected(type)
                                    showReactionsRow = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Expanded Comments Section
            AnimatedVisibility(visible = showCommentsSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Comment Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "إرسال تعليق",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("أضف تعليقاً طيباً...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )
                    }

                    // Comments List
                    if (commentsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        commentsList.forEach { comment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = comment.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(comment.authorAvatarSymbol.ifBlank { "✨" }, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
