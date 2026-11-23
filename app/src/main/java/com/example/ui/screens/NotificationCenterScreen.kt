package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdminMessagePriority
import com.example.data.cloud.AdminMessageType
import com.example.data.cloud.NoorSecurityValidators
import com.example.domain.model.PrayerName
import com.example.notifications.NoorNotificationHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.NoorUriImage
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.NoorViewModel

/**
 * Advanced Notification Center & Admin-to-User Inbox.
 * Displays:
 * 1. Direct, Group, and Broadcast Admin Messages with unread indicator, priority badge,
 *    optional image, and mark-as-read tracking.
 * 2. Official Owner Announcements (General, Islamic Events, App Updates, Emergency).
 * 3. Personal Worship & Prayer Notification preferences + Non-Essential opt-out toggle.
 */
@Composable
fun NotificationCenterScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val announcements by viewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val adminMessages by viewModel.userAdminMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadAdminMessagesCount.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val nonEssentialEnabled by viewModel.nonEssentialNotificationsEnabled.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.sendTestNotification()
    }

    val myUid = currentUser?.uid ?: "local_user"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_center_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "notif_header") {
            NoorScreenHeader(
                title = L10n.pick(lang, "مركز الإشعارات والرسائل", "Notification Center"),
                subtitle = L10n.pick(
                    lang,
                    "رسائل الإدارة ($unreadCount غير مقروءة) وتنبيهات العبادة الشخصية",
                    "Admin Inbox ($unreadCount unread) & personal worship alerts"
                ),
                onBack = onBack
            )
        }

        if (!globalConfig.featureNotificationsEnabled) {
            item(key = "notif_admin_disabled") {
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    goldBorder = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "تنبيه: الإشعارات الإدارية العامة متوقفة مؤقتاً من لوحة المشرف الأعلى. تبقى منبهات الصلاة الشخصية فعالة.",
                            "Note: Global admin notifications are temporarily paused in Super Admin settings. Personal prayer alerts remain active."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Admin-to-User Messages Inbox (Direct, Group, Broadcast)
        item(key = "admin_msgs_header") {
            NoorSectionHeader(
                title = L10n.pick(lang, "صندوق الرسائل الإدارية ($unreadCount جديد)", "Admin Messages Inbox ($unreadCount unread)"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (adminMessages.isEmpty()) {
            item(key = "admin_msgs_empty") {
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "لا توجد رسائل إدارية في صندوقك حالياً.",
                            "No admin messages in your inbox right now."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(adminMessages, key = { "inbox_msg_${it.id}" }) { msg ->
                val isRead = msg.readByUids.contains(myUid) || msg.readByUids.contains("local_user")
                val msgType = AdminMessageType.fromKey(msg.messageType)
                val msgPriority = AdminMessagePriority.fromKey(msg.priority)
                val tint = when (msgPriority) {
                    AdminMessagePriority.URGENT -> SemanticError
                    AdminMessagePriority.HIGH -> GoldPrimary
                    AdminMessagePriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                    AdminMessagePriority.NORMAL -> SemanticSuccess
                }

                LiquidGlassCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("inbox_message_card_${msg.id}"),
                    goldBorder = !isRead || msgPriority == AdminMessagePriority.URGENT,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRead) Icons.Rounded.MarkEmailRead else Icons.Rounded.MarkEmailUnread,
                                contentDescription = null,
                                tint = tint
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, msg.titleAr, msg.titleEn.ifBlank { msg.titleAr }),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.ExtraBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tint.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = L10n.pick(lang, msgType.labelAr, msgType.labelEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tint,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = L10n.pick(lang, msgPriority.labelAr, msgPriority.labelEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tint,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (msg.imageUrl.isNotBlank() && NoorSecurityValidators.isSafeOptionalImageUri(msg.imageUrl)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NoorUriImage(
                            uriString = msg.imageUrl,
                            contentDescription = msg.titleAr,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, msg.bodyAr, msg.bodyEn.ifBlank { msg.bodyAr }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isRead) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.markAdminMessageRead(msg.id) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("mark_read_msg_${msg.id}")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "تحديد كمقروء", "Mark as Read"))
                        }
                    }
                }
            }
        }

        // Section 2: Official Announcements
        item(key = "announcements_header") {
            NoorSectionHeader(
                title = L10n.pick(lang, "الإعلانات والتنبيهات الرسمية", "Official Announcements"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (announcements.isEmpty()) {
            item(key = "announcements_empty") {
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "لا توجد إعلانات عامة جديدة حالياً. ستظهر هنا التحديثات والمناسبات الإسلامية والتنبيهات الهامة.",
                            "No active announcements right now. Islamic event reminders, updates, and notices will appear here."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(announcements, key = { "ann_${it.id}" }) { ann ->
                val (icon, tint) = when (ann.kind) {
                    "EMERGENCY" -> Icons.Rounded.Warning to SemanticError
                    "ISLAMIC_EVENT" -> Icons.Rounded.Event to GoldPrimary
                    "APP_UPDATE" -> Icons.Rounded.SystemUpdate to SemanticSuccess
                    else -> Icons.Rounded.Campaign to noorColors.goldAccent
                }
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    goldBorder = ann.kind == "EMERGENCY" || ann.kind == "ISLAMIC_EVENT",
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = tint)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, ann.titleAr, ann.titleEn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ann.kind.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = tint
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, ann.bodyAr, ann.bodyEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 3: Personal Worship Reminders & Opt-Out Preferences
        item(key = "personal_reminders_header") {
            NoorSectionHeader(
                title = L10n.pick(lang, "تفضيلات الإشعارات وتنبيهات العبادة", "Notification Preferences & Worship Reminders"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "personal_reminders_card") {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                NotifSwitchRow(
                    label = L10n.pick(
                        lang,
                        "الرسائل التعليمية والتذكيرات غير العاجلة",
                        "Educational & Non-Essential Admin Notices"
                    ),
                    checked = nonEssentialEnabled,
                    onCheckedChange = { viewModel.setNonEssentialNotificationsEnabled(it) }
                )
                PrayerName.fardPrayers.forEach { prayer ->
                    NotifSwitchRow(
                        label = L10n.pick(lang, "تنبيه صلاة ${L10n.prayerName(prayer, lang)}", "${L10n.prayerName(prayer, lang)} Prayer"),
                        checked = prefs.isPrayerNotificationEnabled(prayer),
                        onCheckedChange = { viewModel.setPrayerNotification(prayer, it) }
                    )
                }
                NotifSwitchRow(
                    label = L10n.pick(lang, "أذكار الصباح (٦:٣٠ ص)", "Morning Adhkar (6:30 AM)"),
                    checked = prefs.morningAdhkarReminderEnabled,
                    onCheckedChange = { viewModel.setReminder("morning", it) }
                )
                NotifSwitchRow(
                    label = L10n.pick(lang, "أذكار المساء (٥:٣٠ م)", "Evening Adhkar (5:30 PM)"),
                    checked = prefs.eveningAdhkarReminderEnabled,
                    onCheckedChange = { viewModel.setReminder("evening", it) }
                )
                NotifSwitchRow(
                    label = L10n.pick(lang, "ورد القرآن اليومي (٩:٠٠ م)", "Daily Quran Reminder (9:00 PM)"),
                    checked = prefs.quranReminderEnabled,
                    onCheckedChange = { viewModel.setReminder("quran", it) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NoorNotificationHelper.hasNotificationPermission(context)
                        ) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.sendTestNotification()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.pick(lang, "اختبار التنبيهات الآن", "Test Notification Now"))
                }
            }
        }
    }
}

@Composable
private fun NotifSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
