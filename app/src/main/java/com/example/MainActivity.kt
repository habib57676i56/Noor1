package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.cloud.FirebaseAuthHelper
import com.example.shortcuts.NoorShortcutManager
import com.example.ui.admin.AdminControlCenterScreen
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.navigation.NoorAppShell
import com.example.ui.screens.MaintenanceModeScreen
import com.example.ui.screens.NoorAuthGateScreen
import com.example.ui.theme.NoorTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.EcosystemViewModel
import com.example.viewmodel.NoorViewModel

class MainActivity : ComponentActivity() {

    private var incomingShortcutRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

            val pendingRoute = incomingShortcutRoute
            LaunchedEffect(pendingRoute) {
                if (!pendingRoute.isNullOrBlank()) {
                    viewModel.dispatchShortcutRoute(pendingRoute)
                    incomingShortcutRoute = null
                }
            }

            CompositionLocalProvider(
                LocalAppLanguage provides prefs.language,
                LocalLayoutDirection provides layoutDirection
            ) {
                NoorTheme(themeMode = prefs.themeMode) {
                    val user = currentUser
                    if (user == null && !allowOfflineLocalMode) {
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
                }
            }
        }
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
