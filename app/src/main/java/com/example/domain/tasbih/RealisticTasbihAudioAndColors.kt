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

data class TasbihBeadTheme(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val highlightColor: Color,
    val bodyColor: Color,
    val shadowColor: Color,
    val rimGlowColor: Color,
    val cordColor: Color,
    val tasselColor: Color,
    val timbre: BeadMaterialTimbre
)

/**
 * 20 Handcrafted Luxury Physical Misbaha Bead Palettes + Real-Time Acoustic PCM Bead Clack Synthesizer.
 */
object RealisticTasbihCatalog {

    val themes: List<TasbihBeadTheme> = listOf(
        TasbihBeadTheme("royal_amber", "كهرمان ملكي", "Royal Amber", "Shohona kahrabo", Color(0xFFFFE082), Color(0xFFD98A1A), Color(0xFF6B3606), Color(0xFFFFF3C4), Color(0xFFC99B3B), Color(0xFFE3B74E), BeadMaterialTimbre.AMBER),
        TasbihBeadTheme("imperial_emerald", "زمرد فاخر", "Imperial Emerald", "Shohona zumrad", Color(0xFF7BE8B4), Color(0xFF138A5C), Color(0xFF053824), Color(0xFFB5FFE0), Color(0xFFD4AF37), Color(0xFFE6C665), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("natural_oud", "عود خشبي", "Natural Oud Wood", "Tabiiy ud yog'och", Color(0xFFD6A87C), Color(0xFF8B562E), Color(0xFF3E200D), Color(0xFFF0D1B0), Color(0xFFA87D4B), Color(0xFFC49456), BeadMaterialTimbre.WOOD),
        TasbihBeadTheme("yemeni_agate", "عقيق يماني", "Yemeni Agate", "Yaman aqiqi", Color(0xFFFF9A76), Color(0xFFB8321A), Color(0xFF4D0E06), Color(0xFFFFCBB8), Color(0xFFD4A24A), Color(0xFFE0B45C), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("persian_turquoise", "فيروز سماوي", "Persian Turquoise", "Feruza toshi", Color(0xFF9DF5F2), Color(0xFF16A0A8), Color(0xFF06474D), Color(0xFFD2FFFD), Color(0xFFD9B45A), Color(0xFFEAD07A), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("white_pearl", "لؤلؤ أبيض", "White Pearl", "Oq marvarid", Color(0xFFFFFFFF), Color(0xFFE6DFD3), Color(0xFF9B9183), Color(0xFFFFFFFF), Color(0xFFD4AF37), Color(0xFFE7C86E), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("crimson_ruby", "ياقوت قرمزي", "Crimson Ruby", "Qizil yoqut", Color(0xFFFF88A4), Color(0xFFA8123C), Color(0xFF470317), Color(0xFFFFC7D6), Color(0xFFE0B85A), Color(0xFFF0CE78), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("jet_obsidian", "أوبسيديان أسود", "Jet Obsidian", "Qora obsidian", Color(0xFF76828E), Color(0xFF232930), Color(0xFF080A0D), Color(0xFFB8C4D0), Color(0xFFD9B24C), Color(0xFFE8C868), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("lapis_lazuli", "لازورد أزرق", "Lapis Lazuli", "Ko'k lojuvard", Color(0xFF86B4FF), Color(0xFF1A4BA8), Color(0xFF081D4D), Color(0xFFC9DFFF), Color(0xFFE3BC59), Color(0xFFF2D27C), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("jade_green", "يشم أخضر", "Jade Green", "Yashil nefrit", Color(0xFFB5F0AE), Color(0xFF3E9C52), Color(0xFF164A22), Color(0xFFDCFFD8), Color(0xFFCBB168), Color(0xFFE0C87C), BeadMaterialTimbre.STONE),
        TasbihBeadTheme("rose_quartz", "روز كوارتز", "Rose Quartz", "Pushti kvarts", Color(0xFFFFD6E5), Color(0xFFD97C9E), Color(0xFF75304B), Color(0xFFFFEDF4), Color(0xFFE6BE78), Color(0xFFF3D496), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("sea_coral", "مرجان بحري", "Sea Coral", "Dengiz marjoni", Color(0xFFFFB49E), Color(0xFFE0533A), Color(0xFF731E12), Color(0xFFFFD9CE), Color(0xFFD8B568), Color(0xFFEBC97E), BeadMaterialTimbre.AMBER),
        TasbihBeadTheme("golden_topaz", "توباز ذهبي", "Golden Topaz", "Oltin topaz", Color(0xFFFFF4B8), Color(0xFFE0A922), Color(0xFF7A5106), Color(0xFFFFFAE0), Color(0xFFDFB852), Color(0xFFF4D57A), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("royal_amethyst", "أماثيست ملكي", "Royal Amethyst", "Binafsha ametist", Color(0xFFD9A6FF), Color(0xFF7330B5), Color(0xFF320E59), Color(0xFFEED4FF), Color(0xFFE0BD68), Color(0xFFF2D485), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("antique_ivory", "عاج عتيق", "Antique Ivory", "Qadimiy fil suyagi", Color(0xFFFFFAF0), Color(0xFFDDC9A2), Color(0xFF8C734B), Color(0xFFFFFFFF), Color(0xFFB58A3E), Color(0xFFD4AA58), BeadMaterialTimbre.WOOD),
        TasbihBeadTheme("green_amber", "كهرمان أخضر", "Green Amber", "Yashil kahrabo", Color(0xFFD6F288), Color(0xFF739A1E), Color(0xFF2D4406), Color(0xFFEEFFBD), Color(0xFFD5AF4A), Color(0xFFE8C86A), BeadMaterialTimbre.AMBER),
        TasbihBeadTheme("ice_crystal", "ماس ثلجي", "Ice Crystal", "Muz kristalli", Color(0xFFE8F7FF), Color(0xFF7AC2E8), Color(0xFF225B7D), Color(0xFFFFFFFF), Color(0xFFC5D6E3), Color(0xFFE0EDF5), BeadMaterialTimbre.CRYSTAL),
        TasbihBeadTheme("ottoman_bronze", "برونز عثماني", "Ottoman Bronze", "Usmonli bronzasi", Color(0xFFE8C39E), Color(0xFF9C6635), Color(0xFF47280E), Color(0xFFF7DEC3), Color(0xFFC29044), Color(0xFFDBAD5E), BeadMaterialTimbre.WOOD),
        TasbihBeadTheme("celestial_sky", "سماوي هادئ", "Celestial Sky", "Osmon moviysi", Color(0xFFAEE2FF), Color(0xFF2E85C7), Color(0xFF0E395E), Color(0xFFDDF3FF), Color(0xFFDAB65C), Color(0xFFEDCE7C), BeadMaterialTimbre.AMBER),
        TasbihBeadTheme("gilded_saffron", "زعفران مذهّب", "Gilded Saffron", "Zafaron oltini", Color(0xFFFFD67A), Color(0xFFCC7010), Color(0xFF632B02), Color(0xFFFFEBB8), Color(0xFFE3B64B), Color(0xFFF4CF6C), BeadMaterialTimbre.AMBER)
    )

    fun byId(id: String): TasbihBeadTheme = themes.firstOrNull { it.id == id } ?: themes.first()

    private const val PREFS_NAME = "noor_tasbih_bead_theme_v1"
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
 * so sliding beads forward, backward, or completing a round sounds like a real physical Tasbih.
 */
object RealisticBeadSoundPlayer {

    private const val SAMPLE_RATE = 22050
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playBeadClick(
        timbre: BeadMaterialTimbre = BeadMaterialTimbre.AMBER,
        isReverse: Boolean = false,
        isMilestone: Boolean = false
    ) {
        audioScope.launch {
            runCatching {
                val pcm = when {
                    isMilestone -> synthesizeMilestoneChime()
                    isReverse -> synthesizeBeadImpact(timbre, pitchFactor = 0.78, gain = 0.65)
                    else -> synthesizeBeadImpact(timbre, pitchFactor = 1.0, gain = 0.90)
                }
                playPcmBuffer(pcm)
            }
        }
    }

    /**
     * Pure deterministic acoustic resonator modeling two colliding beads:
     * short transient tap + dual damped modal frequencies tuned to wood, amber, stone, or crystal.
     */
    internal fun synthesizeBeadImpact(
        timbre: BeadMaterialTimbre,
        pitchFactor: Double = 1.0,
        gain: Double = 0.85
    ): ShortArray {
        val durationSec = when (timbre) {
            BeadMaterialTimbre.WOOD -> 0.055
            BeadMaterialTimbre.AMBER -> 0.062
            BeadMaterialTimbre.STONE -> 0.068
            BeadMaterialTimbre.CRYSTAL -> 0.095
        }
        val (f1Base, f2Base, decay1, decay2) = when (timbre) {
            BeadMaterialTimbre.WOOD -> Quad(920.0, 2150.0, 88.0, 145.0)
            BeadMaterialTimbre.AMBER -> Quad(1340.0, 2980.0, 74.0, 125.0)
            BeadMaterialTimbre.STONE -> Quad(1680.0, 3620.0, 65.0, 110.0)
            BeadMaterialTimbre.CRYSTAL -> Quad(2280.0, 4420.0, 42.0, 72.0)
        }
        val f1 = f1Base * pitchFactor
        val f2 = f2Base * pitchFactor
        val count = (SAMPLE_RATE * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val attack = (t / 0.0018).coerceIn(0.0, 1.0)
            val body = 0.65 * exp(-decay1 * t) * sin(2.0 * PI * f1 * t) +
                0.35 * exp(-decay2 * t) * sin(2.0 * PI * f2 * t)
            // Micro contact impulse on first 2ms for physical bead collision snap
            val snap = if (t < 0.0025) 0.45 * exp(-900.0 * t) * sin(2.0 * PI * 4200.0 * t) else 0.0
            val sample = ((body + snap) * attack * gain).coerceIn(-1.0, 1.0)
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

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
