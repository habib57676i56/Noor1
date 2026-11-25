package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.ui.graphics.Color
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdminRole
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.cloud.UserAccountDoc
import com.example.data.cloud.UserReportDoc
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Part 1B — Complete User Management & User Reports Moderation.
 * Never exposes passwords or private auth credentials. Primary owner is protected
 * against accidental demotion, suspension, or deletion.
 */
@Composable
fun AdminUsersTab(
    adminViewModel: AdminViewModel,
    onRequireMfa: (() -> Unit) -> Unit,
    onMessageUser: (UserAccountDoc) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val reports by adminViewModel.allUserReports.collectAsStateWithLifecycle()
    val currentRole by adminViewModel.currentRole.collectAsStateWithLifecycle()

    var subTab by remember { mutableIntStateOf(0) } // 0 = Users, 1 = User Reports
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    var viewingUser by remember { mutableStateOf<UserAccountDoc?>(null) }
    var editingUser by remember { mutableStateOf<UserAccountDoc?>(null) }
    var deletingUser by remember { mutableStateOf<UserAccountDoc?>(null) }
    var reviewingReport by remember { mutableStateOf<UserReportDoc?>(null) }

    val filteredUsers = remember(users, searchQuery, statusFilter) {
        val q = searchQuery.trim()
        users.filter { u ->
            val matchStatus = when (statusFilter) {
                "ACTIVE" -> u.status == "ACTIVE"
                "SUSPENDED" -> u.status == "SUSPENDED"
                "ADMIN" -> AdminRole.fromKey(u.role).isPrivileged
                "VERIFIED" -> u.verified
                else -> true
            }
            val matchQuery = q.isEmpty() ||
                u.displayName.contains(q, ignoreCase = true) ||
                u.email.contains(q, ignoreCase = true) ||
                u.cityName.contains(q, ignoreCase = true) ||
                u.cityNameAr.contains(q, ignoreCase = true)
            matchStatus && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_users_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "إدارة المستخدمين والبلاغات", "User Management & Moderation"),
                subtitle = L10n.pick(lang, "لا يتم عرض كلمات المرور أو بيانات الاعتماد الخاصة مطلقاً", "Passwords and private auth secrets are never stored or exposed")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminFilterPill(
                    label = L10n.pick(lang, "المستخدمون (${users.size})", "Users (${users.size})"),
                    selected = subTab == 0,
                    onClick = { subTab = 0 }
                )
                val pendingCount = reports.count { it.status == "PENDING" }
                AdminFilterPill(
                    label = L10n.pick(lang, "البلاغات ($pendingCount معلّق)", "Reports ($pendingCount pending)"),
                    selected = subTab == 1,
                    onClick = { subTab = 1 }
                )
            }
        }

        if (subTab == 0) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text(L10n.pick(lang, "ابحث بالاسم أو البريد أو المدينة...", "Search by name, email, or city...")) }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf(
                        "ALL" to L10n.pick(lang, "الكل", "All"),
                        "ACTIVE" to L10n.pick(lang, "نشط", "Active"),
                        "SUSPENDED" to L10n.pick(lang, "موقوف", "Suspended"),
                        "VERIFIED" to L10n.pick(lang, "موثّق", "Verified"),
                        "ADMIN" to L10n.pick(lang, "المشرفون", "Privileged")
                    )
                    items(filters) { (key, label) ->
                        AdminFilterPill(label = label, selected = statusFilter == key, onClick = { statusFilter = key })
                    }
                }
            }

            if (filteredUsers.isEmpty()) {
                item {
                    AdminPanelCard {
                        Text(
                            text = L10n.pick(lang, "لا يوجد مستخدمون مطابقون للبحث حالياً.", "No matching user accounts found."),
                            color = AdminPalette.TextSecondary
                        )
                    }
                }
            } else {
                items(filteredUsers, key = { it.uid.ifBlank { it.id } }) { user ->
                    val role = AdminRole.fromKey(user.role)
                    val isPrimaryOwner = user.email.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true)
                    AdminPanelCard(
                        accentBorder = if (isPrimaryOwner) AdminPalette.AccentAmber.copy(alpha = 0.55f) else AdminPalette.BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = user.displayName.ifBlank { "NOOR Member" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AdminPalette.TextPrimary
                                    )
                                    if (user.verified) {
                                        Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = AdminPalette.AccentCyan, modifier = Modifier.size(17.dp))
                                        if (user.verifiedBadgeLabel.isNotBlank()) {
                                            AdminStatusBadge(label = user.verifiedBadgeLabel, color = AdminPalette.AccentCyan)
                                        }
                                    }
                                }
                                Text(user.email, style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextSecondary)
                            }
                            AdminStatusBadge(
                                label = L10n.pick(lang, role.labelAr, role.labelEn),
                                color = when (role) {
                                    AdminRole.SUPER_ADMIN -> AdminPalette.AccentAmber
                                    AdminRole.ADMIN -> AdminPalette.AccentCyan
                                    AdminRole.CONTENT_MODERATOR -> AdminPalette.AccentIndigo
                                    AdminRole.SUPPORT_AGENT -> AdminPalette.AccentEmerald
                                    AdminRole.USER -> AdminPalette.TextSecondary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // User Live Location Pill (City + Coordinates)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = "User Location",
                                tint = AdminPalette.GoldLuxe,
                                modifier = Modifier.size(15.dp)
                            )
                            val cityLabel = L10n.pick(
                                lang,
                                user.cityNameAr.ifBlank { user.cityName.ifBlank { "مكة المكرمة" } },
                                user.cityName.ifBlank { "Makkah" }
                            )
                            val coordStr = String.format(Locale.US, "%.2f°, %.2f°", user.latitude, user.longitude)
                            Text(
                                text = "$cityLabel · $coordStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AdminPalette.GoldLuxe
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AdminStatusBadge(
                                label = user.status,
                                color = if (user.status == "ACTIVE") AdminPalette.AccentEmerald else AdminPalette.AccentRose
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "أذكار: ${user.adhkarCompleted} · قرآن: ${user.quranVersesRead} · صوتيات: ${user.audioPlays}",
                                    "Adhkar: ${user.adhkarCompleted} · Quran: ${user.quranVersesRead} · Audio: ${user.audioPlays}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminPalette.TextMuted
                            )
                        }

                        if (currentRole.canManageUsers) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // 1-Tap Verify / Unverify Account Toggle
                                Button(
                                    onClick = { adminViewModel.quickToggleUserVerification(user, !user.verified) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (user.verified) AdminPalette.AccentCyan.copy(alpha = 0.24f) else AdminPalette.AccentCyan,
                                        contentColor = if (user.verified) AdminPalette.AccentCyan else Color(0xFF031522)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (user.verified) L10n.pick(lang, "موثّق ✓", "Verified") else L10n.pick(lang, "توثيق", "Verify"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { editingUser = user },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "إدارة", "Manage"), style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { viewingUser = user }) {
                                    Icon(Icons.Rounded.Info, contentDescription = "View Profile", tint = AdminPalette.AccentCyan)
                                }
                                if (currentRole.canSendAdminMessages) {
                                    IconButton(onClick = { onMessageUser(user) }) {
                                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Message User", tint = AdminPalette.AccentEmerald)
                                    }
                                }
                                if (!isPrimaryOwner && currentRole == AdminRole.SUPER_ADMIN) {
                                    IconButton(onClick = {
                                        onRequireMfa { deletingUser = user }
                                    }) {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete Account", tint = AdminPalette.AccentRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (reports.isEmpty()) {
                item {
                    AdminPanelCard {
                        Text(L10n.pick(lang, "لا توجد بلاغات مستخدمين حالياً.", "No user reports submitted yet."), color = AdminPalette.TextSecondary)
                    }
                }
            } else {
                items(reports, key = { it.id }) { report ->
                    AdminPanelCard(
                        accentBorder = if (report.status == "PENDING") AdminPalette.AccentAmber.copy(alpha = 0.5f) else AdminPalette.BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Flag, null, tint = AdminPalette.AccentAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${report.targetType.uppercase()} · ${report.targetId}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AdminStatusBadge(
                                label = report.status,
                                color = if (report.status == "PENDING") AdminPalette.AccentAmber else AdminPalette.AccentEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(report.reason, style = MaterialTheme.typography.bodySmall, color = AdminPalette.TextPrimary)
                        if (report.resolutionNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Resolution: ${report.resolutionNote}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentEmerald)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { reviewingReport = report },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "مراجعة واتخاذ قرار", "Review & Resolve"))
                        }
                    }
                }
            }
        }
    }

    // Edit User / Role Assignment Dialog
    editingUser?.let { target ->
        val isPrimaryOwner = target.email.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true)
        var nameDraft by remember(target) { mutableStateOf(target.displayName) }
        var statusDraft by remember(target) { mutableStateOf(target.status) }
        var verifiedDraft by remember(target) { mutableStateOf(target.verified) }
        var badgeLabelDraft by remember(target) { mutableStateOf(target.verifiedBadgeLabel.ifBlank { "موثّق" }) }
        var roleDraft by remember(target) { mutableStateOf(AdminRole.fromKey(target.role)) }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text(L10n.pick(lang, "إدارة الحساب: ${target.email}", "Manage: ${target.email}")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        label = { Text(L10n.pick(lang, "الاسم المعروض", "Display Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.pick(lang, "توثيق الحساب (شارة بجانب الاسم)", "Account Verified Badge"))
                        Switch(checked = verifiedDraft, onCheckedChange = { verifiedDraft = it })
                    }

                    if (verifiedDraft) {
                        OutlinedTextField(
                            value = badgeLabelDraft,
                            onValueChange = { badgeLabelDraft = it },
                            label = { Text(L10n.pick(lang, "وصف شارة التوثيق (مثلاً: موثّق / حافظ / قارئ)", "Verified Badge Label")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!isPrimaryOwner) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.pick(lang, "تعليق الحساب", "Suspend Account"), color = AdminPalette.AccentRose)
                            Switch(
                                checked = statusDraft == "SUSPENDED",
                                onCheckedChange = { statusDraft = if (it) "SUSPENDED" else "ACTIVE" }
                            )
                        }
                    } else {
                        Text(
                            text = L10n.pick(lang, "حساب المالك الأساسي محمي من التعليق أو تخفيض الصلاحية.", "Primary Owner account is protected from suspension or demotion."),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.AccentAmber
                        )
                    }

                    if (currentRole.canAssignRoles && !isPrimaryOwner) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, null, tint = AdminPalette.AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "الدور والصلاحيات (للمالك فقط)", "Role Assignment (Owner Only)"), style = MaterialTheme.typography.labelMedium)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AdminRole.entries) { r ->
                                AdminFilterPill(
                                    label = L10n.pick(lang, r.labelAr, r.labelEn),
                                    selected = roleDraft == r,
                                    onClick = { roleDraft = r }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val roleChanged = roleDraft.key != target.role
                    val executeSave = {
                        adminViewModel.updateUser(
                            user = target,
                            newDisplayName = nameDraft,
                            newStatus = statusDraft,
                            newVerified = verifiedDraft,
                            newRole = roleDraft,
                            newVerifiedBadgeLabel = if (verifiedDraft) badgeLabelDraft else ""
                        )
                        editingUser = null
                    }
                    if (roleChanged || statusDraft == "SUSPENDED") {
                        onRequireMfa(executeSave)
                    } else {
                        executeSave()
                    }
                }) {
                    Text(L10n.pick(lang, "حفظ التغييرات", "Save Changes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Confirm Destructive User Deletion Dialog
    deletingUser?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            title = { Text(L10n.pick(lang, "تأكيد حذف الحساب", "Confirm Account Deletion")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "سيتم حذف ملف المستخدم (${target.email}) وإزالة صلاحياته مع الاحتفاظ بسجل التدقيق الأمني. هذا الإجراء نهائي.",
                        "This removes the user profile (${target.email}) and revokes any roles while preserving the immutable security audit trail."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteUser(target)
                        deletingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPalette.AccentRose)
                ) {
                    Text(L10n.pick(lang, "حذف نهائي", "Delete Account"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingUser = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Resolve Report Dialog
    reviewingReport?.let { rep ->
        var noteDraft by remember(rep) { mutableStateOf(rep.resolutionNote) }
        AlertDialog(
            onDismissRequest = { reviewingReport = null },
            title = { Text(L10n.pick(lang, "معالجة البلاغ", "Resolve User Report")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(rep.reason, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text(L10n.pick(lang, "ملاحظة المراجعة", "Resolution Note")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.resolveUserReport(rep.id, "RESOLVED", noteDraft.ifBlank { "Reviewed and resolved" })
                    reviewingReport = null
                }) {
                    Text(L10n.pick(lang, "تم الحل", "Mark Resolved"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    adminViewModel.resolveUserReport(rep.id, "DISMISSED", noteDraft.ifBlank { "Dismissed" })
                    reviewingReport = null
                }) {
                    Text(L10n.pick(lang, "رفض البلاغ", "Dismiss"))
                }
            }
        )
    }

    // Read-Only User Profile & Worship Activity Summary Dialog (zero password/token exposure)
    viewingUser?.let { target ->
        AlertDialog(
            onDismissRequest = { viewingUser = null },
            title = { Text(L10n.pick(lang, "ملف المستخدم والنشاط", "User Profile & Activity")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${target.displayName.ifBlank { "NOOR Member" }} (${target.role})", fontWeight = FontWeight.Bold)
                    Text("Email: ${target.email}", style = MaterialTheme.typography.bodySmall, color = AdminPalette.AccentCyan)
                    Text(
                        text = L10n.pick(
                            lang,
                            "الحالة: ${target.status} · التوثيق: ${if (target.verified) "موثّق ✓ (${target.verifiedBadgeLabel.ifBlank { "موثّق" }})" else "غير موثّق"}",
                            "Status: ${target.status} · Verified: ${target.verified} ${target.verifiedBadgeLabel}"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "الموقع: ${target.cityNameAr.ifBlank { target.cityName }} (${String.format(Locale.US, "%.4f°, %.4f°", target.latitude, target.longitude)})",
                            "Location: ${target.cityName} (${String.format(Locale.US, "%.4f°, %.4f°", target.latitude, target.longitude)})"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminPalette.GoldLuxe
                    )
                    Text("Language: ${target.preferredLanguage}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = L10n.pick(
                            lang,
                            "القرآن: ${target.quranVersesRead} آية · الأذكار: ${target.adhkarCompleted} · التسبيح: ${target.tasbihCount} · الصوتيات: ${target.audioPlays}",
                            "Quran: ${target.quranVersesRead} verses · Adhkar: ${target.adhkarCompleted} · Tasbih: ${target.tasbihCount} · Audio: ${target.audioPlays}"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = AdminPalette.AccentEmerald
                    )
                }
            },
            confirmButton = {
                if (currentRole.canSendAdminMessages) {
                    Button(onClick = {
                        val u = target
                        viewingUser = null
                        onMessageUser(u)
                    }) {
                        Text(L10n.pick(lang, "إرسال رسالة", "Send Message"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingUser = null }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }
}
