package com.gumah.nooran.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val bio: String = "",
    val avatarSymbol: String = "✨",
    val avatarColor: String = "#14532D",
    val isVerified: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val impactScore: Long = 0L,
    val userInterests: List<String> = emptyList(),
    val accountStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastLoginAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarSymbol: String = "✨",
    val authorAvatarColor: String = "#14532D",
    val authorVerified: Boolean = false,
    val content: String = "",
    val postType: String = "standard", // "standard", "thread", "poll"
    val threadId: String = "",
    val threadPosition: Int = 1,
    val threadTotal: Int = 1,
    val pollQuestion: String = "",
    val pollOptions: List<String> = emptyList(),
    val pollVotes: List<Long> = emptyList(),
    val pollTotalVotes: Long = 0L,
    val impactScore: Long = 0L,
    val likesCount: Long = 0L,
    val repostsCount: Long = 0L,
    val quotesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val bookmarksCount: Long = 0L,
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarSymbol: String = "✨",
    val authorAvatarColor: String = "#14532D",
    val authorVerified: Boolean = false,
    val content: String = "",
    val parentCommentId: String? = null,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reaction(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val type: String = "love", // love, jazakallah, barakallah, touched, useful, amen
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarSymbol: String = "✨",
    val authorAvatarColor: String = "#14532D",
    val content: String = "",
    val templateType: String = "خاطرة",
    val bgGradient: String = "emerald", // emerald, night, ivory, gold
    val textColor: String = "#FFFFFF",
    val textSize: Int = 20,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Bookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val postContent: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val category: String = "أخرى", // دعاء, ذكر, علم, خواطر, أخرى
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PollVote(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val optionIndex: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorUsername: String = "",
    val type: String = "like", // like, comment, follow, repost, quote, mention
    val targetId: String = "",
    val previewText: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Draft(
    @DocumentId val id: String = "",
    val userId: String = "",
    val content: String = "",
    val draftType: String = "standard",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyContent(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "word_of_day", // word_of_day, daily_wird, dhikr, wisdom
    val source: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val userId: String = "",
    val isPrivate: Boolean = false,
    val mutedWords: List<String> = emptyList(),
    val allowRepliesFrom: String = "everyone", // everyone, following
    val allowMentionsFrom: String = "everyone",
    val showActivityStatus: Boolean = true,
    val dailyWirdStreak: Long = 0L,
    val completedWirdToday: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
