package com.example.ui.screens.ecosystem

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class MakhrajZone(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val color: Color,
    val normX: Float,
    val normY: Float
) {
    AL_JAWF("jawf", "الجوف", "Oral Cavity (Al-Jawf)", Color(0xFF38BDF8), 0.44f, 0.50f),
    AL_HALQ("halq", "الحلق", "Throat (Al-Halq)", Color(0xFFF59E0B), 0.67f, 0.72f),
    AL_LISAN("lisan", "اللسان", "Tongue (Al-Lisan)", Color(0xFF34D399), 0.43f, 0.57f),
    ASH_SHAFATAN("shafatan", "الشفتان", "Lips (Ash-Shafatan)", Color(0xFFF472B6), 0.16f, 0.54f),
    AL_KHAYSHUM("khayshum", "الخيشوم", "Nasal Passage (Al-Khayshum)", Color(0xFFA78BFA), 0.36f, 0.25f)
}

data class ArabicLetterMakhraj(
    val letter: String,
    val nameAr: String,
    val nameEn: String,
    val zone: MakhrajZone,
    val subMakhrajAr: String,
    val subMakhrajEn: String,
    val pointX: Float,
    val pointY: Float,
    val attributesAr: List<String>,
    val attributesEn: List<String>,
    val tipAr: String,
    val tipEn: String,
    val exampleAyah: String,
    val surahRefAr: String,
    val surahNumber: Int
)

data class TajweedRuleCardData(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val categoryAr: String,
    val categoryEn: String,
    val color: Color,
    val lettersAr: String,
    val explanationAr: String,
    val explanationEn: String,
    val exampleAyah: String,
    val highlightedToken: String,
    val surahNumber: Int
)

data class MakharijQuizQuestion(
    val promptAr: String,
    val promptEn: String,
    val optionsAr: List<String>,
    val optionsEn: List<String>,
    val correctIndex: Int,
    val explanationAr: String,
    val explanationEn: String
)

object TajweedMakharijCatalog {
    val letters: List<ArabicLetterMakhraj> = listOf(
        ArabicLetterMakhraj("أ", "الهمزة والألف", "Hamzah / Alif", MakhrajZone.AL_HALQ, "أقصى الحلق (والألف المدية من الجوف)", "Deepest Throat (Alif Madd from Al-Jawf)", 0.68f, 0.82f, listOf("جهر", "شدة", "استفال", "ترقيق"), listOf("Voiced", "Plosive", "Light"), "تخرج الهمزة بانطباق الوترين الصوتيين في أقصى الحلق دون تشديد مبالغ.", "Produced at the deepest throat cleanly without harsh glottal strain.", "اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ", "العلق: ١", 96),
        ArabicLetterMakhraj("ب", "الباء", "Ba", MakhrajZone.ASH_SHAFATAN, "بانطباق الشفتين معاً", "Closing both lips together", 0.16f, 0.54f, listOf("جهر", "شدة", "قلقلة", "ترقيق"), listOf("Voiced", "Plosive", "Qalqalah", "Light"), "عند سكون الباء تُقلقل بوضوح دون إمالة لفتحة أو كسرة.", "When sukoon, apply clear Qalqalah bounce without adding a vowel.", "تَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ", "المسد: ١", 111),
        ArabicLetterMakhraj("ت", "التاء", "Ta", MakhrajZone.AL_LISAN, "طرف اللسان وأصول الثنايا العليا", "Tip of tongue & upper gum ridge", 0.25f, 0.48f, listOf("همس", "شدة", "استفال", "ترقيق"), listOf("Whispered", "Plosive", "Light"), "يُظهر همس التاء برقة عند السكون والوقف دون مبالغة.", "Show gentle breath (Hams) when paused or carrying sukoon.", "إِذَا الشَّمْسُ كُوِّرَتْ", "التكوير: ١", 81),
        ArabicLetterMakhraj("ث", "الثاء", "Tha", MakhrajZone.AL_LISAN, "طرف اللسان مع أطراف الثنايا العليا", "Tip of tongue with edges of upper teeth", 0.21f, 0.51f, listOf("همس", "رخاوة", "استفال", "ترقيق"), listOf("Whispered", "Fricative", "Light"), "أخرج طرف اللسان قليلاً بين الثنايا لئلا تلتبس بالسين.", "Extend the tongue tip slightly to upper incisors so it never sounds like Seen.", "فَأَمَّا مَن ثَقُلَتْ مَوَازِينُهُ", "القارعة: ٦", 101),
        ArabicLetterMakhraj("ج", "الجيم", "Jeem", MakhrajZone.AL_LISAN, "وسط اللسان مع ما يحاذيه من الحنك الأعلى", "Middle of tongue with hard palate", 0.45f, 0.45f, listOf("جهر", "شدة", "قلقلة", "ترقيق"), listOf("Voiced", "Plosive", "Qalqalah", "Light"), "الجيم الفصيحة شديدة مجهورة لا يجري معها الهواء وتُقلقل ساكنة.", "Classical Jeem is fully blocked (not soft French J) with crisp Qalqalah when sukoon.", "وَالسَّمَاءِ ذَاتِ الْبُرُوجِ", "البروج: ١", 85),
        ArabicLetterMakhraj("ح", "الحاء", "Ha (Throat)", MakhrajZone.AL_HALQ, "وسط الحلق", "Middle of the throat", 0.66f, 0.70f, listOf("همس", "رخاوة", "استفال", "ترقيق"), listOf("Whispered", "Fricative", "Light"), "تخرج بصوت صافٍ نقي من وسط الحلق بلا بحة الخاء.", "Pure breathy pharyngeal sound from the middle throat without scraping.", "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "الفاتحة: ٢", 1),
        ArabicLetterMakhraj("خ", "الخاء", "Kha", MakhrajZone.AL_HALQ, "أدنى الحلق مما يلي الفم", "Upper throat nearest the mouth", 0.62f, 0.58f, listOf("همس", "رخاوة", "استعلاء", "تفخيم"), listOf("Whispered", "Fricative", "Heavy"), "حرف مفخّم مستعلٍ يخرج من أدنى الحلق دون تكلف الشخير.", "Elevated heavy letter from upper throat without harsh rasp.", "خَلَقَ الْإِنسَانَ مِنْ عَلَقٍ", "العلق: ٢", 96),
        ArabicLetterMakhraj("د", "الدال", "Dal", MakhrajZone.AL_LISAN, "طرف اللسان وأصول الثنايا العليا", "Tip of tongue & upper gum ridge", 0.25f, 0.49f, listOf("جهر", "شدة", "قلقلة", "ترقيق"), listOf("Voiced", "Plosive", "Qalqalah", "Light"), "تُقلقل الدال الساكنة بوضوح وخاصة عند الوقف مثل (أَحَدْ).", "Give clear Qalqalah bounce on sukoon Dal, especially on stops like Ahad.", "قُلْ هُوَ اللَّهُ أَحَدٌ", "الإخلاص: ١", 112),
        ArabicLetterMakhraj("ذ", "الذال", "Dhal", MakhrajZone.AL_LISAN, "طرف اللسان مع أطراف الثنايا العليا", "Tip of tongue & upper incisors edge", 0.21f, 0.52f, listOf("جهر", "رخاوة", "استفال", "ترقيق"), listOf("Voiced", "Fricative", "Light"), "حرف لثوي مرقق يجري معه الصوت بنعومة.", "Interdental voiced light letter with smooth airflow.", "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ فِيهِ", "البقرة: ٢", 2),
        ArabicLetterMakhraj("ر", "الراء", "Ra", MakhrajZone.AL_LISAN, "طرف اللسان مع ظهره قليلاً ولثة العليا", "Tip & slight top of tongue with upper gum", 0.29f, 0.46f, listOf("جهر", "توسط", "انحراف", "تكرير خفيف"), listOf("Voiced", "Moderate", "Avoid Trill"), "احذر من تكرير الراء عدة مرات؛ تُفخّم مع الفتح والضم وتُرقق مع الكسر.", "Avoid heavy rolling trill; heavy with Fatha/Damma, light with Kasra.", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "الفاتحة: ١", 1),
        ArabicLetterMakhraj("ز", "الزاي", "Zay", MakhrajZone.AL_LISAN, "طرف اللسان مع أسفل الصفحة الداخلية للثنايا السفلى", "Tip of tongue near lower incisors", 0.23f, 0.57f, listOf("جهر", "رخاوة", "صفير", "ترقيق"), listOf("Voiced", "Fricative", "Whistle"), "تتميز بصفة الصفير مع جهر واضح من غير إخراج اللسان.", "Distinct whistling sibilant with clear voicing and tongue kept inside.", "إِذَا زُلْزِلَتِ الْأَرْضُ زِلْزَالَهَا", "الزلزلة: ١", 99),
        ArabicLetterMakhraj("س", "السين", "Seen", MakhrajZone.AL_LISAN, "طرف اللسان مع الثنايا السفلى", "Tip of tongue near lower teeth", 0.23f, 0.56f, listOf("همس", "رخاوة", "صفير", "ترقيق"), listOf("Whispered", "Fricative", "Whistle"), "صفير نقي مرقق لا يُفخم ولو جاور حرف استعلاء.", "Crisp light whistling sound; keep light even next to heavy letters.", "سَبِّحِ اسْمَ رَبِّكَ الْأَعْلَى", "الأعلى: ١", 87),
        ArabicLetterMakhraj("ش", "الشين", "Sheen", MakhrajZone.AL_LISAN, "وسط اللسان مع الحنك الأعلى", "Middle of tongue with hard palate", 0.45f, 0.46f, listOf("همس", "رخاوة", "تفشٍّ", "ترقيق"), listOf("Whispered", "Fricative", "Tafashshi"), "تتميز بصفة التفشي أي انتشار الهواء داخل الفم بلطف.", "Uniquely has Tafashshi—gentle diffusion of air throughout the mouth.", "أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ", "الشرح: ١", 94),
        ArabicLetterMakhraj("ص", "الصاد", "Sad", MakhrajZone.AL_LISAN, "طرف اللسان مع الثنايا السفلى مع استعلاء أقصى اللسان", "Tongue tip at lower teeth + raised back tongue", 0.24f, 0.55f, listOf("همس", "رخاوة", "إطباق", "صفير", "تفخيم"), listOf("Heavy", "Emphatic", "Whistle"), "ارفع أقصى اللسان دون ضم الشفتين لتحقيق تفخيم الصاد النقي.", "Raise the back of the tongue without rounding the lips for pure heaviness.", "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "الفاتحة: ٦", 1),
        ArabicLetterMakhraj("ض", "الضاد", "Dad", MakhrajZone.AL_LISAN, "إحدى حافتي اللسان أو كلتاهما مع الأضراس العليا", "Side edge(s) of tongue against upper molars", 0.47f, 0.54f, listOf("جهر", "رخاوة", "إطباق", "استطالة", "تفخيم"), listOf("Heavy", "Istitalah", "Unique Arabic"), "أندر حرف في لغات العالم؛ يتميز بالاستطالة وجريان الصوت دون قلقلة.", "Unique to Arabic: uses lateral elongation (Istitalah) without bouncing Qalqalah.", "وَلَا الضَّالِّينَ", "الفاتحة: ٧", 1),
        ArabicLetterMakhraj("ط", "الطاء", "Ta (Emphatic)", MakhrajZone.AL_LISAN, "طرف اللسان وأصول الثنايا العليا مع الإطباق", "Tongue tip at upper gum + full emphasis", 0.26f, 0.48f, listOf("جهر", "شدة", "إطباق", "قلقلة", "أقوى الحروف"), listOf("Strongest Letter", "Heavy", "Qalqalah"), "أقوى حروف الهجاء صفاتٍ؛ مفخّم مطبق يُقلقل بقوة عند السكون.", "Strongest Arabic letter in attributes; emphatic with resonant heavy Qalqalah.", "وَالتِّينِ وَالزَّيْتُونِ وَطُورِ سِينِينَ", "التين: ١-٢", 95),
        ArabicLetterMakhraj("ظ", "الظاء", "Dha (Emphatic)", MakhrajZone.AL_LISAN, "طرف اللسان مع أطراف الثنايا العليا مع الإطباق", "Tongue tip at upper teeth edge + emphasis", 0.22f, 0.51f, listOf("جهر", "رخاوة", "إطباق", "تفخيم"), listOf("Voiced", "Heavy", "Emphatic"), "أخت الذال لكنها مفخّمة مطبقة مع استعلاء أقصى اللسان.", "The emphatic heavy counterpart of Dhal with raised back tongue.", "وَمَا اللَّهُ بِغَافِلٍ عَمَّا تَعْمَلُونَ", "البقرة: ٧٤", 2),
        ArabicLetterMakhraj("ع", "العين", "Ayn", MakhrajZone.AL_HALQ, "وسط الحلق", "Middle of the throat", 0.66f, 0.69f, listOf("جهر", "توسط", "استفال", "ترقيق"), listOf("Voiced", "Moderate", "Light"), "حرف بيني متوسط لا ينقطع صوته فجأة ولا يجري طويلاً.", "Moderate pharyngeal letter from mid-throat—neither cut off nor overly prolonged.", "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "الفاتحة: ٥", 1),
        ArabicLetterMakhraj("غ", "الغين", "Ghayn", MakhrajZone.AL_HALQ, "أدنى الحلق", "Upper throat", 0.62f, 0.59f, listOf("جهر", "رخاوة", "استعلاء", "تفخيم"), listOf("Voiced", "Fricative", "Heavy"), "تخرج من أدنى الحلق بسلاسة مع التفخيم دون قلقلة عند السكون.", "Smooth voiced heavy sound from upper throat; avoid Qalqalah when sukoon.", "غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ", "الفاتحة: ٧", 1),
        ArabicLetterMakhraj("ف", "الفاء", "Fa", MakhrajZone.ASH_SHAFATAN, "باطن الشفة السفلى مع أطراف الثنايا العليا", "Inner lower lip with upper front teeth", 0.19f, 0.54f, listOf("همس", "رخاوة", "استفال", "ترقيق"), listOf("Whispered", "Fricative", "Light"), "يلامس طرف الثنايا العليا باطن الشفة السفلى مع جريان النفس.", "Upper teeth gently touch the inside of the lower lip with smooth airflow.", "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "الفلق: ١", 113),
        ArabicLetterMakhraj("ق", "القاف", "Qaf", MakhrajZone.AL_LISAN, "أقصى اللسان مع ما يحاذيه من الحنك اللحمي", "Deepest tongue against soft palate", 0.57f, 0.53f, listOf("جهر", "شدة", "استعلاء", "قلقلة", "تفخيم"), listOf("Heavy", "Plosive", "Qalqalah"), "تخرج من أقصى اللسان مع الحنك اللحمي وتُقلقل ساكنة بتفخيم واضح.", "Articulated deep against the soft palate; resonant Qalqalah when sukoon.", "ق ۚ وَالْقُرْآنِ الْمَجِيدِ", "ق: ١", 50),
        ArabicLetterMakhraj("ك", "الكاف", "Kaf", MakhrajZone.AL_LISAN, "أقصى اللسان أسفل مخرج القاف مع الحنك العظمي واللحمي", "Back of tongue slightly forward of Qaf", 0.53f, 0.50f, listOf("همس", "شدة", "استفال", "ترقيق"), listOf("Whispered", "Plosive", "Light"), "مرققة دائماً ويظهر همسها اللطيف بعد فك الانحباس عند السكون.", "Always light; release a gentle puff of breath (Hams) after sukoon closure.", "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "الكوثر: ١", 108),
        ArabicLetterMakhraj("ل", "اللام", "Lam", MakhrajZone.AL_LISAN, "أدنى حافتي اللسان إلى منتهى طرفه مع لثة العليا", "Front edges of tongue to upper gum", 0.31f, 0.46f, listOf("جهر", "توسط", "انحراف", "ترقيق"), listOf("Voiced", "Moderate", "Broadest Makhraj"), "أوسع الحروف مخرجاً؛ تُرقق دائماً إلا في لفظ الجلالة بعد فتح أو ضم.", "Broadest articulation point; always light except in 'Allah' after Fatha/Damma.", "لَمْ يَلِدْ وَلَمْ يُولَدْ", "الإخلاص: ٣", 112),
        ArabicLetterMakhraj("م", "الميم", "Meem", MakhrajZone.ASH_SHAFATAN, "الشفتان مع غنة من الخيشوم", "Both lips + nasal resonance (Ghunnah)", 0.16f, 0.52f, listOf("جهر", "توسط", "غنة من الخيشوم", "ترقيق"), listOf("Voiced", "Nasal Ghunnah", "Light"), "تجمع بين الشفتين والخيشوم؛ أكمل ما تكون غنتها إذا كانت مشددة.", "Combines lips and nasal passage; richest 2-count Ghunnah when doubled (Mushaddadah).", "عَمَّ يَتَسَاءَلُونَ", "النبأ: ١", 78),
        ArabicLetterMakhraj("ن", "النون", "Noon", MakhrajZone.AL_KHAYSHUM, "طرف اللسان مع لثة العليا + الخيشوم للغنة", "Tongue tip at upper gum + Nasal Cavity", 0.36f, 0.25f, listOf("جهر", "توسط", "غنة من الخيشوم", "ترقيق"), listOf("Voiced", "Nasal Ghunnah", "Light"), "الخيشوم هو مخرج الغنة الملازمة للنون والميم، وتُمد حركتين في المشددة.", "Al-Khayshum produces the signature Ghunnah of Noon & Meem (2 counts when doubled).", "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ", "القدر: ١", 97),
        ArabicLetterMakhraj("هـ", "الهاء", "Ha (Deep Throat)", MakhrajZone.AL_HALQ, "أقصى الحلق", "Deepest throat", 0.69f, 0.80f, listOf("همس", "رخاوة", "خفاء", "ترقيق"), listOf("Whispered", "Soft", "Light"), "حرف ضعيف يحتاج عناية ببيانه خاصة عند الوقف والسكون.", "Delicate deep-throat letter; articulate clearly so it does not fade at stops.", "وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "الملك: ١", 67),
        ArabicLetterMakhraj("و", "الواو", "Waw", MakhrajZone.ASH_SHAFATAN, "بانضمام الشفتين للواو المتحركة (والمدية من الجوف)", "Rounding lips (Waw Madd from Al-Jawf)", 0.15f, 0.56f, listOf("جهر", "رخاوة", "استفال", "ترقيق"), listOf("Voiced", "Rounded Lips", "Light"), "تُضم الشفتان مع إبقاء فرجة صغيرة لمرور الصوت بنقاء.", "Round both lips forward leaving a small opening for clean flow.", "وَالضُّحَىٰ ۝ وَاللَّيْلِ إِذَا سَجَىٰ", "الضحى: ١-٢", 93),
        ArabicLetterMakhraj("ي", "الياء", "Ya", MakhrajZone.AL_JAWF, "وسط اللسان للمتحركة واللينة، والجوف للياء المدية", "Al-Jawf for Madd Ya; mid-tongue for vowelized Ya", 0.44f, 0.50f, listOf("جهر", "رخاوة", "استفال", "ترقيق"), listOf("Voiced", "Al-Jawf Madd", "Light"), "حروف المد الثلاثة (ا و ي) تخرج من الجوف وتنتهي بانقطاع الهواء.", "The three Madd letters (Alif, Waw, Ya) resonate openly through Al-Jawf.", "يَا أَيُّهَا الَّذِينَ آمَنُوا", "البقرة: ١٥٣", 2)
    )

    val rules: List<TajweedRuleCardData> = listOf(
        TajweedRuleCardData("izhar", "الإظهار الحلقي", "Throat Clarification (Izhar)", "النون الساكنة والتنوين", "Noon Sakinah", Color(0xFF38BDF8), "ء هـ ع ح غ خ", "نطق النون الساكنة أو التنوين بوضوح تام من غير غنة زائدة إذا جاء بعدها أحد حروف الحلق الستة.", "Pronounce Noon Sakinah / Tanween clearly without extra Ghunnah before the 6 throat letters.", "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ", "أَنْعَمْتَ", 1),
        TajweedRuleCardData("idgham", "الإدغام بغنة (ينمو)", "Merging with Ghunnah (Idgham)", "النون الساكنة والتنوين", "Noon Sakinah", Color(0xFF34D399), "ي ن م و", "إدخال النون الساكنة في الحرف التالي بحيث يصيران حرفاً مشدداً مع غنة بمقدار حركتين.", "Merge Noon Sakinah smoothly into Yaa, Noon, Meem, or Waw with a 2-count nasal Ghunnah.", "فَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَرَهُ", "مَن يَعْمَلْ", 99),
        TajweedRuleCardData("iqlab", "الإقلاب", "Conversion (Iqlab)", "النون الساكنة والتنوين", "Noon Sakinah", Color(0xFFA78BFA), "ب", "قلب النون الساكنة أو التنوين ميماً مخفاة عند الباء مع غنة بمقدار حركتين وتلامس لطيف للشفتين.", "Convert Noon Sakinah / Tanween into a hidden Meem before Ba with 2-count Ghunnah.", "مِن بَعْدِ مَا جَاءَتْهُمُ الْبَيِّنَاتُ", "مِن بَعْدِ", 2),
        TajweedRuleCardData("ikhfa", "الإخفاء الحقيقي", "Concealment (Ikhfa)", "النون الساكنة والتنوين", "Noon Sakinah", Color(0xFFF59E0B), "ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ", "النطق بالنون الساكنة بحالة وسط بين الإظهار والإدغام مع بقاء الغنة حركتين وتفخيمها أو ترقيقها حسب الحرف التالي.", "Conceal Noon Sakinah between clear and merged with 2-count Ghunnah shaped toward the next letter.", "وَمَن شَكَرَ فَإِنَّمَا يَشْكُرُ لِنَفْسِهِ", "مَن شَكَرَ", 27),
        TajweedRuleCardData("qalqalah", "القلقلة (قطب جد)", "Echoing Bounce (Qalqalah)", "صفات الحروف", "Letter Attributes", Color(0xFFF472B6), "ق ط ب ج د", "اضطراب المخرج عند النطق بأحد حروف (قطب جد) ساكناً حتى يُسمع له نبرة قوية واضحة.", "Resonant echoing release when any of the 5 letters (Qaf, Ta, Ba, Jeem, Dal) carries sukoon.", "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ", "الْفَلَقِ", 113),
        TajweedRuleCardData("madd_muttasil", "المد الواجب المتصل", "Connected Obligatory Madd", "أحكام المدود", "Elongation Rules", Color(0xFFFBBF24), "حرف مد + همزة في نفس الكلمة", "يمد وجوباً بمقدار ٤ أو ٥ حركات عند اجتماع حرف المد والهمزة في كلمة واحدة.", "Elongate 4–5 counts when a Madd letter is followed by Hamzah within the same word.", "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ", "جَاءَ", 110)
    )

    val quizQuestions: List<MakharijQuizQuestion> = listOf(
        MakharijQuizQuestion(
            "ما المخرج العام لحروف (ء، هـ، ع، ح، غ، خ)؟",
            "Which major articulation zone produces (ء، هـ، ع، ح، غ، خ)?",
            listOf("الحلق", "الشفتان", "الجوف", "الخيشوم"),
            listOf("Throat (Al-Halq)", "Lips (Ash-Shafatan)", "Oral Cavity (Al-Jawf)", "Nasal Cavity (Al-Khayshum)"),
            0,
            "تُسمى بالحروف الحلقية وتخرج من أقصى الحلق ووسطه وأدناه.",
            "These 6 letters are the Throat Letters (Al-Halq), articulated from deep, middle, and upper throat."
        ),
        MakharijQuizQuestion(
            "أي الحروف التالية يتميز بصفة الاستطالة ويخرج من حافة اللسان مع الأضراس العليا؟",
            "Which letter uniquely has Istitalah (lateral elongation) from the side of the tongue?",
            listOf("الصاد (ص)", "الضاد (ض)", "الظاء (ظ)", "الشين (ش)"),
            listOf("Sad (ص)", "Dad (ض)", "Dha (ظ)", "Sheen (ش)"),
            1,
            "حرف الضاد (ض) ينفرد بصفة الاستطالة ولذلك سُميت العربية بلغة الضاد.",
            "Dad (ض) is the only letter with Istitalah, giving Arabic the title 'Language of Dad'."
        ),
        MakharijQuizQuestion(
            "ما الحكم التجويدي في قوله تعالى: (مِن بَعْدِ)؟",
            "What is the Tajweed rule in (مِن بَعْدِ)?",
            listOf("إظهار حلقي", "إدغام بغنة", "إقلاب", "إخفاء شفوي"),
            listOf("Izhar Halqi", "Idgham with Ghunnah", "Iqlab", "Ikhfa Shafawi"),
            2,
            "جاءت الباء بعد النون الساكنة فيتحقق حكم الإقلاب بقلب النون ميماً مخفاة مع غنة حركتين.",
            "Noon Sakinah followed by Ba triggers Iqlab—converting Noon into a hidden Meem with 2-count Ghunnah."
        ),
        MakharijQuizQuestion(
            "من أين تخرج الغنّة الملازمة للنون والميم؟",
            "Where does the nasal Ghunnah of Noon and Meem originate?",
            listOf("الجوف", "الخيشوم", "أقصى الحلق", "طرف اللسان"),
            listOf("Al-Jawf", "Al-Khayshum (Nasal Passage)", "Deepest Throat", "Tip of Tongue"),
            1,
            "الخيشوم هو أقصى الأنف المنجذب إلى داخل الفم وهو المخرج الوحيد للغنة.",
            "Al-Khayshum (the nasal cavity) is the exclusive articulation point for Ghunnah."
        ),
        MakharijQuizQuestion(
            "كم عدد حروف القلقلة المجموعة في عبارة (قطب جد)؟",
            "How many letters belong to Qalqalah (قطب جد)?",
            listOf("٤ حروف", "٥ حروف", "٦ حروف", "٧ حروف"),
            listOf("4 Letters", "5 Letters", "6 Letters", "7 Letters"),
            1,
            "حروف القلقلة خمسة مجموعة في (قُطْبُ جَدٍّ): القاف والطاء والباء والجيم والدال.",
            "There are 5 Qalqalah letters remembered by the phrase Qutbu Jadd: Qaf, Ta, Ba, Jeem, Dal."
        )
    )
}

@Composable
fun TajweedMakharijLabScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Makharij Canvas, 1 = Rules Studio, 2 = Quiz
    var selectedZoneFilter by remember { mutableStateOf<MakhrajZone?>(null) }
    var selectedLetter by remember { mutableStateOf(TajweedMakharijCatalog.letters.first { it.letter == "ض" }) }

    var quizIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    val filteredLetters = remember(selectedZoneFilter) {
        if (selectedZoneFilter == null) TajweedMakharijCatalog.letters
        else TajweedMakharijCatalog.letters.filter { it.zone == selectedZoneFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tajweed_makharij_lab_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مختبر المخارج والتجويد التفاعلي", "Makharij & Tajweed Master Lab"),
            subtitle = L10n.pick(
                lang,
                "خريطة تشريحية تفاعلية لـ ٢٨ حرفاً و٥ مخارج رئيسية وأحكام التجويد الملوّنة",
                "Interactive Vocal Tract Atlas for 28 Letters, 5 Major Zones & Color-Coded Tajweed"
            ),
            onBack = onBack
        )

        // Mode Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "خريطة المخارج", "Makharij Map"),
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f).testTag("makharij_tab_map")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "أحكام التجويد", "Tajweed Rules"),
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f).testTag("makharij_tab_rules")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "اختبار الإتقان", "Mastery Quiz"),
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f).testTag("makharij_tab_quiz")
            )
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Anatomical Interactive Vocal Tract Diagram
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = L10n.pick(lang, "المخرج التشريحي الحي", "Live Anatomical Point"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = noorColors.goldAccent
                                    )
                                    Text(
                                        text = L10n.pick(
                                            lang,
                                            "${selectedLetter.zone.nameAr} · ${selectedLetter.nameAr}",
                                            "${selectedLetter.zone.nameEn} · ${selectedLetter.nameEn}"
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(selectedLetter.zone.color.copy(alpha = 0.22f))
                                        .border(1.5.dp, selectedLetter.zone.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedLetter.letter,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            VocalTractAnatomicalCanvas(
                                activeLetter = selectedLetter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF04130F))
                                    .border(1.dp, selectedLetter.zone.color.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Legend pills for the 5 Major Zones
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, "الكل (٢٨)", "All (28)"),
                                        selected = selectedZoneFilter == null,
                                        onClick = { selectedZoneFilter = null }
                                    )
                                }
                                items(MakhrajZone.entries) { zone ->
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, zone.nameAr, zone.nameEn),
                                        selected = selectedZoneFilter == zone,
                                        onClick = {
                                            selectedZoneFilter = zone
                                            TajweedMakharijCatalog.letters.firstOrNull { it.zone == zone }?.let {
                                                selectedLetter = it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 28 Arabic Letters Selector Grid
                    item {
                        LiquidGlassCard {
                            Text(
                                text = L10n.pick(lang, "اختر الحرف لمعاينة مخرجه وصفاته", "Tap any Arabic Letter to Inspect"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredLetters.chunked(7).forEach { rowLetters ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowLetters.forEach { item ->
                                            val isSelected = item.letter == selectedLetter.letter
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) item.zone.color.copy(alpha = 0.34f)
                                                        else Color.White.copy(alpha = 0.06f)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.6.dp else 1.dp,
                                                        color = if (isSelected) item.zone.color else Color.White.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedLetter = item },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.letter,
                                                    fontFamily = AmiriFontFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        repeat(7 - rowLetters.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Detailed Scholarly Breakdown Card for Selected Letter
                    item {
                        LiquidGlassCard(goldBorder = true) {
                            Text(
                                text = L10n.pick(lang, "المخرج الخاص: ${selectedLetter.subMakhrajAr}", "Exact Point: ${selectedLetter.subMakhrajEn}"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = selectedLetter.zone.color
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Sifaat Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val attrs = L10n.pick(lang, selectedLetter.attributesAr.joinToString("|"), selectedLetter.attributesEn.joinToString("|")).split("|")
                                items(attrs) { attr ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(selectedLetter.zone.color.copy(alpha = 0.16f))
                                            .border(1.dp, selectedLetter.zone.color.copy(alpha = 0.45f), RoundedCornerShape(50))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = attr,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = selectedLetter.zone.color
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(lang, selectedLetter.tipAr, selectedLetter.tipEn),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = selectedLetter.exampleAyah,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        color = GoldBright,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedLetter.surahRefAr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = L10n.pick(lang, "افتح في المصحف ←", "Open in Mushaf →"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBright,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onOpenSurah(selectedLetter.surahNumber) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(TajweedMakharijCatalog.rules, key = { it.id }) { rule ->
                        LiquidGlassCard(goldBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, rule.categoryAr, rule.categoryEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = rule.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(lang, rule.titleAr, rule.titleEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(rule.color.copy(alpha = 0.18f))
                                        .border(1.dp, rule.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = rule.lettersAr,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = rule.color
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = L10n.pick(lang, rule.explanationAr, rule.explanationEn),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(rule.color.copy(alpha = 0.10f))
                                    .border(1.dp, rule.color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = rule.exampleAyah,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 21.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = L10n.pick(lang, "موضع الحكم: ${rule.highlightedToken}", "Focus: ${rule.highlightedToken}"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = rule.color
                                        )
                                        Text(
                                            text = "·",
                                            color = GoldBright
                                        )
                                        Text(
                                            text = L10n.pick(lang, "افتح السورة", "Open Surah"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBright,
                                            modifier = Modifier.clickable { onOpenSurah(rule.surahNumber) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                val questions = TajweedMakharijCatalog.quizQuestions
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            if (quizCompleted) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(44.dp))
                                    Text(
                                        text = L10n.pick(lang, "نتيجتك في اختبار المخارج والتجويد", "Tajweed Mastery Score"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright
                                    )
                                    Text(
                                        text = "$quizScore / ${questions.size}",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF34D399)
                                    )
                                    Button(
                                        onClick = {
                                            quizIndex = 0
                                            selectedOption = null
                                            quizScore = 0
                                            quizCompleted = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(L10n.pick(lang, "إعادة الاختبار", "Retry Quiz"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val q = questions[quizIndex]
                                val options = L10n.pick(lang, q.optionsAr.joinToString("|"), q.optionsEn.joinToString("|")).split("|")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = L10n.pick(lang, "السؤال ${quizIndex + 1} من ${questions.size}", "Question ${quizIndex + 1} of ${questions.size}"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = noorColors.goldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(lang, "النقاط: $quizScore", "Score: $quizScore"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = L10n.pick(lang, q.promptAr, q.promptEn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 26.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    options.forEachIndexed { idx, optText ->
                                        val isPicked = selectedOption == idx
                                        val isCorrect = idx == q.correctIndex
                                        val bg = when {
                                            selectedOption == null -> Color.White.copy(alpha = 0.06f)
                                            isCorrect -> Color(0xFF10B981).copy(alpha = 0.28f)
                                            isPicked -> Color(0xFFEF4444).copy(alpha = 0.28f)
                                            else -> Color.White.copy(alpha = 0.04f)
                                        }
                                        val border = when {
                                            selectedOption == null -> Color.White.copy(alpha = 0.16f)
                                            isCorrect -> Color(0xFF34D399)
                                            isPicked -> Color(0xFFF87171)
                                            else -> Color.White.copy(alpha = 0.10f)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(bg)
                                                .border(1.2.dp, border, RoundedCornerShape(14.dp))
                                                .clickable(enabled = selectedOption == null) {
                                                    selectedOption = idx
                                                    if (idx == q.correctIndex) quizScore++
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = optText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            if (selectedOption != null && isCorrect) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                                            }
                                        }
                                    }
                                }
                                if (selectedOption != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = L10n.pick(lang, q.explanationAr, q.explanationEn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldBright
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (quizIndex + 1 < questions.size) {
                                                quizIndex++
                                                selectedOption = null
                                            } else {
                                                quizCompleted = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                    ) {
                                        Text(
                                            text = if (quizIndex + 1 < questions.size) L10n.pick(lang, "السؤال التالي", "Next Question")
                                            else L10n.pick(lang, "عرض النتيجة", "Show Result"),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocalTractAnatomicalCanvas(
    activeLetter: ArabicLetterMakhraj,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "makhraj_beacon")
    val pulse by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle anatomical grid
        val gridColor = Color(0xFF1E4D3E).copy(alpha = 0.25f)
        for (i in 1..7) {
            val x = w * i / 8f
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (j in 1..4) {
            val y = h * j / 5f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Stylized side-profile silhouette of nasal passage (Al-Khayshum), oral cavity (Al-Jawf),
        // tongue (Al-Lisan), lips (Ash-Shafatan), and throat (Al-Halq)
        val nasalPath = Path().apply {
            moveTo(w * 0.16f, h * 0.34f)
            cubicTo(w * 0.26f, h * 0.16f, w * 0.48f, h * 0.15f, w * 0.58f, h * 0.33f)
            cubicTo(w * 0.52f, h * 0.36f, w * 0.34f, h * 0.36f, w * 0.20f, h * 0.38f)
            close()
        }
        drawPath(nasalPath, color = MakhrajZone.AL_KHAYSHUM.color.copy(alpha = if (activeLetter.zone == MakhrajZone.AL_KHAYSHUM) 0.30f else 0.10f))
        drawPath(nasalPath, color = MakhrajZone.AL_KHAYSHUM.color.copy(alpha = 0.65f), style = Stroke(width = 2.2f))

        // Hard & soft palate arch + throat back wall
        val palateAndThroat = Path().apply {
            moveTo(w * 0.18f, h * 0.45f)
            cubicTo(w * 0.32f, h * 0.36f, w * 0.56f, h * 0.37f, w * 0.66f, h * 0.54f)
            lineTo(w * 0.72f, h * 0.90f)
        }
        drawPath(palateAndThroat, color = Color.White.copy(alpha = 0.45f), style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Throat channel (Al-Halq)
        val throatPath = Path().apply {
            moveTo(w * 0.60f, h * 0.56f)
            lineTo(w * 0.66f, h * 0.54f)
            lineTo(w * 0.73f, h * 0.90f)
            lineTo(w * 0.62f, h * 0.90f)
            close()
        }
        drawPath(throatPath, color = MakhrajZone.AL_HALQ.color.copy(alpha = if (activeLetter.zone == MakhrajZone.AL_HALQ) 0.30f else 0.10f))
        drawPath(throatPath, color = MakhrajZone.AL_HALQ.color.copy(alpha = 0.60f), style = Stroke(width = 2f))

        // Tongue body (Al-Lisan)
        val tonguePath = Path().apply {
            moveTo(w * 0.22f, h * 0.62f)
            cubicTo(w * 0.30f, h * 0.49f, w * 0.49f, h * 0.48f, w * 0.59f, h * 0.60f)
            cubicTo(w * 0.61f, h * 0.72f, w * 0.45f, h * 0.76f, w * 0.25f, h * 0.70f)
            close()
        }
        drawPath(tonguePath, color = MakhrajZone.AL_LISAN.color.copy(alpha = if (activeLetter.zone == MakhrajZone.AL_LISAN) 0.32f else 0.12f))
        drawPath(tonguePath, color = MakhrajZone.AL_LISAN.color.copy(alpha = 0.75f), style = Stroke(width = 2.4f))

        // Lips (Ash-Shafatan)
        drawCircle(
            color = MakhrajZone.ASH_SHAFATAN.color.copy(alpha = if (activeLetter.zone == MakhrajZone.ASH_SHAFATAN) 0.40f else 0.18f),
            radius = h * 0.08f,
            center = Offset(w * 0.16f, h * 0.54f)
        )
        drawCircle(
            color = MakhrajZone.ASH_SHAFATAN.color.copy(alpha = 0.85f),
            radius = h * 0.08f,
            center = Offset(w * 0.16f, h * 0.54f),
            style = Stroke(width = 2.2f)
        )

        // All 5 Zone Anchor Dots
        MakhrajZone.entries.forEach { zone ->
            val pt = Offset(w * zone.normX, h * zone.normY)
            drawCircle(color = zone.color.copy(alpha = 0.35f), radius = 6f, center = pt)
        }

        // Exact Active Letter Articulation Point Pulsing Beacon
        val activeCenter = Offset(w * activeLetter.pointX, h * activeLetter.pointY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeLetter.zone.color.copy(alpha = 0.55f), Color.Transparent),
                center = activeCenter,
                radius = 34f * pulse
            ),
            radius = 34f * pulse,
            center = activeCenter
        )
        drawCircle(
            color = activeLetter.zone.color,
            radius = 12f,
            center = activeCenter,
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = GoldBright,
            radius = 6.5f,
            center = activeCenter
        )
    }
}
