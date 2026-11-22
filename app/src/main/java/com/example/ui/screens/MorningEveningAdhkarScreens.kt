package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.db.AdhkarCompletionHistoryEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.DhikrItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldMuted
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.IvorySurface
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

/**
 * Dedicated MORNING ADHKAR screen (Part 2 — Highest Priority).
 */
@Composable
fun MorningAdhkarScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.morningAdhkarItems.collectAsStateWithLifecycle()
    val history by viewModel.morningAdhkarHistory.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    DedicatedMorningEveningAdhkarView(
        isMorning = true,
        items = items,
        history = history,
        reminderEnabled = prefs.morningAdhkarReminderEnabled,
        onToggleReminder = { viewModel.setReminder("morning", !prefs.morningAdhkarReminderEnabled) },
        viewModel = viewModel,
        onBack = onBack
    )
}

/**
 * Dedicated EVENING ADHKAR screen (Part 2 — Highest Priority).
 */
@Composable
fun EveningAdhkarScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.eveningAdhkarItems.collectAsStateWithLifecycle()
    val history by viewModel.eveningAdhkarHistory.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    DedicatedMorningEveningAdhkarView(
        isMorning = false,
        items = items,
        history = history,
        reminderEnabled = prefs.eveningAdhkarReminderEnabled,
        onToggleReminder = { viewModel.setReminder("evening", !prefs.eveningAdhkarReminderEnabled) },
        viewModel = viewModel,
        onBack = onBack
    )
}

@Composable
private fun DedicatedMorningEveningAdhkarView(
    isMorning: Boolean,
    items: List<DhikrItem>,
    history: List<AdhkarCompletionHistoryEntity>,
    reminderEnabled: Boolean,
    onToggleReminder: () -> Unit,
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val clipboard = LocalClipboardManager.current
    val progressMap by viewModel.adhkarProgressMap.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteDhikrIds.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingDhikrId.collectAsStateWithLifecycle()

    val categoryId = if (isMorning) "morning" else "evening"
    val todayKey = remember { viewModel.todayDateKey() }

    var guidedMode by rememberSaveable { mutableStateOf(true) }
    var showTranslation by rememberSaveable { mutableStateOf(true) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var reportTargetDhikr by remember { mutableStateOf<DhikrItem?>(null) }

    // Resume from first incomplete item when returning to an in-progress session
    val initialIndex = remember(items.size) {
        val firstIncomplete = items.indexOfFirst { progressMap[it.id]?.isCompleted != true }
        if (firstIncomplete >= 0) firstIncomplete else 0
    }
    var currentIndex by rememberSaveable(categoryId) { mutableIntStateOf(initialIndex) }
    val safeIndex = currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    val completedCount = items.count { progressMap[it.id]?.isCompleted == true }
    val totalCount = items.size
    val overallProgress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    // Persist session progress strictly based on actual completed items (never marks complete on open!)
    LaunchedEffect(completedCount, totalCount, safeIndex) {
        if (completedCount > 0 || safeIndex > 0) {
            viewModel.recordAdhkarSessionState(categoryId, completedCount, totalCount, safeIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSpeakingDhikr() }
    }

    val bannerBrush = if (isMorning) {
        Brush.horizontalGradient(listOf(Color(0xFF2B210E), Color(0xFF132D22), Color(0xFF0A1F18)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF091B29), Color(0xFF08241C), Color(0xFF04120E)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(if (isMorning) "morning_adhkar_screen" else "evening_adhkar_screen")
    ) {
        NoorScreenHeader(
            title = if (isMorning) {
                L10n.pick(lang, "أذكار الصباح", "Morning Adhkar")
            } else {
                L10n.pick(lang, "أذكار المساء", "Evening Adhkar")
            },
            subtitle = L10n.pick(
                lang,
                "موثّقة بالكامل بالتشكيل والسند · $completedCount / $totalCount مكتمل",
                "Verified Arabic & Sources · $completedCount / $totalCount Completed"
            ),
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleReminder) {
                        Icon(
                            imageVector = if (reminderEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            contentDescription = "Reminder",
                            tint = if (reminderEnabled) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Rounded.History, contentDescription = "History", tint = noorColors.goldAccent)
                    }
                }
            }
        )

        // Overall Session Progress & Controls Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(bannerBrush)
                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMorning) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (completedCount == totalCount && totalCount > 0) {
                                    L10n.pick(lang, "تقبّل الله! أتممت جميع الأذكار اليوم", "MashaAllah! Collection completed today")
                                } else {
                                    L10n.pick(
                                        lang,
                                        "التقدم الإجمالي: ${(overallProgress * 100).toInt()}% ($completedCount من $totalCount)",
                                        "Overall Progress: ${(overallProgress * 100).toInt()}% ($completedCount of $totalCount)"
                                    )
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "تعمل بالكامل دون إنترنت · احفظ تقدمك تلقائياً",
                                    "100% Offline Ready · Session auto-resumes"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBright.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.resetTodayAdhkar()
                            currentIndex = 0
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.85f))
                    }
                }

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (completedCount == totalCount && totalCount > 0) Color(0xFF4ADE80) else GoldBright,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "بطاقة مركّزة", "Card Mode"),
                        selected = guidedMode,
                        icon = Icons.Rounded.ViewCarousel,
                        onClick = { guidedMode = true },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "القائمة الكاملة", "Full List"),
                        selected = !guidedMode,
                        icon = Icons.Rounded.FormatListBulleted,
                        onClick = { guidedMode = false },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الشرح", "Notes"),
                        selected = showTranslation,
                        icon = Icons.Rounded.Translate,
                        onClick = { showTranslation = !showTranslation }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (guidedMode && items.isNotEmpty()) {
            val activeDhikr = items[safeIndex]
            val prog = progressMap[activeDhikr.id]?.currentCount ?: 0
            val isDone = prog >= activeDhikr.targetCount
            val isFav = favoriteIds.contains(activeDhikr.id)

            // Quick index selector strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items, key = { _, d -> d.id }) { idx, d ->
                    val done = progressMap[d.id]?.isCompleted == true
                    val selected = idx == safeIndex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    selected -> GoldPrimary
                                    done -> EmeraldPrimary.copy(alpha = 0.85f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { currentIndex = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) EmeraldDeep else if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedContent(targetState = activeDhikr, label = "dhikr_card") { dhikr ->
                    AuthenticParchmentDhikrCard(
                        dhikr = dhikr,
                        indexNumber = safeIndex + 1,
                        totalNumber = items.size,
                        currentCount = prog,
                        isCompleted = isDone,
                        isFavorite = isFav,
                        isSpeaking = speakingId == dhikr.id,
                        showTranslation = showTranslation,
                        onIncrement = {
                            val willComplete = prog + 1 >= dhikr.targetCount
                            viewModel.incrementDhikr(dhikr)
                            if (willComplete && safeIndex + 1 < items.size) {
                                currentIndex = safeIndex + 1
                            }
                        },
                        onMarkDone = {
                            viewModel.markDhikrCompleted(dhikr)
                            if (safeIndex + 1 < items.size) {
                                currentIndex = safeIndex + 1
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavoriteDhikr(dhikr.id) },
                        onToggleSpeak = { viewModel.toggleSpeakDhikr(dhikr) },
                        onCopy = {
                            clipboard.setText(AnnotatedString("${dhikr.textAr}\n(${dhikr.sourceAr})"))
                            viewModel.showBannerMessage(L10n.pick(lang, "تم نسخ الذكر مع المصدر", "Copied Dhikr with source"))
                        },
                        onReport = { reportTargetDhikr = dhikr }
                    )
                }

                // Previous / Next Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (safeIndex > 0) currentIndex = safeIndex - 1 },
                        enabled = safeIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "السابق", "Previous"))
                    }
                    Button(
                        onClick = { if (safeIndex + 1 < items.size) currentIndex = safeIndex + 1 },
                        enabled = safeIndex + 1 < items.size,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(L10n.pick(lang, "التالي", "Next"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(items, key = { _, d -> d.id }) { idx, dhikr ->
                    val prog = progressMap[dhikr.id]?.currentCount ?: 0
                    val isDone = prog >= dhikr.targetCount
                    val isFav = favoriteIds.contains(dhikr.id)
                    AuthenticParchmentDhikrCard(
                        dhikr = dhikr,
                        indexNumber = idx + 1,
                        totalNumber = items.size,
                        currentCount = prog,
                        isCompleted = isDone,
                        isFavorite = isFav,
                        isSpeaking = speakingId == dhikr.id,
                        showTranslation = showTranslation,
                        onIncrement = { viewModel.incrementDhikr(dhikr) },
                        onMarkDone = { viewModel.markDhikrCompleted(dhikr) },
                        onToggleFavorite = { viewModel.toggleFavoriteDhikr(dhikr.id) },
                        onToggleSpeak = { viewModel.toggleSpeakDhikr(dhikr) },
                        onCopy = {
                            clipboard.setText(AnnotatedString("${dhikr.textAr}\n(${dhikr.sourceAr})"))
                            viewModel.showBannerMessage(L10n.pick(lang, "تم نسخ الذكر مع المصدر", "Copied Dhikr with source"))
                        },
                        onReport = { reportTargetDhikr = dhikr }
                    )
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text(
                    if (isMorning) L10n.pick(lang, "سجل إتمام أذكار الصباح", "Morning Adhkar History")
                    else L10n.pick(lang, "سجل إتمام أذكار المساء", "Evening Adhkar History")
                )
            },
            text = {
                if (history.isEmpty()) {
                    Text(
                        L10n.pick(
                            lang,
                            "لم يتم تسجيل جلسات بعد. ابدأ بذكر الله اليوم ليتم حفظ سجلك اليومي تلقائياً.",
                            "No completed sessions recorded yet. Start reciting today to build your daily streak."
                        )
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.take(10).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (entry.dateKey == todayKey) L10n.pick(lang, "اليوم (${entry.dateKey})", "Today (${entry.dateKey})") else entry.dateKey,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${entry.completedItems}/${entry.totalItems}" + if (entry.isFullyCompleted) " ✓" else "",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (entry.isFullyCompleted) Color(0xFF26B37E) else GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }

    reportTargetDhikr?.let { target ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTargetDhikr = null },
            title = { Text(L10n.pick(lang, "الإبلاغ عن ملاحظة نصية أو سند", "Report Text / Source Issue")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = target.textAr.take(80) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(L10n.pick(lang, "وصف الملاحظة", "Describe the correction or issue")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reason.trim().length >= 3) {
                            viewModel.submitContentReport("adhkar", target.id, reason)
                            reportTargetDhikr = null
                        }
                    }
                ) {
                    Text(L10n.pick(lang, "إرسال", "Submit"))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetDhikr = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun AuthenticParchmentDhikrCard(
    dhikr: DhikrItem,
    indexNumber: Int,
    totalNumber: Int,
    currentCount: Int,
    isCompleted: Boolean,
    isFavorite: Boolean,
    isSpeaking: Boolean,
    showTranslation: Boolean,
    onIncrement: () -> Unit,
    onMarkDone: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSpeak: () -> Unit,
    onCopy: () -> Unit,
    onReport: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val remaining = (dhikr.targetCount - currentCount).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(IvorySurface)
            .border(
                width = if (isCompleted) 2.dp else 1.2.dp,
                color = if (isCompleted) EmeraldPrimary else GoldPrimary.copy(alpha = 0.75f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row: Number Badge + Verification Badge + Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldDeep)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$indexNumber / $totalNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = L10n.pick(lang, dhikr.gradingAr, dhikr.gradingEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldDeep,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSpeak, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Listen",
                        tint = EmeraldPrimary
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = GoldMuted)
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFC53030) else GoldMuted
                    )
                }
                IconButton(onClick = onReport, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Flag, contentDescription = "Report", tint = GoldMuted.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }

        // Complete Vocalized Arabic Text
        Text(
            text = dhikr.textAr,
            fontFamily = AmiriFontFamily,
            fontSize = 24.sp,
            lineHeight = 44.sp,
            color = IvoryInk,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Authentic Source Reference Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFEDE2CB))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = L10n.pick(lang, "المصدر: ${dhikr.sourceAr}", "Source: ${dhikr.sourceEn}"),
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldDeep,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showTranslation) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = L10n.pick(lang, dhikr.meaningAr, dhikr.meaningEn),
                    style = MaterialTheme.typography.bodySmall,
                    color = IvoryInk.copy(alpha = 0.85f)
                )
                if (dhikr.transliteration.isNotBlank() && lang == AppLanguage.ENGLISH) {
                    Text(
                        text = dhikr.transliteration,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldMuted
                    )
                }
            }
        }

        // Interactive Repetition Counter & Mark Completed Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onIncrement,
                enabled = !isCompleted,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("dhikr_tap_btn_${dhikr.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) EmeraldPrimary else EmeraldDeep,
                    contentColor = Color.White,
                    disabledContainerColor = EmeraldPrimary,
                    disabledContentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF86EFAC) else GoldBright
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) {
                        L10n.pick(lang, "مكتمل (${dhikr.targetCount}/${dhikr.targetCount})", "Completed (${dhikr.targetCount}/${dhikr.targetCount})")
                    } else {
                        L10n.pick(lang, "اضغط للذكر · المتبقي $remaining (${currentCount}/${dhikr.targetCount})", "Tap · $remaining left ($currentCount/${dhikr.targetCount})")
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isCompleted && dhikr.targetCount > 1) {
                IconButton(
                    onClick = onMarkDone,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldPrimary.copy(alpha = 0.25f))
                ) {
                    Icon(
                        Icons.Rounded.DoneAll,
                        contentDescription = L10n.pick(lang, "تحديد كمكتمل", "Mark completed"),
                        tint = EmeraldDeep
                    )
                }
            }
        }
    }
}
