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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

@Composable
fun TajweedScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val progressList by viewModel.tajweedProgress.collectAsStateWithLifecycle()
    val progressMap = remember(progressList) { progressList.associateBy { it.ruleId } }
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }

    val rules = EcosystemContentCatalog.tajweedRules
    val completedCount = rules.count { progressMap[it.id]?.completed == true }
    val overallProgress = if (rules.isEmpty()) 0f else completedCount.toFloat() / rules.size

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "تجويد نور — أحكام التلاوة", "NOOR Tajweed"),
            subtitle = L10n.pick(lang, "قواعد التجويد الموثّقة بأمثلة قرآنية وصوتية وتمارين تفاعلية", "Verified Tajweed Rules with Audio Examples & Quizzes"),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Text(
                        text = L10n.pick(lang, "تقدمك في منهج التجويد: $completedCount / ${rules.size} دروس", "Tajweed Mastery: $completedCount / ${rules.size} lessons"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
                }
            }

            items(rules, key = { it.id }) { rule ->
                val saved = progressMap[rule.id]
                val chosenOption = selectedAnswers[rule.id]
                LiquidGlassCard(goldBorder = saved?.completed == true) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.categoryAr, style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                            Text(L10n.pick(lang, rule.titleAr, rule.titleEn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (saved?.completed == true) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Completed", tint = SemanticSuccess)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(L10n.pick(lang, rule.summaryAr, rule.summaryEn), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Text("الحروف: ${rule.lettersAr}", style = MaterialTheme.typography.labelMedium, color = noorColors.goldAccent, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(8.dp))
                    WarmIvoryParchmentCard {
                        Text(
                            text = rule.quranExampleUthmani,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            color = IvoryInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = rule.exampleRefAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("تمرين عملي: ${rule.practiceTipAr}", style = MaterialTheme.typography.labelSmall)

                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.playReferenceAyah(rule.exampleSurah, rule.exampleAyah) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(L10n.pick(lang, "استماع للمثال", "Listen Example"))
                        }
                        Button(
                            onClick = { viewModel.recordTajweedLessonResult(rule.id, completed = true, quizScorePercent = saved?.quizScorePercent ?: 100) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Text(if (saved?.completed == true) "مكتمل ✓" else "إتمام الدرس")
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("اختبار سريع: ${rule.quizQuestionAr}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    rule.quizOptionsAr.forEachIndexed { idx, opt ->
                        val isChosen = chosenOption == idx
                        val isRight = idx == rule.quizCorrectIndex
                        OutlinedButton(
                            onClick = {
                                selectedAnswers[rule.id] = idx
                                viewModel.recordTajweedLessonResult(rule.id, completed = isRight, quizScorePercent = if (isRight) 100 else 40)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = opt + when {
                                    isChosen && isRight -> " ✓ إجابة صحيحة"
                                    isChosen && !isRight -> " ✗ حاول مجدداً"
                                    else -> ""
                                },
                                color = when {
                                    isChosen && isRight -> SemanticSuccess
                                    isChosen && !isRight -> SemanticError
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
