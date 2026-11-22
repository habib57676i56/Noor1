package com.example.ui.admin

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Part 1E & Part 10 — Enterprise Security, Audit Trail, Emergency Owner Recovery,
 * Rollback Snapshots, CMS JSON Export/Import, Storage/Cache Cleanup & System Error Logs.
 */
@Composable
fun AdminSecuritySystemTab(
    adminViewModel: AdminViewModel,
    onRequestMfaVerification: () -> Unit,
    onRequireMfa: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalAppLanguage.current
    val currentRole by adminViewModel.currentRole.collectAsStateWithLifecycle()
    val isUnlocked by adminViewModel.isPrivilegedUnlocked.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val snapshots by adminViewModel.backupSnapshots.collectAsStateWithLifecycle()
    val storage by adminViewModel.storageMetrics.collectAsStateWithLifecycle()
    val errorLogs by adminViewModel.systemErrorLogs.collectAsStateWithLifecycle()

    var snapshotLabel by remember { mutableStateOf("") }

    val exportCmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = adminViewModel.exportAllCmsJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val importCmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    adminViewModel.importCmsJson(json)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_security_system_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "الأمان وإدارة النظام والنسخ الاحتياطي", "Security, Audit Trail & System Ops"),
                subtitle = L10n.pick(lang, "التحقق الثنائي، سجل التدقيق غير القابل للتعديل، واستعادة النظام", "MFA confirmation, immutable audit trails, snapshots & diagnostics")
            )
        }

        // Security Session & MFA Card + Emergency Owner Recovery
        item {
            AdminPanelCard(accentBorder = if (isUnlocked) AdminPalette.AccentEmerald else AdminPalette.AccentAmber) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (isUnlocked) AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                        )
                        Column {
                            Text(
                                text = L10n.pick(lang, "جلسة المشرف الأعلى (MFA)", "Privileged MFA Session"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUnlocked) L10n.pick(lang, "تم التحقق الثنائي للعمليات الحساسة", "Verified for destructive & role actions")
                                else L10n.pick(lang, "يتطلب تأكيد هوية المالك للعمليات الحساسة", "Owner confirmation required for sensitive actions"),
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminPalette.TextSecondary
                            )
                        }
                    }
                    AdminStatusBadge(
                        label = L10n.pick(lang, currentRole.labelAr, currentRole.labelEn),
                        color = AdminPalette.AccentCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestMfaVerification,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "تأكيد الهوية", "Verify MFA"), style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { adminViewModel.executeEmergencyOwnerRecovery() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(L10n.pick(lang, "استرداد المالك الطارئ", "Owner Recovery"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Storage, Cache & Database Migration Status
        item {
            AdminPanelCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Storage, null, tint = AdminPalette.AccentCyan)
                    Text(L10n.pick(lang, "التخزين وقاعدة البيانات (Room v3)", "Storage, Cache & DB Migration"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "قاعدة البيانات المحلية: ${storage.databaseBytes / 1024} ك.ب · الصوتيات المؤقتة: ${storage.audioCacheBytes / 1024} ك.ب (${storage.downloadedAudioCount} ملف) · Room Schema v3 (MIGRATION_2_3 غير مدمر)",
                        "Local DB: ${storage.databaseBytes / 1024} KB · Audio Cache: ${storage.audioCacheBytes / 1024} KB (${storage.downloadedAudioCount} files) · Room Schema v3 (non-destructive MIGRATION_2_3)"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminPalette.TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { adminViewModel.clearTemporaryCaches() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.CleaningServices, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "تنظيف الكاش", "Clear Cache"), style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { exportCmsLauncher.launch("noor_cms_export.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "تصدير CMS", "Export CMS"), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { importCmsLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.pick(lang, "استيراد محتوى CMS من JSON (كمسودات للمراجعة)", "Import CMS JSON (Imported as DRAFT for Review)"))
                }
            }
        }

        // Rollback Snapshots Section
        item {
            AdminPanelCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Backup, null, tint = AdminPalette.AccentIndigo)
                    Text(L10n.pick(lang, "نقاط الاستعادة السريعة (Rollback Snapshots)", "Rollback Snapshots"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = snapshotLabel,
                        onValueChange = { snapshotLabel = it },
                        placeholder = { Text(L10n.pick(lang, "اسم النقطة الاحتياطية...", "Snapshot label...")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            adminViewModel.createRollbackSnapshot(snapshotLabel.ifBlank { "Manual Snapshot" })
                            snapshotLabel = ""
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(L10n.pick(lang, "إنشاء", "Create"))
                    }
                }
                if (snapshots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    snapshots.take(5).forEach { snap ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(snap.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("${snap.sizeBytes / 1024} KB · #${snap.id}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextMuted)
                            }
                            IconButton(onClick = { onRequireMfa { adminViewModel.restoreRollbackSnapshot(snap) } }) {
                                Icon(Icons.Rounded.Restore, contentDescription = "Restore Snapshot", tint = AdminPalette.AccentEmerald)
                            }
                            IconButton(onClick = { adminViewModel.deleteRollbackSnapshot(snap.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Snapshot", tint = AdminPalette.AccentRose)
                            }
                        }
                    }
                }
            }
        }

        // System Error Logs
        item {
            AdminPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.BugReport, null, tint = AdminPalette.AccentAmber, modifier = Modifier.size(18.dp))
                        Text(L10n.pick(lang, "سجل أخطاء النظام (${errorLogs.size})", "System Error Logs (${errorLogs.size})"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (errorLogs.isNotEmpty()) {
                        OutlinedButton(onClick = { adminViewModel.clearSystemErrorLogs() }, shape = RoundedCornerShape(8.dp)) {
                            Text(L10n.pick(lang, "مسح", "Clear"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (errorLogs.isEmpty()) {
                    Text(L10n.pick(lang, "لا توجد أخطاء نظام مسجلة — الأداء مستقر.", "Zero system errors recorded — healthy runtime."), style = MaterialTheme.typography.bodySmall, color = AdminPalette.AccentEmerald)
                } else {
                    errorLogs.take(6).forEach { err ->
                        Text("[${err.tag}] ${err.message}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentRose)
                    }
                }
            }
        }

        // Immutable Administrative Audit Trail
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "سجل التدقيق الأمني غير القابل للتعديل", "Immutable Security Audit Trail"),
                subtitle = L10n.pick(lang, "يتم تسجيل جميع الإجراءات الإدارية تلقائياً في Firestore", "All privileged actions are permanently logged to Firestore")
            )
        }
        if (auditLogs.isEmpty()) {
            item {
                AdminPanelCard {
                    Text(L10n.pick(lang, "لا توجد سجلات تدقيق بعد.", "No audit events logged yet."), color = AdminPalette.TextSecondary)
                }
            }
        } else {
            items(auditLogs.take(40), key = { it.id }) { log ->
                val sevColor = when (log.severity) {
                    "CRITICAL" -> AdminPalette.AccentRose
                    "WARNING" -> AdminPalette.AccentAmber
                    else -> AdminPalette.AccentCyan
                }
                AdminPanelCard(contentPadding = PaddingValues(12.dp), accentBorder = sevColor.copy(alpha = 0.3f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(log.action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AdminPalette.TextPrimary)
                        AdminStatusBadge(label = log.severity, color = sevColor)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = AdminPalette.TextSecondary)
                    Text("${log.actorEmail} · ${log.category}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextMuted)
                }
            }
        }
    }
}
