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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.model.QuranReciter
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
import com.example.viewmodel.EcosystemViewModel

@Composable
fun NoorDuaaScreen(onBack: () -> Unit) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var selectedCat by remember { mutableStateOf("ALL") }

    val cats = listOf(
        "ALL" to "الكل",
        "QURANIC" to "قرآنية",
        "PROPHETIC" to "نبوية",
        "FORGIVENESS" to "استغفار",
        "KNOWLEDGE" to "العلم والرزق",
        "PROTECTION" to "العافية",
        "TRAVEL" to "السفر"
    )
    val list = remember(selectedCat) {
        if (selectedCat == "ALL") EcosystemContentCatalog.verifiedDuas
        else EcosystemContentCatalog.verifiedDuas.filter { it.category == selectedCat }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "أدعية نور الموثّقة", "NOOR Authentic Duas"),
            subtitle = L10n.pick(lang, "أدعية صحيحة من القرآن الكريم والسنة النبوية بالتخريج", "Verified Quranic & Prophetic Supplications"),
            onBack = onBack
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats) { (k, label) -> LiquidGlassChip(label, selectedCat == k, { selectedCat = k }) }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { dua ->
                LiquidGlassCard {
                    Text(L10n.pick(lang, dua.titleAr, dua.titleEn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
                    Spacer(Modifier.height(8.dp))
                    WarmIvoryParchmentCard {
                        Text(dua.textAr, fontFamily = AmiriFontFamily, fontSize = 21.sp, lineHeight = 36.sp, color = IvoryInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(dua.translationEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("المصدر: ${dua.sourceRef}", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                }
            }
        }
    }
}

@Composable
fun NoorRadioScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "إذاعة نور القرآنية", "NOOR Quran Radio"),
            subtitle = L10n.pick(lang, "بث إذاعي مباشر للقرآن الكريم والتفسير (يتطلب اتصالاً بالإنترنت عند التشغيل)", "Live Quran & Tafseer Audio Stations"),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(EcosystemContentCatalog.quranRadioStations, key = { it.id }) { station ->
                val isPlayingThis = audioState.currentTrack?.id == station.id && (audioState.isPlaying || audioState.isBuffering)
                LiquidGlassCard(goldBorder = isPlayingThis) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Radio, contentDescription = null, tint = noorColors.goldAccent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(L10n.pick(lang, station.nameAr, station.nameEn), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(station.descriptionAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isPlayingThis) {
                            OutlinedButton(onClick = { viewModel.stopAudio() }) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop Radio")
                                Text("إيقاف")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.playRadioStation(station) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Radio")
                                Text("تشغيل")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoorTafseerExplorerScreen(onBack: () -> Unit) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val bundledSurahs = remember { listOf(1, 93, 94, 97, 103, 108, 109, 110, 111, 112, 113, 114) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    val verses = remember(selectedSurah) {
        SurahCatalog.getBundledSurahVerses(selectedSurah, QuranReciter.YASSER_AL_DOSARI).orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "تفسير نور الميسر", "NOOR Tafseer Explorer"),
            subtitle = L10n.pick(lang, "تفسير ميسر موثّق ومعاني الكلمات وأسباب النزول دون إنترنت", "Offline Concise Tafseer, Word Meanings & Asbab al-Nuzul"),
            onBack = onBack
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bundledSurahs) { s ->
                val meta = SurahCatalog.findSurah(s)
                LiquidGlassChip("${meta.number}. ${meta.nameAr}", selectedSurah == s, { selectedSurah = s })
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(verses, key = { it.globalNumber }) { v ->
                LiquidGlassCard {
                    Text("آية ${v.numberInSurah}", style = MaterialTheme.typography.labelMedium, color = noorColors.goldAccent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    WarmIvoryParchmentCard {
                        Text(v.textUthmani, fontFamily = AmiriFontFamily, fontSize = 21.sp, color = IvoryInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("التفسير الميسر: ${v.tafsirAr}", style = MaterialTheme.typography.bodyMedium)
                    if (v.wordMeaningsAr.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("معاني المفردات: ${v.wordMeaningsAr}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                    }
                    if (v.asbabAlNuzulAr.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("سبب النزول: ${v.asbabAlNuzulAr}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
