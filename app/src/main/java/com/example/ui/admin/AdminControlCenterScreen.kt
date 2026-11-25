package com.example.ui.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdminRole
import com.example.data.cloud.UserAccountDoc
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
    AdminNavDest(6, "القراء", "Reciters", Icons.Rounded.GraphicEq, "admin_nav_reciters"),
    AdminNavDest(2, "المحتوى", "CMS", Icons.Rounded.LibraryBooks, "admin_nav_cms"),
    AdminNavDest(3, "التحكم", "Control", Icons.Rounded.Tune, "admin_nav_settings"),
    AdminNavDest(4, "الأمان", "Security", Icons.Rounded.Security, "admin_nav_security")
)

/**
 * Dedicated Super Admin Liquid Glass Control Center Shell.
 * Strictly gated behind authenticated RBAC roles with floating translucent glass bars,
 * live visual/effects/module studio, Reciters Studio, Users Location & Verification, and Audit Security.
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
    var adsMessagingSubTab by rememberSaveable { mutableIntStateOf(0) }
    var messageRecipient by remember { mutableStateOf<UserAccountDoc?>(null) }
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
        AdminLiquidGlassBackground {
            if (!resolvedRole.isPrivileged) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                return@AdminLiquidGlassBackground
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
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
                            containerColor = Color(0x990B172B)
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
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            // Floating Liquid Glass Super Admin Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xCC122844),
                                                    Color(0xBA0C1C33),
                                                    Color(0xCC0D2336)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.42f),
                                                    AdminPalette.AccentCyan.copy(alpha = 0.65f),
                                                    AdminPalette.GoldLuxe.copy(alpha = 0.48f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onExitToApp,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .testTag("admin_exit_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Exit Admin",
                                            tint = AdminPalette.TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(AdminPalette.AccentCyan.copy(alpha = 0.30f), AdminPalette.GoldLuxe.copy(alpha = 0.25f))
                                                )
                                            )
                                            .border(1.dp, AdminPalette.GoldLuxe.copy(alpha = 0.55f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = AdminPalette.GoldLuxe,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = L10n.pick(lang, "سوبر أدمن · نور", "NOOR Super Admin"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AdminPalette.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = FirebaseAuth.getInstance().currentUser?.email ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AdminPalette.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            adsMessagingSubTab = 0
                                            selectedTab = 5
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedTab == 5) AdminPalette.AccentAmber.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.07f)
                                            )
                                            .testTag("admin_open_ads_messaging_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.Campaign,
                                            contentDescription = "Ads & Messaging",
                                            tint = if (selectedTab == 5) AdminPalette.AccentAmber else AdminPalette.AccentCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AdminStatusBadge(
                                        label = L10n.pick(lang, if (resolvedRole == AdminRole.SUPER_ADMIN) "المالك" else resolvedRole.labelAr, if (resolvedRole == AdminRole.SUPER_ADMIN) "Owner" else resolvedRole.labelEn),
                                        color = if (resolvedRole == AdminRole.SUPER_ADMIN) AdminPalette.GoldLuxe else AdminPalette.AccentCyan
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            if (!useRail) {
                                // Floating Liquid Glass Capsule Bottom Dock
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xD90E223A),
                                                        Color(0xD4081526),
                                                        Color(0xD90C2034)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.36f),
                                                        AdminPalette.AccentCyan.copy(alpha = 0.55f),
                                                        AdminPalette.GoldLuxe.copy(alpha = 0.32f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(26.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        adminDestinations.forEach { dest ->
                                            val isSelected = selectedTab == dest.index
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(
                                                        if (isSelected) {
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    AdminPalette.AccentCyan.copy(alpha = 0.28f),
                                                                    AdminPalette.AccentEmerald.copy(alpha = 0.14f)
                                                                )
                                                            )
                                                        } else {
                                                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                                        }
                                                    )
                                                    .clickable { selectedTab = dest.index }
                                                    .padding(vertical = 7.dp, horizontal = 2.dp)
                                                    .testTag(dest.tag),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = dest.icon,
                                                    contentDescription = dest.labelEn,
                                                    tint = if (isSelected) AdminPalette.AccentCyan else AdminPalette.TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = L10n.pick(lang, dest.labelAr, dest.labelEn),
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else AdminPalette.TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                0 -> AdminDashboardTab(
                                    adminViewModel = adminViewModel,
                                    onOpenAdsManager = {
                                        adsMessagingSubTab = 0
                                        selectedTab = 5
                                    },
                                    onOpenMessaging = {
                                        adsMessagingSubTab = 1
                                        selectedTab = 5
                                    }
                                )
                                1 -> AdminUsersTab(
                                    adminViewModel = adminViewModel,
                                    onRequireMfa = ::requireMfa,
                                    onMessageUser = { targetUser ->
                                        messageRecipient = targetUser
                                        adsMessagingSubTab = 1
                                        selectedTab = 5
                                    }
                                )
                                2 -> AdminCmsTab(
                                    adminViewModel = adminViewModel,
                                    onRequireMfa = ::requireMfa
                                )
                                3 -> AdminSettingsAndSupportTab(
                                    adminViewModel = adminViewModel,
                                    onRequireMfa = ::requireMfa,
                                    onOpenAdsAndMessaging = { sub ->
                                        adsMessagingSubTab = sub
                                        selectedTab = 5
                                    },
                                    onOpenRecitersStudio = {
                                        selectedTab = 6
                                    }
                                )
                                5 -> AdminAdsAndMessagingTab(
                                    adminViewModel = adminViewModel,
                                    onRequireMfa = ::requireMfa,
                                    initialSubTab = adsMessagingSubTab,
                                    preselectedRecipient = messageRecipient,
                                    onConsumedPreselectedRecipient = { messageRecipient = null }
                                )
                                6 -> AdminRecitersTab(
                                    adminViewModel = adminViewModel
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
}
