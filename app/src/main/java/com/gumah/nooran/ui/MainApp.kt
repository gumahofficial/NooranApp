package com.gumah.nooran.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.gumah.nooran.model.UserProfile
import com.gumah.nooran.ui.components.*
import com.gumah.nooran.ui.screens.*
import com.gumah.nooran.viewmodel.NooranViewModel

import kotlinx.coroutines.launch

enum class Screen {
    SPLASH, WELCOME, LOGIN, SIGNUP, FORGOT_PASSWORD, VERIFY_EMAIL, ONBOARDING, MAIN, SETTINGS
}

@Composable
fun MainApp(
    viewModel: NooranViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var selectedNavTab by remember { mutableStateOf(NavItem.HOME) }
    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var pendingVerifyEmail by remember { mutableStateOf("") }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    when (currentScreen) {
        Screen.SPLASH -> {
            SplashScreen(
                onSplashFinished = {
                    val user = Firebase.auth.currentUser
                    if (user != null) {
                        viewModel.loadCurrentUser()
                        val isPasswordProvider = user.providerData.any { it.providerId == "password" }
                        if (isPasswordProvider && !user.isEmailVerified) {
                            pendingVerifyEmail = user.email ?: ""
                            currentScreen = Screen.VERIFY_EMAIL
                        } else {
                            currentScreen = Screen.MAIN
                        }
                    } else {
                        currentScreen = Screen.WELCOME
                    }
                }
            )
        }

        Screen.WELCOME -> {
            WelcomeAuthScreen(
                onNavigateToLogin = { currentScreen = Screen.LOGIN },
                onNavigateToSignUp = { currentScreen = Screen.SIGNUP },
                onGoogleSignInSuccess = { user ->
                    coroutineScope.launch {
                        viewModel.onGoogleAuthSuccess(user)
                        currentScreen = Screen.ONBOARDING
                    }
                }
            )
        }

        Screen.LOGIN -> {
            BackHandler { currentScreen = Screen.WELCOME }
            LoginScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.WELCOME },
                onNavigateToSignUp = { currentScreen = Screen.SIGNUP },
                onNavigateToForgotPassword = { currentScreen = Screen.FORGOT_PASSWORD },
                onGoogleSignInSuccess = { user ->
                    coroutineScope.launch {
                        viewModel.onGoogleAuthSuccess(user)
                        currentScreen = Screen.MAIN
                    }
                },
                onEmailLoginSuccess = {
                    val user = Firebase.auth.currentUser
                    if (user != null && !user.isEmailVerified) {
                        pendingVerifyEmail = user.email ?: ""
                        currentScreen = Screen.VERIFY_EMAIL
                    } else {
                        viewModel.loadCurrentUser()
                        currentScreen = Screen.MAIN
                    }
                }
            )
        }

        Screen.SIGNUP -> {
            BackHandler { currentScreen = Screen.WELCOME }
            SignUpScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.WELCOME },
                onNavigateToLogin = { currentScreen = Screen.LOGIN },
                onSignUpSuccess = { regEmail ->
                    pendingVerifyEmail = regEmail
                    currentScreen = Screen.VERIFY_EMAIL
                },
                onGoogleSignInSuccess = { user ->
                    coroutineScope.launch {
                        viewModel.onGoogleAuthSuccess(user)
                        currentScreen = Screen.ONBOARDING
                    }
                }
            )
        }

        Screen.FORGOT_PASSWORD -> {
            BackHandler { currentScreen = Screen.LOGIN }
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.LOGIN },
                onNavigateToLogin = { currentScreen = Screen.LOGIN }
            )
        }

        Screen.VERIFY_EMAIL -> {
            BackHandler { currentScreen = Screen.WELCOME }
            EmailVerificationScreen(
                viewModel = viewModel,
                userEmail = pendingVerifyEmail,
                onVerificationSuccess = {
                    viewModel.loadCurrentUser()
                    currentScreen = Screen.ONBOARDING
                },
                onSignOut = {
                    currentScreen = Screen.WELCOME
                }
            )
        }

        Screen.ONBOARDING -> {
            BackHandler { currentScreen = Screen.MAIN }
            OnboardingInterestsScreen(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onComplete = {
                    viewModel.loadCurrentUser()
                    currentScreen = Screen.MAIN
                }
            )
        }

        Screen.SETTINGS -> {
            BackHandler { currentScreen = Screen.MAIN }
            SettingsScreen(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onBackClick = { currentScreen = Screen.MAIN },
                onSignOut = { currentScreen = Screen.WELCOME }
            )
        }

        Screen.MAIN -> {
            val notifications by viewModel.notifications.collectAsState()
            val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

            Scaffold(
                bottomBar = {
                    NooranBottomNav(
                        selectedItem = selectedNavTab,
                        unreadNotifsCount = unreadCount,
                        onItemSelected = { item ->
                            if (item == NavItem.CREATE) {
                                viewModel.showCreatePostDialog.value = true
                            } else {
                                selectedNavTab = item
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedNavTab) {
                        NavItem.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSearch = { selectedNavTab = NavItem.EXPLORE },
                                onNavigateToNotifications = { selectedNavTab = NavItem.NOTIFICATIONS }
                            )
                        }
                        NavItem.EXPLORE -> {
                            ExploreScreen(viewModel = viewModel)
                        }
                        NavItem.CREATE -> {
                            // Modal sheet will trigger
                        }
                        NavItem.NOTIFICATIONS -> {
                            NotificationsScreen(viewModel = viewModel)
                        }
                        NavItem.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }
                    }
                }
            }

            // Global Dialogs & Modals
            val createPostOpen by viewModel.showCreatePostDialog.collectAsState()
            val storyEditorOpen by viewModel.showStoryEditorDialog.collectAsState()
            val editProfileOpen by viewModel.showEditProfileDialog.collectAsState()
            val quietReadingPost by viewModel.quietReadingPost.collectAsState()
            val activeStoryViewing by viewModel.activeStoryViewing.collectAsState()
            val shareAsImagePost by viewModel.shareAsImagePost.collectAsState()
            val activeStories by viewModel.activeStories.collectAsState()
            val userBookmarks by viewModel.userBookmarks.collectAsState()
            val currentUserProfile by viewModel.currentUserProfile.collectAsState()

            val aiLoading by viewModel.aiLoading.collectAsState()
            val aiSafetyMsg by viewModel.aiSafetyMessage.collectAsState()
            val aiSuggestion by viewModel.aiSuggestion.collectAsState()

            // 1. Create Post / Thread / Poll Sheet
            if (createPostOpen) {
                CreatePostSheet(
                    aiLoading = aiLoading,
                    aiSafetyMsg = aiSafetyMsg,
                    aiSuggestion = aiSuggestion,
                    onCheckSafety = { viewModel.checkContentSafety(it) },
                    onEnhance = { text, action -> viewModel.enhanceText(text, action) },
                    onPublishPost = { content, tags -> viewModel.createPost(content, tags = tags) },
                    onPublishThread = { tweets -> viewModel.createThread(tweets) },
                    onPublishPoll = { q, opts -> viewModel.createPost(content = q, postType = "poll", pollQuestion = q, pollOptions = opts) },
                    onDismiss = {
                        viewModel.showCreatePostDialog.value = false
                        viewModel.aiSafetyMessage.value = null
                        viewModel.aiSuggestion.value = null
                    }
                )
            }

            // 2. Story Editor Dialog
            if (storyEditorOpen) {
                StoryEditorDialog(
                    onPublish = { content, tmpl, gradient, color, size ->
                        viewModel.createStory(content, tmpl, gradient, color, size)
                    },
                    onDismiss = { viewModel.showStoryEditorDialog.value = false }
                )
            }

            // 3. Story Viewer Dialog
            if (activeStoryViewing != null) {
                StoryViewerDialog(
                    stories = activeStories,
                    initialStory = activeStoryViewing!!,
                    currentUserId = firebaseUser?.uid,
                    onDeleteStory = { viewModel.deleteStory(it) },
                    onDismiss = { viewModel.activeStoryViewing.value = null }
                )
            }

            // 4. Quiet Reading Dialog
            if (quietReadingPost != null) {
                QuietReadingDialog(
                    post = quietReadingPost!!,
                    isBookmarked = userBookmarks.contains(quietReadingPost!!.id),
                    onBookmarkClick = { viewModel.toggleBookmark(quietReadingPost!!) },
                    onDismiss = { viewModel.quietReadingPost.value = null }
                )
            }

            // 5. Share As Image Sheet
            if (shareAsImagePost != null) {
                ShareAsImageSheet(
                    post = shareAsImagePost!!,
                    onDismiss = { viewModel.shareAsImagePost.value = null }
                )
            }

            // 6. Edit Profile Dialog
            if (editProfileOpen && currentUserProfile != null) {
                EditProfileDialog(
                    currentUser = currentUserProfile!!,
                    onSave = { name, uname, bio, sym, col ->
                        viewModel.updateProfile(name, uname, bio, sym, col)
                    },
                    onDismiss = { viewModel.showEditProfileDialog.value = false }
                )
            }
        }
    }
}
