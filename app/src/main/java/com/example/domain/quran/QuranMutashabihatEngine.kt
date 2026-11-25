package com.example.domain.quran

data class MutashabihAyahItem(
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val ayahNumber: Int,
    val ayahTextAr: String,
    val uniqueWordsHighlight: List<String>
)

data class MutashabihGroup(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val categoryAr: String,
    val mnemonicRuleAr: String,
    val mnemonicRuleEn: String,
    val mnemonicRuleUz: String,
    val sourceBookAr: String,
    val ayahs: List<MutashabihAyahItem>
)

data class MutashabihDiffWord(
    val word: String,
    val isDistinctive: Boolean
)

/**
 * Scholarly Quranic Mutashabihat (Similar Verses) Master Engine.
 * Derived from classical works (Durrat al-Tanzil by Al-Khatib al-Iskafi & Asrar al-Tikrar by Al-Kirmani),
 * featuring automatic word-level diff highlighting to eliminate Hifz mix-ups across Surahs.
 */
object QuranMutashabihatEngine {

    val curatedGroups: List<MutashabihGroup> = listOf(
        MutashabihGroup(
            id = "rizq_imlaq",
            titleAr = "مِنْ إِمْلَاقٍ أم خَشْيَةَ إِمْلَاقٍ؟",
            titleEn = "'Min Imlaq' vs 'Khashyata Imlaq'",
            titleUz = "'Min imloq' va 'Xashyata imloq' farqi",
            categoryAr = "الأنعام والإسراء",
            mnemonicRuleAr = "الضابط الذهبي: في الأنعام الخطاب للفقراء فعلاً (مِنْ إِمْلَاقٍ) فَقُدِّم رزق الآباء (نَحْنُ نَرْزُقُكُمْ وَإِيَّاهُمْ)، وفي الإسراء الخطاب للأغنياء الخائفين من الفقر (خَشْيَةَ إِمْلَاقٍ) فَقُدِّم رزق الأبناء (نَحْنُ نَرْزُقُهُمْ وَإِيَّاكُمْ).",
            mnemonicRuleEn = "Golden Rule: Al-An'am addresses present poverty ('min imlaq') so parents' provision comes first; Al-Isra addresses fear of future poverty ('khashyata imlaq') so children's provision comes first.",
            mnemonicRuleUz = "Oltin qoida: An'omda mavjud faqirlik ('min imloq') sababli ota-onaning rizqi oldin, Isroda esa faqirlikdan qo'rqish ('xashyata imloq') sababli farzandlar rizqi oldin keltirilgan.",
            sourceBookAr = "درة التنزيل للخطيب الإسكافي",
            ayahs = listOf(
                MutashabihAyahItem(6, "الأنعام", "Al-An'am", 151, "وَلَا تَقْتُلُوا أَوْلَادَكُم مِّنْ إِمْلَاقٍ نَّحْنُ نَرْزُقُكُمْ وَإِيَّاهُمْ", listOf("مِّنْ", "نَرْزُقُكُمْ", "وَإِيَّاهُمْ")),
                MutashabihAyahItem(17, "الإسراء", "Al-Isra", 31, "وَلَا تَقْتُلُوا أَوْلَادَكُمْ خَشْيَةَ إِمْلَاقٍ نَّحْنُ نَرْزُقُهُمْ وَإِيَّاكُمْ", listOf("خَشْيَةَ", "نَرْزُقُهُمْ", "وَإِيَّاكُمْ"))
            )
        ),
        MutashabihGroup(
            id = "sariou_sabiqu",
            titleAr = "وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ أم سَابِقُوا؟",
            titleEn = "'Wa Sari'u' vs 'Sabiqu' to Forgiveness",
            titleUz = "'Va sori'u' va 'Sobiqu' oyatlari",
            categoryAr = "آل عمران والحديد",
            mnemonicRuleAr = "الضابط الذهبي: آل عمران (وَسَارِعُوا — عرضها السماوات والأرض)، أما الحديد فهي أقصر وأسرع (سَابِقُوا — كعرض السماء والأرض بالإفراد).",
            mnemonicRuleEn = "Golden Rule: Aal-E-Imran uses 'Wa Sari'u' with plural 'Samawat'; Al-Hadid uses 'Sabiqu' with singular 'Sama''.",
            mnemonicRuleUz = "Oltin qoida: Oli Imronda 'Va sori'u' bilan ko'plikdagi 'osmonlar' (Samovot), Hadidda esa 'Sobiqu' bilan birlikdagi 'osmon' (Samo) keladi.",
            sourceBookAr = "أسرار التكرار للكرماني",
            ayahs = listOf(
                MutashabihAyahItem(3, "آل عمران", "Aal-E-Imran", 133, "وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ وَجَنَّةٍ عَرْضُهَا السَّمَاوَاتُ وَالْأَرْضُ", listOf("وَسَارِعُوا", "عَرْضُهَا", "السَّمَاوَاتُ")),
                MutashabihAyahItem(57, "الحديد", "Al-Hadid", 21, "سَابِقُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ وَجَنَّةٍ عَرْضُهَا كَعَرْضِ السَّمَاءِ وَالْأَرْضِ", listOf("سَابِقُوا", "كَعَرْضِ", "السَّمَاءِ"))
            )
        ),
        MutashabihGroup(
            id = "tahtaha_min_tahtiha",
            titleAr = "تَجْرِي تَحْتَهَا الْأَنْهَارُ (موضع وحيد في القرآن!)",
            titleEn = "The ONLY Verse Without 'Min' Before 'Tahtaha'",
            titleUz = "'Min' qo'shimchasisiz kelgan yagona oyat",
            categoryAr = "التوبة — موضع فريد",
            mnemonicRuleAr = "الضابط الذهبي: كل مواضع القرآن (مِن تَحْتِهَا الْأَنْهَارُ) إلا موضعاً واحداً في سورة التوبة آية ١٠٠ (تَجْرِي تَحْتَهَا الْأَنْهَارُ خَالِدِينَ فِيهَا أَبَدًا) بغير حرف (مِن)!",
            mnemonicRuleEn = "Golden Rule: Every single verse in the Quran says 'min tahtiha al-anhar' EXCEPT Surah At-Tawbah 9:100 which omits 'min' ('tajri tahtaha al-anhar').",
            mnemonicRuleUz = "Oltin qoida: Qur'ondagi barcha oyatlarda 'min tahtihal-anhor' keladi, faqat Tavba surasi 100-oyatida 'min' so'zisiz 'tajriy tahtahal-anhor' keladi!",
            sourceBookAr = "ضوابط المتشابهات اللفظية",
            ayahs = listOf(
                MutashabihAyahItem(9, "التوبة", "At-Tawbah", 100, "وَأَعَدَّ لَهُمْ جَنَّاتٍ تَجْرِي تَحْتَهَا الْأَنْهَارُ خَالِدِينَ فِيهَا أَبَدًا", listOf("تَحْتَهَا", "أَبَدًا")),
                MutashabihAyahItem(9, "التوبة", "At-Tawbah", 72, "وَعَدَ اللَّهُ الْمُؤْمِنِينَ وَالْمُؤْمِنَاتِ جَنَّاتٍ تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ خَالِدِينَ فِيهَا", listOf("مِن", "تَحْتِهَا"))
            )
        ),
        MutashabihGroup(
            id = "uhilla_bihi",
            titleAr = "وَمَا أُهِلَّ بِهِ لِغَيْرِ اللَّهِ أم لِغَيْرِ اللَّهِ بِهِ؟",
            titleEn = "'Uhilla Bihi Lighayrillah' vs 'Uhilla Lighayrillahi Bihi'",
            titleUz = "'Uhilla bihi' so'z tartibi farqi",
            categoryAr = "البقرة والمائدة والأنعام والنحل",
            mnemonicRuleAr = "الضابط الذهبي: في سورة البقرة وحدها قُدِّم (بِهِ) فقيل (وَمَا أُهِلَّ بِهِ لِغَيْرِ اللَّهِ)، وفي بقية السور الثلاث (المائدة، الأنعام، النحل) أُخِّر (بِهِ): (وَمَا أُهِلَّ لِغَيْرِ اللَّهِ بِهِ).",
            mnemonicRuleEn = "Golden Rule: Only Al-Baqarah 2:173 puts 'bihi' first ('uhilla bihi lighayrillah'); Al-Ma'idah, Al-An'am, and An-Nahl put 'lighayrillah' first.",
            mnemonicRuleUz = "Oltin qoida: Faqat Baqara 173-oyatda 'bihi' oldin keladi; Moida, An'om va Nahl suralarida 'lig'ayrillohi bihi' tartibida keladi.",
            sourceBookAr = "درة التنزيل",
            ayahs = listOf(
                MutashabihAyahItem(2, "البقرة", "Al-Baqarah", 173, "إِنَّمَا حَرَّمَ عَلَيْكُمُ الْمَيْتَةَ وَالدَّمَ وَلَحْمَ الْخِنزِيرِ وَمَا أُهِلَّ بِهِ لِغَيْرِ اللَّهِ", listOf("بِهِ", "لِغَيْرِ", "اللَّهِ")),
                MutashabihAyahItem(16, "النحل", "An-Nahl", 115, "إِنَّمَا حَرَّمَ عَلَيْكُمُ الْمَيْتَةَ وَالدَّمَ وَلَحْمَ الْخِنزِيرِ وَمَا أُهِلَّ لِغَيْرِ اللَّهِ بِهِ", listOf("لِغَيْرِ", "اللَّهِ", "بِهِ"))
            )
        ),
        MutashabihGroup(
            id = "qul_siru",
            titleAr = "قُلْ سِيرُوا فِي الْأَرْضِ ثُمَّ انظُرُوا أم فَانظُرُوا؟",
            titleEn = "'Thummandhuru' vs 'Fandhuru'",
            titleUz = "'Summandhuru' va 'Fandhuru' farqi",
            categoryAr = "الأنعام والنمل والعنكبوت والروم",
            mnemonicRuleAr = "الضابط الذهبي: كل مواضع (قُلْ سِيرُوا فِي الْأَرْضِ) تأتي بالفاء (فَانظُرُوا) إلا موضع الأنعام آية ١١ الوحيد الذي يأتي بـ (ثُمَّ انظُرُوا)!",
            mnemonicRuleEn = "Golden Rule: Only Al-An'am 6:11 uses 'thumma-ndhuru' (then look); An-Naml, Al-Ankabut, and Ar-Rum all use 'fa-ndhuru'.",
            mnemonicRuleUz = "Oltin qoida: Faqat An'om 11-oyatda 'summandhuru' keladi; Naml, Ankabut va Rum suralarida esa 'fandhuru' keladi.",
            sourceBookAr = "ضوابط المتشابهات اللفظية",
            ayahs = listOf(
                MutashabihAyahItem(6, "الأنعام", "Al-An'am", 11, "قُلْ سِيرُوا فِي الْأَرْضِ ثُمَّ انظُرُوا كَيْفَ كَانَ عَاقِبَةُ الْمُكَذِّبِينَ", listOf("ثُمَّ", "انظُرُوا", "الْمُكَذِّبِينَ")),
                MutashabihAyahItem(27, "النمل", "An-Naml", 69, "قُلْ سِيرُوا فِي الْأَرْضِ فَانظُرُوا كَيْفَ كَانَ عَاقِبَةُ الْمُجْرِمِينَ", listOf("فَانظُرُوا", "الْمُجْرِمِينَ"))
            )
        ),
        MutashabihGroup(
            id = "baladan_amina",
            titleAr = "بَلَدًا آمِنًا (البقرة) أم هَٰذَا الْبَلَدَ آمِنًا (إبراهيم)؟",
            titleEn = "Ibrahim's Dua: Indefinite vs Definite Makkah",
            titleUz = "Ibrohim (a.s.) duosidagi noaniq va aniq shakl",
            categoryAr = "البقرة وإبراهيم",
            mnemonicRuleAr = "الضابط الذهبي: في سورة البقرة دعا إبراهيم قبل أن تصبح مكة بلداً مأهولاً فجاءت نكرة (اجْعَلْ هَٰذَا بَلَدًا آمِنًا)، وفي سورة إبراهيم بعد أن استقر بها أهله وولده فجاءت معرفة (اجْعَلْ هَٰذَا الْبَلَدَ آمِنًا).",
            mnemonicRuleEn = "Golden Rule: In Al-Baqarah 2:126 Makkah was not yet a populated city ('baladan amina' indefinite); in Ibrahim 14:35 it had become a town ('al-balada amina' definite).",
            mnemonicRuleUz = "Oltin qoida: Baqara 126-oyatda Makka hali obod shahar bo'lmagani uchun noaniq ('baladan'), Ibrohim 35-oyatda esa obod bo'lgani uchun aniq ('al-balada') kelgan.",
            sourceBookAr = "ملاك التأويل لابن الزبير الغرناطي",
            ayahs = listOf(
                MutashabihAyahItem(2, "البقرة", "Al-Baqarah", 126, "وَإِذْ قَالَ إِبْرَاهِيمُ رَبِّ اجْعَلْ هَٰذَا بَلَدًا آمِنًا وَارْزُقْ أَهْلَهُ مِنَ الثَّمَرَاتِ", listOf("بَلَدًا", "آمِنًا")),
                MutashabihAyahItem(14, "إبراهيم", "Ibrahim", 35, "وَإِذْ قَالَ إِبْرَاهِيمُ رَبِّ اجْعَلْ هَٰذَا الْبَلَدَ آمِنًا وَاجْنُبْنِي وَبَنِيَّ أَن نَّعْبُدَ الْأَصْنَامَ", listOf("الْبَلَدَ", "آمِنًا"))
            )
        ),
        MutashabihGroup(
            id = "fawz_azeem",
            titleAr = "ذَٰلِكَ الْفَوْزُ الْعَظِيمُ أم ذَٰلِكَ هُوَ الْفَوْزُ الْعَظِيمُ؟",
            titleEn = "'Dhalika al-Fawz al-Azim' vs With 'Huwa'",
            titleUz = "'Huva' olmoshi bilan va olmoshsiz kelishi",
            categoryAr = "النساء والتوبة والصف",
            mnemonicRuleAr = "الضابط الذهبي: تأتي (ذَٰلِكَ هُوَ الْفَوْزُ الْعَظِيمُ) بضمير الفصل (هُوَ) في ٥ مواضع شهيرة (التوبة ٧٢ و٨٩ و١١١، يونس ٦٤، الصافات ٦٠، غافر ٩، الدخان ٥٧)، بينما في النساء ١٣ والتوبة ١٠٠ والصف ١٢ بغير (هُوَ).",
            mnemonicRuleEn = "Golden Rule: Look at the emphasis pronoun 'huwa' — present in At-Tawbah 9:72, 89, 111 & Yunus 10:64; omitted in An-Nisa 4:13, At-Tawbah 9:100, and As-Saff 61:12.",
            mnemonicRuleUz = "Oltin qoida: 'Huva' ajratish olmoshi Tavba 72, 89, 111 va Yunus 64-oyatlarida bor; Niso 13, Tavba 100 va Saf 12-oyatlarida esa 'huva'siz keladi.",
            sourceBookAr = "البرهان في توجيه متشابه القرآن",
            ayahs = listOf(
                MutashabihAyahItem(4, "النساء", "An-Nisa", 13, "جَنَّاتٍ تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ خَالِدِينَ فِيهَا وَذَٰلِكَ الْفَوْزُ الْعَظِيمُ", listOf("وَذَٰلِكَ", "الْفَوْزُ")),
                MutashabihAyahItem(9, "التوبة", "At-Tawbah", 72, "وَرِضْوَانٌ مِّنَ اللَّهِ أَكْبَرُ ذَٰلِكَ هُوَ الْفَوْزُ الْعَظِيمُ", listOf("ذَٰلِكَ", "هُوَ", "الْفَوْزُ"))
            )
        ),
        MutashabihGroup(
            id = "ya_qawmi_ibudu",
            titleAr = "يَا قَوْمِ اعْبُدُوا اللَّهَ مَا لَكُم مِّنْ إِلَٰهٍ غَيْرُهُ",
            titleEn = "Call of the Prophets: Al-A'raf vs Hud",
            titleUz = "Payg'ambarlar da'vati: A'rof va Hud suralari",
            categoryAr = "قصص الأنبياء (الأعراف وهود)",
            mnemonicRuleAr = "الضابط الذهبي: في الأعراف تتبعها (أَفَلَا تَتَّقُونَ) مع هود، وفي هود تتبعها (إِنْ أَنتُمْ إِلَّا مُفْتَرُونَ) مع هود و(فَاسْتَغْفِرُوهُ ثُمَّ تُوبُوا إِلَيْهِ) مع صالح وشعيب.",
            mnemonicRuleEn = "Golden Rule: Notice each Prophet's distinctive closing phrase in Al-A'raf (taqwa & warning) vs Surah Hud (istighfar & tawbah).",
            mnemonicRuleUz = "Oltin qoida: A'rof surasida taqvo va ogohlantirish, Hud surasida esa istig'for va tavbaga chaqiriq bilan yakunlanadi.",
            sourceBookAr = "درة التنزيل",
            ayahs = listOf(
                MutashabihAyahItem(7, "الأعراف", "Al-A'raf", 65, "قَالَ يَا قَوْمِ اعْبُدُوا اللَّهَ مَا لَكُم مِّنْ إِلَٰهٍ غَيْرُهُ أَفَلَا تَتَّقُونَ", listOf("أَفَلَا", "تَتَّقُونَ")),
                MutashabihAyahItem(11, "هود", "Hud", 50, "قَالَ يَا قَوْمِ اعْبُدُوا اللَّهَ مَا لَكُم مِّنْ إِلَٰهٍ غَيْرُهُ إِنْ أَنتُمْ إِلَّا مُفْتَرُونَ", listOf("إِنْ", "أَنتُمْ", "إِلَّا", "مُفْتَرُونَ"))
            )
        ),
        MutashabihGroup(
            id = "musa_bayda",
            titleAr = "وَنَزَعَ يَدَهُ (الأعراف والشعراء) أم وَاضْمُمْ يَدَكَ (طه والقصص)؟",
            titleEn = "Miracle of Musa's Hand Across Surahs",
            titleUz = "Muso (a.s.) qo'li mo'jizasi oyatlari farqi",
            categoryAr = "الأعراف وطه والقصص",
            mnemonicRuleAr = "الضابط الذهبي: عند الإخبار بوقوع المعجزة أمام فرعون (وَنَزَعَ يَدَهُ فَإِذَا هِيَ بَيْضَاءُ لِلنَّاظِرِينَ)، وعند الأمر الإلهي لموسى بالوادي المقدس (وَاضْمُمْ يَدَكَ إِلَىٰ جَنَاحِكَ) في طه، و(اسْلُكْ يَدَكَ فِي جَيْبِكَ) في القصص، و(وَأَدْخِلْ يَدَكَ فِي جَيْبِكَ) في النمل.",
            mnemonicRuleEn = "Golden Rule: Before Pharaoh it is 'naza'a yadahu' (Al-A'raf/Ash-Shu'ara); at Mount Tur it is 'udmum' (Ta-Ha), 'adkhil' (An-Naml), and 'usluk' (Al-Qasas).",
            mnemonicRuleUz = "Oltin qoida: Fir'avn oldida 'naza'a yadahu' (A'rof/Shuaro); Tur vodiysida esa 'uzmum' (Toha), 'adxil' (Naml) va 'usluk' (Qasas) lafzlari keladi.",
            sourceBookAr = "أسرار التكرار للكرماني",
            ayahs = listOf(
                MutashabihAyahItem(7, "الأعراف", "Al-A'raf", 108, "وَنَزَعَ يَدَهُ فَإِذَا هِيَ بَيْضَاءُ لِلنَّاظِرِينَ", listOf("وَنَزَعَ", "لِلنَّاظِرِينَ")),
                MutashabihAyahItem(20, "طه", "Ta-Ha", 22, "وَاضْمُمْ يَدَكَ إِلَىٰ جَنَاحِكَ تَخْرُجْ بَيْضَاءَ مِنْ غَيْرِ سُوءٍ آيَةً أُخْرَىٰ", listOf("وَاضْمُمْ", "جَنَاحِكَ", "آيَةً", "أُخْرَىٰ")),
                MutashabihAyahItem(28, "القصص", "Al-Qasas", 32, "اسْلُكْ يَدَكَ فِي جَيْبِكَ تَخْرُجْ بَيْضَاءَ مِنْ غَيْرِ سُوءٍ", listOf("اسْلُكْ", "جَيْبِكَ"))
            )
        ),
        MutashabihGroup(
            id = "fatakuna_minazzalimin",
            titleAr = "فَتَكُونَا مِنَ الظَّالِمِينَ (البقرة) أم فَتَكُونَا مِنَ الظَّالِمِينَ (الأعراف)",
            titleEn = "Adam & Hawwa's Story: 'Wa Kula' vs 'Fakula'",
            titleUz = "Odam (a.s.) qissasi: Baqara va A'rof farqi",
            categoryAr = "البقرة والأعراف",
            mnemonicRuleAr = "الضابط الذهبي: في البقرة جاءت بـ (وَكُلَا مِنْهَا رَغَدًا حَيْثُ شِئْتُمَا) بالواو مع كلمة (رَغَدًا)، وفي الأعراف بـ (فَكُلَا مِنْ حَيْثُ شِئْتُمَا) بالفاء وزيادة (مِنْ) دون كلمة (رَغَدًا).",
            mnemonicRuleEn = "Golden Rule: Al-Baqarah 2:35 has 'wa kula minha raghadan haythu shi'tuma'; Al-A'raf 7:19 has 'fa kula min haythu shi'tuma' without 'raghadan'.",
            mnemonicRuleUz = "Oltin qoida: Baqara 35-oyatda 'va kulo minho rag'adan', A'rof 19-oyatda esa 'fa kulo min haysu shi'tumo' ('rag'adan' so'zisiz) keladi.",
            sourceBookAr = "ملاك التأويل",
            ayahs = listOf(
                MutashabihAyahItem(2, "البقرة", "Al-Baqarah", 35, "وَكُلَا مِنْهَا رَغَدًا حَيْثُ شِئْتُمَا وَلَا تَقْرَبَا هَٰذِهِ الشَّجَرَةَ فَتَكُونَا مِنَ الظَّالِمِينَ", listOf("وَكُلَا", "مِنْهَا", "رَغَدًا")),
                MutashabihAyahItem(7, "الأعراف", "Al-A'raf", 19, "فَكُلَا مِنْ حَيْثُ شِئْتُمَا وَلَا تَقْرَبَا هَٰذِهِ الشَّجَرَةَ فَتَكُونَا مِنَ الظَّالِمِينَ", listOf("فَكُلَا", "مِنْ"))
            )
        )
    )

    /**
     * Computes word-by-word differential highlights between a target Ayah and sibling Ayahs
     * in the same Mutashabihat group so unique/divergent words stand out clearly.
     */
    fun computeWordDiffs(
        targetAyahText: String,
        peerAyahTexts: List<String>
    ): List<MutashabihDiffWord> {
        val targetWords = targetAyahText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (peerAyahTexts.isEmpty()) {
            return targetWords.map { MutashabihDiffWord(it, false) }
        }
        val peerNormalizedSets = peerAyahTexts.map { peer ->
            peer.trim().split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .map { QuranMorphologyIrabEngine.stripDiacritics(it) }
                .toSet()
        }
        return targetWords.map { word ->
            val plain = QuranMorphologyIrabEngine.stripDiacritics(word)
            val sharedInAllPeers = peerNormalizedSets.all { plain in it }
            MutashabihDiffWord(word = word, isDistinctive = !sharedInAllPeers)
        }
    }
}
