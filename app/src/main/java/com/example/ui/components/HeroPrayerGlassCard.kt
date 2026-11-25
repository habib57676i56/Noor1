package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.domain.model.NextPrayerInfo
import com.example.domain.model.PrayerName
import com.example.ui.i18n.L10n
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class CelestialHeroPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val accentGlow: Color,
    val mosqueTintTop: Color,
    val mosqueTintBottom: Color,
    val shadowTint: Color,
    val isNightSky: Boolean
)

private fun paletteForPrayer(prayer: PrayerName): CelestialHeroPalette = when (prayer) {
    PrayerName.FAJR, PrayerName.ISHA -> CelestialHeroPalette(
        skyTop = Color(0xFF071633),
        skyMid = Color(0xFF0C2E47),
        skyBottom = Color(0xFF13505E),
        auroraPrimary = Color(0x6B29E3C8),
        auroraSecondary = Color(0x52F9D98C),
        accentGlow = Color(0xFF6FF7E0),
        mosqueTintTop = Color(0x38FFE7A6),
        mosqueTintBottom = Color(0x16071633),
        shadowTint = Color(0xFF0B2E47),
        isNightSky = true
    )
    PrayerName.MAGHRIB -> CelestialHeroPalette(
        skyTop = Color(0xFF18143E),
        skyMid = Color(0xFF472559),
        skyBottom = Color(0xFF9C4E52),
        auroraPrimary = Color(0x66FFB36B),
        auroraSecondary = Color(0x54D87EFF),
        accentGlow = Color(0xFFFFD89B),
        mosqueTintTop = Color(0x42FFD996),
        mosqueTintBottom = Color(0x1E18143E),
        shadowTint = Color(0xFF472559),
        isNightSky = true
    )
    PrayerName.ASR -> CelestialHeroPalette(
        skyTop = Color(0xFF0B2C42),
        skyMid = Color(0xFF175563),
        skyBottom = Color(0xFF7B5C31),
        auroraPrimary = Color(0x66FFD27A),
        auroraSecondary = Color(0x4A38D9C0),
        accentGlow = Color(0xFFFFE39E),
        mosqueTintTop = Color(0x3DFFE5A0),
        mosqueTintBottom = Color(0x180B2C42),
        shadowTint = Color(0xFF175563),
        isNightSky = false
    )
    PrayerName.SUNRISE, PrayerName.DHUHR -> CelestialHeroPalette(
        skyTop = Color(0xFF093152),
        skyMid = Color(0xFF0F6472),
        skyBottom = Color(0xFF1B9688),
        auroraPrimary = Color(0x66FFE39A),
        auroraSecondary = Color(0x544AF2D8),
        accentGlow = Color(0xFF9AFCEB),
        mosqueTintTop = Color(0x3DFFF0BC),
        mosqueTintBottom = Color(0x18093152),
        shadowTint = Color(0xFF0F6472),
        isNightSky = false
    )
}

/**
 * Luxury Celestial Liquid Glass Next-Prayer Hero Card featuring a
 * luminous Orbital Circular Progress Halo (watch-face dial with glowing orb tip),
 * prayer-aware living sky, mosque silhouette, metallic gold countdown, and Tahajjud shortcut.
 */
@Composable
fun HeroPrayerGlassCard(
    nextInfo: NextPrayerInfo,
    timezoneId: String,
    iqamahDelayMinutes: Int,
    tahajjudHour: Int,
    tahajjudMinute: Int,
    lang: AppLanguage,
    onOpenPrayers: () -> Unit,
    onOpenTahajjudReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteForPrayer(nextInfo.nextPrayer)
    val heroShape = RoundedCornerShape(32.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "hero_celestial_shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .shadow(
                elevation = 18.dp,
                shape = heroShape,
                ambientColor = palette.shadowTint.copy(alpha = 0.55f),
                spotColor = palette.shadowTint.copy(alpha = 0.65f),
                clip = false
            )
            .clip(heroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.skyTop, palette.skyMid, palette.skyBottom),
                    start = Offset(0f, 0f),
                    end = Offset(1100f, 700f)
                )
            )
            .clickable(onClick = onOpenPrayers)
            .testTag("hero_next_prayer_card")
    ) {
        // Layered Celestial Liquid Glass Canvas Artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Living celestial aurora orbs
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.auroraPrimary.copy(alpha = 0.34f + shimmerPhase * 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.20f + shimmerPhase * 0.04f), h * 0.14f),
                    radius = w * 0.72f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.auroraSecondary.copy(alpha = 0.30f + (1f - shimmerPhase) * 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.84f, h * 0.24f),
                    radius = w * 0.66f
                )
            )

            // 2. Subtle Islamic Rub el Hizb luminous geometric watermark
            drawRubElHizb(
                cx = w * 0.50f,
                cy = h * 0.48f,
                radius = h * 0.44f,
                color = GoldBright.copy(alpha = 0.07f + shimmerPhase * 0.03f),
                strokeWidthPx = 1.dp.toPx()
            )

            // 3. Celestial sky details: twinkling stars OR soft golden sunbeams
            if (palette.isNightSky) {
                drawCelestialCrescentAndStars(w, h, shimmerPhase)
            } else {
                drawRadiantSunbeams(w, h, shimmerPhase)
            }

            // 4. Illuminated Royal Mosque Silhouette along the lower horizon
            drawIlluminatedMosqueHorizon(
                w = w,
                h = h,
                topColor = palette.mosqueTintTop,
                bottomColor = palette.mosqueTintBottom
            )

            // 5. Crisp Liquid Glass diagonal crystal specular reflection
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    start = Offset(w * 0.14f, 0f),
                    end = Offset(w * 0.60f, h * 0.48f)
                )
            )
        }

        // Prismatic Liquid Glass outer rim border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.78f),
                            GoldBright.copy(alpha = 0.90f),
                            palette.accentGlow.copy(alpha = 0.55f),
                            GoldPrimary.copy(alpha = 0.42f)
                        )
                    ),
                    shape = heroShape
                )
        )

        // Foreground Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Crystal Glass "Next Prayer" badge + Sleek Tahajjud & Adhkar Shortcut Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x36FFFFFF), Color(0x1CFFFFFF))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.65f), GoldBright.copy(alpha = 0.45f))
                            ),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = prayerIcon(nextInfo.nextPrayer),
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L10n.pick(lang, "الصلاة القادمة", "Next Prayer", "Keyingi namoz"),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFF5D6),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Compact Jewel Tahajjud & Adhkar Quick Shortcut Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x47F9E5AE), Color(0x280E3548))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(GoldBright.copy(alpha = 0.85f), Color.White.copy(alpha = 0.42f))
                            ),
                            RoundedCornerShape(50)
                        )
                        .clickable(onClick = onOpenTahajjudReminders)
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("home_tahajjud_reminders_shortcut")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = L10n.pick(lang, "التهجد", "Tahajjud", "Tahajjud"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", tahajjudHour, tahajjudMinute),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFF6D6)
                    )
                }
            }

            // Main Centerpiece Row: Prayer Typography + Glowing Celestial Orbital Progress Ring
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = L10n.prayerName(nextInfo.nextPrayer, lang),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 31.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = L10n.formatCountdown(nextInfo.remainingMillis, lang),
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum",
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFF8E1),
                                    GoldBright,
                                    Color(0xFFE7BA5C)
                                )
                            )
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Exact Prayer Time + Optional Iqamah Glass Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x33FFFFFF))
                                .border(0.85.dp, GoldBright.copy(alpha = 0.55f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = L10n.formatTime12h(nextInfo.nextPrayerMillis, timezoneId, lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (iqamahDelayMinutes > 0) {
                            val iqTime = L10n.formatTime12h(
                                nextInfo.nextPrayerMillis + iqamahDelayMinutes * 60_000L,
                                timezoneId,
                                lang
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "إقامة $iqTime",
                                    "Iqamah $iqTime",
                                    "Iqomat $iqTime"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentGlow,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing Celestial Orbital Progress Circle (replaces linear progress bar)
                CelestialOrbitalProgressRing(
                    progress = nextInfo.progress,
                    prayer = nextInfo.nextPrayer,
                    accentColor = palette.accentGlow,
                    shimmerPhase = shimmerPhase,
                    lang = lang,
                    modifier = Modifier.size(124.dp)
                )
            }
        }
    }
}

/**
 * Luminous Celestial Circular Progress Dial with frosted glass medallion core,
 * 12 subtle celestial dial markers, glowing gold-to-emerald sweep arc, and orbiting pearl-star tip.
 */
@Composable
private fun CelestialOrbitalProgressRing(
    progress: Float,
    prayer: PrayerName,
    accentColor: Color,
    shimmerPhase: Float,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0.03f, 1f)
    val percent = (clamped * 100f).roundToInt().coerceIn(1, 100)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val strokePx = 7.5.dp.toPx()
            val outerGlowRadius = size.minDimension / 2f - strokePx * 0.9f

            // 1. Frosted liquid glass circular medallion interior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x47FFFFFF),
                        Color(0x360C2E47),
                        Color(0x52061529)
                    ),
                    center = Offset(cx - outerGlowRadius * 0.25f, cy - outerGlowRadius * 0.25f),
                    radius = outerGlowRadius * 1.35f
                ),
                radius = outerGlowRadius - strokePx * 0.55f,
                center = Offset(cx, cy)
            )

            // 2. Subtle inner Rub el Hizb star ornament inside the dial
            drawRubElHizb(
                cx = cx,
                cy = cy,
                radius = outerGlowRadius * 0.62f,
                color = GoldBright.copy(alpha = 0.14f + shimmerPhase * 0.06f),
                strokeWidthPx = 0.85.dp.toPx()
            )

            // 3. Twelve delicate golden celestial watch-face ticks around inner rim
            val tickOuter = outerGlowRadius - strokePx * 0.72f
            val tickInner = tickOuter - 4.dp.toPx()
            for (i in 0 until 12) {
                val angleRad = (i * 30.0 - 90.0) * PI / 180.0
                val isCardinal = i % 3 == 0
                val start = Offset(
                    cx + ((if (isCardinal) tickInner - 1.5.dp.toPx() else tickInner) * cos(angleRad)).toFloat(),
                    cy + ((if (isCardinal) tickInner - 1.5.dp.toPx() else tickInner) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    cx + (tickOuter * cos(angleRad)).toFloat(),
                    cy + (tickOuter * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isCardinal) GoldBright.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.28f),
                    start = start,
                    end = end,
                    strokeWidth = if (isCardinal) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val arcTopLeft = Offset(cx - outerGlowRadius, cy - outerGlowRadius)
            val arcSize = Size(outerGlowRadius * 2f, outerGlowRadius * 2f)

            // 4. Translucent glass orbital ring track
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 5. Soft radiant outer bloom behind active progress arc
            val sweepAngle = 360f * clamped
            drawArc(
                color = GoldBright.copy(alpha = 0.24f + shimmerPhase * 0.10f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 6. Luminous liquid-gold & celestial turquoise active progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to accentColor,
                    0.35f to GoldBright,
                    0.75f to Color(0xFFFFF8DF),
                    1.0f to accentColor,
                    center = Offset(cx, cy)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 7. Orbiting luminous pearl-gold star orb at the exact tip of the arc
            val tipRad = (-90.0 + sweepAngle) * PI / 180.0
            val tipX = cx + (outerGlowRadius * cos(tipRad)).toFloat()
            val tipY = cy + (outerGlowRadius * sin(tipRad)).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xE6FFF8DF), GoldBright.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(tipX, tipY),
                    radius = 10.dp.toPx()
                ),
                radius = 10.dp.toPx(),
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(tipX, tipY)
            )
        }

        // Inner Dial Content: Glowing Prayer Icon + Percentage + Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(GoldBright.copy(alpha = 0.18f))
                    .border(0.7.dp, GoldBright.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = prayerIcon(prayer),
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "$percent%",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, GoldBright)
                    )
                )
            )
            Text(
                text = L10n.pick(lang, "تقدم الوقت", "Elapsed", "Vaqt"),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1
            )
        }
    }
}

private fun DrawScope.drawCelestialCrescentAndStars(w: Float, h: Float, shimmer: Float) {
    val moonX = w * 0.44f
    val moonY = h * 0.24f
    val moonR = 16.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldBright.copy(alpha = 0.24f + shimmer * 0.10f), Color.Transparent),
            center = Offset(moonX, moonY),
            radius = moonR * 3.4f
        ),
        radius = moonR * 3.4f,
        center = Offset(moonX, moonY)
    )

    val stars = listOf(
        Triple(0.32f, 0.20f, 3.8f),
        Triple(0.56f, 0.16f, 4.6f),
        Triple(0.64f, 0.30f, 3.0f),
        Triple(0.38f, 0.37f, 2.6f),
        Triple(0.24f, 0.32f, 3.2f),
        Triple(0.75f, 0.15f, 2.8f)
    )
    stars.forEachIndexed { idx, (fx, fy, sizeDp) ->
        val twinkle = if (idx % 2 == 0) 0.45f + shimmer * 0.50f else 0.90f - shimmer * 0.45f
        val sx = w * fx
        val sy = h * fy
        val r = sizeDp.dp.toPx()
        val starPath = Path().apply {
            moveTo(sx, sy - r)
            quadraticTo(sx, sy, sx + r, sy)
            quadraticTo(sx, sy, sx, sy + r)
            quadraticTo(sx, sy, sx - r, sy)
            quadraticTo(sx, sy, sx, sy - r)
            close()
        }
        drawPath(starPath, color = Color(0xFFFFF5D1).copy(alpha = twinkle))
    }
}

private fun DrawScope.drawRadiantSunbeams(w: Float, h: Float, shimmer: Float) {
    val origin = Offset(w * 0.48f, -h * 0.12f)
    val beamAngles = listOf(58.0, 74.0, 92.0, 110.0, 126.0)
    beamAngles.forEachIndexed { idx, deg ->
        val rad = deg * PI / 180.0
        val spread = 5.5 * PI / 180.0
        val length = w * 0.95f
        val p1 = Offset(
            origin.x + (length * cos(rad - spread)).toFloat(),
            origin.y + (length * sin(rad - spread)).toFloat()
        )
        val p2 = Offset(
            origin.x + (length * cos(rad + spread)).toFloat(),
            origin.y + (length * sin(rad + spread)).toFloat()
        )
        val rayPath = Path().apply {
            moveTo(origin.x, origin.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        val alpha = (0.045f + if (idx % 2 == 0) shimmer * 0.03f else (1f - shimmer) * 0.025f)
        drawPath(
            path = rayPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF4CC).copy(alpha = alpha), Color.Transparent)
            )
        )
    }
}

private fun DrawScope.drawIlluminatedMosqueHorizon(
    w: Float,
    h: Float,
    topColor: Color,
    bottomColor: Color
) {
    val base = h
    val mosqueHeight = h * 0.40f
    val brush = Brush.verticalGradient(
        colors = listOf(topColor, bottomColor),
        startY = base - mosqueHeight,
        endY = base
    )

    val cx = w * 0.50f
    val domeR = w * 0.115f
    val domePath = Path().apply {
        moveTo(cx - domeR, base)
        lineTo(cx - domeR, base - mosqueHeight * 0.34f)
        cubicTo(
            cx - domeR, base - mosqueHeight * 0.76f,
            cx - domeR * 0.24f, base - mosqueHeight * 0.90f,
            cx, base - mosqueHeight
        )
        cubicTo(
            cx + domeR * 0.24f, base - mosqueHeight * 0.90f,
            cx + domeR, base - mosqueHeight * 0.76f,
            cx + domeR, base - mosqueHeight * 0.34f
        )
        lineTo(cx + domeR, base)
        close()
    }
    drawPath(domePath, brush = brush)

    listOf(0.32f, 0.68f).forEach { frac ->
        val sx = w * frac
        val sr = w * 0.068f
        val sidePath = Path().apply {
            moveTo(sx - sr, base)
            lineTo(sx - sr, base - mosqueHeight * 0.26f)
            cubicTo(sx - sr, base - mosqueHeight * 0.56f, sx, base - mosqueHeight * 0.68f, sx, base - mosqueHeight * 0.70f)
            cubicTo(sx, base - mosqueHeight * 0.68f, sx + sr, base - mosqueHeight * 0.56f, sx + sr, base - mosqueHeight * 0.26f)
            lineTo(sx + sr, base)
            close()
        }
        drawPath(sidePath, brush = brush)
    }

    listOf(0.20f, 0.80f).forEach { frac ->
        val mx = w * frac
        val mw = w * 0.018f
        drawRect(
            brush = brush,
            topLeft = Offset(mx - mw / 2f, base - mosqueHeight * 0.80f),
            size = Size(mw, mosqueHeight * 0.80f)
        )
        val spire = Path().apply {
            moveTo(mx - mw * 0.9f, base - mosqueHeight * 0.78f)
            lineTo(mx, base - mosqueHeight * 0.99f)
            lineTo(mx + mw * 0.9f, base - mosqueHeight * 0.78f)
            close()
        }
        drawPath(spire, brush = brush)
    }
}
