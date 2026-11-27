package com.example.ui.screens.ecosystem

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.cards.CardAspectRatio
import com.example.domain.cards.CardVisualTheme
import com.example.domain.cards.IslamicCardCategory
import com.example.domain.cards.IslamicCardStudio
import com.example.domain.model.AppLanguage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslamicCardDesignerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedCategory by remember { mutableStateOf(IslamicCardCategory.QURAN) }
    val categoryTemplates = remember(selectedCategory) {
        IslamicCardStudio.templates.filter { it.category == selectedCategory }
    }
    var selectedTheme by remember { mutableStateOf(IslamicCardStudio.themes.first()) }
    var aspectRatio by remember { mutableStateOf(CardAspectRatio.SQUARE) }

    val firstTemplate = IslamicCardStudio.templates.first()
    var mainText by remember { mutableStateOf(firstTemplate.bodyAr) }
    var referenceText by remember { mutableStateOf(firstTemplate.referenceAr) }
    var subtitleText by remember {
        mutableStateOf(
            when (lang) {
                AppLanguage.UZBEK -> firstTemplate.subtitleUz
                AppLanguage.ENGLISH, AppLanguage.RUSSIAN -> firstTemplate.subtitleEn
                AppLanguage.ARABIC -> ""
            }
        )
    }
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var showWatermark by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("card_designer_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مصمم البطاقات الإسلامية", "Islamic Card Designer", "Islomiy tabrik va oyat dizayneri"),
            subtitle = L10n.pick(
                lang,
                "صمّم بطاقات آيات وأدعية وتهاني بجودة عالية وشاركها أو احفظها مباشرة",
                "Design high-resolution Quran, Dua & Eid cards to share or save",
                "Yuqori sifatli oyat, duo va tabrik kartochkalarini yarating va ulashing"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live High-Craft Card Preview Canvas
            item {
                val previewRatio = if (aspectRatio == CardAspectRatio.SQUARE) 1f else 0.8f
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IslamicCardLivePreview(
                        mainText = mainText,
                        referenceText = referenceText,
                        subtitleText = subtitleText,
                        theme = selectedTheme,
                        fontScale = fontScale,
                        showWatermark = showWatermark,
                        modifier = Modifier
                            .fillMaxWidth(if (aspectRatio == CardAspectRatio.SQUARE) 0.94f else 0.82f)
                            .aspectRatio(previewRatio)
                            .testTag("card_designer_preview")
                    )
                }
            }

            // 2. Primary Share & Save to Gallery Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val bmp = IslamicCardStudio.renderCardBitmap(
                                context = context,
                                mainText = mainText,
                                referenceText = referenceText,
                                subtitleText = subtitleText,
                                theme = selectedTheme,
                                aspectRatio = aspectRatio,
                                fontScale = fontScale,
                                showWatermark = showWatermark
                            )
                            IslamicCardStudio.shareCardBitmap(context, bmp).onFailure {
                                Toast.makeText(
                                    context,
                                    L10n.pick(lang, "تعذّر مشاركة الصورة", "Unable to share image", "Rasmni ulashib bo'lmadi"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("card_designer_share_btn")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = L10n.pick(lang, "مشاركة كصورة", "Share Image", "Rasm ulashish"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val bmp = IslamicCardStudio.renderCardBitmap(
                                context = context,
                                mainText = mainText,
                                referenceText = referenceText,
                                subtitleText = subtitleText,
                                theme = selectedTheme,
                                aspectRatio = aspectRatio,
                                fontScale = fontScale,
                                showWatermark = showWatermark
                            )
                            IslamicCardStudio.saveCardToGallery(context, bmp)
                                .onSuccess {
                                    Toast.makeText(
                                        context,
                                        L10n.pick(lang, "تم حفظ البطاقة في المعرض بنجاح", "Saved to Pictures/NOOR", "Galereyaga (Pictures/NOOR) saqlandi"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        L10n.pick(lang, "تعذّر الحفظ في المعرض", "Failed to save card", "Saqlashda xatolik yuz berdi"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("card_designer_save_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = L10n.pick(lang, "حفظ في المعرض", "Save to Gallery", "Galereyaga saqlash"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Visual Themes & Aspect Ratio Picker
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = L10n.pick(lang, "التصميم والألوان الفاخرة", "Luxury Artistic Theme", "Hashamatli rang mavzusi"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(IslamicCardStudio.themes, key = { it.id }) { theme ->
                                val isSelected = selectedTheme.id == theme.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { selectedTheme = theme }
                                        .testTag("card_theme_${theme.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(theme.bgTopColor), Color(theme.bgBottomColor))
                                                )
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.2.dp,
                                                color = if (isSelected) GoldBright else Color(theme.accentGoldColor).copy(alpha = 0.5f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "۞",
                                            color = Color(theme.accentGoldColor),
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = L10n.pick(lang, theme.nameAr, theme.nameEn, theme.nameUz),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardAspectRatio.entries.forEach { ratio ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, ratio.labelAr, ratio.labelEn, ratio.labelUz),
                                    selected = aspectRatio == ratio,
                                    onClick = { aspectRatio = ratio },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Ready-Made Presets by Category
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "نماذج جاهزة من القرآن والسنة", "Curated Templates", "Tayyor oyat, duo va tabriklar")
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IslamicCardCategory.entries, key = { it.key }) { cat ->
                            LiquidGlassChip(
                                label = L10n.pick(lang, cat.labelAr, cat.labelEn, cat.labelUz),
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categoryTemplates, key = { it.id }) { tmpl ->
                            LiquidGlassCard(
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    mainText = tmpl.bodyAr
                                    referenceText = tmpl.referenceAr
                                    subtitleText = when (lang) {
                                        AppLanguage.UZBEK -> tmpl.subtitleUz
                                        AppLanguage.ENGLISH, AppLanguage.RUSSIAN -> tmpl.subtitleEn
                                        AppLanguage.ARABIC -> ""
                                    }
                                },
                                modifier = Modifier.width(205.dp)
                            ) {
                                Text(
                                    text = tmpl.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tmpl.bodyAr,
                                    fontFamily = AmiriFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tmpl.referenceAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 5. Custom Text, Font Scale & Watermark Controls
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = L10n.pick(lang, "تخصيص النص والخط", "Customize Text & Typography", "Matn va shriftni sozlash"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = mainText,
                            onValueChange = { mainText = it },
                            label = { Text(L10n.pick(lang, "النص الرئيسي (آية / دعاء / تهنئة)", "Main Calligraphy Text", "Asosiy matn")) },
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = subtitleText,
                            onValueChange = { subtitleText = it },
                            label = { Text(L10n.pick(lang, "ترجمة أو سطر إضافي (اختياري)", "Translation / Subtitle (Optional)", "Tarjima yoki izoh (ixtiyoriy)")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = referenceText,
                            onValueChange = { referenceText = it },
                            label = { Text(L10n.pick(lang, "المصدر أو التوقيع", "Reference / Signature", "Manba yoki imzo")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            Text(
                                text = L10n.pick(lang, "حجم الخط: ${(fontScale * 100).toInt()}%", "Font Scale: ${(fontScale * 100).toInt()}%", "Shrift o'lchami: ${(fontScale * 100).toInt()}%"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = fontScale,
                                onValueChange = { fontScale = it },
                                valueRange = 0.75f..1.35f
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = L10n.pick(lang, "إظهار توقيع «تطبيق نور»", "Show NOOR signature", "«NOOR» imzosini ko'rsatish"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(checked = showWatermark, onCheckedChange = { showWatermark = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslamicCardLivePreview(
    mainText: String,
    referenceText: String,
    subtitleText: String,
    theme: CardVisualTheme,
    fontScale: Float,
    showWatermark: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = Color(theme.accentGoldColor)
    val primary = Color(theme.primaryTextColor)
    val secondary = Color(theme.secondaryTextColor)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(theme.bgTopColor), Color(theme.bgMidColor), Color(theme.bgBottomColor))
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
    ) {
        // Ornamental geometric frame + glow rendered cleanly with Compose Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(theme.glowColor), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.45f),
                    radius = size.width * 0.58f
                )
            )

            // Center watermark 8-point star
            drawPreviewStar(size.width / 2f, size.height * 0.46f, size.width * 0.32f, accent.copy(alpha = 0.10f), 1.5f)
            drawPreviewStar(size.width / 2f, size.height * 0.46f, size.width * 0.20f, accent.copy(alpha = 0.08f), 1.2f)

            // Double ornamental border
            val m1 = 14.dp.toPx()
            val m2 = 20.dp.toPx()
            drawRoundRect(
                color = accent.copy(alpha = 0.72f),
                topLeft = Offset(m1, m1),
                size = Size(size.width - m1 * 2, size.height - m1 * 2),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.36f),
                topLeft = Offset(m2, m2),
                size = Size(size.width - m2 * 2, size.height - m2 * 2),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 0.9.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "✦  ۞  ✦",
                color = accent,
                fontSize = 16.sp,
                fontFamily = AmiriFontFamily
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = mainText.ifBlank { "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾" },
                    fontFamily = AmiriFontFamily,
                    fontSize = (22f * fontScale).sp,
                    lineHeight = (35f * fontScale).sp,
                    color = primary,
                    textAlign = TextAlign.Center
                )
                if (subtitleText.isNotBlank()) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(1.dp)
                        .background(accent.copy(alpha = 0.55f))
                )
                if (referenceText.isNotBlank()) {
                    Text(
                        text = referenceText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (showWatermark) {
                Text(
                    text = "تطبيق نور · NOOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary.copy(alpha = 0.72f)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewStar(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val p1 = Path()
    val p2 = Path()
    for (i in 0 until 4) {
        val a1 = Math.toRadians(i * 90.0 + 45.0)
        val a2 = Math.toRadians(i * 90.0)
        val x1 = cx + radius * cos(a1).toFloat()
        val y1 = cy + radius * sin(a1).toFloat()
        val x2 = cx + radius * cos(a2).toFloat()
        val y2 = cy + radius * sin(a2).toFloat()
        if (i == 0) {
            p1.moveTo(x1, y1)
            p2.moveTo(x2, y2)
        } else {
            p1.lineTo(x1, y1)
            p2.lineTo(x2, y2)
        }
    }
    p1.close()
    p2.close()
    drawPath(p1, color = color, style = Stroke(width = strokeWidth))
    drawPath(p2, color = color, style = Stroke(width = strokeWidth))
}
