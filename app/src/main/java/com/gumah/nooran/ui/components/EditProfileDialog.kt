package com.gumah.nooran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gumah.nooran.model.UserProfile
import com.gumah.nooran.ui.theme.SoftGold

@Composable
fun EditProfileDialog(
    currentUser: UserProfile,
    onSave: (displayName: String, username: String, bio: String, avatarSymbol: String, avatarColor: String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser.displayName) }
    var username by remember { mutableStateOf(currentUser.username) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var selectedSymbol by remember { mutableStateOf(currentUser.avatarSymbol.ifBlank { "✨" }) }
    var selectedColor by remember { mutableStateOf(currentUser.avatarColor.ifBlank { "#14532D" }) }

    val symbols = listOf("✨", "🌿", "📖", "🕌", "🤍", "🌟", "🕊️", "📿", "🕯️", "💎")
    val colors = listOf(
        "#14532D" to Color(0xFF14532D),
        "#0B3B24" to Color(0xFF0B3B24),
        "#D4AF37" to Color(0xFFD4AF37),
        "#0F172A" to Color(0xFF0F172A),
        "#6B9080" to Color(0xFF6B9080),
        "#382D16" to Color(0xFF382D16)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تعديل الهوية والرمز الشخصي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Preview
                val parsedColor = remember(selectedColor) {
                    try {
                        Color(android.graphics.Color.parseColor(selectedColor))
                    } catch (_: Exception) {
                        Color(0xFF14532D)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(2.5.dp, SoftGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedSymbol, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symbol Choices
                Text(text = "اختر رمز الهوية:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    symbols.take(5).forEach { sym ->
                        Text(
                            text = sym,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedSymbol = sym }
                                .padding(4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    symbols.drop(5).forEach { sym ->
                        Text(
                            text = sym,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedSymbol = sym }
                                .padding(4.dp)
                        )
                    }
                }

                // Color Choices
                Text(text = "اختر لون الخلفية:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.forEach { (hex, col) ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) SoftGold else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم الظاهر") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("اسم المستخدم (بدون مسافات)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    label = { Text("النبذة التعريفية (Bio)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (displayName.isNotBlank() && username.isNotBlank()) {
                                onSave(displayName, username, bio, selectedSymbol, selectedColor)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}
