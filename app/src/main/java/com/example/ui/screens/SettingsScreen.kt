package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranReciter
import com.example.domain.model.ThemeMode
import com.example.audio.AdhanCatalog
import com.example.notifications.NoorNotificationHelper
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.NoorUserAvatar
import com.example.ui.components.SpiritualRemindersStudioSection
import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.NoorPlatformConstants
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.LocalNoorColors
import com.example.util.AppApkShareHelper
import com.example.viewmodel.NoorViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit,
    onOpenPrayers: () -> Unit,
    onOpenFounderProfile: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminCenter: () -> Unit,
    onOpenAdhanSettings: () -> Unit = {},
    onOpenOfflineAdmin: () -> Unit = {},
    onOpenEcosystemHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalAppLanguage.current
    val isAr = lang == AppLanguage.ARABIC
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val resolvedRole by viewModel.resolvedAdminRole.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val noorColors = LocalNoorColors.current

    var showResetConfirm by remember { mutableStateOf(false) }
    var showEditProfileNameDialog by remember { mutableStateOf(false) }
    var isPreparingApkShare by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for custom profile avatar
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setCustomAvatarUri(uri.toString())
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.sendTestNotification()
    }

    // Zero-permission Storage Access Framework Backup Export
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    // Zero-permission Storage Access Framework Backup Restore
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    viewModel.restoreBackupJson(json)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NoorScreenHeader(
                title = L10n.pick(
                    lang,
                    "الإعدادات (${com.example.data.cloud.NoorPlatformConstants.APP_VERSION_TAG})",
                    "Settings (${com.example.data.cloud.NoorPlatformConstants.APP_VERSION_TAG})"
                ),
                subtitle = L10n.pick(lang, "الحساب، اللغة، القارئ، التنبيهات، الدعم والخصوصية", "Account, language, reciter, alerts, support & privacy"),
                onBack = onBack
            )
        }

        // NOOR 3.0 Signed-In Google Account + Photo + Editable Display Name + Role + Sign Out
        item {
            val effectiveAvatarUrl = prefs.customAvatarUri.ifBlank {
                currentUser?.photoUrl?.toString().orEmpty()
            }
            val effectiveDisplayName = prefs.userName.ifBlank {
                currentUserProfile?.displayName.orEmpty().ifBlank {
                    currentUser?.displayName.orEmpty()
                }
            }.ifBlank {
                L10n.pick(lang, "حساب نور الموثّق", "Verified NOOR Account", "Tasdiqlangan NOOR hisobi")
            }

            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp), goldBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        NoorUserAvatar(
                            photoUrl = effectiveAvatarUrl,
                            displayName = effectiveDisplayName,
                            size = 56.dp,
                            showEditBadge = true,
                            contentDescription = L10n.pick(lang, "تغيير صورة الحساب", "Change Profile Photo", "Profil rasmini o'zgartirish"),
                            onClick = {
                                pickAvatarLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("settings_user_avatar")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val isVerified = currentUserProfile?.verified == true
                            val verifiedLabel = currentUserProfile?.verifiedBadgeLabel.orEmpty()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = effectiveDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isVerified) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = L10n.pick(lang, "حساب موثّق", "Verified Account"),
                                        tint = androidx.compose.ui.graphics.Color(0xFF38BDF8),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .testTag("settings_user_verified_badge")
                                    )
                                }
                                IconButton(
                                    onClick = { showEditProfileNameDialog = true },
                                    modifier = Modifier.testTag("settings_edit_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = L10n.pick(lang, "تغيير الاسم", "Change Name", "Ismni o'zgartirish"),
                                        tint = noorColors.goldAccent
                                    )
                                }
                            }
                            if (isVerified) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "✓ ${verifiedLabel.ifBlank { "حساب موثّق رسمياً من إدارة نور" }}",
                                        "✓ ${verifiedLabel.ifBlank { "Officially Verified NOOR Account" }}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF38BDF8)
                                )
                            }
                            Text(
                                text = "${currentUser?.email ?: ""} · ${L10n.pick(lang, resolvedRole.labelAr, resolvedRole.labelEn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "اضغط على الصورة لتغييرها أو القلم لتعديل اسمك",
                                    "Tap photo to update avatar or pencil to edit name",
                                    "Rasmni o'zgartirish uchun ustiga, ism uchun qalamga bosing"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = noorColors.goldAccent
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.signOut(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.pick(lang, "خروج", "Sign Out"), style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (resolvedRole.isPrivileged) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAdminCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_open_admin_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.pick(lang, "فتح مركز تحكم المالك (Super Admin)", "Open Super Admin Control Center"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // NOOR 3.0 Quick Links: Founder Profile (Osama), Contact & Support, Notification Center
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = L10n.pick(lang, "نور — المؤسس، الدعم والإعلانات", "NOOR — Founder, Support & Announcements"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenFounderProfile,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("settings_founder_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Person, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.pick(lang, "عن المؤسس — أسامة (Osama)", "About the Founder — Osama"))
                    }
                    OutlinedButton(
                        onClick = onOpenSupport,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("settings_support_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.SupportAgent, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.pick(lang, "مركز الدعم والتواصل الرسمي", "Official Contact & Support Center"))
                    }
                    OutlinedButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("settings_notifications_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Campaign, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.pick(lang, "مركز الإشعارات والإعلانات العامة", "Notification & Announcements Center"))
                    }
                }
            }
        }

        // 0. Account & Personal Greeting Name & Daily Goals + Preferred Reciter
        item {
            val initialName = prefs.userName.ifBlank {
                currentUserProfile?.displayName.orEmpty().ifBlank { currentUser?.displayName.orEmpty() }
            }
            var nameDraft by remember(initialName) { mutableStateOf(initialName) }
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp), goldBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = noorColors.goldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "اسم الحساب والترحيب", "Account & Display Name", "Hisob va ko'rinadigan ism"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(L10n.pick(lang, "أدخل اسمك...", "Enter your name...")) }
                    )
                    Button(onClick = { viewModel.setUserName(nameDraft) }) {
                        Text(L10n.pick(lang, "حفظ", "Save"))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = L10n.pick(lang, "القارئ المفضل للقرآن الكريم", "Preferred Quran Reciter"),
                    style = MaterialTheme.typography.labelLarge,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuranReciter.entries) { rec ->
                        LiquidGlassChip(
                            label = L10n.pick(lang, rec.nameAr, rec.nameEn),
                            selected = prefs.quranReciter == rec,
                            onClick = { viewModel.setQuranReciter(rec) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = L10n.pick(lang, "جودة البث الصوتي", "Audio Streaming Quality"),
                    style = MaterialTheme.typography.labelLarge,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("64 kbps", "128 kbps", "192 kbps").forEach { q ->
                        LiquidGlassChip(
                            label = q,
                            selected = prefs.audioQualityLabel == q,
                            onClick = { viewModel.setAudioQualityLabel(q) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = L10n.pick(lang, "هدف وِرد القرآن اليومي (آيات)", "Daily Quran Goal (Ayahs)"),
                    style = MaterialTheme.typography.labelLarge,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 30, 50).forEach { goal ->
                        LiquidGlassChip(
                            label = "$goal",
                            selected = prefs.dailyQuranGoalVerses == goal,
                            onClick = { viewModel.setDailyGoals(goal, prefs.dailyTasbihGoal) }
                        )
                    }
                }
            }
        }

        // 1. Language & Theme
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "اللغة (العربية / English / O'zbekcha / Русский)",
                        "Language (Arabic / English / Uzbek / Russian)",
                        "Til (O'zbekcha / العربية / English / Русский)",
                        "Язык (Русский / العربية / English / O'zbekcha)"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        LiquidGlassChip(
                            label = "العربية",
                            selected = prefs.language == AppLanguage.ARABIC,
                            onClick = { viewModel.setLanguage(AppLanguage.ARABIC) },
                            modifier = Modifier.weight(1f).testTag("lang_arabic_btn")
                        )
                        LiquidGlassChip(
                            label = "English",
                            selected = prefs.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier.weight(1f).testTag("lang_english_btn")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        LiquidGlassChip(
                            label = "O'zbekcha",
                            selected = prefs.language == AppLanguage.UZBEK,
                            onClick = { viewModel.setLanguage(AppLanguage.UZBEK) },
                            modifier = Modifier.weight(1f).testTag("lang_uzbek_btn")
                        )
                        LiquidGlassChip(
                            label = "Русский",
                            selected = prefs.language == AppLanguage.RUSSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.RUSSIAN) },
                            modifier = Modifier.weight(1f).testTag("lang_russian_btn")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = L10n.pick(lang, "المظهر", "Appearance"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "داكن", "Dark"),
                        selected = prefs.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "فاتح", "Light"),
                        selected = prefs.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "النظام", "System"),
                        selected = prefs.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = L10n.pick(lang, "تأثيرات الخلفية والنجوم", "Background Stars & Effects"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = L10n.pick(lang, "إظهار النجوم المتلألئة بالخلفية", "Background Floating Stars"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "يمكنك إطفاء النجوم والذرات الذهبية لخلفية هادئة وتوفير البطارية",
                                "Turn off floating starlight motes for a calm background & battery savings"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.backgroundStarsEnabled,
                        onCheckedChange = { viewModel.setBackgroundStarsEnabled(it) },
                        modifier = Modifier.testTag("settings_toggle_background_stars")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = L10n.pick(lang, "تأثير الشفق المتحرّك بالخلفية", "Animated Background Aurora"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "توهّج الأضواء المتدرجة الهادئة خلف البطاقات الزجاجية",
                                "Soft moving aurora orbs behind Liquid Glass cards"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.backgroundAuroraEnabled,
                        onCheckedChange = { viewModel.setBackgroundAuroraEnabled(it) },
                        modifier = Modifier.testTag("settings_toggle_background_aurora")
                    )
                }
            }
        }

        // 2. Prayer Settings Quick Card
        item {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenPrayers
            ) {
                Text(
                    text = L10n.pick(lang, "إعدادات مواقيت الصلاة والموقع", "Prayer Times & Location"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isAr) "${prefs.cityNameAr} · ${prefs.calculationMethod.nameAr} · ${prefs.asrMadhhab.nameAr}"
                    else "${prefs.cityNameEn} · ${prefs.calculationMethod.nameEn} · ${prefs.asrMadhhab.nameEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2b. Advanced Adhan Sound Customization Card
        item {
            val activeAdhanVoice = remember(prefs.adhanConfig.defaultVoiceId, prefs.customAdhanRecords) {
                AdhanCatalog.resolveVoice(prefs.adhanConfig.defaultVoiceId, prefs.customAdhanRecords)
            }
            LiquidGlassCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("settings_adhan_sound_card"),
                goldBorder = true,
                onClick = onOpenAdhanSettings
            ) {
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
                                text = L10n.pick(lang, "إعدادات أصوات الأذان والمؤذن", "Adhan Voice & Sound Settings"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, activeAdhanVoice.nameAr, activeAdhanVoice.nameEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = onOpenAdhanSettings) {
                        Text(L10n.pick(lang, "تخصيص", "Customize"), color = noorColors.goldAccent)
                    }
                }
            }
        }

        // 2c. Offline Local Super Admin & Ecosystem Hub Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("settings_offline_admin_card"),
                goldBorder = true,
                onClick = onOpenOfflineAdmin
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = noorColors.goldAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = L10n.pick(lang, "الإدارة العامة المحلية (بدون إنترنت)", "Offline Local Super Admin"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, "إدارة المعرفة المحلية، الحلقات، الصلاحيات والنسخ الاحتياطي JSON", "Local RBAC, AI KB Import, Halaqat & JSON Backup"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = onOpenEcosystemHub) {
                        Text(L10n.pick(lang, "المنظومة", "Hub"), color = noorColors.goldAccent)
                    }
                }
            }
        }

        // 3. Notifications & Reminders
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "الإشعارات والتنبيهات", "Notifications & Reminders"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsToggleRow(
                    title = L10n.pick(lang, "مواقيت الصلاة في شريط الإشعارات (دائم)", "Persistent Prayer Times Bar"),
                    checked = prefs.persistentPrayerNotificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NoorNotificationHelper.hasNotificationPermission(context)
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setPersistentPrayerNotificationEnabled(enabled)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L10n.pick(lang, "التنبيه قبل الصلاة بـ", "Pre-Prayer Alert"),
                    style = MaterialTheme.typography.labelLarge,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 5, 10, 15).forEach { mins ->
                        LiquidGlassChip(
                            label = if (mins == 0) L10n.pick(lang, "إيقاف", "Off") else "$mins ${L10n.pick(lang, "د", "m")}",
                            selected = prefs.prePrayerAlertMinutes == mins,
                            onClick = { viewModel.setPrePrayerAlertMinutes(mins) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrayerName.fardPrayers.forEach { prayer ->
                    SettingsToggleRow(
                        title = L10n.pick(lang, "تنبيه صلاة ${L10n.prayerName(prayer, lang)}", "${L10n.prayerName(prayer, lang)} Prayer Alert"),
                        checked = prefs.isPrayerNotificationEnabled(prayer),
                        onCheckedChange = { viewModel.setPrayerNotification(prayer, it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = L10n.pick(lang, "تذكير ورد القرآن اليومي (٩:٠٠ م)", "Daily Quran Reminder (9:00 PM)"),
                    checked = prefs.quranReminderEnabled,
                    onCheckedChange = { viewModel.setReminder("quran", it) }
                )
                SettingsToggleRow(
                    title = L10n.pick(lang, "تذكير التسبيح (٣:٠٠ م)", "Tasbih Reminder (3:00 PM)"),
                    checked = prefs.tasbihReminderEnabled,
                    onCheckedChange = { viewModel.setReminder("tasbih", it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NoorNotificationHelper.hasNotificationPermission(context)
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.sendTestNotification()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_notification_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.pick(lang, "إرسال تنبيه تجريبي", "Send Test Notification"))
                }
            }
        }

        // 3B. Dedicated Morning, Evening & Tahajjud / Qiyam Al-Layl Custom Sound Reminders Studio
        item {
            SpiritualRemindersStudioSection(
                prefs = prefs,
                viewModel = viewModel,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // 4. Sound & Vibration
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = L10n.pick(lang, "الصوت والاهتزاز", "Sound & Haptics"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingsToggleRow(
                    title = L10n.pick(lang, "اهتزاز المسبحة والأذكار", "Tasbih & Adhkar Vibration"),
                    checked = prefs.tasbihVibrationEnabled,
                    onCheckedChange = { viewModel.setTasbihVibration(it) }
                )
                SettingsToggleRow(
                    title = L10n.pick(lang, "صوت نقر المسبحة", "Tasbih Click Sound"),
                    checked = prefs.tasbihSoundEnabled,
                    onCheckedChange = { viewModel.setTasbihSound(it) }
                )
            }
        }

        // 4b. Android Home Screen Shortcuts & Widgets Quick Pin
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PushPin, contentDescription = null, tint = noorColors.goldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "اختصارات الشاشة الرئيسية والويدجت", "Home Screen Shortcuts & Widgets"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "اضغط مطولاً على أيقونة نور أو ثبّت الاختصارات والويدجت (مواقيت الصلاة / مشغل القرآن) على شاشة هاتفك.",
                        "Long-press the NOOR app icon or pin quick shortcuts & widgets (Prayer Times / Quran Player) to your Home Screen."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                val quickPins = listOf(
                    NoorShortcutManager.TARGET_RESUME_READING to L10n.pick(lang, "متابعة القراءة", "Resume Reading"),
                    NoorShortcutManager.TARGET_QURAN to L10n.pick(lang, "القرآن الكريم", "Open Quran"),
                    NoorShortcutManager.TARGET_START_LISTENING to L10n.pick(lang, "الاستماع للقرآن", "Listen Quran"),
                    NoorShortcutManager.TARGET_PRAYER_TIMES to L10n.pick(lang, "مواقيت الصلاة", "Prayer Times"),
                    NoorShortcutManager.TARGET_QIBLA to L10n.pick(lang, "القبلة", "Qibla"),
                    NoorShortcutManager.TARGET_ADHAN_SETTINGS to L10n.pick(lang, "الأذان", "Adhan")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPins) { (route, label) ->
                        LiquidGlassChip(
                            label = "+ $label",
                            selected = false,
                            onClick = {
                                val pinned = NoorShortcutManager.requestPinShortcut(context, route, isAr)
                                if (!pinned) {
                                    viewModel.showBannerMessage(
                                        L10n.pick(
                                            lang,
                                            "الاختصارات جاهزة — اضغط مطولاً على أيقونة نور في الشاشة الرئيسية",
                                            "Shortcuts are active — long-press the NOOR icon on your Home Screen"
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // 5. Privacy, Backup, Restore & Reset
        item {
            LiquidGlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = noorColors.goldAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = L10n.pick(lang, "الخصوصية والنسخ الاحتياطي", "Privacy & Local Backup"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "تُحفظ جميع بياناتك وإحصائياتك محلياً على جهازك فقط. لا يتم إرسال أي بيانات شخصية أو موقعك إلى أي خادم خارجي.",
                        "All your records and statistics stay 100% local on your device. No personal data or location is sent to external servers."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { exportBackupLauncher.launch("noor_backup.json") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Backup, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "نسخ احتياطي", "Backup"))
                    }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "استعادة", "Restore"))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteForever, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.pick(lang, "إعادة ضبط التطبيق", "Reset Application"))
                }
            }
        }

        // 6. Share App as APK + Official Telegram Channel (very last item in Settings)
        item {
            val officialTelegramUrl = remember(globalConfig.updateChannelUrl) {
                AppVersionGate.resolveSafeUpdateUrl(globalConfig.updateChannelUrl)
            }
            LiquidGlassCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("settings_share_app_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = noorColors.goldAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = L10n.pick(
                            lang,
                            "مشاركة التطبيق",
                            "Share App",
                            "Ilovani ulashish",
                            "Поделиться приложением"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "أرسل تطبيق نور مباشرةً بصيغة ملف APK قابل للتثبيت مع رابط قناة التليجرام الرسمية للتحديثات.",
                        "Send NOOR directly as an installable APK file along with our official Telegram updates channel.",
                        "NOOR ilovasini o'rnatiladigan APK fayl sifatida rasmiy Telegram kanal havolasi bilan ulashing.",
                        "Отправьте NOOR напрямую в виде APK-файла вместе со ссылкой на официальный Telegram-канал."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = officialTelegramUrl,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (!isPreparingApkShare) {
                            isPreparingApkShare = true
                            scope.launch {
                                val result = AppApkShareHelper.prepareAndShareApk(
                                    context = context,
                                    lang = lang,
                                    versionTag = NoorPlatformConstants.APP_VERSION_TAG,
                                    rawTelegramUrl = officialTelegramUrl
                                )
                                isPreparingApkShare = false
                                result.onFailure {
                                    viewModel.showBannerMessage(
                                        L10n.pick(
                                            lang,
                                            "تعذّر تجهيز ملف APK للمشاركة. حاول مرة أخرى.",
                                            "Unable to prepare the APK file for sharing. Please try again.",
                                            "APK faylini ulashish uchun tayyorlab bo'lmadi. Qayta urinib ko'ring.",
                                            "Не удалось подготовить APK для отправки. Попробуйте ещё раз."
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isPreparingApkShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_share_app_apk_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isPreparingApkShare) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "جاري تجهيز ملف APK...",
                                "Preparing APK...",
                                "APK tayyorlanmoqda...",
                                "Подготовка APK..."
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "مشاركة التطبيق (APK)",
                                "Share App (APK)",
                                "Ilovani ulashish (APK)",
                                "Поделиться (APK)"
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(L10n.pick(lang, "تأكيد إعادة الضبط", "Confirm Full Reset")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "سيتم مسح جميع الجلسات والعلامات المرجعية والإعدادات والعودة إلى الإعدادات الافتراضية.",
                        "This will clear all local sessions, bookmarks, and preferences and restore defaults."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetApplication()
                    showResetConfirm = false
                }) {
                    Text(L10n.pick(lang, "إعادة ضبط", "Reset"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    if (showEditProfileNameDialog) {
        val currentName = prefs.userName.ifBlank {
            currentUserProfile?.displayName.orEmpty().ifBlank { currentUser?.displayName.orEmpty() }
        }
        var editedName by remember(showEditProfileNameDialog) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showEditProfileNameDialog = false },
            title = {
                Text(
                    L10n.pick(lang, "تغيير اسم الحساب", "Change Display Name", "Hisob ismini o'zgartirish"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        label = { Text(L10n.pick(lang, "الاسم الجديد", "New Name", "Yangi ism")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_edit_name_input")
                    )
                    if (prefs.customAvatarUri.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.setCustomAvatarUri("")
                                showEditProfileNameDialog = false
                            }
                        ) {
                            Text(
                                L10n.pick(
                                    lang,
                                    "استعادة صورة حساب Google الأصلية",
                                    "Restore Original Google Photo",
                                    "Asl Google profil rasmini tiklash"
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(editedName)
                        showEditProfileNameDialog = false
                    },
                    enabled = editedName.trim().isNotEmpty(),
                    modifier = Modifier.testTag("settings_edit_name_save_btn")
                ) {
                    Text(L10n.pick(lang, "حفظ", "Save", "Saqlash"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileNameDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
