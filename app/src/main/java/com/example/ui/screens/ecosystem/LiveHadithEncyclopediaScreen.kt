package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LiveHadithCategory
import com.example.data.remote.LiveHadithDetail
import com.example.data.remote.LiveHadithSummary
import com.example.data.remote.NoorLiveOnlineService
import com.example.domain.model.AppLanguage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.launch

@Composable
fun LiveHadithEncyclopediaScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    val apiLang = when (lang) {
        AppLanguage.ARABIC -> "ar"
        AppLanguage.ENGLISH -> "en"
        AppLanguage.UZBEK -> "uz"
    }

    var categories by remember { mutableStateOf<List<LiveHadithCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var hadiths by remember { mutableStateOf<List<LiveHadithSummary>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var filterQuery by remember { mutableStateOf("") }

    var loadingCategories by remember { mutableStateOf(true) }
    var loadingList by remember { mutableStateOf(false) }
    var loadingDetail by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedDetail by remember { mutableStateOf<LiveHadithDetail?>(null) }

    fun loadList(catId: String, page: Int, append: Boolean) {
        scope.launch {
            loadingList = true
            errorMsg = null
            NoorLiveOnlineService.fetchHadithsByCategory(apiLang, catId, page = page, perPage = 25)
                .onSuccess { list ->
                    hadiths = if (append) (hadiths + list).distinctBy { it.id } else list
                    currentPage = page
                }
                .onFailure {
                    errorMsg = L10n.pick(
                        lang,
                        "تعذّر تحميل الأحاديث من الموسوعة الحية — تأكد من الاتصال بالإنترنت",
                        "Unable to load live Hadiths — check internet connection",
                        "Hadislar yuklanmadi — internet aloqasini tekshiring"
                    )
                }
            loadingList = false
        }
    }

    fun loadCategories() {
        scope.launch {
            loadingCategories = true
            errorMsg = null
            NoorLiveOnlineService.fetchHadithCategories(apiLang)
                .onSuccess { cats ->
                    categories = cats
                    val first = cats.firstOrNull()
                    if (first != null) {
                        selectedCategoryId = first.id
                        loadList(first.id, 1, append = false)
                    }
                }
                .onFailure {
                    errorMsg = L10n.pick(
                        lang,
                        "تعذّر الاتصال بموسوعة الحديث النبوي الحية — تأكد من الإنترنت",
                        "Unable to connect to HadeethEnc live encyclopedia — check internet",
                        "Jonli Hadis ensiklopediyasiga ulanib bo'lmadi"
                    )
                }
            loadingCategories = false
        }
    }

    LaunchedEffect(apiLang) { loadCategories() }

    val visibleHadiths = remember(hadiths, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) hadiths else hadiths.filter { it.title.lowercase().contains(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_hadith_encyclopedia_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "موسوعة الحديث النبوي الحية", "Live Hadith Encyclopedia", "Jonli Hadislar ensiklopediyasi"),
            subtitle = L10n.pick(
                lang,
                "آلاف الأحاديث الصحيحة مع التخريج والدرجة والشرح والفوائد (عربي · إنجليزي · أوزبكي)",
                "Thousands of verified Hadiths with grading, explanation & benefits (AR/EN/UZ)",
                "Minglab sahih hadislar, ularning darajasi, sharhi va foydalari (O'zbekcha/AR/EN)"
            ),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (categories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        LiquidGlassChip(
                            label = "${cat.title} (${cat.hadithsCount})",
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = cat.id
                                filterQuery = ""
                                loadList(cat.id, 1, append = false)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = {
                    Text(L10n.pick(lang, "تصفية الأحاديث بالعنوان...", "Filter hadiths by title...", "Hadislarni sarlavha bo'yicha saralash..."))
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (loadingCategories || loadingDetail) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (errorMsg != null) {
                item {
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = errorMsg.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { loadCategories() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(L10n.pick(lang, "إعادة المحاولة", "Retry", "Qayta urinish"))
                            }
                        }
                    }
                }
            }

            items(visibleHadiths, key = { it.id }) { item ->
                LiquidGlassCard(
                    onClick = {
                        scope.launch {
                            loadingDetail = true
                            NoorLiveOnlineService.fetchHadithDetail(apiLang, item.id)
                                .onSuccess { selectedDetail = it }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        L10n.pick(lang, "تعذّر فتح شرح الحديث", "Unable to open Hadith detail", "Hadis tafsiloti ochilmadi"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            loadingDetail = false
                        }
                    },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = noorColors.goldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (hadiths.isNotEmpty() && selectedCategoryId != null) {
                item {
                    OutlinedButton(
                        onClick = {
                            if (!loadingList) {
                                loadList(selectedCategoryId!!, currentPage + 1, append = true)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (loadingList) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
                        } else {
                            Text(L10n.pick(lang, "تحميل المزيد من الأحاديث", "Load More Hadiths", "Ko'proq hadis yuklash"))
                        }
                    }
                }
            }
        }
    }

    selectedDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedDetail = null },
            title = {
                Text(text = detail.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (detail.grade.isNotBlank() || detail.attribution.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.14f))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = listOf(detail.grade, detail.attribution).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    if (detail.arabicText.isNotBlank()) {
                        Text(
                            text = detail.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 19.sp,
                            lineHeight = 32.sp
                        )
                    }

                    if (detail.hadithText.isNotBlank() && detail.hadithText != detail.arabicText) {
                        Text(
                            text = detail.hadithText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }

                    if (detail.explanation.isNotBlank()) {
                        Text(
                            text = L10n.pick(lang, "الشرح:", "Explanation:", "Sharh:"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Text(
                            text = detail.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }

                    if (detail.hints.isNotEmpty()) {
                        Text(
                            text = L10n.pick(lang, "الفوائد المستنبطة:", "Key Benefits:", "Asosiy foydalar:"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        detail.hints.forEachIndexed { idx, hint ->
                            Text(
                                text = "${idx + 1}. $hint",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Hadith", "${detail.hadithText}\n[${detail.grade} - ${detail.attribution}]"))
                                Toast.makeText(context, L10n.pick(lang, "تم النسخ", "Copied", "Nusxalandi"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L10n.pick(lang, "نسخ", "Copy", "Nusxa"))
                        }
                        OutlinedButton(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${detail.title}\n\n${detail.hadithText}\n(${detail.grade} · ${detail.attribution})"
                                    )
                                }
                                context.startActivity(Intent.createChooser(share, "NOOR"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L10n.pick(lang, "مشاركة", "Share", "Ulashish"))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDetail = null }) {
                    Text(L10n.pick(lang, "إغلاق", "Close", "Yopish"))
                }
            }
        )
    }
}
