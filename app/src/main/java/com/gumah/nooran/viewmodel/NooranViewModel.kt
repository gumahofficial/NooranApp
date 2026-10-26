package com.gumah.nooran.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumah.nooran.ai.AiResult
import com.gumah.nooran.ai.NooranAiService
import com.gumah.nooran.data.NooranRepository
import com.gumah.nooran.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FeedTab {
    data object ForYou : FeedTab
    data object Following : FeedTab
    data object Latest : FeedTab
}

class NooranViewModel(
    private val repository: NooranRepository,
    private val aiService: NooranAiService
) : ViewModel() {

    // Current User
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    // Public Profile Viewing
    private val _viewingProfile = MutableStateFlow<UserProfile?>(null)
    val viewingProfile: StateFlow<UserProfile?> = _viewingProfile.asStateFlow()
    val isFollowingViewingProfile = MutableStateFlow(false)

    // Current Feed Tab
    private val _activeFeedTab = MutableStateFlow<FeedTab>(FeedTab.ForYou)
    val activeFeedTab: StateFlow<FeedTab> = _activeFeedTab.asStateFlow()

    // All Posts & Derived Feeds
    val allPosts: StateFlow<List<Post>> = repository.observePosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stories (Active 24h)
    val activeStories: StateFlow<List<Story>> = repository.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Content (Word of the day & Wird)
    val dailyContentList: StateFlow<List<DailyContent>> = repository.observeDailyContent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks
    val bookmarks: StateFlow<List<Bookmark>> = repository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.observeNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Drafts
    val drafts: StateFlow<List<Draft>> = repository.observeDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Settings
    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    // Active Dialogs & Overlays
    val quietReadingPost = MutableStateFlow<Post?>(null)
    val activeStoryViewing = MutableStateFlow<Story?>(null)
    val shareAsImagePost = MutableStateFlow<Post?>(null)
    val showCreatePostDialog = MutableStateFlow(false)
    val showStoryEditorDialog = MutableStateFlow(false)
    val showEditProfileDialog = MutableStateFlow(false)

    // AI State
    val aiLoading = MutableStateFlow(false)
    val aiSafetyMessage = MutableStateFlow<String?>(null)
    val aiSuggestion = MutableStateFlow<String?>(null)

    // User Reactions Map (postId -> reactionType)
    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()

    // User Bookmarks Map (postId -> Boolean)
    private val _userBookmarks = MutableStateFlow<Set<String>>(emptySet())
    val userBookmarks: StateFlow<Set<String>> = _userBookmarks.asStateFlow()

    init {
        loadCurrentUser()
        viewModelScope.launch {
            repository.seedDailyContentIfEmpty()
        }
    }

    fun setFeedTab(tab: FeedTab) {
        _activeFeedTab.value = tab
    }

    fun loadCurrentUser() {
        val uid = repository.currentUserId ?: return
        viewModelScope.launch {
            repository.observeUserProfile(uid).collect { profile ->
                _currentUserProfile.value = profile
            }
        }
        viewModelScope.launch {
            _userSettings.value = repository.getUserSettings()
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String, avatarSymbol: String, avatarColor: String) {
        val uid = repository.currentUserId ?: return
        viewModelScope.launch {
            val updated = UserProfile(
                uid = uid,
                displayName = displayName,
                username = username,
                bio = bio,
                avatarSymbol = avatarSymbol,
                avatarColor = avatarColor
            )
            repository.createOrUpdateUser(updated)
            _currentUserProfile.value = updated
            showEditProfileDialog.value = false
        }
    }

    fun inspectProfile(user: UserProfile) {
        _viewingProfile.value = user
        viewModelScope.launch {
            isFollowingViewingProfile.value = repository.isFollowing(user.uid)
        }
    }

    fun toggleFollowViewingProfile() {
        val target = _viewingProfile.value ?: return
        viewModelScope.launch {
            val following = repository.toggleFollow(target.uid)
            isFollowingViewingProfile.value = following
        }
    }

    // ----------------------------------------------------
    // POST ACTIONS
    // ----------------------------------------------------
    fun createPost(content: String, postType: String = "standard", pollQuestion: String = "", pollOptions: List<String> = emptyList(), tags: List<String> = emptyList()) {
        val profile = _currentUserProfile.value ?: return
        viewModelScope.launch {
            val post = Post(
                userId = profile.uid,
                authorName = profile.displayName,
                authorUsername = profile.username,
                authorAvatarSymbol = profile.avatarSymbol,
                authorAvatarColor = profile.avatarColor,
                authorVerified = profile.isVerified,
                content = content,
                postType = postType,
                pollQuestion = pollQuestion,
                pollOptions = pollOptions,
                tags = tags
            )
            repository.createPost(post)
            showCreatePostDialog.value = false
            aiSafetyMessage.value = null
            aiSuggestion.value = null
        }
    }

    fun createThread(tweets: List<String>) {
        val profile = _currentUserProfile.value ?: return
        if (tweets.isEmpty()) return
        viewModelScope.launch {
            val threadId = java.util.UUID.randomUUID().toString()
            tweets.forEachIndexed { index, text ->
                val post = Post(
                    userId = profile.uid,
                    authorName = profile.displayName,
                    authorUsername = profile.username,
                    authorAvatarSymbol = profile.avatarSymbol,
                    authorAvatarColor = profile.avatarColor,
                    authorVerified = profile.isVerified,
                    content = text,
                    postType = "thread",
                    threadId = threadId,
                    threadPosition = index + 1,
                    threadTotal = tweets.size
                )
                repository.createPost(post)
            }
            showCreatePostDialog.value = false
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun toggleReaction(postId: String, reactionType: String) {
        viewModelScope.launch {
            val hasReaction = repository.toggleReaction(postId, reactionType)
            val currentMap = _userReactions.value.toMutableMap()
            if (hasReaction) {
                currentMap[postId] = reactionType
            } else {
                currentMap.remove(postId)
            }
            _userReactions.value = currentMap
        }
    }

    fun toggleBookmark(post: Post, category: String = "أخرى") {
        viewModelScope.launch {
            val isNowBookmarked = repository.toggleBookmark(post, category)
            val currentSet = _userBookmarks.value.toMutableSet()
            if (isNowBookmarked) currentSet.add(post.id) else currentSet.remove(post.id)
            _userBookmarks.value = currentSet
        }
    }

    fun repostPost(post: Post) {
        viewModelScope.launch {
            repository.repostPost(post)
        }
    }

    fun quotePost(post: Post, comment: String) {
        viewModelScope.launch {
            repository.quotePost(post, comment)
        }
    }

    fun votePoll(postId: String, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(postId, optionIndex)
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            repository.addComment(postId, text)
        }
    }

    // ----------------------------------------------------
    // STORIES
    // ----------------------------------------------------
    fun createStory(content: String, templateType: String, bgGradient: String, textColor: String, textSize: Int) {
        viewModelScope.launch {
            repository.createStory(content, templateType, bgGradient, textColor, textSize)
            showStoryEditorDialog.value = false
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
            activeStoryViewing.value = null
        }
    }

    // ----------------------------------------------------
    // SEARCH
    // ----------------------------------------------------
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = repository.searchUsers(query)
        }
    }

    // ----------------------------------------------------
    // WIRD & SETTINGS
    // ----------------------------------------------------
    fun completeDailyWird() {
        viewModelScope.launch {
            repository.completeWirdForToday()
            _userSettings.value = repository.getUserSettings()
        }
    }

    fun saveMutedWords(words: List<String>) {
        val current = _userSettings.value
        val updated = current.copy(mutedWords = words)
        viewModelScope.launch {
            repository.saveUserSettings(updated)
            _userSettings.value = updated
        }
    }

    // ----------------------------------------------------
    // AI ASSISTANT
    // ----------------------------------------------------
    fun checkContentSafety(text: String) {
        if (text.isBlank()) return
        aiLoading.value = true
        viewModelScope.launch {
            when (val res = aiService.reviewContentSafety(text)) {
                is AiResult.Success -> aiSafetyMessage.value = res.text
                is AiResult.Error -> aiSafetyMessage.value = res.message
            }
            aiLoading.value = false
        }
    }

    fun enhanceText(text: String, action: String) {
        if (text.isBlank()) return
        aiLoading.value = true
        viewModelScope.launch {
            when (val res = aiService.enhancePost(text, action)) {
                is AiResult.Success -> aiSuggestion.value = res.text
                is AiResult.Error -> aiSuggestion.value = res.message
            }
            aiLoading.value = false
        }
    }

    // ----------------------------------------------------
    // AUTHENTICATION & USERNAME CHECKING
    // ----------------------------------------------------
    val usernameCheckState = MutableStateFlow<Boolean?>(null)
    val usernameSuggestions = MutableStateFlow<List<String>>(emptyList())

    fun checkUsername(username: String) {
        val clean = username.trim().lowercase()
        if (clean.length < 3) {
            usernameCheckState.value = null
            usernameSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            val available = repository.isUsernameAvailable(clean)
            usernameCheckState.value = available
            if (!available) {
                usernameSuggestions.value = com.gumah.nooran.util.AuthValidators.generateUsernameSuggestions(clean)
            } else {
                usernameSuggestions.value = emptyList()
            }
        }
    }

    suspend fun onGoogleAuthSuccess(firebaseUser: com.google.firebase.auth.FirebaseUser): UserProfile {
        val profile = repository.ensureGoogleUserProfile(firebaseUser)
        _currentUserProfile.value = profile
        return profile
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return repository.sendPasswordReset(email)
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return repository.sendEmailVerification()
    }

    suspend fun reloadAndCheckEmailVerified(): Boolean {
        val isVerified = repository.reloadAndCheckEmailVerified()
        if (isVerified) {
            loadCurrentUser()
        }
        return isVerified
    }

    fun saveUserInterests(interests: List<String>) {
        viewModelScope.launch {
            repository.saveUserInterests(interests)
            val current = _currentUserProfile.value
            if (current != null) {
                _currentUserProfile.value = current.copy(userInterests = interests)
            }
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val res = repository.deleteAccount()
        if (res.isSuccess) {
            _currentUserProfile.value = null
        }
        return res
    }
}
