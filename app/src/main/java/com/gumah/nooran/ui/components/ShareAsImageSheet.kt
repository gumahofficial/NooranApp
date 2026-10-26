package com.gumah.nooran.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gumah.nooran.model.Post
import com.gumah.nooran.ui.theme.AmiriFontFamily
import com.gumah.nooran.ui.theme.SoftGold
import java.io.File
import java.io.FileOutputStream

enum class CardTemplate(
    val title: String,
    val bgColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val composeBg: Color,
    val composeText: Color
) {
    EMERALD("زمردي", 0xFF0B3B24.toInt(), 0xFFFFFFFF.toInt(), 0xFFD4AF37.toInt(), Color(0xFF0B3B24), Color.White),
    DARK_FOREST("أخضر داكن", 0xFF062316.toInt(), 0xFFFBF9F4.toInt(), 0xFFE5C365.toInt(), Color(0xFF062316), Color(0xFFFBF9F4)),
    IVORY("عاجي", 0xFFFBF9F4.toInt(), 0xFF14532D.toInt(), 0xFFD4AF37.toInt(), Color(0xFFFBF9F4), Color(0xFF14532D)),
    NIGHT("ليلي", 0xFF0A130E.toInt(), 0xFFF4EFE6.toInt(), 0xFF6B9080.toInt(), Color(0xFF0A130E), Color(0xFFF4EFE6)),
    GOLDEN("ذهبي", 0xFF2A2111.toInt(), 0xFFF5EFE6.toInt(), 0xFFE5C365.toInt(), Color(0xFF2A2111), Color(0xFFF5EFE6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAsImageSheet(
    post: Post,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(CardTemplate.EMERALD) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مشاركة المنشور كصورة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "اختر القالب المفضل للمشاركة على منصات التواصل",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = selectedTemplate.composeBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نُوران | كلمات تنير القلب",
                            fontFamily = AmiriFontFamily,
                            color = SoftGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✦",
                            color = SoftGold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "«${post.content}»",
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                        color = selectedTemplate.composeText,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(
                        color = SoftGold.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "— ${post.authorName} (@${post.authorUsername})",
                        color = selectedTemplate.composeText.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Template Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CardTemplate.values().forEach { template ->
                    val isSelected = template == selectedTemplate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTemplate = template }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(template.composeBg)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) SoftGold else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        shareCardAsBitmap(context, post, selectedTemplate)
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة عبر التطبيقات")
                }
            }
        }
    }
}

private fun shareCardAsBitmap(context: Context, post: Post, template: CardTemplate) {
    try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = template.bgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative Border
        val borderPaint = Paint().apply {
            color = template.accentColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val margin = 50f
        canvas.drawRoundRect(margin, margin, width - margin, height - margin, 40f, 40f, borderPaint)

        // Inner frame
        val innerMargin = 70f
        borderPaint.strokeWidth = 2f
        borderPaint.alpha = 140
        canvas.drawRoundRect(innerMargin, innerMargin, width - innerMargin, height - innerMargin, 30f, 30f, borderPaint)

        // Top App Title
        val headerPaint = Paint().apply {
            color = template.accentColor
            textSize = 52f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("نُوران | كَلِمَاتٌ تُنِيرُ القَلْب", width / 2f, 180f, headerPaint)

        // Divider
        val divPaint = Paint().apply {
            color = template.accentColor
            strokeWidth = 3f
            alpha = 120
        }
        canvas.drawLine(width / 2f - 200f, 220f, width / 2f + 200f, 220f, divPaint)

        // Main Post Text using StaticLayout
        val textPaint = TextPaint().apply {
            color = template.textColor
            textSize = 58f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val textWidth = width - 260

        val formattedText = "«${post.content}»"
        val staticLayout = StaticLayout.Builder.obtain(
            formattedText, 0, formattedText.length, textPaint, textWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        val textTop = (height - staticLayout.height) / 2f - 40f
        canvas.save()
        canvas.translate(width / 2f, textTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // Author Attribution
        val authorPaint = Paint().apply {
            color = template.textColor
            textSize = 42f
            textAlign = Paint.Align.CENTER
            alpha = 220
            isAntiAlias = true
        }
        canvas.drawText("— ${post.authorName} (@${post.authorUsername})", width / 2f, height - 200f, authorPaint)

        // App watermarking
        val appTagPaint = Paint().apply {
            color = template.accentColor
            textSize = 34f
            textAlign = Paint.Align.CENTER
            alpha = 180
            isAntiAlias = true
        }
        canvas.drawText("منصة نُوران الاجتماعية الإسلامية", width / 2f, height - 130f, appTagPaint)

        // Save to cache file and launch Android Share Sheet
        val imagesFolder = File(context.cacheDir, "shared_images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "nooran_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "«${post.content}»\n— ${post.authorName}\nمن تطبيق نُوران | كلمات تنير القلب")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة المنشور"))
    } catch (e: Exception) {
        // Fallback to text sharing if file provider throws
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "«${post.content}»\n— ${post.authorName}\nمن تطبيق نُوران | كلمات تنير القلب")
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "مشاركة المنشور"))
    }
}
