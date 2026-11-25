package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.catalog.WorshipGuidesCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.OrnamentalSurahBadge
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

@Composable
fun WorshipGuidesScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Wudu, 1 = Umrah, 2 = Ruqyah

    val steps = when (selectedTab) {
        0 -> WorshipGuidesCatalog.wuduSteps
        1 -> WorshipGuidesCatalog.umrahSteps
        else -> WorshipGuidesCatalog.ruqyahItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("worship_guides_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "دليل الوضوء والعمرة والرقية", "Wudu, Umrah & Ruqyah Guide", "Tahorat, Umra va Ruqya qo'llanmasi"),
            subtitle = L10n.pick(
                lang,
                "خطوات مصوّرة موثّقة من القرآن والسنة تعمل بدون إنترنت",
                "Authentic offline step-by-step Islamic guides & Ruqyah",
                "Qur'on va Sunnat asosidagi oflayn bosqichma-bosqich qo'llanma"
            ),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "الوضوء", "Wudu", "Tahorat"),
                selected = selectedTab == 0,
                icon = Icons.Rounded.WaterDrop,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f).testTag("guide_tab_wudu")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "العمرة", "Umrah", "Umra"),
                selected = selectedTab == 1,
                icon = Icons.Rounded.Mosque,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f).testTag("guide_tab_umrah")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "الرقية", "Ruqyah", "Ruqya"),
                selected = selectedTab == 2,
                icon = Icons.Rounded.AutoAwesome,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f).testTag("guide_tab_ruqyah")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(steps, key = { "${selectedTab}_${it.stepNumber}" }) { step ->
                LiquidGlassCard(
                    goldBorder = step.arabicText.isNotBlank(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrnamentalSurahBadge(number = step.stepNumber, size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = L10n.pick(lang, step.titleAr, step.titleEn, step.titleUz),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, step.detailAr, step.detailEn, step.detailUz),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (step.arabicText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = step.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (noorColors.isDark) Color(0xFFFFF5D6) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GoldPrimary.copy(alpha = 0.12f))
                                .border(0.9.dp, GoldPrimary.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}
