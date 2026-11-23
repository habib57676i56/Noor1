package com.example.ui.screens.ecosystem

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
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MenuBook
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
import com.example.data.local.db.FastingLogEntity
import com.example.data.local.db.KhatmPlanEntity
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
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.viewmodel.EcosystemViewModel

@Composable
fun KhatmAndWorshipScreen(
    viewModel: EcosystemViewModel,
    currentReaderPage: Int,
    onBack: () -> Unit,
    onOpenQuran: () -> Unit,
    globalConfig: GlobalAppConfigDoc = GlobalAppConfigDoc()
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val plans by viewModel.khatmPlans.collectAsStateWithLifecycle()
    val khatmLogs by viewModel.khatmLogs.collectAsStateWithLifecycle()
    val fastingLogs by viewModel.fastingLogs.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateKhatmDialog by remember { mutableStateOf(false) }
    var showCreateFastDialog by remember { mutableStateOf(false) }
    var pendingExtraWirdPlan by remember { mutableStateOf<KhatmPlanEntity?>(null) }
    var pendingDeleteFast by remember { mutableStateOf<FastingLogEntity?>(null) }

    val todayKey = remember { viewModel.todayKey() }

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

    Column(modifier = Modifier.fillMaxSize().testTag("khatm_worship_screen")) {
        NoorScreenHeader(
            title = L10n.pick(lang, "ختمة نور وتتبع الصيام", "NOOR Khatm & Fasting"),
            subtitle = L10n.pick(
                lang,
                "خطط ختم القرآن والورد اليومي · متابعة صيام السنة والقضاء · سنن الجمعة",
                "Quran Khatm & Daily Wird · Sunnah & Qada Fasting · Friday Sunnahs"
            ),
            onBack = onBack
        )

        statusBanner?.let { msg ->
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                goldBorder = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SemanticSuccess,
                        modifier = Modifier.weight(1f).testTag("khatm_status_banner")
                    )
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text(L10n.pick(lang, "حسناً", "OK"))
                    }
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "tab_khatm") {
                LiquidGlassChip(
                    L10n.pick(lang, "خطط الختمة (${plans.size})", "Khatm Plans (${plans.size})"),
                    selectedTab == 0,
                    { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_khatm_plans")
                )
            }
            item(key = "tab_fasting") {
                LiquidGlassChip(
                    L10n.pick(lang, "متتبع الصيام (${fastingLogs.size})", "Fasting Tracker (${fastingLogs.size})"),
                    selectedTab == 1,
                    { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_fasting_tracker")
                )
            }
            item(key = "tab_friday") {
                LiquidGlassChip(
                    L10n.pick(lang, "سنن يوم الجمعة", "Friday Sunnahs"),
                    selectedTab == 2,
                    { selectedTab = 2 },
                    modifier = Modifier.testTag("tab_friday_sunnahs")
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
                // 0: NOOR KHATM & DAILY WIRD
                0 -> {
                    if (!globalConfig.featureKhatmahEnabled) {
                        item(key = "khatm_admin_disabled") {
                            LiquidGlassCard(goldBorder = true) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "قسم خطط الختمة متوقف مؤقتاً من قِبل الإدارة للصيانة أو التحديث.",
                                        "The Khatmah Tracker is temporarily paused by the Super Admin."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                    if (!globalConfig.featureDailyWirdEnabled) {
                        item(key = "wird_admin_disabled") {
                            LiquidGlassCard {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "تنبيه: تسجيل الورد اليومي متوقف مؤقتاً من إعدادات الإدارة.",
                                        "Note: Daily Wird logging is temporarily paused in Super Admin settings."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item(key = "khatm_action_bar") {
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
                            OutlinedButton(
                                onClick = onOpenQuran,
                                modifier = Modifier.weight(1f).testTag("khatm_open_mushaf_btn")
                            ) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "افتح المصحف", "Open Mushaf"))
                            }
                        }
                    }

                    // Daily Wird summary KPI card
                    item(key = "khatm_daily_summary") {
                        val todayLogs = khatmLogs.filter { it.dateKey == todayKey }
                        val pagesReadToday = todayLogs.sumOf { it.pagesRead }
                        val completedPlansCount = plans.count { it.isCompleted }
                        LiquidGlassCard(goldBorder = pagesReadToday > 0) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "إنجاز الورد اليومي ($todayKey): $pagesReadToday صفحة · ختمات مكتملة: $completedPlansCount",
                                    "Daily Wird ($todayKey): $pagesReadToday pages · Completed Khatms: $completedPlansCount"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (pagesReadToday > 0) SemanticSuccess else noorColors.goldAccent
                            )
                        }
                    }

                    if (plans.isEmpty()) {
                        item(key = "khatm_empty_card") {
                            LiquidGlassCard {
                                Text(
                                    L10n.pick(
                                        lang,
                                        "أنشئ خطة ختمة للقرآن الكريم (مثلاً ٢٠ صفحة يومياً لختمة في ٣٠ يوماً أو ١٠ صفحات لختمة في شهرين) وتابع وردك اليومي بسهولة.",
                                        "Create a Quran Khatm plan (e.g., 20 pages/day for 30 days) and track your Daily Wird."
                                    )
                                )
                            }
                        }
                    }

                    // CRITICAL FIX: Distinct prefixed keys ("khatm_plan_...") prevent LazyColumn key collision
                    // with khatm_reading_logs ("khatm_log_...") that previously crashed the app on completion!
                    items(plans, key = { "khatm_plan_${it.id}" }) { plan ->
                        val pct = ((plan.currentPage.toFloat() / plan.totalPages.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
                        val remainingPages = (plan.totalPages - plan.currentPage).coerceAtLeast(0)
                        val alreadyLoggedToday = khatmLogs.any { it.planId == plan.id && it.dateKey == todayKey }

                        LiquidGlassCard(strong = true, goldBorder = plan.isCompleted || alreadyLoggedToday) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        L10n.pick(
                                            lang,
                                            "الصفحة ${plan.currentPage} / ${plan.totalPages} · الجزء ${plan.currentJuz} · الإنجاز $pct%",
                                            "Page ${plan.currentPage} / ${plan.totalPages} · Juz ${plan.currentJuz} · $pct%"
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = noorColors.goldAccent
                                    )
                                    Text(
                                        L10n.pick(
                                            lang,
                                            "الورد اليومي: ${plan.dailyTargetPages} صفحات · المتبقي: $remainingPages صفحة · الهدف: ${plan.targetDateKey}",
                                            "Daily Wird: ${plan.dailyTargetPages} pages · Remaining: $remainingPages · Target: ${plan.targetDateKey}"
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (alreadyLoggedToday && !plan.isCompleted) {
                                        Text(
                                            text = L10n.pick(lang, "✓ تم تسجيل ورد اليوم لهذه الختمة", "✓ Today's Daily Wird logged"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SemanticSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteKhatmPlan(plan.id) },
                                    modifier = Modifier.testTag("delete_khatm_${plan.id}")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Khatm", tint = SemanticError)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (alreadyLoggedToday) {
                                            pendingExtraWirdPlan = plan
                                        } else {
                                            viewModel.advanceKhatm(plan, plan.dailyTargetPages, allowExtraSameDay = false)
                                        }
                                    },
                                    enabled = !plan.isCompleted && globalConfig.featureDailyWirdEnabled,
                                    modifier = Modifier.weight(1f).testTag("complete_khatm_wird_${plan.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                ) {
                                    Icon(Icons.Rounded.DoneAll, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        when {
                                            plan.isCompleted -> L10n.pick(lang, "تمت الختمة بحمد الله ✓", "Khatm Complete ✓")
                                            alreadyLoggedToday -> L10n.pick(lang, "ورد إضافي (+${plan.dailyTargetPages} ص)", "Extra Wird (+${plan.dailyTargetPages}p)")
                                            else -> L10n.pick(lang, "إتمام ورد اليوم (+${plan.dailyTargetPages} ص)", "Complete Daily Wird (+${plan.dailyTargetPages}p)")
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.syncKhatmWithPage(plan, currentReaderPage) },
                                    modifier = Modifier.weight(1f).testTag("sync_khatm_${plan.id}")
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "مزامنة ص $currentReaderPage", "Sync p.$currentReaderPage"))
                                }
                            }
                        }
                    }

                    if (khatmLogs.isNotEmpty()) {
                        item(key = "khatm_logs_header") {
                            NoorSectionHeader(title = L10n.pick(lang, "سجل قراءات الختمة الأخيرة", "Recent Khatm & Wird Logs"))
                        }
                        items(khatmLogs.take(15), key = { "khatm_log_${it.id}" }) { l ->
                            LiquidGlassCard {
                                Text(
                                    L10n.pick(
                                        lang,
                                        "${l.dateKey} · قرأت ${l.pagesRead} صفحات (وصلت إلى ص ${l.reachedPage})",
                                        "${l.dateKey} · Read ${l.pagesRead} pages (reached p.${l.reachedPage})"
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    }
                }

                // 1: FASTING TRACKER
                1 -> {
                    if (!globalConfig.featureFastingEnabled) {
                        item(key = "fasting_admin_disabled") {
                            LiquidGlassCard(goldBorder = true) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "قسم متتبع الصيام متوقف مؤقتاً من قِبل الإدارة للصيانة أو التحديث.",
                                        "The Fasting Tracker is temporarily paused by the Super Admin."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                    item(key = "fasting_summary_card") {
                        val sunnahCount = fastingLogs.count { it.fastType == "SUNNAH" && it.completed }
                        val whiteCount = fastingLogs.count { it.fastType == "WHITE_DAY" && it.completed }
                        val ramadanCount = fastingLogs.count { it.fastType == "RAMADAN" && it.completed }
                        val qadaCount = fastingLogs.count { it.fastType == "QADA" && it.completed }
                        val todayLogged = fastingLogs.firstOrNull { it.dateKey == todayKey }

                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    L10n.pick(lang, "ملخص الصيام المكتمل", "Completed Fasting Summary"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                OutlinedButton(
                                    onClick = { showCreateFastDialog = true },
                                    modifier = Modifier.testTag("create_custom_fast_btn")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "إضافة يوم", "Log Date"))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                L10n.pick(
                                    lang,
                                    "سنة: $sunnahCount · الأيام البيض: $whiteCount · رمضان: $ramadanCount · قضاء: $qadaCount",
                                    "Sunnah: $sunnahCount · White Days: $whiteCount · Ramadan: $ramadanCount · Qada: $qadaCount"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(10.dp))
                            if (todayLogged != null) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "✓ تم إتمام وتسجيل صيام اليوم ($todayKey — ${fastTypeLabelAr(todayLogged.fastType)})",
                                        "✓ Today's fast completed ($todayKey — ${todayLogged.fastType})"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SemanticSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    L10n.pick(lang, "تأكيد إتمام صيام اليوم ($todayKey):", "Confirm completion of today's fast ($todayKey):"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item(key = "fast_btn_sunnah") {
                                    Button(
                                        onClick = { viewModel.completeFastingDay(todayKey, "SUNNAH", "صيام الإثنين/الخميس") },
                                        modifier = Modifier.testTag("complete_fast_sunnah_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) { Text(L10n.pick(lang, "سنة (إثنين/خميس)", "Sunnah")) }
                                }
                                item(key = "fast_btn_white") {
                                    OutlinedButton(
                                        onClick = { viewModel.completeFastingDay(todayKey, "WHITE_DAY", "الأيام البيض ١٣-١٥") },
                                        modifier = Modifier.testTag("complete_fast_white_btn")
                                    ) {
                                        Text(L10n.pick(lang, "الأيام البيض", "White Days"))
                                    }
                                }
                                item(key = "fast_btn_ramadan") {
                                    OutlinedButton(
                                        onClick = { viewModel.completeFastingDay(todayKey, "RAMADAN", "صيام رمضان") },
                                        modifier = Modifier.testTag("complete_fast_ramadan_btn")
                                    ) {
                                        Text(L10n.pick(lang, "رمضان", "Ramadan"))
                                    }
                                }
                                item(key = "fast_btn_qada") {
                                    OutlinedButton(
                                        onClick = { viewModel.completeFastingDay(todayKey, "QADA", "قضاء صيام") },
                                        modifier = Modifier.testTag("complete_fast_qada_btn")
                                    ) {
                                        Text(L10n.pick(lang, "قضاء", "Qada"))
                                    }
                                }
                            }
                        }
                    }

                    if (fastingLogs.isEmpty()) {
                        item(key = "fasting_empty_card") {
                            LiquidGlassCard {
                                Text(
                                    L10n.pick(
                                        lang,
                                        "لم تُسجّل أيام صيام بعد. اختر نوع الصيام أعلاه لتسجيل وإتمام صيام اليوم.",
                                        "No fasting days recorded yet. Choose a fasting type above to confirm completion."
                                    )
                                )
                            }
                        }
                    }

                    items(fastingLogs, key = { "fast_${it.dateKey}" }) { log ->
                        LiquidGlassCard {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SemanticSuccess)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("${log.dateKey} — ${fastTypeLabelAr(log.fastType)}", fontWeight = FontWeight.Bold)
                                        if (log.notes.isNotBlank()) {
                                            Text(log.notes, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { pendingDeleteFast = log },
                                    modifier = Modifier.testTag("delete_fast_${log.dateKey}")
                                ) {
                                    Text(L10n.pick(lang, "حذف", "Remove"), color = SemanticError)
                                }
                            }
                        }
                    }
                    }
                }

                // 2: FRIDAY SUNNAH REMINDERS
                2 -> {
                    item(key = "friday_sunnahs_card") {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text(
                                L10n.pick(lang, "قائمة سنن يوم الجمعة المبارك", "Blessed Friday Sunnahs Checklist"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
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

    // Create Khatm Plan Dialog
    if (showCreateKhatmDialog) {
        var title by remember { mutableStateOf("ختمة الشهر المبارك") }
        var dailyPages by remember { mutableStateOf("20") }
        var targetDate by remember { mutableStateOf(todayKey) }
        AlertDialog(
            onDismissRequest = { showCreateKhatmDialog = false },
            title = { Text(L10n.pick(lang, "إنشاء خطة ختمة جديدة", "Create New Khatm Plan")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(L10n.pick(lang, "اسم الختمة", "Plan Name")) },
                        singleLine = true,
                        modifier = Modifier.testTag("khatm_title_input")
                    )
                    OutlinedTextField(
                        value = dailyPages,
                        onValueChange = { dailyPages = it.filter(Char::isDigit) },
                        label = { Text(L10n.pick(lang, "عدد الصفحات اليومي (مثلاً ٢٠)", "Daily Target Pages")) },
                        singleLine = true,
                        modifier = Modifier.testTag("khatm_pages_input")
                    )
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text(L10n.pick(lang, "التاريخ المستهدف (YYYY-MM-DD)", "Target Date (YYYY-MM-DD)")) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createKhatmPlan(title, targetDate, dailyPages.toIntOrNull() ?: 20)
                        showCreateKhatmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_khatm_btn")
                ) { Text(L10n.pick(lang, "إنشاء", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateKhatmDialog = false }) { Text(L10n.pick(lang, "إلغاء", "Cancel")) }
            }
        )
    }

    // Extra Same-Day Wird Confirmation Dialog (prevents accidental duplicate completions)
    pendingExtraWirdPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingExtraWirdPlan = null },
            title = { Text(L10n.pick(lang, "تم تسجيل ورد اليوم مسبقاً", "Daily Wird Already Logged Today")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "لقد أتممت ورد اليوم لخطة «${plan.title}» مسبقاً. هل ترغب في إضافة ورد إضافي (+${plan.dailyTargetPages} صفحة)؟",
                        "You already completed today's Daily Wird for \"${plan.title}\". Would you like to log an extra +${plan.dailyTargetPages} pages?"
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.advanceKhatm(plan, plan.dailyTargetPages, allowExtraSameDay = true)
                    pendingExtraWirdPlan = null
                }) { Text(L10n.pick(lang, "إضافة ورد إضافي", "Add Extra Wird")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingExtraWirdPlan = null }) { Text(L10n.pick(lang, "إلغاء", "Cancel")) }
            }
        )
    }

    // Create Custom Date Fasting Task Dialog
    if (showCreateFastDialog) {
        var dateInput by remember { mutableStateOf(todayKey) }
        var fastType by remember { mutableStateOf("SUNNAH") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFastDialog = false },
            title = { Text(L10n.pick(lang, "تسجيل يوم صيام", "Record Fasting Day")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text(L10n.pick(lang, "التاريخ (YYYY-MM-DD)", "Date (YYYY-MM-DD)")) },
                        singleLine = true,
                        modifier = Modifier.testTag("fast_date_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val types = listOf("SUNNAH", "WHITE_DAY", "RAMADAN", "QADA")
                        items(types, key = { "ftype_$it" }) { t ->
                            LiquidGlassChip(
                                label = fastTypeLabelAr(t),
                                selected = fastType == t,
                                onClick = { fastType = t }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(L10n.pick(lang, "ملاحظة (اختياري)", "Notes (optional)")) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeFastingDay(dateInput.ifBlank { todayKey }, fastType, notes)
                        showCreateFastDialog = false
                    },
                    modifier = Modifier.testTag("confirm_custom_fast_btn")
                ) { Text(L10n.pick(lang, "تأكيد إتمام الصيام", "Confirm Completed")) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFastDialog = false }) { Text(L10n.pick(lang, "إلغاء", "Cancel")) }
            }
        )
    }

    // Delete Fasting Log Confirmation Dialog
    pendingDeleteFast?.let { fast ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFast = null },
            title = { Text(L10n.pick(lang, "إزالة سجل الصيام", "Remove Fasting Record")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "هل تريد حذف سجل صيام يوم ${fast.dateKey} (${fastTypeLabelAr(fast.fastType)})؟",
                        "Remove fasting record for ${fast.dateKey} (${fast.fastType})?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFastingDay(fast.dateKey)
                        pendingDeleteFast = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticError)
                ) { Text(L10n.pick(lang, "حذف", "Remove")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFast = null }) { Text(L10n.pick(lang, "إلغاء", "Cancel")) }
            }
        )
    }
}

private fun fastTypeLabelAr(type: String): String = when (type) {
    "SUNNAH" -> "سنة (إثنين/خميس)"
    "WHITE_DAY" -> "الأيام البيض"
    "RAMADAN" -> "رمضان"
    "QADA" -> "قضاء صيام"
    else -> type
}
