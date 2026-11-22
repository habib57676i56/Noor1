package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.NoorPlatformConstants
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

private data class FaqItem(
    val qAr: String,
    val qEn: String,
    val aAr: String,
    val aEn: String
)

private val officialFaqs = listOf(
    FaqItem(
        qAr = "هل أذكار الصباح والمساء موثّقة بمصادرها؟",
        qEn = "Are Morning and Evening Adhkar verified with sources?",
        aAr = "نعم، كل ذكر في نور 3.0 مشكول بالكامل ومرفق بالمصدر الحديثي أو القرآني ودرجة الحديث وعدد التكرار الصحيح، وتعمل بالكامل دون إنترنت.",
        aEn = "Yes. Every Dhikr in NOOR 3.0 includes full Arabic diacritics, authentic Quran/Hadith source citations, scholarly grading, and verified repetition counts, and works 100% offline."
    ),
    FaqItem(
        qAr = "كيف أضبط طريقة حساب مواقيت الصلاة لمدينتي؟",
        qEn = "How do I adjust prayer calculation methods for my city?",
        aAr = "من تبويب الصلاة يمكنك تفعيل تحديد الموقع تلقائياً أو اختيار مدينتك يدوياً مع دعم أم القرى ورابطة العالم الإسلامي والهيئة المصرية وISNA وغيرها.",
        aEn = "Open the Prayers tab to auto-detect your location via GPS or choose your city manually, with Umm al-Qura, MWL, Egyptian Authority, ISNA, and manual minute offsets."
    ),
    FaqItem(
        qAr = "هل يمكن الاستماع لتلاوات الشيخ ياسر الدوسري دون إنترنت؟",
        qEn = "Can I listen to Sheikh Yasser Al-Dosari recitations offline?",
        aAr = "نعم، في المكتبة الصوتية يمكنك حفظ التلاوات المفضلة للاستماع إليها في وضع عدم الاتصال مع دعم التشغيل في الخلفية ومؤقت النوم.",
        aEn = "Yes, inside the Audio Library you can save recitations for offline listening with background playback, speed controls, and a sleep timer."
    ),
    FaqItem(
        qAr = "كيف يتم التعامل مع رسائل الدعم والاقتراحات؟",
        qEn = "How are support tickets and suggestions handled?",
        aAr = "تصل رسائلك مباشرةً إلى لوحة الدعم الرسمية ويمكنك متابعة رد الإدارة مباشرة من هذه الصفحة أو عبر البريد وواتساب وتيليجرام.",
        aEn = "All submitted tickets go directly to the official NOOR Support inbox where administrators respond, and you can track replies right here or contact us via Email, WhatsApp, or Telegram."
    )
)

/**
 * Part 5 — Complete Contact & Support Center.
 * Official Contact Details:
 * - Email: habiybullohy@gmail.com
 * - WhatsApp: +963 984 215 420
 * - Telegram: +963 984 215 420 (strictly phone number — NO invented username!)
 */
@Composable
fun ContactSupportScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val config by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val myTickets by viewModel.mySupportTickets.collectAsStateWithLifecycle()

    val email = config.supportEmail.ifBlank { NoorPlatformConstants.SUPPORT_EMAIL }
    val whatsApp = config.supportWhatsApp.ifBlank { NoorPlatformConstants.SUPPORT_WHATSAPP }
    val telegram = config.supportTelegram.ifBlank { NoorPlatformConstants.SUPPORT_TELEGRAM }

    val digitsWhatsApp = whatsApp.filter { it.isDigit() }
    val digitsTelegram = telegram.filter { it.isDigit() }

    var selectedType by remember { mutableStateOf("SUPPORT") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun openExternalIntent(uri: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            viewModel.showBannerMessage(L10n.pick(lang, "تعذّر فتح التطبيق الخارجي", "Could not open external app"))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contact_support_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NoorScreenHeader(
                title = L10n.pick(lang, "التواصل والدعم الفني", "Contact & Support Center"),
                subtitle = L10n.pick(lang, "نسعد بخدمتك واستقبال ملاحظاتك واقتراحاتك", "Direct official channels, FAQ & support tickets"),
                onBack = onBack
            )
        }

        // Direct Official Channels
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NoorSectionHeader(title = L10n.pick(lang, "قنوات التواصل الرسمية", "Official Direct Channels"))

                ContactChannelCard(
                    icon = Icons.Rounded.Email,
                    title = L10n.pick(lang, "البريد الإلكتروني للدعم", "Email Support"),
                    value = email,
                    accent = GoldPrimary,
                    testTag = "support_email_btn",
                    onClick = {
                        val mailUri = "mailto:$email?subject=${Uri.encode("NOOR Support Inquiry")}"
                        openExternalIntent(mailUri)
                    }
                )

                ContactChannelCard(
                    icon = Icons.Rounded.Chat,
                    title = L10n.pick(lang, "واتساب (WhatsApp)", "WhatsApp Contact"),
                    value = whatsApp,
                    accent = Color(0xFF25D366),
                    testTag = "support_whatsapp_btn",
                    onClick = { openExternalIntent("https://wa.me/$digitsWhatsApp") }
                )

                ContactChannelCard(
                    icon = Icons.AutoMirrored.Rounded.Send,
                    title = L10n.pick(lang, "تيليجرام (Telegram)", "Telegram Contact"),
                    value = telegram,
                    accent = Color(0xFF26A5E4),
                    testTag = "support_telegram_btn",
                    onClick = { openExternalIntent("https://t.me/+$digitsTelegram") }
                )
            }
        }

        // Submit Support / Bug / Feature / Feedback Form
        item {
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                goldBorder = true,
                contentPadding = PaddingValues(18.dp)
            ) {
                Text(
                    text = L10n.pick(lang, "أرسل رسالة مباشرة لفريق نور", "Send a Message to NOOR Support"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "دعم", "Support"),
                        selected = selectedType == "SUPPORT",
                        icon = Icons.Rounded.SupportAgent,
                        onClick = { selectedType = "SUPPORT" },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "مشكلة", "Bug"),
                        selected = selectedType == "BUG",
                        icon = Icons.Rounded.BugReport,
                        onClick = { selectedType = "BUG" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "اقتراح ميزة", "Feature"),
                        selected = selectedType == "FEATURE",
                        icon = Icons.Rounded.Lightbulb,
                        onClick = { selectedType = "FEATURE" },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "ملاحظة عامة", "Feedback"),
                        selected = selectedType == "FEEDBACK",
                        icon = Icons.Rounded.Feedback,
                        onClick = { selectedType = "FEEDBACK" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(L10n.pick(lang, "عنوان الرسالة", "Subject")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_subject_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(L10n.pick(lang, "التفاصيل", "Details")) },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_message_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.submitSupportTicket(selectedType, subject, message)
                        subject = ""
                        message = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("support_submit_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.pick(lang, "إرسال الرسالة", "Submit Message"), fontWeight = FontWeight.Bold)
                }
            }
        }

        // User's Previous Tickets & Live Admin Replies
        if (myTickets.isNotEmpty()) {
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "رسائلك وتذاكر الدعم (${myTickets.size})", "Your Support Tickets (${myTickets.size})"),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(myTickets, key = { it.id }) { ticket ->
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ticket.ticketType} · ${ticket.subject}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ticket.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (ticket.status == "RESOLVED") Color(0xFF4ADE80) else noorColors.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ticket.adminReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.18f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = L10n.pick(lang, "رد الدعم: ${ticket.adminReply}", "Support Reply: ${ticket.adminReply}"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Frequently Asked Questions
        item {
            NoorSectionHeader(
                title = L10n.pick(lang, "الأسئلة الشائعة (FAQ)", "Frequently Asked Questions"),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        items(officialFaqs) { faq ->
            var expanded by remember { mutableStateOf(false) }
            LiquidGlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, faq.qAr, faq.qEn),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = noorColors.goldAccent
                    )
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, faq.aAr, faq.aEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactChannelCard(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
