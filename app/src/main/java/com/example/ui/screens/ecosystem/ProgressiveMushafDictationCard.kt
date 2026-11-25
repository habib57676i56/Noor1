package com.example.ui.screens.ecosystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.quran.ProgressiveRecitationState
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess

/**
 * Interactive Mushaf Page / Surah "Write-As-I-Recite" (يكتب وراي) progressive memorization canvas.
 * - Words start hidden and appear sequentially in immutable Uthmani script as the user recites them.
 * - If the user makes a mistake, the word stays hidden ("لا يكتب"), highlights red, and triggers an alert chime.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressiveMushafDictationSection(
    state: ProgressiveRecitationState,
    isPageMode: Boolean,
    selectedPage: Int,
    isLoading: Boolean,
    loadError: String?,
    mistakeSoundEnabled: Boolean,
    onToggleMistakeSound: () -> Unit,
    onReset: () -> Unit,
    onNudgeWord: () -> Unit,
    onJumpToAyah: (surah: Int, ayah: Int) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onRetryLoad: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header stats & controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(lang, state.scopeTitleAr, state.scopeTitleEn),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Text(
                    text = L10n.pick(
                        lang,
                        "المكتوب: ${state.revealedCount} / ${state.totalWords} كلمة · أخطاء: ${state.mistakeCount} · الآية الحالية: ${state.currentAyahNumber}",
                        "Written: ${state.revealedCount} / ${state.totalWords} words · Mistakes: ${state.mistakeCount} · Ayah ${state.currentAyahNumber}",
                        "Yozilgan: ${state.revealedCount} / ${state.totalWords} so'z · Xato: ${state.mistakeCount} · Oyat ${state.currentAyahNumber}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("hifz_progressive_stats_text")
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleMistakeSound,
                    modifier = Modifier.testTag("hifz_mistake_sound_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (mistakeSoundEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                        contentDescription = L10n.pick(lang, "صوت تنبيه الخطأ", "Mistake Alert Sound", "Xato tovushi"),
                        tint = if (mistakeSoundEnabled) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onNudgeWord,
                    enabled = state.revealedCount < state.totalWords,
                    modifier = Modifier.testTag("hifz_progressive_hint_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = L10n.pick(lang, "إظهار كلمة واحدة", "Hint Next Word", "Keyingi so'zni ko'rsatish"),
                        tint = noorColors.goldAccent
                    )
                }
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("hifz_progressive_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = L10n.pick(lang, "إعادة التسميع", "Reset Page", "Qaytadan boshlash"),
                        tint = noorColors.goldAccent
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (state.isCompleted) SemanticSuccess else GoldPrimary
        )

        // Mistake warning banner (when spoken word did not match -> word NOT written + sound plays)
        AnimatedVisibility(visible = state.isMistakeActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hifz_mistake_banner"),
                shape = RoundedCornerShape(12.dp),
                color = SemanticError.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, SemanticError.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = SemanticError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(
                            lang,
                            "لم تُكتب الكلمة — خطأ في الآية ${state.lastMistakeAyah} (${state.lastMistakeSpoken}). صحّح التلاوة للمتابعة",
                            "Word not written — mistake in Ayah ${state.lastMistakeAyah} (${state.lastMistakeSpoken}). Re-recite accurately",
                            "So'z yozilmadi — ${state.lastMistakeAyah}-oyatda xato (${state.lastMistakeSpoken}). To'g'ri o'qib davom eting"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = SemanticError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else if (!loadError.isNullOrBlank() && state.words.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SemanticError.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, SemanticError.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(loadError, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onRetryLoad) {
                        Text(L10n.pick(lang, "إعادة المحاولة", "Retry"))
                    }
                }
            }
        } else {
            val cursorBg by animateColorAsState(
                targetValue = if (state.isMistakeActive) SemanticError.copy(alpha = 0.24f) else EmeraldPrimary.copy(alpha = 0.16f),
                label = "cursor_bg"
            )
            val cursorBorder by animateColorAsState(
                targetValue = if (state.isMistakeActive) SemanticError else EmeraldPrimary,
                label = "cursor_border"
            )

            WarmIvoryParchmentCard {
                if (state.words.isEmpty()) {
                    Text(
                        text = L10n.pick(lang, "اختر صفحة أو سورة للبدء", "Select a page or Surah to begin"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvoryInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = L10n.pick(
                            lang,
                            "تسميع مصحفي متدرج: اقرأ وسيُكتب المصحف كلمة بكلمة من خلفك (اضغط ﴿رقم﴾ للبدء من آية)",
                            "Write-as-you-recite Mushaf: speak cleanly and each Uthmani word appears in sequence (tap ﴿#﴾ to jump)",
                            "Aytib boring va yozib boradi: to'g'ri o'qilgan har bir so'z ketma-ket chiqadi (o'tish uchun ﴿#﴾ bosing)"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryInk.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))

                    // Enforce authentic RTL Mushaf flow even when app UI language is English or Uzbek
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hifz_progressive_page_canvas"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.words.forEach { word ->
                                if (word.isFirstWordOfSurah) {
                                    val sMeta = SurahCatalog.findSurah(word.surahNumber)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GoldPrimary.copy(alpha = 0.16f))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(vertical = 4.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "﴿ سورة ${sMeta.nameAr} ﴾",
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDeep
                                        )
                                    }
                                }

                                val isRevealed = word.globalIndex < state.revealedCount
                                val isCursor = word.globalIndex == state.revealedCount
                                val isLatestWritten = word.globalIndex == state.revealedCount - 1

                                if (isRevealed) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isLatestWritten) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent
                                            )
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = word.displayUthmani,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 21.sp,
                                            lineHeight = 32.sp,
                                            color = if (isLatestWritten) EmeraldDeep else IvoryInk,
                                            fontWeight = if (isLatestWritten) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else if (isCursor) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cursorBg)
                                            .border(1.2.dp, cursorBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                            .testTag("hifz_progressive_cursor_slot")
                                    ) {
                                        Text(
                                            text = if (state.isMistakeActive) "✕ ____" else "▸ ____",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.isMistakeActive) SemanticError else EmeraldDeep
                                        )
                                    }
                                } else {
                                    val dotCount = word.normalizedToken.length.coerceIn(2, 6)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(IvoryInk.copy(alpha = 0.06f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "•".repeat(dotCount),
                                            fontSize = 15.sp,
                                            color = IvoryInk.copy(alpha = 0.28f)
                                        )
                                    }
                                }

                                if (word.isLastWordOfAyah) {
                                    val ayahDone = word.globalIndex < state.revealedCount
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { onJumpToAyah(word.surahNumber, word.ayahNumber) }
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "﴿${word.ayahNumber}﴾",
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ayahDone) GoldPrimary else IvoryInk.copy(alpha = 0.42f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.isCompleted) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hifz_progressive_completed_banner"),
                shape = RoundedCornerShape(14.dp),
                color = SemanticSuccess.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, SemanticSuccess)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SemanticSuccess)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "ما شاء الله! أتممت تسميع هذا الجزء بدون ترك أي كلمة",
                                "MashAllah! You completed every word in this section",
                                "Mashaalloh! Ushbu bo'limdagi barcha so'zlarni to'liq aytdingiz"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SemanticSuccess
                        )
                    }
                    if (isPageMode && selectedPage < MadinahMushafPageCatalog.TOTAL_PAGES) {
                        Button(
                            onClick = onNextPage,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(L10n.pick(lang, "التالية", "Next"))
                        }
                    }
                }
            }
        }

        if (isPageMode) {
            HorizontalDivider(color = noorColors.goldAccent.copy(alpha = 0.16f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrevPage,
                    enabled = selectedPage > 1,
                    modifier = Modifier.testTag("hifz_prev_page_btn")
                ) {
                    Text(L10n.pick(lang, "← الصفحة السابقة", "← Prev Page", "← Oldingi sahifa"))
                }
                Text(
                    text = L10n.pick(lang, "صفحة $selectedPage / ٦٠٤", "Page $selectedPage / 604"),
                    style = MaterialTheme.typography.labelMedium,
                    color = noorColors.goldAccent,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onNextPage,
                    enabled = selectedPage < MadinahMushafPageCatalog.TOTAL_PAGES,
                    modifier = Modifier.testTag("hifz_next_page_btn")
                ) {
                    Text(L10n.pick(lang, "الصفحة التالية →", "Next Page →", "Keyingi sahifa →"))
                }
            }
        }
    }
}

/**
 * Clean selector dialog covering ALL 604 Madinah Mushaf pages and 30 Ajza so users can recite
 * page-by-page across the entire Quran.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MushafPagePickerDialog(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var pageInput by remember(currentPage) { mutableStateOf(currentPage.toString()) }
    var selectedJuz by remember(currentPage) { mutableStateOf(MadinahMushafPageCatalog.juzForPage(currentPage)) }
    val pagesInJuz = remember(selectedJuz) { MadinahMushafPageCatalog.pagesForJuz(selectedJuz).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("hifz_page_picker_dialog"),
        title = {
            Text(
                text = L10n.pick(lang, "اختر صفحة المصحف (١ – ٦٠٤)", "Choose Mushaf Page (1–604)", "Mushaf sahifasini tanlang (1–604)"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { txt -> pageInput = txt.filter(Char::isDigit).take(3) },
                        label = { Text(L10n.pick(lang, "رقم الصفحة (1-604)", "Page Number (1-604)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val p = (pageInput.toIntOrNull() ?: currentPage).coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES)
                            onSelectPage(p)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(L10n.pick(lang, "انتقال", "Go", "O'tish"))
                    }
                }

                Text(
                    text = L10n.pick(lang, "تصفّح حسب الجزء (١–٣٠):", "Browse by Juz (1–30):", "Juz bo'yicha (1–30):"),
                    style = MaterialTheme.typography.labelMedium,
                    color = noorColors.goldAccent,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..30).forEach { juz ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "جزء $juz", "Juz $juz", "$juz-juz"),
                                    selected = selectedJuz == juz,
                                    onClick = { selectedJuz = juz }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(pagesInJuz, key = { it }) { p ->
                        val meta = remember(p) { MadinahMushafPageCatalog.pageMeta(p) }
                        val isSel = p == currentPage
                        val surahsNames = meta.surahsOnPage.joinToString(" · ") {
                            L10n.pick(lang, it.nameAr, it.nameEn)
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectPage(p)
                                    onDismiss()
                                },
                            color = if (isSel) GoldPrimary.copy(alpha = 0.16f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = noorColors.goldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = L10n.pick(lang, "صفحة $p · $surahsNames", "Page $p · $surahsNames"),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (isSel) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إغلاق", "Close"))
            }
        }
    )
}
