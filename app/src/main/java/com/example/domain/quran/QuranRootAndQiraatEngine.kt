package com.example.domain.quran

import com.example.data.local.catalog.SurahCatalog
import com.example.data.remote.QuranSearchHit

data class RootDerivativeNode(
    val wordAr: String,
    val searchQueryPlain: String,
    val patternAr: String,
    val meaningAr: String,
    val meaningEn: String,
    val meaningUz: String,
    val orbitRing: Int // 1 = inner orbit, 2 = outer orbit
)

data class QuranRootFamily(
    val id: String,
    val rootAr: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val coreMeaningAr: String,
    val coreMeaningEn: String,
    val coreMeaningUz: String,
    val derivatives: List<RootDerivativeNode>
)

data class RootDistributionStats(
    val totalMatches: Int,
    val meccanCount: Int,
    val madinanCount: Int,
    val distinctSurahs: Int,
    val topSurahs: List<Pair<String, Int>> // Surah name Ar -> count
)

data class MutawatirQiraatComparison(
    val id: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val hafsWordAr: String,
    val hafsSnippetAr: String,
    val altQiraahNameAr: String,
    val altQiraahNameEn: String,
    val altWordAr: String,
    val altSnippetAr: String,
    val miracleNoteAr: String,
    val miracleNoteEn: String,
    val miracleNoteUz: String
)

data class SanadImamNode(
    val id: String,
    val tier: Int, // 0 = Revelation/Prophet, 1 = Sahabah, 2 = 10 Imams
    val nameAr: String,
    val nameEn: String,
    val eraAr: String,
    val cityAr: String,
    val rawisAr: String,
    val hallmarkAr: String,
    val hallmarkEn: String
)

/**
 * Crown-Jewel Quranic Knowledge Engine:
 * 1. Living Quranic Root Constellation (16 foundational triliteral roots & morphological derivatives) + live 6,236-Ayah distribution analytics.
 * 2. Ten Mutawatir Qira'at Side-by-Side Comparison & Linguistic Eloquence Miracles (from Ibn al-Jazari's Al-Nashr).
 * 3. Unbroken Golden Sanad Chain of Transmission to Prophet Muhammad ﷺ.
 */
object QuranRootAndQiraatEngine {

    val rootFamilies: List<QuranRootFamily> = listOf(
        QuranRootFamily(
            id = "r_h_m", rootAr = "ر ح م",
            titleAr = "الرحمة", titleEn = "Mercy (R-H-M)", titleUz = "Rahmat (R-H-M)",
            coreMeaningAr = "الرقة والعطف والإنعام الشامل الذي وسع كل شيء",
            coreMeaningEn = "All-encompassing compassion, tenderness, and divine grace",
            coreMeaningUz = "Barcha narsani qamrab olgan cheksiz rahmat va mehribonlik",
            derivatives = listOf(
                RootDerivativeNode("الرَّحْمَـٰن", "الرحمن", "فَعْلان", "ذو الرحمة الواسعة الشاملة", "The Most Gracious", "Mehribon", 1),
                RootDerivativeNode("الرَّحِيم", "الرحيم", "فَعِيل", "ذو الرحمة الواصلة للمؤمنين", "The Most Merciful", "Rahmli", 1),
                RootDerivativeNode("رَحْمَة", "رحمة", "فَعْلَة", "النعمة والهداية والمغفرة", "Mercy & Grace", "Rahmat", 1),
                RootDerivativeNode("يَرْحَمُ", "يرحم", "يَفْعَلُ", "فعل الرحمة والعفو المتجدد", "Bestows Mercy", "Rahm qiladi", 2),
                RootDerivativeNode("رُحَمَاء", "رحماء", "فُعَلاء", "المتراحمون فيما بينهم", "Compassionate to each other", "Bir-biriga mehribon", 2),
                RootDerivativeNode("الأَرْحَام", "الأرحام", "أَفْعَال", "القرابات وصلة الرحم", "Ties of Kinship", "Qarindoshlik rishtalari", 2)
            )
        ),
        QuranRootFamily(
            id = "n_w_r", rootAr = "ن و ر",
            titleAr = "النور", titleEn = "Divine Light (N-W-R)", titleUz = "Nur (N-V-R)",
            coreMeaningAr = "الضياء والهداية والكشف المبين للحق",
            coreMeaningEn = "Radiant illumination, spiritual guidance, and clarity of truth",
            coreMeaningUz = "Yorug'lik, hidoyat va haqiqatni ochuvchi nur",
            derivatives = listOf(
                RootDerivativeNode("نُور", "نور", "فُعْل", "الضياء الإلهي وهداية الوحي", "Light / Guidance", "Nur", 1),
                RootDerivativeNode("مُنِير", "منير", "مُفْعِل", "المضيء الواضح البين", "Illuminating", "Nur taratuvchi", 1),
                RootDerivativeNode("النُّور", "النور", "الفُعْل", "الإسلام والقرآن المبين", "The Light (Quran)", "Al-Nur", 1),
                RootDerivativeNode("نَار", "نار", "فَعْل", "اللهب المحرق (مقابل النور)", "Fire", "Olov", 2),
                RootDerivativeNode("أَنوَار / نُورَهُم", "نورهم", "إضافة", "نور المؤمنين يوم القيامة", "Their Light on Judgment Day", "Ularning nuri", 2)
            )
        ),
        QuranRootFamily(
            id = "h_d_y", rootAr = "هـ د ي",
            titleAr = "الهداية", titleEn = "Guidance (H-D-Y)", titleUz = "Hidoyat (H-D-Y)",
            coreMeaningAr = "الدلالة بلطف إلى الطريق المستقيم الموصل إلى الله",
            coreMeaningEn = "Gentle guidance onto the Straight Path leading to Allah",
            coreMeaningUz = "Allohga eltuvchi to'g'ri yo'lga muloyimlik bilan boshlash",
            derivatives = listOf(
                RootDerivativeNode("هُدًى", "هدى", "فُعَل", "بيان الحق والرشاد", "Guidance", "Hidoyat", 1),
                RootDerivativeNode("اهْدِنَا", "اهدنا", "افْعِلْ", "طلب الثبات والزيادة في الهداية", "Guide us", "Bizni hidoyat qil", 1),
                RootDerivativeNode("يَهْدِي", "يهدي", "يَفْعِلُ", "يدل ويوفق للإيمان", "He guides", "Hidoyat qiladi", 1),
                RootDerivativeNode("الْمُهْتَدُونَ", "المهتدون", "الْمُفْتَعِلُون", "السالكون سبيل الرشاد", "The Rightly Guided", "Hidoyat topganlar", 2),
                RootDerivativeNode("هَادٍ", "هاد", "فَاعِل", "المرشد والدال على الخير", "A Guide", "Yo'lboshchi", 2)
            )
        ),
        QuranRootFamily(
            id = "q_l_b", rootAr = "ق ل ب",
            titleAr = "القلب", titleEn = "The Heart (Q-L-B)", titleUz = "Qalb (Q-L-B)",
            coreMeaningAr = "محل الإيمان والعقل والتقلب بين الأحوال",
            coreMeaningEn = "Seat of faith, discernment, and spiritual transformation",
            coreMeaningUz = "Iymon, aql va ma'naviy holatlar makoni",
            derivatives = listOf(
                RootDerivativeNode("قَلْب", "قلب", "فَعْل", "الفؤاد السليم المنيب", "Heart", "Qalb", 1),
                RootDerivativeNode("قُلُوب", "قلوب", "فُعُول", "أفئدة المؤمنين المطمئنة", "Hearts", "Qalblar", 1),
                RootDerivativeNode("قَلْبٍ سَلِيم", "سليم", "تركيب", "القلب الخالص من الشرك والغل", "Sound Pure Heart", "Salim qalb", 1),
                RootDerivativeNode("تَتَقَلَّبُ", "تتقلب", "تَتَفَعَّلُ", "التحول والتقلب السريع", "Turns / Shifts", "O'zgarib turadi", 2),
                RootDerivativeNode("مُنقَلَب", "منقلب", "مُنفَعَل", "المرجع والمصير", "Place of Return", "Qaytish joyi", 2)
            )
        ),
        QuranRootFamily(
            id = "a_l_m", rootAr = "ع ل م",
            titleAr = "العلم", titleEn = "Knowledge (A-L-M)", titleUz = "Ilm (A-L-M)",
            coreMeaningAr = "إدراك الحقائق على ما هي عليه بيقين وبصيرة",
            coreMeaningEn = "Certain perception of truth, wisdom, and divine omniscience",
            coreMeaningUz = "Haqiqatni aniq bilish, donolik va Allohning cheksiz ilmi",
            derivatives = listOf(
                RootDerivativeNode("عِلْم", "علم", "فِعْل", "المعرفة الموثقة النافعة", "Knowledge", "Ilm", 1),
                RootDerivativeNode("عَلِيم", "عليم", "فَعِيل", "المحيط بكل شيء علماً", "All-Knowing", "Olim / Bilguvchi", 1),
                RootDerivativeNode("يَعْلَمُونَ", "يعلمون", "يَفْعَلُون", "أهل الفهم والوعي", "Those who know", "Biladiganlar", 1),
                RootDerivativeNode("الْعَالَمِينَ", "العالمين", "جَمْع", "كل ما سوى الله من الخلائق", "All the Worlds", "Olamlar", 2),
                RootDerivativeNode("عَلَّمَ", "علم", "فَعَّلَ", "التعليم الإلهي للإنسان", "He taught", "Ta'lim berdi", 2)
            )
        ),
        QuranRootFamily(
            id = "s_b_r", rootAr = "ص ب ر",
            titleAr = "الصبر", titleEn = "Steadfastness (S-B-R)", titleUz = "Sabr (S-B-R)",
            coreMeaningAr = "حبس النفس على طاعة الله وعن معصيته وعلى أقداره",
            coreMeaningEn = "Resilient perseverance in obedience, restraint, and trials",
            coreMeaningUz = "Toatda sobitlik, ma'siyatdan tiyilish va sinovlarga matonat",
            derivatives = listOf(
                RootDerivativeNode("الصَّبْر", "الصبر", "الفَعْل", "الثبات الجميل بلا جزع", "Patience", "Sabr", 1),
                RootDerivativeNode("الصَّابِرِينَ", "الصابرين", "الفَاعِلِين", "أهل المعية الإلهية والأجر بغير حساب", "The Patient Ones", "Sabr qiluvchilar", 1),
                RootDerivativeNode("اصْبِرْ", "اصبر", "افْعِلْ", "أمر رباني بالثبات والسكينة", "Be patient", "Sabr qil", 1),
                RootDerivativeNode("صَبَّار", "صبار", "فَعَّال", "كثير الصبر والاحتمال", "Deeply steadfast", "Ko'p sabrli", 2),
                RootDerivativeNode("وَاصْطَبِرْ", "واصطبر", "افْتَعِلْ", "مبالغة في لزوم الصبر على الصلاة", "Persevere firmly", "Sobit sabr qil", 2)
            )
        ),
        QuranRootFamily(
            id = "dh_k_r", rootAr = "ذ ك ر",
            titleAr = "الذكر", titleEn = "Remembrance (Dh-K-R)", titleUz = "Zikr (Z-K-R)",
            coreMeaningAr = "استحضار عظمة الله بالقلب واللسان والاتعاظ بالوحي",
            coreMeaningEn = "Mindful remembrance of Allah, honor, and Quranic reminder",
            coreMeaningUz = "Allohni qalb va til bilan eslash, Qur'on eslatmasi",
            derivatives = listOf(
                RootDerivativeNode("ذِكْر", "ذكر", "فِعْل", "ذكر الله والقرآن والشرف", "Remembrance / Quran", "Zikr", 1),
                RootDerivativeNode("اذْكُرُوا", "اذكروا", "افْعُلُوا", "أمر بدوام ذكر الله وشكره", "Remember!", "Zikr qilinglar", 1),
                RootDerivativeNode("الذَّاكِرِينَ", "الذاكرين", "الفَاعِلِين", "المكثرون من ذكر الله", "Those who remember", "Zikr qiluvchilar", 1),
                RootDerivativeNode("تَذْكِرَة", "تذكرة", "تَفْعِلَة", "موعظة توقظ القلوب", "A Reminder", "Eslatma", 2),
                RootDerivativeNode("يَتَذَكَّرُونَ", "يتذكرون", "يَتَفَعَّلُون", "يتعظون ويدركون العبرة", "They take heed", "Ibrat oladilar", 2)
            )
        ),
        QuranRootFamily(
            id = "sh_k_r", rootAr = "ش ك ر",
            titleAr = "الشكر", titleEn = "Gratitude (Sh-K-R)", titleUz = "Shukr (Sh-K-R)",
            coreMeaningAr = "ظهور أثر نعمة الله على القلب واللسان والجوارح",
            coreMeaningEn = "Active gratitude in heart, tongue, and righteous action",
            coreMeaningUz = "Alloh ne'matini qalb, til va amal bilan e'tirof etish",
            derivatives = listOf(
                RootDerivativeNode("شَاكِر", "شاكر", "فَاعِل", "المعترف بالفضل الإلهي", "Grateful", "Shukr qiluvchi", 1),
                RootDerivativeNode("شَكُور", "شكور", "فَعُول", "كثير الشكر، ومن أسماء الله الحسنى", "Most Appreciative", "O'ta shukrli", 1),
                RootDerivativeNode("تَشْكُرُونَ", "تشكرون", "تَفْعَلُون", "أداء حق النعمة", "You give thanks", "Shukr qilasizlar", 1),
                RootDerivativeNode("اشْكُرْ", "اشكر", "افْعُلْ", "أمر بشكر الله والوالدين", "Be grateful", "Shukr qil", 2)
            )
        ),
        QuranRootFamily(
            id = "t_w_b", rootAr = "ت و ب",
            titleAr = "التوبة", titleEn = "Repentance (T-W-B)", titleUz = "Tavba (T-V-B)",
            coreMeaningAr = "الرجوع الصادق إلى الله وقبول الله رجوع عبده",
            coreMeaningEn = "Sincere turning back to Allah and His loving acceptance",
            coreMeaningUz = "Allohga chin dildan qaytish va Uning tavbani qabul qilishi",
            derivatives = listOf(
                RootDerivativeNode("التَّوَّاب", "التواب", "الفَعَّال", "كثير القبول لتوبة عباده", "The Acceptor of Repentance", "Tavbalarni qabul qiluvchi", 1),
                RootDerivativeNode("تُوبُوا", "توبوا", "فُعُلُوا", "نداء للرجوع والتطهر", "Turn in repentance", "Tavba qilinglar", 1),
                RootDerivativeNode("تَوْبَة", "توبة", "فَعْلَة", "الندم والعزم النصوح", "Sincere Repentance", "Tavba", 1),
                RootDerivativeNode("التَّائِبُونَ", "التائبون", "الفَاعِلُون", "الراجعون إلى الله دائماً", "The Repentant", "Tavba qiluvchilar", 2)
            )
        ),
        QuranRootFamily(
            id = "s_l_m", rootAr = "س ل م",
            titleAr = "السلام والإسلام", titleEn = "Peace & Submission (S-L-M)", titleUz = "Salom va Islom (S-L-M)",
            coreMeaningAr = "السلامة من الآفات والطمأنينة والانقياد الخالص لله",
            coreMeaningEn = "Wholeness, inner peace, safety, and serene devotion to Allah",
            coreMeaningUz = "Tinchlik, omonlik va Allohga xolis taslim bo'lish",
            derivatives = listOf(
                RootDerivativeNode("سَلَام", "سلام", "فَعَال", "تحية أهل الجنة والأمان التام", "Peace", "Salom / Tinchlik", 1),
                RootDerivativeNode("الإِسْلَام", "الإسلام", "الإِفْعَال", "الدين الخالص لله رب العالمين", "Islam", "Islom", 1),
                RootDerivativeNode("الْمُسْلِمِينَ", "المسلمين", "الْمُفْعِلِين", "المنقادون لأمر الله طوعاً", "The Muslims", "Musulmonlar", 1),
                RootDerivativeNode("سَلِيم", "سليم", "فَعِيل", "القلب الطاهر المعافى", "Pure & Sound", "Pok / Salim", 2)
            )
        ),
        QuranRootFamily(
            id = "gh_f_r", rootAr = "غ ف ر",
            titleAr = "المغفرة", titleEn = "Forgiveness (Gh-F-R)", titleUz = "Mag'firat (G'-F-R)",
            coreMeaningAr = "ستر الذنب ووقاية صاحبه من شره ومحوه",
            coreMeaningEn = "Protective veiling, pardon, and complete erasure of sins",
            coreMeaningUz = "Gunohlarni yashirish, kechirish va o'chirish",
            derivatives = listOf(
                RootDerivativeNode("غَفُور", "غفور", "فَعُول", "كثير المغفرة والستر", "Most Forgiving", "Kechirimli", 1),
                RootDerivativeNode("مَغْفِرَة", "مغفرة", "مَفْعِلَة", "عفو إلهي واسع", "Forgiveness", "Mag'firat", 1),
                RootDerivativeNode("اسْتَغْفِرُوا", "استغفروا", "اسْتَفْعِلُوا", "طلب المغفرة والبركة", "Seek forgiveness", "Istig'for aytinglar", 1),
                RootDerivativeNode("غَفَّار", "غفار", "فَعَّال", "دائم المغفرة مهما تكرر الذنب", "Perpetually Forgiving", "O'ta mag'firatli", 2)
            )
        ),
        QuranRootFamily(
            id = "a_m_n", rootAr = "أ م ن",
            titleAr = "الإيمان والأمان", titleEn = "Faith & Security (A-M-N)", titleUz = "Iymon va Omonlik (A-M-N)",
            coreMeaningAr = "التصديق الجازم والطمأنينة والأمان من الخوف",
            coreMeaningEn = "Deep conviction, trustworthiness, and tranquility from fear",
            coreMeaningUz = "Qat'iy ishonch, omonatdorlik va xotirjamlik",
            derivatives = listOf(
                RootDerivativeNode("آمَنُوا", "آمنوا", "أَفْعَلُوا", "صدّقوا بالله ورسوله", "Those who believe", "Iymon keltirganlar", 1),
                RootDerivativeNode("الْمُؤْمِنِينَ", "المؤمنين", "الْمُفْعِلِين", "أهل الإيمان والأمان", "The Believers", "Mo'minlar", 1),
                RootDerivativeNode("آمِنًا", "آمنا", "فَاعِل", "آمناً مطمئناً لا خوف فيه", "Safe & Secure", "Tinch va omon", 2),
                RootDerivativeNode("أَمَانَة", "أمانة", "فَعَالَة", "التكليف والمسؤولية الشريفة", "Trust / Responsibility", "Omonat", 2)
            )
        )
    )

    val qiraatComparisons: List<MutawatirQiraatComparison> = listOf(
        MutawatirQiraatComparison(
            id = "maliki_fatiha",
            surahNumber = 1, ayahNumber = 4, surahNameAr = "الفاتحة", surahNameEn = "Al-Fatihah",
            hafsWordAr = "مَـٰلِكِ",
            hafsSnippetAr = "مَـٰلِكِ يَوْمِ الدِّينِ",
            altQiraahNameAr = "نافع (ورش وقالون) وأبو عمرو وابن كثير",
            altQiraahNameEn = "Nafi' (Warsh & Qaloon), Abu Amr, Ibn Kathir",
            altWordAr = "مَلِكِ",
            altSnippetAr = "مَلِكِ يَوْمِ الدِّينِ",
            miracleNoteAr = "الإعجاز البياني: «مَـٰلِك» تثبت تمام المِلك والتصرف، و«مَلِك» تثبت تمام السلطان والهيبة والأمر — فاجتمع لله بالقرآءتين كمال المُلْك والمِلْك يوم القيامة!",
            miracleNoteEn = "Eloquence Miracle: 'Maliki' (with long a) affirms absolute Ownership, while 'Maliki' (short a) affirms supreme Sovereignty — both mutawatir readings unite complete Ownership & Kingship!",
            miracleNoteUz = "Balog'at mo'jizasi: 'Moliki' — mutlaq egalikni, 'Maliki' — oliy podshohlikni bildiradi; ikki qiroat Allohning to'liq mulk va saltanatini jamlaydi!"
        ),
        MutawatirQiraatComparison(
            id = "fatabayyanu_hujurat",
            surahNumber = 49, ayahNumber = 6, surahNameAr = "الحجرات", surahNameEn = "Al-Hujurat",
            hafsWordAr = "فَتَبَيَّنُوا",
            hafsSnippetAr = "إِن جَاءَكُمْ فَاسِقٌ بِنَبَإٍ فَتَبَيَّنُوا",
            altQiraahNameAr = "حمزة والكسائي وخلف العاشر",
            altQiraahNameEn = "Hamzah, Al-Kisa'i & Khalaf",
            altWordAr = "فَتَثَبَّتُوا",
            altSnippetAr = "إِن جَاءَكُمْ فَاسِقٌ بِنَبَإٍ فَتَثَبَّتُوا",
            miracleNoteAr = "الإعجاز البياني: «فتبيّنوا» تأمر بكشف الحقيقة والوضوح، و«فتثبّتوا» تأمر بالتأني وعدم العجلة في الحكم — فاجتمع منهج التحقيق العلمي والإعلامي كاملاً في كلمة واحدة!",
            miracleNoteEn = "Eloquence Miracle: 'Fatabayyanu' commands verifying clarity of facts, while 'Fatathabbatu' commands patience without haste — uniting full ethical verification!",
            miracleNoteUz = "Balog'at mo'jizasi: 'Fatabayyanu' — haqiqatni aniqlashga, 'Fatasabbatu' — shoshilmasdan puxta tekshirishga buyuradi!"
        ),
        MutawatirQiraatComparison(
            id = "yukhadioon_baqarah",
            surahNumber = 2, ayahNumber = 9, surahNameAr = "البقرة", surahNameEn = "Al-Baqarah",
            hafsWordAr = "وَمَا يَخْدَعُونَ",
            hafsSnippetAr = "وَمَا يَخْدَعُونَ إِلَّا أَنفُسَهُمْ",
            altQiraahNameAr = "نافع (ورش وقالون) وابن كثير وأبو عمرو",
            altQiraahNameEn = "Nafi' (Warsh/Qaloon), Ibn Kathir & Abu Amr",
            altWordAr = "وَمَا يُخَادِعُونَ",
            altSnippetAr = "وَمَا يُخَادِعُونَ إِلَّا أَنفُسَهُمْ",
            miracleNoteAr = "الإعجاز البياني: «يخدعون» تفيد وقوع الخداع فعلاً على أنفسهم، و«يخادعون» تفيد استمرار محاولتهم ومكرهم — فتتكامل الصورتان في فضح حال المنافقين.",
            miracleNoteEn = "Eloquence Miracle: 'Yakhda'un' shows they actually deceive themselves, while 'Yukhadi'un' shows their continuous scheming returning upon themselves.",
            miracleNoteUz = "Balog'at mo'jizasi: 'Yaxda'un' o'zlarini aldayotganlarini, 'Yuxodi'un' esa makr urinishlari o'zlariga qaytishini ifodalaydi."
        ),
        MutawatirQiraatComparison(
            id = "nushizuha_baqarah",
            surahNumber = 2, ayahNumber = 259, surahNameAr = "البقرة", surahNameEn = "Al-Baqarah",
            hafsWordAr = "نُنشِزُهَا",
            hafsSnippetAr = "وَانظُرْ إِلَى الْعِظَامِ كَيْفَ نُنشِزُهَا ثُمَّ نَكْسُوهَا لَحْمًا",
            altQiraahNameAr = "نافع (ورش وقالون) وابن كثير وأبو عمرو",
            altQiraahNameEn = "Nafi' (Warsh/Qaloon), Ibn Kathir & Abu Amr",
            altWordAr = "نُنشِرُهَا",
            altSnippetAr = "وَانظُرْ إِلَى الْعِظَامِ كَيْفَ نُنشِرُهَا ثُمَّ نَكْسُوهَا لَحْمًا",
            miracleNoteAr = "الإعجاز البياني: «ننشزها» (بالزاي) أي نرفعها ونركّب بعضها على بعض، و«ننشرها» (بالراء) أي نحييها بعد الموت — فتصوّر القراءتان معاً التركيب ثم الإحياء!",
            miracleNoteEn = "Eloquence Miracle: 'Nunshizuha' (zay) means raising & assembling the bones together; 'Nunshiruha' (ra) means bringing them back to life!",
            miracleNoteUz = "Balog'at mo'jizasi: 'Nunshizuho' suyaklarni ko'tarib biriktirishni, 'Nunshiruho' esa ularga qaytadan hayot berishni tasvirlaydi!"
        ),
        MutawatirQiraatComparison(
            id = "khatam_ahzab",
            surahNumber = 33, ayahNumber = 40, surahNameAr = "الأحزاب", surahNameEn = "Al-Ahzab",
            hafsWordAr = "وَخَاتَمَ النَّبِيِّينَ",
            hafsSnippetAr = "وَلَـٰكِن رَّسُولَ اللَّهِ وَخَاتَمَ النَّبِيِّينَ",
            altQiraahNameAr = "الجمهور (نافع، ابن كثير، أبو عمرو، ابن عامر، حمزة، الكسائي)",
            altQiraahNameEn = "Majority (Nafi', Ibn Kathir, Abu Amr, Ibn Amir, Hamzah, Kisa'i)",
            altWordAr = "وَخَاتِمَ النَّبِيِّينَ",
            altSnippetAr = "وَلَـٰكِن رَّسُولَ اللَّهِ وَخَاتِمَ النَّبِيِّينَ",
            miracleNoteAr = "الإعجاز البياني: «خَاتَم» (بالفتح) الطابع والزينة التي يُختم بها على الرسالات، و«خَاتِم» (بالكسر) اسم فاعل بمعنى آخرهم الذي ختم الله به النبوة فلا نبي بعده ﷺ.",
            miracleNoteEn = "Eloquence Miracle: 'Khatam' (with fatha) is the radiant Seal; 'Khatim' (with kasra) is the Final One who completes all Prophethood ﷺ.",
            miracleNoteUz = "Balog'at mo'jizasi: 'Xotam' — payg'ambarlik muhri va ziynati, 'Xotim' — payg'ambarlikni yakunlovchi so'nggi Rasul ﷺ demakdir."
        ),
        MutawatirQiraatComparison(
            id = "mukhlasin_yusuf",
            surahNumber = 12, ayahNumber = 24, surahNameAr = "يوسف", surahNameEn = "Yusuf",
            hafsWordAr = "الْمُخْلَصِينَ",
            hafsSnippetAr = "إِنَّهُ مِنْ عِبَادِنَا الْمُخْلَصِينَ",
            altQiraahNameAr = "نافع وابن كثير وأبو عمرو (ورش وقالون والدوري)",
            altQiraahNameEn = "Nafi', Ibn Kathir & Abu Amr (Warsh, Qaloon, Douri)",
            altWordAr = "الْمُخْلِصِينَ",
            altSnippetAr = "إِنَّهُ مِنْ عِبَادِنَا الْمُخْلِصِينَ",
            miracleNoteAr = "الإعجاز البياني: «المخلِصين» (بالكسر) الذين أخلصوا عبادتهم لله باختيارهم، و«المخلَصين» (بالفتح) الذين اصطفاهم الله وخلّصهم لنفسه عصمةً وكرامة!",
            miracleNoteEn = "Eloquence Miracle: 'Al-Mukhlisin' (kasra) = those who are sincere to Allah; 'Al-Mukhlasin' (fatha) = those purified & chosen by Allah Himself!",
            miracleNoteUz = "Balog'at mo'jizasi: 'Muxlisin' — Allohga xolis bandalik qiluvchilar, 'Muxlasin' — Alloh tomonidan tanlab poklanganlar demakdir!"
        ),
        MutawatirQiraatComparison(
            id = "arjulakum_maidah",
            surahNumber = 5, ayahNumber = 6, surahNameAr = "المائدة", surahNameEn = "Al-Ma'idah",
            hafsWordAr = "وَأَرْجُلَكُمْ",
            hafsSnippetAr = "وَامْسَحُوا بِرُءُوسِكُمْ وَأَرْجُلَكُمْ إِلَى الْكَعْبَيْنِ",
            altQiraahNameAr = "ابن كثير وأبو عمرو وحمزة وشعبة",
            altQiraahNameEn = "Ibn Kathir, Abu Amr, Hamzah & Shu'bah",
            altWordAr = "وَأَرْجُلِكُمْ",
            altSnippetAr = "وَامْسَحُوا بِرُءُوسِكُمْ وَأَرْجُلِكُمْ إِلَى الْكَعْبَيْنِ",
            miracleNoteAr = "الإعجاز الفقهي: قراءة النصب «وَأَرْجُلَكُمْ» (عطفاً على فاغسلوا) تدل على غسل الرجلين المكشوفتين، وقراءة الجر «وَأَرْجُلِكُمْ» تشير إلى مشروعية المسح على الخفين كما بيّنته السنة!",
            miracleNoteEn = "Fiqh Miracle: Accusative 'arjulakum' establishes washing bare feet in Wudu, while genitive 'arjulikum' alludes to wiping over leather socks (khuffayn) as explained by the Sunnah!",
            miracleNoteUz = "Fiqhiy mo'jiza: 'Arjulakum' yalang oyoqlarni yuvishni, 'Arjulikum' esa sunnatda bayon qilingan mahsiga mash tortish hukmini qamrab oladi!"
        )
    )

    val goldenSanadChain: List<SanadImamNode> = listOf(
        SanadImamNode(
            "revelation", 0, "الوحي الأمين — جبريل عليه السلام ← النبي محمد ﷺ", "Revelation — Jibril (AS) → Prophet Muhammad ﷺ",
            "٦١٠–٦٣٢م (٢٣ عاماً)", "مكة والمدينة", "التلقي المباشر المشافهةً والعرضة الأخيرة",
            "تلقى النبي ﷺ القرآن حرفاً حرفاً من جبريل عليه السلام وعارضه به في رمضان الأخير مرتين بالأحرف السبعة.",
            "The Prophet ﷺ received every letter orally from Jibril (AS) and reviewed the entire Quran twice in his final Ramadan."
        ),
        SanadImamNode(
            "sahabah", 1, "قرّاء الصحابة الأجلاء رضي الله عنهم", "The Companion Master Reciters (RA)",
            "القرن الأول الهجري", "المدينة ومكة والكوفة والشام والبصرة",
            "أُبيّ بن كعب · زيد بن ثابت · عبد الله بن مسعود · عثمان بن عفان · علي بن أبي طالب · أبو الدرداء",
            "تلقوا القرآن من فم رسول الله ﷺ مباشرةً ونقلوه بالتواتر إلى التابعين في الأمصار الإسلامية.",
            "Learned directly from the Prophet ﷺ and taught thousands of Tabi'in across Madinah, Makkah, Kufah, Basrah, and Damascus."
        ),
        SanadImamNode(
            "nafi", 2, "الإمام نافع المدني (١٦٩ هـ)", "Imam Nafi' al-Madani (d. 169 AH)",
            "٧٠–١٦٩ هـ", "المدينة المنورة (المسجد النبوي)", "راوياه: قالون وورش",
            "إمام أهل المدينة ٧٠ عاماً؛ قرأ على ٧٠ من التابعين، وتشتهر رواية ورش في المغرب العربي وقالون في ليبيا وتونس.",
            "Led recitation in Al-Masjid an-Nabawi for 70 years; transmitted through Qaloon and Warsh."
        ),
        SanadImamNode(
            "asim", 2, "الإمام عاصم الكوفي (١٢٧ هـ)", "Imam Asim al-Kufi (d. 127 AH)",
            "توفي ١٢٧ هـ", "الكوفة (العراق)", "راوياه: حفص وشعبة",
            "إمام أهل الكوفة؛ سند حفص عن عاصم يرتفع عبر أبي عبد الرحمن السلمي عن علي بن أبي طالب رضي الله عنه إلى النبي ﷺ، وهي الرواية الأكثر انتشاراً اليوم.",
            "Master of Kufah; Hafs 'an Asim traces via Abu Abd al-Rahman al-Sulami directly to Ali ibn Abi Talib (RA) and the Prophet ﷺ."
        ),
        SanadImamNode(
            "ibn_kathir", 2, "الإمام ابن كثير المكي (١٢٠ هـ)", "Imam Ibn Kathir al-Makki (d. 120 AH)",
            "٤٥–١٢٠ هـ", "مكة المكرمة", "راوياه: البزي وقنبل",
            "إمام أهل مكة وأحد التابعين؛ لقي عدداً من الصحابة وتشتهر قراءته بصلة ميم الجمع وهاء الكناية.",
            "Tabi'i Imam of Makkah; renowned for smooth lengthening (silah) of plural mim."
        ),
        SanadImamNode(
            "abu_amr", 2, "الإمام أبو عمرو البصري (١٥٤ هـ)", "Imam Abu Amr al-Basri (d. 154 AH)",
            "٦٨–١٥٤ هـ", "البصرة", "راوياه: الدوري والسوسي",
            "سيد القراء واللغويين في عصره؛ تشتهر رواية الدوري في السودان وشرق إفريقيا، وتُعرف قراءته بالإدغام الكبير والإمالة.",
            "Master of Basrah & classical Arabic philology; Douri remains widely recited in Sudan and East Africa."
        ),
        SanadImamNode(
            "ibn_amir", 2, "الإمام ابن عامر الشامي (١١٨ هـ)", "Imam Ibn Amir al-Shami (d. 118 AH)",
            "٨–١١٨ هـ", "دمشق (المسجد الأموي)", "راوياه: هشام وابن ذكوان",
            "أعلى القراء العشرة سنداً وأقدمهم مولداً؛ قاضي دمشق وإمام المسجد الأموي.",
            "Highest unbroken chain among the Ten Imams; Chief Judge and Imam of the Umayyad Mosque in Damascus."
        ),
        SanadImamNode(
            "hamzah_kisai", 2, "الإمامان حمزة والكسائي + الثلاثة المتممون للعشر", "Hamzah, Al-Kisa'i + The Completing 3 (Abu Ja'far, Ya'qub, Khalaf)",
            "١٣٠–٢٢٩ هـ", "الكوفة والمدينة والبصرة", "خلف، خلاد، أبو الحارث، الدوري، ابن وردان، ابن جماز، رويس، روح، إسحاق، إدريس",
            "أتمّ بهم الإمام ابن الجزري منظومة القراءات العشر المتواترة (طيبة النشر) المحفوظة بالأسانيد المتصلة إلى يومنا هذا.",
            "Completed the Ten Mutawatir Qira'at codified by Imam Ibn al-Jazari, preserved with unbroken Ijazah chains today."
        )
    )

    /**
     * Pure deterministic analytics over live AlQuran.cloud search hits:
     * computes Meccan vs Madinan distribution and Top 5 Surahs containing the queried root/word.
     */
    fun analyzeRootDistribution(hits: List<QuranSearchHit>): RootDistributionStats {
        if (hits.isEmpty()) {
            return RootDistributionStats(0, 0, 0, 0, emptyList())
        }
        var meccan = 0
        var madinan = 0
        val countsBySurah = linkedMapOf<Int, Int>()

        hits.forEach { hit ->
            val meta = SurahCatalog.findSurah(hit.surahNumber)
            if (meta.isMeccan) meccan++ else madinan++
            countsBySurah[hit.surahNumber] = (countsBySurah[hit.surahNumber] ?: 0) + 1
        }

        val top5 = countsBySurah.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (surahNum, count) ->
                val meta = SurahCatalog.findSurah(surahNum)
                meta.nameAr to count
            }

        return RootDistributionStats(
            totalMatches = hits.size,
            meccanCount = meccan,
            madinanCount = madinan,
            distinctSurahs = countsBySurah.size,
            topSurahs = top5
        )
    }
}
