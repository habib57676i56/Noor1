package com.example.ui.screens.ecosystem

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.audio.IslamicSanctuaryAudioEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Interactive Islamic Acoustic Sanctuary & Spatial Soundscape Mixer.
 * Real-time procedural 4-layer PCM audio engine (Haramain Rain, Fajr Breeze, Sanctuary Stream,
 * Tahajjud Harmonic Drone) + Live Reactive Visual Aurora/Rain Canvas + Paired Sakinah Quran Streams
 * + Auto-Fade Sleep/Focus Timer.
 */
@Composable
fun IslamicSanctuaryAudioScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var selectedPreset by remember { mutableStateOf(IslamicSanctuaryAudioEngine.presets.first()) }
    var rainLevel by remember { mutableFloatStateOf(selectedPreset.rainLevel) }
    var breezeLevel by remember { mutableFloatStateOf(selectedPreset.breezeLevel) }
    var streamLevel by remember { mutableFloatStateOf(selectedPreset.streamLevel) }
    var droneLevel by remember { mutableFloatStateOf(selectedPreset.droneLevel) }

    var isSanctuaryActive by remember { mutableStateOf(false) }
    var activeRecitationSurah by remember { mutableStateOf<Int?>(null) }
    var timerRemainingSeconds by remember { mutableIntStateOf(0) }

    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopRecitation() {
        runCatching {
            mediaPlayerRef.value?.stop()
            mediaPlayerRef.value?.release()
        }
        mediaPlayerRef.value = null
        activeRecitationSurah = null
    }

    fun startRecitation(surahNumber: Int, url: String) {
        if (activeRecitationSurah == surahNumber) {
            stopRecitation()
            return
        }
        stopRecitation()
        activeRecitationSurah = surahNumber
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { activeRecitationSurah = null }
                setOnErrorListener { _, _, _ ->
                    activeRecitationSurah = null
                    true
                }
                prepareAsync()
            }
            mediaPlayerRef.value = mp
        } catch (_: Throwable) {
            activeRecitationSurah = null
        }
    }

    // Real-time procedural PCM AudioTrack synthesis loop
    LaunchedEffect(isSanctuaryActive) {
        if (!isSanctuaryActive) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sampleRate = IslamicSanctuaryAudioEngine.SAMPLE_RATE
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                val synth = IslamicSanctuaryAudioEngine.SanctuarySynthesizer()
                val buffer = ShortArray(1024)
                track.play()
                while (isActive && isSanctuaryActive) {
                    synth.renderBuffer(buffer, rainLevel, breezeLevel, streamLevel, droneLevel)
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Throwable) {
                // Gracefully degrade if device audio hardware is busy
            } finally {
                runCatching {
                    track?.pause()
                    track?.flush()
                    track?.release()
                }
            }
        }
    }

    // Optional Sleep / Focus Countdown Timer
    LaunchedEffect(timerRemainingSeconds, isSanctuaryActive) {
        if (timerRemainingSeconds > 0 && (isSanctuaryActive || activeRecitationSurah != null)) {
            delay(1000L)
            if (timerRemainingSeconds <= 1) {
                timerRemainingSeconds = 0
                isSanctuaryActive = false
                stopRecitation()
            } else {
                timerRemainingSeconds -= 1
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isSanctuaryActive = false
            stopRecitation()
        }
    }

    val infinite = rememberInfiniteTransition(label = "sanctuary_visualizer")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora_wave"
    )
    val rainFall by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "rain_streaks"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("islamic_sanctuary_audio_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "واحة السكينة والصوتيات الحية", "Islamic Acoustic Sanctuary", "Sokinlik va Audio Voha"),
            subtitle = L10n.pick(
                lang,
                "مزيج صوتي حيّ (مطر الحرم، نسيم الفجر، جداول الماء، تلاوة هادئة)",
                "Real-time 4-layer ambient audio mixer + Sakinah Quran streams & visualizer",
                "Jonli 4 qatlamli sokin tovushlar + Qur'on tilovati va vizual effektlar"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Reactive Audio Aurora, Rain Streaks & Sacred Geometry Visualizer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF041D1B), Color(0xFF082E29), Color(0xFF031015))
                            )
                        )
                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val energy = if (isSanctuaryActive || activeRecitationSurah != null) {
                            (0.35f + (rainLevel + breezeLevel + streamLevel + droneLevel) * 0.25f).coerceAtMost(1.1f)
                        } else {
                            0.18f
                        }

                        // Harmonic multi-ribbon acoustic aurora waves
                        val colors = listOf(EmeraldBright, GoldBright, Color(0xFF38BDF8))
                        colors.forEachIndexed { idx, col ->
                            val path = Path()
                            val baseY = h * (0.48f + idx * 0.08f)
                            val amp = h * (0.07f + idx * 0.03f) * energy
                            val freq = 1.6f + idx * 0.65f
                            for (xStep in 0..48) {
                                val frac = xStep / 48f
                                val x = frac * w
                                val y = baseY + sin((frac * freq * 2.0 * PI + wavePhase + idx * 1.1).toDouble()).toFloat() * amp
                                if (xStep == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = col.copy(alpha = if (isSanctuaryActive) 0.72f else 0.30f),
                                style = Stroke(width = (2.4f - idx * 0.4f).dp.toPx())
                            )
                        }

                        // Live falling raindrops when rainLevel > 0.05
                        if (rainLevel > 0.05f) {
                            val drops = (rainLevel * 28).toInt().coerceAtLeast(4)
                            for (i in 0 until drops) {
                                val sx = ((i * 67 + 13) % 100) / 100f * w
                                val sy = ((((i * 37) % 100) / 100f + rainFall) % 1f) * h
                                drawLine(
                                    color = Color(0xFF93C5FD).copy(alpha = 0.45f * rainLevel),
                                    start = Offset(sx, sy),
                                    end = Offset(sx - 5.dp.toPx(), sy + 18.dp.toPx()),
                                    strokeWidth = 1.4.dp.toPx()
                                )
                            }
                        }

                        // Center pulsing Tahajjud serenity halo
                        val haloR = size.minDimension * (0.24f + 0.06f * sin(wavePhase.toDouble()).toFloat() * energy)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GoldBright.copy(alpha = 0.25f * energy),
                                    EmeraldPrimary.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(w / 2f, h / 2f),
                                radius = haloR * 1.9f
                            ),
                            radius = haloR * 1.9f,
                            center = Offset(w / 2f, h / 2f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        if (isSanctuaryActive) listOf(GoldBright, GoldPrimary)
                                        else listOf(EmeraldPrimary, EmeraldDeep)
                                    )
                                )
                                .border(2.dp, GoldBright.copy(alpha = 0.8f), CircleShape)
                                .clickable { isSanctuaryActive = !isSanctuaryActive }
                                .testTag("sanctuary_master_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSanctuaryActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = "Toggle Sanctuary Audio",
                                tint = if (isSanctuaryActive) EmeraldDeep else Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = L10n.pick(lang, selectedPreset.titleAr, selectedPreset.titleEn, selectedPreset.titleUz),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isSanctuaryActive) {
                                L10n.pick(lang, "البث الصوتي الطبيعي يعمل الآن بتناغم حيّ", "Live Procedural Acoustic Sanctuary Active", "Jonli sokin audio muhit faol")
                            } else {
                                L10n.pick(lang, "اضغط للتشغيل ومزج أصوات الطبيعة والسكينة", "Tap to launch real-time multi-layer sanctuary", "Jonli sokin muhitni boshlash uchun bosing")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldBright.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. One-Tap Sanctuary Presets
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IslamicSanctuaryAudioEngine.presets.forEach { preset ->
                        val active = preset.id == selectedPreset.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (active) GoldPrimary.copy(alpha = 0.24f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
                                )
                                .border(1.dp, if (active) GoldBright else GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedPreset = preset
                                    rainLevel = preset.rainLevel
                                    breezeLevel = preset.breezeLevel
                                    streamLevel = preset.streamLevel
                                    droneLevel = preset.droneLevel
                                    isSanctuaryActive = true
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = L10n.pick(lang, preset.titleAr, preset.titleEn, preset.titleUz),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (active) GoldBright else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. 4-Channel Real-Time Ambient Sound Mixer Sliders
            item {
                LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "مازج الطبقات الصوتية الحيّ", "Real-Time 4-Layer Acoustic Mixer", "4 qatlamli jonli ovoz mikseri"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SoundLayerSlider(
                        label = L10n.pick(lang, "مطر الحرم الهادئ", "Gentle Haramain Rain", "Makka yomg'iri"),
                        icon = Icons.Rounded.WaterDrop,
                        accent = Color(0xFF38BDF8),
                        value = rainLevel,
                        onValueChange = { rainLevel = it }
                    )
                    SoundLayerSlider(
                        label = L10n.pick(lang, "نسيم الفجر والصحراء", "Fajr Dawn Breeze", "Bomdod shabadasi"),
                        icon = Icons.Rounded.Air,
                        accent = EmeraldBright,
                        value = breezeLevel,
                        onValueChange = { breezeLevel = it }
                    )
                    SoundLayerSlider(
                        label = L10n.pick(lang, "جدول ماء السكينة", "Flowing Sanctuary Stream", "Sokin suv oqimi"),
                        icon = Icons.Rounded.Waves,
                        accent = Color(0xFF2DD4BF),
                        value = streamLevel,
                        onValueChange = { streamLevel = it }
                    )
                    SoundLayerSlider(
                        label = L10n.pick(lang, "رنين التهجد الساكن", "Tahajjud Warm Resonance", "Tahajjud sokin ohangi"),
                        icon = Icons.Rounded.Spa,
                        accent = GoldBright,
                        value = droneLevel,
                        onValueChange = { droneLevel = it }
                    )
                }
            }

            // 4. Paired Live Sakinah Quranic Recitations (Alafasy)
            item {
                LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "تلاوات السكينة المصاحبة (بث مباشر)", "Paired Sakinah Quran Streams", "Hamroh Qur'on tilovatlari"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IslamicSanctuaryAudioEngine.sakinahRecitations.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { track ->
                                    val playing = activeRecitationSurah == track.surahNumber
                                    Button(
                                        onClick = { startRecitation(track.surahNumber, track.audioUrl) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (playing) GoldPrimary else EmeraldPrimary.copy(alpha = 0.28f),
                                            contentColor = if (playing) EmeraldDeep else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = L10n.pick(lang, track.titleAr, track.titleEn, track.titleUz),
                                            maxLines = 1,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Auto-Fade Sleep / Hifz Focus Timer
            item {
                LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "مؤقت الإيقاف التلقائي (نوم / حفظ)", "Sleep & Focus Auto-Stop Timer", "Avto-to'xtash taymeri"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright
                            )
                        }
                        if (timerRemainingSeconds > 0) {
                            val mm = timerRemainingSeconds / 60
                            val ss = timerRemainingSeconds % 60
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", mm, ss),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldBright
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 10, 20, 30, 60).forEach { mins ->
                            val selected = (mins == 0 && timerRemainingSeconds == 0) ||
                                (mins > 0 && timerRemainingSeconds in ((mins - 1) * 60 + 1)..(mins * 60))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) GoldPrimary.copy(alpha = 0.28f) else EmeraldPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, if (selected) GoldBright else GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable { timerRemainingSeconds = mins * 60 }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (mins == 0) L10n.pick(lang, "إيقاف", "Off") else "${mins}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) GoldBright else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundLayerSlider(
    label: String,
    icon: ImageVector,
    accent: Color,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.22f)
            )
        )
    }
}
