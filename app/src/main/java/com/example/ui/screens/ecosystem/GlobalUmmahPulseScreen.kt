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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class UmmahCitySpec(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val lat: Double,
    val lon: Double,
    val tzId: String,
    val method: CalculationMethod
)

data class RareQiraatStream(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val riwayahAr: String,
    val riwayahEn: String,
    val cdnBaseUrl: String
)

private val ummahCities = listOf(
    UmmahCitySpec("makkah", "مكة المكرمة", "Makkah", "Makka", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    UmmahCitySpec("madinah", "المدينة المنورة", "Madinah", "Madina", 24.4672, 39.6112, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    UmmahCitySpec("quds", "القدس الشريف", "Al-Quds", "Quddus", 31.7767, 35.2345, "Asia/Jerusalem", CalculationMethod.MWL),
    UmmahCitySpec("tashkent", "طشقند", "Tashkent", "Toshkent", 41.2995, 69.2401, "Asia/Tashkent", CalculationMethod.MWL),
    UmmahCitySpec("samarkand", "سمرقند", "Samarkand", "Samarqand", 39.6542, 66.9597, "Asia/Samarkand", CalculationMethod.MWL),
    UmmahCitySpec("istanbul", "إسطنبول", "Istanbul", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul", CalculationMethod.TURKEY),
    UmmahCitySpec("cairo", "القاهرة", "Cairo", "Qohira", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPT),
    UmmahCitySpec("damascus", "دمشق", "Damascus", "Damashq", 33.5138, 36.2765, "Asia/Damascus", CalculationMethod.MWL),
    UmmahCitySpec("baghdad", "بغداد", "Baghdad", "Bag'dod", 33.3152, 44.3661, "Asia/Baghdad", CalculationMethod.MWL),
    UmmahCitySpec("rabat", "الرباط", "Rabat", "Rabot", 34.0209, -6.8416, "Africa/Casablanca", CalculationMethod.MWL),
    UmmahCitySpec("kl", "كوالالمبور", "Kuala Lumpur", "Kuala-Lumpur", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalculationMethod.MWL),
    UmmahCitySpec("jakarta", "جاكرتا", "Jakarta", "Jakarta", -6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.MWL)
)

private val rareQiraatVault = listOf(
    RareQiraatStream(
        "warsh_dosary",
        "إبراهيم الدوسري — رواية ورش عن نافع",
        "Ibrahim Al-Dosary — Warsh 'an Nafi'",
        "Ibrohim Ad-Dosariy — Varsh rivoyati",
        "رواية ورش عن نافع", "Warsh 'an Nafi'",
        "https://server10.mp3quran.net/ibrahim_dosri/Warsh/"
    ),
    RareQiraatStream(
        "abdulbasit_mujawwad",
        "عبد الباسط عبد الصمد — المصحف المجوّد النادر",
        "Abdul Basit — Classic Mujawwad",
        "Abdulbosit Abdussamad — Mujavvad",
        "تجويد نادر بالمقامات المصرية", "Classic Egyptian Mujawwad",
        "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/"
    ),
    RareQiraatStream(
        "minshawi_mujawwad",
        "محمد صديق المنشاوي — المجوّد الخاشع",
        "Al-Minshawi — Reverent Mujawwad",
        "Minshoviy — Xushu'li Mujavvad",
        "الصوت الباكي الخاشع", "Reverent Mujawwad",
        "https://server10.mp3quran.net/minsh/Almusshaf-Al-Mojawwad/"
    ),
    RareQiraatStream(
        "husary_muallim",
        "محمود خليل الحصري — المصحف المعلّم",
        "Al-Husary — Teacher Edition",
        "Husoriy — Muallim Mushafi",
        "مصحف تعليمي بضبط مخارج الحروف", "Tajweed Teacher Edition",
        "https://server13.mp3quran.net/husr/Almusshaf-Al-Mojawwad/"
    ),
    RareQiraatStream(
        "qaloon_husary",
        "محمود خليل الحصري — رواية قالون عن نافع",
        "Al-Husary — Qaloon 'an Nafi'",
        "Husoriy — Qolun rivoyati",
        "رواية قالون عن نافع", "Qaloon 'an Nafi'",
        "https://server13.mp3quran.net/husr/Rewayat-Qalon-A-n-Nafi/"
    ),
    RareQiraatStream(
        "douri_husary",
        "محمود خليل الحصري — رواية الدوري عن أبي عمرو",
        "Al-Husary — Ad-Douri 'an Abi Amr",
        "Husoriy — Ad-Duriy rivoyati",
        "رواية الدوري عن أبي عمرو", "Ad-Douri 'an Abi Amr",
        "https://server13.mp3quran.net/husr/Rewayat-Aldori-A-n-Abi-Amr/"
    )
)

@Composable
fun GlobalUmmahPulseScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Live Global Adhan Pulse, 1 = Rare Qira'at Audio Vault
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var selectedSurahNum by remember { mutableIntStateOf(1) }
    var playingStreamId by remember { mutableStateOf<String?>(null) }
    var bufferingStreamId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(5000L)
        }
    }

    fun stopStream() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        playingStreamId = null
        bufferingStreamId = null
    }

    fun playRareStream(stream: RareQiraatStream, surahNum: Int) {
        stopStream()
        val padded = surahNum.toString().padStart(3, '0')
        val url = "${stream.cdnBaseUrl}$padded.mp3"
        bufferingStreamId = stream.id
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
                    bufferingStreamId = null
                    playingStreamId = stream.id
                    it.start()
                }
                setOnCompletionListener {
                    playingStreamId = null
                }
                setOnErrorListener { _, _, _ ->
                    bufferingStreamId = null
                    playingStreamId = null
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure {
            bufferingStreamId = null
            playingStreamId = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopStream() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("global_ummah_pulse_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "نبض الأمة والقراءات النادرة", "Global Adhan & Rare Qira'at", "Ummat va Nodir Qiroatlar"),
            subtitle = L10n.pick(
                lang,
                "أين يُرفع الأذان الآن حول العالم · خزينة الروايات القرآنية النادرة (ورش، قالون، الدوري، المجوّد)",
                "Live Adhan pulse across 12 Islamic capitals + Rare Qira'at Vault (Warsh, Qaloon, Douri)",
                "12 islom shahrida jonli azon vaqti + Nodir qiroatlar (Varsh, Qolun, Duriy, Mujavvad)"
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
                label = L10n.pick(lang, "نبض الأذان العالمي", "Global Adhan Pulse", "Jahon azon vaqtlari"),
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "خزينة القراءات النادرة", "Rare Qira'at Vault", "Nodir qiroatlar"),
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Public, contentDescription = null, tint = GoldBright)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, "لا ينقطع الأذان عن الأرض لحظة واحدة", "The Adhan Never Stops Calling Across Earth", "Yer yuzida azon sadosi bir lahza ham to'xtamaydi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "متابعة حية لمواقيت الصلاة والتوقيت المحلي في ١٢ عاصمة ومدينة إسلامية الآن",
                                        "Live local clocks & astronomical prayer windows across 12 iconic Islamic cities",
                                        "12 ta islom shahrining jonli mahalliy vaqti va namoz vaqtlari"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(ummahCities, key = { it.id }) { city ->
                    val tz = TimeZone.getTimeZone(city.tzId)
                    val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMs }
                    val schedule = remember(city.id, cal.get(Calendar.DAY_OF_YEAR)) {
                        PrayerTimeCalculator.calculateForDate(
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH) + 1,
                            day = cal.get(Calendar.DAY_OF_MONTH),
                            latitude = city.lat,
                            longitude = city.lon,
                            timeZone = tz,
                            method = city.method,
                            madhhab = AsrMadhhab.SHAFI
                        )
                    }
                    val prayers = listOf(
                        PrayerName.FAJR to schedule.fajrMillis,
                        PrayerName.DHUHR to schedule.dhuhrMillis,
                        PrayerName.ASR to schedule.asrMillis,
                        PrayerName.MAGHRIB to schedule.maghribMillis,
                        PrayerName.ISHA to schedule.ishaMillis
                    )
                    val callingNow = prayers.firstOrNull { abs(nowMs - it.second) <= 12 * 60_000L }
                    val nextPrayer = prayers.firstOrNull { it.second > nowMs } ?: prayers.first()
                    val localTimeStr = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(Date(nowMs))
                    val nextTimeStr = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(Date(nextPrayer.second))

                    LiquidGlassCard(
                        goldBorder = callingNow != null,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = L10n.pick(lang, city.nameAr, city.nameEn, city.nameUz),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (callingNow != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(EmeraldPrimary.copy(alpha = 0.25f))
                                                .border(1.dp, GoldBright, RoundedCornerShape(50))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = L10n.pick(
                                                    lang,
                                                    "أذان ${L10n.prayerName(callingNow.first, lang)} الآن",
                                                    "${L10n.prayerName(callingNow.first, lang)} Adhan Now",
                                                    "${L10n.prayerName(callingNow.first, lang)} azoni hozir"
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldBright
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "الصلاة القادمة: ${L10n.prayerName(nextPrayer.first, lang)} ($nextTimeStr)",
                                        "Next: ${L10n.prayerName(nextPrayer.first, lang)} at $nextTimeStr",
                                        "Keyingi: ${L10n.prayerName(nextPrayer.first, lang)} — $nextTimeStr"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = localTimeStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = noorColors.goldAccent
                            )
                        }
                    }
                }
            } else {
                // Tab 1: Rare Qira'at & Vintage Maqamat Audio Vault
                item {
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = L10n.pick(lang, "اختر السورة للاستماع بالروايات النادرة (١–١١٤)", "Pick Surah for Rare Qira'at Streaming (1–114)", "Nodir qiroatlar uchun surani tanlang"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SurahCatalog.allSurahs, key = { it.number }) { s ->
                                    LiquidGlassChip(
                                        label = "${s.number}. ${s.nameAr}",
                                        selected = selectedSurahNum == s.number,
                                        onClick = {
                                            selectedSurahNum = s.number
                                            if (playingStreamId != null) {
                                                rareQiraatVault.firstOrNull { it.id == playingStreamId }?.let {
                                                    playRareStream(it, s.number)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                items(rareQiraatVault, key = { it.id }) { stream ->
                    val isPlaying = playingStreamId == stream.id
                    val isBuffering = bufferingStreamId == stream.id
                    LiquidGlassCard(
                        goldBorder = isPlaying,
                        onClick = {
                            if (isPlaying || isBuffering) stopStream() else playRareStream(stream, selectedSurahNum)
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) EmeraldPrimary else GoldPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isBuffering -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = GoldBright)
                                    isPlaying -> Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = Color.White)
                                    else -> Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = noorColors.goldAccent)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, stream.titleAr, stream.titleEn, stream.titleUz),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${L10n.pick(lang, stream.riwayahAr, stream.riwayahEn)} · ${SurahCatalog.findSurah(selectedSurahNum).nameAr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlaying) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isPlaying) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldBright)
                            }
                        }
                    }
                }
            }
        }
    }
}
