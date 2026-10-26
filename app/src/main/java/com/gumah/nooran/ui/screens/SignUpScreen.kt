package com.gumah.nooran.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.gumah.nooran.model.UserProfile
import com.gumah.nooran.ui.theme.SoftGold
import com.gumah.nooran.util.AuthValidators
import com.gumah.nooran.viewmodel.NooranViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: NooranViewModel,
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,
    onGoogleSignInSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Multi-stage step: 1 = Name & Username, 2 = Email & Password, 3 = Terms & Complete
    var currentStep by remember { mutableStateOf(1) }

    // Form Fields
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    // Validation & State
    val usernameAvailability by viewModel.usernameCheckState.collectAsState()
    val usernameSuggestions by viewModel.usernameSuggestions.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val passwordStrength = remember(password) { AuthValidators.evaluatePasswordStrength(password) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle & Step Indicator
            Text(
                text = "ابدأ رحلتك في نُوران",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "المرحلة $currentStep من 3",
                style = MaterialTheme.typography.labelMedium,
                color = SoftGold,
                fontWeight = FontWeight.Bold
            )

            // Step Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Step 1: Full Name & Username
            if (currentStep == 1) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = { Text("الاسم الكامل") },
                    placeholder = { Text("مثال: أحمد محمد") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_fullname_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        val clean = input.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
                        username = clean
                        errorMessage = null
                        viewModel.checkUsername(clean)
                    },
                    label = { Text("اسم المستخدم (Username)") },
                    placeholder = { Text("مثال: ahmad_7") },
                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        when (usernameAvailability) {
                            true -> Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            false -> Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            null -> Unit
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_username_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Username validation feedback
                Spacer(modifier = Modifier.height(6.dp))
                if (username.isNotBlank()) {
                    val validationRes = AuthValidators.validateUsername(username)
                    if (validationRes is AuthValidators.UsernameValidationResult.Invalid) {
                        Text(
                            text = "✕ ${validationRes.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    } else {
                        when (usernameAvailability) {
                            true -> Text(
                                text = "✓ اسم المستخدم متاح",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            false -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "✕ اسم المستخدم مستخدم بالفعل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    if (usernameSuggestions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "اقتراحات متاحة:", style = MaterialTheme.typography.labelSmall)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            usernameSuggestions.forEach { suggestion ->
                                                SuggestionChip(
                                                    onClick = {
                                                        username = suggestion
                                                        viewModel.checkUsername(suggestion)
                                                    },
                                                    label = { Text(suggestion, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            null -> Unit
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        val nameErr = AuthValidators.validateFullName(fullName)
                        if (nameErr != null) {
                            errorMessage = nameErr
                            return@Button
                        }
                        val userErr = AuthValidators.validateUsername(username)
                        if (userErr is AuthValidators.UsernameValidationResult.Invalid) {
                            errorMessage = userErr.message
                            return@Button
                        }
                        if (usernameAvailability == false) {
                            errorMessage = "اسم المستخدم غير متاح، اختر اسماً آخر"
                            return@Button
                        }
                        errorMessage = null
                        currentStep = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_step1_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("المتابعة (المرحلة 2)", fontWeight = FontWeight.Bold)
                }
            }

            // Step 2: Email & Password
            if (currentStep == 2) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("البريد الإلكتروني") },
                    placeholder = { Text("example@email.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("كلمة المرور") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true
                )

                // Password strength indicator
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قوة كلمة المرور: ${passwordStrength.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (passwordStrength) {
                                AuthValidators.PasswordStrength.STRONG -> Color(0xFF2E7D32)
                                AuthValidators.PasswordStrength.MEDIUM -> SoftGold
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (index < passwordStrength.level) {
                                                when (passwordStrength) {
                                                    AuthValidators.PasswordStrength.STRONG -> Color(0xFF2E7D32)
                                                    AuthValidators.PasswordStrength.MEDIUM -> SoftGold
                                                    else -> MaterialTheme.colorScheme.error
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("تأكيد كلمة المرور") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_confirm_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true
                )

                if (confirmPassword.isNotBlank() && confirmPassword != password) {
                    Text(
                        text = "✕ كلمتا المرور غير متطابقتين",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("السابق")
                    }

                    Button(
                        onClick = {
                            val emailErr = AuthValidators.validateEmail(email)
                            if (emailErr != null) {
                                errorMessage = emailErr
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "كلمة المرور يجب أن لا تقل عن 6 أحرف"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "كلمتا المرور غير متطابقتين"
                                return@Button
                            }
                            errorMessage = null
                            currentStep = 3
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("المتابعة (المرحلة 3)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Step 3: Terms & Final Creation
            if (currentStep == 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملخص الحساب:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "• الاسم: $fullName", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "• اسم المستخدم: @$username", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "• البريد الإلكتروني: $email", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreedToTerms = !agreedToTerms },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أوافق على شروط الاستخدام وسياسة الخصوصية وأتعهد بنشر الكلمة الطيبة في نُوران.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (!agreedToTerms) {
                            errorMessage = "يرجى الموافقة على شروط الاستخدام للمتابعة"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null

                        // 1. Create account via Firebase Auth
                        Firebase.auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { authRes ->
                                val user = authRes.user
                                if (user != null) {
                                    val uid = user.uid
                                    // 2. Send email verification
                                    user.sendEmailVerification()

                                    // 3. Create Firestore User profile
                                    val profile = UserProfile(
                                        uid = uid,
                                        displayName = fullName.trim(),
                                        username = username.trim().lowercase(),
                                        usernameLowercase = username.trim().lowercase(),
                                        email = email.trim().lowercase(),
                                        emailVerified = false,
                                        avatarSymbol = "✨",
                                        avatarColor = "#14532D"
                                    )
                                    coroutineScope.launch {
                                        try {
                                            viewModel.updateProfile(profile.displayName, profile.username, "", profile.avatarSymbol, profile.avatarColor)
                                            isLoading = false
                                            onSignUpSuccess(email.trim())
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = "تم إنشاء الحساب ولكن تعذر حفظ الملف الشخصي: ${e.message}"
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "تعذر الحصول على بيانات المستخدم"
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = when {
                                    e.message?.contains("email-already-in-use") == true ->
                                        "هذا البريد الإلكتروني مرتبط بحساب بالفعل."
                                    e.message?.contains("weak-password") == true ->
                                        "كلمة المرور ضعيفة جداً."
                                    e.message?.contains("password-login-disabled") == true || e.message?.contains("CONFIGURATION_NOT_FOUND") == true ->
                                        "يرجى تفعيل موفر Email/Password في Firebase Console أو استخدام Google Sign-In المتاح والمفعل."
                                    else -> e.localizedMessage ?: "فشل إنشاء الحساب"
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading && agreedToTerms
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("إنشاء الحساب وبدء الرحلة", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider: "──────── أو ────────"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  أو  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Separate Google Sign In button
            FilledTonalButton(
                onClick = {
                    isGoogleLoading = true
                    errorMessage = null
                    launchGoogleSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                        onSuccess = { user ->
                            isGoogleLoading = false
                            onGoogleSignInSuccess(user)
                        },
                        onError = { err ->
                            isGoogleLoading = false
                            errorMessage = err
                        },
                        onCancelled = { isGoogleLoading = false }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_google_button"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isGoogleLoading
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "G  المتابعة باستخدام Google",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Already have an account? Sign in
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لديك حساب بالفعل؟ ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "تسجيل الدخول",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
