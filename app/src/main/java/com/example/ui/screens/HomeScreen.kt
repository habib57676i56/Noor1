package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.catalog.AudioLibraryCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.calendar.HijriCalendarEngine
import com.example.domain.model.HomeDashboardConfig
import com.example.domain.model.PrayerName
import com.example.ui.components.IslamicRosetteOrnament
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.prayerIcon
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: NoorViewModel,
    onOpenPrayers: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenMorningAdhkar: () -> Unit,
    onOpenEveningAdhkar: () -> Unit,
    onOpenAdhkarCategory: (String) -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenQuran: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenAudioLibrary: () -> Unit,
    onOpenMosques: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFounderProfile: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminCenter: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboard by viewModel.prayerDashboard.collectAsStateWithLifecycle()
    val todayActivity by viewModel.todayActivity.collectAsStateWithLifecycle()
    val recentAudio by viewModel.recentAudioTracks.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val announcements by viewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val resolvedRole by viewModel.resolvedAdminRole.collectAsStateWithLifecycle()
    val morningItems by viewModel.morningAdhkarItems.collectAsStateWithLifecycle()
    val eveningItems by viewModel.eveningAdhkarItems.collectAsStateWithLifecycle()
    val adhkarProgress by viewModel.adhkarProgressMap.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.trackSectionVisit("home")
    }

    var showCustomizeDialog by remember { mutableStateOf(false) }

    val upcomingEvent = remember(prefs.hijriAdjustmentDays) {
        HijriCalendarEngine.getUpcomingOccasions(prefs.hijriAdjustmentDays).firstOrNull()
    }
    val currentHour = dashboard.gregorianCal.get(Calendar.HOUR_OF_DAY)
    val isMorningWindow = currentHour in 4..14
    val lastReadSurah = remember(prefs.lastReadSurah) { SurahCatalog.findSurah(prefs.lastReadSurah) }
    val homeCfg = prefs.homeConfig

    val morningCompleted = morningItems.count { adhkarProgress[it.id]?.isCompleted == true }
    val eveningCompleted = eveningItems.count { adhkarProgress[it.id]?.isCompleted == true }
    val liveBrandName = L10n.pick(lang, globalConfig.appNameAr.ifBlank { "نور" }, globalConfig.appNameEn.ifBlank { "NOOR" })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Personalized Greeting Header + Notifications, Customize & Settings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDeep)))
                        .border(1.2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Mosque, contentDescription = null, tint = GoldBright, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val greetingName = prefs.userName.ifBlank { L10n.pick(lang, "مرحباً بك في $liveBrandName", "Welcome to $liveBrandName") }
                    Text(
                        text = L10n.pick(lang, "السلام عليكم", "Assalamu Alaikum"),
                        style = MaterialTheme.typography.labelMedium,
                        color = noorColors.goldAccent
                    )
                    Text(
                        text = greetingName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("home_notifications_button")
                ) {
                    Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = noorColors.goldAccent)
                }
                IconButton(
                    onClick = { showCustomizeDialog = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, contentDescription = "Customize Home", tint = noorColors.goldAccent)
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("home_settings_button")
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        // 1B. Role-Gated Super Admin Control Center Entry (ONLY visible to verified Owner / privileged Admin roles)
        if (resolvedRole.isPrivileged) {
            item {
                LiquidGlassCard(
                    goldBorder = true,
                    onClick = onOpenAdminCenter,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.testTag("home_super_admin_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = GoldBright)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "مركز تحكم المالك — NOOR 3.0", "Super Admin Control Center"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, "الصلاحية: ${resolvedRole.labelAr} · الإحصائيات، المستخدمون، المحتوى والأمان", "Role: ${resolvedRole.labelEn} · Analytics, Users, CMS & Security"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 1C. Live Owner Announcement Banner (if any active announcement exists)
        val latestAnnouncement = announcements.firstOrNull()
        if (latestAnnouncement != null) {
            item {
                LiquidGlassCard(
                    goldBorder = latestAnnouncement.kind == "EMERGENCY",
                    onClick = onOpenNotifications,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, latestAnnouncement.titleAr, latestAnnouncement.titleEn.ifBlank { latestAnnouncement.titleAr }),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = L10n.pick(lang, latestAnnouncement.bodyAr, latestAnnouncement.bodyEn.ifBlank { latestAnnouncement.bodyAr }),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Date & City Strip
        item {
            LiquidGlassCard(
                onClick = onOpenCalendar,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = HijriCalendarEngine.formatHijri(dashboard.hijriToday, lang == com.example.domain.model.AppLanguage.ARABIC),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Text(
                            text = HijriCalendarEngine.formatGregorian(dashboard.gregorianCal, lang == com.example.domain.model.AppLanguage.ARABIC),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldPrimary.copy(alpha = 0.16f))
                            .clickable(onClick = onOpenPrayers)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = L10n.pick(lang, prefs.cityNameAr, prefs.cityNameEn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Hero Mosque Next Prayer Countdown Card
        item {
            val nextInfo = dashboard.nextPrayerInfo
            val iqamahDelay = if (prefs.iqamahDelays.enabled) prefs.iqamahDelays.delayFor(nextInfo.nextPrayer) else 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(198.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.2.dp,
                        Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.7f), GoldPrimary.copy(alpha = 0.25f))),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(onClick = onOpenPrayers)
                    .testTag("hero_next_prayer_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_noor_hero_bg_1795289370813),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xE6041711), Color(0xB807231A), Color(0x6604130E))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(lang, "الصلاة القادمة", "Next Prayer"),
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldBright,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = L10n.formatTime12h(nextInfo.nextPrayerMillis, prefs.timezoneId, lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = L10n.prayerName(nextInfo.nextPrayer, lang),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = L10n.formatCountdown(nextInfo.remainingMillis, lang),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                        if (iqamahDelay > 0) {
                            val iqTime = L10n.formatTime12h(nextInfo.nextPrayerMillis + iqamahDelay * 60_000L, prefs.timezoneId, lang)
                            Text(
                                text = L10n.pick(lang, "الإقامة: $iqTime (+$iqamahDelay د)", "Iqamah: $iqTime (+${iqamahDelay}m)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9AE6B4)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { nextInfo.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = GoldBright,
                        trackColor = Color.White.copy(alpha = 0.22f)
                    )
                }
            }
        }

        // 4. Daily 6-Prayer Horizontal Strip
        if (homeCfg.showPrayerStrip) {
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "مواقيت الصلاة اليوم", "Today's Prayer Times"),
                    actionLabel = L10n.pick(lang, "عرض الكل", "See All"),
                    onActionClick = onOpenPrayers
                )
                Spacer(modifier = Modifier.height(8.dp))
                LiquidGlassCard(contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dashboard.todaySchedule.allOrdered().forEach { (prayer, timeMs) ->
                            val isNext = prayer == dashboard.nextPrayerInfo.nextPrayer
                            val isLogged = when (prayer) {
                                PrayerName.FAJR -> todayActivity?.fajrPrayed == true
                                PrayerName.DHUHR -> todayActivity?.dhuhrPrayed == true
                                PrayerName.ASR -> todayActivity?.asrPrayed == true
                                PrayerName.MAGHRIB -> todayActivity?.maghribPrayed == true
                                PrayerName.ISHA -> todayActivity?.ishaPrayed == true
                                PrayerName.SUNRISE -> false
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isNext) EmeraldPrimary.copy(alpha = 0.26f) else Color.Transparent)
                                    .clickable { if (prayer.isFard) viewModel.togglePrayerLogged(prayer) }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isLogged) Icons.Rounded.CheckCircle else prayerIcon(prayer),
                                    contentDescription = null,
                                    tint = when {
                                        isLogged -> Color(0xFF4ADE80)
                                        isNext -> GoldBright
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.prayerName(prayer, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = L10n.formatTime12h(timeMs, prefs.timezoneId, lang)
                                        .replace(" AM", "").replace(" PM", "").replace(" ص", "").replace(" م", ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isNext) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Ayah of the Day (Framed by Ornamental Gold Rosettes)
        if (homeCfg.showDailyAyah) {
            item {
                val ayah = dashboard.dailyAyah
                LiquidGlassCard(
                    goldBorder = true,
                    onClick = { onOpenSurah(ayah.surahNumber) },
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IslamicRosetteOrnament(size = 28.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = L10n.pick(lang, "آية اليوم", "Ayah of the Day"),
                                style = MaterialTheme.typography.labelMedium,
                                color = noorColors.goldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ayah.textAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 21.sp,
                                lineHeight = 36.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L10n.pick(lang, ayah.referenceAr, "\"${ayah.meaningEn}\" — ${ayah.referenceEn}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        IslamicRosetteOrnament(size = 28.dp)
                    }
                }
            }
        }

        // 6. Quick Access Grid (Respects Live Owner Feature Flags from GlobalAppConfig)
        item {
            val quickTiles = buildList {
                if (globalConfig.featureQuranEnabled) add(Triple(L10n.pick(lang, "القرآن", "Quran"), Icons.Rounded.MenuBook, onOpenQuran))
                add(Triple(L10n.pick(lang, "الأذكار", "Adhkar"), Icons.Rounded.AutoAwesome, onOpenAdhkar))
                add(Triple(L10n.pick(lang, "المسبحة", "Tasbeeh"), Icons.Rounded.TouchApp, onOpenTasbih))
                if (globalConfig.featureQiblaEnabled) add(Triple(L10n.pick(lang, "القبلة", "Qibla"), Icons.Rounded.Explore, onOpenQibla))
                if (globalConfig.featureAudioEnabled) add(Triple(L10n.pick(lang, "ياسر الدوسري", "Audio"), Icons.Rounded.Headphones, onOpenAudioLibrary))
                if (globalConfig.featureMosquesEnabled) add(Triple(L10n.pick(lang, "المساجد", "Mosques"), Icons.Rounded.Mosque, onOpenMosques))
                if (globalConfig.featureCalendarEnabled) add(Triple(L10n.pick(lang, "التقويم", "Calendar"), Icons.Rounded.CalendarMonth, onOpenCalendar))
                add(Triple(L10n.pick(lang, "عباداتي", "Worship"), Icons.Rounded.Insights, onOpenStats))
            }
            NoorSectionHeader(title = L10n.pick(lang, "الوصول السريع", "Quick Access"))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quickTiles.chunked(4).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        rowTiles.forEach { (label, icon, action) ->
                            HomeQuickTile(label, icon, action, Modifier.weight(1f))
                        }
                        repeat(4 - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 7. Dedicated Authentic Morning & Evening Adhkar Cards (with real completion progress)
        if (homeCfg.showAdhkarShortcut) {
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "أذكار الصباح والمساء الموثقة", "Authentic Morning & Evening Adhkar"),
                    actionLabel = L10n.pick(lang, "المكتبة", "All"),
                    onActionClick = onOpenAdhkar
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LiquidGlassCard(
                        goldBorder = isMorningWindow,
                        onClick = onOpenMorningAdhkar,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_morning_adhkar_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = GoldBright, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "أذكار الصباح", "Morning"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$morningCompleted / ${morningItems.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = noorColors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (morningItems.isEmpty()) 0f else (morningCompleted.toFloat() / morningItems.size).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
                            color = GoldBright,
                            trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                        )
                    }

                    LiquidGlassCard(
                        goldBorder = !isMorningWindow,
                        onClick = onOpenEveningAdhkar,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_evening_adhkar_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "أذكار المساء", "Evening"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$eveningCompleted / ${eveningItems.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = noorColors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (eveningItems.isEmpty()) 0f else (eveningCompleted.toFloat() / eveningItems.size).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
                            color = noorColors.goldAccent,
                            trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // 8. Authentic Hadith of the Day
        if (homeCfg.showDailyHadith) {
            item {
                val hadith = dashboard.dailyHadith
                LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = L10n.pick(lang, "حديث نبوي صحيح", "Authentic Hadith of the Day"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, hadith.textAr, hadith.textEn),
                        fontFamily = if (lang == com.example.domain.model.AppLanguage.ARABIC) AmiriFontFamily else MaterialTheme.typography.bodyLarge.fontFamily,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = L10n.pick(
                            lang,
                            "${hadith.narratorAr} · ${hadith.sourceAr} (${hadith.gradingAr})",
                            "${hadith.narratorEn} · ${hadith.sourceEn} (${hadith.gradingEn})"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 9. Quran Reading Progress Card
        if (homeCfg.showQuranProgress) {
            item {
                val readToday = todayActivity?.quranVersesRead ?: 0
                val goal = prefs.dailyQuranGoalVerses.coerceAtLeast(1)
                LiquidGlassCard(
                    onClick = { onOpenSurah(lastReadSurah.number) },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "وِرد القرآن اليومي", "Daily Quran Reading"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "آخر قراءة: سورة ${lastReadSurah.nameAr} (${prefs.lastReadAyah}) · $readToday / $goal آية",
                                    "Last read: ${lastReadSurah.nameEn} (${prefs.lastReadAyah}) · $readToday / $goal Ayahs"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${((readToday * 100) / goal).coerceAtMost(100)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (readToday.toFloat() / goal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = noorColors.goldAccent,
                        trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // 10. Recently Played / Yasser Al-Dosari Featured Audio
        if (homeCfg.showRecentAudio) {
            item {
                val latest = recentAudio.firstOrNull()
                LiquidGlassCard(
                    goldBorder = true,
                    onClick = onOpenAudioLibrary,
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
                                .background(EmeraldPrimary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = noorColors.goldAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (latest != null) L10n.pick(lang, latest.titleAr, latest.titleEn)
                                else L10n.pick(lang, "ياسر الدوسري — تلاوات القرآن الكريم", "Yasser Al-Dosari — Quran Recitations"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (latest != null) L10n.pick(lang, latest.reciterNameAr, latest.reciterNameEn)
                                else L10n.pick(lang, "استمع للمصحف المرتل الكامل بجودة عالية", "Stream complete 114-Surah Murattal collection"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                if (latest != null) {
                                    viewModel.playRecentAudioEntity(latest)
                                } else {
                                    val tracks = AudioLibraryCatalog.yasserAlDosariTracks()
                                    viewModel.playAudioTrack(tracks.first(), tracks)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = EmeraldDeep)
                        }
                    }
                }
            }
        }

        // 11. Upcoming Islamic Event
        if (homeCfg.showUpcomingEvent && upcomingEvent != null) {
            item {
                LiquidGlassCard(
                    onClick = onOpenCalendar,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "المناسبة الإسلامية القادمة", "Upcoming Islamic Event"),
                                style = MaterialTheme.typography.labelSmall,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, upcomingEvent.nameAr, upcomingEvent.nameEn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = L10n.pick(lang, "بعد ${upcomingEvent.daysUntil} يوم", "In ${upcomingEvent.daysUntil}d"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                    }
                }
            }
        }

        // 12. Founder Profile (Osama) & Official Support Center Shortcuts
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LiquidGlassCard(
                    onClick = onOpenFounderProfile,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.weight(1f).testTag("home_founder_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(L10n.pick(lang, "عن المؤسس", "About Founder"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(L10n.pick(lang, "أسامة — مؤسس نور", "Osama · Creator"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                LiquidGlassCard(
                    onClick = onOpenSupport,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.weight(1f).testTag("home_support_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(L10n.pick(lang, "الدعم والتواصل", "Contact & Help"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(L10n.pick(lang, "تذاكر، واتساب وتلغرام", "Email, WA, Telegram"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeHomeDialog(
            config = homeCfg,
            onSave = { updated ->
                viewModel.setHomeDashboardConfig(updated)
                showCustomizeDialog = false
            },
            onDismiss = { showCustomizeDialog = false }
        )
    }
}

@Composable
private fun HomeQuickTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = noorColors.goldAccent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomizeHomeDialog(
    config: HomeDashboardConfig,
    onSave: (HomeDashboardConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var draft by remember(config) { mutableStateOf(config) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.pick(lang, "تخصيص بطاقات الرئيسية", "Customize Home Cards")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleRow(L10n.pick(lang, "شريط الصلوات اليومي", "Daily Prayer Strip"), draft.showPrayerStrip) {
                    draft = draft.copy(showPrayerStrip = it)
                }
                ToggleRow(L10n.pick(lang, "آية اليوم", "Ayah of the Day"), draft.showDailyAyah) {
                    draft = draft.copy(showDailyAyah = it)
                }
                ToggleRow(L10n.pick(lang, "الحديث النبوي اليومي", "Hadith of the Day"), draft.showDailyHadith) {
                    draft = draft.copy(showDailyHadith = it)
                }
                ToggleRow(L10n.pick(lang, "أذكار الصباح والمساء", "Morning/Evening Adhkar"), draft.showAdhkarShortcut) {
                    draft = draft.copy(showAdhkarShortcut = it)
                }
                ToggleRow(L10n.pick(lang, "وِرد القرآن اليومي", "Quran Reading Progress"), draft.showQuranProgress) {
                    draft = draft.copy(showQuranProgress = it)
                }
                ToggleRow(L10n.pick(lang, "المكتبة الصوتية الأخيرة", "Recent Audio Strip"), draft.showRecentAudio) {
                    draft = draft.copy(showRecentAudio = it)
                }
                ToggleRow(L10n.pick(lang, "المناسبة الإسلامية القادمة", "Upcoming Event"), draft.showUpcomingEvent) {
                    draft = draft.copy(showUpcomingEvent = it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(L10n.pick(lang, "حفظ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel"))
            }
        }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
