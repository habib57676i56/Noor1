package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PrayerName
import com.example.notifications.NoorNotificationHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.NoorViewModel

/**
 * Part 8 — Advanced Notification Center.
 * Displays live Owner global announcements (General, Islamic Events, App Updates, Emergency)
 * alongside independent personal worship & prayer notification preferences.
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
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.sendTestNotification()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_center_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NoorScreenHeader(
                title = L10n.pick(lang, "مركز الإشعارات والتنبيهات", "Notification Center"),
                subtitle = L10n.pick(lang, "إعلانات نور الرسمية وتنبيهات العبادة الشخصية", "Official NOOR broadcasts & personal worship alerts"),
                onBack = onBack
            )
        }

        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "الإعلانات والتنبيهات الرسمية", "Official Announcements"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (announcements.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "لا توجد إعلانات جديدة حالياً. ستظهر هنا التحديثات والمناسبات الإسلامية والتنبيهات الهامة.",
                            "No active announcements right now. Islamic event reminders, updates, and notices will appear here."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(announcements, key = { it.id }) { ann ->
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

        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "تنبيهات عبادتك الشخصية", "Your Personal Worship Reminders"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
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
