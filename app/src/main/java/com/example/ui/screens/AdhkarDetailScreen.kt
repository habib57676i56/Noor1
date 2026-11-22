package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.AdhkarCatalog
import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

@Composable
fun AdhkarDetailScreen(
    categoryId: String,
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    val category = remember(categoryId) { AdhkarCategory.fromId(categoryId) }
    val items = remember(category) { AdhkarCatalog.byCategory(category) }

    val favoriteIds by viewModel.favoriteDhikrIds.collectAsStateWithLifecycle()
    val progressMap by viewModel.adhkarProgressMap.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingDhikrId.collectAsStateWithLifecycle()

    var focusCardMode by remember { mutableStateOf(true) }
    var activeIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSpeakingDhikr() }
    }

    fun copyDhikr(item: DhikrItem) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val full = "${item.textAr}\n\n${item.sourceAr} (${item.gradingAr})"
        cm.setPrimaryClip(ClipData.newPlainText("Noor Dhikr", full))
        Toast.makeText(context, L10n.pick(lang, "تم نسخ الذكر", "Copied to clipboard"), Toast.LENGTH_SHORT).show()
    }

    fun shareDhikr(item: DhikrItem) {
        val shareText = buildString {
            append(item.textAr)
            append("\n\n")
            if (item.transliteration.isNotBlank()) {
                append(item.transliteration)
                append("\n")
            }
            append(L10n.pick(lang, item.meaningAr, item.meaningEn))
            append("\n— ")
            append(L10n.pick(lang, "${item.sourceAr} (${item.gradingAr})", "${item.sourceEn} (${item.gradingEn})"))
            append("\n[NOOR]")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, L10n.pick(lang, "مشاركة الذكر", "Share Dhikr"))) }
    }

    val completedInCat = items.count { progressMap[it.id]?.isCompleted == true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adhkar_detail_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, category.nameAr, category.nameEn),
            subtitle = L10n.pick(lang, "$completedInCat / ${items.size} مكتمل اليوم", "$completedInCat / ${items.size} completed today"),
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = { focusCardMode = !focusCardMode },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (focusCardMode) Icons.Rounded.FormatListBulleted else Icons.Rounded.Style,
                        contentDescription = "Switch Mode",
                        tint = noorColors.goldAccent
                    )
                }
            }
        )

        if (focusCardMode && items.isNotEmpty()) {
            val safeIdx = activeIndex.coerceIn(0, items.lastIndex)
            val currentItem = items[safeIdx]
            val currentCount = progressMap[currentItem.id]?.currentCount ?: 0
            val isCompleted = currentCount >= currentItem.targetCount
            val isFav = favoriteIds.contains(currentItem.id)
            val isSpeaking = speakingId == currentItem.id

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Goal Ring Banner
                LiquidGlassCard(goldBorder = isCompleted, contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                            CircularProgressIndicator(
                                progress = { (currentCount.toFloat() / currentItem.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isCompleted) Color(0xFF4ADE80) else noorColors.goldAccent,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                strokeWidth = 5.dp
                            )
                            Text(
                                text = "$currentCount",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$currentCount / ${currentItem.targetCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color(0xFF4ADE80) else noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, "الذكر ${safeIdx + 1} من ${items.size} · اضغط على البطاقة للعد", "Card ${safeIdx + 1} of ${items.size} · Tap parchment to count"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavoriteDhikr(currentItem.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Luxury Warm Ivory Parchment Card
                WarmIvoryParchmentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.incrementDhikr(currentItem)
                            if (currentCount + 1 >= currentItem.targetCount && safeIdx < items.lastIndex) {
                                activeIndex = safeIdx + 1
                            }
                        }
                        .testTag("dhikr_focus_card")
                ) {
                    Text(
                        text = currentItem.textAr,
                        fontFamily = AmiriFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 42.sp,
                        color = IvoryInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentItem.transliteration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = currentItem.transliteration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvoryInk.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = L10n.pick(lang, currentItem.meaningAr, currentItem.meaningEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryInk.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Verified Source & Hadith Grading Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldDeep.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "${currentItem.sourceAr} · الدرجة: ${currentItem.gradingAr}",
                                "${currentItem.sourceEn} · Grade: ${currentItem.gradingEn}"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Listen, Copy & Share Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleSpeakDhikr(currentItem) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = "Listen",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, if (isSpeaking) "إيقاف" else "استماع", if (isSpeaking) "Stop" else "Listen"))
                        }
                        Button(
                            onClick = { shareDhikr(currentItem) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "مشاركة", "Share"))
                        }
                        IconButton(
                            onClick = { copyDhikr(currentItem) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EmeraldDeep.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = EmeraldDeep)
                        }
                    }
                }

                // Previous / Count / Next controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (safeIdx > 0) activeIndex = safeIdx - 1 },
                        enabled = safeIdx > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "السابق", "Previous"))
                    }
                    Button(
                        onClick = { viewModel.incrementDhikr(currentItem) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFF15803D) else GoldPrimary,
                            contentColor = if (isCompleted) Color.White else EmeraldDeep
                        )
                    ) {
                        Text(
                            text = if (isCompleted) {
                                L10n.pick(lang, "تم الإكمال ✓", "Done ✓")
                            } else {
                                L10n.pick(lang, "تسبيح ($currentCount/${currentItem.targetCount})", "Count ($currentCount/${currentItem.targetCount})")
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { if (safeIdx < items.lastIndex) activeIndex = safeIdx + 1 },
                        enabled = safeIdx < items.lastIndex,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(L10n.pick(lang, "التالي", "Next"))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val currentCount = progressMap[item.id]?.currentCount ?: 0
                    val isDone = currentCount >= item.targetCount
                    val isFav = favoriteIds.contains(item.id)
                    val isSpeaking = speakingId == item.id

                    LiquidGlassCard(
                        goldBorder = isDone,
                        onClick = { viewModel.incrementDhikr(item) },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDone) Color(0xFF15803D) else noorColors.goldAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$currentCount / ${item.targetCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) Color.White else noorColors.goldAccent
                                    )
                                }
                                if (isDone) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                                }
                            }
                            Row {
                                IconButton(onClick = { viewModel.toggleSpeakDhikr(item) }, modifier = Modifier.size(40.dp)) {
                                    Icon(if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = noorColors.goldAccent)
                                }
                                IconButton(onClick = { copyDhikr(item) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = { shareDhikr(item) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share")
                                }
                                IconButton(onClick = { viewModel.toggleFavoriteDhikr(item.id) }, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.textAr,
                            fontFamily = AmiriFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 38.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L10n.pick(lang, item.meaningAr, item.meaningEn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(lang, "${item.sourceAr} · ${item.gradingAr}", "${item.sourceEn} · ${item.gradingEn}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = noorColors.goldAccent
                        )
                    }
                }
            }
        }
    }
}
