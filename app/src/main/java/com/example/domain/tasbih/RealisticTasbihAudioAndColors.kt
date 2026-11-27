package com.example.domain.tasbih

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class BeadMaterialTimbre {
    WOOD,
    AMBER,
    STONE,
    CRYSTAL
}

enum class BeadSurfaceTexture {
    JADE_MARBLE,
    WOOD_GRAIN,
    OLIVE_WOOD,
    GLOSSY_AMBER,
    METALLIC_PEARL,
    OBSIDIAN_GLASS,
    TURQUOISE_VEIN,
    CRYSTAL_GEM
}

data class TasbihBeadTheme(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val nameRu: String,
    val highlightColor: Color,
    val bodyColor: Color,
    val shadowColor: Color,
    val rimGlowColor: Color,
    val cordColor: Color,
    val tasselColor: Color,
    val timbre: BeadMaterialTimbre,
    val texture: BeadSurfaceTexture = BeadSurfaceTexture.JADE_MARBLE,
    val veinColor: Color = Color.White.copy(alpha = 0.25f)
)

/**
 * 20 Handcrafted Luxury Physical Misbaha Bead Palettes + Real-Time Acoustic PCM Bead Clack Synthesizer.
 * Default theme is Imperial Emerald Jade (matching classic polished green jade Misbaha beads) alongside
 * authentic Oud Wood, Olive Wood, Golden Amber, White Silver Pearl, Jet Obsidian, Turquoise, and Ruby.
 */
object RealisticTasbihCatalog {

    val themes: List<TasbihBeadTheme> = listOf(
        TasbihBeadTheme(
            id = "imperial_emerald",
            nameAr = "يشم زمردي",
            nameEn = "Imperial Emerald",
            nameUz = "Shohona zumrad",
            nameRu = "Изумрудный нефрит",
            highlightColor = Color(0xFF7DE888),
            bodyColor = Color(0xFF1F7A33),
            shadowColor = Color(0xFF072B10),
            rimGlowColor = Color(0xFFA6F5B0),
            cordColor = Color(0xFFF3F6F4),
            tasselColor = Color(0xFF28C76F),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.JADE_MARBLE,
            veinColor = Color(0xFF0B3F17)
        ),
        TasbihBeadTheme(
            id = "natural_oud",
            nameAr = "خشب العود",
            nameEn = "Natural Oud Wood",
            nameUz = "Tabiiy ud yog'och",
            nameRu = "Дерево Уд",
            highlightColor = Color(0xFFE0B484),
            bodyColor = Color(0xFF8C5327),
            shadowColor = Color(0xFF3A1D0A),
            rimGlowColor = Color(0xFFF4D5B2),
            cordColor = Color(0xFFEDE4D5),
            tasselColor = Color(0xFFD4A36A),
            timbre = BeadMaterialTimbre.WOOD,
            texture = BeadSurfaceTexture.WOOD_GRAIN,
            veinColor = Color(0xFF4F280E)
        ),
        TasbihBeadTheme(
            id = "antique_ivory",
            nameAr = "خشب الزيتون",
            nameEn = "Olive Wood",
            nameUz = "Zaytun yog'ochi",
            nameRu = "Оливковое дерево",
            highlightColor = Color(0xFFF2CFA4),
            bodyColor = Color(0xFFC18A56),
            shadowColor = Color(0xFF643B1A),
            rimGlowColor = Color(0xFFFFE5C7),
            cordColor = Color(0xFFF3ECE1),
            tasselColor = Color(0xFFDFAD72),
            timbre = BeadMaterialTimbre.WOOD,
            texture = BeadSurfaceTexture.OLIVE_WOOD,
            veinColor = Color(0xFF7D4B21)
        ),
        TasbihBeadTheme(
            id = "royal_amber",
            nameAr = "كهرمان ملكي",
            nameEn = "Royal Amber",
            nameUz = "Shohona kahrabo",
            nameRu = "Королевский янтарь",
            highlightColor = Color(0xFFFFF176),
            bodyColor = Color(0xFFD9B200),
            shadowColor = Color(0xFF755700),
            rimGlowColor = Color(0xFFFFF8B8),
            cordColor = Color(0xFFF7F2E2),
            tasselColor = Color(0xFFF0C93D),
            timbre = BeadMaterialTimbre.AMBER,
            texture = BeadSurfaceTexture.GLOSSY_AMBER,
            veinColor = Color(0xFFFFF59D)
        ),
        TasbihBeadTheme(
            id = "white_pearl",
            nameAr = "لؤلؤ فضي",
            nameEn = "Silver Pearl",
            nameUz = "Oq marvarid",
            nameRu = "Серебряный жемчуг",
            highlightColor = Color(0xFFFFFFFF),
            bodyColor = Color(0xFFD0D5DA),
            shadowColor = Color(0xFF474E56),
            rimGlowColor = Color(0xFFFFFFFF),
            cordColor = Color(0xFFEBF0F2),
            tasselColor = Color(0xFFDCE3E8),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.METALLIC_PEARL,
            veinColor = Color(0xFF8E969E)
        ),
        TasbihBeadTheme(
            id = "jet_obsidian",
            nameAr = "أوبسيديان أسود",
            nameEn = "Jet Obsidian",
            nameUz = "Qora obsidian",
            nameRu = "Чёрный обсидиан",
            highlightColor = Color(0xFF6B7682),
            bodyColor = Color(0xFF191D22),
            shadowColor = Color(0xFF040507),
            rimGlowColor = Color(0xFF9EAAB6),
            cordColor = Color(0xFFE2E6EA),
            tasselColor = Color(0xFFD4AF37),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.OBSIDIAN_GLASS,
            veinColor = Color(0xFF3E4752)
        ),
        TasbihBeadTheme(
            id = "ottoman_bronze",
            nameAr = "خشب الجوز الداكن",
            nameEn = "Walnut Rosewood",
            nameUz = "Yong'oq yog'ochi",
            nameRu = "Тёмный орех",
            highlightColor = Color(0xFFD69F72),
            bodyColor = Color(0xFF6B3A1E),
            shadowColor = Color(0xFF281106),
            rimGlowColor = Color(0xFFEDC29C),
            cordColor = Color(0xFFEBE0D1),
            tasselColor = Color(0xFFC99050),
            timbre = BeadMaterialTimbre.WOOD,
            texture = BeadSurfaceTexture.WOOD_GRAIN,
            veinColor = Color(0xFF3A1A09)
        ),
        TasbihBeadTheme(
            id = "yemeni_agate",
            nameAr = "عقيق يماني",
            nameEn = "Yemeni Agate",
            nameUz = "Yaman aqiqi",
            nameRu = "Йеменский агат",
            highlightColor = Color(0xFFFF9E7D),
            bodyColor = Color(0xFFB52E18),
            shadowColor = Color(0xFF470B04),
            rimGlowColor = Color(0xFFFFC9B5),
            cordColor = Color(0xFFF4EDE4),
            tasselColor = Color(0xFFE3784C),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.JADE_MARBLE,
            veinColor = Color(0xFF691208)
        ),
        TasbihBeadTheme(
            id = "persian_turquoise",
            nameAr = "فيروز نيشابوري",
            nameEn = "Persian Turquoise",
            nameUz = "Feruza toshi",
            nameRu = "Персидская бирюза",
            highlightColor = Color(0xFF9DF7F4),
            bodyColor = Color(0xFF149CA6),
            shadowColor = Color(0xFF054148),
            rimGlowColor = Color(0xFFD0FFFD),
            cordColor = Color(0xFFEFF8F8),
            tasselColor = Color(0xFF32C8D0),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.TURQUOISE_VEIN,
            veinColor = Color(0xFF0A3940)
        ),
        TasbihBeadTheme(
            id = "crimson_ruby",
            nameAr = "ياقوت أحمر",
            nameEn = "Crimson Ruby",
            nameUz = "Qizil yoqut",
            nameRu = "Алый рубин",
            highlightColor = Color(0xFFFF88A4),
            bodyColor = Color(0xFFA8123C),
            shadowColor = Color(0xFF450316),
            rimGlowColor = Color(0xFFFFC7D6),
            cordColor = Color(0xFFF6EDF0),
            tasselColor = Color(0xFFE84C72),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.CRYSTAL_GEM,
            veinColor = Color(0xFF5D0720)
        ),
        TasbihBeadTheme(
            id = "lapis_lazuli",
            nameAr = "لازورد ملكي",
            nameEn = "Lapis Lazuli",
            nameUz = "Ko'k lojuvard",
            nameRu = "Лазурит",
            highlightColor = Color(0xFF86B4FF),
            bodyColor = Color(0xFF1A4BA8),
            shadowColor = Color(0xFF071B47),
            rimGlowColor = Color(0xFFC9DFFF),
            cordColor = Color(0xFFECF1F8),
            tasselColor = Color(0xFFD9B44A),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.TURQUOISE_VEIN,
            veinColor = Color(0xFFE5C158)
        ),
        TasbihBeadTheme(
            id = "jade_green",
            nameAr = "يشم أخضر فاتح",
            nameEn = "Spring Jade",
            nameUz = "Yashil nefrit",
            nameRu = "Весенний нефрит",
            highlightColor = Color(0xFFB8F5A8),
            bodyColor = Color(0xFF3FA34D),
            shadowColor = Color(0xFF144A1E),
            rimGlowColor = Color(0xFFDCFFD4),
            cordColor = Color(0xFFF1F7F1),
            tasselColor = Color(0xFF52C963),
            timbre = BeadMaterialTimbre.STONE,
            texture = BeadSurfaceTexture.JADE_MARBLE,
            veinColor = Color(0xFF1C5E28)
        ),
        TasbihBeadTheme(
            id = "rose_quartz",
            nameAr = "كوارتز وردي",
            nameEn = "Rose Quartz",
            nameUz = "Pushti kvarts",
            nameRu = "Розовый кварц",
            highlightColor = Color(0xFFFFD8E6),
            bodyColor = Color(0xFFD97C9E),
            shadowColor = Color(0xFF702B46),
            rimGlowColor = Color(0xFFFFEEF5),
            cordColor = Color(0xFFF9F1F4),
            tasselColor = Color(0xFFEB9BB8),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.CRYSTAL_GEM,
            veinColor = Color(0xFF9A4667)
        ),
        TasbihBeadTheme(
            id = "sea_coral",
            nameAr = "مرجان بحري",
            nameEn = "Sea Coral",
            nameUz = "Dengiz marjoni",
            nameRu = "Морской коралл",
            highlightColor = Color(0xFFFFB5A0),
            bodyColor = Color(0xFFE0523A),
            shadowColor = Color(0xFF701C10),
            rimGlowColor = Color(0xFFFFD9CE),
            cordColor = Color(0xFFF7EFEA),
            tasselColor = Color(0xFFF07B63),
            timbre = BeadMaterialTimbre.AMBER,
            texture = BeadSurfaceTexture.JADE_MARBLE,
            veinColor = Color(0xFF8C2819)
        ),
        TasbihBeadTheme(
            id = "golden_topaz",
            nameAr = "توباز ذهبي",
            nameEn = "Golden Topaz",
            nameUz = "Oltin topaz",
            nameRu = "Золотой топаз",
            highlightColor = Color(0xFFFFF4B8),
            bodyColor = Color(0xFFE0A922),
            shadowColor = Color(0xFF784E06),
            rimGlowColor = Color(0xFFFFFAE0),
            cordColor = Color(0xFFF7F2E4),
            tasselColor = Color(0xFFF4C94E),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.GLOSSY_AMBER,
            veinColor = Color(0xFFFFE57F)
        ),
        TasbihBeadTheme(
            id = "royal_amethyst",
            nameAr = "أماثيست ملكي",
            nameEn = "Royal Amethyst",
            nameUz = "Binafsha ametist",
            nameRu = "Королевский аметист",
            highlightColor = Color(0xFFD9A6FF),
            bodyColor = Color(0xFF7330B5),
            shadowColor = Color(0xFF300D56),
            rimGlowColor = Color(0xFFEED4FF),
            cordColor = Color(0xFFF3EEF9),
            tasselColor = Color(0xFFA766E6),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.CRYSTAL_GEM,
            veinColor = Color(0xFF47167A)
        ),
        TasbihBeadTheme(
            id = "green_amber",
            nameAr = "كهرمان أخضر",
            nameEn = "Green Amber",
            nameUz = "Yashil kahrabo",
            nameRu = "Зелёный янтарь",
            highlightColor = Color(0xFFD8F488),
            bodyColor = Color(0xFF739A1E),
            shadowColor = Color(0xFF2B4205),
            rimGlowColor = Color(0xFFEEFFBD),
            cordColor = Color(0xFFF2F6E8),
            tasselColor = Color(0xFFA3C93E),
            timbre = BeadMaterialTimbre.AMBER,
            texture = BeadSurfaceTexture.GLOSSY_AMBER,
            veinColor = Color(0xFF9AC433)
        ),
        TasbihBeadTheme(
            id = "ice_crystal",
            nameAr = "بلور ثلجي",
            nameEn = "Ice Crystal",
            nameUz = "Muz kristalli",
            nameRu = "Ледяной хрусталь",
            highlightColor = Color(0xFFEAF8FF),
            bodyColor = Color(0xFF7AC2E8),
            shadowColor = Color(0xFF1F587A),
            rimGlowColor = Color(0xFFFFFFFF),
            cordColor = Color(0xFFEFF6FA),
            tasselColor = Color(0xFFA3D8F5),
            timbre = BeadMaterialTimbre.CRYSTAL,
            texture = BeadSurfaceTexture.CRYSTAL_GEM,
            veinColor = Color(0xFFBFE9FF)
        ),
        TasbihBeadTheme(
            id = "celestial_sky",
            nameAr = "سماوي هادئ",
            nameEn = "Celestial Sky",
            nameUz = "Osmon moviysi",
            nameRu = "Небесный сапфир",
            highlightColor = Color(0xFFAEE2FF),
            bodyColor = Color(0xFF2E85C7),
            shadowColor = Color(0xFF0D375B),
            rimGlowColor = Color(0xFFDDF3FF),
            cordColor = Color(0xFFEDF4FA),
            tasselColor = Color(0xFF66B2EB),
            timbre = BeadMaterialTimbre.AMBER,
            texture = BeadSurfaceTexture.GLOSSY_AMBER,
            veinColor = Color(0xFF56A8E8)
        ),
        TasbihBeadTheme(
            id = "gilded_saffron",
            nameAr = "خشب الصندل الذهبي",
            nameEn = "Golden Sandalwood",
            nameUz = "Oltin sandal yog'ochi",
            nameRu = "Золотой сандал",
            highlightColor = Color(0xFFFFD685),
            bodyColor = Color(0xFFC4731C),
            shadowColor = Color(0xFF5E2B04),
            rimGlowColor = Color(0xFFFFECC2),
            cordColor = Color(0xFFF5EFE2),
            tasselColor = Color(0xFFE6A545),
            timbre = BeadMaterialTimbre.WOOD,
            texture = BeadSurfaceTexture.WOOD_GRAIN,
            veinColor = Color(0xFF7D3B08)
        )
    )

    fun byId(id: String): TasbihBeadTheme = themes.firstOrNull { it.id == id } ?: themes.first()

    private const val PREFS_NAME = "noor_tasbih_bead_theme_v2"
    private const val KEY_THEME_ID = "selected_bead_theme_id"

    fun loadSelectedThemeId(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_ID, themes.first().id)
            ?: themes.first().id
    }

    fun saveSelectedThemeId(context: Context, id: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, id)
            .apply()
    }
}

/**
 * Synthesizes authentic physical wooden/amber/stone/crystal Misbaha bead clack PCM audio in real time
 * so sliding beads forward, backward, or completing a round sounds genuinely like colliding physical beads.
 */
object RealisticBeadSoundPlayer {

    private const val SAMPLE_RATE = 22050
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playBeadClick(
        timbre: BeadMaterialTimbre = BeadMaterialTimbre.STONE,
        isReverse: Boolean = false,
        isMilestone: Boolean = false
    ) {
        audioScope.launch {
            runCatching {
                val pcm = when {
                    isMilestone -> synthesizeMilestoneChime()
                    isReverse -> synthesizeBeadImpact(timbre, pitchFactor = 0.80, gain = 0.68)
                    else -> synthesizeBeadImpact(timbre, pitchFactor = 1.0, gain = 0.94)
                }
                playPcmBuffer(pcm)
            }
        }
    }

    /**
     * Multi-mode physical resonator modeling two real spherical Misbaha beads colliding on a taut silk string:
     * - Crisp sub-2ms contact transient ("clack" attack)
     * - 3 material-calibrated damped modal resonance frequencies (Hollow Wood, Warm Amber, Dense Jade Stone, Crisp Crystal)
     * - Subtle 13ms micro-rebound knock when the bead settles against the neighboring bead stack
     */
    internal fun synthesizeBeadImpact(
        timbre: BeadMaterialTimbre,
        pitchFactor: Double = 1.0,
        gain: Double = 0.85
    ): ShortArray {
        val durationSec = when (timbre) {
            BeadMaterialTimbre.WOOD -> 0.068
            BeadMaterialTimbre.AMBER -> 0.072
            BeadMaterialTimbre.STONE -> 0.078
            BeadMaterialTimbre.CRYSTAL -> 0.105
        }
        val (f1Base, f2Base, f3Base, decay1, decay2) = when (timbre) {
            BeadMaterialTimbre.WOOD -> Quint(760.0, 1680.0, 2940.0, 72.0, 130.0)
            BeadMaterialTimbre.AMBER -> Quint(1260.0, 2720.0, 4180.0, 66.0, 118.0)
            BeadMaterialTimbre.STONE -> Quint(1640.0, 3480.0, 5240.0, 58.0, 102.0)
            BeadMaterialTimbre.CRYSTAL -> Quint(2320.0, 4560.0, 6820.0, 38.0, 68.0)
        }
        val f1 = f1Base * pitchFactor
        val f2 = f2Base * pitchFactor
        val f3 = f3Base * pitchFactor
        val count = (SAMPLE_RATE * durationSec).toInt().coerceAtLeast(512)
        val out = ShortArray(count)
        val reboundDelay = 0.0135

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val attack = (t / 0.0014).coerceIn(0.0, 1.0)
            val body = 0.56 * exp(-decay1 * t) * sin(2.0 * PI * f1 * t) +
                0.28 * exp(-decay2 * t) * sin(2.0 * PI * f2 * t) +
                0.14 * exp(-(decay2 * 1.45) * t) * sin(2.0 * PI * f3 * t)
            // Physical contact impulse transient for authentic bead-on-bead clack
            val snap = if (t < 0.0028) 0.48 * exp(-850.0 * t) * sin(2.0 * PI * (f2 * 1.6) * t) else 0.0
            // Subtle micro-rebound click against neighboring bead on the string
            val rebound = if (t > reboundDelay) {
                val tr = t - reboundDelay
                val rAttack = (tr / 0.0012).coerceIn(0.0, 1.0)
                0.22 * rAttack * exp(-(decay1 * 1.35) * tr) * sin(2.0 * PI * (f1 * 1.06) * tr)
            } else {
                0.0
            }
            val sample = (((body + snap) * attack + rebound) * gain).coerceIn(-1.0, 1.0)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    internal fun synthesizeMilestoneChime(): ShortArray {
        val durationSec = 0.28
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val attack = (t / 0.004).coerceIn(0.0, 1.0)
            val note1 = 0.55 * exp(-12.0 * t) * sin(2.0 * PI * 880.0 * t)
            val note2 = if (t > 0.065) {
                val t2 = t - 0.065
                0.55 * exp(-10.0 * t2) * sin(2.0 * PI * 1318.5 * t2)
            } else 0.0
            val sample = ((note1 + note2) * attack * 0.80).coerceIn(-1.0, 1.0)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playPcmBuffer(pcm: ShortArray) {
        val byteSize = pcm.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(byteSize)
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            val durationMs = ((pcm.size.toLong() * 1000L) / SAMPLE_RATE) + 35L
            Thread.sleep(durationMs)
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private data class Quint<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
