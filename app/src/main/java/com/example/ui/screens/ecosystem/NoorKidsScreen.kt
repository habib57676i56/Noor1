package com.example.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

@Composable
fun NoorKidsScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenKidsQuiz: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val progress by viewModel.kidsProgress.collectAsStateWithLifecycle()
    val parentSettings by viewModel.kidsParentSettings.collectAsStateWithLifecycle()

    val completedIds = remember(progress) { progress.filter { it.completed }.map { it.itemId }.toSet() }
    val totalStars = remember(progress) { progress.sumOf { it.starsEarned } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showParentDialog by remember { mutableStateOf(false) }

    val shortSurahs = remember { listOf(1, 108, 109, 110, 111, 112, 113, 114, 103, 97) }
    val tabs = listOf(
        L10n.pick(lang, "قصار السور", "Short Surahs"),
        L10n.pick(lang, "أدعية الطفل", "Daily Duas"),
        L10n.pick(lang, "قصص هادفة", "Stories"),
        L10n.pick(lang, "أركان الإسلام", "Basics")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "نور للأطفال ⭐", "NOOR Kids ⭐"),
            subtitle = L10n.pick(lang, "النجوم المكتسبة: $totalStars ⭐ · الحد اليومي: ${parentSettings.dailyMinutesLimit} دقيقة", "Stars Earned: $totalStars ⭐ · Daily Limit: ${parentSettings.dailyMinutesLimit}m"),
            onBack = onBack,
            trailing = {
                IconButton(onClick = { showParentDialog = true }) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Parent Controls", tint = noorColors.goldAccent)
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { idx ->
                LiquidGlassChip(tabs[idx], selectedTab == idx, { selectedTab = idx })
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("مسابقة الأشبال الممتعة!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                            Text("اختبر معلوماتك الإسلامية واجمع النجوم", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = onOpenKidsQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) { Text("ابدأ المسابقة") }
                    }
                }
            }

            when (selectedTab) {
                0 -> items(shortSurahs) { sNum ->
                    val meta = SurahCatalog.findSurah(sNum)
                    val done = "kids_surah_$sNum" in completedIds
                    LiquidGlassCard(goldBorder = done) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("سورة ${meta.nameAr}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${meta.verseCount} آيات · ${meta.nameEn}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                            }
                            OutlinedButton(onClick = { onOpenSurah(sNum) }) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("اقرأ")
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.completeKidsActivity("kids_surah_$sNum", "SURAH", 5) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (done) SemanticSuccess else GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Rounded.Star, contentDescription = null)
                                Text(if (done) "حفظتها!" else "تم")
                            }
                        }
                    }
                }

                1 -> items(EcosystemContentCatalog.kidsDuas, key = { it.id }) { dua ->
                    val done = dua.id in completedIds
                    LiquidGlassCard(goldBorder = done) {
                        Text(L10n.pick(lang, dua.titleAr, dua.titleEn), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                        Spacer(Modifier.height(6.dp))
                        WarmIvoryParchmentCard {
                            Text(dua.duaAr, fontFamily = AmiriFontFamily, fontSize = 21.sp, color = IvoryInk)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${dua.occasionAr} · (${dua.sourceRef})", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { viewModel.completeKidsActivity(dua.id, "DUA", 3) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) { Text(if (done) "تعلمت الدعاء ⭐" else "حفظت هذا الدعاء (+3 ⭐)") }
                    }
                }

                2 -> items(EcosystemContentCatalog.kidsStories, key = { it.id }) { story ->
                    val done = story.id in completedIds
                    LiquidGlassCard(goldBorder = done) {
                        Text(L10n.pick(lang, story.titleAr, story.titleEn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                        Spacer(Modifier.height(6.dp))
                        Text(story.bodyAr, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("الدرس المستفاد: ${story.moralAr}", style = MaterialTheme.typography.labelMedium, color = SemanticSuccess, fontWeight = FontWeight.Bold)
                        Text("المصدر: ${story.sourceRef}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.completeKidsActivity(story.id, "STORY", 4) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) { Text(if (done) "قرأت القصة ⭐" else "أتممت القراءة (+4 ⭐)") }
                    }
                }

                3 -> items(EcosystemContentCatalog.kidsPillars, key = { it.id }) { pillar ->
                    val done = pillar.id in completedIds
                    LiquidGlassCard(goldBorder = done) {
                        Text(L10n.pick(lang, pillar.titleAr, pillar.titleEn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                        Spacer(Modifier.height(8.dp))
                        pillar.itemsAr.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.completeKidsActivity(pillar.id, "PILLAR", 5) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) { Text(if (done) "مكتمل ⭐" else "تعلمت الأركان (+5 ⭐)") }
                    }
                }
            }
        }
    }

    if (showParentDialog) {
        var minutes by remember { mutableIntStateOf(parentSettings.dailyMinutesLimit) }
        var quizOnly by remember { mutableStateOf(parentSettings.quizOnlyMode) }
        var audioGuide by remember { mutableStateOf(parentSettings.audioGuidanceEnabled) }
        AlertDialog(
            onDismissRequest = { showParentDialog = false },
            title = { Text("إعدادات إشراف الوالدين") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المدة اليومية المقترحة: $minutes دقيقة", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30, 45, 60).forEach { m ->
                            LiquidGlassChip("${m} د", minutes == m, { minutes = m })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تفعيل الإرشاد الصوتي")
                        Switch(checked = audioGuide, onCheckedChange = { audioGuide = it })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("وضع الاختبار والمراجعة المركّز")
                        Switch(checked = quizOnly, onCheckedChange = { quizOnly = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateKidsParentSettings(
                        parentSettings.copy(dailyMinutesLimit = minutes, quizOnlyMode = quizOnly, audioGuidanceEnabled = audioGuide)
                    )
                    showParentDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showParentDialog = false }) { Text("إلغاء") } }
        )
    }
}
