package com.example.ui.admin

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.CmsContentCategory
import com.example.data.cloud.CmsContentDoc
import com.example.data.cloud.CmsWorkflowStatus
import com.example.domain.verification.VerificationSeverity
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Part 1C & Part 3 — Professional Islamic Content Management System (CMS).
 * Supports all 16 content categories, full CRUD, DRAFT -> REVIEW -> VERIFIED -> PUBLISHED
 * workflow, scheduling, immutable revision history with one-click restore, and the
 * built-in Adhkar Content Verification Scanner.
 */
@Composable
fun AdminCmsTab(
    adminViewModel: AdminViewModel,
    onRequireMfa: (() -> Unit) -> Unit
) {
    val lang = LocalAppLanguage.current
    val cmsItems by adminViewModel.allCmsContent.collectAsStateWithLifecycle()
    val revisions by adminViewModel.selectedRevisions.collectAsStateWithLifecycle()
    val verificationReport by adminViewModel.adhkarVerificationReport.collectAsStateWithLifecycle()
    val currentRole by adminViewModel.currentRole.collectAsStateWithLifecycle()

    var subMode by remember { mutableIntStateOf(0) } // 0 = CMS Library, 1 = Adhkar Verification Scanner
    var selectedCategory by remember { mutableStateOf<CmsContentCategory?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var editorItem by remember { mutableStateOf<CmsContentDoc?>(null) }
    var revisionHostItem by remember { mutableStateOf<CmsContentDoc?>(null) }
    var deletingItem by remember { mutableStateOf<CmsContentDoc?>(null) }

    val filteredItems = remember(cmsItems, selectedCategory, statusFilter, searchQuery) {
        val q = searchQuery.trim()
        cmsItems.filter { doc ->
            val matchCat = selectedCategory == null || doc.category == selectedCategory!!.key
            val matchStatus = statusFilter == "ALL" || doc.status == statusFilter
            val matchQuery = q.isEmpty() ||
                doc.titleAr.contains(q, ignoreCase = true) ||
                doc.titleEn.contains(q, ignoreCase = true) ||
                doc.bodyAr.contains(q, ignoreCase = true) ||
                doc.sourceAr.contains(q, ignoreCase = true)
            matchCat && matchStatus && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_cms_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "إدارة المحتوى الإسلامي (CMS)", "Islamic Content CMS (16 Categories)"),
                subtitle = L10n.pick(lang, "مسودة ← مراجعة ← موثّق ← منشور مع سجل التعديلات", "Draft → Review → Verified → Published with version history"),
                trailing = {
                    if (currentRole.canManageContent) {
                        Button(
                            onClick = {
                                editorItem = CmsContentDoc(
                                    category = (selectedCategory ?: CmsContentCategory.MORNING_ADHKAR).key,
                                    status = CmsWorkflowStatus.DRAFT.key,
                                    repetitionCount = 1
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_cms_add_button")
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L10n.pick(lang, "إضافة", "New"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminFilterPill(
                    label = L10n.pick(lang, "مكتبة المحتوى (${cmsItems.size})", "CMS Library (${cmsItems.size})"),
                    selected = subMode == 0,
                    onClick = { subMode = 0 }
                )
                AdminFilterPill(
                    label = L10n.pick(
                        lang,
                        "فحص الأذكار (${verificationReport.errorCount} خطأ)",
                        "Adhkar Scanner (${verificationReport.errorCount} errors)"
                    ),
                    selected = subMode == 1,
                    onClick = { subMode = 1 }
                )
            }
        }

        if (subMode == 0) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text(L10n.pick(lang, "ابحث في النصوص أو المصادر...", "Search titles, Arabic texts, or sources...")) }
                )
            }

            // All 16 Category Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        AdminFilterPill(
                            label = L10n.pick(lang, "كل الأقسام (16)", "All 16 Categories"),
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(CmsContentCategory.entries) { cat ->
                        AdminFilterPill(
                            label = L10n.pick(lang, cat.labelAr, cat.labelEn),
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            // Workflow status filter
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        AdminFilterPill(
                            label = L10n.pick(lang, "كل الحالات", "All Statuses"),
                            selected = statusFilter == "ALL",
                            onClick = { statusFilter = "ALL" }
                        )
                    }
                    items(CmsWorkflowStatus.entries) { st ->
                        AdminFilterPill(
                            label = L10n.pick(lang, st.labelAr, st.labelEn),
                            selected = statusFilter == st.key,
                            onClick = { statusFilter = st.key }
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    AdminPanelCard {
                        Text(
                            text = L10n.pick(
                                lang,
                                "لا توجد عناصر CMS مطابقة حالياً. اضغط (إضافة) لإنشاء محتوى جديد ونشره مباشرة إلى تطبيق نور.",
                                "No CMS items match this filter yet. Tap 'New' to author verified Islamic content and publish live to NOOR."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { doc ->
                    val cat = CmsContentCategory.fromKey(doc.category)
                    val wf = CmsWorkflowStatus.fromKey(doc.status)
                    val wfColor = when (wf) {
                        CmsWorkflowStatus.PUBLISHED -> AdminPalette.AccentEmerald
                        CmsWorkflowStatus.VERIFIED -> AdminPalette.AccentCyan
                        CmsWorkflowStatus.REVIEW -> AdminPalette.AccentAmber
                        CmsWorkflowStatus.DRAFT -> AdminPalette.TextSecondary
                    }
                    AdminPanelCard(accentBorder = wfColor.copy(alpha = 0.35f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdminStatusBadge(label = L10n.pick(lang, cat.labelAr, cat.labelEn), color = AdminPalette.AccentIndigo)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AdminStatusBadge(label = "v${doc.version}", color = AdminPalette.TextSecondary)
                                AdminStatusBadge(label = L10n.pick(lang, wf.labelAr, wf.labelEn), color = wfColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L10n.pick(lang, doc.titleAr, doc.titleEn.ifBlank { doc.titleAr }),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AdminPalette.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doc.bodyAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${doc.sourceAr} · ${doc.gradingAr} · ×${doc.repetitionCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.AccentCyan
                        )

                        if (currentRole.canManageContent) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { editorItem = doc },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "تعديل", "Edit"), style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        adminViewModel.selectContentForRevisions(doc.id)
                                        revisionHostItem = doc
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.History, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "الإصدارات", "Revisions"), style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = {
                                    onRequireMfa { deletingItem = doc }
                                }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = AdminPalette.AccentRose)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Adhkar Verification Scanner Panel
            item {
                AdminPanelCard(
                    accentBorder = if (verificationReport.isClean) AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (verificationReport.isClean) Icons.Rounded.CheckCircle else Icons.Rounded.FactCheck,
                            contentDescription = null,
                            tint = if (verificationReport.isClean) AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                        )
                        Column {
                            Text(
                                text = L10n.pick(lang, "مدقّق أصالة وموثوقية الأذكار", "Adhkar Authenticity & Integrity Scanner"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "إجمالي المفحوص: ${verificationReport.totalChecked} · الصباح: ${verificationReport.morningCount} · المساء: ${verificationReport.eveningCount}",
                                    "Checked: ${verificationReport.totalChecked} · Morning: ${verificationReport.morningCount} · Evening: ${verificationReport.eveningCount}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminPalette.TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatusBadge(
                            label = L10n.pick(lang, "أخطاء: ${verificationReport.errorCount}", "Errors: ${verificationReport.errorCount}"),
                            color = if (verificationReport.errorCount == 0) AdminPalette.AccentEmerald else AdminPalette.AccentRose
                        )
                        AdminStatusBadge(
                            label = L10n.pick(lang, "تنبيهات: ${verificationReport.warningCount}", "Warnings: ${verificationReport.warningCount}"),
                            color = if (verificationReport.warningCount == 0) AdminPalette.AccentCyan else AdminPalette.AccentAmber
                        )
                    }
                }
            }

            if (verificationReport.issues.isEmpty()) {
                item {
                    AdminPanelCard(accentBorder = AdminPalette.AccentEmerald.copy(alpha = 0.5f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "جميع أذكار الصباح والمساء والمحتوى المنشور اجتازت فحص التشكيل والمصادر والدرجة والتكرار والتكرار المزدوج بنسبة 100%.",
                                "All Morning, Evening, and CMS Adhkar passed Arabic diacritics, source citations, grading, repetition count, and duplicate verification with 0 issues."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.AccentEmerald
                        )
                    }
                }
            } else {
                items(verificationReport.issues) { issue ->
                    val col = if (issue.severity == VerificationSeverity.ERROR) AdminPalette.AccentRose else AdminPalette.AccentAmber
                    AdminPanelCard(accentBorder = col.copy(alpha = 0.45f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Warning, null, tint = col, modifier = Modifier.size(16.dp))
                            Text("${issue.code} · ${issue.dhikrId}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = col)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(L10n.pick(lang, issue.messageAr, issue.messageEn), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Create / Edit CMS Entry Dialog
    editorItem?.let { draftInit ->
        var category by remember(draftInit) { mutableStateOf(CmsContentCategory.fromKey(draftInit.category)) }
        var status by remember(draftInit) { mutableStateOf(CmsWorkflowStatus.fromKey(draftInit.status)) }
        var titleAr by remember(draftInit) { mutableStateOf(draftInit.titleAr) }
        var titleEn by remember(draftInit) { mutableStateOf(draftInit.titleEn) }
        var bodyAr by remember(draftInit) { mutableStateOf(draftInit.bodyAr) }
        var bodyEn by remember(draftInit) { mutableStateOf(draftInit.bodyEn) }
        var sourceAr by remember(draftInit) { mutableStateOf(draftInit.sourceAr) }
        var sourceEn by remember(draftInit) { mutableStateOf(draftInit.sourceEn) }
        var gradingAr by remember(draftInit) { mutableStateOf(draftInit.gradingAr) }
        var repText by remember(draftInit) { mutableStateOf(draftInit.repetitionCount.toString()) }
        var audioUrl by remember(draftInit) { mutableStateOf(draftInit.audioUrl) }
        var changeSummary by remember(draftInit) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { editorItem = null },
            title = { Text(if (draftInit.id.isBlank()) L10n.pick(lang, "إنشاء محتوى جديد", "Create CMS Content") else L10n.pick(lang, "تعديل المحتوى", "Edit CMS Content")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(L10n.pick(lang, "القسم (16 قسم)", "Category"), style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentCyan)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CmsContentCategory.entries) { c ->
                            AdminFilterPill(
                                label = L10n.pick(lang, c.labelAr, c.labelEn),
                                selected = category == c,
                                onClick = { category = c }
                            )
                        }
                    }

                    Text(L10n.pick(lang, "حالة التوثيق والنشر", "Workflow Status"), style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentCyan)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CmsWorkflowStatus.entries) { wf ->
                            AdminFilterPill(
                                label = L10n.pick(lang, wf.labelAr, wf.labelEn),
                                selected = status == wf,
                                onClick = { status = wf }
                            )
                        }
                    }

                    OutlinedTextField(value = titleAr, onValueChange = { titleAr = it }, label = { Text("العنوان بالعربية *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("English Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyAr, onValueChange = { bodyAr = it }, label = { Text("النص العربي المشكول *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyEn, onValueChange = { bodyEn = it }, label = { Text("English Translation / Explanation") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sourceAr, onValueChange = { sourceAr = it }, label = { Text("المصدر الشرعي / الحديثي *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sourceEn, onValueChange = { sourceEn = it }, label = { Text("English Source Citation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gradingAr, onValueChange = { gradingAr = it }, label = { Text("الدرجة") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = repText, onValueChange = { repText = it.filter { ch -> ch.isDigit() } }, label = { Text("التكرار") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = audioUrl, onValueChange = { audioUrl = it }, label = { Text("رابط صوتي (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = changeSummary, onValueChange = { changeSummary = it }, label = { Text(L10n.pick(lang, "ملخص التعديل لسجل الإصدارات", "Revision Note")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val doc = draftInit.copy(
                        category = category.key,
                        status = status.key,
                        titleAr = titleAr.trim(),
                        titleEn = titleEn.trim(),
                        bodyAr = bodyAr.trim(),
                        bodyEn = bodyEn.trim(),
                        sourceAr = sourceAr.trim(),
                        sourceEn = sourceEn.trim().ifBlank { sourceAr.trim() },
                        gradingAr = gradingAr.trim().ifBlank { "صحيح" },
                        repetitionCount = repText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        audioUrl = audioUrl.trim()
                    )
                    adminViewModel.saveCmsItem(doc, changeSummary)
                    editorItem = null
                }) {
                    Text(L10n.pick(lang, "حفظ ونشر", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editorItem = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Revision History + Restore Dialog
    revisionHostItem?.let { host ->
        AlertDialog(
            onDismissRequest = {
                revisionHostItem = null
                adminViewModel.selectContentForRevisions(null)
            },
            title = { Text(L10n.pick(lang, "سجل الإصدارات: ${host.titleAr}", "Revision History")) },
            text = {
                if (revisions.isEmpty()) {
                    Text(L10n.pick(lang, "لا توجد مراجعات مسجلة بعد.", "No saved revisions yet."))
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        revisions.forEach { rev ->
                            AdminPanelCard(contentPadding = PaddingValues(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("v${rev.version} · ${rev.status}", fontWeight = FontWeight.Bold, color = AdminPalette.AccentCyan)
                                    TextButton(onClick = {
                                        adminViewModel.restoreCmsRevision(host, rev)
                                        revisionHostItem = null
                                        adminViewModel.selectContentForRevisions(null)
                                    }) {
                                        Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(L10n.pick(lang, "استعادة", "Restore"))
                                    }
                                }
                                Text(rev.changeSummary, style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextSecondary)
                                Text(rev.bodyAr, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    revisionHostItem = null
                    adminViewModel.selectContentForRevisions(null)
                }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }

    // Confirm CMS Delete
    deletingItem?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(L10n.pick(lang, "حذف عنصر المحتوى", "Delete CMS Entry")) },
            text = { Text(L10n.pick(lang, "هل تريد حذف (${target.titleAr})؟", "Delete '${target.titleAr}' from CMS?")) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteCmsItem(target.id)
                        deletingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPalette.AccentRose)
                ) {
                    Text(L10n.pick(lang, "حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}
