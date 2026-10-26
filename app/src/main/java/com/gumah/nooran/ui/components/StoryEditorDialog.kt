package com.gumah.nooran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gumah.nooran.ui.theme.AmiriFontFamily
import com.gumah.nooran.ui.theme.SoftGold

@Composable
fun StoryEditorDialog(
    onPublish: (content: String, templateType: String, bgGradient: String, textColor: String, textSize: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = listOf(
        "دعاء اليوم" to "اللهم إنا نسألك من الخير كله عاجله وآجله، ما علمنا منه وما لم نعلم.",
        "ذكر اليوم" to "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
        "حكمة" to "الكلمة الطيبة بلسمٌ للقلوب وغراسٌ باقٍ في النفوس.",
        "استغفار" to "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ.",
        "آية" to "﴿وَقُولُوا لِلنَّاسِ حُسْنًا﴾",
        "صلاة على النبي ﷺ" to "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ ﷺ",
        "خاطرة" to "كلما ضاقت عليك المسالك، لا تنسَ أن نور الله أقرب إليك من حبل الوريد.",
        "صباح الخير" to "صباحٌ مشرق بنور الإيمان والرضا والسكينة.",
        "مساء الخير" to "مساء السكينة وراحة البال بذكر الرحمن.",
        "شكر وامتنان" to "الحمد لله حمداً كثيراً طيباً مباركاً فيه ملء السماوات والأرض."
    )

    var selectedTemplate by remember { mutableStateOf(templates[0].first) }
    var storyContent by remember { mutableStateOf(templates[0].second) }
    var selectedGradient by remember { mutableStateOf("emerald") }
    var textSize by remember { mutableStateOf(24) }

    val bgBrush = remember(selectedGradient) {
        when (selectedGradient) {
            "emerald" -> Brush.verticalGradient(listOf(Color(0xFF0B3B24), Color(0xFF04190F)))
            "night" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
            "gold" -> Brush.verticalGradient(listOf(Color(0xFF382D16), Color(0xFF140F05)))
            else -> Brush.verticalGradient(listOf(Color(0xFF14532D), Color(0xFF052E16)))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Close & Title & Publish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء")
                    }

                    Text(
                        text = "قصة جديدة • عبّر بالكلمات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            if (storyContent.isNotBlank()) {
                                onPublish(storyContent, selectedTemplate, selectedGradient, "#FFFFFF", textSize)
                            }
                        },
                        enabled = storyContent.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("نشر")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // Templates Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { (tmplName, defaultTxt) ->
                        val isSelected = tmplName == selectedTemplate
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTemplate = tmplName
                                storyContent = defaultTxt
                            },
                            label = { Text(tmplName, fontSize = 12.sp) }
                        )
                    }
                }

                // Live Preview Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgBrush)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = selectedTemplate,
                                color = SoftGold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = storyContent.ifBlank { "اكتب كلماتك هنا..." },
                            fontFamily = AmiriFontFamily,
                            fontSize = textSize.sp,
                            lineHeight = (textSize * 1.6f).sp,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editable Input
                OutlinedTextField(
                    value = storyContent,
                    onValueChange = { storyContent = it },
                    label = { Text("نص القصة (24 ساعة)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Customization Controls: Gradients & Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gradients
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("emerald" to Color(0xFF0B3B24), "night" to Color(0xFF0F172A), "gold" to Color(0xFF382D16)).forEach { (id, color) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedGradient == id) 2.5.dp else 1.dp,
                                        color = if (selectedGradient == id) SoftGold else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedGradient = id }
                            )
                        }
                    }

                    // Font Size Toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20 to "صغير", 24 to "متوسط", 30 to "كبير").forEach { (size, label) ->
                            OutlinedButton(
                                onClick = { textSize = size },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (textSize == size) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
