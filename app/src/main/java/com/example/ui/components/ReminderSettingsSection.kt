package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AdhanCatalog
import com.example.audio.AdhanVoiceOption
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.notifications.NoorNotificationHelper
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import java.util.Locale

private data class SpiritualReminderConfig(
    val type: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val subtitleUz: String,
    val icon: ImageVector,
    val accentColor: Color,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val soundId: String,
    val presets: List<Pair<Int, Int>>
)

/**
 * Liquid Glass panel for Morning Prayer/Adhkar, Evening Adhkar, and Tahajjud/Qiyam Al-Layl reminders
 * with editable times, built-in Islamic voices, and zero-permission device audio import.
 */
@Composable
fun SpiritualRemindersStudioSection(
    prefs: UserPreferences,
    viewModel: NoorViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val previewingVoiceId by viewModel.previewingAdhanVoiceId.collectAsStateWithLifecycle()

    val allVoices = remember(prefs.customAdhanRecords) {
        AdhanCatalog.allVoices(prefs.customAdhanRecords, context)
    }

    var editingTimeFor by remember { mutableStateOf<SpiritualReminderConfig?>(null) }
    var editingSoundFor by remember { mutableStateOf<SpiritualReminderConfig?>(null) }
    var pendingImportType by remember { mutableStateOf<String?>(null) }

    // Zero-permission Storage Access Framework audio picker (MP3, M4A, WAV, OGG)
    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetType = pendingImportType
        pendingImportType = null
        if (uri != null && targetType != null) {
            viewModel.importCustomReminderSound(targetType, uri)
        }
    }

    val reminderConfigs = listOf(
        SpiritualReminderConfig(
            type = "morning",
            titleAr = "تذكير صلاة الصبح وأذكار الصباح",
            titleEn = "Morning Prayer & Adhkar",
            titleUz = "Bomdod va tonggi zikrlar",
            subtitleAr = "تذكير يومي لصلاة الصبح وأذكار الصباح بصوتك المختار",
            subtitleEn = "Daily morning prayer & fortress adhkar alert",
            subtitleUz = "Har kungi Bomdod va tonggi zikr eslatmasi",
            icon = Icons.Rounded.WbTwilight,
            accentColor = Color(0xFFF59E0B),
            enabled = prefs.morningAdhkarReminderEnabled,
            hour = prefs.morningAdhkarHour,
            minute = prefs.morningAdhkarMinute,
            soundId = prefs.morningReminderSoundId,
            presets = listOf(5 to 0, 5 to 30, 6 to 0, 6 to 30)
        ),
        SpiritualReminderConfig(
            type = "evening",
            titleAr = "تذكير المساء وأذكار المساء",
            titleEn = "Evening Adhkar Reminder",
            titleUz = "Kechki zikrlar eslatmasi",
            subtitleAr = "تذكير يومي لأذكار المساء والسكينة في الوقت الذي تحدده",
            subtitleEn = "Daily evening remembrance & tranquility alert",
            subtitleUz = "Har kungi kechki zikr va sokinlik eslatmasi",
            icon = Icons.Rounded.WbSunny,
            accentColor = Color(0xFFFB923C),
            enabled = prefs.eveningAdhkarReminderEnabled,
            hour = prefs.eveningAdhkarHour,
            minute = prefs.eveningAdhkarMinute,
            soundId = prefs.eveningReminderSoundId,
            presets = listOf(16 to 30, 17 to 0, 17 to 30, 18 to 0)
        ),
        SpiritualReminderConfig(
            type = "tahajjud",
            titleAr = "تذكير صلاة التهجد وقيام الليل",
            titleEn = "Tahajjud & Qiyam Al-Layl",
            titleUz = "Tahajjud va tungi namoz (Qiyomul-layl)",
            subtitleAr = "تنبيه خاص للثلث الأخير من الليل وقيام الليل بصوت مخصص",
            subtitleEn = "Wake up in the blessed last third of the night",
            subtitleUz = "Kechaning muborak so'nggi uchdan birida uyg'oning",
            icon = Icons.Rounded.NightsStay,
            accentColor = GoldBright,
            enabled = prefs.tahajjudReminderEnabled,
            hour = prefs.tahajjudReminderHour,
            minute = prefs.tahajjudReminderMinute,
            soundId = prefs.tahajjudReminderSoundId,
            presets = listOf(2 to 0, 2 to 30, 3 to 0, 3 to 30)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NoorSectionHeader(
            title = L10n.pick(
                lang,
                "تذكيرات الصباح والمساء والتهجد (بصوت مخصص)",
                "Morning, Evening & Tahajjud Reminders",
                "Tonggi, Kechki va Tahajjud eslatmalari"
            )
        )

        reminderConfigs.forEach { cfg ->
            val selectedVoice = remember(cfg.soundId, allVoices) {
                allVoices.firstOrNull { it.id == cfg.soundId } ?: allVoices.first()
            }
            val voiceLabel = L10n.pick(lang, selectedVoice.nameAr, selectedVoice.nameEn)
            val isPreviewingThis = previewingVoiceId == selectedVoice.id

            LiquidGlassCard(
                goldBorder = cfg.type == "tahajjud" && cfg.enabled,
                strong = cfg.type == "tahajjud",
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.testTag("reminder_card_${cfg.type}")
            ) {
                // Header row with icon, title, subtitle, and master switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(cfg.accentColor.copy(alpha = 0.34f), cfg.accentColor.copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, cfg.accentColor.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cfg.icon,
                            contentDescription = null,
                            tint = cfg.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(lang, cfg.titleAr, cfg.titleEn, cfg.titleUz),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cfg.type == "tahajjud") noorColors.goldAccent else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = L10n.pick(lang, cfg.subtitleAr, cfg.subtitleEn, cfg.subtitleUz),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = cfg.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NoorNotificationHelper.hasNotificationPermission(context)
                            ) {
                                onRequestNotificationPermission()
                            }
                            viewModel.setReminder(cfg.type, enabled, cfg.hour, cfg.minute)
                        },
                        modifier = Modifier.testTag("reminder_toggle_${cfg.type}")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Time & Custom Sound Glass Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time selector pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (noorColors.isDark) Color(0x5212382E) else Color(0xD9E4F4EE)
                            )
                            .border(1.dp, noorColors.glassBorderBottom, RoundedCornerShape(16.dp))
                            .clickable { editingTimeFor = cfg }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("reminder_time_${cfg.type}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = noorColors.goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatReminderTime(cfg.hour, cfg.minute, lang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Custom Sound selector pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (noorColors.isDark) Color(0x5212382E) else Color(0xD9E4F4EE)
                            )
                            .border(1.dp, noorColors.goldAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .clickable { editingSoundFor = cfg }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("reminder_sound_${cfg.type}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedVoice.isCustomImported) Icons.Rounded.AudioFile else Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = noorColors.goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = voiceLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick actions: Pick Sound from Device, Preview Sound, Test Alert Now
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "تخصيص الصوت", "Custom Sound", "Maxsus ovoz"),
                        selected = false,
                        icon = Icons.Rounded.GraphicEq,
                        onClick = { editingSoundFor = cfg },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = if (isPreviewingThis) {
                            L10n.pick(lang, "إيقاف", "Stop", "To'xtatish")
                        } else {
                            L10n.pick(lang, "معاينة", "Preview", "Tinglash")
                        },
                        selected = isPreviewingThis,
                        icon = if (isPreviewingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        onClick = {
                            if (isPreviewingThis) {
                                viewModel.stopAdhanPreview()
                            } else {
                                viewModel.previewOrStopAdhan(selectedVoice)
                            }
                        }
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "تجربة", "Test Alert", "Sinov"),
                        selected = false,
                        icon = Icons.Rounded.NotificationsActive,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NoorNotificationHelper.hasNotificationPermission(context)
                            ) {
                                onRequestNotificationPermission()
                            } else {
                                viewModel.sendTestReminderAlert(cfg.type)
                            }
                        },
                        modifier = Modifier.testTag("reminder_test_${cfg.type}")
                    )
                }
            }
        }
    }

    // Time Picker Dialog
    editingTimeFor?.let { cfg ->
        ReminderTimePickerDialog(
            config = cfg,
            onDismiss = { editingTimeFor = null },
            onConfirm = { newHour, newMinute ->
                viewModel.setReminder(cfg.type, true, newHour, newMinute)
                editingTimeFor = null
            }
        )
    }

    // Custom Sound Picker Dialog (Built-in Islamic Voices + Imported Custom Audio + Device File Picker)
    editingSoundFor?.let { cfg ->
        ReminderSoundPickerDialog(
            config = cfg,
            allVoices = allVoices,
            selectedVoiceId = cfg.soundId,
            previewingVoiceId = previewingVoiceId,
            onSelectVoice = { voice ->
                viewModel.setReminderSound(cfg.type, voice.id)
                editingSoundFor = null
            },
            onPreviewVoice = { voice ->
                if (previewingVoiceId == voice.id) {
                    viewModel.stopAdhanPreview()
                } else {
                    viewModel.previewOrStopAdhan(voice)
                }
            },
            onImportFromDevice = {
                pendingImportType = cfg.type
                importAudioLauncher.launch(arrayOf("audio/*"))
                editingSoundFor = null
            },
            onStopAll = {
                viewModel.stopActiveAdhanAlarm()
            },
            onDismiss = {
                viewModel.stopAdhanPreview()
                editingSoundFor = null
            }
        )
    }
}

@Composable
private fun ReminderTimePickerDialog(
    config: SpiritualReminderConfig,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var hour by remember(config) { mutableIntStateOf(config.hour) }
    var minute by remember(config) { mutableIntStateOf(config.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = L10n.pick(lang, "ضبط وقت التذكير", "Set Reminder Time", "Eslatma vaqtini belgilash"),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L10n.pick(lang, config.titleAr, config.titleEn, config.titleUz),
                    style = MaterialTheme.typography.bodySmall,
                    color = noorColors.goldAccent
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeNumberStepper(
                        label = L10n.pick(lang, "الساعة", "Hour", "Soat"),
                        value = hour,
                        onIncrement = { hour = (hour + 1) % 24 },
                        onDecrement = { hour = (hour + 23) % 24 }
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    TimeNumberStepper(
                        label = L10n.pick(lang, "الدقيقة", "Minute", "Daqiqa"),
                        value = minute,
                        onIncrement = { minute = (minute + 5) % 60 },
                        onDecrement = { minute = (minute + 55) % 60 }
                    )
                }

                Text(
                    text = L10n.pick(lang, "أوقات مقترحة", "Quick Presets", "Tezkor vaqtlar"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    config.presets.forEach { (ph, pm) ->
                        LiquidGlassChip(
                            label = String.format(Locale.US, "%02d:%02d", ph, pm),
                            selected = hour == ph && minute == pm,
                            onClick = {
                                hour = ph
                                minute = pm
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) },
                modifier = Modifier.testTag("reminder_time_save_button")
            ) {
                Text(L10n.pick(lang, "حفظ", "Save", "Saqlash"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
            }
        }
    )
}

@Composable
private fun TimeNumberStepper(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val noorColors = LocalNoorColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.2f))
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $label", tint = noorColors.goldAccent)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = String.format(Locale.US, "%02d", value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.2f))
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label", tint = noorColors.goldAccent)
        }
    }
}

@Composable
private fun ReminderSoundPickerDialog(
    config: SpiritualReminderConfig,
    allVoices: List<AdhanVoiceOption>,
    selectedVoiceId: String,
    previewingVoiceId: String?,
    onSelectVoice: (AdhanVoiceOption) -> Unit,
    onPreviewVoice: (AdhanVoiceOption) -> Unit,
    onImportFromDevice: () -> Unit,
    onStopAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = L10n.pick(lang, "اختر صوت التذكير", "Choose Reminder Sound", "Eslatma ovozini tanlash"),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L10n.pick(lang, config.titleAr, config.titleEn, config.titleUz),
                    style = MaterialTheme.typography.bodySmall,
                    color = noorColors.goldAccent
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Zero-permission SAF import audio file button
                Button(
                    onClick = onImportFromDevice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reminder_import_audio_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        L10n.pick(
                            lang,
                            "استيراد صوت من جهازي (MP3 / M4A / WAV)",
                            "Import Audio from Device",
                            "Qurilmadan audio fayl tanlash"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allVoices, key = { it.id }) { voice ->
                        val isSelected = voice.id == selectedVoiceId
                        val isPlaying = previewingVoiceId == voice.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) EmeraldBright.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) GoldPrimary else noorColors.glassBorderBottom,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectVoice(voice) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, voice.nameAr, voice.nameEn),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = L10n.pick(lang, voice.muezzinAr, voice.muezzinEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onPreviewVoice(voice) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Preview",
                                    tint = noorColors.goldAccent
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إغلاق", "Close", "Yopish"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onStopAll) {
                Text(L10n.pick(lang, "إيقاف الصوت", "Stop Sound", "Ovozni to'xtatish"))
            }
        }
    )
}

fun formatReminderTime(hour: Int, minute: Int, lang: AppLanguage): String {
    val safeH = hour.coerceIn(0, 23)
    val safeM = minute.coerceIn(0, 59)
    val isAm = safeH < 12
    val h12 = when {
        safeH == 0 -> 12
        safeH > 12 -> safeH - 12
        else -> safeH
    }
    return when (lang) {
        AppLanguage.ARABIC -> String.format(Locale.US, "%02d:%02d %s", h12, safeM, if (isAm) "ص" else "م")
        AppLanguage.UZBEK -> String.format(Locale.US, "%02d:%02d", safeH, safeM)
        AppLanguage.ENGLISH -> String.format(Locale.US, "%02d:%02d %s", h12, safeM, if (isAm) "AM" else "PM")
    }
}
