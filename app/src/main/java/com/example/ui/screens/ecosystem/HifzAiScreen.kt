package com.example.ui.screens.ecosystem

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveRecitationStatus
import com.example.data.local.catalog.SurahCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.viewmodel.EcosystemViewModel

@Composable
fun HifzAiScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val verses by viewModel.hifzVerses.collectAsStateWithLifecycle()
    val liveState by viewModel.hifzLiveState.collectAsStateWithLifecycle()
    val comparison by viewModel.hifzComparison.collectAsStateWithLifecycle()
    val plans by viewModel.hifzPlans.collectAsStateWithLifecycle()
    val logs by viewModel.hifzRevisionLogs.collectAsStateWithLifecycle()
    val weakVerses by viewModel.hifzWeakVerses.collectAsStateWithLifecycle()

    val quickSurahs = remember { listOf(1, 93, 94, 97, 103, 108, 109, 110, 111, 112, 113, 114) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeAyahIdx by remember { mutableIntStateOf(0) }
    var hideVerseText by remember { mutableStateOf(true) }
    var manualReciteInput by remember { mutableStateOf("") }
    var showPlanDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startHifzLiveMic()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopHifzLiveMic() }
    }

    LaunchedEffect(selectedSurah) {
        viewModel.loadHifzSurah(selectedSurah)
        activeAyahIdx = 0
    }

    val activeVerse = verses.getOrNull(activeAyahIdx.coerceIn(0, (verses.size - 1).coerceAtLeast(0)))

    // Live compare when recognizer emits heardText
    LaunchedEffect(liveState.heardText, activeVerse?.numberInSurah) {
        if (liveState.heardText.isNotBlank() && activeVerse != null && liveState.isActive) {
            viewModel.evaluateRecitedVerse(activeVerse, liveState.heardText)
        }
    }

    val tabs = listOf(
        L10n.pick(lang, "التسميع الذكي", "Live Hifz"),
        L10n.pick(lang, "إكمال واختبار", "Drills & Tests"),
        L10n.pick(lang, "الجدول والتقارير", "Plans & Reports")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مساعد الحفظ الذكي", "NOOR Hifz AI"),
            subtitle = L10n.pick(lang, "مقارنة محلية بالتلاوة العثمانية الموثّقة دون تخطئة عشوائية", "On-Device Quran Memorization Assistant"),
            onBack = onBack
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { idx ->
                LiquidGlassChip(tabs[idx], selectedTab == idx, { selectedTab = idx }, Modifier.testTag("hifz_tab_$idx"))
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickSurahs) { sNum ->
                val meta = SurahCatalog.findSurah(sNum)
                LiquidGlassChip(
                    label = "${meta.number}. ${meta.nameAr}",
                    selected = selectedSurah == sNum,
                    onClick = { selectedSurah = sNum }
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
                // 0: Live Mic Hifz Check & Hidden-Text Memorization
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = L10n.pick(lang, "التسميع الصوتي المباشر (محلي)", "On-Device Live Recitation"),
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
                                        text = "آية ${activeVerse.numberInSurah} من ${verses.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (hideVerseText) {
                                            "••••••••••••••••••••••\n(الآية مخفية لاختبار الحفظ — اضغط إظهار للمراجعة)"
                                        } else {
                                            activeVerse.textUthmani
                                        },
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 23.sp,
                                        lineHeight = 38.sp,
                                        color = IvoryInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(10.dp))

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!liveState.isActive && liveState.status != LiveRecitationStatus.PAUSED) {
                                        Button(
                                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                            modifier = Modifier.weight(1f).testTag("hifz_start_mic_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                        ) {
                                            Icon(Icons.Rounded.Mic, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(L10n.pick(lang, "ابدأ التسميع", "Start Mic"))
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
                                        OutlinedButton(onClick = { viewModel.stopHifzLiveMic() }) {
                                            Icon(Icons.Rounded.Stop, contentDescription = "Stop")
                                        }
                                    }
                                    OutlinedButton(onClick = { viewModel.playReferenceAyah(activeVerse.surahNumber, activeVerse.numberInSurah) }) {
                                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Play Reference Ayah")
                                        Spacer(Modifier.width(4.dp))
                                        Text(L10n.pick(lang, "استمع", "Listen"))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(
                                        onClick = { if (activeAyahIdx > 0) activeAyahIdx-- },
                                        enabled = activeAyahIdx > 0
                                    ) { Text("← الآية السابقة") }
                                    TextButton(
                                        onClick = { if (activeAyahIdx < verses.lastIndex) activeAyahIdx++ },
                                        enabled = activeAyahIdx < verses.lastIndex
                                    ) { Text("الآية التالية →") }
                                }

                                if (liveState.statusNoteAr.isNotBlank()) {
                                    Text(
                                        text = L10n.pick(lang, liveState.statusNoteAr, liveState.statusNoteEn),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = noorColors.goldAccent
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualReciteInput,
                                    onValueChange = { manualReciteInput = it },
                                    label = { Text("أو اكتب الآية غيباً للتحقق النصي") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.evaluateRecitedVerse(activeVerse, manualReciteInput) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("تحقق من الحفظ") }
                                    Button(
                                        onClick = {
                                            viewModel.recordHifzSession(
                                                surahNumber = activeVerse.surahNumber,
                                                fromAyah = 1,
                                                toAyah = activeVerse.numberInSurah,
                                                sessionType = "LIVE_CHECK",
                                                accuracyPercent = comparison?.similarityPercent ?: 90
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) { Text("حفظ الجلسة") }
                                }
                            }
                        }
                    }

                    comparison?.let { res ->
                        item {
                            val badgeColor = when (res.status) {
                                "MATCHED" -> SemanticSuccess
                                "POSSIBLE_MISTAKE" -> SemanticWarning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val statusText = when (res.status) {
                                "MATCHED" -> "تلاوة متقنة ومطابقة (${res.similarityPercent}%)"
                                "POSSIBLE_MISTAKE" -> "احتمال خطأ يحتاج مراجعة (${res.similarityPercent}%) — ليس تخطئة جازمة"
                                else -> "الصوت غير واضح بما يكفي — أعد التلاوة بوضوح (لم يُحتسب خطأ)"
                            }
                            LiquidGlassCard(goldBorder = res.status == "MATCHED") {
                                Text(statusText, color = badgeColor, fontWeight = FontWeight.Bold)
                                if (res.missingWordsHint.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("كلمات مقترحة للمراجعة: ${res.missingWordsHint.joinToString(" ، ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("المسموع: ${res.spokenText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 1: Verse Completion & Random Revision Quiz
                1 -> {
                    if (verses.size >= 2) {
                        item {
                            var drillIndex by remember(selectedSurah) { mutableIntStateOf(0) }
                            var showAnswer by remember(selectedSurah, drillIndex) { mutableStateOf(false) }
                            val promptVerse = verses[drillIndex.coerceIn(0, verses.lastIndex - 1)]
                            val nextVerse = verses[(drillIndex + 1).coerceIn(0, verses.lastIndex)]
                            val words = promptVerse.textUthmani.split(" ")
                            val halfPrompt = words.take((words.size / 2).coerceAtLeast(2)).joinToString(" ")

                            LiquidGlassCard(strong = true, goldBorder = true) {
                                Text("تمرين إكمال الآية والربط بين الآيات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                                Spacer(Modifier.height(8.dp))
                                Text("أكمل من قوله تعالى (آية ${promptVerse.numberInSurah}):", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(6.dp))
                                WarmIvoryParchmentCard {
                                    Text(
                                        text = "$halfPrompt ...",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        color = IvoryInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (showAnswer) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("الآية كاملة: ${promptVerse.textUthmani}", fontFamily = AmiriFontFamily, fontSize = 19.sp)
                                    Text("الآية التالية (${nextVerse.numberInSurah}): ${nextVerse.textUthmani}", fontFamily = AmiriFontFamily, fontSize = 19.sp, color = noorColors.goldAccent)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { showAnswer = !showAnswer }, modifier = Modifier.weight(1f)) {
                                        Text(if (showAnswer) "إخفاء الإجابة" else "إظهار الإجابة")
                                    }
                                    Button(
                                        onClick = {
                                            drillIndex = (drillIndex + 1) % (verses.size - 1).coerceAtLeast(1)
                                            showAnswer = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) { Text("آية عشوائية تالية") }
                                }
                            }
                        }
                    }
                    item {
                        NoorSectionHeader(title = L10n.pick(lang, "الآيات الأكثر حاجة للمراجعة", "Frequently Missed Verses"))
                    }
                    if (weakVerses.isEmpty()) {
                        item {
                            LiquidGlassCard {
                                Text("لا توجد آيات ضعيفة مسجلة حالياً — استمر في التسميع والمراجعة!")
                            }
                        }
                    }
                    items(weakVerses, key = { it.id }) { w ->
                        val s = SurahCatalog.findSurah(w.surahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.nameAr} — آية ${w.ayahNumber}", fontWeight = FontWeight.Bold)
                                    Text("عدد التكرارات: ${w.missCount} · ${w.lastMistakeNote}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.playReferenceAyah(w.surahNumber, w.ayahNumber) }) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Listen")
                                }
                                IconButton(onClick = { viewModel.markWeakVerseResolved(w.id, !w.resolved) }) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Resolve",
                                        tint = if (w.resolved) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2: Plans, Schedules, Revision History & Teacher Approval
                2 -> {
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
                    items(plans, key = { it.id }) { p ->
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
                        NoorSectionHeader(title = "سجل المراجعة والتقارير (المتوسط $avgAcc%)")
                    }
                    items(logs.take(15), key = { it.id }) { log ->
                        val s = SurahCatalog.findSurah(log.surahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.nameAr} (${log.fromAyah}–${log.toAyah})", fontWeight = FontWeight.Bold)
                                    Text("${log.dateKey} · الدقة: ${log.accuracyPercent}% · ${log.sessionType}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                                }
                                TextButton(onClick = { viewModel.approveHifzLog(log.id, !log.teacherApproved) }) {
                                    Text(
                                        if (log.teacherApproved) "معتمد من المعلم ✓" else "اعتماد المعلم",
                                        color = if (log.teacherApproved) SemanticSuccess else noorColors.goldAccent
                                    )
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
