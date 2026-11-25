package com.example.ui.screens.ecosystem

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.quran.QuranicSoulRemedyEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

/**
 * Spiritual & Emotional Quranic Remedy Studio (دواء القلوب من القرآن والسنة).
 * Pick your current emotional state -> receive healing Quranic Ayah with 1-tap live Alafasy audio,
 * Sahih Prophetic Dua, interactive Dhikr counter, and guided Serenity Breathwork Halo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranicSoulRemedyScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    var selectedRemedy by remember { mutableStateOf(QuranicSoulRemedyEngine.prescriptions.first()) }
    var dhikrCount by remember(selectedRemedy.id) { mutableIntStateOf(0) }
    var showBreathSession by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }

    fun stopAudio() {
        runCatching {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        }
        isPlaying = false
        isBuffering = false
    }

    fun togglePlayAyah(globalAyahNumber: Int) {
        if (isPlaying || isBuffering) {
            stopAudio()
            return
        }
        stopAudio()
        isBuffering = true
        val url = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyahNumber.mp3"
        runCatching {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener { mp ->
                isBuffering = false
                isPlaying = true
                mp.start()
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                isBuffering = false
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isPlaying = false
                isBuffering = false
                true
            }
            mediaPlayer.prepareAsync()
        }.onFailure {
            isBuffering = false
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_soul_remedy_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "دواء القلوب من القرآن والسنة", "Quranic Heart & Soul Remedy", "Qalblar shifosi (Qur'on va Sunnat)"),
            subtitle = L10n.pick(
                lang,
                "اختر ما يشعر به قلبك الآن لتتلقى آية شافية بصوت العفاسي ودعاء نبوياً صحيحاً وجلسة سكينة",
                "Choose how your heart feels for a healing Quran Ayah, live recitation, Sahih Dua & Dhikr calm",
                "Qalbingiz holatini tanlang: shifoli oyat, jonli tilovat, sahih duo va zikr sokinligi"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Emotional State Picker
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = L10n.pick(lang, "بماذا تشعر الآن؟", "How does your heart feel right now?", "Hozir qalbingiz qanday holatda?"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuranicSoulRemedyEngine.prescriptions.forEach { rx ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, rx.moodAr, rx.moodEn, rx.moodUz),
                                    selected = selectedRemedy.id == rx.id,
                                    onClick = {
                                        stopAudio()
                                        selectedRemedy = rx
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Healing Quranic Ayah Prescription Card with Live Alafasy Audio
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = GoldBright)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "الآية الشافية — سورة ${selectedRemedy.surahNameAr} (${selectedRemedy.ayahNumber})",
                                        "Healing Ayah — ${selectedRemedy.surahNameAr} (${selectedRemedy.surahNumber}:${selectedRemedy.ayahNumber})",
                                        "Shifoli oyat — ${selectedRemedy.surahNameAr} (${selectedRemedy.ayahNumber})"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(EmeraldDeep.copy(alpha = 0.55f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                text = selectedRemedy.ayahTextAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                lineHeight = 42.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = L10n.pick(lang, selectedRemedy.ayahReflectionAr, selectedRemedy.ayahReflectionEn, selectedRemedy.ayahReflectionUz),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { togglePlayAyah(selectedRemedy.globalAyahNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                when {
                                    isBuffering -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                    isPlaying -> Icon(Icons.Rounded.Stop, contentDescription = null)
                                    else -> Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isPlaying) L10n.pick(lang, "إيقاف التلاوة", "Stop Audio", "To'xtatish")
                                    else L10n.pick(lang, "استماع بصوت العفاسي", "Listen Live", "Tilovatni tinglash")
                                )
                            }
                            IconButton(
                                onClick = { onOpenSurah(selectedRemedy.surahNumber) },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(noorColors.glassSurfaceStrong)
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = "Open Mushaf", tint = GoldBright)
                            }
                        }
                    }
                }
            }

            // 3. Authentic Prophetic Dua Card
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "الدعاء النبوي المأثور (${selectedRemedy.duaSourceAr})", "Authentic Prophetic Dua", "Sahih Nabaviy duo"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            IconButton(
                                onClick = {
                                    val shareText = "${selectedRemedy.ayahTextAr}\n\n${selectedRemedy.propheticDuaAr}\n(${selectedRemedy.duaSourceAr})"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share Dua", tint = noorColors.goldAccent)
                            }
                        }
                        Text(
                            text = selectedRemedy.propheticDuaAr,
                            fontFamily = AmiriFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 36.sp
                        )
                        if (lang != com.example.domain.model.AppLanguage.ARABIC) {
                            Text(
                                text = selectedRemedy.propheticDuaEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Targeted Interactive Dhikr Counter + Serenity Breathwork Halo
            item {
                LiquidGlassCard(goldBorder = dhikrCount >= selectedRemedy.dhikrTargetCount) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "الذِّكر العلاجي (${dhikrCount} / ${selectedRemedy.dhikrTargetCount})", "Healing Dhikr ($dhikrCount / ${selectedRemedy.dhikrTargetCount})", "Shifoli zikr ($dhikrCount / ${selectedRemedy.dhikrTargetCount})"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            IconButton(onClick = { dhikrCount = 0 }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset Dhikr", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.16f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .clickable { dhikrCount += 1 }
                                .padding(16.dp)
                                .testTag("soul_remedy_dhikr_tap"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = selectedRemedy.dhikrTextAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$dhikrCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldBright
                                )
                            }
                        }

                        LiquidGlassChip(
                            label = if (showBreathSession) L10n.pick(lang, "إغلاق جلسة السكينة والتنفس", "Close Serenity Breath Halo", "Sokinlik nafasini yopish")
                            else L10n.pick(lang, "ابدأ جلسة السكينة والاطمئنان مع التسبيح", "Start Guided Serenity Dhikr Breath", "Zikrli sokinlik nafasini boshlash"),
                            selected = showBreathSession,
                            onClick = { showBreathSession = !showBreathSession }
                        )

                        if (showBreathSession) {
                            SerenityDhikrBreathHalo()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SerenityDhikrBreathHalo() {
    val lang = LocalAppLanguage.current
    val infinite = rememberInfiniteTransition(label = "serenity_breath")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val isInhaling = phase < 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension * 0.45f
            val currentR = maxR * (0.48f + 0.52f * phase)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldBright.copy(alpha = 0.35f), EmeraldPrimary.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = currentR * 1.25f
                ),
                radius = currentR * 1.25f,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A7A5A), EmeraldDeep),
                    center = Offset(cx, cy),
                    radius = currentR
                ),
                radius = currentR,
                center = Offset(cx, cy)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = GoldBright, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isInhaling) L10n.pick(lang, "شهيق هادئ · سُبْحَانَ اللَّهِ", "Inhale · SubhanAllah", "Nafas oling · Subhanalloh")
                else L10n.pick(lang, "زفير مطمئن · الْحَمْدُ لِلَّهِ", "Exhale · Alhamdulillah", "Nafas chiqaring · Alhamdulillah"),
                fontFamily = AmiriFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
