package com.example.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

@Composable
fun KhatmAndWorshipScreen(
    viewModel: EcosystemViewModel,
    currentReaderPage: Int,
    onBack: () -> Unit,
    onOpenQuran: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val plans by viewModel.khatmPlans.collectAsStateWithLifecycle()
    val khatmLogs by viewModel.khatmLogs.collectAsStateWithLifecycle()
    val fastingLogs by viewModel.fastingLogs.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateKhatmDialog by remember { mutableStateOf(false) }

    val fridaySunnahs = remember {
        listOf(
            "غسل الجمعة والتطيب ولبس أحسن الثياب",
            "قراءة سورة الكهف كاملة",
            "الإكثار من الصلاة على النبي ﷺ",
            "التبكير إلى صلاة الجمعة والإنصات للخطبة",
            "الدعاء في ساعة الاستجابة بعد العصر"
        )
    }
    val fridayChecklist = remember { mutableStateMapOf<Int, Boolean>() }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "ختمة نور وتتبع الصيام", "NOOR Khatm & Fasting"),
            subtitle = L10n.pick(lang, "خطط ختم القرآن المتعددة · متابعة صيام السنة والقضاء · سنن الجمعة", "Multiple Khatm Plans, Sunnah Fasting & Friday Worship"),
            onBack = onBack
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { LiquidGlassChip(L10n.pick(lang, "خطط الختمة (${plans.size})", "Khatm Plans"), selectedTab == 0, { selectedTab = 0 }) }
            item { LiquidGlassChip(L10n.pick(lang, "متتبع الصيام (${fastingLogs.size})", "Fasting Tracker"), selectedTab == 1, { selectedTab = 1 }) }
            item { LiquidGlassChip(L10n.pick(lang, "سنن يوم الجمعة", "Friday Sunnahs"), selectedTab == 2, { selectedTab = 2 }) }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // 0: NOOR KHATM
                0 -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCreateKhatmDialog = true },
                                modifier = Modifier.weight(1f).testTag("create_khatm_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "ختمة جديدة", "New Khatm"))
                            }
                            OutlinedButton(onClick = onOpenQuran, modifier = Modifier.weight(1f)) {
                                Text(L10n.pick(lang, "افتح المصحف", "Open Mushaf"))
                            }
                        }
                    }

                    if (plans.isEmpty()) {
                        item {
                            LiquidGlassCard {
                                Text("أنشئ خطة ختمة للقرآن الكريم (مثلاً ٢٠ صفحة يومياً لختمة في ٣٠ يوماً أو ١٠ صفحات لختمة في شهرين).")
                            }
                        }
                    }

                    items(plans, key = { it.id }) { plan ->
                        val pct = ((plan.currentPage.toFloat() / plan.totalPages.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
                        val remainingPages = (plan.totalPages - plan.currentPage).coerceAtLeast(0)
                        LiquidGlassCard(strong = true, goldBorder = plan.isCompleted) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "الصفحة ${plan.currentPage} / ${plan.totalPages} · الجزء ${plan.currentJuz} · الإنجاز $pct%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = noorColors.goldAccent
                                    )
                                    Text(
                                        "الهدف اليومي: ${plan.dailyTargetPages} صفحات · المتبقي: $remainingPages صفحة · تاريخ الهدف: ${plan.targetDateKey}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteKhatmPlan(plan.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Khatm", tint = SemanticError)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.advanceKhatm(plan, plan.dailyTargetPages) },
                                    enabled = !plan.isCompleted,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                ) { Text(if (plan.isCompleted) "تمت الختمة بحمد الله ✓" else "+${plan.dailyTargetPages} صفحة اليوم") }

                                OutlinedButton(
                                    onClick = { viewModel.syncKhatmWithPage(plan, currentReaderPage) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("مزامنة ص $currentReaderPage")
                                }
                            }
                        }
                    }

                    if (khatmLogs.isNotEmpty()) {
                        item { NoorSectionHeader(title = "سجل قراءات الختمة الأخيرة") }
                        items(khatmLogs.take(10), key = { it.id }) { l ->
                            LiquidGlassCard {
                                Text("${l.dateKey} · قرأت ${l.pagesRead} صفحات (وصلت إلى ص ${l.reachedPage})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // 1: FASTING TRACKER
                1 -> {
                    item {
                        val today = viewModel.todayKey()
                        val sunnahCount = fastingLogs.count { it.fastType == "SUNNAH" }
                        val whiteCount = fastingLogs.count { it.fastType == "WHITE_DAY" }
                        val ramadanCount = fastingLogs.count { it.fastType == "RAMADAN" }
                        val qadaCount = fastingLogs.count { it.fastType == "QADA" }

                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text("ملخص الصيام المسجّل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                            Spacer(Modifier.height(6.dp))
                            Text("سنة (الإثنين والخميس): $sunnahCount · الأيام البيض: $whiteCount · رمضان: $ramadanCount · قضاء: $qadaCount", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            Text("سجّل صيام اليوم ($today):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Button(
                                        onClick = { viewModel.toggleFastingDay(today, "SUNNAH", "صيام الإثنين/الخميس") },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) { Text("سنة (إثنين/خميس)") }
                                }
                                item {
                                    OutlinedButton(onClick = { viewModel.toggleFastingDay(today, "WHITE_DAY", "الأيام البيض ١٣-١٥") }) {
                                        Text("الأيام البيض")
                                    }
                                }
                                item {
                                    OutlinedButton(onClick = { viewModel.toggleFastingDay(today, "RAMADAN", "صيام رمضان") }) {
                                        Text("رمضان")
                                    }
                                }
                                item {
                                    OutlinedButton(onClick = { viewModel.toggleFastingDay(today, "QADA", "قضاء صيام") }) {
                                        Text("قضاء")
                                    }
                                }
                            }
                        }
                    }

                    items(fastingLogs, key = { it.dateKey }) { log ->
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SemanticSuccess)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("${log.dateKey} — ${log.fastType}", fontWeight = FontWeight.Bold)
                                        if (log.notes.isNotBlank()) Text(log.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(onClick = { viewModel.toggleFastingDay(log.dateKey, log.fastType) }) {
                                    Text("حذف", color = SemanticError)
                                }
                            }
                        }
                    }
                }

                // 2: FRIDAY SUNNAH REMINDERS
                2 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text("قائمة سنن يوم الجمعة المبارك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                            Spacer(Modifier.height(8.dp))
                            fridaySunnahs.forEachIndexed { idx, label ->
                                val checked = fridayChecklist[idx] == true
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = { fridayChecklist[idx] = it })
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateKhatmDialog) {
        var title by remember { mutableStateOf("ختمة الشهر المبارك") }
        var dailyPages by remember { mutableStateOf("20") }
        var targetDate by remember { mutableStateOf(viewModel.todayKey()) }
        AlertDialog(
            onDismissRequest = { showCreateKhatmDialog = false },
            title = { Text("إنشاء خطة ختمة جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الختمة") }, singleLine = true)
                    OutlinedTextField(value = dailyPages, onValueChange = { dailyPages = it.filter(Char::isDigit) }, label = { Text("عدد الصفحات اليومي (مثلاً ٢٠)") }, singleLine = true)
                    OutlinedTextField(value = targetDate, onValueChange = { targetDate = it }, label = { Text("التاريخ المستهدف (YYYY-MM-DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createKhatmPlan(title, targetDate, dailyPages.toIntOrNull() ?: 20)
                    showCreateKhatmDialog = false
                }) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showCreateKhatmDialog = false }) { Text("إلغاء") } }
        )
    }
}
