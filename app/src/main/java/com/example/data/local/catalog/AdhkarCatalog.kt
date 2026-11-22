package com.example.data.local.catalog

import com.example.domain.model.AdhkarCategory
import com.example.domain.model.AuthenticHadith
import com.example.domain.model.DhikrItem
import com.example.domain.model.TasbihPreset

data class DailyAyahItem(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val textAr: String,
    val meaningEn: String
) {
    val referenceAr: String get() = "سورة $surahNameAr · آية $ayahNumber"
    val referenceEn: String get() = "$surahNameEn ($surahNumber:$ayahNumber)"
}

object AdhkarCatalog {

    val tasbihPresets: List<TasbihPreset> = listOf(
        TasbihPreset("subhanallah", "سُبْحَانَ اللَّهِ", "SubhanAllah (Glory be to Allah)", 33),
        TasbihPreset("alhamdulillah", "الْحَمْدُ لِلَّهِ", "Alhamdulillah (Praise be to Allah)", 33),
        TasbihPreset("allahuakbar", "اللَّهُ أَكْبَرُ", "Allahu Akbar (Allah is the Greatest)", 33),
        TasbihPreset("istighfar", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ", "Astaghfirullah al-Azim (I seek Allah's forgiveness)", 100),
        TasbihPreset("tahlil", "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "La ilaha illAllah wahdahu la sharika lah", 100),
        TasbihPreset("hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "La hawla wa la quwwata illa billah", 33),
        TasbihPreset("salawat", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "Allahumma salli wa sallim 'ala Nabiyyina Muhammad", 100),
        TasbihPreset("subhanallah_bihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ", "SubhanAllahi wa bihamdihi, SubhanAllahil-Azim", 100)
    )

    val dailyAyahs: List<DailyAyahItem> = listOf(
        DailyAyahItem(94, 6, "الشرح", "Ash-Sharh", "إِنَّ مَعَ الْعُسْرِ يُسْرًا", "Indeed, with hardship comes ease."),
        DailyAyahItem(13, 28, "الرعد", "Ar-Ra'd", "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "Unquestionably, by the remembrance of Allah hearts are assured."),
        DailyAyahItem(2, 286, "البقرة", "Al-Baqarah", "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond that it can bear."),
        DailyAyahItem(2, 152, "البقرة", "Al-Baqarah", "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ", "So remember Me; I will remember you. And be grateful to Me and do not deny Me."),
        DailyAyahItem(65, 3, "الطلاق", "At-Talaq", "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ", "And whoever relies upon Allah — then He is sufficient for him."),
        DailyAyahItem(39, 53, "الزمر", "Az-Zumar", "لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا", "Do not despair of the mercy of Allah. Indeed, Allah forgives all sins."),
        DailyAyahItem(24, 35, "النور", "An-Nur", "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ", "Allah is the Light of the heavens and the earth."),
        DailyAyahItem(2, 186, "البقرة", "Al-Baqarah", "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ", "And when My servants ask you concerning Me — indeed I am near. I respond to the invocation of the supplicant when he calls upon Me."),
        DailyAyahItem(14, 7, "إبراهيم", "Ibrahim", "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ", "If you are grateful, I will surely increase you in favor."),
        DailyAyahItem(3, 139, "آل عمران", "Aal-E-Imran", "وَلَا تَهِنُوا وَلَا تَحْزَنُوا وَأَنتُمُ الْأَعْلَوْنَ إِن كُنتُم مُّؤْمِنِينَ", "So do not weaken and do not grieve, and you will be superior if you are true believers.")
    )

    /**
     * Verified Authentic Hadiths from Sahih al-Bukhari and Sahih Muslim.
     */
    val dailyHadiths: List<AuthenticHadith> = listOf(
        AuthenticHadith(
            id = "h_1",
            textAr = "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى».",
            textEn = "\"Actions are judged by intentions, and every person will have what they intended.\"",
            narratorAr = "عمر بن الخطاب رضي الله عنه",
            narratorEn = "Umar ibn al-Khattab (RA)",
            sourceAr = "صحيح البخاري (١) وصحيح مسلم (١٩٠٧)",
            sourceEn = "Sahih al-Bukhari 1, Sahih Muslim 1907",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)"
        ),
        AuthenticHadith(
            id = "h_2",
            textAr = "«أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ تَعَالَى أَدْوَمُهَا وَإِنْ قَلَّ».",
            textEn = "\"The most beloved of deeds to Allah are those that are most consistent, even if they are small.\"",
            narratorAr = "عائشة رضي الله عنها",
            narratorEn = "Aisha (RA)",
            sourceAr = "صحيح البخاري (٦٤٦٤) وصحيح مسلم (٧٨٣)",
            sourceEn = "Sahih al-Bukhari 6464, Sahih Muslim 783",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)"
        ),
        AuthenticHadith(
            id = "h_3",
            textAr = "«خَيْرُكُمْ مَنْ تَعَلَّمَ القُرْآنَ وَعَلَّمَهُ».",
            textEn = "\"The best among you are those who learn the Quran and teach it.\"",
            narratorAr = "عثمان بن عفان رضي الله عنه",
            narratorEn = "Uthman ibn Affan (RA)",
            sourceAr = "صحيح البخاري (٥٠٢٧)",
            sourceEn = "Sahih al-Bukhari 5027",
            gradingAr = "صحيح",
            gradingEn = "Sahih"
        ),
        AuthenticHadith(
            id = "h_4",
            textAr = "«كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي المِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ العَظِيمِ».",
            textEn = "\"Two phrases are light on the tongue, heavy on the Scale, and beloved to the Most Merciful: SubhanAllahi wa bihamdihi, SubhanAllahil-Azim.\"",
            narratorAr = "أبو هريرة رضي الله عنه",
            narratorEn = "Abu Hurairah (RA)",
            sourceAr = "صحيح البخاري (٦٤٠٦) وصحيح مسلم (٢٦٩٤)",
            sourceEn = "Sahih al-Bukhari 6406, Sahih Muslim 2694",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)"
        ),
        AuthenticHadith(
            id = "h_5",
            textAr = "«مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لَا يَذْكُرُ رَبَّهُ مَثَلُ الحَيِّ وَالمَيِّتِ».",
            textEn = "\"The example of the one who remembers his Lord and the one who does not remember his Lord is like that of the living and the dead.\"",
            narratorAr = "أبو موسى الأشعري رضي الله عنه",
            narratorEn = "Abu Musa al-Ash'ari (RA)",
            sourceAr = "صحيح البخاري (٦٤٠٧)",
            sourceEn = "Sahih al-Bukhari 6407",
            gradingAr = "صحيح",
            gradingEn = "Sahih"
        ),
        AuthenticHadith(
            id = "h_6",
            textAr = "«مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا، سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الجَنَّةِ».",
            textEn = "\"Whoever treads a path seeking knowledge, Allah will make easy for him a path to Paradise.\"",
            narratorAr = "أبو هريرة رضي الله عنه",
            narratorEn = "Abu Hurairah (RA)",
            sourceAr = "صحيح مسلم (٢٦٩٩)",
            sourceEn = "Sahih Muslim 2699",
            gradingAr = "صحيح",
            gradingEn = "Sahih"
        ),
        AuthenticHadith(
            id = "h_7",
            textAr = "«لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ».",
            textEn = "\"None of you truly believes until he loves for his brother what he loves for himself.\"",
            narratorAr = "أنس بن مالك رضي الله عنه",
            narratorEn = "Anas ibn Malik (RA)",
            sourceAr = "صحيح البخاري (١٣) وصحيح مسلم (٤٥)",
            sourceEn = "Sahih al-Bukhari 13, Sahih Muslim 45",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)"
        ),
        AuthenticHadith(
            id = "h_8",
            textAr = "«مَنْ صَلَّى عَلَيَّ وَاحِدَةً، صَلَّى اللَّهُ عَلَيْهِ عَشْرًا».",
            textEn = "\"Whoever sends blessings upon me once, Allah will send blessings upon him tenfold.\"",
            narratorAr = "أبو هريرة رضي الله عنه",
            narratorEn = "Abu Hurairah (RA)",
            sourceAr = "صحيح مسلم (٤٠٨)",
            sourceEn = "Sahih Muslim 408",
            gradingAr = "صحيح",
            gradingEn = "Sahih"
        ),
        AuthenticHadith(
            id = "h_9",
            textAr = "«الطُّهُورُ شَطْرُ الإِيمَانِ، وَالحَمْدُ لِلَّهِ تَمْلَأُ المِيزَانَ».",
            textEn = "\"Purity is half of faith, and Alhamdulillah (praise be to Allah) fills the Scale.\"",
            narratorAr = "أبو مالك الأشعري رضي الله عنه",
            narratorEn = "Abu Malik al-Ash'ari (RA)",
            sourceAr = "صحيح مسلم (٢٢٣)",
            sourceEn = "Sahih Muslim 223",
            gradingAr = "صحيح",
            gradingEn = "Sahih"
        ),
        AuthenticHadith(
            id = "h_10",
            textAr = "«يَسِّرُوا وَلَا تُعَسِّرُوا، وَبَشِّرُوا وَلَا تُنَفِّرُوا».",
            textEn = "\"Make things easy and do not make them difficult; give glad tidings and do not drive people away.\"",
            narratorAr = "أنس بن مالك رضي الله عنه",
            narratorEn = "Anas ibn Malik (RA)",
            sourceAr = "صحيح البخاري (٦٩) وصحيح مسلم (١٧٣٤)",
            sourceEn = "Sahih al-Bukhari 69, Sahih Muslim 1734",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)"
        )
    )

    // Categories 1..12 (Morning & Evening from MorningEveningAdhkarCatalog + After-Prayer through Anxiety)
    private val adhkarPart1: List<DhikrItem> =
        MorningEveningAdhkarCatalog.morningAdhkar +
            MorningEveningAdhkarCatalog.eveningAdhkar +
            listOf(
        // === 3. أذكار بعد الصلاة (AFTER_PRAYER) ===
        DhikrItem(
            id = "ap_1",
            category = AdhkarCategory.AFTER_PRAYER,
            textAr = "أَسْتَغْفِرُ اللَّهَ (ثَلَاثًا)، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٥٩١)",
            sourceEn = "Sahih Muslim 591",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "أول ما يقوله المصلي بعد السلام لجبر نقص الصلاة والثناء على الله.",
            meaningEn = "I seek Allah's forgiveness (3x). O Allah, You are Peace and from You comes peace. Blessed are You, O Possessor of Majesty and Honor.",
            transliteration = "Astaghfirullah (3x). Allahumma Antas-Salam wa minkas-salam, tabarakta ya Dhal-Jalali wal-Ikram."
        ),
        DhikrItem(
            id = "ap_2",
            category = AdhkarCategory.AFTER_PRAYER,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٨٤٤) ومسلم (٥٩٣)",
            sourceEn = "Sahih al-Bukhari 844 & Muslim 593",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "توحيد الله والتسليم الكامل لعطائه ومنعه وحكمته.",
            meaningEn = "None can withhold what You give, nor give what You withhold, and worldly fortune avails no one against Your decree.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah... Allahumma la mani'a lima a'tayta..."
        ),
        DhikrItem(
            id = "ap_3",
            category = AdhkarCategory.AFTER_PRAYER,
            textAr = "سُبْحَانَ اللَّهِ (٣٣)، وَالْحَمْدُ لِلَّهِ (٣٣)، وَاللَّهُ أَكْبَرُ (٣٣)، ثُمَّ تَمَامُ الْمِائَةِ: لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 33,
            sourceAr = "صحيح مسلم (٥٩٧)",
            sourceEn = "Sahih Muslim 597",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من سبح وحمد وكبر دبر كل صلاة ثلاثاً وثلاثين وختم بالتهليل غُفرت خطاياه وإن كانت مثل زبد البحر.",
            meaningEn = "Reciting SubhanAllah, Alhamdulillah, and Allahu Akbar 33 times each after prayer, completed to 100 with Tahlil.",
            transliteration = "SubhanAllah (33), Alhamdulillah (33), Allahu Akbar (33), then La ilaha illAllah..."
        ),
        DhikrItem(
            id = "ap_4",
            category = AdhkarCategory.AFTER_PRAYER,
            textAr = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (١٥٢٢) والنسائي (١٣٠٣)",
            sourceEn = "Sunan Abi Dawud 1522 & Al-Nasa'i 1303",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "وصية النبي ﷺ لمعاذ بن جبل رضي الله عنه ألا يدعها دبر كل صلاة.",
            meaningEn = "O Allah, help me to remember You, thank You, and worship You in excellence.",
            transliteration = "Allahumma a'inni 'ala dhikrika, wa shukrika, wa husni 'ibadatik."
        ),
        DhikrItem(
            id = "ap_5",
            category = AdhkarCategory.AFTER_PRAYER,
            textAr = "قراءة آية الكرسي دبر كل صلاة مكتوبة: ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...﴾",
            targetCount = 1,
            sourceAr = "النسائي في السنن الكبرى (٩٩٢٨)",
            sourceEn = "Al-Nasa'i al-Kubra 9928",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قرأ آية الكرسي دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا أن يموت.",
            meaningEn = "Reciting Ayat al-Kursi after every obligatory prayer is a means to Paradise.",
            transliteration = "Recite Ayat al-Kursi (Surah Al-Baqarah 2:255) after each obligatory prayer."
        ),

        // === 4. أذكار النوم (SLEEP) ===
        DhikrItem(
            id = "sl_1",
            category = AdhkarCategory.SLEEP,
            textAr = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٢٤)",
            sourceEn = "Sahih al-Bukhari 6324",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "ذكر مختصر يقال عند وضع الجنب للنوم استحضاراً لمعية الله.",
            meaningEn = "In Your name, O Allah, I die (sleep) and I live.",
            transliteration = "Bismika Allahumma amutu wa ahya."
        ),
        DhikrItem(
            id = "sl_2",
            category = AdhkarCategory.SLEEP,
            textAr = "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٤٥) والترمذي (٣٣٩٨)",
            sourceEn = "Sunan Abi Dawud 5045 & At-Tirmidhi 3398",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان النبي ﷺ إذا أراد أن يرقد وضع يده اليمنى تحت خده وقالها ثلاثاً.",
            meaningEn = "O Allah, protect me from Your punishment on the Day You resurrect Your servants.",
            transliteration = "Allahumma qini 'adhabaka yawma tab'athu 'ibadak."
        ),
        DhikrItem(
            id = "sl_3",
            category = AdhkarCategory.SLEEP,
            textAr = "اللَّهُمَّ أَسْلَمْتُ نَفْسِي إِلَيْكَ، وَفَوَّضْتُ أَمْرِي إِلَيْكَ، وَوَجَّهْتُ وَجْهِي إِلَيْكَ، وَأَلْجَأْتُ ظَهْرِي إِلَيْكَ، رَغْبَةً وَرَهْبَةً إِلَيْكَ، لَا مَلْجَأَ وَلَا مَنْجَا مِنْكَ إِلَّا إِلَيْكَ، آمَنْتُ بِكِتَابِكَ الَّذِي أَنْزَلْتَ، وَبِنَبِيِّكَ الَّذِي أَرْسَلْتَ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٢٤٧) ومسلم (٢٧١٠)",
            sourceEn = "Sahih al-Bukhari 247 & Muslim 2710",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "من جعلها آخر ما يقول قبل النوم فمات من ليلته مات على الفطرة.",
            meaningEn = "Complete surrender, reliance, and faith in Allah before sleep.",
            transliteration = "Allahumma aslamtu nafsi ilayka, wa fawwadtu amri ilayka..."
        ),
        DhikrItem(
            id = "sl_4",
            category = AdhkarCategory.SLEEP,
            textAr = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَكَفَانَا وَآوَانَا، فَكَمْ مِمَّنْ لَا كَافِيَ لَهُ وَلَا مُؤْوِيَ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧١٥)",
            sourceEn = "Sahih Muslim 2715",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "شكر الله عند المبيت على نعم الطعام والشراب والكفاية والمسكن.",
            meaningEn = "Praise be to Allah who fed us, gave us drink, sufficed us, and sheltered us.",
            transliteration = "Alhamdulillahil-ladhi at'amana wa saqana wa kafana wa awana."
        ),
        DhikrItem(
            id = "sl_5",
            category = AdhkarCategory.SLEEP,
            textAr = "سُبْحَانَ اللَّهِ (٣٣)، وَالْحَمْدُ لِلَّهِ (٣٣)، وَاللَّهُ أَكْبَرُ (٣٤).",
            targetCount = 34,
            sourceAr = "صحيح البخاري (٣٧٠٥) ومسلم (٢٧٢٧)",
            sourceEn = "Sahih al-Bukhari 3705 & Muslim 2727",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "وصية النبي ﷺ لفاطمة وعلي رضي الله عنهما عند النوم؛ خير من خادم.",
            meaningEn = "SubhanAllah 33x, Alhamdulillah 33x, and Allahu Akbar 34x before sleep.",
            transliteration = "SubhanAllah (33), Alhamdulillah (33), Allahu Akbar (34)."
        ),

        // === 5. أذكار الاستيقاظ (WAKING) ===
        DhikrItem(
            id = "wk_1",
            category = AdhkarCategory.WAKING,
            textAr = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣١٢)",
            sourceEn = "Sahih al-Bukhari 6312",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "حمد الله على تجديد الحياة مع كل صباح جديد.",
            meaningEn = "All praise is for Allah who gave us life after having taken it from us, and unto Him is the resurrection.",
            transliteration = "Alhamdulillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur."
        ),
        DhikrItem(
            id = "wk_2",
            category = AdhkarCategory.WAKING,
            textAr = "الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي فِي جَسَدِي، وَرَدَّ عَلَيَّ رُوحِي، وَأَذِنَ لِي بِذِكْرِهِ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٤٠١)",
            sourceEn = "Sunan At-Tirmidhi 3401",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "شكر الله على العافية ورد الروح والتوفيق للذكر.",
            meaningEn = "Praise be to Allah who restored my health, returned my soul, and permitted me to remember Him.",
            transliteration = "Alhamdulillahil-ladhi 'afani fi jasadi, wa radda 'alayya ruhi, wa adhina li bi-dhikrih."
        ),
        DhikrItem(
            id = "wk_3",
            category = AdhkarCategory.WAKING,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَٰهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ، رَبِّ اغْفِرْ لِي.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (١١٥٤)",
            sourceEn = "Sahih al-Bukhari 1154",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من تعارّ من الليل فقالها ثم قال اللهم اغفر لي أو دعا استُجيب له.",
            meaningEn = "Recited upon waking at night; whoever recites it and asks forgiveness or supplicates is answered.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah... Rabbighfir li."
        ),

        // === 6. أذكار الوضوء (WUDU) ===
        DhikrItem(
            id = "wd_1",
            category = AdhkarCategory.WUDU,
            textAr = "بِسْمِ اللَّهِ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (١٠١) وابن ماجه (٣٩٧)",
            sourceEn = "Sunan Abi Dawud 101 & Ibn Majah 397",
            gradingAr = "حسن صحيح",
            gradingEn = "Hasan Sahih",
            meaningAr = "يقال قبل البدء بالوضوء تبركاً باسم الله.",
            meaningEn = "In the name of Allah — said before commencing Wudu.",
            transliteration = "Bismillah."
        ),
        DhikrItem(
            id = "wd_2",
            category = AdhkarCategory.WUDU,
            textAr = "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٣٤)",
            sourceEn = "Sahih Muslim 234",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها بعد إسباغ الوضوء فُتحت له أبواب الجنة الثمانية يدخل من أيها شاء.",
            meaningEn = "Said upon completing Wudu; the eight gates of Paradise are opened for the one who recites it.",
            transliteration = "Ash-hadu an la ilaha illAllahu wahdahu la sharika lah, wa ash-hadu anna Muhammadan 'abduhu wa rasuluh."
        ),
        DhikrItem(
            id = "wd_3",
            category = AdhkarCategory.WUDU,
            textAr = "اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٥٥)",
            sourceEn = "Sunan At-Tirmidhi 55",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "زيادة مباركة بعد الشهادتين عقب الفراغ من الوضوء طلباً للطهارة الظاهرة والباطنة.",
            meaningEn = "O Allah, make me among those who constantly repent and among those who purify themselves.",
            transliteration = "Allahummaj-'alni minat-tawwabina waj-'alni minal-mutatahhirin."
        ),
        DhikrItem(
            id = "wd_4",
            category = AdhkarCategory.WUDU,
            textAr = "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ.",
            targetCount = 1,
            sourceAr = "النسائي في عمل اليوم والليلة (٨١)",
            sourceEn = "Al-Nasa'i 'Amal al-Yawm wal-Laylah 81",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها بعد الوضوء كُتبت في رقٍّ ثم طُبع عليها بطابع فلم تُكسر إلى يوم القيامة.",
            meaningEn = "Glory be to You, O Allah, and praise be to You; I bear witness there is no deity except You, I seek Your forgiveness and repent to You.",
            transliteration = "SubhanakAllahumma wa bihamdika, ash-hadu an la ilaha illa Anta, astaghfiruka wa atubu ilayk."
        ),

        // === 7. دخول وخروج المسجد (MOSQUE) ===
        DhikrItem(
            id = "ms_1",
            category = AdhkarCategory.MOSQUE,
            textAr = "أَعُوذُ بِاللَّهِ الْعَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ، بِسْمِ اللَّهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٧١٣) وسنن أبي داود (٤٦٦)",
            sourceEn = "Sahih Muslim 713 & Abi Dawud 466",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال عند دخول المسجد بالقدم اليمنى؛ يحفظ الله قائله من الشيطان سائر اليوم ويفتح له أبواب رحمته.",
            meaningEn = "Said upon entering the mosque with the right foot, seeking Allah's refuge and asking Him to open the doors of His mercy.",
            transliteration = "A'udhu billahil-'Azim... Bismillah wassalatu wassalamu 'ala Rasulillah, Allahummaf-tah li abwaba rahmatik."
        ),
        DhikrItem(
            id = "ms_2",
            category = AdhkarCategory.MOSQUE,
            textAr = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ، اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٧١٣) وابن ماجه (٧٧٣)",
            sourceEn = "Sahih Muslim 713 & Ibn Majah 773",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال عند الخروج من المسجد بالقدم اليسرى طلباً لفضل الله وحفظه.",
            meaningEn = "Said upon leaving the mosque with the left foot, asking Allah from His bounty and protection.",
            transliteration = "Bismillah wassalatu wassalamu 'ala Rasulillah, Allahumma inni as'aluka min fadlik, Allahumma'simni minash-shaytanir-rajim."
        ),
        DhikrItem(
            id = "ms_3",
            category = AdhkarCategory.MOSQUE,
            textAr = "اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُورًا، وَفِي لِسَانِي نُورًا، وَاجْعَلْ فِي سَمْعِي نُورًا، وَاجْعَلْ فِي بَصَرِي نُورًا، وَاجْعَلْ مِنْ خَلْفِي نُورًا، وَمِنْ أَمَامِي نُورًا، وَاجْعَلْ مِنْ فَوْقِي نُورًا، وَمِنْ تَحْتِي نُورًا، اللَّهُمَّ أَعْطِنِي نُورًا.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣١٦) ومسلم (٧٦٣)",
            sourceEn = "Sahih al-Bukhari 6316 & Muslim 763",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "دعاء الذهاب إلى المسجد بطلب النور في القلب والجوارح والاتجاهات.",
            meaningEn = "Supplication when walking to the mosque: O Allah, place light in my heart, tongue, hearing, sight, and all around me.",
            transliteration = "Allahummaj-'al fi qalbi nuran, wa fi lisani nuran, wa fi sam'i nuran..."
        ),

        // === 8. دخول وخروج المنزل (HOME) ===
        DhikrItem(
            id = "hm_1",
            category = AdhkarCategory.HOME,
            textAr = "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٩٥) والترمذي (٣٤٢٦)",
            sourceEn = "Sunan Abi Dawud 5095 & At-Tirmidhi 3426",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال عند الخروج من المنزل فيقال له: هُديت وكُفيت ووُقيت وتنحّى عنه الشيطان.",
            meaningEn = "In the name of Allah, I place my trust in Allah; there is no power or might except through Allah.",
            transliteration = "Bismillah, tawakkaltu 'alAllah, wa la hawla wa la quwwata illa billah."
        ),
        DhikrItem(
            id = "hm_2",
            category = AdhkarCategory.HOME,
            textAr = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٩٤) والترمذي (٣٤٢٧)",
            sourceEn = "Sunan Abi Dawud 5094 & At-Tirmidhi 3427",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "استعاذة نبوية عند مغادرة البيت من الضلال والزلل والظلم والجهل.",
            meaningEn = "O Allah, I seek refuge in You lest I stray or be led astray, slip or be made to slip, oppress or be oppressed, or behave foolishly.",
            transliteration = "Allahumma inni a'udhu bika an adilla aw udalla, aw azilla aw uzalla..."
        ),
        DhikrItem(
            id = "hm_3",
            category = AdhkarCategory.HOME,
            textAr = "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٩٦)",
            sourceEn = "Sunan Abi Dawud 5096",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "يقال عند دخول المنزل ثم يُسلّم على أهله لتحل البركة والسكينة في البيت.",
            meaningEn = "In the name of Allah we enter, in the name of Allah we leave, and upon our Lord we rely (followed by greeting the household with Salam).",
            transliteration = "Bismillahi walajna, wa bismillahi kharajna, wa 'alAllahi Rabbina tawakkalna."
        ),

        // === 9. أذكار الطعام والشراب (FOOD_DRINK) ===
        DhikrItem(
            id = "fd_1",
            category = AdhkarCategory.FOOD_DRINK,
            textAr = "بِسْمِ اللَّهِ. فإن نسي في أوله فليقل: بِسْمِ اللَّهِ فِي أَوَّلِهِ وَآخِرِهِ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٣٧٦٧) والترمذي (١٨٥٨)",
            sourceEn = "Sunan Abi Dawud 3767 & At-Tirmidhi 1858",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "التسمية قبل الطعام تجلب البركة وتمنع الشيطان من المشاركة فيه.",
            meaningEn = "Say Bismillah before eating; if forgotten at the start, say: In the name of Allah at its beginning and its end.",
            transliteration = "Bismillah (or Bismillahi fi awwalihi wa akhirih)."
        ),
        DhikrItem(
            id = "fd_2",
            category = AdhkarCategory.FOOD_DRINK,
            textAr = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٤٠٢٣) والترمذي (٣٤٥٨)",
            sourceEn = "Sunan Abi Dawud 4023 & At-Tirmidhi 3458",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها بعد فراغه من الطعام غُفر له ما تقدم من ذنبه.",
            meaningEn = "All praise is for Allah who fed me this and provided it for me without any might or power from myself.",
            transliteration = "Alhamdulillahil-ladhi at'amani hadha wa razaqanihi min ghayri hawlin minni wa la quwwah."
        ),
        DhikrItem(
            id = "fd_3",
            category = AdhkarCategory.FOOD_DRINK,
            textAr = "اللَّهُمَّ بَارِكْ لَنَا فِيهِ وَأَطْعِمْنَا خَيْرًا مِنْهُ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٤٥٥) وأبي داود (٣٧٣٠)",
            sourceEn = "Sunan At-Tirmidhi 3455 & Abi Dawud 3730",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "دعاء البركة في الطعام، وفي شرب اللبن يقال: اللهم بارك لنا فيه وزدنا منه.",
            meaningEn = "O Allah, bless us in it and feed us what is better than it.",
            transliteration = "Allahumma barik lana fihi wa at'imna khayran minh."
        ),
        DhikrItem(
            id = "fd_4",
            category = AdhkarCategory.FOOD_DRINK,
            textAr = "أَفْطَرَ عِنْدَكُمُ الصَّائِمُونَ، وَأَكَلَ طَعَامَكُمُ الْأَبْرَارُ، وَصَلَّتْ عَلَيْكُمُ الْمَلَائِكَةُ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٣٨٥٤)",
            sourceEn = "Sunan Abi Dawud 3854",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "الدعاء للضيف والمضيف عند تناول الطعام أو الإفطار عند أهل بيت.",
            meaningEn = "May the fasting break their fast with you, the righteous eat your food, and the angels pray for blessings upon you.",
            transliteration = "Aftara 'indakumus-sa'imun, wa akala ta'amakumul-abrar, wa sallat 'alaykumul-mala'ikah."
        ),

        // === 10. أذكار السفر (TRAVEL) ===
        DhikrItem(
            id = "tr_1",
            category = AdhkarCategory.TRAVEL,
            textAr = "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَلِيفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ، وَكَآبَةِ الْمَنْظَرِ، وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (١٣٤٢)",
            sourceEn = "Sahih Muslim 1342",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء السفر النبوي الكامل؛ طلب للبر والتقوى وتيسير السفر وحفظ الأهل والمال.",
            meaningEn = "Complete Prophetic travel supplication asking for righteousness, ease in journey, and Allah's companionship and protection over family.",
            transliteration = "Allahu Akbar (3x). Subhanal-ladhi sakhkhara lana hadha wa ma kunna lahu muqrinin..."
        ),
        DhikrItem(
            id = "tr_2",
            category = AdhkarCategory.TRAVEL,
            textAr = "آيِبُونَ، تَائِبُونَ، عَابِدُونَ، لِرَبِّنَا حَامِدُونَ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (١٣٤٢)",
            sourceEn = "Sahih Muslim 1342",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يُزاد على دعاء السفر عند الرجوع والعودة إلى الوطن والأهل.",
            meaningEn = "We return, repenting, worshipping, and praising our Lord.",
            transliteration = "Ayibuna, ta'ibuna, 'abiduna, li-Rabbina hamidun."
        ),
        DhikrItem(
            id = "tr_3",
            category = AdhkarCategory.TRAVEL,
            textAr = "أَسْتَوْدِعُ اللَّهَ دِينَكَ وَأَمَانَتَكَ وَخَوَاتِيمَ عَمَلِكَ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٢٦٠٠) والترمذي (٣٤٤٣)",
            sourceEn = "Sunan Abi Dawud 2600 & At-Tirmidhi 3443",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء المقيم للمسافر عند توديعه.",
            meaningEn = "I entrust to Allah your religion, your trust, and the final outcomes of your deeds.",
            transliteration = "Astawdi'ullaha dinaka wa amanataka wa khawatima 'amalik."
        ),

        // === 11. أذكار الركوب والمواصلات (RIDING) ===
        DhikrItem(
            id = "rd_1",
            category = AdhkarCategory.RIDING,
            textAr = "بِسْمِ اللَّهِ، الْحَمْدُ لِلَّهِ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ، الْحَمْدُ لِلَّهِ (ثَلَاثًا)، اللَّهُ أَكْبَرُ (ثَلَاثًا)، سُبْحَانَكَ اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٢٦٠٢) والترمذي (٣٤٤٦)",
            sourceEn = "Sunan Abi Dawud 2602 & At-Tirmidhi 3446",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء ركوب الدابة أو السيارة أو أي وسيلة نقل؛ يجمع التسمية والحمد والتسبيح والاستغفار.",
            meaningEn = "Supplication upon boarding a vehicle or mount, praising Allah for subjecting it to our service and seeking forgiveness.",
            transliteration = "Bismillah, Alhamdulillah, Subhanal-ladhi sakhkhara lana hadha..."
        ),
        DhikrItem(
            id = "rd_2",
            category = AdhkarCategory.RIDING,
            textAr = "بِسْمِ اللَّهِ مَجْرَاهَا وَمُرْسَاهَا ۚ إِنَّ رَبِّي لَغَفُورٌ رَّحِيمٌ.",
            targetCount = 1,
            sourceAr = "سورة هود: ٤١",
            sourceEn = "Surah Hud 11:41",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء ركوب السفينة ووسائل السفر استعانة بالله في المسير والوصول.",
            meaningEn = "In the name of Allah is its course and its anchorage. Indeed, my Lord is Forgiving and Merciful.",
            transliteration = "Bismillahi majraha wa mursaha, inna Rabbi la-Ghafurur-Rahim."
        ),
        DhikrItem(
            id = "rd_3",
            category = AdhkarCategory.RIDING,
            textAr = "التكبير عند صعود المرتفعات: «اللَّهُ أَكْبَرُ»، والتسبيح عند الهبوط: «سُبْحَانَ اللَّهِ».",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٢٩٩٣)",
            sourceEn = "Sahih al-Bukhari 2993",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "سنة نبوية أثناء التنقل والطرقات؛ التكبير في الصعود والتسبيح في الهبوط.",
            meaningEn = "Prophetic Sunnah during travel: saying Allahu Akbar when ascending and SubhanAllah when descending.",
            transliteration = "Allahu Akbar (ascending), SubhanAllah (descending)."
        ),

        // === 12. أدعية الكرب والهم والحزن (ANXIETY) ===
        DhikrItem(
            id = "ax_1",
            category = AdhkarCategory.ANXIETY,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٤٦) ومسلم (٢٧٣٠)",
            sourceEn = "Sahih al-Bukhari 6346 & Muslim 2730",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "دعاء الكرب العظيم الذي كان يدعو به النبي ﷺ عند الشدائد.",
            meaningEn = "Supplication in times of deep distress, affirming the greatness and majesty of Allah.",
            transliteration = "La ilaha illAllahul-'Azimul-Halim, la ilaha illAllahu Rabbul-'Arshil-'Azim..."
        ),
        DhikrItem(
            id = "ax_2",
            category = AdhkarCategory.ANXIETY,
            textAr = "اللَّهُمَّ إِنِّي عَبْدُكَ، وَابْنُ عَبْدِكَ، وَابْنُ أَمَتِكَ، نَاصِيَتِي بِيَدِكَ، مَاضٍ فِيَّ حُكْمُكَ، عَدْلٌ فِيَّ قَضَاؤُكَ، أَسْأَلُكَ بِكُلِّ اسْمٍ هُوَ لَكَ، سَمَّيْتَ بِهِ نَفْسَكَ، أَوْ أَنْزَلْتَهُ فِي كِتَابِكَ، أَوْ عَلَّمْتَهُ أَحَدًا مِنْ خَلْقِكَ، أَوِ اسْتَأْثَرْتَ بِهِ فِي عِلْمِ الْغَيْبِ عِنْدَكَ، أَنْ تَجْعَلَ الْقُرْآنَ رَبِيعَ قَلْبِي، وَنُورَ صَدْرِي، وَجَلَاءَ حُزْنِي، وَذَهَابَ هَمِّي.",
            targetCount = 1,
            sourceAr = "مسند أحمد (٣٧١٢)",
            sourceEn = "Musnad Ahmad 3712",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "ما أصاب أحداً قط همٌّ ولا حزن فقاله إلا أذهب الله همه وحزنه وأبدله مكانه فرجاً.",
            meaningEn = "The great supplication for removing sorrow and grief, asking Allah to make the Quran the spring of the heart and light of the chest.",
            transliteration = "Allahumma inni 'abduka, wabnu 'abdika, wabnu amatika, nasiyati bi-yadik..."
        ),
        DhikrItem(
            id = "ax_3",
            category = AdhkarCategory.ANXIETY,
            textAr = "اللَّهُمَّ رَحْمَتَكَ أَرْجُو فَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ، وَأَصْلِحْ لِي شَأْنِي كُلَّهُ، لَا إِلَٰهَ إِلَّا أَنْتَ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٩٠) وأحمد",
            sourceEn = "Sunan Abi Dawud 5090 & Ahmad",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "دعاء المكروب؛ رجاءٌ لرحمة الله وتفويضٌ كامل للأمر إليه.",
            meaningEn = "O Allah, it is Your mercy that I hope for, so do not leave me in charge of my affairs even for a blink of an eye.",
            transliteration = "Allahumma rahmataka arju fala takilni ila nafsi tarfata 'ayn, wa aslih li sha'ni kullahu, la ilaha illa Anta."
        ),
        DhikrItem(
            id = "ax_4",
            category = AdhkarCategory.ANXIETY,
            textAr = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٦٩)",
            sourceEn = "Sahih al-Bukhari 6369",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "استعاذة نبوية جامعة من الهم والحزن والعجز وتراكم الديون.",
            meaningEn = "O Allah, I seek refuge in You from anxiety and sorrow, weakness and laziness, miserliness and cowardice, the burden of debts and being overpowered by men.",
            transliteration = "Allahumma inni a'udhu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal..."
        ),
        DhikrItem(
            id = "ax_5",
            category = AdhkarCategory.ANXIETY,
            textAr = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ.",
            targetCount = 3,
            sourceAr = "سورة الأنبياء: ٨٧ · سنن الترمذي (٣٥٠٥)",
            sourceEn = "Surah Al-Anbiya 21:87 · At-Tirmidhi 3505",
            gradingAr = "قرآن كريم · صحيح",
            gradingEn = "Holy Quran · Sahih",
            meaningAr = "دعاء ذي النون؛ لم يدعُ بها رجل مسلم في شيء قط إلا استجاب الله له.",
            meaningEn = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            transliteration = "La ilaha illa Anta subhanaka inni kuntu minaz-zalimin."
        )
    )

    val allAdhkar: List<DhikrItem> = adhkarPart1 + AdhkarCatalogExtended.adhkarPart2

    fun byCategory(category: AdhkarCategory): List<DhikrItem> =
        allAdhkar.filter { it.category == category }

    fun dailyDhikr(dayOfYear: Int): DhikrItem =
        allAdhkar[dayOfYear.coerceAtLeast(0) % allAdhkar.size]

    fun dailyAyah(dayOfYear: Int): DailyAyahItem =
        dailyAyahs[dayOfYear.coerceAtLeast(0) % dailyAyahs.size]

    fun dailyHadith(dayOfYear: Int): AuthenticHadith =
        dailyHadiths[dayOfYear.coerceAtLeast(0) % dailyHadiths.size]
}
