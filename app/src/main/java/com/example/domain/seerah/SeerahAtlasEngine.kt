package com.example.domain.seerah

import android.content.Context
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class SeerahEra(val labelAr: String, val labelEn: String, val labelUz: String) {
    ALL("الكل", "All Eras", "Barchasi"),
    MAKKAN("العهد المكي", "Makkan Era", "Makka davri"),
    HIJRAH("الهجرة النبوية", "The Hijrah", "Hijrat yo'li"),
    MADINAN("العهد المدني", "Madinan Era", "Madina davri")
}

data class SeerahChronicleEvent(
    val id: String,
    val era: SeerahEra,
    val yearLabelAr: String,
    val yearLabelEn: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val locationAr: String,
    val locationEn: String,
    val latitude: Double,
    val longitude: Double,
    val summaryAr: String,
    val summaryEn: String,
    val summaryUz: String,
    val lessonAr: String,
    val lessonEn: String
)

data class HijrahWaypoint(
    val stepNumber: Int,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val latitude: Double,
    val longitude: Double,
    val normX: Float, // 0..1 normalized position on Hijrah map canvas
    val normY: Float, // 0..1 normalized position on Hijrah map canvas
    val noteAr: String,
    val noteEn: String
)

data class ShamailSunnahItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val narrationAr: String,
    val narrationEn: String,
    val sourceAr: String,
    val dailyPracticeAr: String,
    val dailyPracticeEn: String
)

/**
 * Interactive Prophetic Seerah Chronicle, Exact GPS Hijrah Route Atlas,
 * and Authentic Shama'il Muhammadiyyah + Daily Sunnah Revival Engine.
 */
object SeerahAtlasEngine {

    val hijrahWaypoints: List<HijrahWaypoint> = listOf(
        HijrahWaypoint(
            1, "مكة المكرمة (بيت النبي ﷺ)", "Makkah (Prophet's Home)", "Makka (Payg'ambar uyi)",
            21.4225, 39.8262, 0.54f, 0.88f,
            "خرج النبي ﷺ ليلاً مع أبي بكر الصديق رضي الله عنه قاصداً جنوب مكة تمويهاً على قريش.",
            "The Prophet ﷺ departed Makkah at night with Abu Bakr (RA), heading south first."
        ),
        HijrahWaypoint(
            2, "غار ثور (٣ ليالٍ)", "Cave of Thawr (3 Nights)", "Savr g'ori (3 kecha)",
            21.3772, 39.8497, 0.68f, 0.94f,
            "«لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا» — مكثا فيه ثلاث ليالٍ وعبد الله بن أبي بكر يأتيهما بالأخبار.",
            "'Do not grieve; indeed Allah is with us' — sheltered for 3 nights south of Makkah."
        ),
        HijrahWaypoint(
            3, "ساحل عسفان وقديد", "Usfan & Qadid Coast", "Usfon va Qadid sohili",
            21.9167, 39.3500, 0.30f, 0.68f,
            "سلك الدليل عبد الله بن أريقط طريق الساحل غير المألوف غرباً ثم شمالاً.",
            "Guide Abdullah ibn Urayqit led them along the uncharted coastal path westward then north."
        ),
        HijrahWaypoint(
            4, "خيمة أم معبد الخزاعية", "Tent of Umm Ma'bad", "Ummu Ma'bad chodiri",
            22.4650, 39.1800, 0.24f, 0.48f,
            "معجزة الشاة الهزيلة ووصف أم معبد الخالد لجمال وبهاء رسول الله ﷺ.",
            "Miracle of the frail ewe and Umm Ma'bad's timeless description of the Prophet ﷺ."
        ),
        HijrahWaypoint(
            5, "العرج ووادي رانوناء", "Al-Arj & Wadi Ranuna", "Al-Arj va Ranuno vodiysi",
            23.7500, 39.4200, 0.42f, 0.27f,
            "العبور عبر الجبال الوعرة والاقتراب من مشارف يثرب بعد أكثر من ٤٠٠ كم.",
            "Crossing rugged mountain passes after over 400 km of desert travel toward Madinah."
        ),
        HijrahWaypoint(
            6, "مسجد قباء (أول مسجد في الإسلام)", "Masjid Quba (First Mosque)", "Qubo masjidi (Ilk masjid)",
            24.4393, 39.6173, 0.56f, 0.14f,
            "أقام النبي ﷺ بقباء وأسس أول مسجد أُسس على التقوى من أول يوم.",
            "Founded the very first mosque in Islam built upon taqwa from day one."
        ),
        HijrahWaypoint(
            7, "المدينة المنورة (المسجد النبوي)", "Al-Masjid an-Nabawi", "Masjidi Nabaviy (Madina)",
            24.4672, 39.6112, 0.52f, 0.06f,
            "استقبال الأنصار الحافل وبناء المسجد النبوي والمؤاخاة بين المهاجرين والأنصار.",
            "Joyful welcome of the Ansar, building Al-Masjid an-Nabawi, and brotherhood of Ummah."
        )
    )

    val chronicleEvents: List<SeerahChronicleEvent> = listOf(
        SeerahChronicleEvent(
            "birth", SeerahEra.MAKKAN, "عام الفيل (٥٧١م)", "Year of the Elephant (571 CE)",
            "مولد خير البشر محمد ﷺ", "Birth of Prophet Muhammad ﷺ", "Payg'ambarimiz Muhammad ﷺ tavalludlari",
            "مكة المكرمة", "Makkah", 21.4225, 39.8262,
            "وُلد النبي ﷺ في شعب بني هاشم بمكة يوم الإثنين ١٢ ربيع الأول عام الفيل، ونشأ يتيماً صادقاً أميناً.",
            "Born in Makkah on Monday, 12 Rabi' al-Awwal in the Year of the Elephant, growing up known as Al-Sadiq Al-Amin.",
            "Fil yili Rabiul-avval oyida Makkada tavallud topdilar va Sodiq hamda Amin sifatlari bilan tanildilar.",
            "الصدق والأمانة هما أساس الدعوة قبل النبوة وبعدها.",
            "Truthfulness and trustworthiness are the foundation of character."
        ),
        SeerahChronicleEvent(
            "hira", SeerahEra.MAKKAN, "رمضان (١٣ ق.هـ / ٦١٠م)", "Ramadan (610 CE)",
            "نزول الوحي الأول في غار حراء", "First Revelation in Cave Hira", "Hiro g'orida ilk vahiy",
            "جبل النور — غار حراء", "Jabal al-Nour — Cave Hira", 21.4575, 39.8592,
            "نزل جبريل عليه السلام بأولى آيات القرآن الكريم: «اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ»، وآزرته أم المؤمنين خديجة رضي الله عنها.",
            "Archangel Jibril revealed the first verses of Surah Al-Alaq ('Read in the name of your Lord'); Khadijah (RA) stood firmly by him.",
            "Jabroil (a.s.) Alaq surasining ilk oyatlarini keltirdilar va Xadicha (r.a.) ularni qo'llab-quvvatladilar.",
            "العلم والقراءة والتأمل هي بوابة النور والهداية.",
            "Knowledge, reflection, and supportive family anchor faith."
        ),
        SeerahChronicleEvent(
            "isra", SeerahEra.MAKKAN, "٢ ق.هـ / ٦٢٠م", "2 BH / 620 CE",
            "الإسراء والمعراج وفرض الصلوات الخمس", "Al-Isra' & Al-Mi'raj — Five Prayers", "Isro va Me'roj — Besh vaqt namoz",
            "المسجد الحرام ← المسجد الأقصى", "Makkah → Al-Aqsa, Al-Quds", 31.7761, 35.2358,
            "أُسرِي بالنبي ﷺ من المسجد الحرام إلى المسجد الأقصى حيث أمّ الأنبياء، ثم عُرِج به إلى السماوات العلا وفُرضت الصلوات الخمس.",
            "Night Journey from Makkah to Al-Aqsa where he led all Prophets in prayer, then ascended above the heavens where the 5 daily prayers were gifted.",
            "Masjidul Haromdan Masjidul Aqsoga sayr qildirilib, so'ngra osmonlarga ko'tarildilar va besh vaqt namoz farz qilindi.",
            "بعد كل شدة وضيق يأتي الفرج الرباني؛ والصلاة معراج المؤمن اليومي.",
            "After hardship comes divine relief; Salah is the believer's daily ascension."
        ),
        SeerahChronicleEvent(
            "hijrah", SeerahEra.HIJRAH, "١ هـ / ٦٢٢م", "1 AH / 622 CE",
            "الهجرة النبوية المباركة إلى المدينة", "The Blessed Hijrah to Madinah", "Madinaga muborak Hijrat",
            "مكة ← غار ثور ← قباء ← المدينة", "Makkah → Thawr → Quba → Madinah", 24.4393, 39.6173,
            "رحلة إيمانية وتخطيط محكم بأكثر من ٤٣٠ كيلومتراً أسّست دولة الإسلام والمجتمع النبوي ووثيقة المدينة.",
            "A 430+ km journey combining immaculate strategic planning with absolute tawakkul in Allah, founding the Madinan community.",
            "430 km dan ortiq masofali hikmatli reja va tavakkul safari bo'lib, Madina jamiyatiga asos soldi.",
            "الأخذ بالأسباب بأعلى درجات الإتقان مع تمام التوكل على الله.",
            "Pair meticulous preparation with complete reliance on Allah."
        ),
        SeerahChronicleEvent(
            "badr", SeerahEra.MADINAN, "١٧ رمضان ٢ هـ", "17 Ramadan 2 AH",
            "غزوة بدر الكبرى (يوم الفرقان)", "Battle of Badr (Yawm al-Furqan)", "Badr g'azoti (Furqon kuni)",
            "بدر — جنوب غرب المدينة", "Badr, SW of Madinah", 23.7800, 38.7900,
            "نصر الله المؤمنين وهم ٣١٣ رجلاً وأمدّهم بالملائكة، ففرّق الله في ذلك اليوم بين الحق والباطل.",
            "With 313 companions and angelic aid, Allah granted a decisive victory that distinguished truth from falsehood.",
            "313 sahoba bilan Allohning madadi va farishtalar yordami ila haq va botil ajralgan buyuk g'alaba.",
            "النصر بالثبات والدعاء والصدق لا بكثرة العدد.",
            "Victory comes through sincerity, supplication, and steadfastness."
        ),
        SeerahChronicleEvent(
            "hudaybiyyah", SeerahEra.MADINAN, "ذو القعدة ٦ هـ", "Dhul Qi'dah 6 AH",
            "صلح الحديبية (الفتح المبين)", "Treaty of Hudaybiyyah (Clear Victory)", "Hudaybiya sulhi (Ochiq g'alaba)",
            "الحديبية — غرب مكة", "Hudaybiyyah, West of Makkah", 21.4414, 39.6075,
            "صلح استراتيجي حكيم سمّاه القرآن «فَتْحًا مُّبِينًا» وأتاح انتشار الدعوة في جزيرة العرب وخارجها بسلام.",
            "A visionary peace treaty named 'Fathan Mubina' (Clear Victory) by the Quran, opening hearts across Arabia.",
            "Qur'onda 'Ochiq g'alaba' deb nomlangan hikmatli sulh bo'lib, Islom tinchlik bilan keng tarqalishiga yo'l ochdi.",
            "الحكمة وبُعد النظر في السلم يفتحان ما لا تفتحه العجلة.",
            "Long-term strategic wisdom and peace yield enduring triumph."
        ),
        SeerahChronicleEvent(
            "fath_makkah", SeerahEra.MADINAN, "٢٠ رمضان ٨ هـ", "20 Ramadan 8 AH",
            "فتح مكة الأعظم والعفو العام", "Conquest of Makkah & Universal Amnesty", "Makka fathi va buyuk afv",
            "مكة المكرمة — الكعبة المشرفة", "Makkah — The Holy Kaaba", 21.4225, 39.8262,
            "دخل النبي ﷺ مكة مطأطئاً رأسه تواضعاً لله، وطهّر الكعبة من الأصنام، وقال لأهلها: «اذهبوا فأنتم الطلقاء».",
            "Entered Makkah bowing in humility to Allah, cleared the Kaaba, and forgave former adversaries: 'Go, for you are free.'",
            "Tavozu bilan Makkaga kirib, Ka'bani pokladilar va barchani kechirib: 'Boringlar, sizlar ozodsizlar', dedilar.",
            "التواضع عند النصر والعفو عند المقدرة أسمى أخلاق الإسلام.",
            "Humility in triumph and mercy when holding power are prophetic perfection."
        ),
        SeerahChronicleEvent(
            "farewell", SeerahEra.MADINAN, "٩ ذو الحجة ١٠ هـ", "9 Dhul Hijjah 10 AH",
            "حجة الوداع وخطبة عرفة الخالدة", "Farewell Pilgrimage & Arafah Sermon", "Vado haji va Arafot xutbasi",
            "صعيد عرفات — جبل الرحمة", "Plains of Arafah", 21.3549, 39.9841,
            "أعلن النبي ﷺ ميثاق الكرامة الإنسانية والمساواة، ونزلت «الْيَوْمَ أَكْمَلْتُ لَكُمْ دِينَكُمْ».",
            "Declared universal human dignity and equality before over 100,000 companions as Surah Al-Ma'idah 5:3 was revealed.",
            "Yuz mingdan ortiq sahoba oldida insoniy tenglik va qadr-qimmat xutbasini o'qidilar.",
            "لا فضل لعربي على أعجمي إلا بالتقوى والعمل الصالح.",
            "True nobility lies solely in taqwa and righteous deeds."
        )
    )

    val shamailSunnahs: List<ShamailSunnahItem> = listOf(
        ShamailSunnahItem(
            "smile", "الابتسامة في وجوه الناس", "Prophetic Radiant Smile", "Tabassum qilish sunnati",
            "قال عبد الله بن الحارث رضي الله عنه: «ما رأيت أحداً أكثر تبسماً من رسول الله ﷺ».",
            "Abdullah ibn al-Harith (RA): 'I have never seen anyone smile more than the Messenger of Allah ﷺ.'",
            "الشمائل المحمدية للترمذي (صحيح)",
            "أدخل السرور اليوم بابتسامة صادقة لكل من تلقاه.",
            "Share a genuine warm smile with everyone you meet today."
        ),
        ShamailSunnahItem(
            "salam", "البدء بالسلام ولين الكلام", "Initiating Salam & Gentle Speech", "Salomni boshlash va muloyim so'z",
            "كان النبي ﷺ يبدأ من لقيه بالسلام، ويمشي مع الأرملة والمسكين حتى يقضي حاجتهما.",
            "The Prophet ﷺ initiated Salam with everyone he met and walked humbly with the needy until their needs were fulfilled.",
            "صحيح البخاري ومسلم والشمائل",
            "بادر بالسلام على أهلك وجيرانك وزملائك اليوم.",
            "Be the first to offer Assalamu Alaikum to family and neighbors today."
        ),
        ShamailSunnahItem(
            "siwak", "السواك وطهارة الفم عند كل صلاة", "Siwak & Purity Before Prayer", "Har namozdan oldin misvok",
            "«لولا أن أشق على أمتي لأمرتهم بالسواك عند كل صلاة» — وكان أول ما يبدأ به عند دخول بيته.",
            "'Were it not for hardship upon my Ummah, I would have commanded the Siwak before every prayer.'",
            "متفق عليه",
            "جدّد طهارة الفم بالسواك أو الفرشاة قبل كل وضوء وصلاة.",
            "Refresh oral purity before every Wudu and prayer today."
        ),
        ShamailSunnahItem(
            "family_help", "خدمة الأهل والتواضع في البيت", "Serving One's Family at Home", "Oilaga yordam va kamtarlik",
            "قالت عائشة رضي الله عنها: «كان يكون في مهنة أهله، فإذا حضرت الصلاة خرج إلى الصلاة».",
            "Aisha (RA): 'He used to serve his family at home, and when prayer time came, he went out to pray.'",
            "صحيح البخاري",
            "ساعد أهلك اليوم في عمل منزلي بنيّة إحياء سنة النبي ﷺ.",
            "Help a family member at home today intending to revive the Sunnah."
        ),
        ShamailSunnahItem(
            "duha", "صلاة الضحى وقيام الليل", "Duha Prayer & Night Devotion", "Zuho va tungi ibodat",
            "أوصى النبي ﷺ أبا هريرة رضي الله عنه بصيام ثلاثة أيام من كل شهر وركعتي الضحى والوتر قبل النوم.",
            "The Prophet ﷺ advised Abu Hurairah (RA) never to leave 2 rak'ahs of Duha and Witr before sleep.",
            "صحيح البخاري ومسلم",
            "صلِّ ركعتي الضحى صباحاً والوتر قبل نومك الليلة.",
            "Pray 2 rak'ahs of Duha this morning and Witr tonight."
        ),
        ShamailSunnahItem(
            "salawat", "الإكثار من الصلاة على النبي ﷺ", "Abundant Salawat upon the Prophet ﷺ", "Payg'ambarimizga ko'p salovot",
            "«من صلى عليَّ صلاةً صلى الله عليه بها عشراً» — اللهم صلِّ وسلم على نبينا محمد.",
            "'Whoever sends one blessing upon me, Allah sends ten blessings upon him.'",
            "صحيح مسلم",
            "قل الآن ١٠ مرات: اللهم صلِّ وسلِّم وبارك على نبينا محمد ﷺ.",
            "Recite 10 heartfelt Salawat upon Prophet Muhammad ﷺ right now."
        )
    )

    /**
     * Exact Haversine great-circle distance in kilometers between two GPS coordinates.
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return earthRadiusKm * c
    }

    /**
     * Total cumulative overland Hijrah route length across the 7 historical waypoints (km).
     */
    fun totalHijrahRouteKm(): Double {
        var total = 0.0
        for (i in 0 until hijrahWaypoints.lastIndex) {
            val a = hijrahWaypoints[i]
            val b = hijrahWaypoints[i + 1]
            total += distanceKm(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return total
    }

    private const val PREFS_NAME = "noor_seerah_sunnah_revival_v1"

    fun loadCompletedSunnahs(context: Context): Set<String> {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet("completed_sunnah_ids", emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun toggleSunnahCompleted(context: Context, id: String): Set<String> {
        val current = loadCompletedSunnahs(context).toMutableSet()
        if (!current.add(id)) current.remove(id)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet("completed_sunnah_ids", current)
            .apply()
        return current
    }
}
