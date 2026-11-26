package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorPlatformConstants
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary

/**
 * Mandatory Telegram Update Gate.
 * Blocks older installed versions from running until the user updates from https://t.me/Noor_ui.
 */
@Composable
fun ForceUpdateGateScreen(
    config: GlobalAppConfigDoc,
    currentVersion: String = NoorPlatformConstants.APP_VERSION_NAME,
    canBypassAsAdmin: Boolean = false,
    onOpenAdminCenter: () -> Unit = {}
) {
    // Non-dismissible gate: back press cannot bypass a mandatory version block.
    BackHandler(enabled = true) {}

    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val safeTelegramUrl = AppVersionGate.resolveSafeUpdateUrl(config.updateChannelUrl)
    val requiredVersion = config.minSupportedVersion.ifBlank { currentVersion }

    val pulseTransition = rememberInfiniteTransition(label = "force_update_pulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    val messageText = L10n.pick(
        lang,
        config.updateMessageAr.ifBlank {
            "يتوفر إصدار جديد ومهم من تطبيق نور! لا يمكن تشغيل هذه النسخة القديمة — يرجى التحديث فوراً من قناة تيليجرام الرسمية للمتابعة."
        },
        config.updateMessageEn.ifBlank {
            "A mandatory new version of NOOR is available! This older version cannot run — please update immediately from our official Telegram channel to continue."
        }
    )

    IslamicGeometricBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("force_update_gate_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Royal Emblem
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(haloScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        GoldPrimary.copy(alpha = 0.34f),
                                        EmeraldPrimary.copy(alpha = 0.16f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF103F34), Color(0xFF061A15))
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(GoldBright, GoldPrimary, Color(0xFF38BDF8))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = L10n.pick(lang, "تحديث إجباري", "Mandatory Update"),
                            tint = GoldBright,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                LiquidGlassCard(
                    strong = true,
                    goldBorder = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, "حدّث التطبيق للمتابعة", "Update Required to Continue"),
                            fontFamily = AmiriFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright,
                            textAlign = TextAlign.Center
                        )

                        // Version comparison pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "نسختك: v$currentVersion", "Installed: v$currentVersion"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold
                            )
                            Text("•", color = GoldBright)
                            Text(
                                text = L10n.pick(lang, "المطلوب: v$requiredVersion+", "Required: v$requiredVersion+"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        // Channel Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B2A38).copy(alpha = 0.65f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = safeTelegramUrl,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Primary CTA -> Open Telegram Channel
                        Button(
                            onClick = {
                                val opened = runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeTelegramUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    true
                                }.getOrDefault(false)
                                if (!opened) {
                                    clipboard.setText(AnnotatedString(safeTelegramUrl))
                                    Toast.makeText(
                                        context,
                                        L10n.pick(
                                            lang,
                                            "تم نسخ رابط قناة التحديث: $safeTelegramUrl",
                                            "Update channel link copied: $safeTelegramUrl"
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("force_update_telegram_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = EmeraldDeep
                            )
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "تحديث التطبيق عبر تيليجرام", "Update Now via Telegram"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Copy Link Fallback
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(safeTelegramUrl))
                                Toast.makeText(
                                    context,
                                    L10n.pick(lang, "تم نسخ رابط القناة $safeTelegramUrl", "Copied $safeTelegramUrl"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("force_update_copy_link_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "نسخ رابط قناة تيليجرام", "Copy Telegram Channel Link"),
                                color = GoldBright,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (canBypassAsAdmin) {
                            TextButton(
                                onClick = onOpenAdminCenter,
                                modifier = Modifier.testTag("force_update_owner_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "دخول لوحة المالك (Super Admin)", "Owner Super Admin Access"),
                                    color = Color.White.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
