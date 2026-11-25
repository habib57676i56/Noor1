package com.example.domain.quran

data class QuranCosmicSign(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val domainAr: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val globalAyahNumber: Int,
    val surahNameAr: String,
    val ayahTextAr: String,
    val reflectionAr: String,
    val reflectionEn: String,
    val reflectionUz: String
)

data class QuranParableItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val ayahTextAr: String,
    val imageryBreakdownAr: String,
    val imageryBreakdownEn: String
)

data class QuranProphetEntry(
    val order: Int,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val mentionCountInQuran: Int,
    val primarySurahNumber: Int,
    val primarySurahAr: String,
    val hallmarkAr: String,
    val hallmarkEn: String
)

/**
 * Encyclopedia of Quranic Cosmic & Natural Signs, Quranic Parables (الأمثال في القرآن),
 * and Complete Verified Index of All 25 Prophets Mentioned by Name in the Holy Quran.
 */
object QuranMiraclesAndParablesEngine {

    val cosmicSigns: List<QuranCosmicSign> = listOf(
        QuranCosmicSign(
            "expanding_universe", "توسّع الكون المستمر", "The Expanding Universe", "Koinotning kengayishi",
            "علم الفلك", 51, 47, 4722, "الذاريات",
            "وَالسَّمَاءَ بَنَيْنَاهَا بِأَيْدٍ وَإِنَّا لَمُوسِعُونَ",
            "أشار القرآن قبل ١٤ قرناً بصيغة اسم الفاعل الدال على الاستمرار (لَمُوسِعُونَ) إلى توسّع بناء السماء والكون.",
            "Using the active participle 'la-musi'un' (continually expanding), the Quran points to the vast, ongoing expansion of the cosmos.",
            "Qur'oni Karim 14 asr avval osmon va koinotning doimiy kengayishiga ishora qilgan."
        ),
        QuranCosmicSign(
            "two_seas_barrier", "برزخ البحار العذب والمالح", "Barrier Between Two Seas", "Ikki dengiz orasidagi to'siq",
            "علوم البحار", 55, 19, 4920, "الرحمن",
            "مَرَجَ الْبَحْرَيْنِ يَلْتَقِيَانِ ۝ بَيْنَهُمَا بَرْزَخٌ لَّا يَبْغِيَانِ",
            "يلتقي البحران المختلفان في الملوحة والكثافة ويبقى بينهما حاجز مائي دقيق يحفظ خصائص كل بحر.",
            "Where two distinct bodies of water meet, differences in salinity and density create a subtle boundary preserving each water's properties.",
            "Ikki dengiz uchrashganda ham ularning o'rtasida sho'rlik va zichlik to'sig'i saqlanib qoladi."
        ),
        QuranCosmicSign(
            "deep_sea_waves", "أمواج الأعماق والظلمات", "Deep Ocean Internal Waves", "Chuqur dengiz ichki to'lqinlari",
            "علوم البحار", 24, 40, 2831, "النور",
            "أَوْ كَظُلُمَاتٍ فِي بَحْرٍ لُّجِّيٍّ يَغْشَاهُ مَوْجٌ مِّن فَوْقِهِ مَوْجٌ مِّن فَوْقِهِ سَحَابٌ",
            "صوّرت الآية ظلمات أعماق المحيطات ووجود أمواج داخلية عميقة يعلوها موج سطحي، وهي حقيقة لم تُكتشف إلا بالغواصات الحديثة.",
            "Describes pitch-black abyssal ocean depths covered by internal waves topped by surface waves — only observable with modern oceanography.",
            "Chuqur okean zulmatlari va ustma-ust ichki to'lqinlar haqidagi hayratlanarli oyat."
        ),
        QuranCosmicSign(
            "embryology", "أطوار خلق الجنين بدقة", "Stages of Human Embryology", "Inson yaralish bosqichlari",
            "الطب والأجنة", 23, 14, 2687, "المؤمنون",
            "ثُمَّ خَلَقْنَا النُّطْفَةَ عَلَقَةً فَخَلَقْنَا الْعَلَقَةَ مُضْغَةً فَخَلَقْنَا الْمُضْغَةَ عِظَامًا فَكَسَوْنَا الْعِظَامَ لَحْمًا",
            "تسلسل مذهل ودقيق لمراحل تخلّق الجنين من النطفة إلى العلقة ثم المضغة ثم العظام المكسوة باللحم.",
            "Remarkable sequential precision tracing prenatal formation: nutfah, alaqah, mudghah, skeletal framework, and muscular clothing.",
            "Inson embrionining nutfa, alaqa, muzg'a va suyak-go'sht bosqichlarining aniq bayoni."
        ),
        QuranCosmicSign(
            "mountains_pegs", "الجبال أوتادٌ للأرض", "Mountains as Deep Pegs", "Tog'lar — yerning qoziqlari",
            "الجيولوجيا", 78, 6, 5678, "النبأ",
            "أَلَمْ نَجْعَلِ الْأَرْضَ مِهَادًا ۝ وَالْجِبَالَ أَوْتَادًا",
            "للجبال جذور عميقة مغروسة في القشرة الأرضية تشبه الوتد الذي يثبت الخيمة ويحقق توازن الصفائح الأرضية.",
            "Like tent pegs, mountains possess deep subterranean roots stabilizing Earth's crustal plates.",
            "Tog'lar xuddi chodir qozig'i kabi yer qa'rida chuqur ildizlarga ega bo'lib, muvozanatni saqlaydi."
        ),
        QuranCosmicSign(
            "iron_sent_down", "إنزال الحديد من السماء", "Iron Sent Down From Above", "Temirning osmondan tushirilishi",
            "الفيزياء الفلكية", 57, 25, 5100, "الحديد",
            "وَأَنزَلْنَا الْحَدِيدَ فِيهِ بَأْسٌ شَدِيدٌ وَمَنَافِعُ لِلنَّاسِ",
            "تؤكد الفيزياء الفلكية أن عنصر الحديد لا يتكون في الأرض بل نشأ في نجوم عملاقة وأُنزل إلى الأرض عبر النيازك.",
            "Astrophysics confirms iron formed in massive supernovae before arriving on Earth — echoing the word 'anzalna' (We sent down).",
            "Astrofizika temir elementi ulkan yulduzlarda hosil bo'lib Yerga tushganini tasdiqlaydi."
        ),
        QuranCosmicSign(
            "fingerprints", "بنان الأصابع الفريد لكل إنسان", "Unique Human Fingerprints", "Barmoq izlarining takrorlanmasligi",
            "التشريح البشري", 75, 4, 5555, "القيامة",
            "بَلَىٰ قَادِرِينَ عَلَىٰ أَن نُّسَوِّيَ بَنَانَهُ",
            "خصّ الله البنان بالذكر لإبراز تمام القدرة على إعادة أدق تفصيل يميز كل إنسان عن مليارات البشر دون تشابه!",
            "Allah highlights reconstructing the very fingertips — the unique biometric signature distinct to every single human being.",
            "Alloh har bir insonning takrorlanmas barmoq izlarigacha qayta tiklashga Qodir ekanini eslatadi."
        )
    )

    val parables: List<QuranParableItem> = listOf(
        QuranParableItem(
            "light_verse", "مَثَل نور الله في قلب المؤمن (آية النور)", "Parable of Divine Light (Ayat al-Nur)", "Nur oyati masali",
            24, 35, "النور",
            "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ۚ مَثَلُ نُورِهِ كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ ۖ الْمِصْبَاحُ فِي زُجَاجَةٍ ۖ الزُّجَاجَةُ كَأَنَّهَا كَوْكَبٌ دُرِّيٌّ",
            "المشكاة = الصدر، والزجاجة الصافية = القلب السليم، والمصباح = نور الإيمان، والزيت المبارك = الوحي والفطرة الصافية؛ يجتمعان معاً ليكونا «نُورٌ عَلَىٰ نُورٍ».",
            "The niche is the chest, the crystal glass is the pure heart, the lamp is Iman, and the blessed olive oil is Revelation — creating 'Light upon Light'."
        ),
        QuranParableItem(
            "good_word", "مَثَل الكلمة الطيبة كالشجرة المثمرة", "The Good Word Like a Flourishing Tree", "Yaxshi so'z — mevali daraxt masali",
            14, 24, "إبراهيم",
            "أَلَمْ تَرَ كَيْفَ ضَرَبَ اللَّهُ مَثَلًا كَلِمَةً طَيِّبَةً كَشَجَرَةٍ طَيِّبَةٍ أَصْلُهَا ثَابِتٌ وَفَرْعُهَا فِي السَّمَاءِ",
            "كلمة التوحيد والكلام الطيب جذورها راسخة في القلب كجذور الشجرة، وثمارها الصالحة تصعد إلى السماء وتنفع الناس في كل حين.",
            "Faith and kind words have deep immovable roots in the heart and lift righteous deeds skyward bearing fruit in every season."
        ),
        QuranParableItem(
            "charity_grain", "مَثَل الإنفاق كالسنابل السبعمائة", "Charity Like a Grain Yielding 700-Fold", "Ehsonning 700 barobar ko'payishi",
            2, 261, "البقرة",
            "مَّثَلُ الَّذِينَ يُنفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنبَتَتْ سَبْعَ سَنَابِلَ فِي كُلِّ سُنبُلَةٍ مِّائَةُ حَبَّةٍ",
            "تصوير حي لنماء الصدقة الخالصة: بذرة واحدة تنبت ٧٠٠ ضعف ويضاعف الله لمن يشاء فوق ذلك بلا حد.",
            "Vivid image of pure charity: one seed blossoms into seven ears of 100 grains each — 700x and multiplied further by Allah."
        ),
        QuranParableItem(
            "mountain_quran", "مَثَل خشوع الجبل للقرآن الكريم", "If the Quran Descended Upon a Mountain", "Qur'on tog'ga tushirilganda",
            59, 21, "الحشر",
            "لَوْ أَنزَلْنَا هَٰذَا الْقُرْآنَ عَلَىٰ جَبَلٍ لَّرَأَيْتَهُ خَاشِعًا مُّتَصَدِّعًا مِّنْ خَشْيَةِ اللَّهِ",
            "دعوة بليغة للقلب البشري أن يلين ويخشع لآيات الله، فالجبل الصمّ الأصم يتصدع لهيبتها فكيف بقلب المؤمن!",
            "A stirring wake-up call for human hearts: if a solid mountain would crumble in awe of the Quran, how deeply should our hearts soften!"
        ),
        QuranParableItem(
            "spider_house", "مَثَل بيت العنكبوت", "Parable of the Spider's Fragile House", "O'rgimchak inining zaifligi",
            29, 41, "العنكبوت",
            "مَثَلُ الَّذِينَ اتَّخَذُوا مِن دُونِ اللَّهِ أَوْلِيَاءَ كَمَثَلِ الْعَنكَبُوتِ اتَّخَذَتْ بَيْتًا ۖ وَإِنَّ أَوْهَنَ الْبُيُوتِ لَبَيْتُ الْعَنكَبُوتِ",
            "كل من يعتمد على غير الله يستند إلى أوهن سند في الوجود، خيوط هشة لا تقي حراً ولا برداً ولا خوفاً.",
            "Relying on anything besides Allah is like sheltering in a spider's web — fragile externally and internally."
        )
    )

    /**
     * All 25 Prophets explicitly named in the Holy Quran with verified mention counts and primary Surahs.
     */
    val all25Prophets: List<QuranProphetEntry> = listOf(
        QuranProphetEntry(1, "آدم عليه السلام", "Adam (AS)", "Odam (a.s.)", 25, 2, "البقرة", "أبو البشر وأول الأنبياء وعُلّم الأسماء كلها", "Father of humanity, taught all names"),
        QuranProphetEntry(2, "إدريس عليه السلام", "Idris (AS)", "Idris (a.s.)", 2, 19, "مريم", "كان صدّيقاً نبياً ورفعه الله مكاناً علياً", "Truthful prophet raised to an exalted station"),
        QuranProphetEntry(3, "نوح عليه السلام", "Nuh (AS)", "Nuh (a.s.)", 43, 71, "نوح", "أول الرسل ودعا قومه ٩٥٠ عاماً وصاحب السفينة", "Called his people 950 years; builder of the Ark"),
        QuranProphetEntry(4, "هود عليه السلام", "Hud (AS)", "Hud (a.s.)", 7, 11, "هود", "أُرسل إلى قوم عاد أصحاب إرم ذات العماد", "Sent to the mighty people of 'Ad"),
        QuranProphetEntry(5, "صالح عليه السلام", "Salih (AS)", "Solih (a.s.)", 9, 11, "هود", "أُرسل إلى ثمود ومعجزته الناقة", "Sent to Thamud with the miracle of the She-Camel"),
        QuranProphetEntry(6, "إبراهيم عليه السلام", "Ibrahim (AS)", "Ibrohim (a.s.)", 69, 14, "إبراهيم", "خليل الرحمن وأبو الأنبياء وباني الكعبة المشرفة", "Khalilullah; rebuilt the Holy Kaaba with Ismail"),
        QuranProphetEntry(7, "لوط عليه السلام", "Lut (AS)", "Lut (a.s.)", 27, 11, "هود", "دعا قومه إلى الطهارة والفطرة السليمة", "Stood firm for moral purity and faith"),
        QuranProphetEntry(8, "إسماعيل عليه السلام", "Ismail (AS)", "Ismoil (a.s.)", 12, 2, "البقرة", "الذبيح الصادق الوعد وباني الكعبة مع أبيه", "True to his promise; co-builder of the Kaaba"),
        QuranProphetEntry(9, "إسحاق عليه السلام", "Ishaq (AS)", "Ishoq (a.s.)", 17, 37, "الصافات", "بشارة الله لإبراهيم وسارة ومن نسله أنبياء بني إسرائيل", "Glad tidings to Ibrahim & Sarah; blessed lineage"),
        QuranProphetEntry(10, "يعقوب عليه السلام", "Ya'qub (AS)", "Yoqub (a.s.)", 16, 12, "يوسف", "نبي الصبر الجميل وحسن الظن بالله", "Model of beautiful patience (Sabr Jamil)"),
        QuranProphetEntry(11, "يوسف عليه السلام", "Yusuf (AS)", "Yusuf (a.s.)", 27, 12, "يوسف", "أحسن القصص؛ نبي العفة وتأويل الأحاديث والعفو", "Best of Stories: chastity, dream interpretation & forgiveness"),
        QuranProphetEntry(12, "شعيب عليه السلام", "Shu'ayb (AS)", "Shuayb (a.s.)", 11, 11, "هود", "خطيب الأنبياء؛ دعا أهل مدين للعدل في الميزان", "Orator of the Prophets; championed fair trade"),
        QuranProphetEntry(13, "أيوب عليه السلام", "Ayyub (AS)", "Ayyub (a.s.)", 4, 21, "الأنبياء", "قدوة الصبر والشكر في البلاء والشفاء", "Pinnacle of gratitude and patience through trials"),
        QuranProphetEntry(14, "ذو الكفل عليه السلام", "Dhul-Kifl (AS)", "Zulkifl (a.s.)", 2, 21, "الأنبياء", "من الصابرين الأخيار الذين أدخلهم الله في رحمته", "Honored among the steadfast and righteous"),
        QuranProphetEntry(15, "موسى عليه السلام", "Musa (AS)", "Muso (a.s.)", 136, 20, "طه", "كليم الله وأكثر الأنبياء ذكراً في القرآن الكريم", "Spoke directly to Allah; most mentioned prophet in Quran"),
        QuranProphetEntry(16, "هارون عليه السلام", "Harun (AS)", "Horun (a.s.)", 20, 20, "طه", "شقيق موسى ووزيره الفصيح في الدعوة", "Musa's eloquent brother and loyal supporter"),
        QuranProphetEntry(17, "داود عليه السلام", "Dawud (AS)", "Dovud (a.s.)", 16, 38, "ص", "أوتي الزبور ولان له الحديد وسبّحت معه الجبال", "Given the Zabur; mountains echoed his Tasbih"),
        QuranProphetEntry(18, "سليمان عليه السلام", "Sulayman (AS)", "Sulaymon (a.s.)", 17, 27, "النمل", "عُلّم منطق الطير وسُخّرت له الريح والجن مع شكر الله", "Taught speech of birds; humble grateful king-prophet"),
        QuranProphetEntry(19, "إلياس عليه السلام", "Ilyas (AS)", "Ilyos (a.s.)", 3, 37, "الصافات", "دعا قومه لترك عبادة بعل وتوحيد الله", "Called his people back to pure monotheism"),
        QuranProphetEntry(20, "اليسع عليه السلام", "Al-Yasa' (AS)", "Alyasa' (a.s.)", 2, 38, "ص", "من الأخيار الذين فضّلهم الله على العالمين", "Exalted among the righteous messengers"),
        QuranProphetEntry(21, "يونس عليه السلام", "Yunus (AS)", "Yunus (a.s.)", 4, 10, "يونس", "ذو النون صاحب التسبيحة العظمى في الظلمات", "Companion of the Whale & timeless Dua of relief"),
        QuranProphetEntry(22, "زكريا عليه السلام", "Zakariyya (AS)", "Zakariyo (a.s.)", 7, 19, "مريم", "كفيل مريم وصاحب الدعاء المستجاب في الكبر", "Guardian of Maryam; answered prayer in old age"),
        QuranProphetEntry(23, "يحيى عليه السلام", "Yahya (AS)", "Yahyo (a.s.)", 5, 19, "مريم", "أوتي الحكم صبياً وكان سيداً وحصوراً وباراً بوالديه", "Granted wisdom in childhood; pure and dutiful"),
        QuranProphetEntry(24, "عيسى عليه السلام", "Isa (AS)", "Iso (a.s.)", 25, 19, "مريم", "كلمة الله وروح منه أوتي الإنجيل والمعجزات بإذن الله", "Miraculous birth; healed the sick by Allah's leave"),
        QuranProphetEntry(25, "محمد ﷺ", "Muhammad ﷺ", "Muhammad ﷺ", 4, 47, "محمد", "خاتم الأنبياء والمرسلين ورحمة الله للعالمين", "Final Seal of Prophets & Mercy to all worlds ﷺ")
    )
}
