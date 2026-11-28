package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
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
import com.example.audio.AdhanDownloadState
import com.example.audio.AdhanVoiceOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.notifications.NoorAlarmScheduler
import com.example.notifications.NoorNotificationHelper
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess
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
    val downloadStates by viewModel.adhanDownloadStates.collectAsStateWithLifecycle()

    var assigningPrayer by remember { mutableStateOf<PrayerName?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAdhanPreview() }
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importCustomAdhanAudio(uri)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setPersistentPrayerNotificationEnabled(true)
    }

    val allVoices = remember(prefs.customAdhanRecords, downloadStates) {
        AdhanCatalog.allVoices(prefs.customAdhanRecords, context)
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
                title = L10n.pick(lang, "نظام الأذان التلقائي", "Automatic Adhan System"),
                subtitle = L10n.pick(
                    lang,
                    "الافتراضي: الشيخ ناصر القطامي · تنزيل للعمل بدون إنترنت · تخصيص كل صلاة",
                    "Default: Sheikh Nasser Al-Qatami · Verified offline download · Per-prayer control"
                ),
                onBack = onBack
            )
        }

        // Master Controls, Volume, Mode & Live / Background Test
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
                                text = L10n.pick(lang, "تشغيل الأذان التلقائي عند دخول الوقت", "Auto-Play Adhan at Prayer Time"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(lang, "يعمل في الخلفية وبلا إنترنت بعد تنزيل الصوت", "Works in background & offline after download"),
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
                            text = L10n.pick(lang, "وضع الإشعار فقط (بدون أذان كامل)", "Notification-Only Mode"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = L10n.pick(lang, "عرض تنبيه الصلاة فقط دون تشغيل ملف الأذان الكامل", "Show prayer notification without playing full Adhan audio"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.adhanNotificationOnly,
                        onCheckedChange = { viewModel.setAdhanNotificationOnly(it) },
                        modifier = Modifier.testTag("adhan_notification_only_switch")
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
                            text = L10n.pick(lang, "كتم الأذان عند الصامت / عدم الإزعاج", "Mute Adhan in Silent / DND Mode"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (prefs.adhanRespectSilentMode) {
                                L10n.pick(lang, "سيتم كتم الأذان عند تفعيل الصامت أو عدم الإزعاج", "Adhan will stay muted when phone is silent or on DND")
                            } else {
                                L10n.pick(lang, "الأذان يعمل دائماً حتى مع تفعيل عدم الإزعاج أو إغلاق الشاشة ✓", "Adhan always plays even during Do Not Disturb or locked screen ✓")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prefs.adhanRespectSilentMode) MaterialTheme.colorScheme.onSurfaceVariant else SemanticSuccess
                        )
                    }
                    Switch(
                        checked = prefs.adhanRespectSilentMode,
                        onCheckedChange = { viewModel.setAdhanRespectSilentMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Independent Adhan Volume Slider
                Text(
                    text = L10n.pick(
                        lang,
                        "مستوى صوت الأذان المستقل: ${(prefs.adhanVolume * 100).toInt()}%",
                        "Independent Adhan Volume: ${(prefs.adhanVolume * 100).toInt()}%"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = noorColors.goldAccent
                )
                Slider(
                    value = prefs.adhanVolume,
                    onValueChange = { viewModel.setAdhanVolume(it) },
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            text = L10n.pick(lang, "يعرض الصلوات الخمس والعد التنازلي المباشر في الخلفية", "Shows all 5 prayers & live countdown in notification bar"),
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

                Spacer(modifier = Modifier.height(10.dp))

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
                        Text(L10n.pick(lang, "تجربة الآن", "Test Now"))
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.scheduleTestAdhanInSeconds(PrayerName.FAJR, 15) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_test_adhan_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "اختبار خلفي (+15ث)", "Bg Test (+15s)"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { viewModel.restoreAdhanDefaults() },
                        modifier = Modifier.testTag("restore_adhan_defaults_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "الافتراضي", "Reset"))
                    }
                }
            }
        }

        // Default Highlighted Banner: Sheikh Nasser Al-Qatami Offline Download
        item {
            val qatami = AdhanCatalog.builtInVoices.first()
            val qState = downloadStates[qatami.id] ?: AdhanDownloadState.Idle
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp), strong = true, goldBorder = true) {
                Text(
                    text = L10n.pick(lang, "المؤذن الافتراضي: الشيخ ناصر القطامي", "Default Reciter: Sheikh Nasser Al-Qatami"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "نزّل التسجيل الكامل للشيخ ناصر القطامي محلياً للتحقق من سلامته وضمان عمله التلقائي بدون إنترنت عند دخول وقت الصلاة.",
                        "Download Sheikh Nasser Al-Qatami's full Adhan locally with integrity verification for reliable offline prayer playback."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (qState) {
                    is AdhanDownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { qState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.pick(lang, "جارٍ التنزيل والتحقق... ${(qState.progress * 100).toInt()}%", "Downloading & verifying... ${(qState.progress * 100).toInt()}%"),
                            style = MaterialTheme.typography.labelSmall,
                            color = noorColors.goldAccent
                        )
                    }
                    is AdhanDownloadState.VerifiedOffline -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = SemanticSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "جاهز للعمل بدون إنترنت ✓ (${qState.sizeBytes / 1024} ك.ب — تم التحقق من صلاحية الملف)",
                                    "Verified Offline Ready ✓ (${qState.sizeBytes / 1024} KB)"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = SemanticSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.downloadAdhanForOffline(qatami) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_qatami_adhan_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(L10n.pick(lang, "تنزيل أذان ناصر القطامي الكامل (بدون إنترنت)", "Download Nasser Al-Qatami Full Adhan (Offline)"))
                        }
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
                        "اختر ملف أذان محفوظ على هاتفك بصيغة MP3 أو M4A أو WAV. يتم نسخه محلياً ليعمل دائماً بدون إنترنت.",
                        "Pick an audio file from your phone (MP3, M4A, or WAV). Copied locally for offline reliability."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { importAudioLauncher.launch(AdhanCatalog.supportedMimeTypes + arrayOf("audio/*")) },
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

        // Custom Imported Voices
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
                    downloadState = AdhanDownloadState.VerifiedOffline(rec.localFilePath, 0L, 0L),
                    onSelect = { viewModel.setDefaultAdhanVoice(rec.id) },
                    onPreviewToggle = { viewModel.previewOrStopAdhan(option) },
                    onDownload = null,
                    onDelete = { viewModel.deleteCustomAdhanRecord(rec) }
                )
            }
        }

        // Curated Adhan Recordings (Sheikh Nasser Al-Qatami FIRST!)
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "أصوات المؤذنين المعتمدة", "Authorized Adhan Voices"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        items(AdhanCatalog.builtInVoices, key = { it.id }) { voice ->
            val isSelected = prefs.adhanConfig.defaultVoiceId == voice.id
            val isPlaying = playingVoiceId == voice.id
            val dState = downloadStates[voice.id] ?: AdhanDownloadState.Idle
            AdhanVoiceCard(
                voice = voice,
                isAr = isAr,
                isSelected = isSelected,
                isPlaying = isPlaying,
                isBuffering = isPlaying && isBuffering,
                downloadState = dState,
                onSelect = { viewModel.setDefaultAdhanVoice(voice.id) },
                onPreviewToggle = { viewModel.previewOrStopAdhan(voice) },
                onDownload = if (!voice.isSystemDefault) ({ viewModel.downloadAdhanForOffline(voice) }) else null,
                onDelete = if (dState is AdhanDownloadState.VerifiedOffline) ({ viewModel.deleteOfflineAdhanDownload(voice.id) }) else null
            )
        }

        // Per-Prayer Enable/Disable & Voice Assignment
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "تفعيل وتخصيص الأذان لكل صلاة", "Per-Prayer Enable & Voice"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "يمكنك تفعيل أو تعطيل الأذان لكل صلاة على حدة وتخصيص مؤذن مختلف لكل وقت.",
                        "Enable or disable Adhan individually for each prayer and assign a specific voice."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                PrayerName.fardPrayers.forEach { prayer ->
                    val enabled = prefs.isPrayerNotificationEnabled(prayer)
                    val hasOverride = prefs.adhanConfig.hasCustomOverride(prayer)
                    val resolved = AdhanCatalog.resolveVoice(
                        prefs.adhanConfig.voiceIdForPrayer(prayer),
                        prefs.customAdhanRecords,
                        context
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { assigningPrayer = prayer }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
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
                            Text(L10n.pick(lang, "تغيير", "Voice"), color = noorColors.goldAccent)
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.setPrayerNotification(prayer, it) },
                            modifier = Modifier.testTag("adhan_prayer_toggle_${prayer.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Background Reliability & Battery Optimization Notice
        item {
            val exactAlarmOk = NoorAlarmScheduler.hasExactAlarmPermission(context)
            val batteryExemptOk = NoorAlarmScheduler.isIgnoringBatteryOptimizations(context)
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                goldBorder = !exactAlarmOk || !batteryExemptOk
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BatteryAlert, contentDescription = null, tint = noorColors.goldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "موثوقية الأذان في الخلفية والبطارية", "Background Reliability & Battery"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "• المنبهات الدقيقة (عند إطفاء الشاشة): ${if (exactAlarmOk) "مفعّل ✓" else "يرجى التفعيل ⚠️"}\n• استثناء البطارية (عدم إيقاف الخلفية): ${if (batteryExemptOk) "مفعّل ✓" else "مُوصى بالتفعيل ⚠️"}",
                        "• Exact Alarms (Screen Off): ${if (exactAlarmOk) "Active ✓" else "Action Needed ⚠️"}\n• Battery Exemption (Background): ${if (batteryExemptOk) "Active ✓" else "Recommended ⚠️"}"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (exactAlarmOk && batteryExemptOk) SemanticSuccess else noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "لضمان عمل الأذان بصوتك المختار حتى عند إغلاق التطبيق وإطفاء الشاشة وتفعيل عدم الإزعاج، تأكد من تفعيل المنبهات الدقيقة واستثناء «نور» من تحسين البطارية.",
                        "To guarantee Adhan plays with your chosen voice when closed, screen-off, or in Do Not Disturb, allow Exact Alarms and exempt NOOR from battery optimization."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.pick(lang, "إعدادات البطارية", "Battery Settings"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                } else {
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.pick(lang, "المنبهات الدقيقة", "Exact Alarms"), maxLines = 1)
                    }
                }
            }
        }

        // Pin Adhan Settings Shortcut
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
                                text = L10n.pick(lang, "استخدام الصوت الافتراضي (الشيخ ناصر القطامي)", "Use Default Adhan Voice"),
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
    downloadState: AdhanDownloadState,
    onSelect: () -> Unit,
    onPreviewToggle: () -> Unit,
    onDownload: (() -> Unit)?,
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
                    .size(48.dp)
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
                if (downloadState is AdhanDownloadState.VerifiedOffline && !voice.isCustomImported) {
                    Text(
                        text = if (isAr) "محفوظ بدون إنترنت ✓" else "Offline Verified ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = SemanticSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (onDownload != null && downloadState !is AdhanDownloadState.VerifiedOffline) {
                if (downloadState is AdhanDownloadState.Downloading) {
                    CircularProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = GoldPrimary
                    )
                } else {
                    IconButton(onClick = onDownload, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download offline",
                            tint = noorColors.goldAccent
                        )
                    }
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete local Adhan",
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
