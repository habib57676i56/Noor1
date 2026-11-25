package com.example.ui.screens.ecosystem

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.remote.CityLiveWeather
import com.example.data.remote.HaramainWeatherBundle
import com.example.data.remote.NoorLiveOnlineService
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class HaramainChannel(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val subtitleUz: String,
    val embedUrl: String,
    val externalYoutubeUrl: String,
    val duaAr: String,
    val duaRefAr: String
)

private val haramainChannels = listOf(
    HaramainChannel(
        id = "makkah",
        titleAr = "المسجد الحرام — مكة المكرمة",
        titleEn = "Al-Masjid Al-Haram — Makkah",
        titleUz = "Masjidul Harom — Makka",
        subtitleAr = "قناة القرآن الكريم · بث مباشر ٢٤ ساعة من الكعبة المشرفة",
        subtitleEn = "Saudi Quran TV · 24/7 Live from the Holy Kaaba",
        subtitleUz = "Saudiya Qur'on TV · Ka'badan 24/7 jonli efir",
        embedUrl = "https://www.youtube.com/embed/live_stream?channel=UCos52azQNBgW63_9uDJoPDA&autoplay=1&playsinline=1",
        externalYoutubeUrl = "https://www.youtube.com/@SaudiQuranTv/live",
        duaAr = "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
        duaRefAr = "اللَّهُمَّ زِدْ هَذَا الْبَيْتَ تَشْرِيفًا وَتَعْظِيمًا وَتَكْرِيمًا وَمَهَابَةً"
    ),
    HaramainChannel(
        id = "madinah",
        titleAr = "المسجد النبوي — المدينة المنورة",
        titleEn = "Al-Masjid An-Nabawi — Madinah",
        titleUz = "Masjidun Nabaviy — Madina",
        subtitleAr = "قناة السنة النبوية · بث مباشر ٢٤ ساعة من المسجد النبوي الشريف",
        subtitleEn = "Saudi Sunnah TV · 24/7 Live from the Prophet's Mosque",
        subtitleUz = "Saudiya Sunnat TV · Payg'ambar masjididan 24/7 jonli efir",
        embedUrl = "https://www.youtube.com/embed/live_stream?channel=UCROKYPep-UuODNwyipe6iiA&autoplay=1&playsinline=1",
        externalYoutubeUrl = "https://www.youtube.com/@SaudiSunnahTv/live",
        duaAr = "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ",
        duaRefAr = "مَا بَيْنَ بَيْتِي وَمِنْبَرِي رَوْضَةٌ مِنْ رِيَاضِ الْجَنَّةِ"
    )
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaramainLiveScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    var selectedIndex by remember { mutableIntStateOf(0) }
    val channel = haramainChannels[selectedIndex]

    var weather by remember { mutableStateOf<HaramainWeatherBundle?>(null) }
    var loadingWeather by remember { mutableStateOf(true) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var makkahClock by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Riyadh")
            }
            makkahClock = sdf.format(Date())
            delay(1000L)
        }
    }

    fun loadWeather() {
        scope.launch {
            loadingWeather = true
            weatherError = null
            NoorLiveOnlineService.fetchHaramainLiveWeather()
                .onSuccess { weather = it }
                .onFailure {
                    weatherError = L10n.pick(
                        lang,
                        "تعذر تحديث الطقس المباشر (تأكد من الاتصال بالإنترنت)",
                        "Unable to load live Haramain weather (check internet)",
                        "Jonli ob-havo yuklanmadi (internetni tekshiring)"
                    )
                }
            loadingWeather = false
        }
    }

    LaunchedEffect(Unit) { loadWeather() }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("haramain_live_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "البث المباشر للحرمين الشريفين", "Live Makkah & Madinah 24/7", "Makka va Madina 24/7 jonli efir"),
            subtitle = L10n.pick(
                lang,
                "شاهد الكعبة المشرفة والمسجد النبوي مباشرة مع الطقس وتوقيت الحرمين",
                "Watch Al-Masjid Al-Haram & An-Nabawi live with real-time weather & clock",
                "Ka'ba va Masjidun Nabaviyni jonli tomosha qiling, ob-havo va vaqtni kuzating"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Channel Switcher (Makkah / Madinah)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "مكة المكرمة (مباشر)", "Makkah Live", "Makka jonli"),
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                        modifier = Modifier.weight(1f).testTag("haramain_tab_makkah")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "المدينة المنورة (مباشر)", "Madinah Live", "Madina jonli"),
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                        modifier = Modifier.weight(1f).testTag("haramain_tab_madinah")
                    )
                }
            }

            // 2. Live Broadcast Cinema Card
            item {
                LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = L10n.pick(lang, channel.titleAr, channel.titleEn, channel.titleUz),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(lang, channel.subtitleAr, channel.subtitleEn, channel.subtitleUz),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x33E53935))
                                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.65f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = L10n.pick(lang, "مباشر LIVE", "LIVE", "JONLI"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF8A80)
                                )
                            }
                        }

                        // Embedded 16:9 Live Player
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        webChromeClient = WebChromeClient()
                                        webViewClient = WebViewClient()
                                        loadUrl(channel.embedUrl)
                                        webViewRef = this
                                    }
                                },
                                update = { view ->
                                    if (view.url != channel.embedUrl) {
                                        view.loadUrl(channel.embedUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "توقيت الحرمين: $makkahClock", "Haramain Time: $makkahClock", "Haramayn vaqti: $makkahClock"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                            }

                            Button(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(channel.externalYoutubeUrl)))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("haramain_open_youtube_btn")
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = L10n.pick(lang, "شاشة كاملة", "Full HD", "To'liq ekran"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Real-Time Live Weather Cards for Makkah & Madinah
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Mosque, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "الطقس الحي في مكة والمدينة الآن", "Live Weather in Makkah & Madinah", "Makka va Madinadagi jonli ob-havo"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { loadWeather() }, modifier = Modifier.size(40.dp)) {
                                if (loadingWeather) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Weather", tint = noorColors.goldAccent)
                                }
                            }
                        }

                        if (weatherError != null) {
                            Text(
                                text = weatherError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (weather != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HaramainWeatherTile(
                                    cityName = L10n.pick(lang, "مكة المكرمة", "Makkah", "Makka"),
                                    data = weather!!.makkah,
                                    modifier = Modifier.weight(1f)
                                )
                                HaramainWeatherTile(
                                    cityName = L10n.pick(lang, "المدينة المنورة", "Madinah", "Madina"),
                                    data = weather!!.madinah,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Companion Spiritual Dua & Talbiyah Card
            item {
                LiquidGlassCard(goldBorder = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = channel.duaAr,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 36.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = channel.duaRefAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = noorColors.goldAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HaramainWeatherTile(
    cityName: String,
    data: CityLiveWeather,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(EmeraldPrimary.copy(alpha = 0.14f), noorColors.glassSurfaceStrong)
                )
            )
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = cityName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format(Locale.US, "%.1f°C", data.temperatureC),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = L10n.pick(lang, data.conditionAr(), data.conditionEn(), data.conditionUz()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${data.humidityPercent}%", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Air, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${data.windKmh.toInt()} km/h", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
