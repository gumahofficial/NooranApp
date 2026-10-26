package com.gumah.nooran.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.gumah.nooran.R
import com.gumah.nooran.ui.theme.AmiriFontFamily
import com.gumah.nooran.ui.theme.SoftGold
import com.gumah.nooran.viewmodel.NooranViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    viewModel: NooranViewModel,
    userEmail: String,
    onVerificationSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var cooldownSeconds by remember { mutableIntStateOf(60) }
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // 60-second cooldown timer
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds--
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nooran_logo),
                contentDescription = "شعار نُوران",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = SoftGold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "تأكيد البريد الإلكتروني",
                fontFamily = AmiriFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "أرسلنا رابط التحقق والتفعيل إلى:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = userEmail.ifBlank { Firebase.auth.currentUser?.email ?: "بريدك المسجل" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "يرجى فتح صندوق الوارد في بريدك الإلكتروني والنقر على رابط التأكيد لتفعيل حسابك وضمان أمان المنصة.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!statusMessage.isNullOrBlank()) {
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Primary Button: "لقد تحققت من بريدي"
            Button(
                onClick = {
                    isChecking = true
                    statusMessage = null
                    coroutineScope.launch {
                        val verified = viewModel.reloadAndCheckEmailVerified()
                        isChecking = false
                        if (verified) {
                            onVerificationSuccess()
                        } else {
                            isError = true
                            statusMessage = "لم يتم تأكيد البريد الإلكتروني بعد. يرجى فتح الرسالة والنقر على الرابط، ثم إعادة المحاولة."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("verify_email_check_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("لقد نقرت على الرابط وتحققت", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resend Button with 60s cooldown
            OutlinedButton(
                onClick = {
                    isResending = true
                    statusMessage = null
                    coroutineScope.launch {
                        val res = viewModel.sendEmailVerification()
                        isResending = false
                        if (res.isSuccess) {
                            cooldownSeconds = 60
                            isError = false
                            statusMessage = "تم إرسال رابط تفعيل جديد بنجاح. تفقد صندوق الوارد أو الرسائل غير المرغوب فيها."
                        } else {
                            isError = true
                            statusMessage = res.exceptionOrNull()?.localizedMessage ?: "تعذر إعادة الإرسال حالياً"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("verify_email_resend_button"),
                shape = RoundedCornerShape(16.dp),
                enabled = cooldownSeconds == 0 && !isResending
            ) {
                if (isResending) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else if (cooldownSeconds > 0) {
                    Text("إعادة إرسال الرابط بعد ($cooldownSeconds ثانية)", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("إعادة إرسال رابط التحقق", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign out button
            TextButton(
                onClick = {
                    Firebase.auth.signOut()
                    onSignOut()
                },
                modifier = Modifier.testTag("verify_email_signout_button")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل الخروج أو تغيير البريد", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
