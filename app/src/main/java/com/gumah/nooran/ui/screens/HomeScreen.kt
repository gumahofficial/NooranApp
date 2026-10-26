package com.gumah.nooran.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gumah.nooran.model.DailyContent
import com.gumah.nooran.model.Post
import com.gumah.nooran.model.Story
import com.gumah.nooran.model.UserProfile
import com.gumah.nooran.ui.components.*
import com.gumah.nooran.ui.theme.SoftGold
import com.gumah.nooran.viewmodel.FeedTab
import com.gumah.nooran.viewmodel.NooranViewModel

@Composable
fun HomeScreen(
    viewModel: NooranViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val currentUser by viewModel.currentUserProfile.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val activeTab by viewModel.activeFeedTab.collectAsState()
    val stories by viewModel.activeStories.collectAsState()
    val dailyContent by viewModel.dailyContentList.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val userBookmarks by viewModel.userBookmarks.collectAsState()

    val wordOfDay = remember(dailyContent) { dailyContent.find { it.type == "word_of_day" } }
    val dailyWird = remember(dailyContent) { dailyContent.find { it.type == "daily_wird" } }

    val filteredPosts = remember(allPosts, activeTab) {
        when (activeTab) {
            FeedTab.ForYou -> allPosts.sortedByDescending { it.impactScore + it.likesCount }
            FeedTab.Following -> allPosts // In real social, filter by following list
            FeedTab.Latest -> allPosts.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    Scaffold(
        topBar = {
            NooranAppBar(
                unreadNotificationsCount = 0,
                onSearchClick = onNavigateToSearch,
                onNotificationsClick = onNavigateToNotifications
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Stories Carousel
            item {
                StoriesBar(
                    stories = stories,
                    onAddStoryClick = { viewModel.showStoryEditorDialog.value = true },
                    onStoryClick = { story -> viewModel.activeStoryViewing.value = story }
                )
            }

            // 2. Daily Word Card
            if (wordOfDay != null) {
                item {
                    WordOfDayCard(
                        content = wordOfDay,
                        onShareAsImage = { /* handled */ }
                    )
                }
            }

            // 3. Daily Wird Card
            if (dailyWird != null) {
                item {
                    DailyWirdCard(
                        content = dailyWird,
                        streakDays = userSettings.dailyWirdStreak,
                        isCompletedToday = userSettings.completedWirdToday,
                        onMarkCompleted = { viewModel.completeDailyWird() }
                    )
                }
            }

            // 4. Feed Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf(
                        FeedTab.ForYou to "لك (المميز)",
                        FeedTab.Following to "المتابعون",
                        FeedTab.Latest to "الأحدث"
                    )

                    tabs.forEach { (tab, label) ->
                        val isSelected = activeTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFeedTab(tab) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // 5. Posts or Strict Empty State (No Mock Data)
            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✨", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "لا توجد منشورات بعد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "كن أول من ينير المساحة بكلمة طيبة، خاطرة، أو أثر نافع.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.showCreatePostDialog.value = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("أنشئ أول منشور")
                            }
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    val isBookmarked = userBookmarks.contains(post.id)
                    val userReaction = userReactions[post.id]

                    PostCard(
                        post = post,
                        currentUserId = currentUser?.uid,
                        userReaction = userReaction,
                        isBookmarked = isBookmarked,
                        onReactionSelected = { reactionType ->
                            viewModel.toggleReaction(post.id, reactionType)
                        },
                        onBookmarkClick = {
                            viewModel.toggleBookmark(post)
                        },
                        onRepostClick = {
                            viewModel.repostPost(post)
                        },
                        onQuoteClick = {
                            viewModel.quotePost(post, "مشاركة طيبة تستحق التأمل.")
                        },
                        onQuietReadingClick = {
                            viewModel.quietReadingPost.value = post
                        },
                        onShareImageClick = {
                            viewModel.shareAsImagePost.value = post
                        },
                        onPollVote = { optionIndex ->
                            viewModel.votePoll(post.id, optionIndex)
                        },
                        onAddComment = { text ->
                            viewModel.addComment(post.id, text)
                        },
                        onDeleteClick = {
                            viewModel.deletePost(post.id)
                        },
                        onAuthorClick = {
                            viewModel.inspectProfile(
                                UserProfile(
                                    uid = post.userId,
                                    displayName = post.authorName,
                                    username = post.authorUsername,
                                    avatarSymbol = post.authorAvatarSymbol,
                                    avatarColor = post.authorAvatarColor,
                                    isVerified = post.authorVerified
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StoriesBar(
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Add Story" Button
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAddStoryClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "أضف قصة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "قصتك",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Active 24h Text Stories
        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, SoftGold, CircleShape)
                        .background(Color(0xFF0B3B24)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.authorAvatarSymbol.ifBlank { "✨" },
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorName.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}
