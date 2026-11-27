package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.NextPrayerInfo
import com.example.domain.model.PrayerName
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalIs24HourFormat
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Luxury Emerald & Gold Liquid Glass Next-Prayer Hero Card.
 * Preserves the iconic deep-emerald Islamic geometric golden-ray hero background
 * in full vivid clarity, paired with a luminous Orbital Circular Progress Dial,
 * metallic gold countdown, frosted emerald-gold pills, and quick Tahajjud shortcut.
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
    val is24Hour = LocalIs24HourFormat.current
    val heroShape = RoundedCornerShape(30.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "hero_emerald_shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gold_glow_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .shadow(
                elevation = 16.dp,
                shape = heroShape,
                ambientColor = EmeraldDeep.copy(alpha = 0.55f),
                spotColor = EmeraldDeep.copy(alpha = 0.65f),
                clip = false
            )
            .clip(heroShape)
            .clickable(onClick = onOpenPrayers)
            .testTag("hero_next_prayer_card")
    ) {
        // 1. Original signature Royal Emerald & Golden Sunbeam Islamic Geometric Background (vivid, unobscured)
        Image(
            painter = painterResource(id = R.drawable.img_noor_hero_bg_1795289370813),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Very light edge-depth vignette + subtle animated golden aura so artwork stays crisp and text pops
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x14031711),
                        Color(0x24041A13),
                        Color(0x52020F0B)
                    )
                )
            )
            // Soft breathing golden light glow enhancing the upper-right sunbeam
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldBright.copy(alpha = 0.10f + shimmerPhase * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.12f),
                    radius = size.width * 0.58f
                )
            )
        }

        // 3. Prismatic Liquid Gold & Emerald outer rim border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GoldBright.copy(alpha = 0.92f),
                            Color.White.copy(alpha = 0.45f),
                            EmeraldSoft.copy(alpha = 0.45f),
                            GoldPrimary.copy(alpha = 0.55f)
                        )
                    ),
                    shape = heroShape
                )
        )

        // 4. Foreground Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Crystal Emerald-Gold "Next Prayer" badge + Sleek Tahajjud Shortcut Pill
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
                                listOf(Color(0x52082A1F), Color(0x360C3D2E))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(GoldBright.copy(alpha = 0.72f), Color.White.copy(alpha = 0.35f))
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
                                listOf(Color(0x47E2BE6E), Color(0x4D07241B))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(GoldBright.copy(alpha = 0.85f), Color.White.copy(alpha = 0.38f))
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

            // Main Centerpiece Row: Prayer Typography + Glowing Emerald-Gold Orbital Progress Circle
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
                                .background(Color(0x47062319))
                                .border(0.9.dp, GoldBright.copy(alpha = 0.60f), RoundedCornerShape(50))
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
                                text = L10n.formatPrayerTime(
                                    millis = nextInfo.nextPrayerMillis,
                                    timeZoneId = timezoneId,
                                    lang = lang,
                                    is24Hour = is24Hour,
                                    includeSeconds = true
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (iqamahDelayMinutes > 0) {
                            val iqTime = L10n.formatPrayerTime(
                                millis = nextInfo.nextPrayerMillis + iqamahDelayMinutes * 60_000L,
                                timeZoneId = timezoneId,
                                lang = lang,
                                is24Hour = is24Hour,
                                includeSeconds = false
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
                                color = EmeraldSoft,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing Emerald & Gold Orbital Progress Circle
                CelestialOrbitalProgressRing(
                    progress = nextInfo.progress,
                    prayer = nextInfo.nextPrayer,
                    accentColor = EmeraldSoft,
                    shimmerPhase = shimmerPhase,
                    lang = lang,
                    modifier = Modifier.size(124.dp)
                )
            }
        }
    }
}

/**
 * Luminous Emerald & Gold Circular Progress Dial with frosted glass medallion core,
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

            // 1. Frosted liquid emerald-glass circular medallion interior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x45184D3C),
                        Color(0x5208261C),
                        Color(0x6B03140E)
                    ),
                    center = Offset(cx - outerGlowRadius * 0.25f, cy - outerGlowRadius * 0.25f),
                    radius = outerGlowRadius * 1.35f
                ),
                radius = outerGlowRadius - strokePx * 0.55f,
                center = Offset(cx, cy)
            )

            // 2. Subtle inner Rub el Hizb golden star ornament inside the dial
            drawRubElHizb(
                cx = cx,
                cy = cy,
                radius = outerGlowRadius * 0.62f,
                color = GoldBright.copy(alpha = 0.16f + shimmerPhase * 0.06f),
                strokeWidthPx = 0.85.dp.toPx()
            )

            // 3. Twelve delicate golden watch-face ticks around inner rim
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
                    color = if (isCardinal) GoldBright.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.26f),
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

            // 5. Soft radiant golden bloom behind active progress arc
            val sweepAngle = 360f * clamped
            drawArc(
                color = GoldBright.copy(alpha = 0.26f + shimmerPhase * 0.10f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 6. Luminous liquid-gold & emerald active progress arc
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
                    colors = listOf(Color(0xE6FFF8DF), GoldBright.copy(alpha = 0.38f), Color.Transparent),
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
                    .background(GoldBright.copy(alpha = 0.20f))
                    .border(0.75.dp, GoldBright.copy(alpha = 0.62f), CircleShape),
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
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 1
            )
        }
    }
}
