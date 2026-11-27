package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.setValue
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

    // Physical sliding bead progress across the taut horizontal silk string (-1f = gliding forward, +1f = gliding backward)
    val beadGlideAnim = remember { Animatable(0f) }

    fun triggerForwardBead() {
        viewModel.incrementTasbih(beadTheme.timbre)
        scope.launch {
            beadGlideAnim.snapTo(-1f)
            beadGlideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun triggerBackwardBead() {
        if (liveCount <= 0) return
        viewModel.decrementTasbih(beadTheme.timbre)
        scope.launch {
            beadGlideAnim.snapTo(1f)
            beadGlideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
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

            // 2. Full Interactive Tap/Swipe Center Arena (tap anywhere to advance bead, swipe horizontally to decrease)
            var dragAccumX by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(liveCount, selectedThemeId) {
                        detectTapGestures { triggerForwardBead() }
                    }
                    .pointerInput(liveCount, selectedThemeId) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumX = 0f },
                            onDragEnd = {
                                if (kotlin.math.abs(dragAccumX) > 45f) {
                                    triggerBackwardBead()
                                }
                                dragAccumX = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumX += dragAmount
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

                // Gesture Instruction Caption matching screenshot
                Text(
                    text = L10n.pick(
                        lang,
                        "اضغط أي مكان للبدء\n(يمين-يسار سحب سينقص العدد)",
                        "Tap anywhere to begin\n(swipe left-right to decrease)",
                        "Boshlash uchun istalgan joyga bosing\n(o'ng-chapga surish sonni kamaytiradi)",
                        "Нажмите в любом месте\n(свайп влево-вправо уменьшит счёт)"
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
 * Full-width hyper-realistic 3D Physical Horizontal Slightly-Angled Tasbih String:
 * - Taut luminous white/ivory silk thread gently ascending left-to-right across the entire viewport
 * - Tight group of 4 photorealistic 3D beads on the left, clean open central string gap,
 *   and tight group of 3 photorealistic 3D beads on the right
 * - When tapped/swiped, a 3D bead smoothly slides across the open string gap and settles with spring physics
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

        // Gentle upward-arching taut white silk string across the full screen width (matching screenshot)
        fun pointOnString(t: Float): Offset {
            val x = -w * 0.04f + t * (w * 1.08f)
            val baseSlopeY = h * (0.64f - t * 0.25f)
            val subtleArch = -sin(t * PI).toFloat() * h * 0.055f
            return Offset(x, baseSlopeY + subtleArch)
        }

        // 1. Taut White Silk String with subtle upper highlight & drop shadow
        val stringPath = Path()
        val steps = 40
        for (i in 0..steps) {
            val p = pointOnString(i.toFloat() / steps.toFloat())
            if (i == 0) stringPath.moveTo(p.x, p.y) else stringPath.lineTo(p.x, p.y)
        }
        // Subtle string shadow
        drawPath(
            path = stringPath,
            color = Color.Black.copy(alpha = 0.55f),
            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
        )
        // Main luminous silk cord
        drawPath(
            path = stringPath,
            color = theme.cordColor.copy(alpha = 0.94f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Fine sheen thread
        drawPath(
            path = stringPath,
            color = Color.White.copy(alpha = 0.72f),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        )

        // Bead radius scaled gracefully to viewport width (~25-29dp on phones)
        val beadRadius = (w * 0.072f).coerceIn(22.dp.toPx(), 32.dp.toPx())
        // Exact spacing so adjacent spheres kiss tightly like real threaded beads
        val beadStepT = (beadRadius * 1.94f) / (w * 1.08f)

        // Left resting stack: 4 tightly packed beads (t ≈ 0.08 .. 0.37)
        // Right resting stack: 3 tightly packed beads (t ≈ 0.72 .. 0.92)
        // Wide open central taut silk cord gap between t ≈ 0.37 and t ≈ 0.72!
        val leftAnchorT = 0.37f
        val rightAnchorT = 0.73f

        // Compute animated positions depending on glideFraction:
        // glideFraction in -1f..0f -> forward click: bead slides smoothly from RIGHT cluster across gap into LEFT cluster
        // glideFraction in 0f..+1f -> backward swipe: bead slides smoothly from LEFT cluster across gap back to RIGHT cluster
        val movingProgress = if (glideFraction < 0f) {
            // 1.0 (just started at rightAnchorT) -> 0.0 (arrived at leftAnchorT)
            -glideFraction
        } else {
            glideFraction
        }

        // Draw static left stack beads (back-most 3 beads)
        for (i in 3 downTo 1) {
            val shift = if (glideFraction < 0f) (1f - movingProgress) * beadStepT else if (glideFraction > 0f) -movingProgress * beadStepT else 0f
            val t = leftAnchorT - i * beadStepT - shift
            val center = pointOnString(t)
            drawRealistic3DBead(
                center = center,
                radius = beadRadius,
                theme = theme,
                seed = beadCount + i * 17,
                showCordHoles = true
            )
        }

        // Draw static right stack beads (3 beads on the right)
        for (i in 0..2) {
            val shift = if (glideFraction < 0f) -movingProgress * beadStepT else if (glideFraction > 0f) (1f - movingProgress) * beadStepT else 0f
            val t = rightAnchorT + i * beadStepT + shift
            if (glideFraction < 0f && i == 0) continue // Bead 0 on right is currently the active sliding bead!
            val center = pointOnString(t)
            drawRealistic3DBead(
                center = center,
                radius = beadRadius,
                theme = theme,
                seed = beadCount - (i + 1) * 23,
                showCordHoles = true
            )
        }

        // Draw the lead/active gliding bead (glides cleanly across the open string gap!)
        val leadT = when {
            glideFraction < 0f -> {
                // Moves from rightAnchorT -> leftAnchorT as glideFraction goes -1f -> 0f
                val progress = 1f + glideFraction // 0f -> 1f
                rightAnchorT + (leftAnchorT - rightAnchorT) * progress
            }
            glideFraction > 0f -> {
                // Moves from leftAnchorT -> rightAnchorT as glideFraction goes +1f -> 0f
                val progress = 1f - glideFraction // 0f -> 1f
                leftAnchorT + (rightAnchorT - leftAnchorT) * progress
            }
            else -> leftAnchorT
        }
        drawRealistic3DBead(
            center = pointOnString(leadT),
            radius = beadRadius * (1f + 0.04f * kotlin.math.abs(glideFraction)),
            theme = theme,
            seed = beadCount * 31 + 7,
            showCordHoles = true
        )
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
