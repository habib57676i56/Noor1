package com.example.ui.screens.ecosystem

import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.quran.QuranMiraclesAndParablesEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

/**
 * Encyclopedia of Quranic Cosmic & Scientific Signs, Quranic Parables (الأمثال في القرآن),
 * and Complete Searchable Atlas of All 25 Prophets Named in the Holy Quran.
 */
@Composable
fun QuranMiraclesAndParablesScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Cosmic Signs, 1 = Parables, 2 = 25 Prophets
    var prophetSearch by remember { mutableStateOf("") }

    val mediaPlayer = remember { MediaPlayer() }
    var playingId by remember { mutableStateOf<String?>(null) }
    var bufferingId by remember { mutableStateOf<String?>(null) }

    fun stopAudio() {
        runCatching {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        }
        playingId = null
        bufferingId = null
    }

    fun playAyah(id: String, globalAyahNumber: Int) {
        if (playingId == id || bufferingId == id) {
            stopAudio()
            return
        }
        stopAudio()
        bufferingId = id
        val url = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyahNumber.mp3"
        runCatching {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener { mp ->
                bufferingId = null
                playingId = id
                mp.start()
            }
            mediaPlayer.setOnCompletionListener {
                playingId = null
                bufferingId = null
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                playingId = null
                bufferingId = null
                true
            }
            mediaPlayer.prepareAsync()
        }.onFailure {
            bufferingId = null
            playingId = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    val filteredProphets = remember(prophetSearch) {
        val q = prophetSearch.trim().lowercase()
        if (q.isEmpty()) QuranMiraclesAndParablesEngine.all25Prophets
        else QuranMiraclesAndParablesEngine.all25Prophets.filter { p ->
            p.nameAr.contains(q) || p.nameEn.lowercase().contains(q) || p.nameUz.lowercase().contains(q) || p.primarySurahAr.contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_miracles_parables_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الآيات الكونية والأمثال والأنبياء", "Cosmic Signs, Parables & Prophets", "Koinot oyatlari, Masallar va 25 Payg'ambar"),
            subtitle = L10n.pick(
                lang,
                "الإعجاز الكوني بالصوت الحي · أمثال القرآن المصوّرة · أطلس الأنبياء الـ٢٥",
                "Cosmic Signs with Live Audio · Quranic Parables · All 25 Prophets Atlas",
                "Jonli audio koinot oyatlari · Qur'on masallari · 25 Payg'ambar atlasi"
            ),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "الآيات الكونية", "Cosmic Signs", "Koinot oyatlari"),
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "أمثال القرآن", "Parables", "Masallar"),
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "الأنبياء ٢٥", "25 Prophets", "25 Payg'ambar"),
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                0 -> {
                    items(QuranMiraclesAndParablesEngine.cosmicSigns, key = { it.id }) { sign ->
                        val isPlaying = playingId == sign.id
                        val isBuffering = bufferingId == sign.id

                        LiquidGlassCard(goldBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Science, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = L10n.pick(lang, sign.titleAr, sign.titleEn, sign.titleUz),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = noorColors.goldAccent
                                            )
                                            Text(
                                                text = "${sign.domainAr} · سورة ${sign.surahNameAr} (${sign.ayahNumber})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { playAyah(sign.id, sign.globalAyahNumber) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            when {
                                                isBuffering -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldBright)
                                                isPlaying -> Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = GoldBright)
                                                else -> Icon(Icons.Rounded.PlayArrow, contentDescription = "Listen", tint = EmeraldPrimary)
                                            }
                                        }
                                        IconButton(
                                            onClick = { onOpenSurah(sign.surahNumber) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Rounded.MenuBook, contentDescription = "Mushaf", tint = noorColors.goldAccent)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldDeep.copy(alpha = 0.45f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = sign.ayahTextAr,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 21.sp,
                                        lineHeight = 36.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text(
                                    text = L10n.pick(lang, sign.reflectionAr, sign.reflectionEn, sign.reflectionUz),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                1 -> {
                    items(QuranMiraclesAndParablesEngine.parables, key = { it.id }) { p ->
                        LiquidGlassCard(goldBorder = true, onClick = { onOpenSurah(p.surahNumber) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = L10n.pick(lang, p.titleAr, p.titleEn, p.titleUz),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = noorColors.goldAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "سورة ${p.surahNameAr} (${p.ayahNumber})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                                Text(
                                    text = p.ayahTextAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 20.sp,
                                    lineHeight = 36.sp
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = L10n.pick(lang, p.imageryBreakdownAr, p.imageryBreakdownEn),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        OutlinedTextField(
                            value = prophetSearch,
                            onValueChange = { prophetSearch = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(L10n.pick(lang, "ابحث في الأنبياء الـ٢٥ المذكورين في القرآن", "Search all 25 Prophets in the Quran", "25 Payg'ambar ichidan qidirish")) }
                        )
                    }

                    items(filteredProphets, key = { it.order }) { prophet ->
                        LiquidGlassCard(onClick = { onOpenSurah(prophet.primarySurahNumber) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.22f))
                                        .border(1.dp, GoldBright, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${prophet.order}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldBright
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = L10n.pick(lang, prophet.nameAr, prophet.nameEn, prophet.nameUz),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = L10n.pick(lang, "ذُكر ${prophet.mentionCountInQuran} مرة", "${prophet.mentionCountInQuran}x", "${prophet.mentionCountInQuran} marta"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBright
                                        )
                                    }
                                    Text(
                                        text = L10n.pick(lang, prophet.hallmarkAr, prophet.hallmarkEn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = L10n.pick(lang, "السورة الأبرز: ${prophet.primarySurahAr} (اضغط للفتح بالمصحف ←)", "Key Surah: ${prophet.primarySurahAr} (Open →)", "Asosiy sura: ${prophet.primarySurahAr} →"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
