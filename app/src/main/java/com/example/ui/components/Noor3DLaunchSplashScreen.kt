package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinematic 3D Self-Assembling Launch Splash Overlay for NOOR.
 * Phase 1: Interlocking golden Rub el Hizb 8-point star, sculpted crescent, & orbital 3D halo assemble with perspective rotation.
 * Phase 2: App name ("نور" / "NOOR") rises beneath the 3D emblem.
 * Phase 3: Warm devotional footer ("لا تنسونا من صالح دعائكم") glows into view with a smooth golden loading bar.
 * Phase 4: Transitions smoothly in 3D scale/fade directly into the live application.
 */
@Composable
fun Noor3DLaunchSplashOverlay(
    visible: Boolean,
    appNameAr: String = "نور",
    appNameEn: String = "NOOR",
    onFinished: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val density = LocalDensity.current.density

    // Sequential 3D assembly choreography
    val emblemFormation = remember { Animatable(0f) }
    val spin3DY = remember { Animatable(-88f) }
    val tilt3DX = remember { Animatable(32f) }
    val coreRadiance = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(36f) }
    val duaAlpha = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash_3d_shimmer")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val pulseGlow by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        // Launch parallel staggered 3D assembly tracks
        val p1 = launch {
            emblemFormation.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
            )
        }
        val p2 = launch {
            spin3DY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val p3 = launch {
            tilt3DX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val p4 = launch {
            delay(420)
            coreRadiance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }
        val p5 = launch {
            delay(760)
            launch {
                titleAlpha.animateTo(1f, tween(580, easing = FastOutSlowInEasing))
            }
            launch {
                titleOffsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
            }
        }
        val p6 = launch {
            delay(1180)
            duaAlpha.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
        val p7 = launch {
            loadingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2550, easing = FastOutSlowInEasing)
            )
        }
        p1.join()
        p2.join()
        p3.join()
        p4.join()
        p5.join()
        p6.join()
        p7.join()
        delay(260)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(460, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 1.14f, animationSpec = tween(460, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0E4535),
                            Color(0xFF06251C),
                            Color(0xFF02110C)
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Allow user to smoothly enter immediately once the emblem is formed
                    if (emblemFormation.value > 0.7f) onFinished()
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("noor_3d_launch_splash"),
            contentAlignment = Alignment.Center
        ) {
            // Subtle celestial stardust & sacred geometric halo rings in background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.42f
                val maxR = size.minDimension * 0.56f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldBright.copy(alpha = 0.22f * coreRadiance.value * pulseGlow),
                            EmeraldPrimary.copy(alpha = 0.14f * coreRadiance.value),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = maxR
                    ),
                    radius = maxR,
                    center = Offset(cx, cy)
                )

                // Subtle expanding geometric concentric rings
                for (ring in 1..3) {
                    val r = maxR * (0.36f + ring * 0.19f) * (0.75f + 0.25f * emblemFormation.value)
                    drawCircle(
                        color = GoldPrimary.copy(alpha = (0.14f / ring) * emblemFormation.value),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                }

                // Orbiting golden light dust particles
                for (i in 0 until 18) {
                    val angleRad = ((i * 20f) + orbitAngle * (if (i % 2 == 0) 0.5f else -0.35f)) * (PI / 180.0)
                    val dist = maxR * (0.28f + (i % 5) * 0.13f) * emblemFormation.value
                    val px = cx + (cos(angleRad) * dist).toFloat()
                    val py = cy + (sin(angleRad) * dist).toFloat()
                    drawCircle(
                        color = GoldBright.copy(alpha = (0.25f + (i % 3) * 0.18f) * coreRadiance.value),
                        radius = (1.5f + (i % 3)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. Custom 3D Self-Assembling Islamic Emblem (Rub el Hizb + Sculpted Crescent + Noor Core)
                Box(
                    modifier = Modifier
                        .size(216.dp)
                        .graphicsLayer {
                            cameraDistance = 16f * density
                            rotationY = spin3DY.value
                            rotationX = tilt3DX.value
                            val s = 0.65f + 0.35f * emblemFormation.value
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val f = emblemFormation.value
                        val glow = coreRadiance.value
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension * 0.36f

                        // 3D Layered Depth Shadow Plate (gives physical extruded thickness)
                        for (depthStep in 4 downTo 1) {
                            val dy = depthStep * 2.4.dp.toPx()
                            val sqSize = r * 1.48f * f
                            val topLeft = Offset(cx - sqSize / 2f, cy - sqSize / 2f + dy)
                            val shadowColor = Color(0xFF031912).copy(alpha = 0.45f * f)
                            rotate(degrees = 45f * f, pivot = Offset(cx, cy + dy)) {
                                drawRoundRect(
                                    color = shadowColor,
                                    topLeft = topLeft,
                                    size = Size(sqSize, sqSize),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                )
                            }
                            rotate(degrees = 0f, pivot = Offset(cx, cy + dy)) {
                                drawRoundRect(
                                    color = shadowColor,
                                    topLeft = topLeft,
                                    size = Size(sqSize, sqSize),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                )
                            }
                        }

                        // Tilted 3D Orbital Halo Ring
                        rotate(degrees = orbitAngle * 0.6f, pivot = Offset(cx, cy)) {
                            drawOval(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        GoldBright.copy(alpha = 0.85f * glow),
                                        Color.Transparent,
                                        GoldPrimary.copy(alpha = 0.65f * glow),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy)
                                ),
                                topLeft = Offset(cx - r * 1.28f, cy - r * 0.66f),
                                size = Size(r * 2.56f, r * 1.32f),
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Square A (rotates from -45° into 0° during formation)
                        val sqSize = r * 1.44f * f
                        val sqTopLeft = Offset(cx - sqSize / 2f, cy - sqSize / 2f)
                        val emeraldBevelBrush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A7A5A),
                                EmeraldDeep,
                                Color(0xFF06271D)
                            ),
                            start = sqTopLeft,
                            end = Offset(cx + sqSize / 2f, cy + sqSize / 2f)
                        )
                        val goldBevelBrush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFF3C4),
                                GoldBright,
                                GoldPrimary,
                                Color(0xFF9C721A)
                            ),
                            start = sqTopLeft,
                            end = Offset(cx + sqSize / 2f, cy + sqSize / 2f)
                        )

                        rotate(degrees = -45f * (1f - f), pivot = Offset(cx, cy)) {
                            drawRoundRect(
                                brush = emeraldBevelBrush,
                                topLeft = sqTopLeft,
                                size = Size(sqSize, sqSize),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                            drawRoundRect(
                                brush = goldBevelBrush,
                                topLeft = sqTopLeft,
                                size = Size(sqSize, sqSize),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Square B (rotates from +90° into +45° locking the 8-pointed Rub el Hizb star)
                        rotate(degrees = 90f - 45f * f, pivot = Offset(cx, cy)) {
                            drawRoundRect(
                                brush = emeraldBevelBrush,
                                topLeft = sqTopLeft,
                                size = Size(sqSize, sqSize),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                            drawRoundRect(
                                brush = goldBevelBrush,
                                topLeft = sqTopLeft,
                                size = Size(sqSize, sqSize),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Inner 3D Glass Medallion Disc
                        val discR = r * 0.72f * f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF155E47),
                                    Color(0xFF072B20),
                                    Color(0xFF031711)
                                ),
                                center = Offset(cx - discR * 0.25f, cy - discR * 0.25f),
                                radius = (discR * 1.4f).coerceAtLeast(1f)
                            ),
                            radius = discR,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            brush = goldBevelBrush,
                            radius = discR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Sculpted 3D Golden Crescent (draws along arc as emblem assembles)
                        val crescentR = discR * 0.56f
                        if (crescentR > 2f) {
                            drawArc(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFF8D6), GoldBright, GoldPrimary),
                                    start = Offset(cx - crescentR, cy - crescentR),
                                    end = Offset(cx + crescentR, cy + crescentR)
                                ),
                                startAngle = 42f,
                                sweepAngle = 276f * f,
                                useCenter = false,
                                topLeft = Offset(cx - crescentR, cy - crescentR),
                                size = Size(crescentR * 2f, crescentR * 2f),
                                style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Radiant 4-Point Noor Star twinkling inside the Crescent
                        if (glow > 0.05f) {
                            val starCx = cx + discR * 0.24f
                            val starCy = cy - discR * 0.14f
                            val starR = discR * 0.28f * glow * pulseGlow
                            val starPath = Path().apply {
                                moveTo(starCx, starCy - starR)
                                quadraticTo(starCx, starCy, starCx + starR, starCy)
                                quadraticTo(starCx, starCy, starCx, starCy + starR)
                                quadraticTo(starCx, starCy, starCx - starR, starCy)
                                quadraticTo(starCx, starCy, starCx, starCy - starR)
                                close()
                            }
                            drawPath(
                                path = starPath,
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, GoldBright, GoldPrimary.copy(alpha = 0.2f)),
                                    center = Offset(starCx, starCy),
                                    radius = starR.coerceAtLeast(1f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. App Name Reveal Beneath the 3D Icon
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = titleAlpha.value
                            translationY = titleOffsetY.value
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = L10n.pick(lang, appNameAr.ifBlank { "نور" }, appNameEn.ifBlank { "NOOR" }, "NOOR"),
                        fontFamily = AmiriFontFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "القرآن الكريم · الصلاة · الذكر",
                            "Holy Quran · Prayer · Remembrance",
                            "Qur'oni Karim · Namoz · Zikr"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.80f),
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Dedicated Sincere Dua Footer: "لا تنسونا من صالح دعائكم"
                Column(
                    modifier = Modifier
                        .alpha(duaAlpha.value)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0C382B).copy(alpha = 0.62f))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldBright.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GoldBright)
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "لا تنسونا من صالح دعائكم",
                                "Remember us in your sincere prayers",
                                "Bizni ham solih duolaringizda unutmang"
                            ),
                            fontFamily = AmiriFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF4CC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("splash_dua_message")
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GoldBright)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))

                // 4. Sleek Golden Shimmer Loading Progress Bar
                Box(
                    modifier = Modifier
                        .width(172.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.value.coerceIn(0.05f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(EmeraldPrimary, GoldBright, Color(0xFFFFF6D1))
                                )
                            )
                    )
                }
            }
        }
    }
}
