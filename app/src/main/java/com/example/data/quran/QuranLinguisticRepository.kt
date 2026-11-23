package com.example.data.quran

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline-first Structured Arabic Linguistic Repository (I'rab & Sarf).
 *
 * Resolution order & strict integrity guarantees:
 * 1. Verified Bundled Classical Scholarly Catalog (`QuranLinguisticCatalog`) — instant offline.
 * 2. User-Imported Verified Offline JSON Linguistic Packs stored in `filesDir/quran_linguistics/`.
 * 3. Honest `LinguisticVerificationStatus.UNAVAILABLE` fallback:
 *    - NEVER invents or fabricates grammatical (I'rab) or morphological (Sarf) analyses.
 *    - Preserves exact immutable Uthmani Quran text separately from linguistic annotations.
 *    - Surfaces verified Tafsir Al-Muyassar separately when cached, clearly distinguished from I'rab/Sarf.
 */
class QuranLinguisticRepository(private val appContext: Context) {

    private val packsDir: File = File(appContext.filesDir, "quran_linguistics").apply { mkdirs() }
    private val importedCache = mutableMapOf<Pair<Int, Int>, AyahLinguisticAnalysis>()
    @Volatile
    private var importedLoaded = false

    suspend fun getAyahLinguisticAnalysis(
        surahNumber: Int,
        ayahNumber: Int,
        exactImmutableUthmaniText: String,
        cachedTafsirAr: String = ""
    ): AyahLinguisticAnalysis = withContext(Dispatchers.IO) {
        ensureImportedPacksLoaded()

        // 1. Verified bundled classical catalog
        QuranLinguisticCatalog.findVerified(surahNumber, ayahNumber)?.let { verified ->
            return@withContext verified.copy(
                // Always keep exact immutable Uthmani Quran text untouched
                exactUthmaniAyahText = exactImmutableUthmaniText.ifBlank { verified.exactUthmaniAyahText },
                tafsirMuyassarAr = cachedTafsirAr
            )
        }

        // 2. Imported local verified JSON linguistic pack
        importedCache[surahNumber to ayahNumber]?.let { imported ->
            return@withContext imported.copy(
                exactUthmaniAyahText = exactImmutableUthmaniText.ifBlank { imported.exactUthmaniAyahText },
                tafsirMuyassarAr = cachedTafsirAr
            )
        }

        // 3. Honest Unavailable — never fabricate I'rab or Sarf!
        val immutableTokens = exactImmutableUthmaniText
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        AyahLinguisticAnalysis(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            exactUthmaniAyahText = exactImmutableUthmaniText,
            sentenceSummaryAr = "",
            words = immutableTokens.mapIndexed { idx, w ->
                WordLinguisticAnalysis(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    wordPosition = idx + 1,
                    exactUthmaniWord = w,
                    irab = null,
                    sarf = null
                )
            },
            tafsirMuyassarAr = cachedTafsirAr,
            sourceAr = "غير متوفر في القاعدة الموثّقة المحلية حالياً",
            sourceEn = "Not available in verified local linguistic dataset",
            verificationStatus = LinguisticVerificationStatus.UNAVAILABLE
        )
    }

    /**
     * Imports a user-supplied structured JSON I'rab/Sarf linguistic pack offline via Android SAF.
     * Returns the number of Ayah records imported.
     */
    suspend fun importOfflineLinguisticPack(uri: Uri): Int = withContext(Dispatchers.IO) {
        val raw = runCatching {
            appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@withContext 0

        val parsed = runCatching { parseLinguisticPackJson(raw) }.getOrDefault(emptyList())
        if (parsed.isEmpty()) return@withContext 0

        val outFile = File(packsDir, "pack_${System.currentTimeMillis()}.json")
        runCatching { outFile.writeText(raw) }
        for (entry in parsed) {
            importedCache[entry.surahNumber to entry.ayahNumber] = entry
        }
        importedLoaded = true
        parsed.size
    }

    fun verifiedBundledAyahCount(): Int = QuranLinguisticCatalog.verifiedEntries.size

    private fun ensureImportedPacksLoaded() {
        if (importedLoaded) return
        importedLoaded = true
        packsDir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.endsWith(".json", ignoreCase = true)) {
                runCatching { parseLinguisticPackJson(f.readText()) }
                    .getOrDefault(emptyList())
                    .forEach { importedCache[it.surahNumber to it.ayahNumber] = it }
            }
        }
    }

    companion object {
        fun parseLinguisticPackJson(rawJson: String): List<AyahLinguisticAnalysis> {
            val root = JSONObject(rawJson)
            val sourceAr = root.optString("sourceAr", "حزمة إعراب وصرف محلية مستوردة")
            val sourceEn = root.optString("sourceEn", "Imported Local Linguistic Pack")
            val arr = root.optJSONArray("ayahs") ?: JSONArray()
            val out = mutableListOf<AyahLinguisticAnalysis>()

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val surah = obj.optInt("surahNumber", 0)
                val ayah = obj.optInt("ayahNumber", 0)
                if (surah !in 1..114 || ayah <= 0) continue

                val wordsArr = obj.optJSONArray("words") ?: JSONArray()
                val words = mutableListOf<WordLinguisticAnalysis>()
                for (w in 0 until wordsArr.length()) {
                    val wObj = wordsArr.optJSONObject(w) ?: continue
                    val irabObj = wObj.optJSONObject("irab")
                    val sarfObj = wObj.optJSONObject("sarf")
                    words += WordLinguisticAnalysis(
                        surahNumber = surah,
                        ayahNumber = ayah,
                        wordPosition = wObj.optInt("wordPosition", w + 1),
                        exactUthmaniWord = wObj.optString("word", ""),
                        irab = irabObj?.let {
                            WordIrabEntry(
                                grammaticalRoleAr = it.optString("roleAr", ""),
                                grammaticalRoleEn = it.optString("roleEn", ""),
                                caseEndingAr = it.optString("caseAr", ""),
                                relationshipAr = it.optString("relationshipAr", ""),
                                explanationAr = it.optString("explanationAr", "")
                            )
                        },
                        sarf = sarfObj?.let {
                            WordSarfEntry(
                                rootAr = it.optString("rootAr", "—"),
                                patternAr = it.optString("patternAr", "—"),
                                partOfSpeechAr = it.optString("posAr", ""),
                                partOfSpeechEn = it.optString("posEn", ""),
                                verbFormAr = it.optString("verbFormAr", "—"),
                                tenseAr = it.optString("tenseAr", "—"),
                                numberGenderAr = it.optString("numberGenderAr", "—"),
                                derivationAr = it.optString("derivationAr", "—"),
                                notesAr = it.optString("notesAr", "")
                            )
                        }
                    )
                }

                if (words.isNotEmpty()) {
                    out += AyahLinguisticAnalysis(
                        surahNumber = surah,
                        ayahNumber = ayah,
                        exactUthmaniAyahText = obj.optString("exactUthmaniAyahText", ""),
                        sentenceSummaryAr = obj.optString("sentenceSummaryAr", ""),
                        words = words,
                        sourceAr = sourceAr,
                        sourceEn = sourceEn,
                        verificationStatus = LinguisticVerificationStatus.IMPORTED_LOCAL_PACK
                    )
                }
            }
            return out
        }
    }
}
