package com.example.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.db.CustomQuizQuestionEntity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

@Composable
fun NoorQuizScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    initialCategory: String = "ALL"
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val allQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val attempts by viewModel.quizAttempts.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDifficulty by remember { mutableStateOf("ALL") }

    var activeQuiz by remember { mutableStateOf<List<CustomQuizQuestionEntity>>(emptyList()) }
    var currentIdx by remember { mutableIntStateOf(0) }
    var selectedAnswerIdx by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    val categories = listOf(
        "ALL" to L10n.pick(lang, "الكل", "All"),
        "QURAN" to L10n.pick(lang, "القرآن", "Quran"),
        "HADITH" to L10n.pick(lang, "الحديث", "Hadith"),
        "TAJWEED" to L10n.pick(lang, "التجويد", "Tajweed"),
        "KNOWLEDGE" to L10n.pick(lang, "ثقافة إسلامية", "Knowledge"),
        "MEMORIZATION" to L10n.pick(lang, "الحفظ", "Memorization"),
        "KIDS" to L10n.pick(lang, "الأشبال", "Kids")
    )

    val difficulties = listOf(
        "ALL" to L10n.pick(lang, "جميع المستويات", "All Levels"),
        "EASY" to L10n.pick(lang, "سهل", "Easy"),
        "MEDIUM" to L10n.pick(lang, "متوسط", "Medium"),
        "HARD" to L10n.pick(lang, "متقدم", "Hard")
    )

    val matchingPool = remember(allQuestions, selectedCategory, selectedDifficulty) {
        allQuestions.filter { q ->
            val catOk = selectedCategory == "ALL" || q.category.equals(selectedCategory, ignoreCase = true)
            val diffOk = selectedDifficulty == "ALL" || q.difficulty.equals(selectedDifficulty, ignoreCase = true)
            catOk && diffOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مسابقات نور الإسلامية", "NOOR Islamic Quiz"),
            subtitle = L10n.pick(lang, "اختبارات تعليمية محلية بدون إنترنت مع تصحيح وشرح موثّق", "Offline Educational Assessments with Verified Explanations"),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeQuiz.isEmpty()) {
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Text(L10n.pick(lang, "اختر المجال:", "Select Category:"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { (k, label) ->
                                LiquidGlassChip(label, selectedCategory == k, { selectedCategory = k })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(L10n.pick(lang, "مستوى الصعوبة:", "Difficulty:"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(difficulties) { (k, label) ->
                                LiquidGlassChip(label, selectedDifficulty == k, { selectedDifficulty = k })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("الأسئلة المتاحة: ${matchingPool.size} سؤال", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (matchingPool.isNotEmpty()) {
                                    activeQuiz = matchingPool.shuffled().take(8)
                                    currentIdx = 0
                                    selectedAnswerIdx = null
                                    score = 0
                                    quizFinished = false
                                }
                            },
                            enabled = matchingPool.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().testTag("start_quiz_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Text(L10n.pick(lang, "ابدأ الاختبار العشوائي", "Start Randomized Quiz"))
                        }
                    }
                }

                if (attempts.isNotEmpty()) {
                    item { NoorSectionHeader(title = L10n.pick(lang, "سجل نتائجك السابقة", "Your Learning History")) }
                    items(attempts.take(10), key = { it.id }) { a ->
                        val pct = if (a.totalQuestions > 0) (a.score * 100) / a.totalQuestions else 0
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${a.category} (${a.difficulty}) — ${a.dateKey}", fontWeight = FontWeight.Bold)
                                Text("${a.score}/${a.totalQuestions} ($pct%)", color = noorColors.goldAccent, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else if (quizFinished) {
                item {
                    val pct = (score * 100) / activeQuiz.size.coerceAtLeast(1)
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Text(L10n.pick(lang, "نتيجة الاختبار", "Quiz Completed"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                        Spacer(Modifier.height(8.dp))
                        Text("أجبت بشكل صحيح على $score من أصل ${activeQuiz.size} سؤال ($pct%)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { activeQuiz = emptyList() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) { Text("العودة لقائمة المسابقات") }
                    }
                }
            } else {
                item {
                    val q = activeQuiz[currentIdx]
                    val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Text("سؤال ${currentIdx + 1} من ${activeQuiz.size} · ${q.category} · ${q.difficulty}", style = MaterialTheme.typography.labelMedium, color = noorColors.goldAccent)
                        Spacer(Modifier.height(8.dp))
                        Text(L10n.pick(lang, q.questionAr, q.questionEn.ifBlank { q.questionAr }), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        options.forEachIndexed { idx, optText ->
                            val answered = selectedAnswerIdx != null
                            val isChosen = selectedAnswerIdx == idx
                            val isCorrect = idx == q.correctIndex
                            OutlinedButton(
                                onClick = {
                                    if (!answered) {
                                        selectedAnswerIdx = idx
                                        if (isCorrect) score++
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = optText + when {
                                        answered && isCorrect -> " ✓"
                                        answered && isChosen && !isCorrect -> " ✗"
                                        else -> ""
                                    },
                                    color = when {
                                        answered && isCorrect -> SemanticSuccess
                                        answered && isChosen && !isCorrect -> SemanticError
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        if (selectedAnswerIdx != null) {
                            Spacer(Modifier.height(8.dp))
                            if (q.explanationAr.isNotBlank()) {
                                Text("الشرح: ${q.explanationAr}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                            }
                            if (q.sourceRef.isNotBlank()) {
                                Text("المصدر: ${q.sourceRef}", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (currentIdx < activeQuiz.lastIndex) {
                                        currentIdx++
                                        selectedAnswerIdx = null
                                    } else {
                                        viewModel.saveQuizAttempt(selectedCategory, selectedDifficulty, score, activeQuiz.size)
                                        quizFinished = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Text(if (currentIdx < activeQuiz.lastIndex) "السؤال التالي" else "عرض النتيجة النهائية")
                            }
                        }
                    }
                }
            }
        }
    }
}
