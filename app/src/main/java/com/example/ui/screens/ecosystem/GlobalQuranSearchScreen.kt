package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AyahMultiTafsirBundle
import com.example.data.remote.NoorLiveOnlineService
import com.example.data.remote.QuranSearchHit
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

private val quickTopicsAr = listOf("الصبر", "الرزق", "الرحمة", "التوبة", "الوالدين", "الجنة", "الشفاء", "التوكل")
private val quickTopicsEn = listOf("patience", "mercy", "forgiveness", "parents", "paradise", "gratitude")
private val quickTopicsUz = listOf("sabr", "rahmat", "tavba", "jannat", "ota-ona", "shukr")

@Composable
fun GlobalQuranSearchScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    var searchEdition by remember(lang) {
        mutableStateOf(
            when (lang) {
                AppLanguage.ARABIC -> "quran-simple-clean"
                AppLanguage.ENGLISH -> "en.sahih"
                AppLanguage.UZBEK -> "uz.sodik"
            }
        )
    }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<QuranSearchHit>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    var loadingTafsir by remember { mutableStateOf(false) }
    var activeTafsir by remember { mutableStateOf<AyahMultiTafsirBundle?>(null) }
    var playingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopAudio() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        playingAudio = false
    }

    fun playAyahAudio(url: String) {
        if (url.isBlank()) return
        stopAudio()
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener {
                    playingAudio = true
                    it.start()
                }
                setOnCompletionListener {
                    playingAudio = false
                }
                setOnErrorListener { _, _, _ ->
                    playingAudio = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure {
            playingAudio = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    fun runSearch(term: String = query) {
        val clean = term.trim()
        if (clean.isEmpty()) return
        query = clean
        scope.launch {
            searching = true
            searchError = null
            hasSearched = true
            NoorLiveOnlineService.searchFullQuran(clean, searchEdition)
                .onSuccess { hits = it }
                .onFailure {
                    hits = emptyList()
                    searchError = L10n.pick(
                        lang,
                        "تعذّر البحث المباشر — تأكد من الاتصال بالإنترنت أو جرّب كلمة أخرى",
                        "Live search failed — check your internet connection or try another word",
                        "Jonli qidiruv amalga oshmadi — internetni tekshiring"
                    )
                }
            searching = false
        }
    }

    fun openMultiTafsir(surah: Int, ayah: Int) {
        scope.launch {
            loadingTafsir = true
            NoorLiveOnlineService.fetchAyahMultiTafsir(surah, ayah)
                .onSuccess { activeTafsir = it }
                .onFailure {
                    Toast.makeText(
                        context,
                        L10n.pick(lang, "تعذّر تحميل التفاسير الحية", "Unable to load live Tafsir", "Tafsir yuklanmadi"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            loadingTafsir = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("global_quran_search_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الباحث القرآني ومقارنة التفاسير", "Global Quran & Multi-Tafsir", "Qur'on qidiruvi va Tafsirlar"),
            subtitle = L10n.pick(
                lang,
                "ابحث في جميع آيات القرآن (٦٢٣٦ آية) مع التفسير الميسر والجلالين والأوزبكية والتلاوة",
                "Search all 6,236 verses live with Muyassar, Jalalayn, English, Uzbek & Audio",
                "Barcha 6236 oyat bo'yicha jonli qidiruv, tafsirlar, o'zbekcha tarjima va tilovat"
            ),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassChip(
                    label = "العربية",
                    selected = searchEdition == "quran-simple-clean",
                    onClick = { searchEdition = "quran-simple-clean" },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassChip(
                    label = "English",
                    selected = searchEdition == "en.sahih",
                    onClick = { searchEdition = "en.sahih" },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassChip(
                    label = "O'zbekcha",
                    selected = searchEdition == "uz.sodik",
                    onClick = { searchEdition = "uz.sodik" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            L10n.pick(lang, "اكتب كلمة أو آية للبحث...", "Search any word in 6,236 ayahs...", "Oyat yoki so'z kiriting..."),
                            maxLines = 1
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("global_quran_search_input")
                )
                Button(
                    onClick = { runSearch() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("global_quran_search_btn")
                ) {
                    Text(L10n.pick(lang, "بحث", "Go", "Izlash"), fontWeight = FontWeight.Bold)
                }
            }

            val chips = when (searchEdition) {
                "en.sahih" -> quickTopicsEn
                "uz.sodik" -> quickTopicsUz
                else -> quickTopicsAr
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips) { topic ->
                    LiquidGlassChip(
                        label = topic,
                        selected = query.equals(topic, ignoreCase = true),
                        onClick = { runSearch(topic) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (searching || loadingTafsir) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (searchError != null) {
                item {
                    LiquidGlassCard {
                        Text(text = searchError.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (hasSearched && !searching && hits.isEmpty()) {
                item {
                    LiquidGlassCard {
                        Text(
                            text = L10n.pick(lang, "لم يتم العثور على آيات مطابقة لهذه الكلمة.", "No matching verses found for this query.", "Bu so'z bo'yicha oyatlar topilmadi."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!hasSearched) {
                item {
                    LiquidGlassCard(goldBorder = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = L10n.pick(lang, "اختر موضوعاً أو ابحث عن أي كلمة قرآنية", "Pick a topic or search any Quranic word", "Mavzu tanlang yoki istalgan so'zni qidiring"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "اضغط على أي نتيجة لعرض التفسير الميسر، وتفسير الجلالين، وترجمة الشيخ محمد صادق الأوزبكية، وسماع التلاوة بصوت العفاسي.",
                                    "Tap any verse result to compare Muyassar, Jalalayn, English & Uzbek Shaykh Sodiq translations and listen live.",
                                    "Istalgan oyatni bosing — Tafsiri Muyassar, Jalolayn, Shayx Muhammad Sodiq tarjimasi va jonli tilovatni ochadi."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hits.isNotEmpty()) {
                item {
                    Text(
                        text = L10n.pick(lang, "النتائج: ${hits.size} آية", "Found ${hits.size} verses", "Topildi: ${hits.size} oyat"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
            }

            items(hits, key = { "${it.surahNumber}_${it.ayahNumberInSurah}_${it.globalAyahNumber}" }) { hit ->
                LiquidGlassCard(
                    onClick = { openMultiTafsir(hit.surahNumber, hit.ayahNumberInSurah) },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${hit.surahNameAr} · ${L10n.pick(lang, "آية", "Ayah", "Oyat")} ${hit.ayahNumberInSurah}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = L10n.pick(lang, "التفاسير والاستماع", "Tafsir & Audio", "Tafsir va Audio"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = hit.matchedText,
                            fontFamily = if (searchEdition == "quran-simple-clean") AmiriFontFamily else null,
                            fontSize = if (searchEdition == "quran-simple-clean") 20.sp else 15.sp,
                            lineHeight = if (searchEdition == "quran-simple-clean") 33.sp else 22.sp
                        )
                    }
                }
            }
        }
    }

    activeTafsir?.let { bundle ->
        AlertDialog(
            onDismissRequest = {
                stopAudio()
                activeTafsir = null
            },
            title = {
                Text(
                    text = "${bundle.surahNameAr} (${bundle.surahNumber}:${bundle.ayahNumber})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "﴿ ${bundle.uthmaniText} ﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 22.sp,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (bundle.audioUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                if (playingAudio) stopAudio() else playAyahAudio(bundle.audioUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(
                                if (playingAudio) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (playingAudio) L10n.pick(lang, "إيقاف التلاوة", "Stop Recitation", "To'xtatish")
                                else L10n.pick(lang, "استمع للآية (مشاري العفاسي)", "Listen to Ayah (Alafasy)", "Oyatni tinglash (Alafasy)")
                            )
                        }
                    }

                    TafsirBlock(L10n.pick(lang, "التفسير الميسر", "Tafsir Al-Muyassar (AR)", "Tafsiri Muyassar (AR)"), bundle.tafsirMuyassarAr)
                    TafsirBlock(L10n.pick(lang, "تفسير الجلالين", "Tafsir Al-Jalalayn (AR)", "Tafsiri Jalolayn (AR)"), bundle.tafsirJalalaynAr)
                    if (bundle.uzbekSodik.isNotBlank()) {
                        TafsirBlock("O'zbekcha (Shayx Muhammad Sodiq)", bundle.uzbekSodik)
                    }
                    if (bundle.englishSahih.isNotBlank()) {
                        TafsirBlock("English (Sahih International)", bundle.englishSahih)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Ayah", "${bundle.uthmaniText}\n[${bundle.surahNameAr}: ${bundle.ayahNumber}]"))
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
                                        "﴿ ${bundle.uthmaniText} ﴾\n${bundle.surahNameAr} (${bundle.ayahNumber})\n\n${bundle.tafsirMuyassarAr}"
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
                TextButton(onClick = {
                    val target = bundle.surahNumber
                    stopAudio()
                    activeTafsir = null
                    onOpenSurah(target)
                }) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(L10n.pick(lang, "فتح في المصحف", "Open in Mushaf", "Mushafda ochish"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    stopAudio()
                    activeTafsir = null
                }) {
                    Text(L10n.pick(lang, "إغلاق", "Close", "Yopish"))
                }
            }
        )
    }
}

@Composable
private fun TafsirBlock(title: String, body: String) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(noorColors.glassSurfaceStrong)
            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
        Text(text = body, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
    }
}
