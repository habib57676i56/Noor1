package com.example.domain.quran

data class SoulRemedyPrescription(
    val id: String,
    val moodAr: String,
    val moodEn: String,
    val moodUz: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val globalAyahNumber: Int,
    val surahNameAr: String,
    val ayahTextAr: String,
    val ayahReflectionAr: String,
    val ayahReflectionEn: String,
    val ayahReflectionUz: String,
    val propheticDuaAr: String,
    val propheticDuaEn: String,
    val duaSourceAr: String,
    val dhikrTextAr: String,
    val dhikrTargetCount: Int
)

/**
 * Quranic & Prophetic Emotional Soul Remedy Studio (دواء القلوب من القرآن والسنة).
 * Authentic Quranic verses (with verified global Ayah numbers for live Alafasy audio) paired with Sahih Prophetic Duas.
 */
object QuranicSoulRemedyEngine {

    val prescriptions: List<SoulRemedyPrescription> = listOf(
        SoulRemedyPrescription(
            id = "anxiety", moodAr = "الهمّ والضيق", moodEn = "Anxiety & Distress", moodUz = "Tashvish va siqilish",
            surahNumber = 94, ayahNumber = 5, globalAyahNumber = 6095, surahNameAr = "الشرح",
            ayahTextAr = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            ayahReflectionAr = "اليسر لا يأتي بعد العسر فحسب، بل يولَد معه وفي قلبه؛ ولن يغلب عسرٌ يسرين!",
            ayahReflectionEn = "Ease is born right alongside hardship — never can one hardship overcome two eases.",
            ayahReflectionUz = "Qiyinchilik bilan birga albatta yengillik bor; bir mashaqqat ikki yengillikni yenga olmaydi.",
            propheticDuaAr = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ.",
            propheticDuaEn = "O Allah, I seek refuge in You from worry and grief, helplessness and laziness, cowardice and miserliness, and being overwhelmed by debt or people.",
            duaSourceAr = "صحيح البخاري",
            dhikrTextAr = "لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            dhikrTargetCount = 33
        ),
        SoulRemedyPrescription(
            id = "sadness", moodAr = "الحزن والأسى", moodEn = "Deep Sadness", moodUz = "G'amginlik va mahzunlik",
            surahNumber = 12, ayahNumber = 86, globalAyahNumber = 1682, surahNameAr = "يوسف",
            ayahTextAr = "قَالَ إِنَّمَا أَشْكُو بَثِّي وَحُزْنِي إِلَى اللَّهِ وَأَعْلَمُ مِنَ اللَّهِ مَا لَا تَعْلَمُونَ",
            ayahReflectionAr = "ارفع شكوى قلبك للسميع القريب، فكلما اشتدّ الحزن اقترب الفرج كما جاء يوسف بعد طول غياب.",
            ayahReflectionEn = "Pour out your deepest grief to Allah alone; relief draws closest when the heart turns sincerely to Him.",
            ayahReflectionUz = "Dard va g'amingizni yolg'iz Allohga ayting; U siz bilmagan go'zal yechimlarni tayyorlab qo'ygan.",
            propheticDuaAr = "اللَّهُمَّ إِنِّي عَبْدُكَ ابْنُ عَبْدِكَ ابْنُ أَمَتِكَ... أَسْأَلُكَ أَنْ تَجْعَلَ الْقُرْآنَ رَبِيعَ قَلْبِي وَنُورَ صَدْرِي وَجِلَاءَ حُزْنِي وَذَهَابَ هَمِّي.",
            propheticDuaEn = "O Allah, make the Quran the spring of my heart, the light of my chest, the remover of my sorrow, and the reliever of my distress.",
            duaSourceAr = "مسند أحمد (صحيح)",
            dhikrTextAr = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            dhikrTargetCount = 33
        ),
        SoulRemedyPrescription(
            id = "fear", moodAr = "الخوف والقلق", moodEn = "Fear & Restlessness", moodUz = "Qo'rquv va bezovtalik",
            surahNumber = 13, ayahNumber = 28, globalAyahNumber = 1735, surahNameAr = "الرعد",
            ayahTextAr = "الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُم بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            ayahReflectionAr = "الطمأنينة ليست في الأسباب الزائلة، بل في سكون القلب إلى ذكر الله الحفيظ الذي لا يُعجزه شيء.",
            ayahReflectionEn = "True stillness is found when the heart anchors itself in the remembrance of the Ever-Watchful Protector.",
            ayahReflectionUz = "Qalblar faqat Allohning zikri bilan haqiqiy xotirjamlik va osoyishtalik topadi.",
            propheticDuaAr = "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ.",
            propheticDuaEn = "There is no deity worthy of worship except Allah, the Tremendous, the Forbearing, Lord of the Mighty Throne.",
            duaSourceAr = "صحيح البخاري ومسلم",
            dhikrTextAr = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ — سُبْحَانَ اللَّهِ",
            dhikrTargetCount = 33
        ),
        SoulRemedyPrescription(
            id = "repentance", moodAr = "التوبة والندم", moodEn = "Repentance & Hope", moodUz = "Tavba va umid",
            surahNumber = 39, ayahNumber = 53, globalAyahNumber = 4111, surahNameAr = "الزمر",
            ayahTextAr = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
            ayahReflectionAr = "أرجى آية في كتاب الله: ناداك الله بـ «يا عبادي» رغم التقصير ليفتح لك باب رحمته بلا حدود!",
            ayahReflectionEn = "The most hopeful verse in the Quran: Allah warmly calls you 'My servants' and promises to forgive ALL sins upon sincere return.",
            ayahReflectionUz = "Qur'ondagi eng umidbaxsh oyat: Allohning rahmatidan noumid bo'lmang, U barcha gunohlarni kechiradi.",
            propheticDuaAr = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ... فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            propheticDuaEn = "Sayyid al-Istighfar: O Allah, You are my Lord, none has the right to be worshipped except You... forgive me, for none forgives sins except You.",
            duaSourceAr = "صحيح البخاري",
            dhikrTextAr = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
            dhikrTargetCount = 100
        ),
        SoulRemedyPrescription(
            id = "anger", moodAr = "الغضب والانفعال", moodEn = "Anger & Frustration", moodUz = "G'azab va asabiylik",
            surahNumber = 3, ayahNumber = 134, globalAyahNumber = 427, surahNameAr = "آل عمران",
            ayahTextAr = "وَالْكَاظِمِينَ الْغَيْظَ وَالْعَافِينَ عَنِ النَّاسِ ۗ وَاللَّهُ يُحِبُّ الْمُحْسِنِينَ",
            ayahReflectionAr = "كظم الغيظ لحظةٌ قصيرة، لكن ثمرتها محبة الله لك ورفعة قدرك وراحة صدرك.",
            ayahReflectionEn = "Restraining anger lasts a brief moment, yet it earns the eternal love of Allah and peace of heart.",
            ayahReflectionUz = "G'azabni yutish bir lahzalik sabr, ammo mukofoti Allohning muhabbatidir.",
            propheticDuaAr = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ — اللَّهُمَّ اغْفِرْ لِي ذَنْبِي، وَأَذْهِبْ غَيْظَ قَلْبِي، وَأَجِرْنِي مِنَ الشَّيْطَانِ.",
            propheticDuaEn = "I seek refuge in Allah from the accursed Shaytan. O Allah, forgive my sin and remove the anger from my heart.",
            duaSourceAr = "صحيح البخاري ومسلم",
            dhikrTextAr = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ",
            dhikrTargetCount = 11
        ),
        SoulRemedyPrescription(
            id = "provision", moodAr = "طلب الرزق والبركة", moodEn = "Provision & Barakah", moodUz = "Rizq va baraka so'rash",
            surahNumber = 65, ayahNumber = 3, globalAyahNumber = 5220, surahNameAr = "الطلاق",
            ayahTextAr = "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ۚ وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
            ayahReflectionAr = "التقوى مع التوكل الصادق تفتح أبواب رزق لم تكن في حسبانك ولا تخطيطك.",
            ayahReflectionEn = "Taqwa paired with sincere reliance opens doors of sustenance from avenues you never imagined.",
            ayahReflectionUz = "Taqvo va chin tavakkul siz o'ylamagan tomondan rizq eshiklarini ochadi.",
            propheticDuaAr = "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ.",
            propheticDuaEn = "O Allah, suffice me with Your lawful provision over the unlawful, and enrich me by Your bounty from needing anyone besides You.",
            duaSourceAr = "سنن الترمذي (حسن)",
            dhikrTextAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، أَسْتَغْفِرُ اللَّهَ",
            dhikrTargetCount = 33
        ),
        SoulRemedyPrescription(
            id = "healing", moodAr = "المرض والشفاء", moodEn = "Illness & Healing", moodUz = "Bemorlik va shifo",
            surahNumber = 26, ayahNumber = 80, globalAyahNumber = 3012, surahNameAr = "الشعراء",
            ayahTextAr = "وَإِذَا مَرِضْتُ فَهُوَ يَشْفِينِ",
            ayahReflectionAr = "أدب نبي الله إبراهيم عليه السلام: نسب المرض لنفسه والشفاء كله لله الشافي المعافي وحده.",
            ayahReflectionEn = "The prophetic etiquette of Ibrahim (AS): complete certainty that healing belongs solely to Al-Shafi.",
            ayahReflectionUz = "Ibrohim (a.s.) odoblari: shifo faqat yolg'iz Alloh — Shofiy Zotdandir.",
            propheticDuaAr = "أَذْهِبِ الْبَاسَ رَبَّ النَّاسِ، وَاشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا.",
            propheticDuaEn = "Remove the harm, O Lord of mankind, and heal; You are the Healer. There is no cure except Your cure, a cure that leaves no illness behind.",
            duaSourceAr = "صحيح البخاري ومسلم",
            dhikrTextAr = "بِسْمِ اللَّهِ (٣) أَعُوذُ بِاللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ (٧)",
            dhikrTargetCount = 7
        ),
        SoulRemedyPrescription(
            id = "gratitude", moodAr = "الفرح والشكر", moodEn = "Joy & Gratitude", moodUz = "Quvonch va shukrona",
            surahNumber = 14, ayahNumber = 7, globalAyahNumber = 1757, surahNameAr = "إبراهيم",
            ayahTextAr = "وَإِذْ تَأَذَّنَ رَبُّكُمْ لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ",
            ayahReflectionAr = "الشكر قيد النعم الموجودة وصيد النعم المفقودة؛ ووعد الله بالزيادة وعدٌ لا يتخلف.",
            ayahReflectionEn = "Gratitude secures existing blessings and invites boundless increase guaranteed by Allah.",
            ayahReflectionUz = "Shukr mavjud ne'matlarni saqlaydi va Allohning va'dasiga ko'ra ziyoda qiladi.",
            propheticDuaAr = "الْحَمْدُ لِلَّهِ الَّذِي بِنِعْمَتِهِ تَتِمُّ الصَّالِحَاتُ — اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ.",
            propheticDuaEn = "All praise is for Allah by whose favor good deeds are completed. O Allah, help me remember You, thank You, and worship You excellently.",
            duaSourceAr = "سنن أبي داود وابن ماجه (صحيح)",
            dhikrTextAr = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ",
            dhikrTargetCount = 33
        )
    )
}
