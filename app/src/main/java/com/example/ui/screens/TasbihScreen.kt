package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.AdhkarCatalog
import com.example.domain.model.TasbihPreset
import com.example.domain.tasbih.RealisticTasbihCatalog
import com.example.domain.tasbih.TasbihBeadTheme
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.MosqueHorizonSilhouette
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TasbihScreen(viewModel: NoorViewModel) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val liveCount by viewModel.liveTasbihCount.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTasbihTotal.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()
    val sessions by viewModel.tasbihSessions.collectAsStateWithLifecycle()
    val customPresets by viewModel.customDhikrPresets.collectAsStateWithLifecycle()

    var selectedThemeId by remember { mutableStateOf(RealisticTasbihCatalog.loadSelectedThemeId(context)) }
    val beadTheme = remember(selectedThemeId) { RealisticTasbihCatalog.byId(selectedThemeId) }

    var fullScreenFocusMode by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }

    // Continuous spring offset for real physical bead glide animation (-1f..+1f)
    val beadSlideAnim = remember { Animatable(0f) }

    fun triggerForwardBead() {
        viewModel.incrementTasbih(beadTheme.timbre)
        scope.launch {
            beadSlideAnim.snapTo(-1f)
            beadSlideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun triggerBackwardBead() {
        if (liveCount <= 0) return
        viewModel.decrementTasbih(beadTheme.timbre)
        scope.launch {
            beadSlideAnim.snapTo(1f)
            beadSlideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

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
    val roundsCompleted = liveCount / target
    val countInCycle = liveCount % target

    if (fullScreenFocusMode) {
        // Distraction-Free Full-Screen 100% Physical Misbaha Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF041510), Color(0xFF020B08))))
        ) {
            MosqueHorizonSilhouette(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter),
                tint = beadTheme.tasselColor.copy(alpha = 0.14f)
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
                    .padding(20.dp),
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
                    text = "$liveCount / $target" + if (roundsCompleted > 0) "  ·  ${L10n.pick(lang, "الجولة $roundsCompleted", "Round $roundsCompleted", "$roundsCompleted-davr")}" else "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(18.dp))

                RealisticPhysicalMisbahaStage(
                    beadCount = liveCount,
                    countInCycle = countInCycle,
                    target = target,
                    rounds = roundsCompleted,
                    slideFraction = beadSlideAnim.value,
                    theme = beadTheme,
                    onForward = { triggerForwardBead() },
                    onBackward = { triggerBackwardBead() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .testTag("tasbih_tap_button")
                )

                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { triggerBackwardBead() },
                        enabled = liveCount > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, tint = GoldBright)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "ترجيع (-1)", "Step Back (-1)", "Orqaga (-1)"), color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetTasbihCounter() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = GoldBright)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "تصفير", "Reset", "Nollash"), color = Color.White)
                    }
                }
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
            title = L10n.pick(lang, "المسبحة الحقيقية الفاخرة", "Realistic 3D Misbaha", "Haqiqiy 3D Tasbeh"),
            subtitle = L10n.pick(
                lang,
                "اسحب الخرزة للأمام أو للخلف · ٢٠ لوناً فاخراً مع صوت خرز طبيعي",
                "Slide beads forward or backward · 20 luxury bead colors & authentic sound",
                "Donalarni oldinga yoki orqaga suring · 20 ta hashamatli rang va tabiiy tovush"
            ),
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
            // 1. 100% Real-Feel Physical Misbaha Interactive Stage
            item {
                LiquidGlassCard(
                    strong = true,
                    goldBorder = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Active Dhikr Banner
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldPrimary.copy(alpha = 0.22f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = activePreset.textAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 22.sp,
                                color = if (noorColors.isDark) GoldBright else EmeraldDeep,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Physical 3D Misbaha Canvas (Swipe forward/backward or tap!)
                        RealisticPhysicalMisbahaStage(
                            beadCount = liveCount,
                            countInCycle = countInCycle,
                            target = target,
                            rounds = roundsCompleted,
                            slideFraction = beadSlideAnim.value,
                            theme = beadTheme,
                            onForward = { triggerForwardBead() },
                            onBackward = { triggerBackwardBead() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .testTag("tasbih_tap_button")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = L10n.pick(
                                lang,
                                "اضغط أو اسحب للأمام للتسبيح · اسحب للأعلى أو اضغط «ترجيع» للعودة",
                                "Tap or swipe down/right to count · Swipe up/left or tap Back to undo",
                                "Sanash uchun bosing yoki pastga suring · Orqaga qaytarish uchun tepaga suring"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Forward / Step-Back / Reset Tactile Control Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { triggerBackwardBead() },
                                enabled = liveCount > 0,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("tasbih_undo_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = L10n.pick(lang, "ترجيع (-1)", "Back (-1)", "Orqaga (-1)"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { triggerForwardBead() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = beadTheme.bodyColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("tasbih_forward_button")
                            ) {
                                Text(
                                    text = L10n.pick(lang, "تسبيح (+1)", "Count (+1)", "Tasbeh (+1)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.resetTasbihCounter() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .testTag("tasbih_reset_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = noorColors.goldAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(33, 99, 100).forEach { t ->
                                LiquidGlassChip(
                                    label = t.toString(),
                                    selected = target == t,
                                    onClick = { viewModel.updateTasbihTarget(t) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            LiquidGlassChip(
                                label = L10n.pick(lang, "مخصص", "Custom", "Maxsus"),
                                selected = target !in listOf(33, 99, 100),
                                onClick = { showCustomTargetDialog = true },
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }

            // 2. 20 Luxury Physical Bead Colors & Materials Picker
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "ألوان كرات المسبحة (٢٠ لوناً فاخراً)", "20 Luxury Bead Colors", "20 ta hashamatli tasbeh rangi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = L10n.pick(lang, beadTheme.nameAr, beadTheme.nameEn, beadTheme.nameUz),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("tasbih_20_colors_row")
                        ) {
                            items(RealisticTasbihCatalog.themes, key = { it.id }) { itemTheme ->
                                val isSelected = itemTheme.id == beadTheme.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .clickable {
                                            selectedThemeId = itemTheme.id
                                            RealisticTasbihCatalog.saveSelectedThemeId(context, itemTheme.id)
                                        }
                                        .testTag("bead_color_${itemTheme.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.8.dp else 1.dp,
                                                color = if (isSelected) GoldBright else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                            .padding(3.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val r = size.minDimension / 2f
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(itemTheme.highlightColor, itemTheme.bodyColor, itemTheme.shadowColor),
                                                    center = Offset(size.width * 0.32f, size.height * 0.28f),
                                                    radius = r * 1.35f
                                                ),
                                                radius = r
                                            )
                                            // Specular glass pearl highlight
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.65f),
                                                radius = r * 0.22f,
                                                center = Offset(size.width * 0.34f, size.height * 0.28f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = L10n.pick(lang, itemTheme.nameAr, itemTheme.nameEn, itemTheme.nameUz),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = if (isSelected) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Today / This Week / Total Stats Row
            item {
                val todayDisplay = maxOf(todayTotal, liveCount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "اليوم", "Today", "Bugun"),
                        value = todayDisplay.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "هذا الأسبوع", "This Week", "Shu hafta"),
                        value = maxOf(stats.weekTasbih, todayDisplay).toString(),
                        modifier = Modifier.weight(1f)
                    )
                    TasbihStatMiniCard(
                        label = L10n.pick(lang, "الإجمالي", "Total", "Jami"),
                        value = maxOf(stats.allTimeTasbih, todayDisplay).toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Quick Dhikr Presets & Custom Dhikr Creation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, "الأذكار السريعة والمخصصة", "Quick & Custom Dhikr", "Tezkor va maxsus zikrlar"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "إضافة ذكر", "Add Dhikr", "Zikr qo'shish"),
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

            // 5. Recent Sessions History
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = L10n.pick(lang, "سجل الجلسات الأخيرة", "Recent Sessions", "Oxirgi sessiyalar"),
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
            title = { Text(L10n.pick(lang, "إضافة ذكر مخصص", "Add Custom Dhikr", "Maxsus zikr qo'shish")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = arText,
                        onValueChange = { arText = it },
                        label = { Text(L10n.pick(lang, "نص الذكر", "Dhikr Text", "Zikr matni")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = enText,
                        onValueChange = { enText = it },
                        label = { Text(L10n.pick(lang, "الترجمة أو الاسم (اختياري)", "Label (Optional)", "Izoh (ixtiyoriy)")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(L10n.pick(lang, "الهدف اليومي", "Default Target", "Kunlik maqsad")) },
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
                    Text(L10n.pick(lang, "حفظ", "Save", "Saqlash"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
                }
            }
        )
    }

    if (showCustomTargetDialog) {
        var targetStr by remember { mutableStateOf(target.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text(L10n.pick(lang, "تحديد هدف مخصص", "Set Custom Target", "Maxsus maqsad")) },
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
                    Text(L10n.pick(lang, "تطبيق", "Apply", "Qo'llash"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
                }
            }
        )
    }
}

/**
 * 100% Real-Feel Interactive Physical Misbaha Canvas:
 * - Curved braided silk cord with ornate golden Imam headpiece & hanging silk tassel (الشرابة)
 * - 3D glossy shaded spheres with realistic specular reflection, drop shadow & 11-bead golden divider
 * - Interactive Swipe Down/Right (+1) & Swipe Up/Left (-1) + Tap support with spring bead physics
 */
@Composable
private fun RealisticPhysicalMisbahaStage(
    beadCount: Int,
    countInCycle: Int,
    target: Int,
    rounds: Int,
    slideFraction: Float,
    theme: TasbihBeadTheme,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var dragAccumY by remember { mutableFloatStateOf(0f) }
    var dragAccumX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        theme.bodyColor.copy(alpha = 0.22f),
                        Color(0xFF061610).copy(alpha = 0.78f)
                    )
                )
            )
            .border(1.2.dp, theme.tasselColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .pointerInput(beadCount) {
                detectTapGestures { onForward() }
            }
            .pointerInput(beadCount) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumY = 0f },
                    onDragEnd = {
                        if (dragAccumY > 42f) onForward()
                        else if (dragAccumY < -42f) onBackward()
                        dragAccumY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumY += dragAmount
                    }
                )
            }
            .pointerInput(beadCount) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumX = 0f },
                    onDragEnd = {
                        if (dragAccumX > 48f) onForward()
                        else if (dragAccumX < -48f) onBackward()
                        dragAccumX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumX += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Graceful curved physical Misbaha loop arc from top-left down-center to bottom-right
            fun pointOnCord(t: Float): Offset {
                val x = w * (0.10f + t * 0.80f)
                val curve = sin(t * PI).toFloat()
                val y = h * (0.16f + curve * 0.64f)
                return Offset(x, y)
            }

            // 1. Braided Silk String (الخيط الحريري)
            val cordPath = Path()
            val samples = 48
            for (i in 0..samples) {
                val p = pointOnCord(i.toFloat() / samples.toFloat())
                if (i == 0) cordPath.moveTo(p.x, p.y) else cordPath.lineTo(p.x, p.y)
            }
            drawPath(
                path = cordPath,
                color = theme.cordColor.copy(alpha = 0.85f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Subtle silk highlight thread
            drawPath(
                path = cordPath,
                color = Color.White.copy(alpha = 0.30f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Ornate Golden Imam Headpiece & Hanging Tassel (الإمام والشرابة) on the top-left end
            val tasselAnchor = pointOnCord(0.03f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GoldBright, theme.tasselColor, Color(0xFF5A3B08)),
                    center = tasselAnchor,
                    radius = 11.dp.toPx()
                ),
                radius = 9.dp.toPx(),
                center = tasselAnchor
            )
            // Hanging tassel strands
            for (s in -2..2) {
                val endX = tasselAnchor.x + s * 3.5.dp.toPx()
                val endY = tasselAnchor.y + 34.dp.toPx() + (2 - kotlin.math.abs(s)) * 3.dp.toPx()
                drawLine(
                    color = theme.tasselColor.copy(alpha = 0.85f),
                    start = Offset(tasselAnchor.x, tasselAnchor.y + 6.dp.toPx()),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw 11 Visible 3D Beads with Realistic Finger-Slide Gap in the Center!
            // Beads 0..4 are the "waiting" stack (left), gap in middle, Bead 5 slides across the gap, Beads 6..10 are the "counted" stack (right)
            val beadPositions = listOf(
                0.13f, 0.21f, 0.29f, 0.37f, // waiting stack
                0.50f + slideFraction * 0.13f, // actively sliding bead across the physical gap!
                0.66f, 0.74f, 0.82f, 0.90f // counted stack
            )

            beadPositions.forEachIndexed { idx, rawT ->
                val t = rawT.coerceIn(0.08f, 0.95f)
                val center = pointOnCord(t)
                val logicalBeadNum = beadCount + (idx - 4)
                val isDividerBead = logicalBeadNum > 0 && logicalBeadNum % 11 == 0
                val baseRadius = when {
                    idx == 4 -> 22.dp.toPx() // Active center bead closest to thumb
                    isDividerBead -> 13.dp.toPx() // Classic metallic Imam/11-divider bead
                    else -> 18.dp.toPx()
                }

                // Soft contact shadow underneath each physical bead
                drawCircle(
                    color = Color.Black.copy(alpha = 0.42f),
                    radius = baseRadius * 0.98f,
                    center = Offset(center.x + 2.dp.toPx(), center.y + 5.dp.toPx())
                )

                if (isDividerBead) {
                    // Ornate golden metallic divider disc
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFF8D6), GoldBright, Color(0xFF7D5810)),
                            center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f),
                            radius = baseRadius * 1.4f
                        ),
                        radius = baseRadius,
                        center = center
                    )
                } else {
                    // Authentic 3D glossy gemstone/wood bead body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.highlightColor, theme.bodyColor, theme.shadowColor),
                            center = Offset(center.x - baseRadius * 0.32f, center.y - baseRadius * 0.32f),
                            radius = baseRadius * 1.38f
                        ),
                        radius = baseRadius,
                        center = center
                    )

                    // Subtle inner translucent rim bounce light (bottom-right)
                    drawCircle(
                        color = theme.rimGlowColor.copy(alpha = 0.22f),
                        radius = baseRadius * 0.82f,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    // Primary crisp glass specular reflection dot (top-left)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.74f),
                        radius = baseRadius * 0.22f,
                        center = Offset(center.x - baseRadius * 0.34f, center.y - baseRadius * 0.34f)
                    )

                    // Secondary micro pinpoint sparkle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.40f),
                        radius = baseRadius * 0.09f,
                        center = Offset(center.x - baseRadius * 0.14f, center.y - baseRadius * 0.50f)
                    )
                }

                // Subtle cord hole shadow ring at bead edges
                drawCircle(
                    color = Color.Black.copy(alpha = 0.28f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(center.x - baseRadius * 0.92f, center.y)
                )
            }
        }

        // Top-center floating digital counter medallion (doesn't obstruct the physical bead strand)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = beadCount.toString(),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("tasbih_count_text")
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$countInCycle / $target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
                if (rounds > 0) {
                    Text(
                        text = "· ${L10n.pick(lang, "الجولة $rounds", "Round $rounds", "$rounds-davr")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF7BE8B4),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
