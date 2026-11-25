package com.example.domain.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class SanctuaryPreset(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val rainLevel: Float,
    val breezeLevel: Float,
    val streamLevel: Float,
    val droneLevel: Float,
    val recommendedSurah: Int,
    val recommendedSurahAr: String,
    val recommendedSurahEn: String
)

data class SakinahRecitationTrack(
    val surahNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val audioUrl: String
)

/**
 * Real-Time Procedural Multi-Layer Islamic Sanctuary Acoustic Synthesizer Engine.
 * Generates silky-smooth PCM 16-bit audio buffers on the fly for Rain, Fajr Breeze,
 * Flowing Stream, and Deep Tahajjud Harmonic Drone — zero static MP3 bloat, 100% offline-capable.
 */
object IslamicSanctuaryAudioEngine {

    const val SAMPLE_RATE = 22050

    val presets: List<SanctuaryPreset> = listOf(
        SanctuaryPreset(
            "haram_rain", "مطر الحرم المكي", "Rain in Makkah", "Makka yomg'iri",
            "هدوء قطرات المطر مع رنين سكينة خفيف للتدبر", "Gentle rainfall with soft serene warmth",
            rainLevel = 0.72f, breezeLevel = 0.22f, streamLevel = 0.15f, droneLevel = 0.18f,
            recommendedSurah = 55, recommendedSurahAr = "سورة الرحمن", recommendedSurahEn = "Surah Ar-Rahman"
        ),
        SanctuaryPreset(
            "tahajjud_stillness", "سكون التهجد والليل", "Tahajjud Stillness", "Tahajjud sokinligi",
            "نسيم الليل الهادئ مع نغمة سكون عميقة لقيام الليل", "Soft night breeze & deep harmonic peace",
            rainLevel = 0.0f, breezeLevel = 0.55f, streamLevel = 0.12f, droneLevel = 0.58f,
            recommendedSurah = 67, recommendedSurahAr = "سورة الملك", recommendedSurahEn = "Surah Al-Mulk"
        ),
        SanctuaryPreset(
            "hifz_focus", "تركيز الحفظ والمراجعة", "Deep Hifz Focus", "Hifz diqqat rejimi",
            "تدفق ماء لطيف يعزل الضوضاء ويزيد صفاء الذهن", "Flowing sanctuary stream for pure mental clarity",
            rainLevel = 0.28f, breezeLevel = 0.18f, streamLevel = 0.68f, droneLevel = 0.22f,
            recommendedSurah = 36, recommendedSurahAr = "سورة يس", recommendedSurahEn = "Surah Ya-Sin"
        ),
        SanctuaryPreset(
            "fajr_breeze", "نسيم الفجر النديّ", "Fajr Dawn Breeze", "Bomdod tongi shabadasi",
            "نسمات السحر النقية للاستعداد لأذكار الصباح", "Pure pre-dawn desert air for morning Adhkar",
            rainLevel = 0.12f, breezeLevel = 0.76f, streamLevel = 0.30f, droneLevel = 0.15f,
            recommendedSurah = 93, recommendedSurahAr = "سورة الضحى", recommendedSurahEn = "Surah Ad-Duha"
        ),
        SanctuaryPreset(
            "sleep_sakinah", "سكينة النوم والأذكار", "Peaceful Sleep Sanctuary", "Tinch uyqu sokinligi",
            "مزيج ناعم جداً من المطر الخفيف والنسيم للطمأنينة", "Ultra-soft warm rain & breeze for tranquil rest",
            rainLevel = 0.52f, breezeLevel = 0.42f, streamLevel = 0.20f, droneLevel = 0.35f,
            recommendedSurah = 18, recommendedSurahAr = "سورة الكهف", recommendedSurahEn = "Surah Al-Kahf"
        )
    )

    val sakinahRecitations: List<SakinahRecitationTrack> = listOf(
        SakinahRecitationTrack(55, "سورة الرحمن", "Surah Ar-Rahman", "Rahmon surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/55.mp3"),
        SakinahRecitationTrack(67, "سورة الملك", "Surah Al-Mulk", "Mulk surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/67.mp3"),
        SakinahRecitationTrack(36, "سورة يس", "Surah Ya-Sin", "Yosin surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/36.mp3"),
        SakinahRecitationTrack(56, "سورة الواقعة", "Surah Al-Waqi'ah", "Voqea surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/56.mp3"),
        SakinahRecitationTrack(18, "سورة الكهف", "Surah Al-Kahf", "Kahf surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/18.mp3"),
        SakinahRecitationTrack(93, "سورة الضحى", "Surah Ad-Duha", "Zuho surasi", "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/93.mp3")
    )

    /**
     * Stateful DSP synthesizer that renders continuous glitch-free 16-bit PCM frames
     * combining 4 independent organic ambient sound layers.
     */
    class SanctuarySynthesizer {
        private var b0 = 0.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var breezeLp = 0.0
        private var streamLp1 = 0.0
        private var streamLp2 = 0.0
        private var sampleIndex = 0L
        private val rng = Random(42)

        fun renderBuffer(
            out: ShortArray,
            rainLevel: Float,
            breezeLevel: Float,
            streamLevel: Float,
            droneLevel: Float
        ) {
            val rVol = rainLevel.coerceIn(0f, 1f).toDouble()
            val wVol = breezeLevel.coerceIn(0f, 1f).toDouble()
            val sVol = streamLevel.coerceIn(0f, 1f).toDouble()
            val dVol = droneLevel.coerceIn(0f, 1f).toDouble()

            for (i in out.indices) {
                val t = sampleIndex.toDouble() / SAMPLE_RATE.toDouble()
                sampleIndex++

                val white = rng.nextDouble() * 2.0 - 1.0

                // 1. Warm Pink-Noise Raindrop Texture
                var mix = 0.0
                if (rVol > 0.005) {
                    b0 = 0.99765 * b0 + white * 0.0990460
                    b1 = 0.96300 * b1 + white * 0.2965164
                    b2 = 0.57000 * b2 + white * 1.0526913
                    val pink = (b0 + b1 + b2 + white * 0.1848) * 0.075
                    val rainSwirl = 0.85 + 0.15 * sin(2.0 * PI * 0.14 * t)
                    mix += pink * rVol * rainSwirl
                }

                // 2. Slow-Swaying Fajr Desert Breeze (deep low-pass modulated noise)
                if (wVol > 0.005) {
                    breezeLp += 0.028 * (white - breezeLp)
                    val gust = 0.55 + 0.45 * sin(2.0 * PI * 0.09 * t + 0.6 * sin(2.0 * PI * 0.03 * t))
                    mix += breezeLp * 0.65 * wVol * gust
                }

                // 3. Flowing Sanctuary Water Stream (dual-band resonant ripple)
                if (sVol > 0.005) {
                    streamLp1 += 0.11 * (white - streamLp1)
                    streamLp2 += 0.018 * (streamLp1 - streamLp2)
                    val band = streamLp1 - streamLp2
                    val trickle = 0.72 + 0.28 * sin(2.0 * PI * 0.45 * t + sin(2.0 * PI * 0.19 * t))
                    mix += band * 0.60 * sVol * trickle
                }

                // 4. Tahajjud Serene Warm Harmonic Chord (D2 + A2 + F#3 ultra-gentle breath)
                if (dVol > 0.005) {
                    val breath = 0.78 + 0.22 * sin(2.0 * PI * 0.08 * t)
                    val drone = (
                        0.50 * sin(2.0 * PI * 146.83 * t) +
                            0.32 * sin(2.0 * PI * 220.00 * t) +
                            0.18 * sin(2.0 * PI * 293.66 * t)
                        ) * 0.14
                    mix += drone * dVol * breath
                }

                // Soft tanh-like warm limiting to prevent any clipping
                val clamped = mix.coerceIn(-0.92, 0.92)
                out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
