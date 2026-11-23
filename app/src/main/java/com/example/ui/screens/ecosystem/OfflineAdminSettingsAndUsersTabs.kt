package com.example.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.RecordingPlaybackState
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.db.AdminAuditLogLocalEntity
import com.example.data.local.db.HalaqahCircleEntity
import com.example.data.local.db.HalaqahStudentEntity
import com.example.data.local.db.HalaqahTeacherEntity
import com.example.data.local.db.HifzRevisionLogEntity
import com.example.data.local.db.ImportedAiDocumentEntity
import com.example.data.local.db.LibraryBookEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.data.local.db.RecitationAssessmentEntity
import com.example.data.local.db.StudentRecitationRecordingEntity
import com.example.data.repository.StorageUsageSummary
import com.example.domain.ai.AiIndexStats
import com.example.domain.quran.AssessmentStrictness
import com.example.security.LocalAdminRole
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorSectionHeader
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.viewmodel.AdminSettingsState

fun LazyListScope.adminOverviewTabItems(
    students: List<HalaqahStudentEntity>,
    teachers: List<HalaqahTeacherEntity>,
    circles: List<HalaqahCircleEntity>,
    assessments: List<RecitationAssessmentEntity>,
    hifzLogs: List<HifzRevisionLogEntity>,
    recordings: List<StudentRecitationRecordingEntity>,
    allKb: List<LocalAiKnowledgeEntity>,
    importedDocs: List<ImportedAiDocumentEntity>,
    books: List<LibraryBookEntity>,
    indexStats: AiIndexStats,
    storage: StorageUsageSummary,
    sttAvailable: Boolean,
    auditLogs: List<AdminAuditLogLocalEntity>,
    onRefreshDiagnostics: () -> Unit
) {
    item {
        val noorColors = LocalNoorColors.current
        val activeStudents = students.count { it.active }
        val pendingReviews = recordings.count { it.teacherReviewStatus == "PENDING" } +
            assessments.count { !it.approvedByTeacher }
        LiquidGlassCard(strong = true, goldBorder = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("نظرة عامة على النظام (بيانات حقيقية)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                IconButton(onClick = onRefreshDiagnostics) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Metrics", tint = noorColors.goldAccent)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("• الطلاب: ${students.size} (نشط: $activeStudents) · المعلمون: ${teachers.size} · الحلقات: ${circles.size}")
            Text("• جلسات الحفظ المكتملة: ${hifzLogs.size} · التقييمات: ${assessments.size} · بانتظار المراجعة: $pendingReviews")
            Text("• تسجيلات التلاوة المحفوظة: ${recordings.size}")
            Text("• قاعدة معرفة الذكاء: ${allKb.size} (معتمد: ${allKb.count { it.approvalStatus == "APPROVED" }}) · كلمات مفهرسة: ${indexStats.uniqueTokenCount}")
            Text("• المستندات المستوردة (TXT/JSON/DOCX/PDF): ${importedDocs.size} · كتب المكتبة: ${books.size}")
            Text("• مساحة التخزين المستخدمة: ${storage.totalMbFormatted}")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "حالة الأنظمة المحلية:\n" +
                    "• تسجيل الصوت (MediaRecorder M4A): جاهز محلياً\n" +
                    "• التعرف الصوتي العربي (SpeechRecognizer): ${if (sttAvailable) "متوفر على الجهاز + إملاء بديل" else "غير متوفر — يعمل الإملاء البديل والتسجيل الصوتي"}\n" +
                    "• الذكاء المحلي (Local RAG): نشط دون إنترنت",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "شفافية تقنية: تدير هذه اللوحة قاعدة البيانات والملفات على هذا الجهاز محلياً دون خادم سحابي، ويمكن تصدير/استيراد النسخ الاحتياطية بين الأجهزة عبر ملفات JSON.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (auditLogs.isNotEmpty()) {
        item { NoorSectionHeader(title = "آخر نشاط الإدارة (${auditLogs.size})") }
        items(auditLogs.take(5), key = { "ov_${it.id}" }) { log ->
            LiquidGlassCard {
                Text("[${log.category}] ${log.action} · ${log.actorUid}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (log.details.isNotBlank()) Text(log.details, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun LazyListScope.adminHalaqatAndRecordingsTabItems(
    role: LocalAdminRole,
    students: List<HalaqahStudentEntity>,
    teachers: List<HalaqahTeacherEntity>,
    circles: List<HalaqahCircleEntity>,
    recordings: List<StudentRecitationRecordingEntity>,
    playbackState: RecordingPlaybackState,
    onExportReport: () -> Unit,
    onPlayRecording: (String) -> Unit,
    onStopRecording: () -> Unit,
    onReviewRecording: (Long, String, String) -> Unit,
    onDeleteRecording: (StudentRecitationRecordingEntity) -> Unit
) {
    item {
        val noorColors = LocalNoorColors.current
        LiquidGlassCard(strong = true, goldBorder = true) {
            Text("إدارة الطلاب والمعلمين والحلقات وتسجيلات التسميع", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
            Spacer(Modifier.height(4.dp))
            Text("الطلاب: ${students.size} · المعلمون: ${teachers.size} · الحلقات: ${circles.size} · التسجيلات: ${recordings.size}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onExportReport,
                modifier = Modifier.fillMaxWidth().testTag("admin_export_halaqat_report_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تصدير تقرير الطلاب والحلقات (TXT)")
            }
        }
    }

    item { NoorSectionHeader(title = "تسجيلات تلاوة الطلاب للمراجعة (${recordings.size})") }
    if (recordings.isEmpty()) {
        item { LiquidGlassCard { Text("لا توجد تسجيلات تلاوة محفوظة حالياً.") } }
    }
    items(recordings, key = { "adm_rec_${it.id}" }) { rec ->
        val s = SurahCatalog.findSurah(rec.surahNumber)
        val isPlaying = playbackState.isPlaying && playbackState.playingFilePath == rec.filePath
        LiquidGlassCard(goldBorder = rec.teacherReviewStatus == "APPROVED") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${rec.studentName} · سورة ${s.nameAr} (${rec.fromAyah}–${rec.toAyah})", fontWeight = FontWeight.Bold)
                    Text("المدة: ${rec.durationMs / 1000}ث · الدقة: ${rec.accuracyPercent}% · الحالة: ${rec.teacherReviewStatus}", style = MaterialTheme.typography.labelSmall, color = LocalNoorColors.current.goldAccent)
                    if (rec.suspectedMistakesSummary.isNotBlank()) {
                        Text("ملاحظات التقييم: ${rec.suspectedMistakesSummary}", style = MaterialTheme.typography.bodySmall, color = SemanticWarning)
                    }
                }
                if (role.canViewRecordings()) {
                    IconButton(onClick = { if (isPlaying) onStopRecording() else onPlayRecording(rec.filePath) }) {
                        Icon(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "Play Student Recording", tint = GoldBright)
                    }
                    IconButton(onClick = {
                        val next = if (rec.teacherReviewStatus == "APPROVED") "NEEDS_REVISION" else "APPROVED"
                        onReviewRecording(rec.id, next, rec.teacherComment.ifBlank { if (next == "APPROVED") "تلاوة معتمدة" else "يرجى المراجعة" })
                    }) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Approve Recording",
                            tint = if (rec.teacherReviewStatus == "APPROVED") SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (role.canManageHalaqat()) {
                    IconButton(onClick = { onDeleteRecording(rec) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Recording", tint = SemanticError)
                    }
                }
            }
        }
    }

    item { NoorSectionHeader(title = "قائمة الطلاب المسجلين (${students.size})") }
    items(students.take(25), key = { "adm_st_${it.id}" }) { st ->
        val circleName = circles.firstOrNull { it.id == st.circleId }?.name ?: "بدون حلقة"
        LiquidGlassCard {
            Text("${st.fullName} (العمر ${st.age}) · $circleName", fontWeight = FontWeight.Bold)
            Text("محفوظ: ${st.memorizedJuz} جزء (${st.memorizedPages} صفحة) · مراجعة: ${st.revisionPages} صفحة", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun LazyListScope.adminAdvancedSettingsTabItems(
    role: LocalAdminRole,
    settings: AdminSettingsState,
    storage: StorageUsageSummary,
    onUpdateSetting: (String, String) -> Unit,
    onClearAudioCache: () -> Unit,
    onRefreshStorage: () -> Unit
) {
    if (!role.canManageSettings()) {
        item {
            LiquidGlassCard {
                Text("إعدادات الإدارة المتقدمة مخصصة لصلاحية «مدير النظام العام (SUPER_ADMIN)» فقط.")
            }
        }
        return
    }

    item {
        val noorColors = LocalNoorColors.current
        LiquidGlassCard(strong = true, goldBorder = true) {
            Text("تفعيل وإيقاف الوحدات (Feature Flags)", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
            Spacer(Modifier.height(6.dp))
            AdminSettingToggleRow("حلقات نور القرآنية", settings.featureHalaqatEnabled) { onUpdateSetting("feature_halaqat", it.toString()) }
            AdminSettingToggleRow("مساعد الحفظ والتسميع", settings.featureHifzAiEnabled) { onUpdateSetting("feature_hifz_ai", it.toString()) }
            AdminSettingToggleRow("نور الذكي المحلي (Local RAG)", settings.featureLocalAiEnabled) { onUpdateSetting("feature_local_ai", it.toString()) }
            AdminSettingToggleRow("المكتبة الإسلامية المحلية", settings.featureLibraryEnabled) { onUpdateSetting("feature_library", it.toString()) }
            AdminSettingToggleRow("نور للأطفال", settings.featureKidsEnabled) { onUpdateSetting("feature_kids", it.toString()) }
            AdminSettingToggleRow("المسابقات الإسلامية", settings.featureQuizEnabled) { onUpdateSetting("feature_quiz", it.toString()) }
            AdminSettingToggleRow("إذاعات القرآن الكريم", settings.featureRadioEnabled) { onUpdateSetting("feature_radio", it.toString()) }
        }
    }

    item {
        val noorColors = LocalNoorColors.current
        var minConfSlider by remember(settings.aiMinConfidencePercent) { mutableFloatStateOf(settings.aiMinConfidencePercent.toFloat()) }
        var announcementDraft by remember(settings.appAnnouncementBanner) { mutableStateOf(settings.appAnnouncementBanner) }

        LiquidGlassCard(strong = true) {
            Text("إعدادات التعرف الصوتي وتقييم الحفظ والذكاء المحلي", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
            Spacer(Modifier.height(6.dp))
            AdminSettingToggleRow("أولوية التعرف الصوتي دون إنترنت (Offline-First STT)", settings.sttPreferOffline) {
                onUpdateSetting("stt_prefer_offline", it.toString())
            }
            AdminSettingToggleRow("اعتماد المستندات المستوردة تلقائياً للمدير", settings.autoApproveAdminImports) {
                onUpdateSetting("auto_approve_imports", it.toString())
            }

            Spacer(Modifier.height(8.dp))
            Text("صرامة تقييم التلاوة (Assessment Strictness):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssessmentStrictness.entries.forEach { level ->
                    LiquidGlassChip(
                        label = level.labelAr.substringBefore(" "),
                        selected = settings.assessmentStrictness == level,
                        onClick = { onUpdateSetting("assessment_strictness", level.name) },
                        modifier = Modifier.weight(1f).testTag("admin_strictness_${level.name.lowercase()}")
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("الحد الأدنى لثقة إجابة الذكاء المحلي: ${minConfSlider.toInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = minConfSlider,
                onValueChange = { minConfSlider = it },
                onValueChangeFinished = { onUpdateSetting("ai_min_confidence", minConfSlider.toInt().toString()) },
                valueRange = 18f..60f
            )

            Spacer(Modifier.height(8.dp))
            Text("إعداد النموذج المحلي: ${settings.localModelModeLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = announcementDraft,
                onValueChange = { announcementDraft = it },
                label = { Text("شريط إعلان داخلي للتطبيق (اختياري)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onUpdateSetting("app_announcement_banner", announcementDraft.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
            ) { Text("حفظ شريط الإعلان") }
        }
    }

    item {
        val noorColors = LocalNoorColors.current
        LiquidGlassCard {
            Text("إدارة التخزين المحلي والتشخيص", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
            Spacer(Modifier.height(6.dp))
            Text("• قاعدة البيانات (Room): ${storage.databaseBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            Text("• تسجيلات الحفظ (M4A): ${storage.recordingsBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            Text("• ملفات الأذان المحمّلة والمخصّصة: ${storage.adhanAudioBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            Text("• الذاكرة المؤقتة لتلاوات القرآن: ${storage.cachedQuranAudioBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            Text("• الإجمالي: ${storage.totalMbFormatted}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefreshStorage, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("تحديث")
                }
                OutlinedButton(onClick = onClearAudioCache, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("تفريغ الكاش الصوتي")
                }
            }
        }
    }
}

@Composable
private fun AdminSettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
