package com.example

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.FirebaseAuthHelper
import com.example.data.cloud.NoorPlatformConstants
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.admin.AdminControlCenterScreen
import com.example.ui.components.Noor3DLaunchSplashOverlay
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalIs24HourFormat
import com.example.ui.navigation.NoorAppShell
import com.example.ui.screens.ForceUpdateGateScreen
import com.example.ui.screens.MaintenanceModeScreen
import com.example.ui.screens.NoorAuthGateScreen
import com.example.ui.theme.NoorTheme
import com.example.ui.theme.NoorVisualSettings
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.EcosystemViewModel
import com.example.viewmodel.NoorViewModel

class MainActivity : ComponentActivity() {

    private var incomingShortcutRoute by mutableStateOf<String?>(null)
    private var isSystem24HourFormat by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isSystem24HourFormat = DateFormat.is24HourFormat(this)
        incomingShortcutRoute = intent?.getStringExtra(NoorShortcutManager.EXTRA_SHORTCUT_ROUTE)
        setContent {
            val viewModel: NoorViewModel = viewModel()
            val adminViewModel: AdminViewModel = viewModel()
            val ecosystemViewModel: EcosystemViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
            val resolvedRole by viewModel.resolvedAdminRole.collectAsStateWithLifecycle()
            val layoutDirection = if (prefs.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            var allowOfflineLocalMode by remember { mutableStateOf(false) }
            var adminBypassMaintenance by remember { mutableStateOf(false) }
            var show3DLaunchSplash by rememberSaveable { mutableStateOf(true) }

            val pendingRoute = incomingShortcutRoute
            LaunchedEffect(pendingRoute) {
                if (!pendingRoute.isNullOrBlank()) {
                    viewModel.dispatchShortcutRoute(pendingRoute)
                    incomingShortcutRoute = null
                }
            }

            val liveVisualSettings = remember(
                globalConfig.themePreset,
                globalConfig.glassOpacity,
                globalConfig.effectsStarlightEnabled,
                globalConfig.effectsAuroraEnabled,
                globalConfig.effects3DSplashEnabled,
                globalConfig.effectsIntensity,
                prefs.backgroundStarsEnabled,
                prefs.backgroundAuroraEnabled
            ) {
                NoorVisualSettings(
                    themePreset = globalConfig.themePreset,
                    glassOpacity = globalConfig.glassOpacity.toFloat().coerceIn(0.45f, 1.0f),
                    effectsStarlightEnabled = globalConfig.effectsStarlightEnabled && prefs.backgroundStarsEnabled,
                    effectsAuroraEnabled = globalConfig.effectsAuroraEnabled && prefs.backgroundAuroraEnabled,
                    effects3DSplashEnabled = globalConfig.effects3DSplashEnabled,
                    effectsIntensity = globalConfig.effectsIntensity.toFloat().coerceIn(0.2f, 1.5f)
                )
            }

            val isForceUpdateBlocked = remember(
                globalConfig.forceUpdateEnabled,
                globalConfig.minSupportedVersion
            ) {
                AppVersionGate.isForceUpdateRequired(
                    currentVersion = NoorPlatformConstants.APP_VERSION_NAME,
                    config = globalConfig
                )
            }

            // When the owner (Super Admin) launches a newly built release (e.g. v3.1.0),
            // automatically raise Firestore's minSupportedVersion so older APKs immediately
            // display the mandatory Telegram update screen (https://t.me/Noor_ui).
            var didAutoSyncMinVersion by remember { mutableStateOf(false) }
            LaunchedEffect(resolvedRole, globalConfig.minSupportedVersion, currentUser?.uid) {
                if (!didAutoSyncMinVersion &&
                    currentUser != null &&
                    resolvedRole.canControlGlobalFeatures &&
                    AppVersionGate.compareVersions(
                        NoorPlatformConstants.APP_VERSION_NAME,
                        globalConfig.minSupportedVersion
                    ) > 0
                ) {
                    didAutoSyncMinVersion = true
                    adminViewModel.saveGlobalConfig(
                        globalConfig.copy(
                            minSupportedVersion = NoorPlatformConstants.APP_VERSION_NAME,
                            forceUpdateEnabled = false,
                            updateChannelUrl = AppVersionGate.resolveSafeUpdateUrl(globalConfig.updateChannelUrl)
                        )
                    )
                }
            }

            CompositionLocalProvider(
                LocalAppLanguage provides prefs.language,
                LocalIs24HourFormat provides isSystem24HourFormat,
                LocalLayoutDirection provides layoutDirection
            ) {
                NoorTheme(
                    themeMode = prefs.themeMode,
                    visualSettings = liveVisualSettings
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val user = currentUser
                        if (isForceUpdateBlocked && !(adminBypassMaintenance && resolvedRole.isPrivileged)) {
                            ForceUpdateGateScreen(
                                config = globalConfig,
                                currentVersion = NoorPlatformConstants.APP_VERSION_NAME,
                                canBypassAsAdmin = resolvedRole.isPrivileged,
                                onOpenAdminCenter = { adminBypassMaintenance = true }
                            )
                        } else if (isForceUpdateBlocked && adminBypassMaintenance && resolvedRole.isPrivileged) {
                            AdminControlCenterScreen(
                                adminViewModel = adminViewModel,
                                resolvedRole = resolvedRole,
                                onExitToApp = { adminBypassMaintenance = false }
                            )
                        } else if (user == null && !allowOfflineLocalMode) {
                            NoorAuthGateScreen(
                                onContinueOfflineWhenUnconfigured = { allowOfflineLocalMode = true }
                            )
                        } else {
                            key(user?.uid ?: "offline_local") {
                                when {
                                    globalConfig.maintenanceMode && adminBypassMaintenance && resolvedRole.isPrivileged -> {
                                        AdminControlCenterScreen(
                                            adminViewModel = adminViewModel,
                                            resolvedRole = resolvedRole,
                                            onExitToApp = { adminBypassMaintenance = false }
                                        )
                                    }
                                    globalConfig.maintenanceMode -> {
                                        MaintenanceModeScreen(
                                            config = globalConfig,
                                            canBypassAsAdmin = resolvedRole.isPrivileged,
                                            onOpenAdminCenter = { adminBypassMaintenance = true }
                                        )
                                    }
                                    else -> {
                                        NoorAppShell(
                                            viewModel = viewModel,
                                            adminViewModel = adminViewModel,
                                            ecosystemViewModel = ecosystemViewModel
                                        )
                                    }
                                }
                            }
                        }

                        Noor3DLaunchSplashOverlay(
                            visible = show3DLaunchSplash && globalConfig.effects3DSplashEnabled,
                            appNameAr = globalConfig.appNameAr.ifBlank { "نور" },
                            appNameEn = globalConfig.appNameEn.ifBlank { "NOOR" },
                            onFinished = { show3DLaunchSplash = false }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isSystem24HourFormat = DateFormat.is24HourFormat(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(NoorShortcutManager.EXTRA_SHORTCUT_ROUTE)
        if (!route.isNullOrBlank()) {
            incomingShortcutRoute = route
        }
    }
}
