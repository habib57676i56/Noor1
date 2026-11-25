package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.AudioPlaybackState
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.admin.AdminControlCenterScreen
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.AdhanSettingsScreen
import com.example.ui.screens.AdhkarDetailScreen
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.AudioLibraryScreen
import com.example.ui.screens.ContactSupportScreen
import com.example.ui.screens.EveningAdhkarScreen
import com.example.ui.screens.FounderProfileScreen
import com.example.ui.screens.HijriCalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MorningAdhkarScreen
import com.example.ui.screens.MosqueFinderScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.screens.ecosystem.AsmaUlHusnaScreen
import com.example.ui.screens.ecosystem.CosmicQiblaGlobeScreen
import com.example.ui.screens.ecosystem.CrescentObservatoryScreen
import com.example.ui.screens.ecosystem.EcosystemHubScreen
import com.example.ui.screens.ecosystem.GlobalQuranSearchScreen
import com.example.ui.screens.ecosystem.GlobalUmmahPulseScreen
import com.example.ui.screens.ecosystem.HalaqatScreen
import com.example.ui.screens.ecosystem.HaramainLiveScreen
import com.example.ui.screens.ecosystem.HifzAiScreen
import com.example.ui.screens.ecosystem.IslamicAstrolabeScreen
import com.example.ui.screens.ecosystem.IslamicCardDesignerScreen
import com.example.ui.screens.ecosystem.IslamicLibraryScreen
import com.example.ui.screens.ecosystem.IslamicSanctuaryAudioScreen
import com.example.ui.screens.ecosystem.KhatmAndWorshipScreen
import com.example.ui.screens.ecosystem.LiveHadithEncyclopediaScreen
import com.example.ui.screens.ecosystem.MirathCalculatorScreen
import com.example.ui.screens.ecosystem.NoorDuaaScreen
import com.example.ui.screens.ecosystem.NoorKidsScreen
import com.example.ui.screens.ecosystem.NoorLocalAiScreen
import com.example.ui.screens.ecosystem.NoorNotesScreen
import com.example.ui.screens.ecosystem.NoorQuizScreen
import com.example.ui.screens.ecosystem.NoorRadioScreen
import com.example.ui.screens.ecosystem.NoorTafseerExplorerScreen
import com.example.ui.screens.ecosystem.NoorVideosScreen
import com.example.ui.screens.ecosystem.NoorZakatScreen
import com.example.ui.screens.ecosystem.OfflineSuperAdminScreen
import com.example.ui.screens.ecosystem.QadaTrackerScreen
import com.example.ui.screens.ecosystem.QuranCalligraphyStudioScreen
import com.example.ui.screens.ecosystem.QuranIrabSarfScreen
import com.example.ui.screens.ecosystem.QuranMiraclesAndParablesScreen
import com.example.ui.screens.ecosystem.QuranMutashabihatScreen
import com.example.ui.screens.ecosystem.QuranRootAndQiraatUniverseScreen
import com.example.ui.screens.ecosystem.QuranicSoulRemedyScreen
import com.example.ui.screens.ecosystem.SeerahChronicleAtlasScreen
import com.example.ui.screens.ecosystem.TajweedScreen
import com.example.ui.screens.ecosystem.WorshipGuidesScreen
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.EcosystemViewModel
import com.example.viewmodel.NoorViewModel

object NoorRoutes {
    const val HOME = "home"
    const val PRAYERS = "prayers"
    const val QURAN = "quran"
    const val ADHKAR = "adhkar"
    const val MORNING_ADHKAR = "morning_adhkar"
    const val EVENING_ADHKAR = "evening_adhkar"
    const val TASBIH = "tasbih"
    const val QIBLA = "qibla"
    const val MOSQUES = "mosques"
    const val AUDIO_LIBRARY = "audio_library"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADHAN_SETTINGS = "adhan_settings"
    const val FOUNDER_PROFILE = "founder_profile"
    const val CONTACT_SUPPORT = "contact_support"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN_CENTER = "admin_center"
    const val QURAN_READER = "quran_reader/{surahNumber}"
    const val ADHKAR_DETAIL = "adhkar_detail/{categoryId}"

    // NOOR Complete Islamic Ecosystem Routes
    const val ECOSYSTEM_HUB = "ecosystem_hub"
    const val HALAQAT = "halaqat"
    const val HIFZ_AI = "hifz_ai"
    const val HIFZ_AI_TARGET = "hifz_ai/{surahNumber}/{ayahNumber}"
    const val LOCAL_AI = "local_ai"
    const val LIBRARY = "library"
    const val HADITH_LIB = "hadith_lib"
    const val TAFSEER_EXPLORER = "tafseer_explorer"
    const val TAJWEED = "tajweed"
    const val KIDS = "kids"
    const val KHATM = "khatm"
    const val QUIZ = "quiz"
    const val KIDS_QUIZ = "kids_quiz"
    const val VIDEOS = "videos"
    const val KIDS_VIDEOS = "kids_videos"
    const val NOTES = "notes"
    const val DUAA = "duaa"
    const val RADIO = "radio"
    const val OFFLINE_ADMIN = "offline_admin"
    const val ZAKAT = "zakat"
    const val ASMA_UL_HUSNA = "asma_ul_husna"
    const val WORSHIP_GUIDES = "worship_guides"
    const val QADA_TRACKER = "qada_tracker"
    const val MIRATH = "mirath"
    const val CARD_DESIGNER = "card_designer"
    const val HARAMAIN_LIVE = "haramain_live"
    const val GLOBAL_QURAN_SEARCH = "global_quran_search"
    const val LIVE_HADITH = "live_hadith"
    const val QURAN_IRAB_SARF = "quran_irab_sarf"
    const val CRESCENT_OBSERVATORY = "crescent_observatory"
    const val GLOBAL_UMMAH_PULSE = "global_ummah_pulse"
    const val QURAN_MUTASHABIHAT = "quran_mutashabihat"
    const val SEERAH_ATLAS = "seerah_atlas"
    const val QURAN_UNIVERSE = "quran_root_qiraat_universe"
    const val COSMIC_QIBLA_GLOBE = "cosmic_qibla_globe"
    const val QURAN_SOUL_REMEDY = "quran_soul_remedy"
    const val QURAN_MIRACLES_PROPHETS = "quran_miracles_prophets"
    const val QURAN_CALLIGRAPHY_STUDIO = "quran_calligraphy_studio"
    const val ISLAMIC_SANCTUARY_AUDIO = "islamic_sanctuary_audio"
    const val ISLAMIC_ASTROLABE = "islamic_astrolabe"

    fun quranReader(surahNumber: Int) = "quran_reader/$surahNumber"
    fun adhkarDetail(categoryId: String) = "adhkar_detail/$categoryId"
    fun hifzAiTarget(surahNumber: Int, ayahNumber: Int) = "hifz_ai/$surahNumber/$ayahNumber"
}

private data class NavTabItem(
    val route: String,
    val labelAr: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    NavTabItem(NoorRoutes.HOME, "الرئيسية", "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTabItem(NoorRoutes.PRAYERS, "الصلاة", "Prayers", Icons.Rounded.Mosque, Icons.Outlined.Mosque, "nav_prayers"),
    NavTabItem(NoorRoutes.QURAN, "القرآن", "Quran", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook, "nav_quran"),
    NavTabItem(NoorRoutes.ADHKAR, "الأذكار", "Adhkar", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_adhkar"),
    NavTabItem(NoorRoutes.TASBIH, "المسبحة", "Tasbih", Icons.Rounded.TouchApp, Icons.Outlined.TouchApp, "nav_tasbih")
)

@Composable
fun NoorAppShell(
    viewModel: NoorViewModel,
    adminViewModel: AdminViewModel,
    ecosystemViewModel: EcosystemViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NoorRoutes.HOME
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val ecosystemStatus by ecosystemViewModel.statusMessage.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val resolvedRole by viewModel.resolvedAdminRole.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val pendingShortcut by viewModel.pendingShortcutRoute.collectAsStateWithLifecycle()

    LaunchedEffect(resolvedRole) {
        adminViewModel.syncAuthorizedRole(resolvedRole)
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(ecosystemStatus) {
        ecosystemStatus?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            ecosystemViewModel.clearStatusMessage()
        }
    }

    var isMushafFullScreen by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != NoorRoutes.QURAN_READER) {
            isMushafFullScreen = false
        }
    }

    val topLevelRoutes = remember { bottomTabs.map { it.route }.toSet() }
    val isAdminCenter = currentRoute == NoorRoutes.ADMIN_CENTER
    val isReaderFullScreen = isMushafFullScreen && currentRoute == NoorRoutes.QURAN_READER
    val showNavBar = currentRoute in topLevelRoutes && !isAdminCenter
    val showMiniPlayer = audioState.hasActiveSession &&
        currentRoute != NoorRoutes.AUDIO_LIBRARY &&
        !isAdminCenter &&
        !isReaderFullScreen

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Handle Android App Shortcuts, Notification taps, and Home Screen Widget deep-links
    LaunchedEffect(pendingShortcut) {
        val target = pendingShortcut ?: return@LaunchedEffect
        viewModel.consumeShortcutRoute()
        when (target) {
            NoorShortcutManager.TARGET_QURAN -> navigateToTab(NoorRoutes.QURAN)
            NoorShortcutManager.TARGET_RESUME_READING -> {
                navController.navigate(NoorRoutes.quranReader(prefs.lastReadSurah.coerceIn(1, 114))) {
                    launchSingleTop = true
                }
            }
            NoorShortcutManager.TARGET_START_LISTENING -> {
                if (!audioState.isPlaying) {
                    viewModel.playFullSurahWithReciter(prefs.lastReadSurah.coerceIn(1, 114), prefs.quranReciter)
                }
                navController.navigate(NoorRoutes.AUDIO_LIBRARY) { launchSingleTop = true }
            }
            NoorShortcutManager.TARGET_AUDIO_LIBRARY -> {
                navController.navigate(NoorRoutes.AUDIO_LIBRARY) { launchSingleTop = true }
            }
            NoorShortcutManager.TARGET_PRAYER_TIMES -> navigateToTab(NoorRoutes.PRAYERS)
            NoorShortcutManager.TARGET_QIBLA -> {
                navController.navigate(NoorRoutes.QIBLA) { launchSingleTop = true }
            }
            NoorShortcutManager.TARGET_ADHAN_SETTINGS -> {
                navController.navigate(NoorRoutes.ADHAN_SETTINGS) { launchSingleTop = true }
            }
        }
    }

    IslamicGeometricBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && showNavBar) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = noorColors.glassSurfaceStrong
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = L10n.pick(lang, tab.labelAr, tab.labelEn)
                                    )
                                },
                                label = { Text(L10n.pick(lang, tab.labelAr, tab.labelEn)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            if (showMiniPlayer) {
                                FloatingAudioMiniPlayer(
                                    audioState = audioState,
                                    onOpenPlayer = { navController.navigate(NoorRoutes.AUDIO_LIBRARY) },
                                    onTogglePlayPause = { viewModel.toggleAudioPauseResume() },
                                    onNext = { viewModel.playNextAudio() },
                                    onStop = { viewModel.stopQuranAudio() }
                                )
                            }
                            if (!useRail && showNavBar) {
                                val dockShape = RoundedCornerShape(32.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .clip(dockShape)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    noorColors.glassDockTop,
                                                    noorColors.glassDockBottom
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    noorColors.glassBorderTop,
                                                    GoldPrimary.copy(alpha = if (noorColors.isDark) 0.42f else 0.36f),
                                                    noorColors.glassBorderBottom
                                                )
                                            ),
                                            shape = dockShape
                                        )
                                ) {
                                    // Top curved liquid-glass specular sheen line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter)
                                            .padding(horizontal = 24.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        noorColors.glassSpecularTop,
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .size(height = 2.dp, width = 200.dp)
                                    )
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets(0, 0, 0, 0)
                                    ) {
                                        bottomTabs.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateToTab(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = L10n.pick(lang, tab.labelAr, tab.labelEn)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = L10n.pick(lang, tab.labelAr, tab.labelEn),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = if (noorColors.isDark) GoldBright else noorColors.goldAccent,
                                                    selectedTextColor = if (noorColors.isDark) GoldBright else noorColors.goldAccent,
                                                    indicatorColor = if (noorColors.isDark) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                                    } else {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                    },
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.testTag(tab.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = NoorRoutes.HOME
                        ) {
                            composable(NoorRoutes.HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onOpenPrayers = { navigateToTab(NoorRoutes.PRAYERS) },
                                    onOpenQibla = { navController.navigate(NoorRoutes.QIBLA) },
                                    onOpenAdhkar = { navigateToTab(NoorRoutes.ADHKAR) },
                                    onOpenMorningAdhkar = { navController.navigate(NoorRoutes.MORNING_ADHKAR) },
                                    onOpenEveningAdhkar = { navController.navigate(NoorRoutes.EVENING_ADHKAR) },
                                    onOpenAdhkarCategory = { catId ->
                                        when (catId) {
                                            "morning" -> navController.navigate(NoorRoutes.MORNING_ADHKAR)
                                            "evening" -> navController.navigate(NoorRoutes.EVENING_ADHKAR)
                                            else -> navController.navigate(NoorRoutes.adhkarDetail(catId))
                                        }
                                    },
                                    onOpenTasbih = { navigateToTab(NoorRoutes.TASBIH) },
                                    onOpenQuran = { navigateToTab(NoorRoutes.QURAN) },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) },
                                    onOpenAudioLibrary = { navController.navigate(NoorRoutes.AUDIO_LIBRARY) },
                                    onOpenMosques = { navController.navigate(NoorRoutes.MOSQUES) },
                                    onOpenCalendar = { navController.navigate(NoorRoutes.CALENDAR) },
                                    onOpenStats = { navController.navigate(NoorRoutes.STATS) },
                                    onOpenSettings = { navController.navigate(NoorRoutes.SETTINGS) },
                                    onOpenFounderProfile = { navController.navigate(NoorRoutes.FOUNDER_PROFILE) },
                                    onOpenSupport = { navController.navigate(NoorRoutes.CONTACT_SUPPORT) },
                                    onOpenNotifications = { navController.navigate(NoorRoutes.NOTIFICATIONS) },
                                    onOpenAdminCenter = { navController.navigate(NoorRoutes.ADMIN_CENTER) },
                                    onOpenEcosystemHub = { navController.navigate(NoorRoutes.ECOSYSTEM_HUB) },
                                    onOpenHalaqat = { navController.navigate(NoorRoutes.HALAQAT) },
                                    onOpenHifzAi = { navController.navigate(NoorRoutes.HIFZ_AI) },
                                    onOpenLocalAi = { navController.navigate(NoorRoutes.LOCAL_AI) },
                                    onOpenLibrary = { navController.navigate(NoorRoutes.LIBRARY) },
                                    onOpenKhatm = { navController.navigate(NoorRoutes.KHATM) },
                                    onOpenVideos = { navController.navigate(NoorRoutes.VIDEOS) },
                                    onOpenOfflineAdmin = { navController.navigate(NoorRoutes.OFFLINE_ADMIN) },
                                    onOpenZakat = { navController.navigate(NoorRoutes.ZAKAT) },
                                    onOpenAsmaUlHusna = { navController.navigate(NoorRoutes.ASMA_UL_HUSNA) },
                                    onOpenWorshipGuides = { navController.navigate(NoorRoutes.WORSHIP_GUIDES) },
                                    onOpenQadaTracker = { navController.navigate(NoorRoutes.QADA_TRACKER) },
                                    onOpenMirath = { navController.navigate(NoorRoutes.MIRATH) },
                                    onOpenCardDesigner = { navController.navigate(NoorRoutes.CARD_DESIGNER) },
                                    onOpenHaramainLive = { navController.navigate(NoorRoutes.HARAMAIN_LIVE) },
                                    onOpenGlobalQuranSearch = { navController.navigate(NoorRoutes.GLOBAL_QURAN_SEARCH) },
                                    onOpenLiveHadith = { navController.navigate(NoorRoutes.LIVE_HADITH) },
                                    onOpenQuranIrabSarf = { navController.navigate(NoorRoutes.QURAN_IRAB_SARF) },
                                    onOpenCrescentObservatory = { navController.navigate(NoorRoutes.CRESCENT_OBSERVATORY) },
                                    onOpenGlobalUmmahPulse = { navController.navigate(NoorRoutes.GLOBAL_UMMAH_PULSE) },
                                    onOpenMutashabihat = { navController.navigate(NoorRoutes.QURAN_MUTASHABIHAT) },
                                    onOpenSeerahAtlas = { navController.navigate(NoorRoutes.SEERAH_ATLAS) },
                                    onOpenQuranUniverse = { navController.navigate(NoorRoutes.QURAN_UNIVERSE) },
                                    onOpenCosmicGlobe = { navController.navigate(NoorRoutes.COSMIC_QIBLA_GLOBE) },
                                    onOpenSoulRemedy = { navController.navigate(NoorRoutes.QURAN_SOUL_REMEDY) },
                                    onOpenMiraclesProphets = { navController.navigate(NoorRoutes.QURAN_MIRACLES_PROPHETS) },
                                    onOpenCalligraphyStudio = { navController.navigate(NoorRoutes.QURAN_CALLIGRAPHY_STUDIO) },
                                    onOpenSanctuaryAudio = { navController.navigate(NoorRoutes.ISLAMIC_SANCTUARY_AUDIO) },
                                    onOpenAstrolabe = { navController.navigate(NoorRoutes.ISLAMIC_ASTROLABE) }
                                )
                            }
                            composable(NoorRoutes.PRAYERS) {
                                PrayerTimesScreen(
                                    viewModel = viewModel,
                                    onOpenQibla = { navController.navigate(NoorRoutes.QIBLA) },
                                    onOpenMosques = { navController.navigate(NoorRoutes.MOSQUES) }
                                )
                            }
                            composable(NoorRoutes.QURAN) {
                                QuranScreen(
                                    viewModel = viewModel,
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) },
                                    onOpenAudioLibrary = { navController.navigate(NoorRoutes.AUDIO_LIBRARY) }
                                )
                            }
                            composable(
                                route = NoorRoutes.QURAN_READER,
                                arguments = listOf(navArgument("surahNumber") { type = NavType.IntType })
                            ) { entry ->
                                val surahNum = entry.arguments?.getInt("surahNumber") ?: 1
                                QuranReaderScreen(
                                    surahNumber = surahNum,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToSurah = { nextSurah ->
                                        navController.navigate(NoorRoutes.quranReader(nextSurah.coerceIn(1, 114))) {
                                            popUpTo(NoorRoutes.QURAN_READER) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenInMemorization = { targetSurah, targetAyah ->
                                        navController.navigate(NoorRoutes.hifzAiTarget(targetSurah, targetAyah))
                                    },
                                    ecosystemViewModel = ecosystemViewModel,
                                    onFullScreenChanged = { isMushafFullScreen = it }
                                )
                            }
                            composable(NoorRoutes.ADHKAR) {
                                AdhkarScreen(
                                    viewModel = viewModel,
                                    onOpenCategory = { catId ->
                                        when (catId) {
                                            "morning" -> navController.navigate(NoorRoutes.MORNING_ADHKAR)
                                            "evening" -> navController.navigate(NoorRoutes.EVENING_ADHKAR)
                                            else -> navController.navigate(NoorRoutes.adhkarDetail(catId))
                                        }
                                    }
                                )
                            }
                            composable(NoorRoutes.MORNING_ADHKAR) {
                                MorningAdhkarScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.EVENING_ADHKAR) {
                                EveningAdhkarScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = NoorRoutes.ADHKAR_DETAIL,
                                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                            ) { entry ->
                                val catId = entry.arguments?.getString("categoryId") ?: "morning"
                                when (catId) {
                                    "morning" -> MorningAdhkarScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                    "evening" -> EveningAdhkarScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                    else -> AdhkarDetailScreen(
                                        categoryId = catId,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                            composable(NoorRoutes.TASBIH) {
                                TasbihScreen(viewModel = viewModel)
                            }
                            composable(NoorRoutes.QIBLA) {
                                QiblaScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenMosques = { navController.navigate(NoorRoutes.MOSQUES) }
                                )
                            }
                            composable(NoorRoutes.MOSQUES) {
                                MosqueFinderScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.AUDIO_LIBRARY) {
                                AudioLibraryScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.CALENDAR) {
                                HijriCalendarScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.STATS) {
                                StatsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.FOUNDER_PROFILE) {
                                FounderProfileScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenSupport = { navController.navigate(NoorRoutes.CONTACT_SUPPORT) }
                                )
                            }
                            composable(NoorRoutes.CONTACT_SUPPORT) {
                                ContactSupportScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.NOTIFICATIONS) {
                                NotificationCenterScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.ADMIN_CENTER) {
                                AdminControlCenterScreen(
                                    adminViewModel = adminViewModel,
                                    resolvedRole = resolvedRole,
                                    onExitToApp = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.SETTINGS) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenPrayers = {
                                        navController.popBackStack()
                                        navigateToTab(NoorRoutes.PRAYERS)
                                    },
                                    onOpenFounderProfile = { navController.navigate(NoorRoutes.FOUNDER_PROFILE) },
                                    onOpenSupport = { navController.navigate(NoorRoutes.CONTACT_SUPPORT) },
                                    onOpenNotifications = { navController.navigate(NoorRoutes.NOTIFICATIONS) },
                                    onOpenAdminCenter = { navController.navigate(NoorRoutes.ADMIN_CENTER) },
                                    onOpenAdhanSettings = { navController.navigate(NoorRoutes.ADHAN_SETTINGS) },
                                    onOpenOfflineAdmin = { navController.navigate(NoorRoutes.OFFLINE_ADMIN) },
                                    onOpenEcosystemHub = { navController.navigate(NoorRoutes.ECOSYSTEM_HUB) }
                                )
                            }
                            composable(NoorRoutes.ADHAN_SETTINGS) {
                                AdhanSettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            // NOOR Complete Islamic Ecosystem Destinations
                            composable(NoorRoutes.ECOSYSTEM_HUB) {
                                val activeAdCards by viewModel.activeAdCards.collectAsStateWithLifecycle()
                                EcosystemHubScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHalaqat = { navController.navigate(NoorRoutes.HALAQAT) },
                                    onOpenHifzAi = { navController.navigate(NoorRoutes.HIFZ_AI) },
                                    onOpenLocalAi = { navController.navigate(NoorRoutes.LOCAL_AI) },
                                    onOpenLibrary = { navController.navigate(NoorRoutes.LIBRARY) },
                                    onOpenTajweed = { navController.navigate(NoorRoutes.TAJWEED) },
                                    onOpenKids = { navController.navigate(NoorRoutes.KIDS) },
                                    onOpenVideos = { navController.navigate(NoorRoutes.VIDEOS) },
                                    onOpenKhatm = { navController.navigate(NoorRoutes.KHATM) },
                                    onOpenQuiz = { navController.navigate(NoorRoutes.QUIZ) },
                                    onOpenNotes = { navController.navigate(NoorRoutes.NOTES) },
                                    onOpenHadith = { navController.navigate(NoorRoutes.HADITH_LIB) },
                                    onOpenTafseer = { navController.navigate(NoorRoutes.TAFSEER_EXPLORER) },
                                    onOpenDuaa = { navController.navigate(NoorRoutes.DUAA) },
                                    onOpenRadio = { navController.navigate(NoorRoutes.RADIO) },
                                    onOpenOfflineAdmin = { navController.navigate(NoorRoutes.OFFLINE_ADMIN) },
                                    onOpenZakat = { navController.navigate(NoorRoutes.ZAKAT) },
                                    onOpenAsmaUlHusna = { navController.navigate(NoorRoutes.ASMA_UL_HUSNA) },
                                    onOpenWorshipGuides = { navController.navigate(NoorRoutes.WORSHIP_GUIDES) },
                                    onOpenQadaTracker = { navController.navigate(NoorRoutes.QADA_TRACKER) },
                                    onOpenMirath = { navController.navigate(NoorRoutes.MIRATH) },
                                    onOpenCardDesigner = { navController.navigate(NoorRoutes.CARD_DESIGNER) },
                                    onOpenHaramainLive = { navController.navigate(NoorRoutes.HARAMAIN_LIVE) },
                                    onOpenGlobalQuranSearch = { navController.navigate(NoorRoutes.GLOBAL_QURAN_SEARCH) },
                                    onOpenLiveHadith = { navController.navigate(NoorRoutes.LIVE_HADITH) },
                                    onOpenQuranIrabSarf = { navController.navigate(NoorRoutes.QURAN_IRAB_SARF) },
                                    onOpenCrescentObservatory = { navController.navigate(NoorRoutes.CRESCENT_OBSERVATORY) },
                                    onOpenGlobalUmmahPulse = { navController.navigate(NoorRoutes.GLOBAL_UMMAH_PULSE) },
                                    onOpenMutashabihat = { navController.navigate(NoorRoutes.QURAN_MUTASHABIHAT) },
                                    onOpenSeerahAtlas = { navController.navigate(NoorRoutes.SEERAH_ATLAS) },
                                    onOpenQuranUniverse = { navController.navigate(NoorRoutes.QURAN_UNIVERSE) },
                                    onOpenCosmicGlobe = { navController.navigate(NoorRoutes.COSMIC_QIBLA_GLOBE) },
                                    onOpenSoulRemedy = { navController.navigate(NoorRoutes.QURAN_SOUL_REMEDY) },
                                    onOpenMiraclesProphets = { navController.navigate(NoorRoutes.QURAN_MIRACLES_PROPHETS) },
                                    onOpenCalligraphyStudio = { navController.navigate(NoorRoutes.QURAN_CALLIGRAPHY_STUDIO) },
                                    onOpenSanctuaryAudio = { navController.navigate(NoorRoutes.ISLAMIC_SANCTUARY_AUDIO) },
                                    onOpenAstrolabe = { navController.navigate(NoorRoutes.ISLAMIC_ASTROLABE) },
                                    activeAdCards = activeAdCards,
                                    onAdImpression = { viewModel.recordAdImpression(it) },
                                    onAdClick = { viewModel.recordAdClick(it) }
                                )
                            }
                            composable(NoorRoutes.HALAQAT) {
                                HalaqatScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.HIFZ_AI) {
                                val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
                                HifzAiScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    globalConfig = globalConfig,
                                    onOpenMushaf = { targetSurah ->
                                        navController.navigate(NoorRoutes.quranReader(targetSurah.coerceIn(1, 114)))
                                    }
                                )
                            }
                            composable(
                                route = NoorRoutes.HIFZ_AI_TARGET,
                                arguments = listOf(
                                    navArgument("surahNumber") { type = NavType.IntType },
                                    navArgument("ayahNumber") { type = NavType.IntType }
                                )
                            ) { entry ->
                                val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
                                val initSurah = entry.arguments?.getInt("surahNumber") ?: 1
                                val initAyah = entry.arguments?.getInt("ayahNumber") ?: 1
                                HifzAiScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    globalConfig = globalConfig,
                                    initialSurah = initSurah,
                                    initialAyah = initAyah,
                                    onOpenMushaf = { targetSurah ->
                                        navController.navigate(NoorRoutes.quranReader(targetSurah.coerceIn(1, 114)))
                                    }
                                )
                            }
                            composable(NoorRoutes.LOCAL_AI) {
                                NoorLocalAiScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.LIBRARY) {
                                IslamicLibraryScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.HADITH_LIB) {
                                IslamicLibraryScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    initialCategory = "HADITH"
                                )
                            }
                            composable(NoorRoutes.TAFSEER_EXPLORER) {
                                NoorTafseerExplorerScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.TAJWEED) {
                                TajweedScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.KIDS) {
                                NoorKidsScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { s -> navController.navigate(NoorRoutes.quranReader(s)) },
                                    onOpenKidsQuiz = { navController.navigate(NoorRoutes.KIDS_QUIZ) },
                                    onOpenCartoons = { navController.navigate(NoorRoutes.KIDS_VIDEOS) }
                                )
                            }
                            composable(NoorRoutes.VIDEOS) {
                                val authUser by viewModel.currentUser.collectAsStateWithLifecycle()
                                val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
                                val effectiveName = prefs.userName.ifBlank {
                                    userProfile?.displayName.orEmpty().ifBlank { authUser?.displayName.orEmpty() }
                                }
                                val effectiveAvatar = prefs.customAvatarUri.ifBlank {
                                    authUser?.photoUrl?.toString().orEmpty()
                                }
                                NoorVideosScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    currentUserName = effectiveName,
                                    currentUserAvatarUrl = effectiveAvatar
                                )
                            }
                            composable(NoorRoutes.KIDS_VIDEOS) {
                                val authUser by viewModel.currentUser.collectAsStateWithLifecycle()
                                val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
                                val effectiveName = prefs.userName.ifBlank {
                                    userProfile?.displayName.orEmpty().ifBlank { authUser?.displayName.orEmpty() }
                                }
                                val effectiveAvatar = prefs.customAvatarUri.ifBlank {
                                    authUser?.photoUrl?.toString().orEmpty()
                                }
                                NoorVideosScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    initialCategory = IslamicVideoCategory.ISLAMIC_CARTOONS,
                                    currentUserName = effectiveName,
                                    currentUserAvatarUrl = effectiveAvatar
                                )
                            }
                            composable(NoorRoutes.KHATM) {
                                val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
                                val currentPage = com.example.data.local.catalog.SurahCatalog.findSurah(prefs.lastReadSurah).startPage
                                KhatmAndWorshipScreen(
                                    viewModel = ecosystemViewModel,
                                    currentReaderPage = currentPage,
                                    onBack = { navController.popBackStack() },
                                    onOpenQuran = { navigateToTab(NoorRoutes.QURAN) },
                                    globalConfig = globalConfig
                                )
                            }
                            composable(NoorRoutes.QUIZ) {
                                NoorQuizScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.KIDS_QUIZ) {
                                NoorQuizScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    initialCategory = "KIDS"
                                )
                            }
                            composable(NoorRoutes.NOTES) {
                                NoorNotesScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { s -> navController.navigate(NoorRoutes.quranReader(s)) }
                                )
                            }
                            composable(NoorRoutes.DUAA) {
                                NoorDuaaScreen(onBack = { navController.popBackStack() })
                            }
                            composable(NoorRoutes.RADIO) {
                                NoorRadioScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.OFFLINE_ADMIN) {
                                OfflineSuperAdminScreen(
                                    viewModel = ecosystemViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.ZAKAT) {
                                NoorZakatScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.ASMA_UL_HUSNA) {
                                AsmaUlHusnaScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.WORSHIP_GUIDES) {
                                WorshipGuidesScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.QADA_TRACKER) {
                                QadaTrackerScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.MIRATH) {
                                MirathCalculatorScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.CARD_DESIGNER) {
                                IslamicCardDesignerScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.HARAMAIN_LIVE) {
                                HaramainLiveScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.GLOBAL_QURAN_SEARCH) {
                                GlobalQuranSearchScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.LIVE_HADITH) {
                                LiveHadithEncyclopediaScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.QURAN_IRAB_SARF) {
                                QuranIrabSarfScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.CRESCENT_OBSERVATORY) {
                                CrescentObservatoryScreen(
                                    latitude = prefs.latitude,
                                    longitude = prefs.longitude,
                                    cityLabel = L10n.pick(lang, prefs.cityNameAr, prefs.cityNameEn),
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.GLOBAL_UMMAH_PULSE) {
                                GlobalUmmahPulseScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.QURAN_MUTASHABIHAT) {
                                QuranMutashabihatScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.SEERAH_ATLAS) {
                                SeerahChronicleAtlasScreen(
                                    userLatitude = prefs.latitude,
                                    userLongitude = prefs.longitude,
                                    userCityLabel = L10n.pick(lang, prefs.cityNameAr, prefs.cityNameEn),
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.QURAN_UNIVERSE) {
                                QuranRootAndQiraatUniverseScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.COSMIC_QIBLA_GLOBE) {
                                CosmicQiblaGlobeScreen(
                                    userLatitude = prefs.latitude,
                                    userLongitude = prefs.longitude,
                                    userCityLabel = L10n.pick(lang, prefs.cityNameAr, prefs.cityNameEn),
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.QURAN_SOUL_REMEDY) {
                                QuranicSoulRemedyScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.QURAN_MIRACLES_PROPHETS) {
                                QuranMiraclesAndParablesScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.QURAN_CALLIGRAPHY_STUDIO) {
                                QuranCalligraphyStudioScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSurah = { surah -> navController.navigate(NoorRoutes.quranReader(surah)) }
                                )
                            }
                            composable(NoorRoutes.ISLAMIC_SANCTUARY_AUDIO) {
                                IslamicSanctuaryAudioScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(NoorRoutes.ISLAMIC_ASTROLABE) {
                                IslamicAstrolabeScreen(
                                    userLatitude = prefs.latitude,
                                    userLongitude = prefs.longitude,
                                    userCityLabel = L10n.pick(lang, prefs.cityNameAr, prefs.cityNameEn),
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingAudioMiniPlayer(
    audioState: AudioPlaybackState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val pillShape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(pillShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xE810362C), Color(0xEC071D17), Color(0xF0051410))
                )
            )
            .border(
                1.2.dp,
                Brush.linearGradient(
                    listOf(GoldBright.copy(alpha = 0.8f), noorColors.glassBorderTop, GoldPrimary.copy(alpha = 0.4f))
                ),
                pillShape
            )
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = L10n.pick(lang, audioState.titleAr, audioState.titleEn).ifBlank { "NOOR Audio" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = L10n.pick(lang, audioState.subtitleAr, audioState.subtitleEn),
                style = MaterialTheme.typography.labelSmall,
                color = GoldBright.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play/Pause",
                tint = GoldBright
            )
        }
        if (audioState.hasNext) {
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
            }
        }
        IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Player", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}
