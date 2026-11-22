package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdminRole
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth

private data class AdminNavDest(
    val index: Int,
    val labelAr: String,
    val labelEn: String,
    val icon: ImageVector,
    val tag: String
)

private val adminDestinations = listOf(
    AdminNavDest(0, "القيادة", "Overview", Icons.Rounded.Dashboard, "admin_nav_overview"),
    AdminNavDest(1, "المستخدمون", "Users", Icons.Rounded.Group, "admin_nav_users"),
    AdminNavDest(2, "المحتوى", "CMS", Icons.Rounded.LibraryBooks, "admin_nav_cms"),
    AdminNavDest(3, "الإعدادات", "Control", Icons.Rounded.Tune, "admin_nav_settings"),
    AdminNavDest(4, "الأمان", "Security", Icons.Rounded.Security, "admin_nav_security")
)

/**
 * Part 1 — Dedicated Super Admin Control Center Shell.
 * Strictly gated behind authenticated RBAC roles (never accessible to regular users)
 * with its own Enterprise Slate/Cyan design system completely distinct from NOOR user UI.
 */
@Composable
fun AdminControlCenterScreen(
    adminViewModel: AdminViewModel,
    resolvedRole: AdminRole,
    onExitToApp: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val snackbarHostState = remember { SnackbarHostState() }
    val feedback by adminViewModel.adminFeedback.collectAsStateWithLifecycle()
    val isUnlocked by adminViewModel.isPrivilegedUnlocked.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showMfaDialog by remember { mutableStateOf(false) }
    var pendingMfaAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(resolvedRole) {
        adminViewModel.syncAuthorizedRole(resolvedRole)
    }

    LaunchedEffect(feedback) {
        feedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            adminViewModel.clearFeedback()
        }
    }

    BackHandler(onBack = onExitToApp)

    fun requireMfa(action: () -> Unit) {
        if (isUnlocked) {
            action()
        } else {
            pendingMfaAction = action
            showMfaDialog = true
        }
    }

    NoorAdminTheme {
        if (!resolvedRole.isPrivileged) {
            // Hard client-side RBAC block (mirrored by zero-trust Firestore rules on the server)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AdminPalette.BgCanvas)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AdminPanelCard(accentBorder = AdminPalette.AccentRose) {
                    Text(
                        text = L10n.pick(lang, "وصول غير مصرح به", "Access Denied"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AdminPalette.AccentRose
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "لوحة التحكم مخصصة لمالك التطبيق والمشرفين المعتمدين فقط.",
                            "The Super Admin Control Center is strictly restricted to the verified Owner and authorized administrators."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdminPalette.TextSecondary
                    )
                    Spacer(modifier = Modifier.size(14.dp))
                    Button(onClick = onExitToApp, modifier = Modifier.fillMaxWidth()) {
                        Text(L10n.pick(lang, "العودة لتطبيق نور", "Return to NOOR"))
                    }
                }
            }
            return@NoorAdminTheme
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AdminPalette.BgCanvas)
                .testTag("super_admin_control_center")
        ) {
            val useRail = maxWidth >= 700.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = AdminPalette.SurfaceCard
                    ) {
                        adminDestinations.forEach { dest ->
                            NavigationRailItem(
                                selected = selectedTab == dest.index,
                                onClick = { selectedTab = dest.index },
                                icon = { Icon(dest.icon, contentDescription = dest.labelEn) },
                                label = { Text(L10n.pick(lang, dest.labelAr, dest.labelEn)) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = AdminPalette.BgCanvas,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AdminPalette.SurfaceCard)
                                .border(1.dp, AdminPalette.BorderSubtle)
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onExitToApp,
                                modifier = Modifier.testTag("admin_exit_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Exit Admin",
                                    tint = AdminPalette.TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = AdminPalette.AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, "مركز تحكم نور 3.0", "NOOR 3.0 Control Center"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AdminPalette.TextPrimary
                                )
                                Text(
                                    text = FirebaseAuth.getInstance().currentUser?.email ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminPalette.TextMuted
                                )
                            }
                            AdminStatusBadge(
                                label = L10n.pick(lang, resolvedRole.labelAr, resolvedRole.labelEn),
                                color = if (resolvedRole == AdminRole.SUPER_ADMIN) AdminPalette.AccentAmber else AdminPalette.AccentCyan
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = AdminPalette.SurfaceCard,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                adminDestinations.forEach { dest ->
                                    NavigationBarItem(
                                        selected = selectedTab == dest.index,
                                        onClick = { selectedTab = dest.index },
                                        icon = { Icon(dest.icon, contentDescription = dest.labelEn) },
                                        label = { Text(L10n.pick(lang, dest.labelAr, dest.labelEn), maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = AdminPalette.AccentCyan,
                                            selectedTextColor = AdminPalette.AccentCyan,
                                            indicatorColor = AdminPalette.AccentCyan.copy(alpha = 0.16f),
                                            unselectedIconColor = AdminPalette.TextMuted,
                                            unselectedTextColor = AdminPalette.TextMuted
                                        ),
                                        modifier = Modifier.testTag(dest.tag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> AdminDashboardTab(adminViewModel = adminViewModel)
                            1 -> AdminUsersTab(
                                adminViewModel = adminViewModel,
                                onRequireMfa = ::requireMfa
                            )
                            2 -> AdminCmsTab(
                                adminViewModel = adminViewModel,
                                onRequireMfa = ::requireMfa
                            )
                            3 -> AdminSettingsAndSupportTab(
                                adminViewModel = adminViewModel,
                                onRequireMfa = ::requireMfa
                            )
                            else -> AdminSecuritySystemTab(
                                adminViewModel = adminViewModel,
                                onRequestMfaVerification = { showMfaDialog = true },
                                onRequireMfa = ::requireMfa
                            )
                        }
                    }
                }
            }
        }

        // Secondary MFA / Owner Identity Confirmation Dialog
        if (showMfaDialog) {
            var emailInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = {
                    showMfaDialog = false
                    pendingMfaAction = null
                },
                title = { Text(L10n.pick(lang, "تأكيد الهوية الثنائي للمالك (MFA)", "Owner Second-Factor Confirmation")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "لإتمام العمليات الإدارية الحساسة، أعد كتابة بريدك الإلكتروني الموثّق المسجّل به حالياً.",
                                "To authorize sensitive administrative operations, re-enter your verified signed-in Google email address."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary
                        )
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            singleLine = true,
                            label = { Text(L10n.pick(lang, "البريد الإلكتروني الموثّق", "Verified Email")) },
                            modifier = Modifier.fillMaxWidth().testTag("admin_mfa_email_input")
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val passed = adminViewModel.verifySecondFactorEmailMatch(emailInput)
                        if (passed) {
                            showMfaDialog = false
                            val action = pendingMfaAction
                            pendingMfaAction = null
                            action?.invoke()
                        }
                    }) {
                        Text(L10n.pick(lang, "تحقق ومتابعة", "Verify & Continue"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showMfaDialog = false
                        pendingMfaAction = null
                    }) {
                        Text(L10n.pick(lang, "إلغاء", "Cancel"))
                    }
                }
            )
        }
    }
}
