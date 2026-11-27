package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.AdhkarCatalog
import com.example.domain.model.AppLanguage
import com.example.domain.model.TasbihPreset
import com.example.domain.tasbih.BeadSurfaceTexture
import com.example.domain.tasbih.RealisticBeadSoundPlayer
import com.example.domain.tasbih.RealisticTasbihCatalog
import com.example.domain.tasbih.TasbihBeadTheme
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TasbihBgObsidian = Color(0xFF0E1213)
private val TasbihSheetDark = Color(0xFF1A1F21)
private val TasbihEmeraldAccent = Color(0xFF28C76F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(viewModel: NoorViewModel) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val liveCount by viewModel.liveTasbihCount.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTasbihTotal.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()
    val sessions by viewModel.tasbihSessions.collectAsStateWithLifecycle()
    val customPresets by viewModel.customDhikrPresets.collectAsStateWithLifecycle()

    var selectedThemeId by remember { mutableStateOf(RealisticTasbihCatalog.loadSelectedThemeId(context)) }
    val beadTheme = remember(selectedThemeId) { RealisticTasbihCatalog.byId(selectedThemeId) }

    var showDhikrDrawer by remember { mutableStateOf(false) }
    var showAllColorsAndStatsSheet by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }

    // Smooth non-overshooting bead glide progress:
    //  0f       = resting stacks (ZERO collisions!)
    //  0f..+1f  = forward bead gliding LEFT -> RIGHT (count +1 at +1f)
    // -1f..0f   = backward bead gliding RIGHT -> LEFT (count -1 at -1f)
    val beadGlideAnim = remember { Animatable(0f) }
    var isDraggingBead by remember { mutableStateOf(false) }
    var arenaWidthPx by remember { mutableFloatStateOf(1080f) }

    val latestLiveCount by rememberUpdatedState(liveCount)
    val latestBeadTheme by rememberUpdatedState(beadTheme)

    fun triggerForwardBead() {
        if (isDraggingBead) return
        scope.launch {
            // If an animation was already mid-flight, finish it cleanly before starting next bead
            if (beadGlideAnim.value > 0.05f) {
                beadGlideAnim.snapTo(0f)
            }
            beadGlideAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 175, easing = FastOutSlowInEasing)
            )
            viewModel.incrementTasbih(latestBeadTheme.timbre)
            beadGlideAnim.snapTo(0f)
        }
    }

    fun triggerBackwardBead() {
        if (isDraggingBead || latestLiveCount <= 0) return
        scope.launch {
            if (beadGlideAnim.value < -0.05f) {
                beadGlideAnim.snapTo(0f)
            }
            beadGlideAnim.animateTo(
                targetValue = -1f,
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
            )
            viewModel.decrementTasbih(latestBeadTheme.timbre)
            beadGlideAnim.snapTo(0f)
        }
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
    val displayedCount = if (liveCount > 0 && countInCycle == 0) target else countInCycle
    val countDigits = formatTasbihDigits(displayedCount.toString().padStart(2, '0'), lang)
    val targetDigits = formatTasbihDigits(target.toString(), lang)
    val roundsDigits = formatTasbihDigits(roundsCompleted.toString(), lang)
    val liveAccent = if (beadTheme.id == "imperial_emerald") TasbihEmeraldAccent else beadTheme.tasselColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E0F), TasbihBgObsidian, Color(0xFF101516))
                )
            )
            .testTag("tasbih_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Minimalist Sleek Top Header Bar (matching physical Misbaha aesthetic)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = L10n.pick(lang, "المسبحة", "Tasbih", "Tasbeh", "Тасбих"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Subtle active Dhikr pill (tap to switch Dhikr)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, liveAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .clickable { showDhikrDrawer = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = activePreset.textAr,
                            fontFamily = AmiriFontFamily,
                            fontSize = 14.sp,
                            color = liveAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.setTasbihVibration(!prefs.tasbihVibrationEnabled) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Vibration,
                            contentDescription = L10n.pick(lang, "الاهتزاز", "Vibration"),
                            tint = if (prefs.tasbihVibrationEnabled) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetTasbihCounter() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("tasbih_reset_button")
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = L10n.pick(lang, "تصفير", "Reset"),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setTasbihSound(!prefs.tasbihSoundEnabled) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            if (prefs.tasbihSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = L10n.pick(lang, "الصوت", "Sound"),
                            tint = if (prefs.tasbihSoundEnabled) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // 2. Full Interactive Tap + Live Hold-and-Drag Center Arena
            //    - Single tap anywhere: smoothly glides 1 bead left -> right (+1)
            //    - Hold & drag RIGHT (+dx): grab bead under finger and slide forward (+1)
            //    - Hold & drag LEFT (-dx): grab counted bead and pull back (-1)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { if (it.width > 0) arenaWidthPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { triggerForwardBead() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDraggingBead = true
                                scope.launch { beadGlideAnim.stop() }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                // Full gap span is ~32% of viewport width; dragging across it moves 1 full bead
                                val gapSpanPx = (arenaWidthPx * 0.34f).coerceAtLeast(160f)
                                val deltaProgress = dragAmount / gapSpanPx
                                val rawNext = beadGlideAnim.value + deltaProgress
                                scope.launch {
                                    when {
                                        // Continuous drag forward past 92% -> commit bead seamlessly & keep dragging!
                                        rawNext >= 0.92f -> {
                                            viewModel.incrementTasbih(latestBeadTheme.timbre)
                                            beadGlideAnim.snapTo((rawNext - 1f).coerceIn(0f, 0.25f))
                                        }
                                        // Continuous drag backward past -92% -> decrement seamlessly if count > 0!
                                        rawNext <= -0.92f && latestLiveCount > 0 -> {
                                            viewModel.decrementTasbih(latestBeadTheme.timbre)
                                            beadGlideAnim.snapTo((rawNext + 1f).coerceIn(-0.25f, 0f))
                                        }
                                        // Gentle rubber-band resistance if already at 0 and pulling left
                                        rawNext < 0f && latestLiveCount <= 0 -> {
                                            beadGlideAnim.snapTo(rawNext.coerceAtLeast(-0.14f))
                                        }
                                        else -> {
                                            beadGlideAnim.snapTo(rawNext.coerceIn(-1f, 1f))
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                isDraggingBead = false
                                scope.launch {
                                    beadGlideAnim.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                                }
                            },
                            onDragEnd = {
                                isDraggingBead = false
                                val current = beadGlideAnim.value
                                scope.launch {
                                    when {
                                        current >= 0.36f -> {
                                            beadGlideAnim.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
                                            viewModel.incrementTasbih(latestBeadTheme.timbre)
                                            beadGlideAnim.snapTo(0f)
                                        }
                                        current <= -0.36f && latestLiveCount > 0 -> {
                                            beadGlideAnim.animateTo(-1f, tween(130, easing = FastOutSlowInEasing))
                                            viewModel.decrementTasbih(latestBeadTheme.timbre)
                                            beadGlideAnim.snapTo(0f)
                                        }
                                        else -> {
                                            beadGlideAnim.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .testTag("tasbih_tap_button"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cycle / Round Counter (e.g., "مرة ٣")
                Text(
                    text = L10n.pick(
                        lang,
                        "مرة $roundsDigits",
                        "Round $roundsCompleted",
                        "$roundsCompleted-davr",
                        "Круг $roundsCompleted"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Giant Luminous Emerald / Theme Live Counter (e.g. "٠٣")
                Text(
                    text = countDigits,
                    fontSize = 72.sp,
                    lineHeight = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = liveAccent,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Editable Target Pill ("١٠٠ / ✎")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCustomTargetDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "/ $targetDigits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = L10n.pick(lang, "تعديل الهدف", "Edit Target"),
                        tint = Color.White.copy(alpha = 0.58f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. HYPER-REALISTIC 3D HORIZONTAL SLIGHTLY-ANGLED PHYSICAL MISBAHA STRING
                HorizontalPhysical3DMisbahaString(
                    beadCount = liveCount,
                    glideFraction = beadGlideAnim.value,
                    theme = beadTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Clear Gesture Instruction Caption (Tap + Hold-Drag Right +1 / Drag Left -1)
                Text(
                    text = L10n.pick(
                        lang,
                        "اضغط للعد أو امسك واسحب لليمين (+١)\nاسحب لليسار للرجوع وإنقاص العدد (-١)",
                        "Tap to count or hold & drag right (+1)\nDrag left to pull back (-1)",
                        "Sanash uchun bosing yoki o'ngga suring (+1)\nKamaytirish uchun chapga suring (-1)",
                        "Нажмите или тяните вправо (+1)\nПотяните влево для возврата (-1)"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // 4. Sleek Dark Rounded Bottom Material & Dhikr Picker Panel (matching screenshot)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = TasbihSheetDark,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Minimal "=" double drag handle (tap to open full Dhikr & Stats drawer)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAllColorsAndStatsSheet = true }
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.65f))
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.65f))
                        )
                    }

                    // Header row: "تشغيل الذكر" (Start Dhikr) | "عرض الكل" (Show All)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(lang, "تشغيل الذكر", "Start Dhikr", "Zikrni tanlash", "Выбрать зикр"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDhikrDrawer = true }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Text(
                            text = L10n.pick(lang, "عرض الكل", "Show All", "Barchasi", "Показать все"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TasbihEmeraldAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAllColorsAndStatsSheet = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // Horizontal Row of Realistic 3D Bead Material Spheres (Wood, Jade, Amber, Silver Pearl, Obsidian, etc.)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("tasbih_20_colors_row")
                    ) {
                        items(RealisticTasbihCatalog.themes, key = { it.id }) { itemTheme ->
                            val isSelected = itemTheme.id == beadTheme.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(62.dp)
                                    .clickable {
                                        selectedThemeId = itemTheme.id
                                        RealisticTasbihCatalog.saveSelectedThemeId(context, itemTheme.id)
                                        if (prefs.tasbihSoundEnabled) {
                                            RealisticBeadSoundPlayer.playBeadClick(itemTheme.timbre)
                                        }
                                    }
                                    .testTag("bead_color_${itemTheme.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) TasbihEmeraldAccent else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(if (isSelected) 3.dp else 1.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRealistic3DBead(
                                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                                            radius = size.minDimension * 0.48f,
                                            theme = itemTheme,
                                            seed = itemTheme.id.hashCode(),
                                            showCordHoles = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, itemTheme.nameAr, itemTheme.nameEn, itemTheme.nameUz, itemTheme.nameRu),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) TasbihEmeraldAccent else Color.White.copy(alpha = 0.65f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dhikr Preset & Custom Dhikr Bottom Sheet
    if (showDhikrDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDhikrDrawer = false },
            sheetState = sheetState,
            containerColor = TasbihSheetDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, "اختر الذكر أو أضف ذكراً مخصصاً", "Choose or Add Dhikr", "Zikrni tanlang", "Выберите или добавьте зикр"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "إضافة ذكر", "Add Dhikr"),
                        selected = true,
                        icon = Icons.Rounded.Add,
                        onClick = {
                            showDhikrDrawer = false
                            showAddCustomDialog = true
                        }
                    )
                }

                // Quick Target Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(33, 99, 100, 1000).forEach { t ->
                        LiquidGlassChip(
                            label = "$t",
                            selected = target == t,
                            onClick = { viewModel.updateTasbihTarget(t) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allPresets, key = { it.id }) { preset ->
                        val isSelected = preset.id == prefs.activeTasbihPresetId
                        LiquidGlassCard(
                            goldBorder = isSelected,
                            onClick = {
                                viewModel.selectTasbihPreset(preset.id, preset.defaultTarget, preset.textAr)
                                showDhikrDrawer = false
                            },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.textAr,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TasbihEmeraldAccent else Color.White
                                    )
                                    Text(
                                        text = "${preset.textEn} · ${preset.defaultTarget}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                                if (preset.isCustom) {
                                    IconButton(
                                        onClick = { viewModel.deleteCustomDhikrPreset(preset.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Expanded All-Materials + Statistics + Session History Sheet ("عرض الكل")
    if (showAllColorsAndStatsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val todayDisplay = maxOf(todayTotal, liveCount)
        ModalBottomSheet(
            onDismissRequest = { showAllColorsAndStatsSheet = false },
            sheetState = sheetState,
            containerColor = TasbihSheetDark
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = L10n.pick(
                            lang,
                            "جميع خامات المسبحة الحقيقية (٢٠ نوعاً) والإحصائيات",
                            "All 20 Realistic Bead Materials & Stats",
                            "Barcha 20 ta tasbeh materiallari va statistika",
                            "Все 20 материалов бусин и статистика"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Today / Week / All-Time Stats
                item {
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

                // 4-Column Grid of all 20 3D Bead Spheres
                items(RealisticTasbihCatalog.themes.chunked(4)) { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowThemes.forEach { itemTheme ->
                            val isSelected = itemTheme.id == beadTheme.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) TasbihEmeraldAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) TasbihEmeraldAccent else Color.White.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedThemeId = itemTheme.id
                                        RealisticTasbihCatalog.saveSelectedThemeId(context, itemTheme.id)
                                        if (prefs.tasbihSoundEnabled) {
                                            RealisticBeadSoundPlayer.playBeadClick(itemTheme.timbre)
                                        }
                                        showAllColorsAndStatsSheet = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Canvas(modifier = Modifier.size(46.dp)) {
                                    drawRealistic3DBead(
                                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                                        radius = size.minDimension * 0.47f,
                                        theme = itemTheme,
                                        seed = itemTheme.id.hashCode(),
                                        showCordHoles = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = L10n.pick(lang, itemTheme.nameAr, itemTheme.nameEn, itemTheme.nameUz, itemTheme.nameRu),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) TasbihEmeraldAccent else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        repeat(4 - rowThemes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (sessions.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(lang, "سجل الجلسات الأخيرة", "Recent Sessions"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    items(sessions.take(6), key = { it.id }) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, s.labelAr, s.labelEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = s.dateKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                            Text(
                                text = "${s.count} / ${s.target}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TasbihEmeraldAccent
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
                        label = { Text(L10n.pick(lang, "الترجمة أو الاسم (اختياري)", "Label (Optional)")) },
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
            title = { Text(L10n.pick(lang, "تحديد هدف التسبيح", "Set Custom Target")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(33, 99, 100, 1000).forEach { presetVal ->
                            LiquidGlassChip(
                                label = "$presetVal",
                                selected = targetStr == presetVal.toString(),
                                onClick = { targetStr = presetVal.toString() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { c -> c.isDigit() }.take(4) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

/**
 * Pure, zero-collision physical bead slot representation along the normalized silk cord (`t` in `0f..1f`).
 */
internal data class TasbihBeadSlot(
    val t: Float,
    val seedOffset: Int,
    val isActive: Boolean
)

/**
 * Computes strictly monotonic, non-overlapping bead positions along the cord for ANY interactive
 * drag/animation progress in `[-1f..+1f]`:
 * - `progress == 0f`       -> resting left waiting stack + open central gap + right counted stack
 * - `progress in (0f..1f]` -> lead bead glides smoothly LEFT -> RIGHT (`gapStartT` -> `gapEndT`),
 *                             left & right stacks advance synchronously by `+progress * beadStepT`
 *                             so `progress = 1f` matches `progress = 0f` pixel-for-pixel (ZERO jump!)
 * - `progress in [-1f..0f)` -> front right bead glides smoothly RIGHT -> LEFT (`gapEndT` -> `gapStartT`),
 *                             both stacks shift synchronously by `-|progress| * beadStepT`
 *                             so `progress = -1f` matches `progress = 0f` pixel-for-pixel (ZERO jump!)
 */
internal fun computeTasbihBeadSlots(
    progress: Float,
    beadStepT: Float,
    gapStartT: Float = 0.34f,
    gapEndT: Float = 0.66f
): List<TasbihBeadSlot> {
    val p = progress.coerceIn(-1f, 1f)
    val safeStep = beadStepT.coerceAtLeast(0.05f)
    val slots = mutableListOf<TasbihBeadSlot>()

    when {
        p > 0f -> {
            // Left back stack (beads 1..5 shifting smoothly rightward to fill gapStartT)
            for (i in 5 downTo 1) {
                slots += TasbihBeadSlot(
                    t = gapStartT - i * safeStep + p * safeStep,
                    seedOffset = -i,
                    isActive = false
                )
            }
            // Active gliding bead sliding smoothly across the center gap from LEFT -> RIGHT
            slots += TasbihBeadSlot(
                t = gapStartT + (gapEndT - gapStartT) * p,
                seedOffset = 0,
                isActive = true
            )
            // Right counted stack (beads 0..4 shifting smoothly rightward to receive arriving bead)
            for (i in 0..4) {
                slots += TasbihBeadSlot(
                    t = gapEndT + i * safeStep + p * safeStep,
                    seedOffset = i + 1,
                    isActive = false
                )
            }
        }
        p < 0f -> {
            val q = -p // 0f..1f (RIGHT -> LEFT undo)
            // Left waiting stack (beads 0..4 shifting leftward to receive returning bead at gapStartT)
            for (i in 4 downTo 0) {
                slots += TasbihBeadSlot(
                    t = gapStartT - i * safeStep - q * safeStep,
                    seedOffset = -i,
                    isActive = false
                )
            }
            // Active returning bead sliding smoothly across the center gap from RIGHT -> LEFT
            slots += TasbihBeadSlot(
                t = gapEndT - (gapEndT - gapStartT) * q,
                seedOffset = 1,
                isActive = true
            )
            // Right remaining stack (beads 1..5 shifting leftward to fill gapEndT)
            for (i in 1..5) {
                slots += TasbihBeadSlot(
                    t = gapEndT + i * safeStep - q * safeStep,
                    seedOffset = i + 1,
                    isActive = false
                )
            }
        }
        else -> {
            // Resting state: 5 beads on left (gapStartT leftward), 5 beads on right (gapEndT rightward)
            for (i in 4 downTo 0) {
                slots += TasbihBeadSlot(
                    t = gapStartT - i * safeStep,
                    seedOffset = -i,
                    isActive = false
                )
            }
            for (i in 0..4) {
                slots += TasbihBeadSlot(
                    t = gapEndT + i * safeStep,
                    seedOffset = i + 1,
                    isActive = false
                )
            }
        }
    }

    return slots.sortedBy { it.t }
}

/**
 * Full-width hyper-realistic 3D Physical Horizontal Slightly-Angled Tasbih String:
 * - Taut luminous white/ivory silk thread gently ascending left-to-right across the entire viewport
 * - Zero-collision continuous bead conveyor: smooth finger hold-and-drag forward (+1) or backward (-1)
 */
@Composable
private fun HorizontalPhysical3DMisbahaString(
    beadCount: Int,
    glideFraction: Float,
    theme: TasbihBeadTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Gentle upward-arching taut white silk string across the full screen width
        val stringSpanX = w * 1.12f
        val stringStartX = -w * 0.06f
        fun pointOnString(t: Float): Offset {
            val x = stringStartX + t * stringSpanX
            val baseSlopeY = h * (0.64f - t * 0.24f)
            val subtleArch = -sin(t.coerceIn(0f, 1f) * PI).toFloat() * h * 0.052f
            return Offset(x, baseSlopeY + subtleArch)
        }

        // 1. Taut White Silk String with subtle upper highlight & drop shadow
        val stringPath = Path()
        val steps = 44
        for (i in 0..steps) {
            val p = pointOnString(i.toFloat() / steps.toFloat())
            if (i == 0) stringPath.moveTo(p.x, p.y) else stringPath.lineTo(p.x, p.y)
        }
        drawPath(
            path = stringPath,
            color = Color.Black.copy(alpha = 0.55f),
            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = stringPath,
            color = theme.cordColor.copy(alpha = 0.94f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = stringPath,
            color = Color.White.copy(alpha = 0.72f),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        )

        // Bead radius scaled gracefully to viewport width; strictly > 2.04x diameter step -> ZERO collisions!
        val beadRadius = (w * 0.068f).coerceIn(21.dp.toPx(), 30.dp.toPx())
        val beadStepT = (beadRadius * 2.06f) / stringSpanX

        val slots = computeTasbihBeadSlots(
            progress = glideFraction,
            beadStepT = beadStepT,
            gapStartT = 0.34f,
            gapEndT = 0.66f
        )

        // Draw non-active beads first, then active dragged/gliding bead cleanly on top
        for (slot in slots) {
            if (slot.isActive) continue
            if (slot.t < -0.14f || slot.t > 1.14f) continue
            drawRealistic3DBead(
                center = pointOnString(slot.t),
                radius = beadRadius,
                theme = theme,
                seed = (beadCount + slot.seedOffset) * 31 + 17,
                showCordHoles = true
            )
        }
        slots.firstOrNull { it.isActive }?.let { active ->
            val liftScale = 1f + 0.045f * sin(kotlin.math.abs(glideFraction).coerceIn(0f, 1f) * PI).toFloat()
            drawRealistic3DBead(
                center = pointOnString(active.t),
                radius = beadRadius * liftScale,
                theme = theme,
                seed = (beadCount + active.seedOffset) * 31 + 17,
                showCordHoles = true
            )
        }
    }
}

/**
 * Photorealistic 3D Spherical Bead Shader rendered natively on Compose Canvas:
 * Supports organic marbled Emerald Jade, concentric natural Wood Grain, Olive Wood,
 * translucent Golden Amber, reflective Silver Pearl, Obsidian Glass, Turquoise Veins, and Gem Crystal.
 */
private fun DrawScope.drawRealistic3DBead(
    center: Offset,
    radius: Float,
    theme: TasbihBeadTheme,
    seed: Int,
    showCordHoles: Boolean
) {
    // 1. Soft ambient drop shadow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent),
            center = Offset(center.x + radius * 0.12f, center.y + radius * 0.28f),
            radius = radius * 1.15f
        ),
        radius = radius * 1.12f,
        center = Offset(center.x + radius * 0.10f, center.y + radius * 0.24f)
    )

    val spherePath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(spherePath) {
        // 2. Base 3D Spherical Diffuse Gradient (light from upper-left)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to theme.highlightColor,
                    0.42f to theme.bodyColor,
                    0.82f to theme.shadowColor,
                    1.0f to Color(0xFF040706)
                ),
                center = Offset(center.x - radius * 0.30f, center.y - radius * 0.32f),
                radius = radius * 1.48f
            ),
            radius = radius,
            center = center
        )

        // 3. Procedural Realistic Surface Material Texture
        val phase = ((seed % 11) * 0.52f)
        when (theme.texture) {
            BeadSurfaceTexture.JADE_MARBLE -> {
                // Organic mottled mineral jade clouds + bright inner translucency
                for (m in 0..5) {
                    val ang = phase + m * 1.08f
                    val dist = radius * (0.22f + (m % 3) * 0.20f)
                    val blobCenter = Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist)
                    val blobCol = if (m % 2 == 0) theme.veinColor.copy(alpha = 0.42f) else theme.highlightColor.copy(alpha = 0.28f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blobCol, Color.Transparent),
                            center = blobCenter,
                            radius = radius * (0.45f + (m % 2) * 0.22f)
                        ),
                        radius = radius * 0.65f,
                        center = blobCenter
                    )
                }
            }

            BeadSurfaceTexture.WOOD_GRAIN, BeadSurfaceTexture.OLIVE_WOOD -> {
                // Realistic curved concentric hardwood growth rings & satin wood fibers
                val ringCount = if (theme.texture == BeadSurfaceTexture.WOOD_GRAIN) 7 else 5
                for (rIdx in 0 until ringCount) {
                    val yOffset = (rIdx - ringCount / 2f) * (radius * 0.30f) + sin(phase) * radius * 0.08f
                    val grainPath = Path().apply {
                        moveTo(center.x - radius * 1.1f, center.y + yOffset + radius * 0.14f)
                        quadraticTo(
                            center.x,
                            center.y + yOffset - radius * 0.24f,
                            center.x + radius * 1.1f,
                            center.y + yOffset + radius * 0.10f
                        )
                    }
                    drawPath(
                        path = grainPath,
                        color = theme.veinColor.copy(alpha = if (theme.texture == BeadSurfaceTexture.WOOD_GRAIN) 0.42f else 0.26f),
                        style = Stroke(width = (radius * 0.09f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
                    )
                }
            }

            BeadSurfaceTexture.METALLIC_PEARL -> {
                // Smooth metallic chrome/silver-pearl dual horizon reflection
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFF9EA6AE).copy(alpha = 0.35f),
                            Color(0xFF23282E).copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.55f)
                        ),
                        start = Offset(center.x - radius, center.y - radius),
                        end = Offset(center.x + radius, center.y + radius)
                    ),
                    radius = radius,
                    center = center
                )
            }

            BeadSurfaceTexture.OBSIDIAN_GLASS -> {
                // Deep obsidian mirror sheen with sharp curved glass reflection
                drawArc(
                    color = Color.White.copy(alpha = 0.22f),
                    startAngle = 195f,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                    style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round)
                )
            }

            BeadSurfaceTexture.GLOSSY_AMBER, BeadSurfaceTexture.CRYSTAL_GEM -> {
                // Translucent internal caustic bounce glow at the lower-right opposite the light source
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.highlightColor.copy(alpha = 0.52f), Color.Transparent),
                        center = Offset(center.x + radius * 0.22f, center.y + radius * 0.36f),
                        radius = radius * 0.68f
                    ),
                    radius = radius * 0.68f,
                    center = Offset(center.x + radius * 0.22f, center.y + radius * 0.36f)
                )
            }

            BeadSurfaceTexture.TURQUOISE_VEIN -> {
                // Fine mineral matrix veins across turquoise / lapis sphere
                val veinPath = Path().apply {
                    moveTo(center.x - radius * 0.75f, center.y - radius * 0.15f)
                    quadraticTo(center.x - radius * 0.1f, center.y + radius * 0.35f, center.x + radius * 0.75f, center.y - radius * 0.25f)
                }
                drawPath(
                    path = veinPath,
                    color = theme.veinColor.copy(alpha = 0.45f),
                    style = Stroke(width = (radius * 0.06f).coerceAtLeast(1.2f), cap = StrokeCap.Round)
                )
            }
        }

        // 4. Lower-Right Subtle Rim Bounce Light for realistic 3D volume
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.rimGlowColor.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(center.x + radius * 0.34f, center.y + radius * 0.42f),
                radius = radius * 0.62f
            ),
            radius = radius * 0.62f,
            center = Offset(center.x + radius * 0.34f, center.y + radius * 0.42f)
        )

        // 5. Spherical Limb Darkening (authentic 3D depth near edges)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.74f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.44f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // 6. Crisp Dual Specular Reflection (upper-left studio softbox highlight)
        val isMatteWood = theme.texture == BeadSurfaceTexture.WOOD_GRAIN || theme.texture == BeadSurfaceTexture.OLIVE_WOOD
        val specAlpha = if (isMatteWood) 0.32f else 0.78f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = specAlpha), Color.Transparent),
                center = Offset(center.x - radius * 0.30f, center.y - radius * 0.34f),
                radius = radius * 0.40f
            ),
            radius = radius * 0.40f,
            center = Offset(center.x - radius * 0.30f, center.y - radius * 0.34f)
        )
        if (!isMatteWood) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.11f,
                center = Offset(center.x - radius * 0.34f, center.y - radius * 0.36f)
            )
        }

        // 7. Tiny drill-hole shadow where the white silk thread enters & exits the sphere
        if (showCordHoles) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = radius * 0.11f,
                center = Offset(center.x - radius * 0.94f, center.y + radius * 0.08f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = radius * 0.11f,
                center = Offset(center.x + radius * 0.94f, center.y - radius * 0.08f)
            )
        }
    }
}

private fun formatTasbihDigits(raw: String, lang: AppLanguage): String {
    if (lang != AppLanguage.ARABIC) return raw
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return buildString(raw.length) {
        for (ch in raw) {
            if (ch in '0'..'9') append(arabicDigits[ch - '0']) else append(ch)
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
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = noorColors.goldAccent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
