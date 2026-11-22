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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.cloud.SupportTicketDoc
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Part 1D, Part 5 & Part 8 Admin Management:
 * - Sub-tab 0: Live Global Application Settings (app name, theme, language, feature flags,
 *   maintenance mode, Founder Profile live editor, Contact Details editor).
 * - Sub-tab 1: Global Announcements Broadcast Manager.
 * - Sub-tab 2: Support & Feedback Tickets Inbox with live admin replies.
 */
@Composable
fun AdminSettingsAndSupportTab(
    adminViewModel: AdminViewModel,
    onRequireMfa: (() -> Unit) -> Unit
) {
    val lang = LocalAppLanguage.current
    val currentRole by adminViewModel.currentRole.collectAsStateWithLifecycle()
    val liveConfig by adminViewModel.globalConfig.collectAsStateWithLifecycle()
    val announcements by adminViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val tickets by adminViewModel.allSupportTickets.collectAsStateWithLifecycle()

    var subTab by remember { mutableIntStateOf(0) } // 0 = Global Config, 1 = Announcements, 2 = Support Inbox
    var showNewAnnouncementDialog by remember { mutableStateOf(false) }
    var replyingTicket by remember { mutableStateOf<SupportTicketDoc?>(null) }

    var draftConfig by remember { mutableStateOf(liveConfig) }
    LaunchedEffect(liveConfig) { draftConfig = liveConfig }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_settings_support_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "إعدادات التطبيق والإعلانات والدعم", "Global Config, Broadcasts & Support"),
                subtitle = L10n.pick(lang, "تنعكس التغييرات فورياً في تطبيق نور دون إعادة بناء", "Changes propagate live to all NOOR users without rebuilding")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AdminFilterPill(
                        label = L10n.pick(lang, "الإعدادات العامة", "Global Settings"),
                        selected = subTab == 0,
                        onClick = { subTab = 0 }
                    )
                }
                item {
                    AdminFilterPill(
                        label = L10n.pick(lang, "الإعلانات (${announcements.size})", "Broadcasts (${announcements.size})"),
                        selected = subTab == 1,
                        onClick = { subTab = 1 }
                    )
                }
                item {
                    val openCount = tickets.count { it.status != "RESOLVED" }
                    AdminFilterPill(
                        label = L10n.pick(lang, "الدعم الفني ($openCount)", "Support Inbox ($openCount)"),
                        selected = subTab == 2,
                        onClick = { subTab = 2 }
                    )
                }
            }
        }

        when (subTab) {
            0 -> {
                // Branding, Theme, Language & Maintenance Mode
                item {
                    AdminPanelCard(accentBorder = if (draftConfig.maintenanceMode) AdminPalette.AccentRose else AdminPalette.BorderSubtle) {
                        Text(L10n.pick(lang, "الهوية واللغة ووضع الصيانة", "Branding, Defaults & Maintenance Mode"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draftConfig.appNameAr,
                                onValueChange = { draftConfig = draftConfig.copy(appNameAr = it) },
                                label = { Text("اسم التطبيق (عربي)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draftConfig.appNameEn,
                                onValueChange = { draftConfig = draftConfig.copy(appNameEn = it) },
                                label = { Text("App Name (EN)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("emerald_gold" to "Emerald & Gold", "midnight_jade" to "Midnight Jade", "royal_ivory" to "Royal Ivory").forEach { (k, label) ->
                                AdminFilterPill(label = label, selected = draftConfig.themePreset == k, onClick = { draftConfig = draftConfig.copy(themePreset = k) })
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdminFilterPill(label = "Default: العربية", selected = draftConfig.defaultLanguage == "ar", onClick = { draftConfig = draftConfig.copy(defaultLanguage = "ar") })
                            AdminFilterPill(label = "Default: English", selected = draftConfig.defaultLanguage == "en", onClick = { draftConfig = draftConfig.copy(defaultLanguage = "en") })
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AdminSwitchRow(
                            label = L10n.pick(lang, "تفعيل وضع الصيانة الشامل (Maintenance Mode)", "Enable Global Maintenance Mode"),
                            checked = draftConfig.maintenanceMode,
                            onCheckedChange = { draftConfig = draftConfig.copy(maintenanceMode = it) }
                        )
                        if (draftConfig.maintenanceMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = draftConfig.maintenanceMessageAr,
                                onValueChange = { draftConfig = draftConfig.copy(maintenanceMessageAr = it) },
                                label = { Text("رسالة الصيانة (عربي)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = draftConfig.maintenanceMessageEn,
                                onValueChange = { draftConfig = draftConfig.copy(maintenanceMessageEn = it) },
                                label = { Text("Maintenance Message (EN)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Live Feature Flags
                item {
                    AdminPanelCard {
                        Text(L10n.pick(lang, "تبديل الميزات الحية (Feature Flags)", "Live Feature Availability"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        AdminSwitchRow(L10n.pick(lang, "القرآن الكريم", "Holy Quran Module"), draftConfig.featureQuranEnabled) { draftConfig = draftConfig.copy(featureQuranEnabled = it) }
                        AdminSwitchRow(L10n.pick(lang, "المكتبة الصوتية", "Audio Library & Streaming"), draftConfig.featureAudioEnabled) { draftConfig = draftConfig.copy(featureAudioEnabled = it) }
                        AdminSwitchRow(L10n.pick(lang, "البحث عن المساجد", "Nearby Mosques Finder"), draftConfig.featureMosquesEnabled) { draftConfig = draftConfig.copy(featureMosquesEnabled = it) }
                        AdminSwitchRow(L10n.pick(lang, "بوصلة القبلة", "Qibla Compass"), draftConfig.featureQiblaEnabled) { draftConfig = draftConfig.copy(featureQiblaEnabled = it) }
                        AdminSwitchRow(L10n.pick(lang, "التقويم الهجري", "Hijri Calendar"), draftConfig.featureCalendarEnabled) { draftConfig = draftConfig.copy(featureCalendarEnabled = it) }
                    }
                }

                // Founder Profile Live Editor (Osama)
                item {
                    AdminPanelCard {
                        Text(L10n.pick(lang, "تحرير ملف المؤسس (أسامة)", "Founder Profile Editor (Osama)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draftConfig.founderName,
                            onValueChange = { draftConfig = draftConfig.copy(founderName = it) },
                            label = { Text("Founder Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = draftConfig.founderTitleEn,
                            onValueChange = { draftConfig = draftConfig.copy(founderTitleEn = it) },
                            label = { Text("Title (EN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = draftConfig.founderBioEn,
                            onValueChange = { draftConfig = draftConfig.copy(founderBioEn = it) },
                            label = { Text("Official Biography (EN)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = draftConfig.founderBioAr,
                            onValueChange = { draftConfig = draftConfig.copy(founderBioAr = it) },
                            label = { Text("السيرة الرسمية (عربي)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Official Contact Channels Editor
                item {
                    AdminPanelCard {
                        Text(L10n.pick(lang, "قنوات الدعم والتواصل الرسمية", "Official Support & Contact Channels"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draftConfig.supportEmail,
                            onValueChange = { draftConfig = draftConfig.copy(supportEmail = it) },
                            label = { Text("Support Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = draftConfig.supportWhatsApp,
                            onValueChange = { draftConfig = draftConfig.copy(supportWhatsApp = it) },
                            label = { Text("WhatsApp Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = draftConfig.supportTelegram,
                            onValueChange = { draftConfig = draftConfig.copy(supportTelegram = it) },
                            label = { Text("Telegram Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (draftConfig.maintenanceMode != liveConfig.maintenanceMode) {
                                    onRequireMfa { adminViewModel.saveGlobalConfig(draftConfig) }
                                } else {
                                    adminViewModel.saveGlobalConfig(draftConfig)
                                }
                            },
                            enabled = currentRole == AdminRole.SUPER_ADMIN,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Save, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "نشر الإعدادات الحية عبر نور", "Publish Live Global Settings"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            1 -> {
                // Global Announcements Broadcast Manager
                item {
                    Button(
                        onClick = { showNewAnnouncementDialog = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "بث إعلان عام جديد", "Publish Global Announcement"))
                    }
                }
                if (announcements.isEmpty()) {
                    item {
                        AdminPanelCard {
                            Text(L10n.pick(lang, "لا توجد إعلانات منشورة حالياً.", "No announcements published yet."), color = AdminPalette.TextSecondary)
                        }
                    }
                } else {
                    items(announcements, key = { it.id }) { ann ->
                        AdminPanelCard(accentBorder = if (ann.kind == "EMERGENCY") AdminPalette.AccentRose else AdminPalette.BorderSubtle) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Campaign, null, tint = AdminPalette.AccentCyan, modifier = Modifier.size(18.dp))
                                    AdminStatusBadge(label = ann.kind, color = if (ann.kind == "EMERGENCY") AdminPalette.AccentRose else AdminPalette.AccentCyan)
                                }
                                IconButton(onClick = { adminViewModel.deleteAnnouncement(ann.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = AdminPalette.AccentRose)
                                }
                            }
                            Text(L10n.pick(lang, ann.titleAr, ann.titleEn.ifBlank { ann.titleAr }), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(L10n.pick(lang, ann.bodyAr, ann.bodyEn.ifBlank { ann.bodyAr }), style = MaterialTheme.typography.bodySmall, color = AdminPalette.TextSecondary)
                        }
                    }
                }
            }

            2 -> {
                // Support Tickets Inbox
                if (tickets.isEmpty()) {
                    item {
                        AdminPanelCard {
                            Text(L10n.pick(lang, "صندوق تذاكر الدعم فارغ حالياً.", "Support inbox is currently empty."), color = AdminPalette.TextSecondary)
                        }
                    }
                } else {
                    items(tickets, key = { it.id }) { ticket ->
                        AdminPanelCard(accentBorder = if (ticket.status == "OPEN") AdminPalette.AccentAmber.copy(alpha = 0.5f) else AdminPalette.BorderSubtle) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AdminStatusBadge(label = ticket.ticketType, color = AdminPalette.AccentIndigo)
                                AdminStatusBadge(
                                    label = ticket.status,
                                    color = if (ticket.status == "RESOLVED") AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ticket.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${ticket.userName} · ${ticket.userEmail}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentCyan)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = AdminPalette.TextSecondary)
                            if (ticket.adminReply.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Admin Reply: ${ticket.adminReply}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentEmerald)
                            }
                            if (currentRole.canManageSupport) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { replyingTicket = ticket },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Reply, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(L10n.pick(lang, "رد / تحديث الحالة", "Reply & Update"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Announcement Dialog
    if (showNewAnnouncementDialog) {
        var titleAr by remember { mutableStateOf("") }
        var titleEn by remember { mutableStateOf("") }
        var bodyAr by remember { mutableStateOf("") }
        var bodyEn by remember { mutableStateOf("") }
        var kind by remember { mutableStateOf("GENERAL") }

        AlertDialog(
            onDismissRequest = { showNewAnnouncementDialog = false },
            title = { Text(L10n.pick(lang, "بث إعلان عام", "Publish Global Announcement")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("GENERAL", "ISLAMIC_EVENT", "APP_UPDATE", "EMERGENCY")) { k ->
                            AdminFilterPill(label = k, selected = kind == k, onClick = { kind = k })
                        }
                    }
                    OutlinedTextField(value = titleAr, onValueChange = { titleAr = it }, label = { Text("العنوان (عربي)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (EN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyAr, onValueChange = { bodyAr = it }, label = { Text("المحتوى (عربي)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyEn, onValueChange = { bodyEn = it }, label = { Text("Body (EN)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titleAr.isNotBlank() || titleEn.isNotBlank()) {
                        adminViewModel.publishAnnouncement(
                            titleAr = titleAr.ifBlank { titleEn },
                            titleEn = titleEn.ifBlank { titleAr },
                            bodyAr = bodyAr.ifBlank { bodyEn },
                            bodyEn = bodyEn.ifBlank { bodyAr },
                            kind = kind
                        )
                        showNewAnnouncementDialog = false
                    }
                }) {
                    Text(L10n.pick(lang, "نشر", "Publish"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAnnouncementDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Support Ticket Reply Dialog
    replyingTicket?.let { ticket ->
        var replyDraft by remember(ticket) { mutableStateOf(ticket.adminReply) }
        var statusDraft by remember(ticket) { mutableStateOf(if (ticket.status == "OPEN") "IN_PROGRESS" else ticket.status) }
        AlertDialog(
            onDismissRequest = { replyingTicket = null },
            title = { Text(L10n.pick(lang, "الرد على تذكرة الدعم", "Reply to Support Ticket")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ticket.subject, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPEN", "IN_PROGRESS", "RESOLVED").forEach { st ->
                            AdminFilterPill(label = st, selected = statusDraft == st, onClick = { statusDraft = st })
                        }
                    }
                    OutlinedTextField(
                        value = replyDraft,
                        onValueChange = { replyDraft = it },
                        label = { Text(L10n.pick(lang, "رد فريق الدعم", "Admin Reply")) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.replySupportTicket(ticket.id, statusDraft, replyDraft)
                    replyingTicket = null
                }) {
                    Text(L10n.pick(lang, "إرسال الرد", "Save Reply"))
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTicket = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun AdminSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AdminPalette.TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
