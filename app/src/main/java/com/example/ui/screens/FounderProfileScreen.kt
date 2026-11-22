package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.NoorPlatformConstants
import com.example.ui.components.IslamicRosetteOrnament
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

/**
 * Part 4 — Dedicated Premium Founder Profile ("About the Founder").
 * Live-reflects Owner edits stored in GlobalAppConfigDoc while defaulting to
 * the exact official biography for Osama without invented qualifications.
 */
@Composable
fun FounderProfileScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val config by viewModel.globalAppConfig.collectAsStateWithLifecycle()

    val founderName = config.founderName.ifBlank { NoorPlatformConstants.FOUNDER_NAME }
    val founderTitle = L10n.pick(
        lang,
        config.founderTitleAr.ifBlank { NoorPlatformConstants.FOUNDER_TITLE_AR },
        config.founderTitleEn.ifBlank { NoorPlatformConstants.FOUNDER_TITLE_EN }
    )
    val founderProject = L10n.pick(
        lang,
        config.founderProjectAr.ifBlank { NoorPlatformConstants.FOUNDER_PROJECT_AR },
        config.founderProjectEn.ifBlank { NoorPlatformConstants.FOUNDER_PROJECT_EN }
    )
    val founderBio = L10n.pick(
        lang,
        config.founderBioAr.ifBlank { NoorPlatformConstants.FOUNDER_BIO_AR },
        config.founderBioEn.ifBlank { NoorPlatformConstants.FOUNDER_BIO_EN }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("founder_profile_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NoorScreenHeader(
                title = L10n.pick(lang, "عن المؤسس", "About the Founder"),
                subtitle = L10n.pick(lang, "رؤية ورسالة منصة نور", "The Vision & Mission Behind NOOR"),
                onBack = onBack
            )
        }

        // Hero Identity Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0A2E22), Color(0xFF061B14), Color(0xFF04100C))
                        )
                    )
                    .border(
                        1.4.dp,
                        Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.8f), GoldPrimary.copy(alpha = 0.3f))),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(EmeraldPrimary, EmeraldDeep)))
                            .border(2.dp, GoldBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = founderName.take(1).uppercase(),
                            fontFamily = AmiriFontFamily,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = founderName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = "Verified Founder",
                            tint = GoldBright,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = founderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GoldPrimary.copy(alpha = 0.16f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = founderProject,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Official Biography Card
        item {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                goldBorder = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.pick(lang, "السيرة والرؤية", "Biography & Vision"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    IslamicRosetteOrnament(size = 26.dp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = founderBio,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp
                )
            }
        }

        // Core Pillars Reflected in Official Mission
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = L10n.pick(lang, "ركائز منصة نور", "Core Commitments of NOOR"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FounderPillarCard(
                        icon = Icons.Rounded.MenuBook,
                        title = L10n.pick(lang, "الأصالة والموثوقية", "Authenticity"),
                        subtitle = L10n.pick(lang, "نصوص شرعية موثّقة بمصادرها", "Verified texts & authentic sources"),
                        modifier = Modifier.weight(1f)
                    )
                    FounderPillarCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = L10n.pick(lang, "البساطة والجمال", "Simplicity"),
                        subtitle = L10n.pick(lang, "تصميم هادئ يعين على العبادة", "Thoughtful, serene worship design"),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FounderPillarCard(
                        icon = Icons.Rounded.Lock,
                        title = L10n.pick(lang, "الخصوصية والأمان", "Privacy"),
                        subtitle = L10n.pick(lang, "حماية كاملة لبيانات المستخدم", "Respecting user trust & security"),
                        modifier = Modifier.weight(1f)
                    )
                    FounderPillarCard(
                        icon = Icons.Rounded.TrendingUp,
                        title = L10n.pick(lang, "التطوير المستمر", "Improvement"),
                        subtitle = L10n.pick(lang, "تحديثات متواصلة لخدمة المسلمين", "Continuous refinement worldwide"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Direct Contact CTA
        item {
            Button(
                onClick = onOpenSupport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .testTag("founder_contact_cta"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Rounded.SupportAgent, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = L10n.pick(lang, "التواصل والدعم الرسمي", "Official Contact & Support"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FounderPillarCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    LiquidGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
