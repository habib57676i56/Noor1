package com.example.ui.screens.ecosystem

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.NoorLiveOnlineService
import com.example.data.remote.SkySightingWeather
import com.example.domain.astronomy.LunarAstronomyEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos

@Composable
fun CrescentObservatoryScreen(
    latitude: Double,
    longitude: Double,
    cityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    val lunar = remember { LunarAstronomyEngine.computeLunarPhase(System.currentTimeMillis()) }
    var skyWeather by remember { mutableStateOf<SkySightingWeather?>(null) }
    var loadingSky by remember { mutableStateOf(true) }

    fun loadSky() {
        scope.launch {
            loadingSky = true
            NoorLiveOnlineService.fetchSkySightingWeather(latitude, longitude)
                .onSuccess { skyWeather = it }
            loadingSky = false
        }
    }

    LaunchedEffect(latitude, longitude) { loadSky() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("crescent_observatory_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مرصد الهلال وأطوار القمر الحي", "Crescent & Lunar Observatory", "Hilol va Oy fazalari rasadxonasi"),
            subtitle = L10n.pick(
                lang,
                "عمر الهلال، نسبة الإضاءة، المنزلة القمرية، وتوقع رؤية الهلال الليلة في سماء $cityLabel",
                "Live moon age, illumination, Arabic lunar mansion & Hilal sighting clarity for $cityLabel",
                "$cityLabel osmonida hilol yoshi, yorug'ligi, oy manzili va ko'rinish prognozi"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live 3D Shaded Crescent / Moon Observatory Hero Card
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF10243C), Color(0xFF060E1C), Color(0xFF02060D))
                                    )
                                )
                                .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            LiveLunarMoonCanvas(
                                phaseFraction = lunar.phaseFraction,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = L10n.pick(lang, lunar.phaseNameAr, lunar.phaseNameEn, lunar.phaseNameUz),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = noorColors.goldAccent
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarStatBox(
                                label = L10n.pick(lang, "الإضاءة", "Illumination", "Yorug'lik"),
                                value = "${lunar.illuminationPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            LunarStatBox(
                                label = L10n.pick(lang, "عمر الهلال", "Moon Age", "Oy yoshi"),
                                value = String.format(Locale.US, "%.1f d", lunar.ageDays),
                                modifier = Modifier.weight(1f)
                            )
                            LunarStatBox(
                                label = L10n.pick(lang, "المنزلة القمرية", "Lunar Mansion", "Oy manzili"),
                                value = L10n.pick(lang, lunar.lunarMansionAr, lunar.lunarMansionEn, lunar.lunarMansionEn),
                                modifier = Modifier.weight(1.25f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarStatBox(
                                label = L10n.pick(lang, "البدر القادم بعد", "Next Full Moon", "Keyingi to'lin oy"),
                                value = String.format(Locale.US, "%.1f %s", lunar.daysToNextFullMoon, L10n.pick(lang, "يوم", "days", "kun")),
                                modifier = Modifier.weight(1f)
                            )
                            LunarStatBox(
                                label = L10n.pick(lang, "الهلال القادم بعد", "Next New Moon", "Keyingi yangi oy"),
                                value = String.format(Locale.US, "%.1f %s", lunar.daysToNextNewMoon, L10n.pick(lang, "يوم", "days", "kun")),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Live Atmospheric Hilal Sighting Forecast (Open-Meteo Real Cloud Cover & Sunset)
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "رصد صفاء السماء لرؤية الهلال الليلة", "Tonight's Live Hilal Sighting Clarity", "Bugun kechasi hilol ko'rinish prognozi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { loadSky() }, modifier = Modifier.size(40.dp)) {
                                if (loadingSky) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = noorColors.goldAccent)
                                }
                            }
                        }

                        val sky = skyWeather
                        if (sky != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LunarStatBox(
                                    label = L10n.pick(lang, "صفاء السماء", "Sky Clarity", "Osmon tiniqligi"),
                                    value = "${100 - sky.cloudCoverPercent}%",
                                    modifier = Modifier.weight(1f)
                                )
                                LunarStatBox(
                                    label = L10n.pick(lang, "الشروق", "Sunrise", "Quyosh chiqishi"),
                                    value = sky.sunriseLocal.ifBlank { "—" },
                                    modifier = Modifier.weight(1f)
                                )
                                LunarStatBox(
                                    label = L10n.pick(lang, "الغروب (وقت التحري)", "Sunset", "Quyosh botishi"),
                                    value = sky.sunsetLocal.ifBlank { "—" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        LunarAstronomyEngine.sightingVerdictAr(lunar.ageDays, sky.cloudCoverPercent),
                                        LunarAstronomyEngine.sightingVerdictEn(lunar.ageDays, sky.cloudCoverPercent),
                                        LunarAstronomyEngine.sightingVerdictUz(lunar.ageDays, sky.cloudCoverPercent)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Authentic Dua on Sighting the New Crescent
            item {
                LiquidGlassCard(goldBorder = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "دعاء رؤية الهلال المأثور", "Authentic Dua Upon Sighting the Crescent", "Yangi oyni ko'rganda o'qiladigan duo"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Text(
                            text = "اللَّهُ أَكْبَرُ، اللَّهُمَّ أَهِلَّهُ عَلَيْنَا بِالأَمْنِ وَالإِيمَانِ، وَالسَّلَامَةِ وَالإِسْلَامِ، وَالتَّوْفِيقِ لِمَا تُحِبُّ وَتَرْضَى، رَبُّنَا وَرَبُّكَ اللَّهُ",
                            fontFamily = AmiriFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 36.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = L10n.pick(lang, "رواه الترمذي والدارمي", "Narrated by At-Tirmidhi & Ad-Darimi", "Termiziy va Dorimiy rivoyati"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Sunnah Eclipse Prayer (صلاة الكسوف والخسوف) Quick Guide
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WbTwilight, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "صفة صلاة الكسوف والخسوف (سنة مؤكدة)", "How to Pray Eclipse Prayer (Kusuf & Khusuf)", "Quyosh va Oy tutilishi namozi tartibi"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Text(
                            text = L10n.pick(
                                lang,
                                "• ركعتان جهريتان في كل ركعة قيامان وقراءتان وركوعان وسجدتان.\n• يُستحب الإطالة في القراءة والركوع والسجود والإكثار من الذكر والاستغفار والصدقة والتكبير حتى ينجلي الكسوف أو الخسوف.",
                                "• Two audible rak'ahs, each containing TWO standings (Qiyam), TWO recitations, TWO bowings (Ruku'), and TWO prostrations (Sujud).\n• Engage abundantly in Dua, Istighfar, Takbir, and charity until the eclipse clears.",
                                "• Ikki rakat namoz bo'lib, har bir rakatda ikki qiyom, ikki qiroat, ikki ruku va ikki sajda qilinadi. Tutilish ochilguncha duo, istig'for va sadaqa ko'paytiriladi."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLunarMoonCanvas(
    phaseFraction: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.36f

        // Subtle starfield pinpoints around the lunar disc
        val stars = listOf(
            Offset(size.width * 0.16f, size.height * 0.20f),
            Offset(size.width * 0.84f, size.height * 0.24f),
            Offset(size.width * 0.22f, size.height * 0.80f),
            Offset(size.width * 0.78f, size.height * 0.76f),
            Offset(size.width * 0.50f, size.height * 0.10f)
        )
        stars.forEach { pt ->
            drawCircle(color = GoldBright.copy(alpha = 0.55f), radius = 1.6.dp.toPx(), center = pt)
        }

        // Soft lunar atmospheric halo
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x55F4DCA0), Color(0x18F4DCA0), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.48f
            ),
            radius = radius * 1.48f,
            center = Offset(cx, cy)
        )

        val moonCircle = Path().apply {
            addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
        }

        clipPath(moonCircle) {
            // Unlit dark lunar hemisphere (Earthshine)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1F2A3A), Color(0xFF0E1520)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Lit hemisphere computed mathematically from phaseFraction (0..1)
            val cosPhase = cos(2.0 * Math.PI * phaseFraction).toFloat()
            val litPath = Path()
            val steps = 48
            val isWaxing = phaseFraction < 0.5

            // Right limb when waxing, left limb when waning
            for (i in 0..steps) {
                val theta = -Math.PI / 2.0 + Math.PI * (i.toDouble() / steps.toDouble())
                val y = cy + radius * kotlin.math.sin(theta).toFloat()
                val limbX = cx + (if (isWaxing) 1f else -1f) * radius * cos(theta).toFloat()
                if (i == 0) litPath.moveTo(limbX, y) else litPath.lineTo(limbX, y)
            }
            // Terminator curve back
            for (i in steps downTo 0) {
                val theta = -Math.PI / 2.0 + Math.PI * (i.toDouble() / steps.toDouble())
                val y = cy + radius * kotlin.math.sin(theta).toFloat()
                val termX = cx + (if (isWaxing) 1f else -1f) * cosPhase * radius * cos(theta).toFloat()
                litPath.lineTo(termX, y)
            }
            litPath.close()

            drawPath(
                path = litPath,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFFCEB), Color(0xFFF3DEAC), Color(0xFFD6B97A)),
                    center = Offset(cx + radius * 0.15f, cy - radius * 0.15f),
                    radius = radius * 1.3f
                )
            )

            // Subtle realistic lunar maria craters
            drawCircle(color = Color(0x16000000), radius = radius * 0.16f, center = Offset(cx - radius * 0.18f, cy - radius * 0.22f))
            drawCircle(color = Color(0x14000000), radius = radius * 0.22f, center = Offset(cx + radius * 0.22f, cy + radius * 0.12f))
            drawCircle(color = Color(0x12000000), radius = radius * 0.12f, center = Offset(cx + radius * 0.05f, cy - radius * 0.35f))
        }
    }
}

@Composable
private fun LunarStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(noorColors.glassSurfaceStrong)
            .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = noorColors.goldAccent, maxLines = 1)
    }
}
