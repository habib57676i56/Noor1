package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveRecitationStatus
import com.example.audio.RecitationRecordingStatus
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.model.QuranVerse
import com.example.domain.quran.RecitationOverallLevel
import com.example.domain.quran.WordEvaluationStatus
import com.example.ui.components.LiquidGlassCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

/**
 * Integrated In-Reader "My Recitation & Memorization" Panel.
 *
 * Distinct from Sheikh Listening Mode:
 * 1. Real Android `MediaRecorder` M4A Voice Recording (Start / Pause / Resume / Stop / Play / Delete).
 * 2. Real Arabic Speech-to-Text (`SpeechRecognizer` live stream + 1-tap Android `RecognizerIntent`
 *    voice dictation fallback) displaying live recognized Arabic text inside a dedicated text box.
 * 3. Real Word-by-Word Memorization Assessment (`QuranRecitationAssessmentEngine`) comparing the
 *    recognized Arabic recitation against the verified Uthmani Quran text (omissions, additions,
 *    repetitions, substitutions, skipped Ayahs, and honest uncertain-confidence indicator).
 * 4. Optional "Hide Verses (Test Mode)" to recite from memory before revealing and evaluating.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InReaderRecitationPanel(
    surahNumber: Int,
    verses: List<QuranVerse>,
    initialAyah: Int,
    ecosystemViewModel: EcosystemViewModel,
    onOpenFullHifzStudio: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val liveState by ecosystemViewModel.hifzLiveState.collectAsStateWithLifecycle()
    val recordingState by ecosystemViewModel.recordingState.collectAsStateWithLifecycle()
    val playbackState by ecosystemViewModel.recordingPlaybackState.collectAsStateWithLifecycle()
    val report by ecosystemViewModel.recitationReport.collectAsStateWithLifecycle()
    val savedRecordings by ecosystemViewModel.recitationRecordings.collectAsStateWithLifecycle()

    val surahMeta = remember(surahNumber) { SurahCatalog.findSurah(surahNumber) }
    val maxAyah = remember(verses, surahMeta) {
        verses.maxOfOrNull { it.numberInSurah } ?: surahMeta.verseCount
    }

    var fromAyah by remember(surahNumber, initialAyah) {
        mutableIntStateOf(initialAyah.coerceIn(1, maxAyah.coerceAtLeast(1)))
    }
    var toAyah by remember(surahNumber, initialAyah) {
        mutableIntStateOf((initialAyah + 2).coerceIn(fromAyah, maxAyah.coerceAtLeast(1)))
    }
    var hideTargetText by remember { mutableStateOf(false) }
    var pendingMicAction by remember { mutableStateOf<String?>(null) }

    val selectedVerses = remember(verses, fromAyah, toAyah) {
        verses.filter { it.numberInSurah in fromAyah..toAyah }
    }

    LaunchedEffect(surahNumber, verses.size) {
        if (verses.isNotEmpty()) {
            ecosystemViewModel.loadHifzSurah(surahNumber)
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
            val spoken = matches.firstOrNull { it.isNotBlank() }
            if (!spoken.isNullOrBlank()) {
                ecosystemViewModel.injectHifzSpeechResult(spoken)
                if (selectedVerses.isNotEmpty()) {
                    val combined = ecosystemViewModel.hifzLiveState.value.effectiveTranscript.ifBlank { spoken }
                    ecosystemViewModel.evaluateRecitedVerses(selectedVerses, combined, testMode = "IN_READER_SPEECH")
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingMicAction) {
                "RECORD" -> ecosystemViewModel.startRecitationRecording(surahNumber, fromAyah)
                "STT" -> ecosystemViewModel.startHifzLiveMic(resetTranscript = false)
            }
        }
        pendingMicAction = null
    }

    fun requireMicThen(actionKey: String, block: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            block()
        } else {
            pendingMicAction = actionKey
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val latestForSurah = remember(savedRecordings, surahNumber) {
        savedRecordings.firstOrNull { it.surahNumber == surahNumber }
    }

    LiquidGlassCard(
        goldBorder = true,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("in_reader_recitation_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "تسميعي وتحفيظي · سورة ${surahMeta.nameAr}",
                            "My Recitation & Memorization · ${surahMeta.nameEn}"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "سجّل صوتك أو حوّل تلاوتك إلى نص عربي وقيّم حفظك كلمةً بكلمة",
                            "Record your voice or convert Arabic recitation to text & evaluate word-by-word"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onOpenFullHifzStudio(surahNumber, fromAyah) },
                        modifier = Modifier.size(40.dp).testTag("open_full_hifz_studio_btn")
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = L10n.pick(lang, "فتح مركز الحفظ الكامل", "Open Full Hifz Studio"),
                            tint = GoldPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            ecosystemViewModel.stopHifzLiveMic(clearTranscript = false)
                            onClose()
                        },
                        modifier = Modifier.size(40.dp).testTag("close_recitation_panel_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = L10n.pick(lang, "إغلاق لوحة التسميع", "Close Recitation Panel"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Ayah Range Selector + Hide Verses (Test Mode) toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = L10n.pick(lang, "النطاق:", "Range:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            if (fromAyah > 1) {
                                fromAyah -= 1
                                if (toAyah < fromAyah) toAyah = fromAyah
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) { Text("-") }
                    Text(
                        text = "$fromAyah–$toAyah",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    OutlinedButton(
                        onClick = {
                            if (toAyah < maxAyah) {
                                toAyah += 1
                            } else if (fromAyah < maxAyah) {
                                fromAyah += 1
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) { Text("+") }
                }

                OutlinedButton(
                    onClick = { hideTargetText = !hideTargetText },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("in_reader_toggle_hide_verses_btn")
                ) {
                    Icon(
                        imageVector = if (hideTargetText) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GoldPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hideTargetText) L10n.pick(lang, "إظهار", "Reveal") else L10n.pick(lang, "إخفاء للاختبار", "Hide to Test"),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Target Passage Preview (hidden during memory test mode)
            if (selectedVerses.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    if (hideTargetText) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "وضع التسميع غيباً: الآيات ($fromAyah–$toAyah) مخفية الآن. اتلُ من حفظك ثم اضغط «تقييم التلاوة».",
                                "Blind Memorization Test: Ayahs $fromAyah–$toAyah hidden. Recite from memory then tap Evaluate."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = noorColors.goldAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = selectedVerses.joinToString("  ") { "${it.textUthmani} ﴿${it.numberInSurah}﴾" },
                            fontFamily = AmiriFontFamily,
                            fontSize = 18.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // SECTION A: Real Voice Recording (MediaRecorder M4A)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(lang, "١. تسجيل صوتي لتلاوتك (M4A)", "1. Voice Recorder (Real M4A)"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (recordingState.isRecordingOrPaused) {
                            val secs = (recordingState.elapsedMs / 1000L).toInt()
                            Text(
                                text = String.format("%02d:%02d", secs / 60, secs % 60),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                modifier = Modifier.testTag("in_reader_record_timer")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!recordingState.isRecordingOrPaused) {
                            Button(
                                onClick = {
                                    requireMicThen("RECORD") {
                                        ecosystemViewModel.startRecitationRecording(surahNumber, fromAyah)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("in_reader_start_record_btn")
                            ) {
                                Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(L10n.pick(lang, "تسجيل صوتي", "Record Voice"), maxLines = 1)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    if (recordingState.status == RecitationRecordingStatus.RECORDING) {
                                        ecosystemViewModel.pauseRecitationRecording()
                                    } else {
                                        ecosystemViewModel.resumeRecitationRecording()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (recordingState.status == RecitationRecordingStatus.RECORDING) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (recordingState.status == RecitationRecordingStatus.RECORDING) L10n.pick(lang, "إيقاف مؤقت", "Pause")
                                    else L10n.pick(lang, "استئناف", "Resume")
                                )
                            }
                            Button(
                                onClick = {
                                    ecosystemViewModel.stopRecitationRecording(
                                        surahNumber = surahNumber,
                                        fromAyah = fromAyah,
                                        toAyah = toAyah,
                                        testMode = if (hideTargetText) "BLIND_READER" else "IN_READER"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("in_reader_stop_record_btn")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "إيقاف وحفظ", "Stop & Save"))
                            }
                        }
                    }

                    // Live Microphone Amplitude Bar — ONLY active when MediaRecorder is genuinely recording!
                    if (recordingState.status == RecitationRecordingStatus.RECORDING) {
                        LinearProgressIndicator(
                            progress = { recordingState.amplitudeNormalized.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFE53935),
                            trackColor = Color(0xFFE53935).copy(alpha = 0.2f)
                        )
                    }

                    val recErr = L10n.pick(lang, recordingState.errorAr.orEmpty(), recordingState.errorEn.orEmpty())
                    if (recErr.isNotBlank()) {
                        Text(
                            text = recErr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Immediate Playback & Delete for Latest Recording
                    val savedPath = recordingState.lastSavedFilePath ?: latestForSurah?.filePath
                    if (!savedPath.isNullOrBlank()) {
                        val isPlayingThis = playbackState.isPlaying && playbackState.playingFilePath == savedPath
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isPlayingThis) {
                                        ecosystemViewModel.pauseSavedRecording()
                                    } else {
                                        ecosystemViewModel.playSavedRecording(savedPath)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("in_reader_play_recording_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isPlayingThis) L10n.pick(lang, "إيقاف التشغيل", "Pause Recording")
                                    else L10n.pick(lang, "استماع لتسجيلي", "Play My Recording")
                                )
                            }
                            if (latestForSurah != null && latestForSurah.filePath == savedPath) {
                                IconButton(
                                    onClick = { ecosystemViewModel.deleteSavedRecording(latestForSurah) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = L10n.pick(lang, "حذف التسجيل", "Delete Recording"),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION B: Real Arabic Speech-to-Text + Live Recognized Arabic Text Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = L10n.pick(lang, "٢. تحويل تلاوتك إلى نص عربي وتقييم الحفظ", "2. Arabic Speech-to-Text & Memorization Check"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isSttActive = liveState.isActive
                        Button(
                            onClick = {
                                if (isSttActive) {
                                    ecosystemViewModel.stopHifzLiveMic(clearTranscript = false)
                                    val spoken = ecosystemViewModel.hifzLiveState.value.effectiveTranscript
                                    if (spoken.isNotBlank() && selectedVerses.isNotEmpty()) {
                                        ecosystemViewModel.evaluateRecitedVerses(selectedVerses, spoken, testMode = "IN_READER_LIVE_STT")
                                    }
                                } else {
                                    requireMicThen("STT") {
                                        ecosystemViewModel.startHifzLiveMic(resetTranscript = false)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSttActive) GoldPrimary else EmeraldPrimary,
                                contentColor = if (isSttActive) Color(0xFF071B14) else Color.White
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .heightIn(min = 48.dp)
                                .testTag("in_reader_start_stt_btn")
                        ) {
                            Icon(
                                imageVector = if (isSttActive) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSttActive) L10n.pick(lang, "إيقاف وتقييم", "Stop & Evaluate")
                                else L10n.pick(lang, "ابدأ التسميع", "Start Reciting"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    systemSpeechLauncher.launch(ecosystemViewModel.buildSystemSpeechIntent())
                                }
                            },
                            modifier = Modifier
                                .weight(0.95f)
                                .heightIn(min = 48.dp)
                                .testTag("in_reader_system_dictation_btn")
                        ) {
                            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = L10n.pick(lang, "إملاء النظام", "Voice Input"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val sttStatus = L10n.pick(lang, liveState.statusNoteAr, liveState.statusNoteEn)
                    if (sttStatus.isNotBlank()) {
                        Text(
                            text = sttStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (liveState.status == LiveRecitationStatus.ERROR) MaterialTheme.colorScheme.error else noorColors.goldAccent
                        )
                    }

                    // Dedicated Visible Recognized Arabic Text Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("in_reader_recognized_arabic_text_box")
                    ) {
                        val recognized = liveState.effectiveTranscript
                        Text(
                            text = recognized.ifBlank {
                                L10n.pick(
                                    lang,
                                    "النص العربي المُتعرَّف عليه من صوتك سيظهر هنا مباشرة أثناء التلاوة...",
                                    "Your recognized Arabic recitation text will appear here live..."
                                )
                            },
                            fontFamily = if (recognized.isNotBlank()) AmiriFontFamily else null,
                            fontSize = if (recognized.isNotBlank()) 18.sp else 13.sp,
                            color = if (recognized.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Evaluate & Retry Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val spoken = liveState.effectiveTranscript
                                if (spoken.isNotBlank() && selectedVerses.isNotEmpty()) {
                                    ecosystemViewModel.evaluateRecitedVerses(selectedVerses, spoken, testMode = "IN_READER_EVAL")
                                }
                            },
                            enabled = liveState.effectiveTranscript.isNotBlank() && selectedVerses.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep, contentColor = GoldBright),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("in_reader_evaluate_btn")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "تقييم التلاوة", "Evaluate"))
                        }
                        TextButton(
                            onClick = {
                                ecosystemViewModel.clearHifzTranscript()
                                ecosystemViewModel.clearRecitationReport()
                            },
                            modifier = Modifier.testTag("in_reader_retry_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(L10n.pick(lang, "إعادة المحاولة", "Retry"))
                        }
                    }
                }
            }

            // SECTION C: Real Word-by-Word Assessment Result (when evaluated)
            report?.let { r ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("in_reader_assessment_result")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val totalExpectedWords = remember(r) {
                            r.wordEvaluations.count { it.expectedWord != null }.coerceAtLeast(1)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "نتيجة التقييم: ${r.accuracyPercent}% (${r.correctCount}/$totalExpectedWords كلمة)",
                                    "Assessment: ${r.accuracyPercent}% (${r.correctCount}/$totalExpectedWords words)"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (r.overallLevel == RecitationOverallLevel.EXCELLENT) SemanticSuccess else noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, r.overallLevel.labelAr, r.overallLevel.labelEn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Color-coded word chips: Green = matched, Red = omitted/replaced, Amber = uncertain
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            r.wordEvaluations.take(40).forEach { w ->
                                val chipBg = when (w.status) {
                                    WordEvaluationStatus.CORRECT -> SemanticSuccess.copy(alpha = 0.22f)
                                    WordEvaluationStatus.OMITTED, WordEvaluationStatus.REPLACED -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
                                    WordEvaluationStatus.PHONETIC_UNCERTAIN -> GoldPrimary.copy(alpha = 0.28f)
                                    WordEvaluationStatus.ADDED, WordEvaluationStatus.REPEATED -> Color(0xFF8E24AA).copy(alpha = 0.22f)
                                }
                                Text(
                                    text = w.expectedWord?.takeIf { it.isNotBlank() } ?: w.spokenWord.orEmpty(),
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(chipBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (r.omittedWords.isNotEmpty()) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "كلمات ناقصة: ${r.omittedWords.take(6).joinToString(" ، ")}",
                                    "Omitted: ${r.omittedWords.take(6).joinToString(", ")}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (r.recognitionUncertainWords.isNotEmpty()) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "ملاحظة: بعض الكلمات (${r.recognitionUncertainWords.size}) غير مؤكدة بسبب التعرّف الصوتي وليست بالضرورة خطأ حفظ.",
                                    "Note: ${r.recognitionUncertainWords.size} word(s) had low speech-recognition confidence (not necessarily a memorization mistake)."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
