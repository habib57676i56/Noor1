package com.example.ui.screens.ecosystem

import android.Manifest
import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveRecitationStatus
import com.example.audio.RecitationRecordingStatus
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.quran.RecitationAssessmentReport
import com.example.domain.quran.RecitationOverallLevel
import com.example.domain.quran.WordEvaluationStatus
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.data.cloud.GlobalAppConfigDoc
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
    globalConfig: GlobalAppConfigDoc = GlobalAppConfigDoc()
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val verses by viewModel.hifzVerses.collectAsStateWithLifecycle()
    val liveState by viewModel.hifzLiveState.collectAsStateWithLifecycle()
    val recState by viewModel.recordingState.collectAsStateWithLifecycle()
    val playbackState by viewModel.recordingPlaybackState.collectAsStateWithLifecycle()
    val report by viewModel.recitationReport.collectAsStateWithLifecycle()
    val plans by viewModel.hifzPlans.collectAsStateWithLifecycle()
    val logs by viewModel.hifzRevisionLogs.collectAsStateWithLifecycle()
    val weakVerses by viewModel.hifzWeakVerses.collectAsStateWithLifecycle()
    val recordings by viewModel.recitationRecordings.collectAsStateWithLifecycle()

    val quickSurahs = remember { listOf(1, 93, 94, 97, 103, 108, 109, 110, 111, 112, 113, 114) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeAyahIdx by remember { mutableIntStateOf(0) }
    var hideVerseText by remember { mutableStateOf(true) }
    var manualReciteInput by remember { mutableStateOf("") }
    var showPlanDialog by remember { mutableStateOf(false) }
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

    // Android System RecognizerIntent Fallback Launcher (works even when OEM blocks background SpeechRecognizer)
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
        activeAyahIdx = 0
    }

    val activeVerse = verses.getOrNull(activeAyahIdx.coerceIn(0, (verses.size - 1).coerceAtLeast(0)))

    // Live compare when recognizer emits transcript
    LaunchedEffect(liveState.effectiveTranscript, activeVerse?.numberInSurah, liveState.isActive) {
        val spoken = liveState.effectiveTranscript
        if (spoken.isNotBlank() && activeVerse != null && liveState.isActive) {
            viewModel.evaluateRecitedVerses(listOf(activeVerse), spoken, testMode = selectedTestMode.key)
        }
    }

    val tabs = listOf(
        L10n.pick(lang, "التسجيل والتسميع", "Record & Recite"),
        L10n.pick(lang, "اختبارات الحفظ (8)", "8 Hifz Tests"),
        L10n.pick(lang, "التسجيلات والخطط", "Archive & Plans")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "نظام الحفظ والتسميع الذكي", "NOOR Quran Hifz System"),
            subtitle = L10n.pick(lang, "تسجيل صوتي حقيقي · تعرف عربي · مقارنة كلمة بكلمة دون تخطئة عشوائية", "Real Audio Recording · Arabic STT · Word-Level Alignment"),
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

        if (!globalConfig.featureListeningAssessmentEnabled) {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = L10n.pick(
                        lang,
                        "تنبيه: التقييم الصوتي المباشر متوقف مؤقتاً من إعدادات الإدارة — يمكنك الاستمرار في الحفظ والمراجعة.",
                        "Note: Live listening assessment is temporarily paused by Admin settings — memorization plans remain available."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.size) { idx ->
                LiquidGlassChip(tabs[idx], selectedTab == idx, { selectedTab = idx }, Modifier.testTag("hifz_tab_$idx"))
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickSurahs) { sNum ->
                val meta = SurahCatalog.findSurah(sNum)
                LiquidGlassChip("${meta.number}. ${meta.nameAr}", selectedSurah == sNum, { selectedSurah = sNum })
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // 0: Real Audio Recording + Repaired Arabic STT + Word-Level Assessment
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = L10n.pick(lang, "١) التسجيل الصوتي الحقيقي للتلاوة (M4A)", "1) Real Audio Recording (M4A)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                IconButton(onClick = { hideVerseText = !hideVerseText }) {
                                    Icon(
                                        if (hideVerseText) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle Verse Visibility"
                                    )
                                }
                            }

                            if (activeVerse != null) {
                                WarmIvoryParchmentCard {
                                    Text(
                                        text = "سورة ${SurahCatalog.findSurah(selectedSurah).nameAr} · آية ${activeVerse.numberInSurah} من ${verses.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (hideVerseText) {
                                            "••••••••••••••••••••••\n(النص مخفي لاختبار الحفظ — اضغط أيقونة العين للمراجعة)"
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

                                Spacer(Modifier.height(10.dp))
                                // MediaRecorder Controls
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!recState.isRecordingOrPaused) {
                                        Button(
                                            onClick = {
                                                pendingMicAction = "RECORD"
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            modifier = Modifier.weight(1f).testTag("hifz_record_start_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SemanticError, contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(L10n.pick(lang, "تسجيل صوتي", "Record Audio"))
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                if (recState.status == RecitationRecordingStatus.PAUSED) viewModel.resumeRecitationRecording()
                                                else viewModel.pauseRecitationRecording()
                                            },
                                            modifier = Modifier.weight(1f).testTag("hifz_record_pause_btn")
                                        ) {
                                            Icon(if (recState.status == RecitationRecordingStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (recState.status == RecitationRecordingStatus.PAUSED) "استئناف" else "إيقاف مؤقت")
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.stopRecitationRecording(
                                                    surahNumber = selectedSurah,
                                                    fromAyah = activeVerse.numberInSurah,
                                                    toAyah = activeVerse.numberInSurah,
                                                    testMode = selectedTestMode.key
                                                )
                                            },
                                            modifier = Modifier.testTag("hifz_record_stop_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                        ) {
                                            Icon(Icons.Rounded.Stop, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("حفظ")
                                        }
                                    }
                                    OutlinedButton(onClick = { viewModel.playReferenceAyah(activeVerse.surahNumber, activeVerse.numberInSurah) }) {
                                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Reference Sheikh")
                                        Spacer(Modifier.width(4.dp))
                                        Text(L10n.pick(lang, "القارئ", "Sheikh"))
                                    }
                                }

                                if (recState.isRecordingOrPaused) {
                                    Spacer(Modifier.height(6.dp))
                                    val sec = recState.elapsedMs / 1000
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
                                    LinearProgressIndicator(
                                        progress = { recState.amplitudeNormalized },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                        color = SemanticError
                                    )
                                }

                                recState.lastSavedFilePath?.let { savedPath ->
                                    Spacer(Modifier.height(6.dp))
                                    val isPlayingThis = playbackState.isPlaying && playbackState.playingFilePath == savedPath
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = "آخر تسجيل محفوظ (${recState.lastSavedDurationMs / 1000} ث · ${recState.lastSavedSizeBytes / 1024} KB)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SemanticSuccess
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                if (isPlayingThis) viewModel.stopSavedRecording()
                                                else viewModel.playSavedRecording(savedPath)
                                            },
                                            modifier = Modifier.testTag("hifz_play_last_recording_btn")
                                        ) {
                                            Icon(if (isPlayingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isPlayingThis) "إيقاف" else "استمع لتلاوتك")
                                        }
                                    }
                                }
                                recState.errorAr?.let { err ->
                                    Text(L10n.pick(lang, err, recState.errorEn ?: err), style = MaterialTheme.typography.labelSmall, color = SemanticError)
                                }

                                Spacer(Modifier.height(10.dp))
                                // Speech-to-Text Controls + System Fallback
                                Text(
                                    text = L10n.pick(lang, "٢) التعرف الصوتي العربي والتصحيح المباشر", "2) Arabic Speech Recognition & Evaluation"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!liveState.isActive && liveState.status != LiveRecitationStatus.PAUSED) {
                                        Button(
                                            onClick = {
                                                pendingMicAction = "STT"
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            modifier = Modifier.weight(1f).testTag("hifz_start_mic_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                        ) {
                                            Icon(Icons.Rounded.Mic, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(L10n.pick(lang, "تسميع مباشر", "Live STT"))
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                if (liveState.status == LiveRecitationStatus.PAUSED) viewModel.resumeHifzLiveMic()
                                                else viewModel.pauseHifzLiveMic()
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(if (liveState.status == LiveRecitationStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (liveState.status == LiveRecitationStatus.PAUSED) "استئناف" else "إيقاف مؤقت")
                                        }
                                        OutlinedButton(onClick = { viewModel.stopHifzLiveMic(clearTranscript = false) }) {
                                            Icon(Icons.Rounded.Stop, contentDescription = "Stop STT")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            runCatching { systemSpeechFallbackLauncher.launch(viewModel.buildSystemSpeechIntent()) }
                                        },
                                        modifier = Modifier.testTag("hifz_system_stt_fallback_btn")
                                    ) {
                                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(L10n.pick(lang, "إملاء بديل", "System Voice"))
                                    }
                                }

                                if (liveState.isActive) {
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { liveState.rmsLevel.coerceIn(0.08f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = GoldBright
                                    )
                                }
                                if (liveState.statusNoteAr.isNotBlank()) {
                                    Text(L10n.pick(lang, liveState.statusNoteAr, liveState.statusNoteEn), style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                }
                                if (liveState.errorMessageAr.isNotBlank()) {
                                    Text(L10n.pick(lang, liveState.errorMessageAr, liveState.errorMessageEn), style = MaterialTheme.typography.labelSmall, color = SemanticError)
                                }
                                if (liveState.effectiveTranscript.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "النص الملتقط: ${liveState.effectiveTranscript}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualReciteInput,
                                    onValueChange = { manualReciteInput = it },
                                    label = { Text(L10n.pick(lang, "أو اكتب / عدّل النص المتلو للتقييم الدقيق", "Or type / edit recited text to evaluate")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val input = manualReciteInput.ifBlank { liveState.effectiveTranscript }
                                            viewModel.evaluateRecitedVerses(listOf(activeVerse), input, testMode = selectedTestMode.key)
                                        },
                                        modifier = Modifier.weight(1f).testTag("hifz_evaluate_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) {
                                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(L10n.pick(lang, "تقييم التلاوة كلمة بكلمة", "Word-by-Word Check"))
                                    }
                                    TextButton(onClick = {
                                        manualReciteInput = ""
                                        viewModel.clearHifzTranscript()
                                    }) { Text("مسح") }
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { if (activeAyahIdx > 0) activeAyahIdx-- }, enabled = activeAyahIdx > 0) {
                                        Text("← الآية السابقة")
                                    }
                                    TextButton(onClick = { if (activeAyahIdx < verses.lastIndex) activeAyahIdx++ }, enabled = activeAyahIdx < verses.lastIndex) {
                                        Text("الآية التالية →")
                                    }
                                }
                            }
                        }
                    }

                    report?.let { r ->
                        item { RecitationAssessmentReportCard(report = r) }
                    }
                }

                // 1: Complete 8 Memorization Test Modes + Weak Verses
                1 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text(
                                text = L10n.pick(lang, "اختر نمط اختبار الحفظ (٨ أنماط حقيقية)", "Choose Memorization Test Mode (8 Modes)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                HifzTestMode.entries.forEach { mode ->
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, mode.labelAr, mode.labelEn),
                                        selected = selectedTestMode == mode,
                                        onClick = { selectedTestMode = mode },
                                        modifier = Modifier.testTag("hifz_mode_${mode.key.lowercase()}")
                                    )
                                }
                            }
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
                                HifzTestMode.FULL_SURAH -> "اتلُ سورة ${SurahCatalog.findSurah(selectedSurah).nameAr} كاملة (${verses.size} آيات)"
                                HifzTestMode.DAILY_TEST -> "اختبار الورد اليومي: الآيات 1 إلى ${targetVerses.last().numberInSurah}"
                                else -> "اتلُ غيباً: سورة ${SurahCatalog.findSurah(selectedSurah).nameAr} — آية ${promptVerse.numberInSurah}"
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
                                    label = { Text("أدخل تلاوتك أو استخدم الإملاء الصوتي للتصحيح الفوري") },
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
                                    ) { Text("تصحيح الاختبار") }
                                    OutlinedButton(onClick = { showAnswer = !showAnswer }) {
                                        Text(if (showAnswer) "إخفاء" else "إظهار")
                                    }
                                    OutlinedButton(onClick = {
                                        drillIndex = if (selectedTestMode == HifzTestMode.RANDOM_VERSE) {
                                            verses.indices.random()
                                        } else {
                                            (drillIndex + 1) % verses.size.coerceAtLeast(1)
                                        }
                                        showAnswer = false
                                        testAnswerText = ""
                                    }) { Text("التالي") }
                                }
                            }
                        }
                    }

                    report?.let { r -> item { RecitationAssessmentReportCard(report = r) } }

                    item { NoorSectionHeader(title = L10n.pick(lang, "الآيات الأكثر حاجة للمراجعة (${weakVerses.size})", "Weak Verses (${weakVerses.size})")) }
                    items(weakVerses, key = { "weak_${it.id}" }) { w ->
                        val s = SurahCatalog.findSurah(w.surahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.nameAr} — آية ${w.ayahNumber}", fontWeight = FontWeight.Bold)
                                    Text("مرات الخطأ: ${w.missCount} · ${w.lastMistakeNote}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.playReferenceAyah(w.surahNumber, w.ayahNumber) }) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Listen")
                                }
                                IconButton(onClick = { viewModel.markWeakVerseResolved(w.id, !w.resolved) }) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Resolve", tint = if (w.resolved) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // 2: Saved Recitation Recordings Archive + Plans + Logs
                2 -> {
                    item { NoorSectionHeader(title = L10n.pick(lang, "أرشيف تسجيلات التلاوة (${recordings.size})", "Saved Recitation Recordings (${recordings.size})")) }
                    if (recordings.isEmpty()) {
                        item {
                            LiquidGlassCard {
                                Text("لا توجد تسجيلات صوتية محفوظة بعد. استخدم زر «تسجيل صوتي» في التبويب الأول لحفظ تلاوتك ومراجعتها.")
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
                        Button(
                            onClick = { showPlanDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إنشاء جدول حفظ ومراجعة شخصي", "Create Memorization Schedule"))
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

                    item {
                        val avgAcc = if (logs.isEmpty()) 0 else logs.map { it.accuracyPercent }.average().toInt()
                        NoorSectionHeader(title = "سجل جلسات التسميع (المتوسط $avgAcc%)")
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
