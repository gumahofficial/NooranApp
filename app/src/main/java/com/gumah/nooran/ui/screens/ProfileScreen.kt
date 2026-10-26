package com.gumah.nooran.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gumah.nooran.model.Post
import com.gumah.nooran.model.UserProfile
import com.gumah.nooran.ui.components.PostCard
import com.gumah.nooran.ui.theme.SoftGold
import com.gumah.nooran.viewmodel.NooranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NooranViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUserProfile.collectAsState()
    val viewingProfile by viewModel.viewingProfile.collectAsState()
    val isFollowingViewing by viewModel.isFollowingViewingProfile.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    val profile = viewingProfile ?: currentUser ?: UserProfile()
    val isMyProfile = viewingProfile == null || viewingProfile?.uid == currentUser?.uid

    val userPosts = remember(allPosts, profile.uid) {
        allPosts.filter { it.userId == profile.uid }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("المنشورات", "السلاسل", "المحفوظات", "الأثر")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isMyProfile) "الملف الشخصي" else profile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isMyProfile) {
                        IconButton(onClick = { viewModel.inspectProfile(currentUser ?: UserProfile()) }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "الإعدادات")
                        }
                    } else {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "تابع ${profile.displayName} (@${profile.username}) على منصة نُوران الاجتماعية الهادئة")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الشخصي"))
                        }) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "مشاركة")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header: Symbol Avatar + Name + Verification + Username + Bio
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarColor = remember(profile.avatarColor) {
                        try {
                            Color(android.graphics.Color.parseColor(profile.avatarColor))
                        } catch (_: Exception) {
                            Color(0xFF14532D)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                            .border(3.dp, SoftGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.avatarSymbol.ifBlank { "✨" },
                            fontSize = 38.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profile.isVerified) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "موثق",
                                tint = SoftGold,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = profile.displayName.ifBlank { "مستخدم نُوران" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "@${profile.username.ifBlank { "nooran_user" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (profile.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats Row (Impact Score, Posts, Followers, Following)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(title = "الأثر المبارك", value = "${profile.impactScore} ⭐")
                        ProfileStatItem(title = "المنشورات", value = "${userPosts.size}")
                        ProfileStatItem(title = "المتابعون", value = "${profile.followersCount}")
                        ProfileStatItem(title = "يتابع", value = "${profile.followingCount}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Edit Profile OR Follow/Unfollow
                    if (isMyProfile) {
                        OutlinedButton(
                            onClick = { viewModel.showEditProfileDialog.value = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تعديل الهوية والرمز الشخصي")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.toggleFollowViewingProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingViewing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isFollowingViewing) "يتم المتابعة ✓" else "+ متابعة")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Tabs Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            label = {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> { // المنشورات
                    if (userPosts.isEmpty()) {
                        item {
                            EmptyProfileSection(message = "لا توجد منشورات لهذا الحساب حتى الآن.")
                        }
                    } else {
                        items(userPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = currentUser?.uid,
                                userReaction = null,
                                isBookmarked = false,
                                onReactionSelected = { viewModel.toggleReaction(post.id, it) },
                                onBookmarkClick = { viewModel.toggleBookmark(post) },
                                onRepostClick = { viewModel.repostPost(post) },
                                onQuoteClick = { viewModel.quotePost(post, "مشاركة طيبة.") },
                                onQuietReadingClick = { viewModel.quietReadingPost.value = post },
                                onShareImageClick = { viewModel.shareAsImagePost.value = post },
                                onPollVote = { viewModel.votePoll(post.id, it) },
                                onAddComment = { viewModel.addComment(post.id, it) },
                                onDeleteClick = { viewModel.deletePost(post.id) },
                                onAuthorClick = {}
                            )
                        }
                    }
                }
                1 -> { // السلاسل Threads
                    val threads = userPosts.filter { it.postType == "thread" }
                    if (threads.isEmpty()) {
                        item {
                            EmptyProfileSection(message = "لم يتم إنشاء سلاسل (Threads) بعد.")
                        }
                    } else {
                        items(threads, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = currentUser?.uid,
                                userReaction = null,
                                isBookmarked = false,
                                onReactionSelected = { viewModel.toggleReaction(post.id, it) },
                                onBookmarkClick = { viewModel.toggleBookmark(post) },
                                onRepostClick = { viewModel.repostPost(post) },
                                onQuoteClick = { viewModel.quotePost(post, "اقتباس.") },
                                onQuietReadingClick = { viewModel.quietReadingPost.value = post },
                                onShareImageClick = { viewModel.shareAsImagePost.value = post },
                                onPollVote = { viewModel.votePoll(post.id, it) },
                                onAddComment = { viewModel.addComment(post.id, it) },
                                onDeleteClick = { viewModel.deletePost(post.id) },
                                onAuthorClick = {}
                            )
                        }
                    }
                }
                2 -> { // المحفوظات Bookmarks
                    if (bookmarks.isEmpty()) {
                        item {
                            EmptyProfileSection(message = "قائمة المحفوظات فارغة • يمكنك حفظ أي منشور للرجوع إليه لاحقاً.")
                        }
                    } else {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = bookmark.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "— ${bookmark.authorName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = bookmark.postContent, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                3 -> { // نظام الأثر Impact Breakdown
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "نظام الأثر في نُوران",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يقاس الأثر المبارك بالبركة والنفع وليس مجرد الإعجاب:\n• تفاعل طيب: +٢ أثر\n• إعادة نشر الكلمة: +٣ أثر\n• اقتباس ومشاركة: +٣ أثر\n• حفظ للمراجعة: +٢ أثر\n• تعليق هادف: +١ أثر",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyProfileSection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
