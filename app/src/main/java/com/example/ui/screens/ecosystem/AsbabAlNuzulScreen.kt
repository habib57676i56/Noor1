package com.example.ui.screens.ecosystem

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

data class AsbabNuzulEntry(
    val id: String,
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val ayahRefAr: String,
    val ayahRefEn: String,
    val isMakki: Boolean,
    val themeKey: String,
    val themeAr: String,
    val themeEn: String,
    val ayahTextAr: String,
    val occasionAr: String,
    val occasionEn: String,
    val lessonAr: String,
    val lessonEn: String,
    val sourceAr: String,
    val sourceEn: String
)

object AsbabAlNuzulCatalog {
    val entries: List<AsbabNuzulEntry> = listOf(
        AsbabNuzulEntry(
            id = "duha_1_3",
            surahNumber = 93,
            surahNameAr = "سورة الضحى",
            surahNameEn = "Surah Ad-Duha",
            ayahRefAr = "الضحى: ١–٣",
            ayahRefEn = "93:1–3",
            isMakki = true,
            themeKey = "comfort",
            themeAr = "البشارة والرحمة",
            themeEn = "Comfort & Mercy",
            ayahTextAr = "وَالضُّحَىٰ ۝ وَاللَّيْلِ إِذَا سَجَىٰ ۝ مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ",
            occasionAr = "اشتكى النبي ﷺ فلم يقم ليلةً أو ليلتين، فجاءت امرأة فقالت: يا محمد، ما أرى شيطانك إلا قد تركك! فأنزل الله تعالى سورة الضحى تسليةً وتثبيتاً لقلب رسوله ﷺ.",
            occasionEn = "When the Prophet ﷺ fell ill and could not stand for night prayer for a night or two, a woman taunted him claiming revelation had forsaken him. Allah revealed Surah Ad-Duha to reassure and honor him.",
            lessonAr = "فترات الضيق والانقطاع المؤقت لا تعني أبداً تخلّي الله عن عبده، بل يعقبها فرج وعطاء أعظم.",
            lessonEn = "Periods of hardship or delay never mean Allah has abandoned His servant; greater relief always follows.",
            sourceAr = "صحيح البخاري (٤٩٥٠) وصحيح مسلم (١٧٩٧)",
            sourceEn = "Sahih al-Bukhari 4950 & Sahih Muslim 1797"
        ),
        AsbabNuzulEntry(
            id = "ikhlas_1_4",
            surahNumber = 112,
            surahNameAr = "سورة الإخلاص",
            surahNameEn = "Surah Al-Ikhlas",
            ayahRefAr = "الإخلاص: ١–٤",
            ayahRefEn = "112:1–4",
            isMakki = true,
            themeKey = "tawhid",
            themeAr = "التوحيد والعقيدة",
            themeEn = "Tawhid & Creed",
            ayahTextAr = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
            occasionAr = "قال المشركون لرسول الله ﷺ: انسب لنا ربك! فأنزل الله تعالى سورة الإخلاص التي تعدل ثلث القرآن في بيان صفات الربوبية الخالصة.",
            occasionEn = "The polytheists asked the Prophet ﷺ: 'Describe the lineage of your Lord to us!' Allah revealed Surah Al-Ikhlas, equal to one-third of the Quran in declaring pure monotheism.",
            lessonAr = "أعظم جواب على الشبهات هو الوضوح التام في توحيد الله وكماله واستغنائه عن كل شيء.",
            lessonEn = "The clearest answer to doubts is pure, unambiguous affirmation of Allah's Oneness and perfection.",
            sourceAr = "سنن الترمذي (٣٣٦٤) ومسند أحمد — صحيح",
            sourceEn = "Jami at-Tirmidhi 3364 & Musnad Ahmad (Sahih)"
        ),
        AsbabNuzulEntry(
            id = "kawthar_1_3",
            surahNumber = 108,
            surahNameAr = "سورة الكوثر",
            surahNameEn = "Surah Al-Kawthar",
            ayahRefAr = "الكوثر: ١–٣",
            ayahRefEn = "108:1–3",
            isMakki = true,
            themeKey = "comfort",
            themeAr = "البشارة والرحمة",
            themeEn = "Comfort & Mercy",
            ayahTextAr = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۝ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۝ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
            occasionAr = "لما توفي ابن النبي ﷺ عيّره العاص بن وائل وقال: هو أبتر لا عقب له إذا مات انقطع ذكره! فأنزل الله البشارة بنهر الكوثر والذكر الخالد.",
            occasionEn = "After the passing of the Prophet's ﷺ son, Al-'As ibn Wa'il mocked him as 'cut off without legacy.' Allah revealed Surah Al-Kawthar promising eternal abundance and remembrance.",
            lessonAr = "العبرة ليست بضجيج الشامتين، بل بالعمل الصالح والذكر الحسن الذي يبارك الله فيه أبد الدهر.",
            lessonEn = "True legacy is never measured by critics' taunts, but by righteous deeds blessed forever by Allah.",
            sourceAr = "تفسير الطبري وابن كثير وأسباب النزول للواحدي",
            sourceEn = "Tafsir at-Tabari, Ibn Kathir & Al-Wahidi"
        ),
        AsbabNuzulEntry(
            id = "baqarah_186",
            surahNumber = 2,
            surahNameAr = "سورة البقرة",
            surahNameEn = "Surah Al-Baqarah",
            ayahRefAr = "البقرة: ١٨٦",
            ayahRefEn = "2:186",
            isMakki = false,
            themeKey = "comfort",
            themeAr = "البشارة والرحمة",
            themeEn = "Comfort & Mercy",
            ayahTextAr = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            occasionAr = "جاء أعرابي إلى النبي ﷺ فقال: يا رسول الله، أقريبٌ ربنا فنناجيه أم بعيد فنناديه؟ فسكت النبي ﷺ فأنزل الله هذه الآية العظيمة بلا واسطة (فإني قريب).",
            occasionEn = "A Bedouin asked the Prophet ﷺ: 'Is our Lord near so we whisper to Him, or far so we call out loudly?' Allah directly answered without even 'Say': 'Indeed, I am near.'",
            lessonAr = "لا حجاب بين قلب الداعي وربّه؛ أقرب ما يكون العبد إلى الله حين يهمس بدعائه صادقاً.",
            lessonEn = "There is no barrier between a sincere heart and Allah; a quiet whispered Du'a reaches Him immediately.",
            sourceAr = "تفسير ابن أبي حاتم وابن كثير (صحيح الإسناد بمعناه)",
            sourceEn = "Tafsir Ibn Abi Hatim & Ibn Kathir"
        ),
        AsbabNuzulEntry(
            id = "mujadilah_1",
            surahNumber = 58,
            surahNameAr = "سورة المجادلة",
            surahNameEn = "Surah Al-Mujadilah",
            ayahRefAr = "المجادلة: ١",
            ayahRefEn = "58:1",
            isMakki = false,
            themeKey = "family",
            themeAr = "الأسرة والمجتمع",
            themeEn = "Family & Justice",
            ayahTextAr = "قَدْ سَمِعَ اللَّهُ قَوْلَ الَّتِي تُجَادِلُكَ فِي زَوْجِهَا وَتَشْتَكِي إِلَى اللَّهِ",
            occasionAr = "جاءت خولة بنت ثعلبة رضي الله عنها تشتكي ظهار زوجها أوس بن الصامت وتبكي خوفاً على ضياع أولادها، فقالت عائشة: سبحان الذي وسع سمعه الأصوات، لقد كانت تكلم النبي ﷺ ويخفى عليّ بعض كلامها فأنزل الله الآية من فوق سبع سماوات.",
            occasionEn = "Khawlah bint Tha'labah (RA) pleaded with the Prophet ﷺ regarding her husband's oath of Zihar, fearing for her family. Aisha (RA) marveled that even as some words were too quiet to hear across the room, Allah heard every word from above the seven heavens.",
            lessonAr = "الله يسمع أنين المظلومة وشكوى الصادقين ولو خفيت أصواتهم عن أقرب الناس إليهم.",
            lessonEn = "Allah hears the quietest plea of the distressed even when those right beside them cannot.",
            sourceAr = "صحيح البخاري تعليقاً وسنن ابن ماجه (٢٠٦٣) — صحيح",
            sourceEn = "Sahih al-Bukhari & Sunan Ibn Majah 2063 (Sahih)"
        ),
        AsbabNuzulEntry(
            id = "baqarah_144",
            surahNumber = 2,
            surahNameAr = "سورة البقرة",
            surahNameEn = "Surah Al-Baqarah",
            ayahRefAr = "البقرة: ١٤٤",
            ayahRefEn = "2:144",
            isMakki = false,
            themeKey = "legislation",
            themeAr = "التشريع والعبادة",
            themeEn = "Worship & Law",
            ayahTextAr = "قَدْ نَرَىٰ تَقَلُّبَ وَجْهِكَ فِي السَّمَاءِ ۖ فَلَنُوَلِّيَنَّكَ قِبْلَةً تَرْضَاهَا ۚ فَوَلِّ وَجْهَكَ شَطْرَ الْمَسْجِدِ الْحَرَامِ",
            occasionAr = "صلّى النبي ﷺ بالمدينة نحو بيت المقدس ستة عشر أو سبعة عشر شهراً، وكان يحب أن يوجَّه نحو الكعبة المشرفة ويقلّب بصره في السماء رجاءً، فأكرمه الله بتحويل القبلة إلى المسجد الحرام.",
            occasionEn = "After migrating to Madinah, the Prophet ﷺ prayed toward Jerusalem for 16–17 months while yearning in his heart for the Kaaba. Allah honored his quiet longing by turning the Qibla permanently to Makkah.",
            lessonAr = "الامتثال لأمر الله مع حسن الرجاء يفتح أبواب الكرامة والرضا الإلهي.",
            lessonEn = "Obedience paired with hopeful patience opens doors to divine honor and fulfillment.",
            sourceAr = "صحيح البخاري (٤٤٨٦) وصحيح مسلم (٥٢٥)",
            sourceEn = "Sahih al-Bukhari 4486 & Sahih Muslim 525"
        ),
        AsbabNuzulEntry(
            id = "abasa_1_4",
            surahNumber = 80,
            surahNameAr = "سورة عبس",
            surahNameEn = "Surah Abasa",
            ayahRefAr = "عبس: ١–٤",
            ayahRefEn = "80:1–4",
            isMakki = true,
            themeKey = "family",
            themeAr = "الأسرة والمجتمع",
            themeEn = "Family & Justice",
            ayahTextAr = "عَبَسَ وَتَوَلَّىٰ ۝ أَن جَاءَهُ الْأَعْمَىٰ ۝ وَمَا يُدْرِيكَ لَعَلَّهُ يَزَّكَّىٰ",
            occasionAr = "كان النبي ﷺ يدعو بعض كبراء قريش رجاء إسلامهم فجاء عبد الله بن أم مكتوم رضي الله عنه وهو أعمى يطلب التعلّم، فانشغل النبي ﷺ بصناديد قريش، فنزل العتاب الرباني اللطيف، فكان ﷺ بعد ذلك يبسط له رداءه ويقول: مرحباً بمن عاتبني فيه ربي.",
            occasionEn = "While the Prophet ﷺ was inviting Quraysh leaders to Islam, the blind companion Abdullah ibn Umm Maktum (RA) eagerly asked to learn. Allah gently reminded that a sincere seeker outweighs worldly status; thereafter the Prophet ﷺ always welcomed him saying: 'Welcome to the one on whose account my Lord admonished me!'",
            lessonAr = "ميزان القيمة في الإسلام هو الصدق والإقبال على الله لا الجاه والمناصب الدنيوية.",
            lessonEn = "True worth in Islam is sincerity and eagerness for guidance, never worldly status.",
            sourceAr = "سنن الترمذي (٣٣٣١) وموطأ مالك — صحيح",
            sourceEn = "Jami at-Tirmidhi 3331 & Muwatta Malik (Sahih)"
        ),
        AsbabNuzulEntry(
            id = "tawbah_118",
            surahNumber = 9,
            surahNameAr = "سورة التوبة",
            surahNameEn = "Surah At-Tawbah",
            ayahRefAr = "التوبة: ١١٨",
            ayahRefEn = "9:118",
            isMakki = false,
            themeKey = "trial",
            themeAr = "الابتلاء والصدق",
            themeEn = "Trials & Sincerity",
            ayahTextAr = "وَعَلَى الثَّلَاثَةِ الَّذِينَ خُلِّفُوا حَتَّىٰ إِذَا ضَاقَتْ عَلَيْهِمُ الْأَرْضُ بِمَا رَحُبَتْ ... ثُمَّ تَابَ عَلَيْهِمْ لِيَتُوبُوا",
            occasionAr = "نزلت في الصحابة الثلاثة الصادقين (كعب بن مالك، ومرارة بن الربيع، وهلال بن أمية) الذين تخلّفوا عن غزوة تبوك بغير عذر ورفضوا الكذب، فصبروا ٥٠ ليلة حتى أنزل الله توبتهم الخالدة.",
            occasionEn = "Revealed for the three truthful companions (Ka'b ibn Malik, Murarah, and Hilal) who missed the Tabuk expedition without excuse yet refused to lie. After 50 days of intense soul-searching, Allah revealed His complete forgiveness.",
            lessonAr = "الصدق مع الله ينجي صاحبه وإن بدا في أوله شاقاً؛ وبه تُفتح أعظم أبواب التوبة.",
            lessonEn = "Truthfulness with Allah always saves the believer—even when honesty feels hardest at first.",
            sourceAr = "صحيح البخاري (٤٤١٨) وصحيح مسلم (٢٧٦٩)",
            sourceEn = "Sahih al-Bukhari 4418 & Sahih Muslim 2769"
        ),
        AsbabNuzulEntry(
            id = "nur_22",
            surahNumber = 24,
            surahNameAr = "سورة النور",
            surahNameEn = "Surah An-Nur",
            ayahRefAr = "النور: ٢٢",
            ayahRefEn = "24:22",
            isMakki = false,
            themeKey = "family",
            themeAr = "الأسرة والمجتمع",
            themeEn = "Family & Justice",
            ayahTextAr = "وَلْيَعْفُوا وَلْيَصْفَحُوا ۗ أَلَا تُحِبُّونَ أَن يَغْفِرَ اللَّهُ لَكُمْ",
            occasionAr = "حلف أبو بكر الصديق رضي الله عنه ألا ينفق على قريبه مسطح بن أثاثة بعد أن خاض في حديث الإفك، فنزلت الآية، فقال الصديق فوراً: بلى والله إني لأحب أن يغفر الله لي! وأعاد النفقة عليه.",
            occasionEn = "Abu Bakr as-Siddiq (RA) vowed to stop supporting his relative Mistah after Mistah got involved in the slander incident. Upon hearing 'Would you not like Allah to forgive you?', Abu Bakr immediately said: 'Yes, by Allah!' and resumed his charity.",
            lessonAr = "العفو عند المقدرة من أعظم أسباب مغفرة الله لك ورفعة منزلتك.",
            lessonEn = "Forgiving others when you hold the power to withhold is a direct key to Allah's own forgiveness.",
            sourceAr = "صحيح البخاري (٤٧٥٠) وصحيح مسلم (٢٧٧٠)",
            sourceEn = "Sahih al-Bukhari 4750 & Sahih Muslim 2770"
        ),
        AsbabNuzulEntry(
            id = "zumar_53",
            surahNumber = 39,
            surahNameAr = "سورة الزمر",
            surahNameEn = "Surah Az-Zumar",
            ayahRefAr = "الزمر: ٥٣",
            ayahRefEn = "39:53",
            isMakki = true,
            themeKey = "comfort",
            themeAr = "البشارة والرحمة",
            themeEn = "Comfort & Mercy",
            ayahTextAr = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
            occasionAr = "أرسل أناس من أهل الشرك كانوا قد أكثروا القتل والزنا إلى النبي ﷺ يسألون: هل لما عملنا من كفارة؟ فأنزل الله أرجى آية في كتاب الله فاتحاً باب الرحمة لكل تائب.",
            occasionEn = "People who had committed grave sins in their past sent word to the Prophet ﷺ asking if repentance was even possible for them. Allah revealed the most hopeful verse in the Quran welcoming every repentant soul.",
            lessonAr = "مهما عظمت الذنوب فرحمة الله أوسع منها؛ القنوط حيلة شيطانية وباب التوبة مفتوح أبداً.",
            lessonEn = "No matter how heavy past mistakes are, Allah's mercy is infinitely greater—never despair.",
            sourceAr = "صحيح البخاري (٤٨١٠) وصحيح مسلم (١٢٢)",
            sourceEn = "Sahih al-Bukhari 4810 & Sahih Muslim 122"
        ),
        AsbabNuzulEntry(
            id = "al_imran_173",
            surahNumber = 3,
            surahNameAr = "سورة آل عمران",
            surahNameEn = "Surah Aal-Imran",
            ayahRefAr = "آل عمران: ١٧٣",
            ayahRefEn = "3:173",
            isMakki = false,
            themeKey = "trial",
            themeAr = "الابتلاء والصدق",
            themeEn = "Trials & Sincerity",
            ayahTextAr = "الَّذِينَ قَالَ لَهُمُ النَّاسُ إِنَّ النَّاسَ قَدْ جَمَعُوا لَكُمْ فَاخْشَوْهُمْ فَزَادَهُمْ إِيمَانًا وَقَالُوا حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            occasionAr = "بعد غزوة أحد بلغ الصحابة والجراح لم تلتئم أن قريشاً تتجمع للكرة عليهم، فلم يضعفوا بل توكلوا على الله وقالوا: حسبنا الله ونعم الوكيل، فانقلبوا بنعمة من الله وفضل.",
            occasionEn = "Right after Uhud, despite their wounds, the Companions were warned of a renewed enemy mobilization. Instead of panicking, their faith deepened as they declared: 'Allah is sufficient for us, and He is the best Disposer of affairs.'",
            lessonAr = "كلمة (حسبنا الله ونعم الوكيل) عند الشدائد تحوّل الخوف إلى سكينة ويقين ونصر.",
            lessonEn = "Relying wholeheartedly on Allah in moments of fear turns anxiety into unshakable strength.",
            sourceAr = "صحيح البخاري (٤٥٦٣)",
            sourceEn = "Sahih al-Bukhari 4563"
        ),
        AsbabNuzulEntry(
            id = "luqman_15",
            surahNumber = 31,
            surahNameAr = "سورة لقمان",
            surahNameEn = "Surah Luqman",
            ayahRefAr = "لقمان: ١٥",
            ayahRefEn = "31:15",
            isMakki = true,
            themeKey = "family",
            themeAr = "الأسرة والمجتمع",
            themeEn = "Family & Justice",
            ayahTextAr = "وَإِن جَاهَدَاكَ عَلَىٰ أَن تُشْرِكَ بِي مَا لَيْسَ لَكَ بِهِ عِلْمٌ فَلَا تُطِعْهُمَا ۖ وَصَاحِبْهُمَا فِي الدُّنْيَا مَعْرُوفًا",
            occasionAr = "لما أسلم سعد بن أبي وقاص رضي الله عنه حلفت أمه ألا تأكل ولا تشرب حتى يرجع عن دينه، فأمر الله بالثبات على التوحيد مع حفظ كمال البرّ والصحبة بالمعروف للوالدين.",
            occasionEn = "When Sa'd ibn Abi Waqqas (RA) embraced Islam, his mother went on a hunger strike demanding he renounce faith. Allah commanded steadfastness in faith paired with the highest kindness and companionship toward parents.",
            lessonAr = "الاختلاف في الرأي أو الدين لا يسقط أبداً واجب البرّ وحسن الصحبة للوالدين.",
            lessonEn = "Disagreement—even on the deepest matters—never cancels kindness and noble treatment of parents.",
            sourceAr = "صحيح مسلم (١٧٤٨)",
            sourceEn = "Sahih Muslim 1748"
        )
    )
}

@Composable
fun AsbabAlNuzulScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var query by remember { mutableStateOf("") }
    var eraFilter by remember { mutableStateOf("ALL") } // ALL, MAKKI, MADANI
    var themeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(query, eraFilter, themeFilter) {
        val q = query.trim().lowercase()
        AsbabAlNuzulCatalog.entries.filter { entry ->
            val eraMatch = when (eraFilter) {
                "MAKKI" -> entry.isMakki
                "MADANI" -> !entry.isMakki
                else -> true
            }
            val themeMatch = themeFilter == "ALL" || entry.themeKey == themeFilter
            val queryMatch = q.isEmpty() ||
                entry.surahNameAr.contains(q) ||
                entry.surahNameEn.lowercase().contains(q) ||
                entry.ayahTextAr.contains(q) ||
                entry.occasionAr.contains(q) ||
                entry.occasionEn.lowercase().contains(q)
            eraMatch && themeMatch && queryMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("asbab_al_nuzul_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "أطلس أسباب النزول الصحيحة", "Authentic Asbab al-Nuzul Atlas"),
            subtitle = L10n.pick(
                lang,
                "قصص نزول الآيات الموثّقة من الصحيحين مع العِبر الحياتية والانتقال الفوري للمصحف",
                "Verified Occasions of Revelation from Sahih Bukhari & Muslim with Direct Mushaf Links"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LiquidGlassCard {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = noorColors.goldAccent) },
                        placeholder = {
                            Text(L10n.pick(lang, "ابحث بالسورة أو القصة أو الصحابي...", "Search Surah, story, or companion..."))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("asbab_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الكل (${AsbabAlNuzulCatalog.entries.size})", "All (${AsbabAlNuzulCatalog.entries.size})"),
                                selected = eraFilter == "ALL",
                                onClick = { eraFilter = "ALL" }
                            )
                        }
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "مكي", "Makki"),
                                selected = eraFilter == "MAKKI",
                                onClick = { eraFilter = "MAKKI" }
                            )
                        }
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "مدني", "Madani"),
                                selected = eraFilter == "MADANI",
                                onClick = { eraFilter = "MADANI" }
                            )
                        }
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "البشارة والرحمة", "Comfort & Mercy"),
                                selected = themeFilter == "comfort",
                                onClick = { themeFilter = if (themeFilter == "comfort") "ALL" else "comfort" }
                            )
                        }
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الأسرة والمجتمع", "Family & Justice"),
                                selected = themeFilter == "family",
                                onClick = { themeFilter = if (themeFilter == "family") "ALL" else "family" }
                            )
                        }
                        item {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الابتلاء والصدق", "Trials"),
                                selected = themeFilter == "trial",
                                onClick = { themeFilter = if (themeFilter == "trial") "ALL" else "trial" }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    LiquidGlassCard {
                        Text(
                            text = L10n.pick(lang, "لا توجد نتائج مطابقة للبحث الحالي.", "No matching revelation stories found."),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { entry ->
                LiquidGlassCard(goldBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "${entry.surahNameAr} · ${entry.ayahRefAr}", "${entry.surahNameEn} · ${entry.ayahRefEn}"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                            Text(
                                text = L10n.pick(lang, entry.themeAr, entry.themeEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (entry.isMakki) GoldPrimary.copy(alpha = 0.22f)
                                    else EmeraldPrimary.copy(alpha = 0.24f)
                                )
                                .border(
                                    1.dp,
                                    if (entry.isMakki) GoldPrimary else Color(0xFF34D399),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (entry.isMakki) L10n.pick(lang, "مكي", "Makki") else L10n.pick(lang, "مدني", "Madani"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.isMakki) GoldBright else Color(0xFF6EE7B7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = entry.ayahTextAr,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 34.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = L10n.pick(lang, "سبب النزول الموثّق:", "Authentic Occasion of Revelation:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = L10n.pick(lang, entry.occasionAr, entry.occasionEn),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldPrimary.copy(alpha = 0.10f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                        Text(
                            text = L10n.pick(lang, "الهداية المستنبطة: ${entry.lessonAr}", "Life Lesson: ${entry.lessonEn}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright,
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = L10n.pick(lang, entry.sourceAr, entry.sourceEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.22f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onOpenSurah(entry.surahNumber) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = GoldBright, modifier = Modifier.size(15.dp))
                            Text(
                                text = L10n.pick(lang, "المصحف", "Mushaf"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                        }
                    }
                }
            }
        }
    }
}
