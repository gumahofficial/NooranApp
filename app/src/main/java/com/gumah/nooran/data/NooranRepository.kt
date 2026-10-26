package com.gumah.nooran.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.gumah.nooran.R
import com.gumah.nooran.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class NooranRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )
    private val auth = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun requireUserId(): String {
        return currentUserId ?: throw IllegalStateException("يجب تسجيل الدخول أولاً")
    }

    // ----------------------------------------------------
    // USER PROFILES
    // ----------------------------------------------------
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            null
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        return db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, "users/$uid")
                emit(null)
            }
    }

    suspend fun createOrUpdateUser(user: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val exists = docRef.get().await().exists()
            val data = hashMapOf<String, Any>(
                "uid" to uid,
                "displayName" to user.displayName.ifBlank { "مستخدم نُوران" },
                "username" to user.username.ifBlank { "user_${uid.take(6)}" },
                "bio" to user.bio,
                "avatarSymbol" to user.avatarSymbol,
                "avatarColor" to user.avatarColor,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                data["isVerified"] = false
                data["followersCount"] = 0L
                data["followingCount"] = 0L
                data["postsCount"] = 0L
                data["impactScore"] = 0L
                data["createdAt"] = FieldValue.serverTimestamp()
            }
            if (user.usernameLowercase.isNotBlank()) {
                data["usernameLowercase"] = user.usernameLowercase
            }
            if (user.email.isNotBlank()) {
                data["email"] = user.email
            }
            if (user.userInterests.isNotEmpty()) {
                data["userInterests"] = user.userInterests
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // USERNAME RESERVATION & VERIFICATION
    // ----------------------------------------------------
    suspend fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim().lowercase()
        if (clean.isBlank()) return false
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            !doc.exists()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$clean")
            false
        }
    }

    suspend fun claimUsername(uid: String, username: String): Boolean {
        val clean = username.trim().lowercase()
        return try {
            val docRef = db.collection("usernames").document(clean)
            val exists = docRef.get().await().exists()
            if (exists) return false
            docRef.set(hashMapOf(
                "uid" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "usernames/$clean")
            false
        }
    }

    // ----------------------------------------------------
    // GOOGLE PROFILE INITIALIZATION & ACCOUNT LINKING
    // ----------------------------------------------------
    suspend fun ensureGoogleUserProfile(user: com.google.firebase.auth.FirebaseUser): UserProfile {
        val uid = user.uid
        val existing = getUserProfile(uid)
        if (existing != null) {
            // Update lastLoginAt
            try {
                db.collection("users").document(uid).update("lastLoginAt", FieldValue.serverTimestamp()).await()
            } catch (_: Exception) {}
            return existing
        }

        // Generate clean username from email or display name
        val emailPrefix = user.email?.substringBefore("@")?.filter { it.isLetterOrDigit() || it == '_' } ?: "user"
        var candidate = emailPrefix.take(15).lowercase()
        if (candidate.isBlank() || !candidate.first().isLetter()) {
            candidate = "noor_${uid.take(4).lowercase()}"
        }

        // Ensure uniqueness
        if (!isUsernameAvailable(candidate)) {
            val randomSuffix = (100..999).random()
            candidate = "${candidate.take(12)}_$randomSuffix"
        }

        claimUsername(uid, candidate)

        val newProfile = UserProfile(
            uid = uid,
            displayName = user.displayName ?: "مستخدم نُوران",
            username = candidate,
            usernameLowercase = candidate.lowercase(),
            email = user.email ?: "",
            emailVerified = user.isEmailVerified,
            avatarSymbol = "✨",
            avatarColor = "#14532D"
        )
        createOrUpdateUser(newProfile)
        return newProfile
    }

    // ----------------------------------------------------
    // PASSWORD RESET & VERIFICATION
    // ----------------------------------------------------
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("لا يوجد مستخدم مسجل")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadAndCheckEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload().await()
            val isVerified = user.isEmailVerified
            if (isVerified) {
                db.collection("users").document(user.uid).update("emailVerified", true).await()
            }
            isVerified
        } catch (_: Exception) {
            false
        }
    }

    // ----------------------------------------------------
    // ONBOARDING & INTERESTS
    // ----------------------------------------------------
    suspend fun saveUserInterests(interests: List<String>) {
        val uid = requireUserId()
        try {
            db.collection("users").document(uid).update("userInterests", interests).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        }
    }

    // ----------------------------------------------------
    // DELETE ACCOUNT
    // ----------------------------------------------------
    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("لا يوجد مستخدم"))
        val uid = user.uid
        return try {
            val profile = getUserProfile(uid)
            if (profile != null && profile.usernameLowercase.isNotBlank()) {
                db.collection("usernames").document(profile.usernameLowercase).delete().await()
            }
            db.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users")
                .limit(20)
                .get()
                .await()
            val cleanQuery = query.trim().lowercase()
            snapshot.toObjects(UserProfile::class.java).filter {
                it.displayName.lowercase().contains(cleanQuery) ||
                it.username.lowercase().contains(cleanQuery)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            emptyList()
        }
    }

    // ----------------------------------------------------
    // POSTS & FEEDS
    // ----------------------------------------------------
    fun observePosts(): Flow<List<Post>> {
        return db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects(Post::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "posts")
                emit(emptyList())
            }
    }

    fun observeUserPosts(uid: String): Flow<List<Post>> {
        return db.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects(Post::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "posts_user")
                emit(emptyList())
            }
    }

    suspend fun createPost(post: Post): String {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val initialVotes = if (post.postType == "poll") {
            List(post.pollOptions.size) { 0L }
        } else {
            emptyList()
        }

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "authorName" to post.authorName,
            "authorUsername" to post.authorUsername,
            "authorAvatarSymbol" to post.authorAvatarSymbol,
            "authorAvatarColor" to post.authorAvatarColor,
            "authorVerified" to post.authorVerified,
            "content" to post.content,
            "postType" to post.postType,
            "threadId" to (post.threadId.ifBlank { docRef.id }),
            "threadPosition" to post.threadPosition,
            "threadTotal" to post.threadTotal,
            "pollQuestion" to post.pollQuestion,
            "pollOptions" to post.pollOptions,
            "pollVotes" to initialVotes,
            "pollTotalVotes" to 0L,
            "impactScore" to 0L,
            "likesCount" to 0L,
            "repostsCount" to 0L,
            "quotesCount" to 0L,
            "commentsCount" to 0L,
            "bookmarksCount" to 0L,
            "tags" to post.tags,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            // Increment user's post count
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(1)).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts")
            throw e
        }
    }

    suspend fun deletePost(postId: String) {
        val uid = requireUserId()
        try {
            db.collection("posts").document(postId).delete().await()
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(-1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            throw e
        }
    }

    // ----------------------------------------------------
    // POLL VOTING
    // ----------------------------------------------------
    suspend fun votePoll(postId: String, optionIndex: Int) {
        val uid = requireUserId()
        val voteDocId = "${postId}_$uid"
        val voteRef = db.collection("pollVotes").document(voteDocId)

        try {
            val existing = voteRef.get().await()
            if (existing.exists()) return // Already voted

            db.runTransaction { tx ->
                val postRef = db.collection("posts").document(postId)
                val postSnap = tx.get(postRef)
                val rawVotes = postSnap.get("pollVotes") as? List<*> ?: emptyList<Any>()
                val votesList = rawVotes.map { (it as? Number)?.toLong() ?: 0L }.toMutableList()

                if (optionIndex in votesList.indices) {
                    votesList[optionIndex] = votesList[optionIndex] + 1
                    tx.update(postRef, "pollVotes", votesList)
                    tx.update(postRef, "pollTotalVotes", FieldValue.increment(1))
                    tx.update(postRef, "impactScore", FieldValue.increment(2))
                }

                tx.set(voteRef, hashMapOf(
                    "postId" to postId,
                    "userId" to uid,
                    "optionIndex" to optionIndex,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }.await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId/poll")
            throw e
        }
    }

    suspend fun getUserPollVote(postId: String): Int? {
        val uid = currentUserId ?: return null
        return try {
            val doc = db.collection("pollVotes").document("${postId}_$uid").get().await()
            if (doc.exists()) (doc.getLong("optionIndex"))?.toInt() else null
        } catch (e: Exception) {
            null
        }
    }

    // ----------------------------------------------------
    // REACTIONS & IMPACT SYSTEM
    // ----------------------------------------------------
    suspend fun toggleReaction(postId: String, reactionType: String): Boolean {
        val uid = requireUserId()
        val reactionDocId = "${postId}_$uid"
        val reactionRef = db.collection("reactions").document(reactionDocId)
        val postRef = db.collection("posts").document(postId)

        return try {
            val existing = reactionRef.get().await()
            if (existing.exists()) {
                val currentType = existing.getString("type")
                if (currentType == reactionType) {
                    // Remove reaction
                    reactionRef.delete().await()
                    postRef.update(
                        "likesCount", FieldValue.increment(-1),
                        "impactScore", FieldValue.increment(-2)
                    ).await()
                    false
                } else {
                    // Change reaction type
                    reactionRef.update("type", reactionType).await()
                    true
                }
            } else {
                // Add new reaction
                reactionRef.set(hashMapOf(
                    "postId" to postId,
                    "userId" to uid,
                    "type" to reactionType,
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()
                postRef.update(
                    "likesCount", FieldValue.increment(1),
                    "impactScore", FieldValue.increment(2)
                ).await()

                // Trigger notification to author
                notifyAuthor(postRef, "reaction", "تفاعل مع منشورك المبارك")
                true
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reactions/$reactionDocId")
            false
        }
    }

    suspend fun getUserReaction(postId: String): String? {
        val uid = currentUserId ?: return null
        return try {
            val doc = db.collection("reactions").document("${postId}_$uid").get().await()
            if (doc.exists()) doc.getString("type") else null
        } catch (e: Exception) {
            null
        }
    }

    // ----------------------------------------------------
    // REPOST & QUOTE
    // ----------------------------------------------------
    suspend fun repostPost(originalPost: Post) {
        val uid = requireUserId()
        val userProfile = getUserProfile(uid) ?: return
        val newPost = Post(
            userId = uid,
            authorName = userProfile.displayName,
            authorUsername = userProfile.username,
            authorAvatarSymbol = userProfile.avatarSymbol,
            authorAvatarColor = userProfile.avatarColor,
            authorVerified = userProfile.isVerified,
            content = "🔄 إعادة نشر:\n«${originalPost.content}»\n— ${originalPost.authorName}",
            postType = "standard"
        )
        createPost(newPost)
        db.collection("posts").document(originalPost.id).update(
            "repostsCount", FieldValue.increment(1),
            "impactScore", FieldValue.increment(3)
        ).await()
    }

    suspend fun quotePost(originalPost: Post, myComment: String) {
        val uid = requireUserId()
        val userProfile = getUserProfile(uid) ?: return
        val combinedContent = "$myComment\n\n📌 اقتباس من ${originalPost.authorName}:\n«${originalPost.content}»"
        val newPost = Post(
            userId = uid,
            authorName = userProfile.displayName,
            authorUsername = userProfile.username,
            authorAvatarSymbol = userProfile.avatarSymbol,
            authorAvatarColor = userProfile.avatarColor,
            authorVerified = userProfile.isVerified,
            content = combinedContent,
            postType = "standard"
        )
        createPost(newPost)
        db.collection("posts").document(originalPost.id).update(
            "quotesCount", FieldValue.increment(1),
            "impactScore", FieldValue.increment(3)
        ).await()
    }

    // ----------------------------------------------------
    // COMMENTS
    // ----------------------------------------------------
    fun observeComments(postId: String): Flow<List<Comment>> {
        return db.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { it.toObjects(Comment::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "comments")
                emit(emptyList())
            }
    }

    suspend fun addComment(postId: String, content: String, parentCommentId: String? = null) {
        val uid = requireUserId()
        val profile = getUserProfile(uid) ?: return
        val docRef = db.collection("comments").document()

        val payload = hashMapOf<String, Any?>(
            "postId" to postId,
            "userId" to uid,
            "authorName" to profile.displayName,
            "authorUsername" to profile.username,
            "authorAvatarSymbol" to profile.avatarSymbol,
            "authorAvatarColor" to profile.avatarColor,
            "authorVerified" to profile.isVerified,
            "content" to content,
            "parentCommentId" to parentCommentId,
            "likesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            val postRef = db.collection("posts").document(postId)
            postRef.update(
                "commentsCount", FieldValue.increment(1),
                "impactScore", FieldValue.increment(1)
            ).await()

            notifyAuthor(postRef, "comment", "علّق: ${content.take(40)}")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "comments")
            throw e
        }
    }

    // ----------------------------------------------------
    // STORIES (24 Hours Text Only)
    // ----------------------------------------------------
    fun observeStories(): Flow<List<Story>> {
        val now = Timestamp.now()
        return db.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { it.toObjects(Story::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "stories")
                emit(emptyList())
            }
    }

    suspend fun createStory(content: String, templateType: String, bgGradient: String, textColor: String, textSize: Int) {
        val uid = requireUserId()
        val profile = getUserProfile(uid) ?: return
        val docRef = db.collection("stories").document()

        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.HOUR_OF_DAY, 24)
        val expiresAt = Timestamp(cal.time)

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "authorName" to profile.displayName,
            "authorUsername" to profile.username,
            "authorAvatarSymbol" to profile.avatarSymbol,
            "authorAvatarColor" to profile.avatarColor,
            "content" to content,
            "templateType" to templateType,
            "bgGradient" to bgGradient,
            "textColor" to textColor,
            "textSize" to textSize,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "stories")
            throw e
        }
    }

    suspend fun deleteStory(storyId: String) {
        requireUserId()
        try {
            db.collection("stories").document(storyId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "stories/$storyId")
            throw e
        }
    }

    // ----------------------------------------------------
    // BOOKMARKS
    // ----------------------------------------------------
    fun observeBookmarks(): Flow<List<Bookmark>> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("bookmarks")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Bookmark::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "bookmarks")
                emit(emptyList())
            }
    }

    suspend fun toggleBookmark(post: Post, category: String = "أخرى"): Boolean {
        val uid = requireUserId()
        val bookmarkId = "${post.id}_$uid"
        val docRef = db.collection("bookmarks").document(bookmarkId)

        return try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
                db.collection("posts").document(post.id).update(
                    "bookmarksCount", FieldValue.increment(-1)
                ).await()
                false
            } else {
                val payload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "postId" to post.id,
                    "postContent" to post.content,
                    "authorName" to post.authorName,
                    "authorUsername" to post.authorUsername,
                    "category" to category,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                db.collection("posts").document(post.id).update(
                    "bookmarksCount", FieldValue.increment(1),
                    "impactScore", FieldValue.increment(2)
                ).await()
                true
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "bookmarks/$bookmarkId")
            false
        }
    }

    suspend fun isPostBookmarked(postId: String): Boolean {
        val uid = currentUserId ?: return false
        return try {
            db.collection("bookmarks").document("${postId}_$uid").get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    // ----------------------------------------------------
    // DRAFTS
    // ----------------------------------------------------
    fun observeDrafts(): Flow<List<Draft>> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("drafts")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects(Draft::class.java) }
            .catch { emit(emptyList()) }
    }

    suspend fun saveDraft(content: String, draftType: String = "standard", tags: List<String> = emptyList()) {
        val uid = requireUserId()
        val docRef = db.collection("drafts").document()
        val payload = hashMapOf(
            "userId" to uid,
            "content" to content,
            "draftType" to draftType,
            "tags" to tags,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun deleteDraft(draftId: String) {
        db.collection("drafts").document(draftId).delete().await()
    }

    // ----------------------------------------------------
    // NOTIFICATIONS
    // ----------------------------------------------------
    fun observeNotifications(): Flow<List<NotificationItem>> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { it.toObjects(NotificationItem::class.java) }
            .catch { emit(emptyList()) }
    }

    suspend fun markNotificationRead(id: String) {
        db.collection("notifications").document(id).update("isRead", true).await()
    }

    private suspend fun notifyAuthor(postRef: com.google.firebase.firestore.DocumentReference, type: String, preview: String) {
        val uid = currentUserId ?: return
        try {
            val postSnap = postRef.get().await()
            val authorId = postSnap.getString("userId") ?: return
            if (authorId == uid) return // Don't notify self

            val sender = getUserProfile(uid)
            val notifRef = db.collection("notifications").document()
            notifRef.set(hashMapOf(
                "recipientId" to authorId,
                "actorId" to uid,
                "actorName" to (sender?.displayName ?: "مستخدم نُوران"),
                "actorUsername" to (sender?.username ?: ""),
                "type" to type,
                "targetId" to postRef.id,
                "previewText" to preview,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()
        } catch (_: Exception) {}
    }

    // ----------------------------------------------------
    // FOLLOWS
    // ----------------------------------------------------
    suspend fun isFollowing(targetUserId: String): Boolean {
        val uid = currentUserId ?: return false
        val docId = "${uid}_$targetUserId"
        return try {
            db.collection("follows").document(docId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFollow(targetUserId: String): Boolean {
        val uid = requireUserId()
        if (uid == targetUserId) return false
        val docId = "${uid}_$targetUserId"
        val docRef = db.collection("follows").document(docId)

        return try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
                db.collection("users").document(uid).update("followingCount", FieldValue.increment(-1)).await()
                db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(-1)).await()
                false
            } else {
                docRef.set(hashMapOf(
                    "followerId" to uid,
                    "followingId" to targetUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()
                db.collection("users").document(uid).update("followingCount", FieldValue.increment(1)).await()
                db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(1)).await()

                val sender = getUserProfile(uid)
                db.collection("notifications").document().set(hashMapOf(
                    "recipientId" to targetUserId,
                    "actorId" to uid,
                    "actorName" to (sender?.displayName ?: "مستخدم"),
                    "actorUsername" to (sender?.username ?: ""),
                    "type" to "follow",
                    "targetId" to uid,
                    "previewText" to "بدأ بمتابعتك في نُوران",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()
                true
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "follows/$docId")
            false
        }
    }

    // ----------------------------------------------------
    // USER SETTINGS & WIRD PROGRESS
    // ----------------------------------------------------
    suspend fun getUserSettings(): UserSettings {
        val uid = requireUserId()
        return try {
            val doc = db.collection("userSettings").document(uid).get().await()
            doc.toObject(UserSettings::class.java) ?: UserSettings(userId = uid)
        } catch (e: Exception) {
            UserSettings(userId = uid)
        }
    }

    suspend fun saveUserSettings(settings: UserSettings) {
        val uid = requireUserId()
        val docRef = db.collection("userSettings").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "isPrivate" to settings.isPrivate,
            "mutedWords" to settings.mutedWords,
            "allowRepliesFrom" to settings.allowRepliesFrom,
            "allowMentionsFrom" to settings.allowMentionsFrom,
            "showActivityStatus" to settings.showActivityStatus,
            "dailyWirdStreak" to settings.dailyWirdStreak,
            "completedWirdToday" to settings.completedWirdToday,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun completeWirdForToday() {
        val uid = requireUserId()
        val settings = getUserSettings()
        val newStreak = settings.dailyWirdStreak + 1
        saveUserSettings(settings.copy(completedWirdToday = true, dailyWirdStreak = newStreak))
    }

    // ----------------------------------------------------
    // DAILY CONTENT (Word of the day, Wird)
    // ----------------------------------------------------
    fun observeDailyContent(): Flow<List<DailyContent>> {
        return db.collection("dailyContent")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .snapshots()
            .map { it.toObjects(DailyContent::class.java) }
            .catch { emit(emptyList()) }
    }

    suspend fun seedDailyContentIfEmpty() {
        try {
            val count = db.collection("dailyContent").limit(1).get().await().size()
            if (count == 0) {
                val seed1 = hashMapOf(
                    "title" to "كلمة اليوم",
                    "content" to "«إِنَّ مِنَ الكَلِمِ لَسِحْرًا، وَإِنَّ مِنَ الشِّعْرِ لَحُكْمًا» — الكلمة الطيبة شجرة أصلها ثابت وفرعها في السماء، تُنير دروب الحائرين وتغرس الأمل في القلوب.",
                    "type" to "word_of_day",
                    "source" to "حديث نبوي شريف وأثر",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val seed2 = hashMapOf(
                    "title" to "ورد اليوم",
                    "content" to "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾\nقراءة سورة الملك قبل النوم، والاستغفار مائة مرة، والصلاة على النبي ﷺ.",
                    "type" to "daily_wird",
                    "source" to "سورة الرعد | الذكر والسكينة",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("dailyContent").document("word_of_day_1").set(seed1).await()
                db.collection("dailyContent").document("daily_wird_1").set(seed2).await()
            }
        } catch (_: Exception) {}
    }
}
