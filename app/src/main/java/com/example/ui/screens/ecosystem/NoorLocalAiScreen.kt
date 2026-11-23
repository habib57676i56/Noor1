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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ai.AiVerificationLevel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.viewmodel.EcosystemViewModel

@Composable
fun NoorLocalAiScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val conversation by viewModel.aiConversation.collectAsStateWithLifecycle()
    val approvedKb by viewModel.approvedAiKnowledge.collectAsStateWithLifecycle()
    val indexStats by viewModel.aiIndexStats.collectAsStateWithLifecycle()

    var queryText by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "شلون أحفظ القرآن الكريم دون نسيان؟",
            "ما فضل قراءة القرآن الكريم؟",
            "ما هي شروط الصلاة وأركانها؟",
            "ما هي الأيام المستحب صيامها؟",
            "ما هي أحكام النون الساكنة في التجويد؟",
            "ما الفرق بين الإسلام والإيمان والإحسان؟"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "نور الذكي المحلي (Local RAG)", "NOOR Local AI (RAG)"),
            subtitle = L10n.pick(
                lang,
                "فهرس محلي دون إنترنت: ${approvedKb.size} مادة معتمدة · ${indexStats.uniqueTokenCount} كلمة مفهرسة · ${indexStats.indexedLibraryBooks} كتاب",
                "Offline Local RAG Index: ${approvedKb.size} entries · ${indexStats.uniqueTokenCount} tokens · ${indexStats.indexedLibraryBooks} books"
            ),
            onBack = onBack,
            trailing = {
                if (conversation.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAiConversation() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Chat")
                    }
                }
            }
        )

        // Input Box
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text(L10n.pick(lang, "اسأل عن القرآن، التجويد، الصلاة، الصيام، السيرة...", "Ask about Quran, Tajweed, Salah, Fasting...")) },
                    modifier = Modifier.weight(1f).testTag("local_ai_query_input"),
                    maxLines = 3
                )
                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            viewModel.askLocalNoorAi(queryText)
                            queryText = ""
                        }
                    },
                    modifier = Modifier.testTag("local_ai_send_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ask Local AI")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickPrompts) { prompt ->
                    LiquidGlassChip(
                        label = prompt,
                        selected = false,
                        onClick = { viewModel.askLocalNoorAi(prompt) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (conversation.isEmpty()) {
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = noorColors.goldAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "منهجية الأمانة العلمية في نور المحلي", "Islamic Accuracy & Transparency"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "• يعمل بالكامل على جهازك دون إنترنت ودون أي اشتراك أو API مدفوع.\n• يبحث في قاعدة المعرفة والمكتبة الإسلامية المحلية ويعرض المصدر لكل إجابة.\n• لا يختلق آيات قرآنية أو أحاديث نبوية أبداً؛ وإذا لم يجد مرجعاً موثّقاً يوضح ذلك بصراحة.\n• ليس مفتياً شرعياً: في المسائل الفقهية المعقدة يُرجى الرجوع لأهل العلم المعتمدين.",
                                "• Runs 100% locally offline with zero paid API keys or subscriptions.\n• Retrieves answers from verified local knowledge & library with explicit citations.\n• Never invents Quranic verses or Hadith; clearly states when uncertain.\n• Not a qualified Mufti — always consult qualified scholars for complex Fiqh rulings."
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(conversation) { item ->
                val (badgeLabel, badgeColor, badgeIcon) = when (item.verificationLevel) {
                    AiVerificationLevel.VERIFIED -> Triple(
                        L10n.pick(lang, "مرجع موثّق (${item.confidencePercent}%)", "Verified (${item.confidencePercent}%)"),
                        SemanticSuccess,
                        Icons.Rounded.Verified
                    )
                    AiVerificationLevel.PARTIAL_MATCH -> Triple(
                        L10n.pick(lang, "مطابقة جزئية (${item.confidencePercent}%)", "Partial Match (${item.confidencePercent}%)"),
                        SemanticWarning,
                        Icons.Rounded.Info
                    )
                    AiVerificationLevel.UNCERTAIN -> Triple(
                        L10n.pick(lang, "غير مؤكد — راجع العلماء", "Uncertain — Consult Scholars"),
                        SemanticWarning,
                        Icons.Rounded.Warning
                    )
                }

                LiquidGlassCard(strong = true, goldBorder = item.verificationLevel == AiVerificationLevel.VERIFIED) {
                    Text(
                        text = "س: ${item.query}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeColor)
                        Spacer(Modifier.width(6.dp))
                        Text(badgeLabel, style = MaterialTheme.typography.labelMedium, color = badgeColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, item.answerAr, item.answerEn),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (item.sources.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = L10n.pick(lang, "المصادر والمراجع:", "Sources & Citations:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        item.sources.forEach { src ->
                            Text("• ${src.title} — ${src.reference}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (item.scholarlyNote.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "ملاحظة فقهية/علمية: ${item.scholarlyNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldPrimary.copy(alpha = 0.10f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, item.disclaimerAr, item.disclaimerEn),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
