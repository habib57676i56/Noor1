package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.AuthResult
import com.example.data.cloud.FirebaseAuthHelper
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

/**
 * Google Sign-In Gate via Jetpack Credential Manager (GetSignInWithGoogleOption).
 * Automatically attempts silent returning-user sign-in once, then offers explicit Sign-In button.
 */
@Composable
fun NoorAuthGateScreen(
    onSignedIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalAppLanguage.current

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isSigningIn = true
        when (FirebaseAuthHelper.attemptSilentAutoSignIn(context)) {
            is AuthResult.Success -> onSignedIn()
            else -> Unit
        }
        isSigningIn = false
    }

    IslamicGeometricBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("auth_gate_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xEE0B251B), Color(0xF505120D))))
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(EmeraldPrimary, EmeraldDeep)))
                        .border(2.dp, GoldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Mosque, contentDescription = null, tint = GoldBright, modifier = Modifier.size(40.dp))
                }

                Text(
                    text = L10n.pick(lang, "نور — NOOR 3.0", "NOOR 3.0"),
                    fontFamily = AmiriFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )

                Text(
                    text = L10n.pick(
                        lang,
                        "الرفيق الإسلامي المتكامل — أذكار موثّقة، قرآن كريم، مواقيت صلاة ومزامنة سحابية آمنة",
                        "The Complete Islamic Companion — Authentic Adhkar, Holy Quran, Prayer Times & Secure Cloud Sync"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (isSigningIn) return@Button
                        errorMessage = null
                        scope.launch {
                            isSigningIn = true
                            when (val res = FirebaseAuthHelper.signInWithGoogleInteractive(context)) {
                                is AuthResult.Success -> onSignedIn()
                                is AuthResult.Cancelled -> Unit
                                is AuthResult.NoCredential -> {
                                    errorMessage = L10n.pick(
                                        lang,
                                        "لم يتم العثور على حساب Google مضاف على الجهاز. أضف حساب Google من إعدادات الجهاز ثم أعد المحاولة.",
                                        "No Google account found on device. Add a Google account in device settings and try again."
                                    )
                                }
                                is AuthResult.Error -> {
                                    errorMessage = res.message
                                }
                            }
                            isSigningIn = false
                        }
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = EmeraldDeep
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = EmeraldDeep, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "تسجيل الدخول عبر Google", "Sign in with Google"),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Text(
                    text = L10n.pick(
                        lang,
                        "تسجيل دخول آمن وموثّق عبر Google لحماية بياناتك ومزامنة عباداتك",
                        "Verified Google Sign-In protects your profile, support tickets, and cloud sync"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Live Maintenance Mode Gate reflected immediately from GlobalAppConfigDoc.
 * Verified Super Admin / Owner can bypass maintenance mode to enter Admin Control Center.
 */
@Composable
fun MaintenanceModeScreen(
    config: GlobalAppConfigDoc,
    canBypassAsAdmin: Boolean,
    onOpenAdminCenter: () -> Unit
) {
    val lang = LocalAppLanguage.current
    IslamicGeometricBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("maintenance_mode_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF0081B14))
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.Build, contentDescription = null, tint = GoldBright, modifier = Modifier.size(52.dp))
                Text(
                    text = L10n.pick(lang, "صيانة مجدولة — نور", "Scheduled Maintenance — NOOR"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = L10n.pick(lang, config.maintenanceMessageAr, config.maintenanceMessageEn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                if (canBypassAsAdmin) {
                    OutlinedButton(
                        onClick = onOpenAdminCenter,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = GoldBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "دخول لوحة تحكم المالك", "Enter Super Admin Control Center"),
                            color = GoldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
