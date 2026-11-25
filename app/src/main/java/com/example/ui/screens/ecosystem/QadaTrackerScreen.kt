package com.example.ui.screens.ecosystem

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.prefs.QadaPrayerStore
import com.example.domain.qada.QadaPrayerEntry
import com.example.domain.qada.QadaPrayerType
import com.example.domain.qada.QadaTrackerEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlin.math.roundToInt

@Composable
fun QadaTrackerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { QadaPrayerStore(context) }
    val state by store.state.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var showPeriodDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editingPrayerType by remember { mutableStateOf<QadaPrayerType?>(null) }
    var includeWitrInDay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qada_tracker_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "متابع قضاء الصلوات", "Missed Prayers (Qada)", "Qazo namozlar kuzatuvchisi"),
            subtitle = L10n.pick(
                lang,
                "سجّل صلواتك الفائتة وتابع قضاءها يومياً بخطوات منظمة ومحفوظة",
                "Track and complete missed prayers steadily with offline persistence",
                "Qazo namozlaringizni tartibli ravishda hisoblab, o'qib boring"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Orbital Progress Summary Card
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = state.progressFraction
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 10.dp.toPx()
                                val inset = stroke / 2f
                                val arcSize = Size(size.width - stroke, size.height - stroke)
                                drawArc(
                                    color = Color.White.copy(alpha = 0.12f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(EmeraldBright, GoldPrimary, GoldBright, EmeraldBright)),
                                    startAngle = -90f,
                                    sweepAngle = (progress * 360f).coerceAtLeast(if (state.totalCompleted > 0) 6f else 0f),
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(state.progressFraction * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = L10n.pick(lang, "مُنجز", "Done", "O'qildi"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "المتبقي للقضاء: ${state.totalRemaining} صلاة",
                                    "Remaining: ${state.totalRemaining} prayers",
                                    "Qolgan qazo: ${state.totalRemaining} namoz"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "تم قضاؤها: ${state.totalCompleted} صلاة",
                                    "Completed: ${state.totalCompleted} prayers",
                                    "O'qib tugatilgan: ${state.totalCompleted} namoz"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            val etaDays = state.estimatedDaysToFinish
                            val etaLabel = when {
                                state.totalRemaining == 0 -> L10n.pick(lang, "بارك الله فيك! لا توجد فوائت مسجلة", "No missed prayers pending", "Qazo namozlari qolmadi!")
                                etaDays >= 365 -> {
                                    val yrs = etaDays / 365
                                    val remMonths = (etaDays % 365) / 30
                                    L10n.pick(lang, "المدة المتوقعة: ~$yrs سنة و $remMonths شهر", "Est. finish: ~${yrs}y ${remMonths}m", "Taxminiy muddat: ~$yrs yil $remMonths oy")
                                }
                                etaDays >= 30 -> {
                                    val months = etaDays / 30
                                    val remDays = etaDays % 30
                                    L10n.pick(lang, "المدة المتوقعة: ~$months شهر و $remDays يوم", "Est. finish: ~${months}m ${remDays}d", "Taxminiy muddat: ~$months oy $remDays kun")
                                }
                                else -> L10n.pick(lang, "المدة المتوقعة: $etaDays يوم", "Est. finish: $etaDays days", "Taxminiy muddat: $etaDays kun")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = etaLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Actions (Complete Full Day + Period Calculator + Daily Pace)
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { store.completeFullDay(includeWitrInDay) },
                                enabled = state.totalRemaining > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("qada_complete_day_btn")
                            ) {
                                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "قضيت يوماً كاملاً", "Complete 1 Day", "1 kunlik qazo"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { showPeriodDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("qada_add_period_btn")
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "حساب فترة فائتة", "Add Period", "Davr qo'shish"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = L10n.pick(lang, "خطة القضاء اليومية (صلوات/يوم):", "Daily Goal (prayers/day):", "Kunlik maqsad (namoz/kun):"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("qada_reset_btn")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(L10n.pick(lang, "تصفير", "Reset", "Nollash"))
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(1, 2, 3, 5, 10, 15)) { pace ->
                                val label = when (pace) {
                                    5 -> L10n.pick(lang, "٥ (يوم كامل)", "5 (1 Day)", "5 (1 kun)")
                                    10 -> L10n.pick(lang, "١٠ (يومان)", "10 (2 Days)", "10 (2 kun)")
                                    else -> "$pace"
                                }
                                LiquidGlassChip(
                                    label = label,
                                    selected = state.dailyPacePrayers == pace,
                                    onClick = { store.setDailyPace(pace) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Individual Prayer Cards
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "الصلوات الفائتة لكل فرض", "Prayers Breakdown", "Har bir namoz bo'yicha hisob")
                )
            }

            items(QadaPrayerType.entries, key = { it.key }) { type ->
                val entry = state.entries[type] ?: QadaPrayerEntry(type)
                QadaPrayerCard(
                    entry = entry,
                    onPrayedOne = { store.markPrayed(type) },
                    onAddOne = { store.adjustMissed(type, +1) },
                    onEditExact = { editingPrayerType = type }
                )
            }

            // 4. Encouraging Fiqh Note
            item {
                LiquidGlassCard {
                    Text(
                        text = L10n.pick(
                            lang,
                            "نصيحة عملية: أيسر طريقة لقضاء الفوائت الكثيرة هي صلاة فرض قضاء مع كل فرض حاضر (٥ صلوات يومياً)، فتقضي عاماً كاملاً خلال عام واحد بيُسر وثبات.",
                            "Practical Tip: The easiest consistent way to complete missed prayers is praying 1 Qada prayer alongside every daily obligatory prayer (5/day).",
                            "Amaliy maslahat: Ko'p qazo namozlarini ado etishning eng oson yo'li — har bir farz namozi bilan birga 1 ta qazo namozini o'qishdir."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    if (showPeriodDialog) {
        AddMissedPeriodDialog(
            onDismiss = { showPeriodDialog = false },
            onConfirm = { years, months, days, includeWitr ->
                includeWitrInDay = includeWitr
                store.addMissedPeriod(years, months, days, includeWitr)
                showPeriodDialog = false
            }
        )
    }

    editingPrayerType?.let { prayerType ->
        val currentRem = state.entries[prayerType]?.remaining ?: 0
        EditRemainingDialog(
            prayerType = prayerType,
            initialCount = currentRem,
            onDismiss = { editingPrayerType = null },
            onConfirm = { exact ->
                store.setRemainingExact(prayerType, exact)
                editingPrayerType = null
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(L10n.pick(lang, "تصفير سجل القضاء", "Reset Qada Tracker", "Qazo hisobini nollash")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "هل أنت متأكد من تصفير جميع الأعداد المتبقية والمنجزة؟",
                        "Are you sure you want to clear all remaining and completed counts?",
                        "Barcha qolgan va o'qilgan qazo hisoblarini tozalashni xohlaysizmi?"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.resetAll()
                    showResetDialog = false
                }) {
                    Text(L10n.pick(lang, "تصفير", "Reset", "Nollash"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
                }
            }
        )
    }
}

@Composable
private fun QadaPrayerCard(
    entry: QadaPrayerEntry,
    onPrayedOne: () -> Unit,
    onAddOne: () -> Unit,
    onEditExact: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val type = entry.type

    LiquidGlassCard(
        modifier = Modifier.testTag("qada_card_${type.key}"),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.16f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${type.rakahs}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.pick(lang, type.nameAr, type.nameEn, type.nameUz),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "${type.rakahs} ركعات · تم قضاء ${entry.completed}",
                            "${type.rakahs} rak'ahs · Completed ${entry.completed}",
                            "${type.rakahs} rakat · O'qilgan: ${entry.completed}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.remaining}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (entry.remaining == 0) EmeraldPrimary else noorColors.goldAccent
                    )
                    Text(
                        text = L10n.pick(lang, "متبقي", "left", "qoldi"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.totalLogged > 0) {
                LinearProgressIndicator(
                    progress = { entry.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPrayedOne,
                    enabled = entry.remaining > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("qada_prayed_${type.key}")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L10n.pick(lang, "قضيت صلاة (-1)", "Prayed (-1)", "O'qidim (-1)"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onAddOne,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(noorColors.glassSurfaceStrong)
                        .border(1.dp, noorColors.glassBorderBottom, RoundedCornerShape(14.dp))
                        .testTag("qada_add_${type.key}")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Missed", tint = noorColors.goldAccent)
                }

                IconButton(
                    onClick = onEditExact,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(noorColors.glassSurfaceStrong)
                        .border(1.dp, noorColors.glassBorderBottom, RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Count", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddMissedPeriodDialog(
    onDismiss: () -> Unit,
    onConfirm: (years: Int, months: Int, days: Int, includeWitr: Boolean) -> Unit
) {
    val lang = LocalAppLanguage.current
    var yearsText by remember { mutableStateOf("0") }
    var monthsText by remember { mutableStateOf("0") }
    var daysText by remember { mutableStateOf("7") }
    var includeWitr by remember { mutableStateOf(false) }

    val y = yearsText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val m = monthsText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val d = daysText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalDays = QadaTrackerEngine.calculateDaysFromPeriod(y, m, d)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(L10n.pick(lang, "إضافة فترة صلوات فائتة", "Add Missed Period", "Qazo davrini qo'shish"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearsText,
                        onValueChange = { yearsText = it.filter(Char::isDigit).take(2) },
                        label = { Text(L10n.pick(lang, "سنوات", "Years", "Yil")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it.filter(Char::isDigit).take(2) },
                        label = { Text(L10n.pick(lang, "أشهر", "Months", "Oy")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter(Char::isDigit).take(3) },
                        label = { Text(L10n.pick(lang, "أيام", "Days", "Kun")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.pick(lang, "تضمين صلاة الوتر", "Include Witr prayer", "Vitr namozini qo'shish"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = includeWitr, onCheckedChange = { includeWitr = it })
                }
                Text(
                    text = L10n.pick(
                        lang,
                        "سيتم إضافة $totalDays صلاة لكل فرض (${totalDays * if (includeWitr) 6 else 5} صلاة إجمالاً).",
                        "Adds $totalDays per prayer (${totalDays * if (includeWitr) 6 else 5} prayers total).",
                        "Har bir namozga $totalDays tadan qo'shiladi (jami ${totalDays * if (includeWitr) 6 else 5})."
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = totalDays > 0,
                onClick = { onConfirm(y, m, d, includeWitr) }
            ) {
                Text(L10n.pick(lang, "إضافة", "Add", "Qo'shish"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
            }
        }
    )
}

@Composable
private fun EditRemainingDialog(
    prayerType: QadaPrayerType,
    initialCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    var text by remember { mutableStateOf(initialCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                L10n.pick(
                    lang,
                    "تعديل عدد ${prayerType.nameAr}",
                    "Edit ${prayerType.nameEn} Count",
                    "${prayerType.nameUz} sonini tahrirlash"
                )
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(6) },
                label = { Text(L10n.pick(lang, "العدد المتبقي", "Remaining Count", "Qolgan soni")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull()?.coerceAtLeast(0) ?: 0) }) {
                Text(L10n.pick(lang, "حفظ", "Save", "Saqlash"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
            }
        }
    )
}
