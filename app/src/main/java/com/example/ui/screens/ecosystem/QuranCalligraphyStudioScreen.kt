package com.example.ui.screens.ecosystem

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.quran.IlluminationTheme
import com.example.domain.quran.QuranCalligraphyEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class GoldenSparkParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float, // 1f -> 0f
    val radius: Float,
    val isRipple: Boolean = false
)

/**
 * Interactive 3D Golden Islamic Calligraphy & Mamluk/Ottoman Illumination Studio.
 * Features touch-reactive gold-leaf spark particles, rotating 12-point Islamic mandala,
 * 5 illumination themes, live Alafasy Ayah audio with reactive light halo, and HD PNG export.
 */
@Composable
fun QuranCalligraphyStudioScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current

    var selectedPiece by remember { mutableStateOf(QuranCalligraphyEngine.masterpieces.first()) }
    var selectedTheme by remember { mutableStateOf(QuranCalligraphyEngine.themes.first()) }
    var customText by remember { mutableStateOf("") }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioStatus by remember { mutableStateOf<String?>(null) }

    val particles = remember { mutableStateListOf<GoldenSparkParticle>() }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopAudio() {
        runCatching {
            mediaPlayerRef.value?.stop()
            mediaPlayerRef.value?.release()
        }
        mediaPlayerRef.value = null
        isPlayingAudio = false
    }

    fun playAyahAudio(globalAyah: Int) {
        stopAudio()
        isPlayingAudio = true
        audioStatus = L10n.pick(lang, "جارٍ تشغيل التلاوة المضيئة...", "Streaming illuminated recitation...", "Tilovat yuklanmoqda...")
        val url = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyah.mp3"
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener {
                    it.start()
                    audioStatus = L10n.pick(lang, "تلاوة حيّة بصوت العفاسي", "Live Recitation · Alafasy", "Jonli tilovat · Alafasy")
                }
                setOnCompletionListener {
                    isPlayingAudio = false
                    audioStatus = null
                }
                setOnErrorListener { _, _, _ ->
                    isPlayingAudio = false
                    audioStatus = L10n.pick(lang, "تعذّر الاتصال بالصوت حالياً", "Unable to stream audio right now", "Audio ulanmadi")
                    true
                }
                prepareAsync()
            }
            mediaPlayerRef.value = mp
        } catch (_: Throwable) {
            isPlayingAudio = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    // Smooth 60fps particle physics ticker
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.isNotEmpty()) {
                val iter = particles.listIterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    val nextLife = p.life - if (p.isRipple) 0.032f else 0.025f
                    if (nextLife <= 0f) {
                        iter.remove()
                    } else {
                        iter.set(
                            p.copy(
                                x = p.x + p.vx,
                                y = p.y + p.vy,
                                vy = if (p.isRipple) 0f else p.vy - 0.14f,
                                life = nextLife,
                                radius = if (p.isRipple) p.radius + 3.4f else p.radius
                            )
                        )
                    }
                }
            }
            delay(16L)
        }
    }

    fun emitBurst(pos: Offset, withRipple: Boolean) {
        if (withRipple) {
            particles += GoldenSparkParticle(pos.x, pos.y, 0f, 0f, 1f, 12f, isRipple = true)
        }
        val count = if (withRipple) 14 else 4
        repeat(count) {
            if (particles.size < 140) {
                val angle = Random.nextDouble() * 2.0 * PI
                val speed = 1.2f + Random.nextFloat() * 3.8f
                particles += GoldenSparkParticle(
                    x = pos.x,
                    y = pos.y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    life = 0.75f + Random.nextFloat() * 0.25f,
                    radius = 2.5f + Random.nextFloat() * 4.5f
                )
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "calligraphy_studio_fx")
    val rosetteAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing), RepeatMode.Restart),
        label = "rosette_spin"
    )
    val shimmerPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "gold_shimmer"
    )

    val displayedText = customText.trim().ifBlank { selectedPiece.calligraphyTextAr }
    val displayedRef = if (customText.isNotBlank()) {
        L10n.pick(lang, "مخطوطة نور التفاعلية", "NOOR Illuminated Manuscript", "NOOR zarhal qo'lyozmasi")
    } else {
        L10n.pick(lang, selectedPiece.surahRefAr, selectedPiece.surahRefEn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_calligraphy_studio_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مرسم التذهيب والخط العربي ٣D", "Golden Calligraphy & Noor Studio", "Zarhal Xattotlik Studiyasi"),
            subtitle = L10n.pick(
                lang,
                "المس اللوحة لنثر ذرات النور الذهبية وتصدير لوحتك بدقة عالية",
                "Touch canvas to emit golden Noor sparks, stream audio & export HD art",
                "Oltin nur zarrachalarini sochish uchun oynaga teging va HD saqlang"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive Illuminated Manuscript Canvas with Live Touch Particles & Audio Halo
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(selectedTheme.bgTop, selectedTheme.bgMid, selectedTheme.bgBottom)
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    selectedTheme.goldAccent,
                                    selectedTheme.secondaryGlow.copy(alpha = 0.7f),
                                    selectedTheme.goldAccent
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { offset -> emitBurst(offset, withRipple = true) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                emitBurst(change.position, withRipple = false)
                            }
                        }
                        .testTag("calligraphy_interactive_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val minR = size.minDimension * 0.43f

                        // Divine central aura glow (expands dynamically while audio plays!)
                        val audioBoost = if (isPlayingAudio) 0.28f * shimmerPulse else 0.08f * shimmerPulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    selectedTheme.goldAccent.copy(alpha = 0.24f + audioBoost),
                                    selectedTheme.secondaryGlow.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = minR * (1.28f + audioBoost)
                            ),
                            radius = minR * (1.28f + audioBoost),
                            center = Offset(cx, cy)
                        )

                        // Rotating 12-Point Mamluk Golden Star Mandala
                        rotate(degrees = rosetteAngle, pivot = Offset(cx, cy)) {
                            for (layer in 0 until 3) {
                                val angleOffset = layer * 30.0
                                val starPath = Path()
                                for (i in 0 until 4) {
                                    val rad = Math.toRadians(angleOffset + i * 90.0)
                                    val px = cx + (minR * 0.96f * cos(rad)).toFloat()
                                    val py = cy + (minR * 0.96f * sin(rad)).toFloat()
                                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                                }
                                starPath.close()
                                drawPath(
                                    path = starPath,
                                    color = selectedTheme.goldAccent.copy(alpha = 0.26f),
                                    style = Stroke(width = 1.4.dp.toPx())
                                )
                            }
                        }

                        // Counter-rotating inner scalloped halo
                        rotate(degrees = -rosetteAngle * 0.65f, pivot = Offset(cx, cy)) {
                            drawCircle(
                                color = selectedTheme.goldAccent.copy(alpha = 0.35f),
                                radius = minR * 0.78f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            val dotCount = 24
                            for (i in 0 until dotCount) {
                                val a = Math.toRadians(i * (360.0 / dotCount))
                                val dx = cx + (minR * 0.85f * cos(a)).toFloat()
                                val dy = cy + (minR * 0.85f * sin(a)).toFloat()
                                drawCircle(
                                    color = selectedTheme.goldAccent.copy(alpha = 0.55f),
                                    radius = 2.dp.toPx(),
                                    center = Offset(dx, dy)
                                )
                            }
                        }

                        // Live Audio Reactive Concentric Wave Rings
                        if (isPlayingAudio) {
                            for (ring in 1..3) {
                                val waveR = minR * (0.55f + ((shimmerPulse + ring * 0.33f) % 1f) * 0.65f)
                                val alpha = (1f - ((shimmerPulse + ring * 0.33f) % 1f)) * 0.45f
                                drawCircle(
                                    color = selectedTheme.secondaryGlow.copy(alpha = alpha),
                                    radius = waveR,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }

                        // Ornamental inner frame border
                        val inset = 14.dp.toPx()
                        drawRoundRect(
                            color = selectedTheme.goldAccent.copy(alpha = 0.42f),
                            topLeft = Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Interactive Touch-Emitted Golden Starlight Particles & Ripples
                        particles.forEach { p ->
                            if (p.isRipple) {
                                drawCircle(
                                    color = selectedTheme.goldAccent.copy(alpha = p.life * 0.65f),
                                    radius = p.radius,
                                    center = Offset(p.x, p.y),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            } else {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            selectedTheme.goldAccent.copy(alpha = p.life),
                                            selectedTheme.secondaryGlow.copy(alpha = p.life * 0.4f),
                                            Color.Transparent
                                        ),
                                        center = Offset(p.x, p.y),
                                        radius = p.radius * 2.5f
                                    ),
                                    radius = p.radius * 2.5f,
                                    center = Offset(p.x, p.y)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = p.life),
                                    radius = p.radius * 0.65f,
                                    center = Offset(p.x, p.y)
                                )
                            }
                        }
                    }

                    // Illuminated Arabic Calligraphy Text Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = selectedTheme.goldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = displayedText,
                            fontFamily = AmiriFontFamily,
                            fontSize = if (displayedText.length > 52) 23.sp else 28.sp,
                            lineHeight = if (displayedText.length > 52) 40.sp else 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedTheme.inkColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✦ $displayedRef ✦",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedTheme.goldAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.pick(lang, "المس واسحب هنا لرسم شرارات النور الذهبية", "Touch & drag here to paint golden Noor sparks", "Oltin nur chizish uchun barmog'ingizni yurgizing"),
                            style = MaterialTheme.typography.labelSmall,
                            color = selectedTheme.inkColor.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // 2. Action Bar: Live Recitation Audio, Jump to Mushaf, Export & Share HD PNG
            item {
                LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPlayingAudio) stopAudio() else playAyahAudio(selectedPiece.globalAyahNumber)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calligraphy_audio_button")
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingAudio) L10n.pick(lang, "إيقاف", "Stop") else L10n.pick(lang, "تلاوة حيّة", "Listen"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                shareIlluminatedManuscriptBitmap(context, displayedText, displayedRef, selectedTheme)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calligraphy_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "حفظ ومشاركة HD", "Share HD Art"), color = GoldBright, fontWeight = FontWeight.Bold)
                        }
                    }
                    audioStatus?.let { status ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldBright,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenSurah(selectedPiece.surahNumber) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.pick(lang, "افتح السورة في المصحف الشريف", "Open Surah in Holy Mushaf", "Surani Mushafda ochish"))
                    }
                }
            }

            // 3. Five Museum Illumination Palettes
            item {
                LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Brush, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "مدارس التذهيب الإسلامي (٥ لوحات)", "Islamic Illumination Palettes (5)", "Zarhal palitralar (5)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuranCalligraphyEngine.themes.forEach { theme ->
                            val selected = theme.id == selectedTheme.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedTheme = theme }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(theme.bgMid, theme.bgBottom)))
                                        .border(
                                            width = if (selected) 3.dp else 1.2.dp,
                                            color = if (selected) GoldBright else theme.goldAccent.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(theme.goldAccent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, theme.nameAr.substringBefore(" "), theme.nameEn.substringBefore(" "), theme.nameUz.substringBefore(" ")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 4. Masterpiece Presets Selector
            item {
                LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = L10n.pick(lang, "مختارات اللوحات القرآنية المذهّبة", "Illuminated Quranic Masterpieces", "Tanlangan Qur'on lavhalari"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuranCalligraphyEngine.masterpieces.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { piece ->
                                    val active = piece.id == selectedPiece.id && customText.isBlank()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (active) GoldPrimary.copy(alpha = 0.22f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                            )
                                            .border(
                                                1.dp,
                                                if (active) GoldBright else GoldPrimary.copy(alpha = 0.25f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedPiece = piece
                                                customText = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = L10n.pick(lang, piece.titleAr, piece.titleEn, piece.titleUz),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) GoldBright else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = L10n.pick(lang, piece.surahRefAr, piece.surahRefEn),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Custom Arabic Calligraphy Input
            item {
                LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = L10n.pick(lang, "اكتب آية أو دعاء لتذهيبه فوراً", "Write Any Ayah or Dua to Illuminate", "Zarhal qilish uchun oyat yoki duo yozing"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        placeholder = { Text(L10n.pick(lang, "مثال: رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً...", "e.g., Rabbana atina fid-dunya hasanah...")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

private fun shareIlluminatedManuscriptBitmap(
    context: Context,
    text: String,
    reference: String,
    theme: IlluminationTheme
) {
    try {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(theme.bgTop.toArgb(), theme.bgMid.toArgb(), theme.bgBottom.toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Gold ornamental double border + star rosette
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.goldAccent.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(48f, 48f, width - 48f, height - 48f, 48f, 48f, goldPaint)
        goldPaint.strokeWidth = 2.5f
        goldPaint.alpha = 140
        canvas.drawCircle(width / 2f, height / 2f, 410f, goldPaint)
        canvas.drawCircle(width / 2f, height / 2f, 360f, goldPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.inkColor.toArgb()
            textSize = if (text.length > 55) 60f else 74f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val layoutWidth = width - 200
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(16f, 1.25f)
            .build()
        canvas.save()
        canvas.translate(100f, (height - staticLayout.height) / 2f - 30f)
        staticLayout.draw(canvas)
        canvas.restore()

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.goldAccent.toArgb()
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("✦ $reference ✦", width / 2f, height - 125f, refPaint)
        refPaint.textSize = 26f
        refPaint.alpha = 175
        canvas.drawText("NOOR — نور", width / 2f, height - 78f, refPaint)

        val file = File(context.cacheDir, "noor_illuminated_ayah.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$text\n— $reference")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "NOOR Calligraphy"))
    } catch (_: Throwable) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$text\n— $reference")
        }
        context.startActivity(Intent.createChooser(fallback, "NOOR"))
    }
}
