package com.example.ui.screens.ecosystem

import android.Manifest
import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveRecitationStatus
import com.example.audio.RecitationRecordingStatus
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.quran.RecitationAssessmentReport
import com.example.domain.quran.RecitationOverallLevel
import com.example.domain.quran.WordEvaluationStatus
import com.example.ui.components.ChooseReciterButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
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
import com.example.ui.theme.SemanticWarning
import com.example.viewmodel.EcosystemViewModel

private enum class HifzTestMode(val key: String, val labelAr: String, val labelEn: String) {
    HIDDEN_TEXT("HIDDEN_TEXT", "إخفاء النص", "Hidden Text"),
    COMPLETE_MISSING("COMPLETE_MISSING", "إكمال الآية", "Complete Verse"),
    CONTINUE_FROM_VERSE("CONTINUE_FROM_VERSE", "متابعة من آية", "Continue From"),
    RANDOM_VERSE("RANDOM_VERSE", "آية عشوائية", "Random Verse"),
    FULL_SURAH("FULL_SURAH", "السورة كاملة", "Full Surah"),
    DAILY_TEST("DAILY_TEST", "الورد اليومي", "Daily Test"),
    REVISION_TEST("REVISION_TEST", "اختبار مراجعة", "Revision Test"),
    MISTAKE_REVISION("MISTAKE_REVISION", "مراجعة الأخطاء", "Mistake Review")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HifzAiScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    globalConfig: GlobalAppConfigDoc = GlobalAppConfigDoc(),
    initialSurah: Int = 1,
    initialAyah: Int = 1,
    onOpenMushaf: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val verses by viewModel.hifzVerses.collectAsStateWithLifecycle()
    val liveState by viewModel.hifzLiveState.collectAsStateWithLifecycle()
    val recState by viewModel.recordingState.collectAsStateWithLifecycle()
    val playbackState by viewModel.recordingPlaybackState.collectAsStateWithLifecycle()
    val audioState by viewModel.quranAudioState.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val report by viewModel.recitationReport.collectAsStateWithLifecycle()
    val plans by viewModel.hifzPlans.collectAsStateWithLifecycle()
    val logs by viewModel.hifzRevisionLogs.collectAsStateWithLifecycle()
    val weakVerses by viewModel.hifzWeakVerses.collectAsStateWithLifecycle()
    val recordings by viewModel.recitationRecordings.collectAsStateWithLifecycle()

    val clampedInitialSurah = initialSurah.coerceIn(1, 114)
    var selectedSurah by remember(clampedInitialSurah) { mutableIntStateOf(clampedInitialSurah) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeAyahIdx by remember(clampedInitialSurah, initialAyah) { mutableIntStateOf((initialAyah - 1).coerceAtLeast(0)) }
    var pendingInitialAyah by remember(clampedInitialSurah, initialAyah) { mutableStateOf<Int?>(initialAyah.coerceAtLeast(1)) }
    var hideVerseText by remember { mutableStateOf(true) }
    var showTafsirCard by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualReciteInput by remember { mutableStateOf("") }
    var showPlanDialog by remember { mutableStateOf(false) }
    var showSurahPicker by remember { mutableStateOf(false) }
    var showMoreToolsDialog by remember { mutableStateOf(false) }
    var showTestModeDialog by remember { mutableStateOf(false) }
    var selectedTestMode by remember { mutableStateOf(HifzTestMode.HIDDEN_TEXT) }
    var pendingMicAction by remember { mutableStateOf("STT") } // "STT" or "RECORD"

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val ayahNum = verses.getOrNull(activeAyahIdx)?.numberInSurah ?: 1
            if (pendingMicAction == "RECORD") {
                viewModel.startRecitationRecording(selectedSurah, ayahNum)
            } else {
                viewModel.startHifzLiveMic(resetTranscript = false)
            }
        }
    }

    val systemSpeechFallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                viewModel.injectHifzSpeechResult(spoken)
                manualReciteInput = spoken
                val target = verses.getOrNull(activeAyahIdx)
                if (target != null) {
                    viewModel.evaluateRecitedVerses(listOf(target), spoken, testMode = selectedTestMode.key)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopHifzLiveMic(clearTranscript = false)
            viewModel.stopSavedRecording()
        }
    }

    LaunchedEffect(selectedSurah) {
        viewModel.loadHifzSurah(selectedSurah)
        if (selectedSurah != clampedInitialSurah) {
            pendingInitialAyah = null
            activeAyahIdx = 0
        }
    }

    LaunchedEffect(verses, pendingInitialAyah) {
        val targetAyah = pendingInitialAyah ?: return@LaunchedEffect
        if (verses.isNotEmpty() && verses.first().surahNumber == selectedSurah) {
            val idx = verses.indexOfFirst { it.numberInSurah == targetAyah }
            if (idx >= 0) {
                activeAyahIdx = idx
            }
            pendingInitialAyah = null
        }
    }

    val activeVerse = verses.getOrNull(activeAyahIdx.coerceIn(0, (verses.size - 1).coerceAtLeast(0)))
    val currentSurahMeta = remember(selectedSurah) { SurahCatalog.findSurah(selectedSurah) }

    LaunchedEffect(liveState.effectiveTranscript, activeVerse?.numberInSurah, liveState.isActive) {
        val spoken = liveState.effectiveTranscript
        if (spoken.isNotBlank() && activeVerse != null && liveState.isActive) {
            viewModel.evaluateRecitedVerses(listOf(activeVerse), spoken, testMode = selectedTestMode.key)
        }
    }

    val tabs = listOf(
        L10n.pick(lang, "التسميع", "Recite"),
        L10n.pick(lang, "الاختبار", "Test"),
        L10n.pick(lang, "التقدم", "Progress")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الحفظ والتسميع", "Quran Memorization"),
            subtitle = L10n.pick(lang, "واجهة هادئة للحفظ والتسميع والمراجعة", "Calm space for memorization, recitation & review"),
            onBack = onBack
        )

        if (!globalConfig.featureMemorizationEnabled) {
            LiquidGlassCard(
                modifier = Modifier.padding(16.dp),
                goldBorder = true
            ) {
                Text(
                    text = L10n.pick(
                        lang,
                        "قسم الحفظ والمراجعة متوقف مؤقتاً من إعدادات المشرف الأعلى.",
                        "Quran Memorization is temporarily paused by Super Admin settings."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return
        }

        // Compact Top Controls: Surah Picker + ONE unified "Choose Reciter" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showSurahPicker = true },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("hifz_surah_picker_btn"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = L10n.pick(lang, "اختر السورة", "Choose Surah"),
                    tint = noorColors.goldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.pick(lang, "السورة", "Surah"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "${currentSurahMeta.number}. ${L10n.pick(lang, currentSurahMeta.nameAr, currentSurahMeta.nameEn)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = noorColors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            ChooseReciterButton(
                selectedReciter = selectedReciter,
                onSelectReciter = { viewModel.selectReciter(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Clean 3-Section Switcher + Organized "More" Tools Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { idx, label ->
                LiquidGlassChip(
                    label = label,
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hifz_tab_$idx")
                )
            }
            OutlinedButton(
                onClick = { showMoreToolsDialog = true },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("hifz_more_options_btn"),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = L10n.pick(lang, "المزيد من الأدوات", "More Tools"),
                    tint = noorColors.goldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // 0: Clean Recitation & Memorization Primary View
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            if (activeVerse != null) {
                                // Header row: Ayah position + Show/Hide text toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = L10n.pick(
                                            lang,
                                            "سورة ${currentSurahMeta.nameAr} · آية ${activeVerse.numberInSurah} / ${verses.size}",
                                            "${currentSurahMeta.nameEn} · Ayah ${activeVerse.numberInSurah} / ${verses.size}"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = noorColors.goldAccent
                                    )
                                    IconButton(
                                        onClick = { hideVerseText = !hideVerseText },
                                        modifier = Modifier.testTag("hifz_toggle_hide_text_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (hideVerseText) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = L10n.pick(lang, "إظهار أو إخفاء الآية", "Show or hide Ayah")
                                        )
                                    }
                                }

                                WarmIvoryParchmentCard {
                                    Text(
                                        text = if (hideVerseText) {
                                            L10n.pick(
                                                lang,
                                                "••••••••••••••\n(النص مخفي لاختبار الحفظ — اضغط العين للإظهار)",
                                                "••••••••••••••\n(Verse hidden — tap the eye icon to reveal)"
                                            )
                                        } else {
                                            activeVerse.textUthmani
                                        },
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        lineHeight = 36.sp,
                                        color = IvoryInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Optional Tafsir View (toggled from More Options)
                                AnimatedVisibility(visible = showTafsirCard && activeVerse.tafsirAr.isNotBlank()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Text(
                                            text = L10n.pick(lang, "التفسير الميسر", "Tafsir"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = activeVerse.tafsirAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // 3 Clean Primary Actions: Listen to Sheikh | Record | Live Recite
                                val isPlayingThisVerse = audioState.isPlaying &&
                                    audioState.playingSurah == activeVerse.surahNumber &&
                                    audioState.playingAyah == activeVerse.numberInSurah
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1) Listen to Selected Sheikh
                                    Button(
                                        onClick = {
                                            if (isPlayingThisVerse) {
                                                viewModel.stopAudio()
                                            } else {
                                                viewModel.playReferenceAyah(
                                                    surahNumber = activeVerse.surahNumber,
                                                    ayahNumber = activeVerse.numberInSurah,
                                                    explicitReciter = selectedReciter
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("hifz_listen_sheikh_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThisVerse) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isPlayingThisVerse) {
                                                L10n.pick(lang, "إيقاف", "Stop")
                                            } else {
                                                L10n.pick(lang, "استماع", "Listen")
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // 2) Record Voice (Real M4A)
                                    if (!recState.isRecordingOrPaused) {
                                        OutlinedButton(
                                            onClick = {
                                                pendingMicAction = "RECORD"
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("hifz_record_start_btn"),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FiberManualRecord,
                                                contentDescription = null,
                                                tint = SemanticError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = L10n.pick(lang, "تسجيل", "Record"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.stopRecitationRecording(
                                                    surahNumber = selectedSurah,
                                                    fromAyah = activeVerse.numberInSurah,
                                                    toAyah = activeVerse.numberInSurah,
                                                    testMode = selectedTestMode.key
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("hifz_record_stop_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SemanticError, contentColor = Color.White),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(L10n.pick(lang, "حفظ", "Save"), maxLines = 1)
                                        }
                                    }

                                    // 3) Live Recite & Evaluate (Arabic STT)
                                    if (!liveState.isActive && liveState.status != LiveRecitationStatus.PAUSED) {
                                        Button(
                                            onClick = {
                                                pendingMicAction = "STT"
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("hifz_start_mic_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = L10n.pick(lang, "تسميع", "Recite"),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.stopHifzLiveMic(clearTranscript = false) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("hifz_stop_mic_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = EmeraldDeep),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(L10n.pick(lang, "إنهاء", "Stop"), fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }

                                // Live Recording Status (only shown while recording)
                                if (recState.isRecordingOrPaused) {
                                    Spacer(Modifier.height(8.dp))
                                    val sec = recState.elapsedMs / 1000
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "جارٍ التسجيل: %02d:%02d".format(sec / 60, sec % 60),
                                                "Recording: %02d:%02d".format(sec / 60, sec % 60)
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SemanticError,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(
                                            onClick = {
                                                if (recState.status == RecitationRecordingStatus.PAUSED) viewModel.resumeRecitationRecording()
                                                else viewModel.pauseRecitationRecording()
                                            },
                                            modifier = Modifier.testTag("hifz_record_pause_btn")
                                        ) {
                                            Icon(
                                                if (recState.status == RecitationRecordingStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (recState.status == RecitationRecordingStatus.PAUSED) L10n.pick(lang, "استئناف", "Resume") else L10n.pick(lang, "إيقاف مؤقت", "Pause"))
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { recState.amplitudeNormalized },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
                                        color = SemanticError
                                    )
                                }

                                // Live Speech-to-Text Status & Transcript (only shown when active or captured)
                                if (liveState.isActive) {
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { liveState.rmsLevel.coerceIn(0.08f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = GoldBright
                                    )
                                }
                                if (liveState.statusNoteAr.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(L10n.pick(lang, liveState.statusNoteAr, liveState.statusNoteEn), style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                }
                                if (liveState.errorMessageAr.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(L10n.pick(lang, liveState.errorMessageAr, liveState.errorMessageEn), style = MaterialTheme.typography.labelSmall, color = SemanticError)
                                }
                                recState.errorAr?.let { err ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(L10n.pick(lang, err, recState.errorEn ?: err), style = MaterialTheme.typography.labelSmall, color = SemanticError)
                                }

                                if (liveState.effectiveTranscript.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${L10n.pick(lang, "النص الملتقط", "Recognized")}: ${liveState.effectiveTranscript}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = {
                                                viewModel.evaluateRecitedVerses(
                                                    listOf(activeVerse),
                                                    liveState.effectiveTranscript,
                                                    testMode = selectedTestMode.key
                                                )
                                            },
                                            modifier = Modifier.testTag("hifz_evaluate_btn")
                                        ) {
                                            Text(L10n.pick(lang, "تقييم", "Check"))
                                        }
                                    }
                                }

                                // Latest Saved Recording quick play bar (only when a recording was just saved)
                                recState.lastSavedFilePath?.let { savedPath ->
                                    Spacer(Modifier.height(8.dp))
                                    val isPlayingThis = playbackState.isPlaying && playbackState.playingFilePath == savedPath
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "آخر تسجيل (${recState.lastSavedDurationMs / 1000} ث)",
                                                "Last recording (${recState.lastSavedDurationMs / 1000}s)"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SemanticSuccess
                                        )
                                        TextButton(
                                            onClick = {
                                                if (isPlayingThis) viewModel.stopSavedRecording()
                                                else viewModel.playSavedRecording(savedPath)
                                            },
                                            modifier = Modifier.testTag("hifz_play_last_recording_btn")
                                        ) {
                                            Icon(if (isPlayingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isPlayingThis) L10n.pick(lang, "إيقاف", "Stop") else L10n.pick(lang, "استمع لتلاوتك", "Play Recording"))
                                        }
                                    }
                                }

                                // Optional Manual Input Evaluation (toggled from More Options — keeps idle view uncluttered)
                                AnimatedVisibility(visible = showManualInput) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        OutlinedTextField(
                                            value = manualReciteInput,
                                            onValueChange = { manualReciteInput = it },
                                            label = { Text(L10n.pick(lang, "اكتب أو عدّل النص المتلو للتقييم", "Type or edit recited text")) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    val input = manualReciteInput.ifBlank { liveState.effectiveTranscript }
                                                    viewModel.evaluateRecitedVerses(listOf(activeVerse), input, testMode = selectedTestMode.key)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                            ) {
                                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(L10n.pick(lang, "تقييم كلمة بكلمة", "Word-by-Word Check"))
                                            }
                                            TextButton(onClick = {
                                                manualReciteInput = ""
                                                viewModel.clearHifzTranscript()
                                            }) { Text(L10n.pick(lang, "مسح", "Clear")) }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = noorColors.goldAccent.copy(alpha = 0.16f))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { if (activeAyahIdx > 0) activeAyahIdx-- }, enabled = activeAyahIdx > 0) {
                                        Text(L10n.pick(lang, "← الآية السابقة", "← Previous"))
                                    }
                                    TextButton(onClick = { if (activeAyahIdx < verses.lastIndex) activeAyahIdx++ }, enabled = activeAyahIdx < verses.lastIndex) {
                                        Text(L10n.pick(lang, "الآية التالية →", "Next →"))
                                    }
                                }
                            }
                        }
                    }

                    report?.let { r ->
                        item { RecitationAssessmentReportCard(report = r) }
                    }
                }

                // 1: Memorization Test (Clean Single Test-Mode Picker + Drill Card + Weak Verses)
                1 -> {
                    item {
                        OutlinedButton(
                            onClick = { showTestModeDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("hifz_test_mode_picker_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${L10n.pick(lang, "نمط الاختبار", "Test Mode")}: ${L10n.pick(lang, selectedTestMode.labelAr, selectedTestMode.labelEn)}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = noorColors.goldAccent)
                        }
                    }

                    if (verses.isNotEmpty()) {
                        item {
                            var drillIndex by remember(selectedSurah, selectedTestMode) {
                                mutableIntStateOf(
                                    when (selectedTestMode) {
                                        HifzTestMode.RANDOM_VERSE -> verses.indices.random()
                                        HifzTestMode.MISTAKE_REVISION -> {
                                            val weak = weakVerses.firstOrNull { it.surahNumber == selectedSurah && !it.resolved }
                                            verses.indexOfFirst { it.numberInSurah == weak?.ayahNumber }.coerceAtLeast(0)
                                        }
                                        else -> 0
                                    }
                                )
                            }
                            var showAnswer by remember(selectedSurah, selectedTestMode, drillIndex) { mutableStateOf(false) }
                            var testAnswerText by remember(selectedSurah, selectedTestMode, drillIndex) { mutableStateOf("") }

                            val targetVerses = when (selectedTestMode) {
                                HifzTestMode.FULL_SURAH -> verses
                                HifzTestMode.DAILY_TEST -> verses.take(5)
                                HifzTestMode.REVISION_TEST -> verses.drop(drillIndex).take(3).ifEmpty { verses.take(3) }
                                HifzTestMode.CONTINUE_FROM_VERSE -> verses.drop(drillIndex).take(2).ifEmpty { listOf(verses[drillIndex]) }
                                else -> listOf(verses[drillIndex.coerceIn(0, verses.lastIndex)])
                            }
                            val promptVerse = targetVerses.first()
                            val words = promptVerse.textUthmani.split(" ")
                            val promptDisplay = when (selectedTestMode) {
                                HifzTestMode.COMPLETE_MISSING -> "${words.take((words.size / 2).coerceAtLeast(2)).joinToString(" ")} ......."
                                HifzTestMode.CONTINUE_FROM_VERSE -> "${promptVerse.textUthmani}\n← أكمل الآية التالية (${promptVerse.numberInSurah + 1})"
                                HifzTestMode.FULL_SURAH -> "اتلُ سورة ${currentSurahMeta.nameAr} كاملة (${verses.size} آيات)"
                                HifzTestMode.DAILY_TEST -> "اختبار الورد اليومي: الآيات 1 إلى ${targetVerses.last().numberInSurah}"
                                else -> "اتلُ غيباً: سورة ${currentSurahMeta.nameAr} — آية ${promptVerse.numberInSurah}"
                            }

                            LiquidGlassCard(strong = true) {
                                Text(L10n.pick(lang, selectedTestMode.labelAr, selectedTestMode.labelEn), fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                                Spacer(Modifier.height(8.dp))
                                WarmIvoryParchmentCard {
                                    Text(
                                        text = promptDisplay,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 20.sp,
                                        color = IvoryInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (showAnswer) {
                                    Spacer(Modifier.height(8.dp))
                                    targetVerses.forEach { v ->
                                        Text("${v.textUthmani} ﴿${v.numberInSurah}﴾", fontFamily = AmiriFontFamily, fontSize = 18.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = testAnswerText,
                                    onValueChange = { testAnswerText = it },
                                    label = { Text(L10n.pick(lang, "أدخل تلاوتك للتصحيح الفوري", "Enter recitation to evaluate")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val spoken = testAnswerText.ifBlank { liveState.effectiveTranscript }
                                            viewModel.evaluateRecitedVerses(targetVerses, spoken, testMode = selectedTestMode.key)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) { Text(L10n.pick(lang, "تصحيح", "Grade")) }
                                    OutlinedButton(onClick = { showAnswer = !showAnswer }) {
                                        Text(if (showAnswer) L10n.pick(lang, "إخفاء", "Hide") else L10n.pick(lang, "إظهار", "Reveal"))
                                    }
                                    OutlinedButton(onClick = {
                                        drillIndex = if (selectedTestMode == HifzTestMode.RANDOM_VERSE) {
                                            verses.indices.random()
                                        } else {
                                            (drillIndex + 1) % verses.size.coerceAtLeast(1)
                                        }
                                        showAnswer = false
                                        testAnswerText = ""
                                    }) { Text(L10n.pick(lang, "التالي", "Next")) }
                                }
                            }
                        }
                    }

                    report?.let { r -> item { RecitationAssessmentReportCard(report = r) } }

                    if (weakVerses.isNotEmpty()) {
                        item { NoorSectionHeader(title = L10n.pick(lang, "الآيات الأكثر حاجة للمراجعة (${weakVerses.size})", "Weak Verses (${weakVerses.size})")) }
                        items(weakVerses, key = { "weak_${it.id}" }) { w ->
                            val s = SurahCatalog.findSurah(w.surahNumber)
                            LiquidGlassCard {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("سورة ${s.nameAr} — آية ${w.ayahNumber}", fontWeight = FontWeight.Bold)
                                        Text("مرات الخطأ: ${w.missCount} · ${w.lastMistakeNote}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = {
                                        viewModel.playReferenceAyah(w.surahNumber, w.ayahNumber, explicitReciter = selectedReciter)
                                    }) {
                                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Listen")
                                    }
                                    IconButton(onClick = { viewModel.markWeakVerseResolved(w.id, !w.resolved) }) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Resolve", tint = if (w.resolved) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2: Progress Tracking, Memorization Plans & Saved Recordings Archive
                2 -> {
                    item {
                        Button(
                            onClick = { showPlanDialog = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إنشاء جدول حفظ ومراجعة شخصي", "Create Memorization Plan"))
                        }
                    }
                    items(plans, key = { "plan_${it.id}" }) { p ->
                        val surah = SurahCatalog.findSurah(p.targetSurahNumber)
                        val progress = ((p.currentAyah - p.fromAyah + 1).toFloat() / (p.toAyah - p.fromAyah + 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, fontWeight = FontWeight.Bold)
                                    Text("سورة ${surah.nameAr} (${p.fromAyah}–${p.toAyah}) · وصلت إلى آية ${p.currentAyah}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                                }
                                IconButton(onClick = { viewModel.deleteHifzPlan(p.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Plan", tint = SemanticError)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.advanceHifzPlan(p) },
                                enabled = !p.isCompleted,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Text(if (p.isCompleted) "مكتمل بحمد الله ✓" else "تسجيل ورد اليوم (+${p.dailyAyahTarget} آيات)")
                            }
                        }
                    }

                    item { NoorSectionHeader(title = L10n.pick(lang, "أرشيف تسجيلات التلاوة (${recordings.size})", "Saved Recordings (${recordings.size})")) }
                    if (recordings.isEmpty()) {
                        item {
                            LiquidGlassCard {
                                Text(L10n.pick(lang, "لا توجد تسجيلات صوتية محفوظة بعد. استخدم زر «تسجيل» لحفظ تلاوتك.", "No saved recordings yet. Use 'Record' to save your recitation."))
                            }
                        }
                    }
                    items(recordings, key = { "rec_${it.id}" }) { rec ->
                        val s = SurahCatalog.findSurah(rec.surahNumber)
                        val isPlaying = playbackState.isPlaying && playbackState.playingFilePath == rec.filePath
                        LiquidGlassCard(goldBorder = rec.teacherReviewStatus == "APPROVED") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.nameAr} (${rec.fromAyah}–${rec.toAyah}) · ${rec.durationMs / 1000}ث", fontWeight = FontWeight.Bold)
                                    Text("الدقة: ${rec.accuracyPercent}% · الحالة: ${rec.teacherReviewStatus}", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                    if (rec.teacherComment.isNotBlank()) {
                                        Text("ملاحظة المعلم: ${rec.teacherComment}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = {
                                    if (isPlaying) viewModel.stopSavedRecording()
                                    else viewModel.playSavedRecording(rec.filePath)
                                }) {
                                    Icon(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "Play Recording", tint = GoldBright)
                                }
                                IconButton(onClick = {
                                    val nextStatus = if (rec.teacherReviewStatus == "APPROVED") "NEEDS_REVISION" else "APPROVED"
                                    viewModel.reviewStudentRecording(rec.id, nextStatus, rec.teacherComment)
                                }) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Approve Recording",
                                        tint = if (rec.teacherReviewStatus == "APPROVED") SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSavedRecording(rec) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Recording", tint = SemanticError)
                                }
                            }
                        }
                    }

                    item {
                        val avgAcc = if (logs.isEmpty()) 0 else logs.map { it.accuracyPercent }.average().toInt()
                        NoorSectionHeader(title = L10n.pick(lang, "سجل جلسات التسميع (المتوسط $avgAcc%)", "Revision Log (Avg $avgAcc%)"))
                    }
                    items(logs.take(15), key = { "log_${it.id}" }) { log ->
                        val s = SurahCatalog.findSurah(log.surahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.nameAr} (${log.fromAyah}–${log.toAyah})", fontWeight = FontWeight.Bold)
                                    Text("${log.dateKey} · الدقة: ${log.accuracyPercent}% · ${log.sessionType}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                                }
                                TextButton(onClick = { viewModel.approveHifzLog(log.id, !log.teacherApproved) }) {
                                    Text(if (log.teacherApproved) "معتمد ✓" else "اعتماد", color = if (log.teacherApproved) SemanticSuccess else noorColors.goldAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Clean Surah Picker Dialog (replaces 12 cluttered horizontal chips!)
    if (showSurahPicker) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query) {
            val q = query.trim()
            if (q.isBlank()) SurahCatalog.allSurahs
            else SurahCatalog.allSurahs.filter {
                it.nameAr.contains(q, ignoreCase = true) ||
                    it.nameEn.contains(q, ignoreCase = true) ||
                    it.number.toString() == q
            }
        }
        AlertDialog(
            onDismissRequest = { showSurahPicker = false },
            modifier = Modifier.testTag("hifz_surah_picker_dialog"),
            title = { Text(L10n.pick(lang, "اختر السورة", "Choose Surah"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(L10n.pick(lang, "ابحث باسم السورة أو رقمها", "Search Surah name or number")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered, key = { it.number }) { s ->
                            val isSel = s.number == selectedSurah
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedSurah = s.number
                                        showSurahPicker = false
                                    },
                                color = if (isSel) GoldPrimary.copy(alpha = 0.16f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${s.number}. ${L10n.pick(lang, s.nameAr, s.nameEn)}", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                    if (isSel) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSurahPicker = false }) { Text(L10n.pick(lang, "إغلاق", "Close")) } }
        )
    }

    // Organized Secondary Tools Menu (Mushaf View, Tafsir, System Voice Dictation, Manual Input)
    if (showMoreToolsDialog) {
        AlertDialog(
            onDismissRequest = { showMoreToolsDialog = false },
            modifier = Modifier.testTag("hifz_more_options_dialog"),
            title = { Text(L10n.pick(lang, "أدوات الحفظ الإضافية", "More Memorization Tools"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showMoreToolsDialog = false
                            onOpenMushaf(selectedSurah)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hifz_open_mushaf_btn")
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(L10n.pick(lang, "فتح السورة في المصحف", "Open Surah in Mushaf"), modifier = Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = {
                            showTafsirCard = !showTafsirCard
                            selectedTab = 0
                            showMoreToolsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hifz_toggle_tafsir_btn")
                    ) {
                        Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showTafsirCard) L10n.pick(lang, "إخفاء التفسير", "Hide Tafsir")
                            else L10n.pick(lang, "عرض تفسير الآية", "Show Verse Tafsir"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            showMoreToolsDialog = false
                            runCatching { systemSpeechFallbackLauncher.launch(viewModel.buildSystemSpeechIntent()) }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hifz_system_stt_fallback_btn")
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(L10n.pick(lang, "إملاء صوتي عبر النظام (بديل)", "System Voice Dictation"), modifier = Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = {
                            showManualInput = !showManualInput
                            selectedTab = 0
                            showMoreToolsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hifz_toggle_manual_input_btn")
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showManualInput) L10n.pick(lang, "إخفاء التصحيح اليدوي", "Hide Manual Check")
                            else L10n.pick(lang, "كتابة / تعديل النص للتصحيح", "Manual Text Evaluation"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMoreToolsDialog = false }) { Text(L10n.pick(lang, "إغلاق", "Close")) } }
        )
    }

    // 8 Memorization Test Modes Selector Dialog
    if (showTestModeDialog) {
        AlertDialog(
            onDismissRequest = { showTestModeDialog = false },
            modifier = Modifier.testTag("hifz_test_mode_dialog"),
            title = { Text(L10n.pick(lang, "اختر نمط اختبار الحفظ", "Choose Memorization Test Mode"), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(HifzTestMode.entries, key = { it.key }) { mode ->
                        val isSel = selectedTestMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedTestMode = mode
                                    showTestModeDialog = false
                                }
                                .testTag("hifz_mode_${mode.key.lowercase()}"),
                            color = if (isSel) GoldPrimary.copy(alpha = 0.16f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(L10n.pick(lang, mode.labelAr, mode.labelEn), fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                if (isSel) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTestModeDialog = false }) { Text(L10n.pick(lang, "إغلاق", "Close")) } }
        )
    }

    if (showPlanDialog) {
        var title by remember { mutableStateOf("ورد حفظ جزء عمّ") }
        var surahText by remember { mutableStateOf(selectedSurah.toString()) }
        var fromText by remember { mutableStateOf("1") }
        var toText by remember { mutableStateOf("15") }
        var dailyText by remember { mutableStateOf("5") }
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("جدول حفظ ومراجعة جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الخطة") }, singleLine = true)
                    OutlinedTextField(value = surahText, onValueChange = { surahText = it.filter(Char::isDigit) }, label = { Text("رقم السورة (1-114)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fromText, onValueChange = { fromText = it.filter(Char::isDigit) }, label = { Text("من آية") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = toText, onValueChange = { toText = it.filter(Char::isDigit) }, label = { Text("إلى آية") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = dailyText, onValueChange = { dailyText = it.filter(Char::isDigit) }, label = { Text("الهدف اليومي (آيات)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveHifzPlan(
                        title,
                        surahText.toIntOrNull() ?: 1,
                        fromText.toIntOrNull() ?: 1,
                        toText.toIntOrNull() ?: 10,
                        dailyText.toIntOrNull() ?: 5
                    )
                    showPlanDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showPlanDialog = false }) { Text("إلغاء") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecitationAssessmentReportCard(report: RecitationAssessmentReport) {
    val lang = LocalAppLanguage.current
    val levelColor = when (report.overallLevel) {
        RecitationOverallLevel.EXCELLENT -> SemanticSuccess
        RecitationOverallLevel.GOOD_REVIEW -> GoldPrimary
        RecitationOverallLevel.NEEDS_REVISION -> SemanticWarning
        RecitationOverallLevel.RECOGNITION_UNCLEAR -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassCard(strong = true, goldBorder = report.overallLevel == RecitationOverallLevel.EXCELLENT) {
        Text(
            text = "${L10n.pick(lang, report.overallLevel.labelAr, report.overallLevel.labelEn)} — ${report.accuracyPercent}%",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = levelColor
        )
        Spacer(Modifier.height(4.dp))
        Text(L10n.pick(lang, report.summaryAr, report.summaryEn), style = MaterialTheme.typography.bodySmall)

        if (report.wordEvaluations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                report.wordEvaluations.take(40).forEach { w ->
                    val (bg, borderCol, label) = when (w.status) {
                        WordEvaluationStatus.CORRECT -> Triple(SemanticSuccess.copy(alpha = 0.16f), SemanticSuccess, w.expectedWord.orEmpty())
                        WordEvaluationStatus.OMITTED -> Triple(SemanticError.copy(alpha = 0.16f), SemanticError, "ناقص: ${w.expectedWord}")
                        WordEvaluationStatus.REPLACED -> Triple(SemanticError.copy(alpha = 0.16f), SemanticError, "${w.expectedWord} ← ${w.spokenWord}")
                        WordEvaluationStatus.ADDED -> Triple(SemanticWarning.copy(alpha = 0.16f), SemanticWarning, "زائد: ${w.spokenWord}")
                        WordEvaluationStatus.REPEATED -> Triple(SemanticWarning.copy(alpha = 0.16f), SemanticWarning, "مكرر: ${w.spokenWord}")
                        WordEvaluationStatus.PHONETIC_UNCERTAIN -> Triple(GoldPrimary.copy(alpha = 0.18f), GoldPrimary, "فرق صوتي: ${w.expectedWord}")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = L10n.pick(
                lang,
                "ملاحظة: الفروق الصوتية المصنّفة بـ «فرق صوتي» قد تكون ناتجة عن التعرف الآلي وليست خطأً جازماً — يُنصح بمراجعة المعلم للتثبت.",
                "Note: Items marked 'Phonetic Uncertain' may stem from ASR variance and are not confirmed Quranic errors — teacher verification is recommended."
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
