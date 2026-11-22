package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AdhanCatalog
import com.example.audio.AdhanVoiceOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.notifications.NoorNotificationHelper
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

@Composable
fun AdhanSettingsScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val isAr = lang == AppLanguage.ARABIC
    val noorColors = LocalNoorColors.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val playingVoiceId by viewModel.previewingAdhanVoiceId.collectAsStateWithLifecycle()
    val isBuffering by viewModel.adhanPreviewBuffering.collectAsStateWithLifecycle()

    var assigningPrayer by remember { mutableStateOf<PrayerName?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAdhanPreview()
        }
    }

    // Zero-permission Storage Access Framework Audio Importer (MP3, M4A, WAV)
    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomAdhanAudio(uri)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setPersistentPrayerNotificationEnabled(true)
        }
    }

    val allVoices = remember(prefs.customAdhanRecords) {
        AdhanCatalog.allVoices(prefs.customAdhanRecords)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adhan_settings_screen"),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NoorScreenHeader(
                title = L10n.pick(lang, "تخصيص أصوات الأذان", "Adhan Sound Settings"),
                subtitle = L10n.pick(
                    lang,
                    "الشيخ ماهر المعيقلي، مؤذنو الحرمين، استيراد ملفات MP3/M4A/WAV وتخصيص كل صلاة",
                    "Sheikh Maher Al-Muaiqly, renowned muezzins, device audio import & per-prayer sounds"
                ),
                onBack = onBack
            )
        }

        // Master Controls + Persistent Prayer Bar + Live Adhan Test
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp), goldBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = noorColors.goldAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = L10n.pick(lang, "تشغيل صوت الأذان عند دخول الوقت", "Play Adhan Audio at Prayer Time"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(lang, "يتم تشغيل المؤذن المحدد مع بديل تلقائي مضمون", "Plays selected voice with guaranteed default fallback"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = prefs.adhanSoundEnabled,
                        onCheckedChange = { viewModel.setAdhanSoundEnabled(it) },
                        modifier = Modifier.testTag("adhan_sound_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(lang, "احترام الوضع الصامت والاهتزاز", "Respect Silent & Vibrate Mode"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = L10n.pick(lang, "كتم صوت الأذان تلقائياً عندما يكون الهاتف صامتاً", "Automatically mute Adhan when phone is on silent"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.adhanRespectSilentMode,
                        onCheckedChange = { viewModel.setAdhanRespectSilentMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(lang, "مواقيت الصلاة المستمرة في شريط الإشعارات", "Persistent Prayer Times Notification"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = L10n.pick(lang, "يعرض الصلوات الخمس والعد التنازلي المباشر في الخلفية", "Shows all 5 prayers & live countdown in the notification bar"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.persistentPrayerNotificationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NoorNotificationHelper.hasNotificationPermission(context)
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setPersistentPrayerNotificationEnabled(enabled)
                            }
                        },
                        modifier = Modifier.testTag("persistent_prayer_notif_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testAdhanPlaybackNow(PrayerName.FAJR) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_adhan_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "تجربة الأذان الآن", "Test Adhan Now"))
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopActiveAdhanAlarm() },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("stop_adhan_now_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "إيقاف", "Stop"))
                    }
                }
            }
        }

        // Import Audio From Device (MP3 / M4A / WAV)
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = noorColors.goldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "استيراد صوت أذان من جهازك", "Import Audio From Device"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "اختر ملف أذان محفوظ على هاتفك بصيغة MP3 أو M4A أو WAV. يتم نسخه محلياً داخل التطبيق ليعمل دائماً حتى بعد إغلاق التطبيق.",
                        "Pick an audio file from your phone (MP3, M4A, or WAV). It is safely copied inside NOOR so it stays available after restart."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        importAudioLauncher.launch(AdhanCatalog.supportedMimeTypes + arrayOf("audio/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_adhan_audio_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.pick(lang, "استيراد ملف صوتي (MP3 · M4A · WAV)", "Import Audio File (MP3 · M4A · WAV)"))
                }
            }
        }

        // Custom Imported Voices (if any)
        if (prefs.customAdhanRecords.isNotEmpty()) {
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "أصواتك المستوردة", "Your Imported Adhan Files"),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(prefs.customAdhanRecords, key = { it.id }) { rec ->
                val option = allVoices.firstOrNull { it.id == rec.id } ?: return@items
                val isSelected = prefs.adhanConfig.defaultVoiceId == rec.id
                val isPlaying = playingVoiceId == rec.id
                AdhanVoiceCard(
                    voice = option,
                    isAr = isAr,
                    isSelected = isSelected,
                    isPlaying = isPlaying,
                    isBuffering = isPlaying && isBuffering,
                    onSelect = { viewModel.setDefaultAdhanVoice(rec.id) },
                    onPreviewToggle = { viewModel.previewOrStopAdhan(option) },
                    onDelete = { viewModel.deleteCustomAdhanRecord(rec) }
                )
            }
        }

        // Curated Adhan Recordings (Sheikh Maher Al-Muaiqly first!)
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "أصوات المؤذنين المختارة", "Curated Adhan Voices"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        items(AdhanCatalog.builtInVoices, key = { it.id }) { voice ->
            val isSelected = prefs.adhanConfig.defaultVoiceId == voice.id
            val isPlaying = playingVoiceId == voice.id
            AdhanVoiceCard(
                voice = voice,
                isAr = isAr,
                isSelected = isSelected,
                isPlaying = isPlaying,
                isBuffering = isPlaying && isBuffering,
                onSelect = { viewModel.setDefaultAdhanVoice(voice.id) },
                onPreviewToggle = { viewModel.previewOrStopAdhan(voice) },
                onDelete = null
            )
        }

        // Per-Prayer Adhan Voice Assignment
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "تخصيص صوت مختلف لكل صلاة", "Per-Prayer Adhan Sound"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "يمكنك اختيار مؤذن مخصص لكل صلاة (مثل صوت خاص لصلاة الفجر) أو تركها على الصوت الافتراضي.",
                        "Assign a distinct Adhan voice to each prayer (such as a special Fajr Adhan) or keep the default voice."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                PrayerName.fardPrayers.forEach { prayer ->
                    val hasOverride = prefs.adhanConfig.hasCustomOverride(prayer)
                    val resolved = AdhanCatalog.resolveVoice(
                        prefs.adhanConfig.voiceIdForPrayer(prayer),
                        prefs.customAdhanRecords
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { assigningPrayer = prayer }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.prayerName(prayer, lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasOverride) {
                                    L10n.pick(lang, resolved.nameAr, resolved.nameEn)
                                } else {
                                    L10n.pick(lang, "الافتراضي (${resolved.nameAr})", "Default (${resolved.nameEn})")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasOverride) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { assigningPrayer = prayer }) {
                            Text(L10n.pick(lang, "تغيير", "Change"), color = noorColors.goldAccent)
                        }
                    }
                }
            }
        }

        // Pin Adhan Settings Shortcut to Home Screen
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(lang, "اختصار الشاشة الرئيسية", "Home Screen Shortcut"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = L10n.pick(lang, "أضف اختصاراً سريعاً لإعدادات الأذان على شاشة هاتفك", "Pin an Adhan Settings shortcut directly to your home screen"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val ok = NoorShortcutManager.requestPinShortcut(
                                context,
                                NoorShortcutManager.TARGET_ADHAN_SETTINGS,
                                isAr
                            )
                            if (!ok) {
                                viewModel.showBannerMessage(
                                    L10n.pick(lang, "يمكنك الضغط المطوّل على أيقونة نور للوصول لجميع الاختصارات", "Long-press the NOOR app icon to access all quick shortcuts")
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "تثبيت", "Pin"))
                    }
                }
            }
        }
    }

    // Per-Prayer Voice Picker Dialog
    assigningPrayer?.let { targetPrayer ->
        val currentSpecific = when (targetPrayer) {
            PrayerName.FAJR -> prefs.adhanConfig.fajrVoiceId
            PrayerName.DHUHR -> prefs.adhanConfig.dhuhrVoiceId
            PrayerName.ASR -> prefs.adhanConfig.asrVoiceId
            PrayerName.MAGHRIB -> prefs.adhanConfig.maghribVoiceId
            PrayerName.ISHA -> prefs.adhanConfig.ishaVoiceId
            PrayerName.SUNRISE -> ""
        }
        AlertDialog(
            onDismissRequest = { assigningPrayer = null },
            title = {
                Text(
                    L10n.pick(
                        lang,
                        "صوت أذان صلاة ${L10n.prayerName(targetPrayer, lang)}",
                        "${L10n.prayerName(targetPrayer, lang)} Adhan Voice"
                    )
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setPrayerSpecificAdhanVoice(targetPrayer, "")
                                    assigningPrayer = null
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSpecific.isBlank(),
                                onClick = {
                                    viewModel.setPrayerSpecificAdhanVoice(targetPrayer, "")
                                    assigningPrayer = null
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                            Text(
                                text = L10n.pick(lang, "استخدام الصوت الافتراضي", "Use Default Adhan Voice"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    items(allVoices, key = { "dlg_${it.id}" }) { voice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setPrayerSpecificAdhanVoice(targetPrayer, voice.id)
                                    assigningPrayer = null
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSpecific == voice.id,
                                onClick = {
                                    viewModel.setPrayerSpecificAdhanVoice(targetPrayer, voice.id)
                                    assigningPrayer = null
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, voice.nameAr, voice.nameEn),
                                    style = MaterialTheme.typography.bodyMedium,
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
                            IconButton(onClick = { viewModel.previewOrStopAdhan(voice) }) {
                                Icon(
                                    imageVector = if (playingVoiceId == voice.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = GoldPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assigningPrayer = null }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }
}

@Composable
private fun AdhanVoiceCard(
    voice: AdhanVoiceOption,
    isAr: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSelect: () -> Unit,
    onPreviewToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val noorColors = LocalNoorColors.current
    LiquidGlassCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .testTag("adhan_voice_${voice.id}"),
        goldBorder = isSelected || isPlaying,
        onClick = onSelect,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) GoldPrimary.copy(alpha = 0.28f)
                        else EmeraldPrimary.copy(alpha = 0.22f)
                    )
                    .border(
                        1.dp,
                        if (isPlaying) GoldBright else GoldPrimary.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .clickable(onClick = onPreviewToggle)
                    .testTag("preview_adhan_${voice.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = GoldBright
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Stop preview" else "Preview Adhan",
                        tint = GoldBright
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAr) voice.nameAr else voice.nameEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isAr) voice.muezzinAr else voice.muezzinEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete imported Adhan",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = noorColors.goldAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
