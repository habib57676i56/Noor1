package com.example.data.local.catalog

import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.model.SurahMeta

data class AsbabEntry(
    val surahNumber: Int,
    val ayahNumber: Int,
    val reasonAr: String,
    val reasonEn: String,
    val sourceAr: String,
    val sourceEn: String
)

object SurahCatalog {

    val allSurahs: List<SurahMeta> = listOf(
        SurahMeta(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, true, 1, 1, 1),
        SurahMeta(2, "البقرة", "Al-Baqarah", "The Cow", 286, false, 1, 1, 2),
        SurahMeta(3, "آل عمران", "Aal-E-Imran", "Family of Imran", 200, false, 3, 5, 50),
        SurahMeta(4, "النساء", "An-Nisa", "The Women", 176, false, 4, 8, 77),
        SurahMeta(5, "المائدة", "Al-Ma'idah", "The Table Spread", 120, false, 6, 11, 106),
        SurahMeta(6, "الأنعام", "Al-An'am", "The Cattle", 165, true, 7, 13, 128),
        SurahMeta(7, "الأعراف", "Al-A'raf", "The Heights", 206, true, 8, 16, 151),
        SurahMeta(8, "الأنفال", "Al-Anfal", "The Spoils of War", 75, false, 9, 18, 177),
        SurahMeta(9, "التوبة", "At-Tawbah", "The Repentance", 129, false, 10, 19, 187),
        SurahMeta(10, "يونس", "Yunus", "Jonah", 109, true, 11, 21, 208),
        SurahMeta(11, "هود", "Hud", "Hud", 123, true, 11, 22, 221),
        SurahMeta(12, "يوسف", "Yusuf", "Joseph", 111, true, 12, 24, 235),
        SurahMeta(13, "الرعد", "Ar-Ra'd", "The Thunder", 43, false, 13, 25, 249),
        SurahMeta(14, "إبراهيم", "Ibrahim", "Abraham", 52, true, 13, 26, 255),
        SurahMeta(15, "الحجر", "Al-Hijr", "The Rocky Tract", 99, true, 14, 27, 262),
        SurahMeta(16, "النحل", "An-Nahl", "The Bee", 128, true, 14, 27, 267),
        SurahMeta(17, "الإسراء", "Al-Isra", "The Night Journey", 111, true, 15, 29, 282),
        SurahMeta(18, "الكهف", "Al-Kahf", "The Cave", 110, true, 15, 30, 293),
        SurahMeta(19, "مريم", "Maryam", "Mary", 98, true, 16, 31, 305),
        SurahMeta(20, "طه", "Ta-Ha", "Ta-Ha", 135, true, 16, 32, 312),
        SurahMeta(21, "الأنبياء", "Al-Anbiya", "The Prophets", 112, true, 17, 33, 322),
        SurahMeta(22, "الحج", "Al-Hajj", "The Pilgrimage", 78, false, 17, 34, 332),
        SurahMeta(23, "المؤمنون", "Al-Mu'minun", "The Believers", 118, true, 18, 35, 342),
        SurahMeta(24, "النور", "An-Nur", "The Light", 64, false, 18, 36, 350),
        SurahMeta(25, "الفرقان", "Al-Furqan", "The Criterion", 77, true, 18, 36, 359),
        SurahMeta(26, "الشعراء", "Ash-Shu'ara", "The Poets", 227, true, 19, 37, 367),
        SurahMeta(27, "النمل", "An-Naml", "The Ant", 93, true, 19, 38, 377),
        SurahMeta(28, "القصص", "Al-Qasas", "The Stories", 88, true, 20, 39, 385),
        SurahMeta(29, "العنكبوت", "Al-Ankabut", "The Spider", 69, true, 20, 40, 396),
        SurahMeta(30, "الروم", "Ar-Rum", "The Romans", 60, true, 21, 41, 404),
        SurahMeta(31, "لقمان", "Luqman", "Luqman", 34, true, 21, 41, 411),
        SurahMeta(32, "السجدة", "As-Sajdah", "The Prostration", 30, true, 21, 42, 415),
        SurahMeta(33, "الأحزاب", "Al-Ahzab", "The Combined Forces", 73, false, 21, 42, 418),
        SurahMeta(34, "سبأ", "Saba", "Sheba", 54, true, 22, 43, 428),
        SurahMeta(35, "فاطر", "Fatir", "Originator", 45, true, 22, 44, 434),
        SurahMeta(36, "يس", "Ya-Sin", "Ya Sin", 83, true, 22, 44, 440),
        SurahMeta(37, "الصافات", "As-Saffat", "Those Who Set the Ranks", 182, true, 23, 45, 446),
        SurahMeta(38, "ص", "Sad", "The Letter Sad", 88, true, 23, 46, 453),
        SurahMeta(39, "الزمر", "Az-Zumar", "The Troops", 75, true, 23, 46, 458),
        SurahMeta(40, "غافر", "Ghafir", "The Forgiver", 85, true, 24, 47, 467),
        SurahMeta(41, "فصلت", "Fussilat", "Explained in Detail", 54, true, 24, 48, 477),
        SurahMeta(42, "الشورى", "Ash-Shura", "The Consultation", 53, true, 25, 49, 483),
        SurahMeta(43, "الزخرف", "Az-Zukhruf", "Ornaments of Gold", 89, true, 25, 49, 489),
        SurahMeta(44, "الدخان", "Ad-Dukhan", "The Smoke", 59, true, 25, 50, 496),
        SurahMeta(45, "الجاثية", "Al-Jathiyah", "The Crouching", 37, true, 25, 50, 499),
        SurahMeta(46, "الأحقاف", "Al-Ahqaf", "The Wind-Curved Sandhills", 35, true, 26, 51, 502),
        SurahMeta(47, "محمد", "Muhammad", "Muhammad", 38, false, 26, 51, 507),
        SurahMeta(48, "الفتح", "Al-Fath", "The Victory", 29, false, 26, 52, 511),
        SurahMeta(49, "الحجرات", "Al-Hujurat", "The Rooms", 18, false, 26, 52, 515),
        SurahMeta(50, "ق", "Qaf", "The Letter Qaf", 45, true, 26, 52, 518),
        SurahMeta(51, "الذاريات", "Adh-Dhariyat", "The Winnowing Winds", 60, true, 26, 52, 520),
        SurahMeta(52, "الطور", "At-Tur", "The Mount", 49, true, 27, 53, 523),
        SurahMeta(53, "النجم", "An-Najm", "The Star", 62, true, 27, 53, 526),
        SurahMeta(54, "القمر", "Al-Qamar", "The Moon", 55, true, 27, 53, 528),
        SurahMeta(55, "الرحمن", "Ar-Rahman", "The Beneficent", 78, false, 27, 54, 531),
        SurahMeta(56, "الواقعة", "Al-Waqi'ah", "The Inevitable", 96, true, 27, 54, 534),
        SurahMeta(57, "الحديد", "Al-Hadid", "The Iron", 29, false, 27, 54, 537),
        SurahMeta(58, "المجادلة", "Al-Mujadila", "The Pleading Woman", 22, false, 28, 55, 542),
        SurahMeta(59, "الحشر", "Al-Hashr", "The Exile", 24, false, 28, 55, 545),
        SurahMeta(60, "الممتحنة", "Al-Mumtahanah", "She That Is to Be Examined", 13, false, 28, 55, 549),
        SurahMeta(61, "الصف", "As-Saff", "The Ranks", 14, false, 28, 55, 551),
        SurahMeta(62, "الجمعة", "Al-Jumu'ah", "The Congregation", 11, false, 28, 56, 553),
        SurahMeta(63, "المنافقون", "Al-Munafiqun", "The Hypocrites", 11, false, 28, 56, 554),
        SurahMeta(64, "التغابن", "At-Taghabun", "The Mutual Disillusion", 18, false, 28, 56, 556),
        SurahMeta(65, "الطلاق", "At-Talaq", "The Divorce", 12, false, 28, 56, 558),
        SurahMeta(66, "التحريم", "At-Tahrim", "The Prohibition", 12, false, 28, 56, 560),
        SurahMeta(67, "الملك", "Al-Mulk", "The Sovereignty", 30, true, 29, 57, 562),
        SurahMeta(68, "القلم", "Al-Qalam", "The Pen", 52, true, 29, 57, 564),
        SurahMeta(69, "الحاقة", "Al-Haqqah", "The Reality", 52, true, 29, 57, 566),
        SurahMeta(70, "المعارج", "Al-Ma'arij", "The Ascending Stairways", 44, true, 29, 57, 568),
        SurahMeta(71, "نوح", "Nuh", "Noah", 28, true, 29, 57, 570),
        SurahMeta(72, "الجن", "Al-Jinn", "The Jinn", 28, true, 29, 58, 572),
        SurahMeta(73, "المزمل", "Al-Muzzammil", "The Enshrouded One", 20, true, 29, 58, 574),
        SurahMeta(74, "المدثر", "Al-Muddaththir", "The Cloaked One", 56, true, 29, 58, 575),
        SurahMeta(75, "القيامة", "Al-Qiyamah", "The Resurrection", 40, true, 29, 58, 577),
        SurahMeta(76, "الإنسان", "Al-Insan", "The Man", 31, false, 29, 58, 578),
        SurahMeta(77, "المرسلات", "Al-Mursalat", "The Emissaries", 50, true, 29, 58, 580),
        SurahMeta(78, "النبأ", "An-Naba", "The Tidings", 40, true, 30, 59, 582),
        SurahMeta(79, "النازعات", "An-Nazi'at", "Those Who Drag Forth", 46, true, 30, 59, 583),
        SurahMeta(80, "عبس", "Abasa", "He Frowned", 42, true, 30, 59, 585),
        SurahMeta(81, "التكوير", "At-Takwir", "The Overthrowing", 29, true, 30, 59, 586),
        SurahMeta(82, "الانفطار", "Al-Infitar", "The Cleaving", 19, true, 30, 59, 587),
        SurahMeta(83, "المطففين", "Al-Mutaffifin", "The Defrauding", 36, true, 30, 59, 587),
        SurahMeta(84, "الانشقاق", "Al-Inshiqaq", "The Sundering", 25, true, 30, 59, 589),
        SurahMeta(85, "البروج", "Al-Buruj", "The Mansions of the Stars", 22, true, 30, 59, 590),
        SurahMeta(86, "الطارق", "At-Tariq", "The Nightcomer", 17, true, 30, 59, 591),
        SurahMeta(87, "الأعلى", "Al-A'la", "The Most High", 19, true, 30, 60, 591),
        SurahMeta(88, "الغاشية", "Al-Ghashiyah", "The Overwhelming", 26, true, 30, 60, 592),
        SurahMeta(89, "الفجر", "Al-Fajr", "The Dawn", 30, true, 30, 60, 593),
        SurahMeta(90, "البلد", "Al-Balad", "The City", 20, true, 30, 60, 594),
        SurahMeta(91, "الشمس", "Ash-Shams", "The Sun", 15, true, 30, 60, 595),
        SurahMeta(92, "الليل", "Al-Layl", "The Night", 21, true, 30, 60, 595),
        SurahMeta(93, "الضحى", "Ad-Duhaa", "The Morning Hours", 11, true, 30, 60, 596),
        SurahMeta(94, "الشرح", "Ash-Sharh", "The Relief", 8, true, 30, 60, 596),
        SurahMeta(95, "التين", "At-Tin", "The Fig", 8, true, 30, 60, 597),
        SurahMeta(96, "العلق", "Al-Alaq", "The Clot", 19, true, 30, 60, 597),
        SurahMeta(97, "القدر", "Al-Qadr", "The Power", 5, true, 30, 60, 598),
        SurahMeta(98, "البينة", "Al-Bayyinah", "The Clear Proof", 8, false, 30, 60, 598),
        SurahMeta(99, "الزلزلة", "Az-Zalzalah", "The Earthquake", 8, false, 30, 60, 599),
        SurahMeta(100, "العاديات", "Al-Adiyat", "The Courser", 11, true, 30, 60, 599),
        SurahMeta(101, "القارعة", "Al-Qari'ah", "The Calamity", 11, true, 30, 60, 600),
        SurahMeta(102, "التكاثر", "At-Takathur", "The Rivalry in World Increase", 8, true, 30, 60, 600),
        SurahMeta(103, "العصر", "Al-Asr", "The Declining Day", 3, true, 30, 60, 601),
        SurahMeta(104, "الهمزة", "Al-Humazah", "The Traducer", 9, true, 30, 60, 601),
        SurahMeta(105, "الفيل", "Al-Fil", "The Elephant", 5, true, 30, 60, 601),
        SurahMeta(106, "قريش", "Quraysh", "Quraysh", 4, true, 30, 60, 602),
        SurahMeta(107, "الماعون", "Al-Ma'un", "The Small Kindnesses", 7, true, 30, 60, 602),
        SurahMeta(108, "الكوثر", "Al-Kawthar", "The Abundance", 3, true, 30, 60, 602),
        SurahMeta(109, "الكافرون", "Al-Kafirun", "The Disbelievers", 6, true, 30, 60, 603),
        SurahMeta(110, "النصر", "An-Nasr", "The Divine Support", 3, false, 30, 60, 603),
        SurahMeta(111, "المسد", "Al-Masad", "The Palm Fiber", 5, true, 30, 60, 603),
        SurahMeta(112, "الإخلاص", "Al-Ikhlas", "The Sincerity", 4, true, 30, 60, 604),
        SurahMeta(113, "الفلق", "Al-Falaq", "The Daybreak", 5, true, 30, 60, 604),
        SurahMeta(114, "الناس", "An-Nas", "Mankind", 6, true, 30, 60, 604)
    )

    fun findSurah(number: Int): SurahMeta =
        allSurahs.firstOrNull { it.number == number } ?: allSurahs.first()

    fun findSurahByPage(page: Int): SurahMeta {
        val clamped = page.coerceIn(1, 604)
        return allSurahs.lastOrNull { it.startPage <= clamped } ?: allSurahs.first()
    }

    /**
     * Authoritative verse-level recitation URL via EveryAyah (supports Sheikh Yasser Al-Dosari & all reciters).
     */
    fun audioUrlForAyah(surahNumber: Int, ayahNumber: Int, reciter: QuranReciter): String =
        reciter.ayahAudioUrl(surahNumber, ayahNumber)

    /**
     * Legacy fallback kept for compatibility.
     */
    fun audioUrlForGlobalAyah(globalNumber: Int, reciter: QuranReciter): String =
        "https://cdn.islamic.network/quran/audio/128/${reciter.cdnSlug}/$globalNumber.mp3"

    /**
     * Verified Asbab al-Nuzul (Occasions of Revelation) from Sahih al-Bukhari, Sahih Muslim, and Sunan At-Tirmidhi.
     */
    val verifiedAsbab: Map<Pair<Int, Int>, AsbabEntry> = listOf(
        AsbabEntry(
            surahNumber = 93,
            ayahNumber = 1,
            reasonAr = "أبطأ الوحي على النبي ﷺ أياماً فقالت امرأة من قريش: ما أرى شيطانك إلا قد تركك، فأنزل الله تعالى سورة الضحى تطميناً لقلب نبيه ﷺ وتثبيتاً له.",
            reasonEn = "When revelation paused briefly for some days, a woman from Quraysh taunted the Prophet (PBUH), so Allah revealed Surah Ad-Duhaa reassuring him that his Lord had neither forsaken nor been displeased with him.",
            sourceAr = "صحيح البخاري (٤٩٥٠) وصحيح مسلم (١٧٩٧)",
            sourceEn = "Sahih al-Bukhari 4950 & Sahih Muslim 1797"
        ),
        AsbabEntry(
            surahNumber = 108,
            ayahNumber = 1,
            reasonAr = "أُنزلت سورة الكوثر بشارة للنبي ﷺ بنهر الكوثر في الجنة والخير الكثير، وردّاً على من عيّره بانقطاع النسل من مشركي مكة.",
            reasonEn = "Revealed to honor Prophet Muhammad (PBUH) with Al-Kawthar in Paradise and refute those who mocked him over the loss of his sons.",
            sourceAr = "صحيح مسلم (٤٠٠)",
            sourceEn = "Sahih Muslim 400"
        ),
        AsbabEntry(
            surahNumber = 110,
            ayahNumber = 1,
            reasonAr = "نزلت بعد فتح مكة ودخول الناس في دين الله أفواجاً؛ قال ابن عباس رضي الله عنهما: هي أجل رسول الله ﷺ أُعلمه الله تعالى وأمره بالتسبيح والاستغفار.",
            reasonEn = "Revealed following the Conquest of Makkah; Ibn Abbas (RA) explained that it signified the completion of the Prophet's mission and instructed him to glorify and seek forgiveness from Allah.",
            sourceAr = "صحيح البخاري (٤٩٧٠)",
            sourceEn = "Sahih al-Bukhari 4970"
        ),
        AsbabEntry(
            surahNumber = 111,
            ayahNumber = 1,
            reasonAr = "لما صعد النبي ﷺ الصفا ودعا قريشاً إلى التوحيد، قال أبو لهب: تبّاً لك ألهذا جمعتنا؟ فأنزل الله تعالى سورة المسد.",
            reasonEn = "When the Prophet (PBUH) stood on Mount Safa calling Quraysh to monotheism, Abu Lahab spoke harshly against him, upon which Surah Al-Masad was revealed.",
            sourceAr = "صحيح البخاري (٤٧٧٠)",
            sourceEn = "Sahih al-Bukhari 4770"
        ),
        AsbabEntry(
            surahNumber = 112,
            ayahNumber = 1,
            reasonAr = "قال المشركون للنبي ﷺ: انسب لنا ربك، فأنزل الله سورة الإخلاص بياناً خالصاً لوحدانية الله وكماله.",
            reasonEn = "Polytheists asked the Prophet (PBUH) to describe the lineage of his Lord, so Allah revealed Surah Al-Ikhlas defining His absolute Oneness.",
            sourceAr = "سنن الترمذي (٣٣٦٤) ومسند أحمد",
            sourceEn = "Sunan At-Tirmidhi 3364 & Musnad Ahmad"
        )
    ).associateBy { it.surahNumber to it.ayahNumber }

    fun getAsbab(surahNumber: Int, ayahNumber: Int): AsbabEntry? =
        verifiedAsbab[surahNumber to ayahNumber]

    /**
     * Verified bundled offline Uthmani text, Al-Muyassar Tafsir, English Sahih International meaning,
     * and concise Arabic word meanings so key Surahs work instantaneously offline on first launch.
     */
    fun getBundledSurahVerses(surahNumber: Int, reciter: QuranReciter): List<QuranVerse>? {
        val raw = bundledSurahs[surahNumber] ?: return null
        val meta = findSurah(surahNumber)
        return raw.map { v ->
            val asbab = getAsbab(surahNumber, v.numberInSurah)
            QuranVerse(
                surahNumber = surahNumber,
                numberInSurah = v.numberInSurah,
                globalNumber = v.globalNumber,
                textUthmani = v.textUthmani,
                tafsirAr = v.tafsirAr,
                audioUrl = audioUrlForAyah(surahNumber, v.numberInSurah, reciter),
                translationEn = v.translationEn,
                pageNumber = meta.startPage,
                juzNumber = meta.startJuz,
                hizbQuarter = meta.startHizb * 4 - 3,
                wordMeaningsAr = v.wordMeaningsAr,
                asbabAlNuzulAr = asbab?.let { "${it.reasonAr} (${it.sourceAr})" } ?: "",
                asbabAlNuzulEn = asbab?.let { "${it.reasonEn} (${it.sourceEn})" } ?: ""
            )
        }
    }

    private data class RawVerse(
        val numberInSurah: Int,
        val globalNumber: Int,
        val textUthmani: String,
        val tafsirAr: String,
        val translationEn: String = "",
        val wordMeaningsAr: String = ""
    )

    private val bundledSurahs: Map<Int, List<RawVerse>> = mapOf(
        1 to listOf(
            RawVerse(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "أبتدئ قراءة القرآن باسم الله مستعيناً به؛ اللهِ المعبود بحق، الرحمنِ ذي الرحمة العامة، الرحيمِ بالمؤمنين.", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "الرَّحْمَٰنِ: ذو الرحمة الواسعة · الرَّحِيمِ: الموصل رحمته لمن يشاء"),
            RawVerse(2, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "الثناء الكامل لله وحده، مالك جميع الخلائق ومدبر شؤونهم ومربيهم بنعمه.", "[All] praise is [due] to Allah, Lord of the worlds —", "الْعَالَمِينَ: كل ما سوى الله من الخلائق"),
            RawVerse(3, 3, "الرَّحْمَٰنِ الرَّحِيمِ", "ذو الرحمة الواسعة التي شملت كل شيء، والرحمة الخاصة بالمؤمنين.", "The Entirely Merciful, the Especially Merciful,", ""),
            RawVerse(4, 4, "مَالِكِ يَوْمِ الدِّينِ", "المتفرد بالملك والحكم يوم الجزاء والحساب.", "Sovereign of the Day of Recompense.", "يَوْمِ الدِّينِ: يوم الجزاء والحساب"),
            RawVerse(5, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "نخصك وحدك بالعبادة والطاعة، ونستعين بك وحدك في جميع أمورنا.", "It is You we worship and You we ask for help.", "نَسْتَعِينُ: نطلب العون والتوفيق"),
            RawVerse(6, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "دلّنا وأرشدنا ووفقنا إلى الطريق الواضح القويم الذي لا اعوجاج فيه وهو الإسلام.", "Guide us to the straight path —", "الصِّرَاطَ الْمُسْتَقِيمَ: الطريق القويم وهو الإسلام"),
            RawVerse(7, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "طريق النبيين والصديقين والشهداء والصالحين، غير طريق من علموا الحق وتركوه، ولا من تاهوا عنه بجهل.", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", "الضَّالِّينَ: التائهين عن طريق الحق بجهل")
        ),
        93 to listOf(
            RawVerse(1, 6080, "وَالضُّحَىٰ", "أقسم الله بوقت الضحى؛ أي أول النهار حين يرتفع نور الشمس.", "By the morning brightness", "الضُّحَىٰ: صدر النهار عند ارتفاع الشمس"),
            RawVerse(2, 6081, "وَاللَّيْلِ إِذَا سَجَىٰ", "وبالليل إذا سكن واشتد ظلامه وهدأت فيه الأصوات.", "And [by] the night when it covers with darkness,", "سَجَىٰ: سكن وغطّى بظلامه"),
            RawVerse(3, 6082, "مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ", "ما تركك ربك يا محمد وما أبغضك بإبطاء الوحي عنك.", "Your Lord has not taken leave of you, [O Muhammad], nor has He detested [you].", "وَدَّعَكَ: تركك · قَلَىٰ: أبغضك"),
            RawVerse(4, 6083, "وَلَلْآخِرَةُ خَيْرٌ لَّكَ مِنَ الْأُولَىٰ", "وللدار الآخرة خير لك من الحياة الدنيا وما فيها.", "And the Hereafter is better for you than the first [life].", ""),
            RawVerse(5, 6084, "وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ", "ولسوف يعطيك ربك من أنواع الكرامة والفضل حتى ترضى.", "And your Lord is going to give you, and you will be satisfied.", ""),
            RawVerse(6, 6085, "أَلَمْ يَجِدْكَ يَتِيمًا فَآوَىٰ", "ألم يجدك يتيماً قد مات أبوك فآواك ورعاك؟", "Did He not find you an orphan and give [you] refuge?", "آوَىٰ: ضمّك ورعاك"),
            RawVerse(7, 6086, "وَوَجَدَكَ ضَالًّا فَهَدَىٰ", "ووجدك غير عالم بالوحي والشرائع فهداك إلى الحق.", "And He found you lost and guided [you],", ""),
            RawVerse(8, 6087, "وَوَجَدَكَ عَائِلًا فَأَغْنَىٰ", "ووجدك فقيراً فأغناك ورزقك القناعة والكفاية.", "And He found you poor and made [you] self-sufficient.", "عَائِلًا: فقيراً محتاجاً"),
            RawVerse(9, 6088, "فَأَمَّا الْيَتِيمَ فَلَا تَقْهَرْ", "فلا تسئ معاملة اليتيم ولا تذله.", "So as for the orphan, do not oppress [him].", "تَقْهَرْ: تذل وتغلب"),
            RawVerse(10, 6089, "وَأَمَّا السَّائِلَ فَلَا تَنْهَرْ", "ولا تزجر السائل بل أطعمه أو رده برفق.", "And as for the petitioner, do not repel [him].", "تَنْهَرْ: تزجر بغلظة"),
            RawVerse(11, 6090, "وَأَمَّا بِنِعْمَةِ رَبِّكَ فَحَدِّثْ", "وأظهر شكر نعم الله عليك وتحدث بها اعترافاً بفضله.", "But as for the favor of your Lord, report [it].", "")
        ),
        94 to listOf(
            RawVerse(1, 6091, "أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ", "ألم نوسّع لك يا محمد صدرك بنور الإسلام والهداية والسكينة؟", "Did We not expand for you, [O Muhammad], your breast?", "نَشْرَحْ: نوسّع وننوّر"),
            RawVerse(2, 6092, "وَوَضَعْنَا عَنكَ وِزْرَكَ", "وحططنا عنك حملك الثقيل الذي كان يرهقك.", "And We removed from you your burden", "وِزْرَكَ: حملك الثقيل"),
            RawVerse(3, 6093, "الَّذِي أَنقَضَ ظَهْرَكَ", "الذي أثقل ظهرك قبل النبوة فأذهبناه عنك.", "Which had weighed upon your back", "أَنقَضَ: أثقل وأجهد"),
            RawVerse(4, 6094, "وَرَفَعْنَا لَكَ ذِكْرَكَ", "وجعلناك مرفوع الشأن في الدنيا والآخرة يُذكر اسمك مع اسم الله في الأذان والشهادة.", "And raised high for you your repute.", ""),
            RawVerse(5, 6095, "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "فلا يثنينّك أذى عن الدعوة فإن مع الضيق فرجاً قريباً.", "For indeed, with hardship [will be] ease.", "الْعُسْرِ: الشدة والضيق · يُسْرًا: فرجاً وسعة"),
            RawVerse(6, 6096, "إِنَّ مَعَ الْعُسْرِ يُسْرًا", "تأكيد لبشارة الله أن العسر الواحد يعقبه يسران.", "Indeed, with hardship [will be] ease.", ""),
            RawVerse(7, 6097, "فَإِذَا فَرَغْتَ فَانصَبْ", "فإذا فرغت من أشغالك فاجتهد في العبادة والدعاء.", "So when you have finished [your duties], then stand up [for worship].", "فَانصَبْ: اجتهد في العبادة"),
            RawVerse(8, 6098, "وَإِلَىٰ رَبِّكَ فَارْغَب", "واجعل رغبتك وتوجهك كله إلى ربك وحده.", "And to your Lord direct [your] longing.", "فَارْغَب: تضرّع وتوجّه برجائك")
        ),
        97 to listOf(
            RawVerse(1, 6126, "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ", "إنا أنزلنا القرآن الكريم في ليلة الشرف والفضل العظيم.", "Indeed, We sent the Qur'an down during the Night of Decree.", "الْقَدْرِ: الشرف والعظمة والتقدير"),
            RawVerse(2, 6127, "وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ", "وما أعلمك يا محمد بعظمة هذه الليلة وفضلها الجليل؟", "And what can make you know what is the Night of Decree?", ""),
            RawVerse(3, 6128, "لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ", "العمل الصالح فيها أفضل من عبادة ألف شهر ليس فيها ليلة القدر.", "The Night of Decree is better than a thousand months.", ""),
            RawVerse(4, 6129, "تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِم مِّن كُلِّ أَمْرٍ", "ينزل الملائكة وجبريل عليه السلام فيها بكثرة بالخير والبركة بأمر ربهم.", "The angels and the Spirit descend therein by permission of their Lord for every matter.", "الرُّوحُ: جبريل عليه السلام"),
            RawVerse(5, 6130, "سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ", "هي أمن وسلام وخير خالص حتى طلوع الفجر.", "Peace it is until the emergence of dawn.", "")
        ),
        103 to listOf(
            RawVerse(1, 6177, "وَالْعَصْرِ", "أقسم الله بالدهر والزمن لما فيه من العبر وتقلبات الأحوال.", "By time,", "الْعَصْرِ: الدهر والزمن"),
            RawVerse(2, 6178, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "إن جنس الإنسان لفي هلاك ونقصان وخسارة.", "Indeed, mankind is in loss,", "خُسْرٍ: نقصان وهلاك"),
            RawVerse(3, 6179, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ", "إلا المؤمنين العاملين للصالحات الذين يوصي بعضهم بعضاً بالحق والصبر على طاعة الله.", "Except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience.", "تَوَاصَوْا: أوصى بعضهم بعضاً")
        ),
        108 to listOf(
            RawVerse(1, 6205, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "إنا أعطيناك يا محمد الخير الكثير ومنه نهر الكوثر في الجنة.", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "الْكَوْثَرَ: الخير الكثير ونهر في الجنة"),
            RawVerse(2, 6206, "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "فأخلص صلاتك كلها لربك واذبح نسكك له وحده شكراً لنعمته.", "So pray to your Lord and sacrifice [to Him alone].", "انْحَرْ: اذبح نسكك وأضحيتك لله"),
            RawVerse(3, 6207, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "إن مبغضك هو المنقطع ذكره وخيره في الدنيا والآخرة.", "Indeed, your enemy is the one cut off.", "شَانِئَكَ: مبغضك · الْأَبْتَرُ: المنقطع من كل خير")
        ),
        109 to listOf(
            RawVerse(1, 6208, "قُلْ يَا أَيُّهَا الْكَافِرُونَ", "قل يا محمد للذين كفروا بالله ورسوله.", "Say, \"O disbelievers,", ""),
            RawVerse(2, 6209, "لَا أَعْبُدُ مَا تَعْبُدُونَ", "لا أعبد الأصنام والآلهة الباطلة التي تعبدونها.", "I do not worship what you worship.", ""),
            RawVerse(3, 6210, "وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ", "ولا أنتم عابدون الله وحده الذي أعبده.", "Nor are you worshippers of what I worship.", ""),
            RawVerse(4, 6211, "وَلَا أَنَا عَابِدٌ مَّا عَبَدتُّمْ", "ولن أكون عابداً يوماً ما عبدتم من دون الله.", "Nor will I be a worshipper of what you worship.", ""),
            RawVerse(5, 6212, "وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ", "ولا أنتم عابدون معبودي ما دمتم على شرككم.", "Nor will you be worshippers of what I worship.", ""),
            RawVerse(6, 6213, "لَكُمْ دِينُكُمْ وَلِيَ دِينِ", "لكم دينكم الذي اخترتموه، ولي ديني الذي لا أبغي غيره وهو الإسلام.", "For you is your religion, and for me is my religion.\"", "")
        ),
        110 to listOf(
            RawVerse(1, 6214, "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ", "إذا تم لك يا محمد نصر الله على أعدائك وفتح مكة المكرمة.", "When the victory of Allah has come and the conquest,", "الْفَتْحُ: فتح مكة المكرمة"),
            RawVerse(2, 6215, "وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا", "ورأيت الناس يقبلون على الإسلام جماعات كثيرة.", "And you see the people entering into the religion of Allah in multitudes,", "أَفْوَاجًا: جماعات كثيرة"),
            RawVerse(3, 6216, "فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْفِرْهُ ۚ إِنَّهُ كَانَ تَوَّابًا", "فنزّه ربك حامداً له شاكراً على نعمته، واطلب مغفرته فإنه كثير التوبة على عباده.", "Then exalt [Him] with praise of your Lord and ask forgiveness of Him. Indeed, He is ever Accepting of repentance.", "تَوَّابًا: كثير القبول لتوبة عباده")
        ),
        111 to listOf(
            RawVerse(1, 6217, "تَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ", "خسرت وهلكت يدا أبي لهب وخسر هو بسعيه في أذى النبي ﷺ.", "May the hands of Abu Lahab be ruined, and ruined is he.", "تَبَّتْ: خسرت وهلكت"),
            RawVerse(2, 6218, "مَا أَغْنَىٰ عَنْهُ مَالُهُ وَمَا كَسَبَ", "ما دفع عنه ماله وولده شيئاً من عذاب الله.", "His wealth will not avail him or that which he gained.", ""),
            RawVerse(3, 6219, "سَيَصْلَىٰ نَارًا ذَاتَ لَهَبٍ", "سيدخل ناراً متأججة ذات لهب شديد.", "He will [enter to] burn in a Fire of [blazing] flame", ""),
            RawVerse(4, 6220, "وَامْرَأَتُهُ حَمَّالَةَ الْحَطَبِ", "وامرأته التي كانت تحمل الشوك لتؤذي به طريق النبي ﷺ.", "And his wife [as well] — the carrier of firewood.", ""),
            RawVerse(5, 6221, "فِي جِيدِهَا حَبْلٌ مِّن مَّسَدٍ", "في عنقها حبل محكم الفتل من ليف خشن.", "Around her neck is a rope of [twisted] fiber.", "جِيدِهَا: عنقها · مَّسَدٍ: ليف خشن مفتول")
        ),
        112 to listOf(
            RawVerse(1, 6222, "قُلْ هُوَ اللَّهُ أَحَدٌ", "قل يا محمد: هو الله المتفرد بالألوهية والربوبية والأسماء الحسنى، لا شريك له.", "Say, \"He is Allah, [who is] One,", "أَحَدٌ: المتفرد بالكمال لا شريك له"),
            RawVerse(2, 6223, "اللَّهُ الصَّمَدُ", "الله المقصود وحده في قضاء الحوائج، الكامل في صفاته الغني عن كل ما سواه.", "Allah, the Eternal Refuge.", "الصَّمَدُ: السيد الكامل الذي تقصده الخلائق في حوائجها"),
            RawVerse(3, 6224, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "ليس له ولد ولا والد، منزه عن المجانسة والمماثلة.", "He neither begets nor is born,", ""),
            RawVerse(4, 6225, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "وليس له مثيل ولا نظير ولا مكافئ من خلقه.", "Nor is there to Him any equivalent.\"", "كُفُوًا: مثيلاً ونظيراً")
        ),
        113 to listOf(
            RawVerse(1, 6226, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "قل يا محمد: ألتجئ وأعتصم برب الصبح الذي يفلق ظلام الليل بنوره.", "Say, \"I seek refuge in the Lord of daybreak", "الْفَلَقِ: الصبح"),
            RawVerse(2, 6227, "مِن شَرِّ مَا خَلَقَ", "من أذى جميع المخلوقات وشرها.", "From the evil of that which He created", ""),
            RawVerse(3, 6228, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "ومن شر الليل الشديد الظلمة إذا أقبل وانتشر ما فيه من المؤذيات.", "And from the evil of darkness when it settles", "غَاسِقٍ: الليل المظلم · وَقَبَ: دخل واشتد ظلامه"),
            RawVerse(4, 6229, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "ومن شر السواحر اللاتي ينفثن في عقد الخيوط لإلحاق الأذى.", "And from the evil of the blowers in knots", "النَّفَّاثَاتِ: السواحر النافثات في العقد"),
            RawVerse(5, 6230, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "ومن شر كل حاسد يتمنى زوال النعمة إذا أظهر حسده وعمل بمقتضاه.", "And from the evil of an envier when he envies.\"", "")
        ),
        114 to listOf(
            RawVerse(1, 6231, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "قل: ألتجئ وأتحصن برب الناس وخالقهم ومدبر شؤونهم.", "Say, \"I seek refuge in the Lord of mankind,", ""),
            RawVerse(2, 6232, "مَلِكِ النَّاسِ", "المتصرف في شؤونهم الغني عنهم.", "The Sovereign of mankind.", ""),
            RawVerse(3, 6233, "إِلَٰهِ النَّاسِ", "المعبود بحق لا معبود سواهُ.", "The God of mankind,", ""),
            RawVerse(4, 6234, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "من أذى الشيطان الذي يلقي الوسوسة في الصدور ويخنس متراجعاً عند ذكر الله.", "From the evil of the retreating whisperer —", "الْخَنَّاسِ: المتراجع المختفي عند ذكر الله"),
            RawVerse(5, 6235, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "الذي يبث الشك والشر في قلوب البشر.", "Who whispers [evil] into the breasts of mankind —", ""),
            RawVerse(6, 6236, "مِنَ الْجِنَّةِ وَالنَّاسِ", "سواء أكان من شياطين الجن أم من شياطين الإنس.", "From among the jinn and mankind.\"", "")
        )
    )
}
