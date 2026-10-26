package com.gumah.nooran.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.gumah.nooran.ui.theme.SoftGold
import com.gumah.nooran.viewmodel.NooranViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NooranViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUserProfile.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var showMutedWordsDialog by remember { mutableStateOf(false) }
    var newWordInput by remember { mutableStateOf("") }
    var currentMutedWords by remember { mutableStateOf(userSettings.mutedWords) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات والخصوصية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Account section
            item {
                SettingsSectionHeader("الحساب والأمان")
                SettingsItem(
                    title = "اسم المستخدم",
                    subtitle = "@${currentUser?.username ?: "user"}",
                    icon = Icons.Outlined.Person
                )
                SettingsItem(
                    title = "حالة التحقق (الشارة المباركة)",
                    subtitle = if (currentUser?.isVerified == true) "موثق رسمياً ✓" else "غير موثق (تُمنح تلقائياً حسب الأثر والنشاط)",
                    icon = Icons.Outlined.Verified,
                    badgeColor = if (currentUser?.isVerified == true) SoftGold else null
                )
            }

            // Appearance & Reading
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("المظهر والقراءة")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onToggleDarkMode
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "الوضع الداكن (الليلي الهادئ)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "أخضر زمردي داكن مريح للعين أثناء القراءة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(imageVector = Icons.Outlined.DarkMode, contentDescription = null)
                        }
                    }
                }
            }

            // Privacy & Moderation
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("الخصوصية وحجب الكلمات")
                SettingsItem(
                    title = "الكلمات المخفية",
                    subtitle = "إخفاء أي منشور يحتوي على كلمات تحددها (${userSettings.mutedWords.size} كلمة)",
                    icon = Icons.Outlined.VisibilityOff,
                    onClick = { showMutedWordsDialog = true }
                )
                SettingsItem(
                    title = "من يستطيع الرد والمتابعة",
                    subtitle = "الجميع في نُوران",
                    icon = Icons.Outlined.Lock
                )
            }

            // About & Islamic Values
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("عن تطبيق نُوران")
                SettingsItem(
                    title = "شعار التطبيق",
                    subtitle = "«كَلِمَاتٌ تُنِيرُ القَلْب» — منصة نصية خالية من الإعلانات والصخب",
                    icon = Icons.Outlined.Info
                )
                SettingsItem(
                    title = "الإصدار",
                    subtitle = "1.0.0 (المعمارية الأصلية)",
                    icon = Icons.Outlined.Code
                )
            }

            // Sign Out & Account Deletion
            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        Firebase.auth.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج من نُوران")
                }

                Spacer(modifier = Modifier.height(12.dp))

                var showDeleteDialog by remember { mutableStateOf(false) }
                var isDeleting by remember { mutableStateOf(false) }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_delete_account_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف الحساب نهائيًا")
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            Text(
                                text = "تأكيد حذف الحساب نهائيًا",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "تحذير: هذا الإجراء نهائي ولا يمكن التراجع عنه مطلقاً.",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "سيتم حذف ملفك الشخصي واسم المستخدم (@${currentUser?.username ?: ""}) وجميع منشوراتك ومشاركاتك نهائياً من قاعدة بيانات نُوران."
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isDeleting = true
                                    coroutineScope.launch {
                                        val res = viewModel.deleteAccount()
                                        isDeleting = false
                                        showDeleteDialog = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_LONG).show()
                                            onSignOut()
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "تعذر حذف الحساب، قد تحتاج لتسجيل الدخول مجدداً للأمان", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !isDeleting
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                                } else {
                                    Text("نعم، احذف حسابي الآن")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteDialog = false },
                                enabled = !isDeleting
                            ) {
                                Text("إلغاء")
                            }
                        }
                    )
                }
            }
        }
    }

    // Muted Words Dialog
    if (showMutedWordsDialog) {
        AlertDialog(
            onDismissRequest = { showMutedWordsDialog = false },
            title = { Text("إدارة الكلمات المخفية") },
            text = {
                Column {
                    Text("المنشورات التي تحتوي هذه الكلمات سيتم تصفيتها تلقائياً من صفحتك:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Button(
                            onClick = {
                                if (newWordInput.isNotBlank()) {
                                    currentMutedWords = currentMutedWords + newWordInput.trim()
                                    viewModel.saveMutedWords(currentMutedWords)
                                    newWordInput = ""
                                }
                            },
                            enabled = newWordInput.isNotBlank()
                        ) {
                            Text("إضافة")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newWordInput,
                            onValueChange = { newWordInput = it },
                            placeholder = { Text("أدخل كلمة...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    currentMutedWords.forEach { word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                currentMutedWords = currentMutedWords.filter { it != word }
                                viewModel.saveMutedWords(currentMutedWords)
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                            Text(text = "• $word", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMutedWordsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = badgeColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
