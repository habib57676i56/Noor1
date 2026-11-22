package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.AdhkarCatalog
import com.example.domain.model.TasbihPreset
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.MosqueHorizonSilhouette
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldMuted
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TasbihScreen(viewModel: NoorViewModel) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val liveCount by viewModel.liveTasbihCount.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTasbihTotal.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()
    val sessions by viewModel.tasbihSessions.collectAsStateWithLifecycle()
    val customPresets by viewModel.customDhikrPresets.collectAsStateWithLifecycle()

    var fullScreenFocusMode by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }

    if (fullScreenFocusMode) {
        BackHandler { fullScreenFocusMode = false }
    }

    val allPresets = remember(customPresets) {
        AdhkarCatalog.tasbihPresets + customPresets.map {
            TasbihPreset(
                id = it.id,
                textAr = it.textAr,
                textEn = it.textEn,
                defaultTarget = it.defaultTarget,
                isCustom = true
            )
        }
    }

    val activePreset = allPresets.firstOrNull { it.id == prefs.activeTasbihPresetId }
        ?: TasbihPreset("custom", prefs.customTasbihText, prefs.customTasbihText, prefs.activeTasbihTarget, true)

    val target = prefs.activeTasbihTarget.coerceAtLeast(1)
    val cycleProgress = ((liveCount % target).let { if (it == 0 && liveCount > 0) target else it }).toFloat() / target.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = cycleProgress.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "tasbih_ring"
    )
    val roundsCompleted = liveCount / target

    if (fullScreenFocusMode) {
        // Distraction-Free Full-Screen Tasbeeh Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF04140F), Color(0xFF030B08))))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.incrementTasbih() }
                .testTag("tasbih_tap_button")
        ) {
            MosqueHorizonSilhouette(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter),
                tint = GoldPrimary.copy(alpha = 0.14f)
            )
            IconButton(
                onClick = { fullScreenFocusMode = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Icon(Icons.Rounded.CloseFullscreen, contentDescription = "Exit focus mode", tint = GoldBright)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = activePreset.textAr,
                    fontFamily = AmiriFontFamily,
                    fontSize = 30.sp,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activePreset.textEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Spacer(modifier = Modifier.height(34.dp))
                LuminousTasbihDial(
                    count = liveCount,
                    target = target,
                    progress = animatedProgress,
                    rounds = roundsCompleted,
                    onTap = { viewModel.incrementTasbih() }
                )
                Spacer(modifier = Modifier.height(22.dp))
                GoldenBeadsStrandCanvas(
                    beadCount = liveCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = L10n.pick(lang, "اضغط في أي مكان على الشاشة للتسبيح", "Tap anywhere on screen to count"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasbih_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "المسبحة الذكية", "Smart Tasbeeh"),
            subtitle = L10n.pick(lang, "تعمل بالكامل دون إنترنت مع حفظ تلقائي", "100% offline with automatic session tracking"),
            trailing = {
                Row {
                    IconButton(
                        onClick = { viewModel.setTasbihVibration(!prefs.tasbihVibrationEnabled) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Vibration,
                            contentDescription = "Vibration",
                            tint = if (prefs.tasbihVibrationEnabled) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setTasbihSound(!prefs.tasbihSoundEnabled) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            if (prefs.tasbihSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (prefs.tasbihSoundEnabled) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { fullScreenFocusMode = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInFull, contentDescription = "Full-Screen Mode", tint = noorColors.goldAccent)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Luminous Tasbeeh Counter Card
            item {
                LiquidGlassCard(
                    strong = true,
                    goldBorder = true,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LuminousTasbihDial(
                            count = liveCount,
                            target = target,
                            progress = animatedProgress,
                            rounds = roundsCompleted,
                            onTap = { viewModel.incrementTasbih() },
                            modifier = Modifier.testTag("tasbih_tap_button")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sculpted Golden Tasbeeh Beads Strand
                        GoldenBeadsStrandCanvas(
                            beadCount = liveCount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Dhikr Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldPrimary.copy(alpha = 0.24f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = activePreset.textAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 22.sp,
                                color = if (noorColors.isDark) GoldBright else EmeraldDeep,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Presets + Reset Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(33, 99, 100).forEach { t ->
                                    LiquidGlassChip(
                                        label = t.toString(),
                                        selected = target == t,
                                        onClick = { viewModel.updateTasbihTarget(t) }
                                    )
                                }
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "مخصص", "Custom"),
                                    selected = target !in listOf(33, 99, 100),
                                    onClick = { showCustomTargetDialog = true }
                                )
                            }
                            IconButton(
                                onClick = { viewModel.resetTasbihCounter() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .testTag("tasbih_reset_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = noorColors.goldAccent)
                            }
                        }
                    }
                }
            }

            // Today / This Week / Total Stats Row
            item {
                val todayDisplay = maxOf(todayTotal, liveCount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "اليوم", "Today"),
                        value = todayDisplay.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "هذا الأسبوع", "This Week"),
                        value = maxOf(stats.weekTasbih, todayDisplay).toString(),
                        modifier = Modifier.weight(1f)
                    )
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "الإجمالي", "Total"),
                        value = maxOf(stats.allTimeTasbih, todayDisplay).toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Dhikr Presets & Custom Dhikr Creation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, "الأذكار السريعة والمخصصة", "Quick & Custom Dhikr"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "إضافة ذكر", "Add Dhikr"),
                        selected = true,
                        icon = Icons.Rounded.Add,
                        onClick = { showAddCustomDialog = true }
                    )
                }
            }

            items(allPresets.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = preset.id == prefs.activeTasbihPresetId
                        LiquidGlassCard(
                            goldBorder = isSelected,
                            onClick = { viewModel.selectTasbihPreset(preset.id, preset.defaultTarget, preset.textAr) },
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.textAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) noorColors.goldAccent else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${preset.textEn} · ${preset.defaultTarget}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (preset.isCustom) {
                                    IconButton(
                                        onClick = { viewModel.deleteCustomDhikrPreset(preset.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Recent Sessions History
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = L10n.pick(lang, "سجل الجلسات الأخيرة", "Recent Sessions"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(sessions.take(8), key = { it.id }) { s ->
                    LiquidGlassCard(contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, s.labelAr, s.labelEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = s.dateKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${s.count} / ${s.target}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        var arText by remember { mutableStateOf("") }
        var enText by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("33") }
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text(L10n.pick(lang, "إضافة ذكر مخصص", "Add Custom Dhikr")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = arText,
                        onValueChange = { arText = it },
                        label = { Text(L10n.pick(lang, "نص الذكر", "Dhikr Text")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = enText,
                        onValueChange = { enText = it },
                        label = { Text(L10n.pick(lang, "الترجمة أو الاسم (اختياري)", "Label / Transliteration (Optional)")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(L10n.pick(lang, "الهدف اليومي", "Default Target")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (arText.isNotBlank()) {
                            viewModel.addCustomDhikrPreset(arText, enText, targetText.toIntOrNull() ?: 33)
                            showAddCustomDialog = false
                        }
                    }
                ) {
                    Text(L10n.pick(lang, "حفظ", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    if (showCustomTargetDialog) {
        var targetStr by remember { mutableStateOf(target.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text(L10n.pick(lang, "تحديد هدف مخصص", "Set Custom Target")) },
            text = {
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { c -> c.isDigit() }.take(4) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetStr.toIntOrNull()?.let { viewModel.updateTasbihTarget(it) }
                        showCustomTargetDialog = false
                    }
                ) {
                    Text(L10n.pick(lang, "تطبيق", "Apply"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun LuminousTasbihDial(
    count: Int,
    target: Int,
    progress: Float,
    rounds: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    Box(
        modifier = modifier
            .size(226.dp)
            .clip(CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 14.dp.toPx()
            val inset = strokeW / 2f + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)

            // Outer ambient emerald glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x5522C58B), Color(0x22104F38), Color.Transparent)
                ),
                radius = size.minDimension * 0.5f
            )
            // Inner obsidian-emerald face
            drawCircle(
                brush = Brush.verticalGradient(listOf(Color(0xFF0E3427), Color(0xFF05130E))),
                radius = size.minDimension * 0.39f
            )
            // Track ring
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            // Luminous active emerald-gold arc
            drawArc(
                brush = Brush.sweepGradient(listOf(EmeraldSoft, GoldBright, Color(0xFF39D98A), EmeraldSoft)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("tasbih_count_text")
            )
            Text(
                text = "/ $target",
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright,
                fontWeight = FontWeight.SemiBold
            )
            if (rounds > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = L10n.pick(lang, "الجولة $rounds", "Round $rounds"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7BE8B4)
                )
            }
        }
    }
}

@Composable
private fun GoldenBeadsStrandCanvas(
    beadCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 11
        val shift = (beadCount % 3) * 0.015f

        for (i in 0 until count) {
            val t = i.toFloat() / (count - 1).toFloat()
            val x = w * (0.08f + t * 0.84f)
            // Gentle catenary curve dipping down in the center
            val sag = sin((t + shift).coerceIn(0f, 1f) * PI).toFloat()
            val y = h * (0.26f + sag * 0.44f)
            val beadR = if (i == count / 2) 10.dp.toPx() else 7.5.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF2C6), GoldPrimary, GoldMuted),
                    center = Offset(x - beadR * 0.25f, y - beadR * 0.25f),
                    radius = beadR * 1.4f
                ),
                radius = beadR,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun TasbihStatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    LiquidGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = noorColors.goldAccent
            )
        }
    }
}
