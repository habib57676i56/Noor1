package com.example.ui.screens.ecosystem

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.LocalAdminRole
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
import kotlinx.coroutines.launch

@Composable
fun OfflineSuperAdminScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasSuperAdmin by viewModel.hasSuperAdmin.collectAsStateWithLifecycle()
    val session by viewModel.adminSession.collectAsStateWithLifecycle()
    val allKb by viewModel.allAiKnowledge.collectAsStateWithLifecycle()
    val books by viewModel.libraryBooks.collectAsStateWithLifecycle()
    val circles by viewModel.circles.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val assessments by viewModel.assessments.collectAsStateWithLifecycle()
    val quizzes by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val khatmPlans by viewModel.khatmPlans.collectAsStateWithLifecycle()
    val localAdmins by viewModel.localAdmins.collectAsStateWithLifecycle()
    val auditLogs by viewModel.localAuditLogs.collectAsStateWithLifecycle()
    val errorLogs by viewModel.systemErrorLogs.collectAsStateWithLifecycle()
    val backups by viewModel.backupSnapshots.collectAsStateWithLifecycle()

    // SAF launchers for Knowledge import, Full Backup Export, and Full Backup Restore
    val importKnowledgeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "knowledge.txt"
                if (text.isNotBlank()) {
                    viewModel.importKnowledgeFileContent(text, fileName, "GENERAL")
                }
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.exportLocalBackupJson()
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (json.isNotBlank()) viewModel.restoreLocalBackupJson(json)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الإدارة العامة المحلية (NOOR Admin)", "Offline Local Super Admin"),
            subtitle = if (session.isSignedIn) {
                "${session.account?.displayName} · ${session.role.labelAr}"
            } else {
                L10n.pick(lang, "إدارة محلية آمنة بالكامل بدون خادم سحابي", "100% Offline Local Administration")
            },
            onBack = onBack,
            trailing = {
                if (session.isSignedIn) {
                    IconButton(onClick = { viewModel.signOutLocalAdmin() }) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Sign Out Admin", tint = SemanticError)
                    }
                }
            }
        )

        if (!session.isSignedIn) {
            LocalAdminAuthGate(
                isFirstRunSetup = hasSuperAdmin == false,
                onSetupSuperAdmin = { user, name, pass -> viewModel.setupInitialSuperAdmin(user, name, pass) },
                onSignIn = { user, pass -> viewModel.signInLocalAdmin(user, pass) }
            )
            return
        }

        val role = session.role
        var selectedTab by remember { mutableIntStateOf(0) }
        var showAddKbDialog by remember { mutableStateOf(false) }
        var showAddQuizDialog by remember { mutableStateOf(false) }
        var showAddStaffDialog by remember { mutableStateOf(false) }

        val tabs = listOf(
            "الإحصائيات",
            "معرفة الذكاء (${allKb.size})",
            "المسابقات (${quizzes.size})",
            "الحسابات (${localAdmins.size})",
            "النسخ والسجلات"
        )

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.size) { idx ->
                LiquidGlassChip(tabs[idx], selectedTab == idx, { selectedTab = idx }, Modifier.testTag("offline_admin_tab_$idx"))
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // 0: Dashboard & Analytics
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text("لوحة الإحصائيات المحلية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                            Spacer(Modifier.height(8.dp))
                            Text("• قاعدة المعرفة المحلية: ${allKb.size} (معتمد: ${allKb.count { it.approvalStatus == "APPROVED" }} · بانتظار المراجعة: ${allKb.count { it.approvalStatus == "PENDING" }})")
                            Text("• المكتبة الإسلامية: ${books.size} كتاب/مرجع")
                            Text("• الحلقات: ${circles.size} · الطلاب: ${students.size} · المعلمون: ${teachers.size} · التقييمات: ${assessments.size}")
                            Text("• بنك المسابقات: ${quizzes.size} سؤال · خطط الختمة: ${khatmPlans.size}")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "ملاحظة تقنية شفافة: هذه اللوحة تدير بيانات هذا الجهاز محلياً بالكامل وتصدّر/تستورد حزم JSON لمشاركتها بين الأجهزة دون خادم سحابي. وهي لا تتحكم عن بُعد بأجهزة غير متصلة بخادم مزامنة.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1: AI Knowledge Management (Q&A + Import TXT/JSON + Approval + Reindex)
                1 -> {
                    item {
                        if (!role.canManageAiKnowledge()) {
                            LiquidGlassCard { Text("تحتاج صلاحية «مدير النظام العام» أو «محرر المحتوى» لإدارة قاعدة المعرفة.") }
                        } else {
                            LiquidGlassCard(strong = true, goldBorder = true) {
                                Text("إدارة فهرس المعرفة المحلية للذكاء", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "تحديث أو استيراد الملفات يضيفها إلى فهرس البحث المحلي (RAG) مع كشف التكرار ولا يعني إعادة تدريب نموذج لغوي عصبي على الجهاز.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showAddKbDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("إضافة سؤال/جواب")
                                    }
                                    OutlinedButton(
                                        onClick = { importKnowledgeLauncher.launch(arrayOf("text/plain", "application/json", "*/*")) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("استيراد TXT/JSON")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = { viewModel.reindexKnowledgeBase() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("إعادة فهرسة قاعدة المعرفة المحلية")
                                }
                            }
                        }
                    }
                    items(allKb, key = { it.id }) { kb ->
                        LiquidGlassCard(goldBorder = kb.approvalStatus == "PENDING") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${kb.titleAr} [${kb.category}]", fontWeight = FontWeight.Bold)
                                    Text("الحالة: ${kb.approvalStatus} · المصدر: ${kb.sourceRef}", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                    Text(kb.questionAr, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                                if (role.canApproveContent()) {
                                    IconButton(onClick = {
                                        val next = if (kb.approvalStatus == "APPROVED") "PENDING" else "APPROVED"
                                        viewModel.setAiKnowledgeApproval(kb.id, next)
                                    }) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Approve",
                                            tint = if (kb.approvalStatus == "APPROVED") SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (role.canManageAiKnowledge()) {
                                    IconButton(onClick = { viewModel.deleteAiKnowledge(kb.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete KB", tint = SemanticError)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2: Quiz Management
                2 -> {
                    item {
                        Button(
                            onClick = { showAddQuizDialog = true },
                            enabled = role.canManageQuizzes(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة سؤال مسابقة جديد")
                        }
                    }
                    items(quizzes, key = { it.id }) { q ->
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("[${q.category} · ${q.difficulty}] ${q.questionAr}", fontWeight = FontWeight.Bold)
                                    Text("المصدر: ${q.sourceRef}", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                }
                                if (role.canManageQuizzes()) {
                                    IconButton(onClick = { viewModel.deleteQuizQuestion(q.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Quiz", tint = SemanticError)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3: Staff & Role-Based Accounts
                3 -> {
                    item {
                        if (!role.canManageAccounts()) {
                            LiquidGlassCard { Text("إدارة الحسابات والصلاحيات مخصصة للمدير العام فقط (SUPER_ADMIN).") }
                        } else {
                            Button(
                                onClick = { showAddStaffDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("إنشاء حساب معلم أو محرر محلي")
                            }
                        }
                    }
                    items(localAdmins, key = { it.id }) { acct ->
                        val r = LocalAdminRole.fromKey(acct.role)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${acct.displayName} (@${acct.username})", fontWeight = FontWeight.Bold)
                                    Text("الدور: ${r.labelAr} (${r.key})", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                }
                                if (role.canManageAccounts() && acct.id != session.account?.id) {
                                    IconButton(onClick = { viewModel.deleteStaffAccount(acct.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove Account", tint = SemanticError)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4: Backup / Restore & Audit / Error Logs
                4 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text("النسخ الاحتياطي ونقل الحزم بين الأجهزة (JSON)", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "صدّر نسخة كاملة من قاعدة المعرفة والحلقات والطلاب والكتب والمسابقات والملاحظات كملف JSON آمن لاستيراده على أي جهاز آخر.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { exportBackupLauncher.launch("noor_ecosystem_backup_${viewModel.todayKey()}.json") },
                                    enabled = role.canBackupAndRestore(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                ) {
                                    Icon(Icons.Rounded.Backup, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("تصدير JSON")
                                }
                                OutlinedButton(
                                    onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                    enabled = role.canBackupAndRestore(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("استيراد JSON")
                                }
                            }
                            if (backups.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("النسخ المحفوظة محلياً: ${backups.size}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (role.canViewAuditLogs()) {
                        item { NoorSectionHeader(title = "سجل التدقيق الأمني (${auditLogs.size})") }
                        items(auditLogs.take(15), key = { it.id }) { log ->
                            LiquidGlassCard {
                                Text("[${log.category}] ${log.action} — ${log.actorUid}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                if (log.details.isNotBlank()) Text(log.details, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (errorLogs.isNotEmpty()) {
                            item { NoorSectionHeader(title = "سجل الأخطاء (${errorLogs.size})") }
                            items(errorLogs.take(10), key = { it.id }) { err ->
                                LiquidGlassCard {
                                    Text("[${err.tag}] ${err.message}", style = MaterialTheme.typography.bodySmall, color = SemanticError)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showAddKbDialog) {
            AddKnowledgeDialog(
                onDismiss = { showAddKbDialog = false },
                onSave = { cat, title, q, a, src, notes ->
                    viewModel.addOrUpdateAiKnowledge(
                        category = cat,
                        titleAr = title,
                        questionAr = q,
                        answerAr = a,
                        sourceRef = src,
                        scholarlyNotes = notes,
                        verificationStatus = "VERIFIED",
                        approvalStatus = if (role == LocalAdminRole.SUPER_ADMIN) "APPROVED" else "PENDING"
                    )
                    showAddKbDialog = false
                }
            )
        }
        if (showAddQuizDialog) {
            AddQuizDialog(
                onDismiss = { showAddQuizDialog = false },
                onSave = { cat, diff, q, a, b, c, d, idx, exp, src ->
                    viewModel.addCustomQuizQuestion(cat, diff, q, a, b, c, d, idx, exp, src)
                    showAddQuizDialog = false
                }
            )
        }
        if (showAddStaffDialog) {
            AddStaffDialog(
                onDismiss = { showAddStaffDialog = false },
                onSave = { u, name, pass, r ->
                    viewModel.createStaffAccount(u, name, pass, r)
                    showAddStaffDialog = false
                }
            )
        }
    }
}

@Composable
private fun LocalAdminAuthGate(
    isFirstRunSetup: Boolean,
    onSetupSuperAdmin: (String, String, String) -> Unit,
    onSignIn: (String, String) -> Unit
) {
    val noorColors = LocalNoorColors.current
    var username by remember { mutableStateOf(if (isFirstRunSetup) "admin" else "") }
    var displayName by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassCard(strong = true, goldBorder = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = noorColors.goldAccent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFirstRunSetup) "تفعيل حساب المدير العام المحلي (أول مرة)" else "تسجيل الدخول للإدارة المحلية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isFirstRunSetup) {
                    "لا توجد كلمات مرور افتراضية مخزّنة في التطبيق. أنشئ الآن حساب المدير العام المحلي بكلمة مرور خاصة بك (تُحفظ مشفرة محلياً بخوارزمية PBKDF2-SHA256)."
                } else {
                    "أدخل اسم المستخدم ورمز المرور الخاص بحسابك (مدير عام / معلم / محرر)."
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("local_admin_username")
            )
            if (isFirstRunSetup) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم الظاهر (مثلاً: الشيخ محمد)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text("رمز المرور (٦ أحرف أو أرقام على الأقل)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("local_admin_passcode")
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (isFirstRunSetup) onSetupSuperAdmin(username, displayName, passcode)
                    else onSignIn(username, passcode)
                },
                modifier = Modifier.fillMaxWidth().testTag("local_admin_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
            ) {
                Text(if (isFirstRunSetup) "إنشاء وتفعيل المدير العام" else "دخول لوحة الإدارة")
            }
        }
    }
}

@Composable
private fun AddKnowledgeDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var category by remember { mutableStateOf("QURAN") }
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مادة معرفية موثّقة") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("QURAN", "HADITH", "FIQH", "AQEEDAH", "TAJWEED", "SEERAH", "WORSHIP")) { c ->
                        LiquidGlassChip(c, category == c, { category = c })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("العنوان") }, singleLine = true)
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("السؤال") })
                OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("الإجابة الموثّقة") }, minLines = 3)
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("المصدر والمرجع الشرعي") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات الخلاف الفقهي (اختياري)") })
            }
        },
        confirmButton = { Button(onClick = { onSave(category, title, question, answer, source, notes) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AddQuizDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Int, String, String) -> Unit
) {
    var category by remember { mutableStateOf("QURAN") }
    var difficulty by remember { mutableStateOf("EASY") }
    var question by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة سؤال مسابقة") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("QURAN", "HADITH", "TAJWEED", "KNOWLEDGE", "MEMORIZATION", "KIDS")) { c ->
                        LiquidGlassChip(c, category == c, { category = c })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("EASY", "MEDIUM", "HARD").forEach { d -> LiquidGlassChip(d, difficulty == d, { difficulty = d }) }
                }
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("نص السؤال") })
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("الخيار ١") }, singleLine = true)
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("الخيار ٢") }, singleLine = true)
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("الخيار ٣") }, singleLine = true)
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("الخيار ٤") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { i -> LiquidGlassChip("صحيح: ${i + 1}", correctIdx == i, { correctIdx = i }) }
                }
                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("الشرح") })
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("المصدر") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(category, difficulty, question, optA, optB, optC, optD, correctIdx, explanation, source) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, LocalAdminRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(LocalAdminRole.TEACHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء حساب صلاحيات محلي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true)
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("الاسم الكامل") }, singleLine = true)
                OutlinedTextField(value = passcode, onValueChange = { passcode = it }, label = { Text("رمز المرور (٦+ أحرف)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiquidGlassChip("معلم", role == LocalAdminRole.TEACHER, { role = LocalAdminRole.TEACHER })
                    LiquidGlassChip("محرر", role == LocalAdminRole.EDITOR, { role = LocalAdminRole.EDITOR })
                    LiquidGlassChip("مدير عام", role == LocalAdminRole.SUPER_ADMIN, { role = LocalAdminRole.SUPER_ADMIN })
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(username, displayName, passcode, role) }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
