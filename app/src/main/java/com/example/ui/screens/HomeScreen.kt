package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.TravelExplore
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.data.cloud.AdPlacement
import com.example.data.local.catalog.AudioLibraryCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.calendar.HijriCalendarEngine
import com.example.domain.model.HomeDashboardConfig
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeGate
import com.example.ui.components.HeroPrayerGlassCard
import com.example.ui.components.IslamicRosetteOrnament
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorAdCardBannerSection
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.NoorUserAvatar
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
    onOpenAdminCenter: () -> Unit,
    onOpenEcosystemHub: () -> Unit = {},
    onOpenHalaqat: () -> Unit = {},
    onOpenHifzAi: () -> Unit = {},
    onOpenLocalAi: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenKhatm: () -> Unit = {},
    onOpenVideos: () -> Unit = {},
    onOpenOfflineAdmin: () -> Unit = {},
    onOpenZakat: () -> Unit = {},
    onOpenAsmaUlHusna: () -> Unit = {},
    onOpenWorshipGuides: () -> Unit = {},
    onOpenQadaTracker: () -> Unit = {},
    onOpenMirath: () -> Unit = {},
    onOpenCardDesigner: () -> Unit = {},
    onOpenHaramainLive: () -> Unit = {},
    onOpenGlobalQuranSearch: () -> Unit = {},
    onOpenLiveHadith: () -> Unit = {},
    onOpenQuranIrabSarf: () -> Unit = {},
    onOpenCrescentObservatory: () -> Unit = {},
    onOpenGlobalUmmahPulse: () -> Unit = {},
    onOpenMutashabihat: () -> Unit = {},
    onOpenSeerahAtlas: () -> Unit = {},
    onOpenQuranUniverse: () -> Unit = {},
    onOpenCosmicGlobe: () -> Unit = {},
    onOpenSoulRemedy: () -> Unit = {},
    onOpenMiraclesProphets: () -> Unit = {},
    onOpenCalligraphyStudio: () -> Unit = {},
    onOpenSanctuaryAudio: () -> Unit = {},
    onOpenAstrolabe: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboard by viewModel.prayerDashboard.collectAsStateWithLifecycle()
    val todayActivity by viewModel.todayActivity.collectAsStateWithLifecycle()
    val recentAudio by viewModel.recentAudioTracks.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val announcements by viewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val activeAdCards by viewModel.activeAdCards.collectAsStateWithLifecycle()
    val unreadMessagesCount by viewModel.unreadAdminMessagesCount.collectAsStateWithLifecycle()
    val resolvedRole by viewModel.resolvedAdminRole.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val morningItems by viewModel.morningAdhkarItems.collectAsStateWithLifecycle()
    val eveningItems by viewModel.eveningAdhkarItems.collectAsStateWithLifecycle()
    val adhkarProgress by viewModel.adhkarProgressMap.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.trackSectionVisit("home")
    }

    var showCustomizeDialog by remember { mutableStateOf(false) }
    var ecosystemExpanded by rememberSaveable { mutableStateOf(false) }

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
                val effectiveAvatarUrl = prefs.customAvatarUri.ifBlank {
                    currentUser?.photoUrl?.toString().orEmpty()
                }
                val effectiveUserName = prefs.userName.ifBlank {
                    currentUserProfile?.displayName.orEmpty().ifBlank {
                        currentUser?.displayName.orEmpty()
                    }
                }
                NoorUserAvatar(
                    photoUrl = effectiveAvatarUrl,
                    displayName = effectiveUserName,
                    size = 48.dp,
                    contentDescription = L10n.pick(lang, "صورة الحساب", "Account Profile Photo", "Profil rasmi"),
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_user_avatar")
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSettings() }
                ) {
                    val greetingName = effectiveUserName.ifBlank {
                        L10n.pick(lang, "مرحباً بك في $liveBrandName", "Welcome to $liveBrandName", "$liveBrandName ilovasiga xush kelibsiz")
                    }
                    val isVerified = currentUserProfile?.verified == true
                    val verifiedLabel = currentUserProfile?.verifiedBadgeLabel.orEmpty()
                    Text(
                        text = L10n.pick(lang, "السلام عليكم", "Assalamu Alaikum", "Assalomu alaykum"),
                        style = MaterialTheme.typography.labelMedium,
                        color = noorColors.goldAccent
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = greetingName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = L10n.pick(lang, "حساب موثّق", "Verified Account"),
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("home_user_verified_badge")
                            )
                            if (verifiedLabel.isNotBlank()) {
                                Text(
                                    text = verifiedLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                if (resolvedRole.isPrivileged) {
                    IconButton(
                        onClick = onOpenAdminCenter,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(noorColors.glassSurface)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape)
                            .testTag("home_corner_super_admin_button")
                    ) {
                        Icon(
                            Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Super Admin Dashboard",
                            tint = GoldBright
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Box {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(noorColors.glassSurface)
                            .border(1.dp, noorColors.glassBorderBottom, CircleShape)
                            .testTag("home_notifications_button")
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = noorColors.goldAccent)
                    }
                    if (unreadMessagesCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showCustomizeDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(noorColors.glassSurface)
                        .border(1.dp, noorColors.glassBorderBottom, CircleShape)
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, contentDescription = "Customize Home", tint = noorColors.goldAccent)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(noorColors.glassSurface)
                        .border(1.dp, noorColors.glassBorderBottom, CircleShape)
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

        // 1C. Administrative Advertisement Cards (Approved HOME placement only, Super Admin controlled)
        if (globalConfig.featureAdsEnabled && activeAdCards.any { it.placement == AdPlacement.HOME.key }) {
            item(key = "home_admin_ad_cards") {
                NoorAdCardBannerSection(
                    adCards = activeAdCards,
                    placement = AdPlacement.HOME,
                    onImpression = { viewModel.recordAdImpression(it) },
                    onClick = { viewModel.recordAdClick(it) }
                )
            }
        }

        // 1D. Live Owner Announcement Banner (if any active announcement exists)
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

        // 2B. NOOR Complete Islamic Ecosystem Quick Launch Card (Collapsible by Default, Smoothly Expands Downward)
        // 2. Collapsible NOOR Ecosystem Hub Drawer (controlled live by Super Admin feature flags)
        if (globalConfig.featureEcosystemHubEnabled) {
        item {
            val arrowRotation by animateFloatAsState(
                targetValue = if (ecosystemExpanded) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "ecosystem_arrow_rotation"
            )
            LiquidGlassCard(
                strong = true,
                goldBorder = true,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.testTag("home_ecosystem_hub_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenEcosystemHub)
                    ) {
                        Text(
                            text = L10n.pick(lang, "منظومة نور المتكاملة", "NOOR Complete Ecosystem", "NOOR to'liq tizimi"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = noorColors.goldAccent
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "اضغط على السهم لعرض جميع الأقسام والإضافات النادرة",
                                "Tap arrow to reveal all Islamic tools & rare modules",
                                "Barcha bo'limlarni ochish uchun strelkani bosing"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L10n.pick(lang, "الكل ←", "All →", "Barchasi →"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenEcosystemHub)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { ecosystemExpanded = !ecosystemExpanded },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.18f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), CircleShape)
                            .testTag("home_ecosystem_expand_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = L10n.pick(lang, "عرض المزيد", "Show More", "Ko'proq ko'rsatish"),
                            tint = noorColors.goldAccent,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Always visible top compact row (4 essentials)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeEcoMiniTile(L10n.pick(lang, "الحلقات", "Halaqat"), Icons.Rounded.Groups, onOpenHalaqat, Modifier.weight(1f))
                    HomeEcoMiniTile(L10n.pick(lang, "الحفظ", "Hifz AI"), Icons.Rounded.Mic, onOpenHifzAi, Modifier.weight(1f))
                    HomeEcoMiniTile(L10n.pick(lang, "نور AI", "Local AI"), Icons.Rounded.Psychology, onOpenLocalAi, Modifier.weight(1f))
                    HomeEcoMiniTile(L10n.pick(lang, "المكتبة", "Library"), Icons.Rounded.LocalLibrary, onOpenLibrary, Modifier.weight(1f))
                }

                // Expandable drawer with smooth vertical slide-down animation & live Super Admin module toggles
                AnimatedVisibility(
                    visible = ecosystemExpanded,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val drawerTiles = buildList {
                        add(Triple(L10n.pick(lang, "فيديوهات", "Videos"), Icons.Rounded.OndemandVideo, onOpenVideos))
                        if (globalConfig.featureKhatmahEnabled || globalConfig.featureFastingEnabled) {
                            add(Triple(L10n.pick(lang, "الختمة والصيام", "Khatm"), Icons.Rounded.NightsStay, onOpenKhatm))
                        }
                        add(Triple(L10n.pick(lang, "الإدارة المحلية", "Local Admin"), Icons.Rounded.AdminPanelSettings, onOpenOfflineAdmin))
                        if (globalConfig.featureZakatMirathEnabled) {
                            add(Triple(L10n.pick(lang, "حاسبة الزكاة", "Zakat", "Zakot"), Icons.Rounded.Calculate, onOpenZakat))
                        }
                        add(Triple(L10n.pick(lang, "أسماء الله", "99 Names", "99 Ism"), Icons.Rounded.AutoAwesome, onOpenAsmaUlHusna))
                        add(Triple(L10n.pick(lang, "الوضوء والعمرة", "Guides", "Qo'llanma"), Icons.Rounded.Mosque, onOpenWorshipGuides))
                        add(Triple(L10n.pick(lang, "قضاء الصلوات", "Qada", "Qazo"), Icons.Rounded.EventRepeat, onOpenQadaTracker))
                        if (globalConfig.featureZakatMirathEnabled) {
                            add(Triple(L10n.pick(lang, "المواريث", "Mirath", "Meros"), Icons.Rounded.Gavel, onOpenMirath))
                        }
                        add(Triple(L10n.pick(lang, "بطاقات آيات", "Cards", "Kartalar"), Icons.Rounded.Palette, onOpenCardDesigner))
                        if (globalConfig.featureHaramainLiveEnabled) {
                            add(Triple(L10n.pick(lang, "بث الحرمين", "Haramain", "Haramayn"), Icons.Rounded.LiveTv, onOpenHaramainLive))
                        }
                        add(Triple(L10n.pick(lang, "الباحث القرآني", "Quran Search", "Qur'on izlash"), Icons.Rounded.TravelExplore, onOpenGlobalQuranSearch))
                        add(Triple(L10n.pick(lang, "موسوعة الحديث", "Live Hadith", "Hadislar"), Icons.Rounded.MenuBook, onOpenLiveHadith))
                        if (globalConfig.featureIrabSarfEnabled) {
                            add(Triple(L10n.pick(lang, "إعراب وصرف", "I'rab & Sarf", "E'rob va Sarf"), Icons.Rounded.Spellcheck, onOpenQuranIrabSarf))
                        }
                        add(Triple(L10n.pick(lang, "مرصد الهلال", "Crescent Lab", "Hilol rasadxonasi"), Icons.Rounded.NightsStay, onOpenCrescentObservatory))
                        add(Triple(L10n.pick(lang, "نبض الأمة", "Ummah Pulse", "Ummat nafas"), Icons.Rounded.Public, onOpenGlobalUmmahPulse))
                        if (globalConfig.featureQuranUniverseEnabled) {
                            add(Triple(L10n.pick(lang, "مجرة القرآن", "Root Galaxy", "Ildiz galaktikasi"), Icons.Rounded.Hub, onOpenQuranUniverse))
                        }
                        if (globalConfig.featureMutashabihatSeerahEnabled) {
                            add(Triple(L10n.pick(lang, "متشابهات القرآن", "Mutashabihat", "Mutashobihot"), Icons.Rounded.CompareArrows, onOpenMutashabihat))
                            add(Triple(L10n.pick(lang, "أطلس السيرة", "Seerah Atlas", "Siyrat atlasi"), Icons.Rounded.HistoryEdu, onOpenSeerahAtlas))
                        }
                        if (globalConfig.featureCosmicGlobeEnabled) {
                            add(Triple(L10n.pick(lang, "الكرة ٣D للقبلة", "3D Globe", "3D Globus"), Icons.Rounded.Language, onOpenCosmicGlobe))
                        }
                        if (globalConfig.featureSoulRemedyEnabled) {
                            add(Triple(L10n.pick(lang, "دواء القلوب", "Soul Remedy", "Qalb shifosi"), Icons.Rounded.Favorite, onOpenSoulRemedy))
                        }
                        if (globalConfig.featureMiraclesProphetsEnabled) {
                            add(Triple(L10n.pick(lang, "الإعجاز والأنبياء", "25 Prophets", "25 Payg'ambar"), Icons.Rounded.Science, onOpenMiraclesProphets))
                        }
                        if (globalConfig.featureCalligraphyEnabled) {
                            add(Triple(L10n.pick(lang, "مرسم التذهيب", "Calligraphy", "Xattotlik"), Icons.Rounded.Brush, onOpenCalligraphyStudio))
                        }
                        if (globalConfig.featureSanctuaryAudioEnabled) {
                            add(Triple(L10n.pick(lang, "واحة السكينة", "Sanctuary", "Sokinlik"), Icons.Rounded.GraphicEq, onOpenSanctuaryAudio))
                        }
                        if (globalConfig.featureAstrolabeEnabled) {
                            add(Triple(L10n.pick(lang, "الأسطرلاب الحي", "Astrolabe", "Asturlob"), Icons.Rounded.Explore, onOpenAstrolabe))
                        }
                    }
                    Column {
                        drawerTiles.chunked(3).forEach { rowTiles ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTiles.forEach { (title, icon, action) ->
                                    HomeEcoMiniTile(title, icon, action, Modifier.weight(1f))
                                }
                                repeat(3 - rowTiles.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Bottom clickable expand/collapse arrow pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                        .clickable { ecosystemExpanded = !ecosystemExpanded }
                        .padding(vertical = 8.dp)
                        .testTag("home_ecosystem_more_pill"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ecosystemExpanded) {
                            L10n.pick(lang, "طيّ المنظومة", "Show Less", "Yig'ish")
                        } else {
                            L10n.pick(lang, "عرض المزيد من منظومة نور", "Show More NOOR Tools", "Ko'proq bo'limlarni ko'rish")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = noorColors.goldAccent,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
        }
        }

        // 3. Celestial Liquid Glass Hero Mosque Next Prayer Countdown Card
        item {
            val nextInfo = dashboard.nextPrayerInfo
            val iqamahDelay = if (prefs.iqamahDelays.enabled) prefs.iqamahDelays.delayFor(nextInfo.nextPrayer) else 0
            HeroPrayerGlassCard(
                nextInfo = nextInfo,
                timezoneId = prefs.timezoneId,
                iqamahDelayMinutes = iqamahDelay,
                tahajjudHour = prefs.tahajjudReminderHour,
                tahajjudMinute = prefs.tahajjudReminderMinute,
                lang = lang,
                onOpenPrayers = onOpenPrayers,
                onOpenTahajjudReminders = onOpenSettings
            )
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
                        val nowMillis = dashboard.gregorianCal.timeInMillis
                        dashboard.todaySchedule.allOrdered().forEach { (prayer, timeMs) ->
                            val isNext = prayer == dashboard.nextPrayerInfo.nextPrayer
                            val hasEntered = PrayerTimeGate.hasPrayerEntered(dashboard.todaySchedule, prayer, nowMillis)
                            val isLogged = hasEntered && when (prayer) {
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
                                    .background(
                                        when {
                                            isLogged -> Color(0xFF4ADE80).copy(alpha = 0.14f)
                                            isNext -> EmeraldPrimary.copy(alpha = 0.26f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .let { mod ->
                                        if (hasEntered) {
                                            mod.clickable { viewModel.togglePrayerLogged(prayer, nowMillis) }
                                        } else {
                                            mod
                                        }
                                    }
                                    .testTag("home_prayer_chip_${prayer.key}")
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isLogged) Icons.Rounded.CheckCircle else prayerIcon(prayer),
                                    contentDescription = null,
                                    tint = when {
                                        isLogged -> Color(0xFF4ADE80)
                                        isNext -> GoldBright
                                        hasEntered -> noorColors.goldAccent.copy(alpha = 0.9f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.prayerName(prayer, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isNext || isLogged) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasEntered || isNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    maxLines = 1
                                )
                                Text(
                                    text = L10n.formatTime12h(timeMs, prefs.timezoneId, lang)
                                        .replace(" AM", "").replace(" PM", "").replace(" ص", "").replace(" م", ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isLogged -> Color(0xFF4ADE80)
                                        isNext -> noorColors.goldAccent
                                        hasEntered -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    },
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

@Composable
private fun HomeEcoMiniTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    val tileShape = RoundedCornerShape(18.dp)
    val tileBrush = if (noorColors.isDark) {
        Brush.verticalGradient(listOf(Color(0x5217B88E), Color(0x280B2720)))
    } else {
        Brush.verticalGradient(listOf(Color(0xE8FFFFFF), Color(0xCCE1F3EC)))
    }
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(tileBrush)
            .border(
                1.dp,
                Brush.linearGradient(listOf(noorColors.glassBorderTop, GoldPrimary.copy(alpha = 0.42f))),
                tileShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

