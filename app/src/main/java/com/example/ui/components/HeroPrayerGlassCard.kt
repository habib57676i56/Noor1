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
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
 * Luxury Celestial Liquid Glass Next-Prayer Hero Card.
 * Replaces flat dark green with a living, luminous prayer-aware sapphire/turquoise/twilight sky,
 * gently shimmering crescent moon & stars (or golden sunbeams), glowing mosque silhouette,
 * metallic gold countdown typography, jewel Tahajjud shortcut pill, and glowing progress beam.
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Gentle living luminescence for celestial stars & aurora shimmer
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
            .height(228.dp)
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
                cy = h * 0.44f,
                radius = h * 0.44f,
                color = GoldBright.copy(alpha = 0.08f + shimmerPhase * 0.03f),
                strokeWidthPx = 1.dp.toPx()
            )
            drawRubElHizb(
                cx = w * 0.50f,
                cy = h * 0.44f,
                radius = h * 0.29f,
                color = Color.White.copy(alpha = 0.06f),
                strokeWidthPx = 0.8.dp.toPx()
            )

            // 3. Celestial sky details: glowing crescent & twinkling diamond stars OR soft golden sunbeams
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

            // 5. Crisp Liquid Glass diagonal crystal specular reflection across upper edge
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    start = Offset(w * 0.15f, 0f),
                    end = Offset(w * 0.62f, h * 0.52f)
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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Crystal Glass "Next Prayer" badge + Luminous Prayer Time pill
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
                        .padding(horizontal = 13.dp, vertical = 6.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x47F9E5AE), Color(0x24102F3E))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(GoldBright.copy(alpha = 0.85f), Color.White.copy(alpha = 0.35f))
                            ),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = L10n.formatTime12h(nextInfo.nextPrayerMillis, timezoneId, lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Middle Row: Prayer Name + Metallic Gold Countdown + Jewel Tahajjud Glass Shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.prayerName(nextInfo.nextPrayer, lang),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 33.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = L10n.formatCountdown(nextInfo.remainingMillis, lang),
                        style = TextStyle(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum",
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFF8E1),
                                    GoldBright,
                                    Color(0xFFE7BA5C)
                                )
                            )
                        )
                    )
                    if (iqamahDelayMinutes > 0) {
                        val iqTime = L10n.formatTime12h(
                            nextInfo.nextPrayerMillis + iqamahDelayMinutes * 60_000L,
                            timezoneId,
                            lang
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "الإقامة: $iqTime (+$iqamahDelayMinutes د)",
                                "Iqamah: $iqTime (+${iqamahDelayMinutes}m)",
                                "Iqomat: $iqTime (+$iqamahDelayMinutes daq)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Jewel Liquid-Glass Tahajjud & Morning/Evening Reminders Capsule
                val pillShape = RoundedCornerShape(22.dp)
                Column(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x42FFFFFF),
                                    Color(0x290E3548),
                                    Color(0x47071A2B)
                                )
                            )
                        )
                        .border(
                            1.15.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.72f),
                                    GoldBright.copy(alpha = 0.75f),
                                    palette.accentGlow.copy(alpha = 0.35f)
                                )
                            ),
                            pillShape
                        )
                        .clickable(onClick = onOpenTahajjudReminders)
                        .padding(horizontal = 13.dp, vertical = 10.dp)
                        .testTag("home_tahajjud_reminders_shortcut"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(GoldBright.copy(alpha = 0.38f), Color.Transparent)
                                )
                            )
                            .border(0.8.dp, GoldBright.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = L10n.pick(lang, "التهجد والأذكار", "Tahajjud Alerts", "Tahajjud"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x3DF9E5AE))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", tahajjudHour, tahajjudMinute),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFF6D6)
                        )
                    }
                }
            }

            // Bottom: Seamless Glowing Liquid-Gold Progress Beam with Luminous Orb Tip (Zero M3 gap)
            LiquidGoldProgressBeam(
                progress = nextInfo.progress,
                accentColor = palette.accentGlow,
                isRtl = isRtl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
        }
    }
}

@Composable
private fun LiquidGoldProgressBeam(
    progress: Float,
    accentColor: Color,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0.03f, 1f)
    Canvas(modifier = modifier) {
        val barHeight = 7.dp.toPx()
        val topY = (size.height - barHeight) / 2f
        val radius = CornerRadius(barHeight / 2f, barHeight / 2f)

        // Frosted glass track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(0f, topY),
            size = Size(size.width, barHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(0f, topY),
            size = Size(size.width, barHeight),
            cornerRadius = radius,
            style = Stroke(width = 0.6.dp.toPx())
        )

        val fillWidth = (size.width * clamped).coerceAtLeast(barHeight)
        val fillStartX = if (isRtl) size.width - fillWidth else 0f
        val tipX = if (isRtl) fillStartX else fillStartX + fillWidth

        // Glowing liquid-gold gradient beam
        val beamColors = if (isRtl) {
            listOf(Color(0xFFFFF8DF), GoldBright, accentColor)
        } else {
            listOf(accentColor, GoldBright, Color(0xFFFFF8DF))
        }
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = beamColors,
                startX = fillStartX,
                endX = fillStartX + fillWidth
            ),
            topLeft = Offset(fillStartX, topY),
            size = Size(fillWidth, barHeight),
            cornerRadius = radius
        )

        // Soft glowing pearl orb at the progress tip
        val centerY = size.height / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xCCFFF8DF), Color.Transparent),
                center = Offset(tipX, centerY),
                radius = 9.dp.toPx()
            ),
            radius = 9.dp.toPx(),
            center = Offset(tipX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(tipX, centerY)
        )
    }
}

private fun DrawScope.drawCelestialCrescentAndStars(w: Float, h: Float, shimmer: Float) {
    // Soft golden moonlight halo in upper middle-left
    val moonX = w * 0.44f
    val moonY = h * 0.26f
    val moonR = 16.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldBright.copy(alpha = 0.26f + shimmer * 0.10f), Color.Transparent),
            center = Offset(moonX, moonY),
            radius = moonR * 3.4f
        ),
        radius = moonR * 3.4f,
        center = Offset(moonX, moonY)
    )

    // Delicate 4-point twinkling celestial stars
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
    val mosqueHeight = h * 0.42f
    val brush = Brush.verticalGradient(
        colors = listOf(topColor, bottomColor),
        startY = base - mosqueHeight,
        endY = base
    )

    // Central grand dome
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

    // Side domes
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

    // Slender minarets
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
