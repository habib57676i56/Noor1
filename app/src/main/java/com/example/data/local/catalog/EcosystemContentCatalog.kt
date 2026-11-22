package com.example.data.local.catalog

import com.example.data.local.db.CustomQuizQuestionEntity
import com.example.data.local.db.LibraryBookEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.security.LocalAdminSecurityManager

data class TajweedRuleLesson(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val categoryAr: String,
    val summaryAr: String,
    val summaryEn: String,
    val lettersAr: String,
    val quranExampleUthmani: String,
    val exampleSurah: Int,
    val exampleAyah: Int,
    val exampleRefAr: String,
    val practiceTipAr: String,
    val quizQuestionAr: String,
    val quizOptionsAr: List<String>,
    val quizCorrectIndex: Int
)

data class KidsStoryItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val moralAr: String,
    val bodyAr: String,
    val sourceRef: String
)

data class KidsDuaItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val duaAr: String,
    val occasionAr: String,
    val sourceRef: String
)

data class KidsPillarLesson(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val itemsAr: List<String>
)

data class VerifiedDuaEntry(
    val id: String,
    val category: String, // QURANIC, PROPHETIC, PROTECTION, FORGIVENESS, TRAVEL, KNOWLEDGE
    val titleAr: String,
    val titleEn: String,
    val textAr: String,
    val translationEn: String,
    val sourceRef: String
)

data class QuranRadioStation(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val streamUrl: String
)

/**
 * Curated, verified, offline-first Islamic knowledge, library, Tajweed, Kids, Duaa, Quiz, and Radio catalog.
 * Strict accuracy rule:
 * - Only authentic Quranic references and established Sahih/Hasan Hadith & scholarly summaries.
 * - Never fabricates Quranic verses or unverified Hadith.
 */
object EcosystemContentCatalog {

    val tajweedRules: List<TajweedRuleLesson> = listOf(
        TajweedRuleLesson(
            id = "tj_izhar",
            titleAr = "الإظهار الحلقي",
            titleEn = "Al-Izhar Al-Halqi (Clear Pronunciation)",
            categoryAr = "أحكام النون الساكنة والتنوين",
            summaryAr = "إخراج النون الساكنة أو التنوين من مخرجها بوضوح تام من غير غنة زائدة إذا جاء بعدها أحد حروف الحلق الستة.",
            summaryEn = "Pronouncing Noon Sakinah or Tanween clearly without extra nasalization when followed by one of the six throat letters.",
            lettersAr = "ء ، هـ ، ع ، ح ، غ ، خ",
            quranExampleUthmani = "مِنْ خَوْفٍ — صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ",
            exampleSurah = 1,
            exampleAyah = 7,
            exampleRefAr = "سورة الفاتحة: آية ٧ (أَنْعَمْتَ)",
            practiceTipAr = "انتبه لإلصاق طرف اللسان باللثة العليا لحظة النطق بالنون ثم الانتقال فوراً لحرف العين بلا سكت ولا تشديد.",
            quizQuestionAr = "ما الحكم التجويدي في كلمة (أَنْعَمْتَ) في سورة الفاتحة؟",
            quizOptionsAr = listOf("الإظهار الحلقي", "الإدغام بغنة", "الإقلاب", "الإخفاء الحقيقي"),
            quizCorrectIndex = 0
        ),
        TajweedRuleLesson(
            id = "tj_idgham",
            titleAr = "الإدغام (بغنة وبغير غنة)",
            titleEn = "Al-Idgham (Merging)",
            categoryAr = "أحكام النون الساكنة والتنوين",
            summaryAr = "إدخال النون الساكنة أو التنوين في الحرف الذي يليها من حروف (يرملون) بحيث يصيران حرفاً واحداً مشدداً. ينقسم إلى إدغام بغنة (ينمو) وإدغام بغير غنة (ل، ر).",
            summaryEn = "Merging Noon Sakinah or Tanween into one of the letters (ي ر م ل و ن): with Ghunnah for (ي ن م و) and without Ghunnah for (ل ر).",
            lettersAr = "ي ، ر ، م ، ل ، و ، ن (يرملون)",
            quranExampleUthmani = "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ — فِي جِيدِهَا حَبْلٌ مِّن مَّسَدٍ",
            exampleSurah = 111,
            exampleAyah = 5,
            exampleRefAr = "سورة المسد: آية ٥ (حَبْلٌ مِّن مَّسَدٍ)",
            practiceTipAr = "في (مِّن مَّسَدٍ) أدمج النون في الميم مع غنة بمقدار حركتين، وفي (يَكُن لَّهُ) أدمجها في اللام بغير غنة.",
            quizQuestionAr = "أيّ من الحروف التالية يُعدّ من حروف الإدغام بغير غنة؟",
            quizOptionsAr = listOf("اللام والراء (ل، ر)", "الميم والنون (م، ن)", "الياء والواو (ي، و)", "الباء (ب)"),
            quizCorrectIndex = 0
        ),
        TajweedRuleLesson(
            id = "tj_iqlab",
            titleAr = "الإقلاب",
            titleEn = "Al-Iqlab (Conversion)",
            categoryAr = "أحكام النون الساكنة والتنوين",
            summaryAr = "قلب النون الساكنة أو التنوين ميماً مخفاة مع الغنة بمقدار حركتين إذا جاء بعدها حرف الباء فقط.",
            summaryEn = "Converting Noon Sakinah or Tanween into a concealed Meem with a 2-count Ghunnah when followed by Baa (ب).",
            lettersAr = "ب (حرف واحد فقط)",
            quranExampleUthmani = "مِن بَعْدِ — كَلَّا لَيُنبَذَنَّ فِي الْحُطَمَةِ",
            exampleSurah = 104,
            exampleAyah = 4,
            exampleRefAr = "سورة الهمزة: آية ٤ (لَيُنبَذَنَّ)",
            practiceTipAr = "تُطبق الشفتان برفق دون كزّ شديد عند قلب النون ميماً مع إخراج الغنة من الخيشوم.",
            quizQuestionAr = "ما هو الحرف الوحيد الذي يقع بعد النون الساكنة فيُحدث حكم الإقلاب؟",
            quizOptionsAr = listOf("حرف الميم", "حرف الباء", "حرف الفاء", "حرف القاف"),
            quizCorrectIndex = 1
        ),
        TajweedRuleLesson(
            id = "tj_ikhfa",
            titleAr = "الإخفاء الحقيقي",
            titleEn = "Al-Ikhfa Al-Haqiqi (Concealment)",
            categoryAr = "أحكام النون الساكنة والتنوين",
            summaryAr = "النطق بالنون الساكنة أو التنوين بحالة متوسطة بين الإظهار والإدغام مع بقاء الغنة بمقدار حركتين عند الحروف الخمسة عشر المتبقية.",
            summaryEn = "Pronouncing Noon Sakinah or Tanween in a state between clearness and merging with 2-count Ghunnah before the remaining 15 letters.",
            lettersAr = "ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ",
            quranExampleUthmani = "مِن شَرِّ مَا خَلَقَ — إِنَّ الْإِنسَانَ لَفِي خُسْرٍ",
            exampleSurah = 113,
            exampleAyah = 2,
            exampleRefAr = "سورة الفلق: آية ٢ (مِن شَرِّ)",
            practiceTipAr = "هيّئ فمك لمخرج الحرف التالي (مثل الشين) أثناء إخراج الغنة؛ وتُفخّم الغنة إن كان الحرف التالي مفخماً.",
            quizQuestionAr = "ما الحكم التجويدي في قوله تعالى (مِن شَرِّ مَا خَلَقَ)؟",
            quizOptionsAr = listOf("الإخفاء الحقيقي", "الإظهار الحلقي", "القلقلة", "الإقلاب"),
            quizCorrectIndex = 0
        ),
        TajweedRuleLesson(
            id = "tj_meem_sakinah",
            titleAr = "أحكام الميم الساكنة",
            titleEn = "Rules of Meem Sakinah",
            categoryAr = "أحكام الميم الساكنة",
            summaryAr = "للميم الساكنة ثلاثة أحكام: الإخفاء الشفوي عند الباء، والإدغام الشفوي (المثلين الصغير) عند الميم، والإظهار الشفوي عند باقي الحروف.",
            summaryEn = "Meem Sakinah has three rules: Ikhfa Shafawi before Baa, Idgham Shafawi before Meem, and Izhar Shafawi before all other letters.",
            lettersAr = "ب (إخفاء شفوي) · م (إدغام شفوي) · باقي الحروف (إظهار شفوي)",
            quranExampleUthmani = "لَكُمْ دِينُكُمْ وَلِيَ دِينِ — عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ",
            exampleSurah = 109,
            exampleAyah = 6,
            exampleRefAr = "سورة الكافرون: آية ٦ (لَكُمْ دِينُكُمْ)",
            practiceTipAr = "احذر من إخفاء الميم عند الواو والفاء؛ أظهرها بوضوح (عَلَيْهِمْ وَلَا).",
            quizQuestionAr = "إذا جاء بعد الميم الساكنة حرف ميم متحركة فما الحكم؟",
            quizOptionsAr = listOf("إظهار شفوي", "إدغام مثلين صغير (شفوي) بغنة", "إقلاب", "مد طبيعي"),
            quizCorrectIndex = 1
        ),
        TajweedRuleLesson(
            id = "tj_qalqalah",
            titleAr = "القلقلة",
            titleEn = "Al-Qalqalah (Echoing Bounce)",
            categoryAr = "صفات الحروف",
            summaryAr = "اضطراب المخرج عند النطق بأحد حروف القلقلة ساكناً حتى يُسمع له نبرة قوية واضحة. وهي صغرى وسط الكلمة وكبرى عند الوقف.",
            summaryEn = "A distinct echoing bounce when pronouncing the five Qalqalah letters in a vowelless state (Sakin). Stronger when stopping at the end of a verse.",
            lettersAr = "ق ، ط ، ب ، ج ، د (قطب جد)",
            quranExampleUthmani = "قُلْ هُوَ اللَّهُ أَحَدٌ — لَمْ يَلِدْ وَلَمْ يُولَدْ",
            exampleSurah = 112,
            exampleAyah = 3,
            exampleRefAr = "سورة الإخلاص: آية ٣ (لَمْ يَلِدْ وَلَمْ يُولَدْ)",
            practiceTipAr = "لا تُمل القلقلة نحو الفتح أو الكسر أو الضم، بل أخرجها صافية من مخرج الحرف نفسه.",
            quizQuestionAr = "ما هي الحروف المجموعة في عبارة القلقلة؟",
            quizOptionsAr = listOf("يرملون", "قطب جد", "خص ضغط قظ", "أجد قط بكت"),
            quizCorrectIndex = 1
        ),
        TajweedRuleLesson(
            id = "tj_madd",
            titleAr = "أحكام المد (الطبيعي والفرعي)",
            titleEn = "Rules of Al-Madd (Elongation)",
            categoryAr = "أحكام المدود",
            summaryAr = "المد الطبيعي يمدّ حركتين (ا، و، ي) إذا لم يأتِ بعده همز أو سكون. أما المد الفرعي فيزيد على حركتين بسبب الهمز (متصل/منفصل) أو السكون (لازم/عارض للسكون).",
            summaryEn = "Natural Madd is held for 2 counts. Secondary Madd extends to 4–6 counts due to Hamzah or Sukoon (e.g., Madd Lazim in Ad-Daaalleen).",
            lettersAr = "الألف الساكنة المفتوح ما قبلها · الواو المضموم ما قبلها · الياء المكسور ما قبلها",
            quranExampleUthmani = "وَلَا الضَّالِّينَ — إِذَا جَاءَ نَصْرُ اللَّهِ",
            exampleSurah = 1,
            exampleAyah = 7,
            exampleRefAr = "سورة الفاتحة: آية ٧ (الضَّالِّينَ — مد لازم ٦ حركات)",
            practiceTipAr = "احرص على تساوي مقادير المدود المتماثلة في القراءة الواحدة، ومدّ (الضَّالِّينَ) ست حركات كاملة.",
            quizQuestionAr = "كم حركة يُمدّ المد اللازم الكلمي المثقل في كلمة (الضَّالِّينَ)؟",
            quizOptionsAr = listOf("حركتان فقط", "ست حركات لزوماً", "ثلاث حركات", "لا مد فيها"),
            quizCorrectIndex = 1
        ),
        TajweedRuleLesson(
            id = "tj_ghunnah_tafkheem",
            titleAr = "الغنة والتفخيم والترقيق",
            titleEn = "Ghunnah, Tafkheem & Tarqeeq",
            categoryAr = "صفات ومخارج الحروف",
            summaryAr = "الغنة صوت رخيم يخرج من الخيشوم ويبلغ أكمل مراتبه في النون والميم المشددتين (إِنَّ، ثُمَّ). وحروف الاستعلاء السبعة (خص ضغط قظ) تُفخّم دائماً، وتُفخّم الراء المفتوحة والمضمومة وتُرقق المكسورة.",
            summaryEn = "Ghunnah is a 2-count nasal resonance strongest in doubled Noon/Meem (نّ، مّ). Heavy letters (خص ضغط قظ) are always Tafkheem.",
            lettersAr = "الغنة: نّ، مّ · التفخيم: خ ص ض غ ط ق ظ",
            quranExampleUthmani = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ — قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            exampleSurah = 114,
            exampleAyah = 1,
            exampleRefAr = "سورة الناس: آية ١ (النَّاسِ — نون مشددة بغنة حركتين)",
            practiceTipAr = "أعطِ النون المشددة في (إِنَّا) و(النَّاسِ) زمن غنة واضح بمقدار حركتين قبل الانتقال للحرف التالي.",
            quizQuestionAr = "أيّ من الكلمات التالية تحتوي على نون مشددة تجب فيها الغنة بمقدار حركتين؟",
            quizOptionsAr = listOf("النَّاسِ", "الْفَلَقِ", "أَحَدٌ", "الصَّمَدُ"),
            quizCorrectIndex = 0
        )
    )

    val kidsStories: List<KidsStoryItem> = listOf(
        KidsStoryItem(
            id = "kids_story_nuh",
            titleAr = "سفينة نبي الله نوح عليه السلام",
            titleEn = "The Ark of Prophet Nuh (AS)",
            moralAr = "الصبر والطاعة لله والثقة بنصره رغم سخرية الآخرين.",
            bodyAr = "دعا نبي الله نوح عليه السلام قومه إلى عبادة الله وحده سنين طويلة برفق وحكمة. فأمره الله تعالى أن يصنع سفينة عظيمة. كان نوح يعمل بإتقان ويذكر الله، ولما جاء أمر الله ركب فيها المؤمنون وزوجان من كل مخلوق، فأنجاهم الله برحمته. نتعلم من هذه القصة أن الصبر وطاعة الله هما طريق النجاة والفوز.",
            sourceRef = "مقتبس من قصص القرآن (سورة هود وسورة نوح)"
        ),
        KidsStoryItem(
            id = "kids_story_ibrahim",
            titleAr = "إبراهيم عليه السلام والكعبة المشرفة",
            titleEn = "Prophet Ibrahim (AS) and the Kaaba",
            moralAr = "التوكل على الله وبر الوالدين والمحافظة على الصلاة.",
            bodyAr = "بنى نبي الله إبراهيم عليه السلام وابنه إسماعيل عليه السلام قواعد البيت الحرام (الكعبة المشرفة) في مكة المكرمة وهما يدعوان الله بقلب خاشع: «رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ». فجعله الله قبلة المسلمين في كل بقاع الأرض.",
            sourceRef = "مقتبس من سورة البقرة: آية ١٢٧"
        ),
        KidsStoryItem(
            id = "kids_story_honesty",
            titleAr = "الصادق الأمين محمد ﷺ",
            titleEn = "The Truthful & Trustworthy Prophet (PBUH)",
            moralAr = "الصدق والأمانة يجلبان محبة الله ومحبة الناس.",
            bodyAr = "عُرف نبينا محمد ﷺ منذ صغره بالصدق والأمانة حتى لقّبه أهل مكة بـ«الصادق الأمين»، فكانوا يحفظون عنده أماناتهم ويثقون بكلامه. وقد علّمنا ﷺ أن الصدق يهدي إلى البر وأن البر يهدي إلى الجنة.",
            sourceRef = "صحيح البخاري (٦٠٩٤) والسيرة النبوية"
        ),
        KidsStoryItem(
            id = "kids_story_abu_bakr",
            titleAr = "كرم الصحابي الجليل أبي بكر الصديق رضي الله عنه",
            titleEn = "Generosity of Abu Bakr As-Siddiq (RA)",
            moralAr = "المسابقة في الخير ومساعدة المحتاجين لوجه الله.",
            bodyAr = "كان أبو بكر الصديق رضي الله عنه أسرع الصحابة إلى الخير؛ يطعم الفقراء، ويزور المرضى، ويتصدق في سبيل الله بسخاء وحب. وعلّمنا أن المسلم يفرح حين يسعد الآخرين ويشاركهم ما لديه.",
            sourceRef = "صحيح مسلم (١٠٢٨) وسير الصحابة"
        )
    )

    val kidsDuas: List<KidsDuaItem> = listOf(
        KidsDuaItem("kdua_eat", "دعاء قبل الطعام", "Before Eating", "بِسْمِ اللَّهِ", "نقوله قبل أن نبدأ الأكل لتنزل البركة.", "صحيح البخاري (٥٣٧٦)"),
        KidsDuaItem("kdua_after_eat", "دعاء بعد الطعام", "After Eating", "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "نشكر الله تعالى على نعمة الطعام.", "سنن الترمذي (٣٤٥٨)"),
        KidsDuaItem("kdua_sleep", "دعاء النوم", "Before Sleeping", "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "نقوله عند النوم لنكون في حفظ الله.", "صحيح البخاري (٦٣٢٤)"),
        KidsDuaItem("kdua_wake", "دعاء الاستيقاظ", "Waking Up", "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "نبدأ يومنا بحمد الله وشكره.", "صحيح البخاري (٦٣١٢)"),
        KidsDuaItem("kdua_parents", "دعاء للوالدين", "For Parents", "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "ندعو لأمنا وأبينا كل يوم.", "سورة الإسراء: آية ٢٤"),
        KidsDuaItem("kdua_knowledge", "دعاء طلب العلم", "Seeking Knowledge", "رَّبِّ زِدْنِي عِلْمًا", "نقوله قبل المذاكرة وحفظ القرآن.", "سورة طه: آية ١١٤")
    )

    val kidsPillars: List<KidsPillarLesson> = listOf(
        KidsPillarLesson(
            id = "pillars_islam",
            titleAr = "أركان الإسلام الخمسة",
            titleEn = "The Five Pillars of Islam",
            itemsAr = listOf(
                "١. الشهادتان: أشهد أن لا إله إلا الله وأشهد أن محمداً رسول الله.",
                "٢. إقام الصلاة: خمس صلوات في اليوم والليلة.",
                "٣. إيتاء الزكاة: مساعدة الفقراء والمحتاجين.",
                "٤. صوم شهر رمضان المبارك.",
                "٥. حج بيت الله الحرام لمن استطاع إليه سبيلاً."
            )
        ),
        KidsPillarLesson(
            id = "pillars_iman",
            titleAr = "أركان الإيمان الستة",
            titleEn = "The Six Articles of Faith",
            itemsAr = listOf(
                "١. الإيمان بالله تعالى وحده.",
                "٢. الإيمان بالملائكة الكرام.",
                "٣. الإيمان بالكتب السماوية وآخرها القرآن الكريم.",
                "٤. الإيمان بجميع الرسل والأنبياء عليهم السلام.",
                "٥. الإيمان باليوم الآخر.",
                "٦. الإيمان بالقدر خيره وشره."
            )
        )
    )

    val verifiedDuas: List<VerifiedDuaEntry> = listOf(
        VerifiedDuaEntry("duaa_1", "QURANIC", "دعاء خير الدنيا والآخرة", "Good in This World & Hereafter", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Our Lord, give us in this world [that which is] good and in the Hereafter [that which is] good and protect us from the punishment of the Fire.", "سورة البقرة: آية ٢٠١"),
        VerifiedDuaEntry("duaa_2", "QURANIC", "دعاء الثبات على الهداية", "Steadfastness Upon Guidance", "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ", "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy.", "سورة آل عمران: آية ٨"),
        VerifiedDuaEntry("duaa_3", "QURANIC", "دعاء شرح الصدر وتيسير الأمر", "Ease & Clarity", "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي يَفْقَهُوا قَوْلِي", "My Lord, expand for me my breast and ease for me my task.", "سورة طه: آية ٢٥-٢٨"),
        VerifiedDuaEntry("duaa_4", "PROPHETIC", "دعاء الهم والحزن", "Relief from Anxiety & Grief", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ", "O Allah, I seek refuge in You from worry and grief, helplessness and laziness, miserliness and cowardice, debt and overpowering of men.", "صحيح البخاري (٦٣٦٩)"),
        VerifiedDuaEntry("duaa_5", "FORGIVENESS", "سيد الاستغفار", "Master Supplication for Forgiveness", "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ", "O Allah, You are my Lord, none has the right to be worshipped except You...", "صحيح البخاري (٦٣٠٦)"),
        VerifiedDuaEntry("duaa_6", "KNOWLEDGE", "دعاء العلم النافع والرزق الطيب", "Beneficial Knowledge & Provision", "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا", "O Allah, I ask You for beneficial knowledge, goodly provision, and acceptable deeds.", "سنن ابن ماجه (٩٢٥) — صحيح"),
        VerifiedDuaEntry("duaa_7", "PROTECTION", "دعاء حفظ النعم والعافية", "Well-Being & Protection", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ", "O Allah, I ask You for pardon and well-being in this world and the Hereafter.", "سنن أبي داود (٥٠٧٤) — صحيح"),
        VerifiedDuaEntry("duaa_8", "TRAVEL", "دعاء السفر", "Supplication of Travel", "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى", "Glory is to Him who has subjected this to us, and we could never have it by our efforts...", "صحيح مسلم (١٣٤٢)")
    )

    val quranRadioStations: List<QuranRadioStation> = listOf(
        QuranRadioStation(
            id = "radio_mp3quran_general",
            nameAr = "إذاعة القرآن الكريم — تلاوات خاشعة",
            nameEn = "Holy Quran Radio — Live Recitations",
            descriptionAr = "بث مباشر على مدار الساعة لتلاوات نخبة من كبار قراء العالم الإسلامي.",
            streamUrl = "https://backup.qurango.net/radio/tarateel"
        ),
        QuranRadioStation(
            id = "radio_maher",
            nameAr = "إذاعة الشيخ ماهر المعيقلي",
            nameEn = "Sheikh Maher Al-Muaiqly Radio",
            descriptionAr = "تلاوات متواصلة بصوت الشيخ ماهر المعيقلي إمام المسجد الحرام.",
            streamUrl = "https://backup.qurango.net/radio/maher"
        ),
        QuranRadioStation(
            id = "radio_yasser",
            nameAr = "إذاعة الشيخ ياسر الدوسري",
            nameEn = "Sheikh Yasser Al-Dosari Radio",
            descriptionAr = "تلاوات خاشعة متواصلة بصوت الشيخ ياسر الدوسري.",
            streamUrl = "https://backup.qurango.net/radio/yasser_aldosari"
        ),
        QuranRadioStation(
            id = "radio_minshawi",
            nameAr = "إذاعة الشيخ محمد صديق المنشاوي",
            nameEn = "Sheikh Al-Minshawi Radio",
            descriptionAr = "المصحف المرتل بصوت القارئ الشيخ محمد صديق المنشاوي رحمه الله.",
            streamUrl = "https://backup.qurango.net/radio/mohammed_siddiq_alminshawi"
        ),
        QuranRadioStation(
            id = "radio_husary",
            nameAr = "إذاعة الشيخ محمود خليل الحصري — المصحف المعلم",
            nameEn = "Sheikh Al-Husary — Teaching Mushaf Radio",
            descriptionAr = "مثالية للحفظ وضبط أحكام التجويد ومخارج الحروف.",
            streamUrl = "https://backup.qurango.net/radio/mahmoud_khalil_alhussary"
        ),
        QuranRadioStation(
            id = "radio_tafsir",
            nameAr = "إذاعة التفسير المختصر للقرآن الكريم",
            nameEn = "Quran Tafseer Radio",
            descriptionAr = "بث متواصل لتفسير آيات القرآن الكريم وتدبر معانيها.",
            streamUrl = "https://backup.qurango.net/radio/tafseer"
        )
    )

    fun seedAiKnowledge(): List<LocalAiKnowledgeEntity> {
        val raw = listOf(
            LocalAiKnowledgeEntity(
                id = "ai_seed_1",
                category = "QURAN",
                titleAr = "فضل قراءة القرآن وتدبره",
                titleEn = "Virtue of Reciting the Holy Quran",
                questionAr = "ما فضل قراءة القرآن الكريم وحفظه؟",
                questionEn = "What is the virtue of reciting and memorizing the Quran?",
                answerAr = "لقراءة القرآن أجر عظيم؛ فبكل حرف حسنة والحسنة بعشر أمثالها كما صح عن النبي ﷺ: «مَن قَرَأَ حَرْفًا مِنْ كِتَابِ اللَّهِ فَلَهُ بِهِ حَسَنَةٌ، وَالْحَسَنَةُ بِعَشْرِ أَمْثَالِهَا». وقال ﷺ: «خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ»، ويشفع القرآن لأصحابه يوم القيامة.",
                answerEn = "Every letter recited from the Book of Allah earns a reward multiplied tenfold, and the Prophet (PBUH) said: 'The best among you are those who learn the Quran and teach it.'",
                sourceRef = "صحيح البخاري (٥٠٢٧) · سنن الترمذي (٢٩١٠) · صحيح مسلم (٨٠٤)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "متفق على فضله عند جميع أهل العلم؛ ويُستحب الجمع بين التلاوة والتدبر والعمل."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_2",
                category = "WORSHIP",
                titleAr = "أركان الصلاة وشروطها",
                titleEn = "Pillars and Conditions of Salah",
                questionAr = "ما هي شروط الصلاة وأركانها الأساسية؟",
                questionEn = "What are the main conditions and pillars of prayer?",
                answerAr = "من شروط صحة الصلاة: الطهارة من الحدث والنجاسة، ودخول الوقت، وستر العورة، واستقبال القبلة، والنية. ومن أركانها الأساسية: تكبيرة الإحرام، والقيام مع القدرة، وقراءة الفاتحة، والركوع والرفع منه، والسجود والجلوس بين السجدتين، والطمأنينة في جميع الأركان، والتشهد الأخير والجلوس له، والتسليم.",
                answerEn = "Conditions include purity (Wudu), prayer time entry, covering Awrah, facing Qibla, and intention. Pillars include Takbirat Al-Ihram, standing, reciting Al-Fatihah, Ruku, Sujud, tranquility, final Tashahhud, and Tasleem.",
                sourceRef = "حديث المسيء صلاته — صحيح البخاري (٧٥٧) وصحيح مسلم (٣٩٧)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "توجد فروق فرعية بين المذاهب الأربعة في اعتبار بعض الهيئات ركناً أو واجباً (مثل قراءة الفاتحة للمأموم والتشهد الأول)؛ يُرجع في المسائل التفصيلية للفقهاء المعتمدين."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_3",
                category = "WORSHIP",
                titleAr = "أحكام الصيام وصيام التطوع",
                titleEn = "Fasting & Voluntary Sunnah Fasts",
                questionAr = "ما هي الأيام المستحب صيامها في السنة؟",
                questionEn = "Which voluntary days are recommended for fasting?",
                answerAr = "يُستحب للمسلم الإكثار من صيام التطوع؛ ومن آكده: صيام يومي الإثنين والخميس لأن الأعمال تُعرض فيهما، وصيام الأيام البيض (١٣، ١٤، ١٥ من كل شهر هجري)، وصيام ستة أيام من شوال، وصيام يوم عرفة لغير الحاج، وصيام يوم عاشوراء مع يوم قبله أو بعده.",
                answerEn = "Recommended voluntary fasts include Mondays and Thursdays, the White Days (13th–15th of each Hijri month), six days of Shawwal, the Day of Arafah (for non-pilgrims), and Ashura.",
                sourceRef = "صحيح مسلم (١١٦٢، ١١٦٤) · سنن الترمذي (٧٤٧) · سنن النسائي (٢٤٢٤)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "من كان عليه قضاء من رمضان فيُقدّم قضاء الفريضة، وللعلماء تفصيل في الجمع بين نية القضاء والتطوع يُرجع فيه لأهل الفتوى."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_4",
                category = "HADITH",
                titleAr = "النية والإخلاص في الأعمال",
                titleEn = "Intention and Sincerity in Deeds",
                questionAr = "ما معنى حديث إنما الأعمال بالنيات؟",
                questionEn = "What is the meaning of the Hadith on intentions?",
                answerAr = "عن عمر بن الخطاب رضي الله عنه قال: سمعت رسول الله ﷺ يقول: «إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى». هذا الحديث أصل عظيم من أصول الإسلام، يدل على أن قبول العبادة وثوابها مشروط بإخلاص القصد لله تعالى، وأن النية تميّز العادات عن العبادات.",
                answerEn = "Reported by Umar ibn Al-Khattab (RA): 'Actions are judged by intentions, and every person will have what they intended.' Sincerity for Allah is essential for every act of worship.",
                sourceRef = "متفق عليه: صحيح البخاري (١) وصحيح مسلم (١٩٠٧)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "محل النية القلب باتفاق العلماء."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_5",
                category = "QURAN",
                titleAr = "أفضل طرق حفظ القرآن الكريم ومراجعته",
                titleEn = "Best Methods for Quran Memorization & Revision",
                questionAr = "كيف أحفظ القرآن الكريم بطريقة متقنة دون نسيان؟",
                questionEn = "How can I memorize the Quran effectively without forgetting?",
                answerAr = "يوصي أهل الإقراء بخمس خطوات عملية: (١) تصحيح التلاوة أولاً بسماع قارئ متقن أو العرض على شيخ، (٢) الالتزام بمصحف واحد لثبات الصورة البصرية، (٣) تقسيم الورد اليومي إلى حفظ جديد ومراجعة قريبة ومراجعة بعيدة، (٤) الربط بين الآيات وتكرارها في الصلاة، (٥) المداومة على ورد يومي ثابت ولو قليلاً فـ«أحب الأعمال إلى الله أدومها وإن قل».",
                answerEn = "Key practices: correct pronunciation with a qualified teacher/reciter, stick to one Mushaf print, balance new Hifz with near & far revision, recite in prayer, and stay consistent daily.",
                sourceRef = "صحيح البخاري (٦٤٦٥) · منهجية حلقات تحفيظ القرآن",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "ينبغي عرض المحفوظ على معلم متقن للتأكد من صحة المخارج والصفات."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_6",
                category = "TAJWEED",
                titleAr = "مقدمة في علم التجويد وأحكام النون الساكنة",
                titleEn = "Introduction to Tajweed & Noon Sakinah",
                questionAr = "ما هي أحكام النون الساكنة والتنوين في التجويد؟",
                questionEn = "What are the four rules of Noon Sakinah and Tanween?",
                answerAr = "للنون الساكنة والتنوين أربعة أحكام: (١) الإظهار الحلقي عند الحروف (ء هـ ع ح غ خ)، (٢) الإدغام عند حروف (يرملون) وهو بغنة في (ينمو) وبغير غنة في (ل ر)، (٣) الإقلاب عند حرف الباء (ب) بقلبها ميماً مخفاة بغنة، (٤) الإخفاء الحقيقي عند باقي الحروف الخمسة عشر مع غنة بمقدار حركتين.",
                answerEn = "Noon Sakinah & Tanween have four rules: Izhar (6 throat letters), Idgham (يرملون), Iqlab (ب), and Ikhfa (remaining 15 letters).",
                sourceRef = "تحفة الأطفال للجمزوري والمقدمة الجزرية",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "التجويد العملي يُتلقى بالمشافهة والاستماع إلى القراء المتقنين."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_7",
                category = "AQEEDAH",
                titleAr = "أركان الإسلام والإيمان والإحسان",
                titleEn = "Pillars of Islam, Iman, and Ihsan",
                questionAr = "ما الفرق بين الإسلام والإيمان والإحسان؟",
                questionEn = "What is the difference between Islam, Iman, and Ihsan?",
                answerAr = "بيّن النبي ﷺ في حديث جبريل المشهور أن الإسلام هو الأعمال الظاهرة (الشهادتان، الصلاة، الزكاة، الصوم، الحج)، والإيمان هو الاعتقاد القلبي الراسخ (الإيمان بالله وملائكته وكتبه ورسله واليوم الآخر والقدر خيره وشره)، والإحسان هو أعلى المراتب: «أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ، فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ».",
                answerEn = "As explained in Hadith Jibril: Islam covers the 5 outward pillars, Iman covers the 6 articles of faith, and Ihsan is worshipping Allah as though you see Him, knowing He sees you.",
                sourceRef = "صحيح مسلم (٨) وصحيح البخاري (٥٠)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "حديث جبريل أصل جامع لمراتب الدين الثلاث."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_8",
                category = "FIQH",
                titleAr = "الزكاة ومصارفها الشرعية",
                titleEn = "Zakah and Its Eight Categories",
                questionAr = "لمن تُصرف الزكاة وما شروط وجوبها؟",
                questionEn = "Who is eligible to receive Zakah?",
                answerAr = "تجب الزكاة في المال إذا بلغ النصاب وحال عليه الحول الهجري. وقد حدد الله تعالى مصارفها الثمانية في قوله تعالى في سورة التوبة (آية ٦٠): الفقراء، والمساكين، والعاملين عليها، والمؤلفة قلوبهم، وفي الرقاب، والغارمين، وفي سبيل الله، وابن السبيل.",
                answerEn = "Zakah is obligatory on wealth meeting the Nisab for one lunar year, distributed among the eight Quranic categories in Surah At-Tawbah 9:60.",
                sourceRef = "سورة التوبة: آية ٦٠",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "تختلف تفاصيل حساب زكاة الأسهم والديون والحلي بين المذاهب الفقهية؛ يُنصح بسؤال مفتٍ أو لجنة فتوى معتمدة للحالات المالية الخاصة."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_9",
                category = "SEERAH",
                titleAr = "نبذة عن السيرة النبوية الشريفة",
                titleEn = "Overview of the Prophetic Seerah",
                questionAr = "ما أبرز محطات السيرة النبوية؟",
                questionEn = "What are the major milestones in the Seerah of Prophet Muhammad (PBUH)?",
                answerAr = "وُلد النبي محمد ﷺ في مكة المكرمة عام الفيل، وبُعث بالوحي في غار حراء وعمره أربعون عاماً. دعا في مكة ثلاث عشرة سنة صابراً على الأذى، ثم هاجر إلى المدينة المنورة حيث أسس المسجد النبوي وآخى بين المهاجرين والأنصار، وفتح مكة في السنة الثامنة للهجرة بعفو وتسامح عظيمين، وحج حجة الوداع في السنة العاشرة.",
                answerEn = "Born in Makkah in the Year of the Elephant, received revelation at age 40, preached 13 years in Makkah, migrated to Madinah, established brotherhood, peacefully opened Makkah in 8 AH, and performed the Farewell Pilgrimage in 10 AH.",
                sourceRef = "الرحيق المختوم · السيرة النبوية لابن هشام · صحيح البخاري",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "قراءة السيرة تعين المسلم على فهم أسباب النزول وتطبيق الأخلاق النبوية."
            ),
            LocalAiKnowledgeEntity(
                id = "ai_seed_10",
                category = "WORSHIP",
                titleAr = "سنن يوم الجمعة وفضائلها",
                titleEn = "Sunnahs and Virtues of Friday",
                questionAr = "ما هي السنن المستحبة في يوم الجمعة؟",
                questionEn = "What are the recommended Sunnahs on Friday?",
                answerAr = "يوم الجمعة خير يوم طلعت فيه الشمس؛ ومن سننه: الاغتسال، والتطيب ولبس أحسن الثياب، والتبكير إلى صلاة الجمعة، وقراءة سورة الكهف، والإكثار من الصلاة على النبي ﷺ، وتحري ساعة الاستجابة خاصة في آخر ساعة بعد العصر.",
                answerEn = "Friday Sunnahs include Ghusl, wearing clean garments and fragrance, going early to Jumu'ah, reciting Surah Al-Kahf, abundant Salawat upon the Prophet (PBUH), and making Dua during the hour of acceptance.",
                sourceRef = "صحيح البخاري (٨٨٣) · صحيح مسلم (٨٥٤) · سنن أبي داود (١٠٤٧)",
                verificationStatus = "VERIFIED",
                scholarlyNotes = "الإنصات للخطبة واجب حال خطبة الإمام."
            )
        )
        return raw.map { item ->
            item.copy(contentHash = LocalAdminSecurityManager.sha256Hex("${item.questionAr}|${item.answerAr}"))
        }
    }

    fun seedLibraryBooks(): List<LibraryBookEntity> {
        val books = listOf(
            LibraryBookEntity(
                id = "lib_tafseer_fatiha",
                category = "TAFSEER",
                titleAr = "التفسير الميسر لفاتحة الكتاب وقصار السور",
                titleEn = "Concise Tafseer of Al-Fatihah & Short Surahs",
                authorAr = "مقتطفات من التفسير الميسر وتفسير ابن كثير",
                authorEn = "Excerpts from Al-Muyassar & Ibn Kathir",
                summaryAr = "شرح ميسر وموثق لمعاني سورة الفاتحة وسور الإخلاص والفلق والناس والعصر والضحى.",
                sourceReference = "مجمع الملك فهد — التفسير الميسر",
                contentText = """
سورة الفاتحة (أم الكتاب):
سميت الفاتحة لافتتاح الكتاب العزيز بها، وتُسمى السبع المثاني والقرآن العظيم.
١. ﴿بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ﴾: أبتدئ قراءة القرآن باسم الله مستعيناً به؛ فالله هو المعبود بحق وحده، والرحمن ذو الرحمة الواسعة التي شملت الخلق، والرحيم بالمؤمنين.
٢. ﴿الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ﴾: الثناء الكامل على الله بصفات كماله ونعمه الظاهرة والباطنة، وهو مالك الخلائق ومدبّر شؤونهم.
٣. ﴿مَالِكِ يَوْمِ الدِّينِ﴾: المنفرد بالحكم والجزاء يوم القيامة حين لا يملك أحد لأحد شيئاً.
٤. ﴿إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ﴾: جوهر التوحيد؛ فلا يُعبد إلا الله ولا يُستعان على الطاعة إلا بالله.
٥. ﴿اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ﴾: أعظم دعاء يكرره المسلم في كل ركعة طلباً للثبات على الحق وطريق الأنبياء والصالحين.

سورة الإخلاص:
تعدل ثلث القرآن لأنها خالصة في بيان توحيد الله وصفاته العليا: ﴿قُلْ هُوَ اللَّهُ أَحَدٌ * اللَّهُ الصَّمَدُ * لَمْ يَلِدْ وَلَمْ يُولَدْ * وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ﴾. الصمد هو السيد الكامل في صفاته الذي تقصده الخلائق في كل حوائجها.

سورة العصر:
قال الإمام الشافعي رحمه الله: «لو ما أنزل الله حجة على خلقه إلا هذه السورة لكفتهم». فهي تجمع أسباب النجاة الأربعة: الإيمان الصادق، والعمل الصالح، والتواصي بالحق، والتواصي بالصبر.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_hadith_nawawi",
                category = "HADITH",
                titleAr = "مختارات من الأربعين النووية وشرحها",
                titleEn = "Selections from Imam An-Nawawi's 40 Hadith",
                authorAr = "الإمام يحيى بن شرف النووي رحمه الله",
                authorEn = "Imam Yahya ibn Sharaf An-Nawawi",
                summaryAr = "جوامع كلم النبي ﷺ في النية والدين والنصيحة وحسن الخلق مع شرح ميسر وتخريج صحيح.",
                sourceReference = "صحيح البخاري وصحيح مسلم",
                contentText = """
الحديث الأول — الإخلاص والنية:
عن عمر بن الخطاب رضي الله عنه قال: سمعت رسول الله ﷺ يقول: «إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى». (متفق عليه).
الفائدة: يزن الله الأعمال بصدق القلوب؛ فجدّد نيتك في طلب العلم والقرآن والعمل الصالح لتنال الأجر كاملاً.

الحديث السابع — الدين النصيحة:
عن تميم الداري رضي الله عنه أن النبي ﷺ قال: «الدِّينُ النَّصِيحَةُ». قلنا: لمن؟ قال: «لِلَّهِ وَلِكِتَابِهِ وَلِرَسُولِهِ وَلأَئِمَّةِ الْمُسْلِمِينَ وَعَامَّتِهِمْ». (رواه مسلم).
الفائدة: النصيحة لكتاب الله تكون بتلاوته وحفظه وتدبره والعمل بأحكامه، والنصيحة للناس بإرشادهم للخير برفق وأدب.

الحديث الثالث عشر — محبة الخير للناس:
عن أنس بن مالك رضي الله عنه عن النبي ﷺ قال: «لا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ». (متفق عليه).
الفائدة: سلامة الصدر من الحسد وحب الخير للمسلمين من علامات كمال الإيمان.

الحديث الثامن عشر — التقوى وحسن الخلق:
عن أبي ذر ومعاذ بن جبل رضي الله عنهما عن رسول الله ﷺ قال: «اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ». (رواه الترمذي وحسنه).
الفائدة: يجمع هذا الحديث حقوق الله تعالى في السر والعلن وحقوق العباد بالمعاملة الحسنة.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_fiqh_ibadat",
                category = "FIQH",
                titleAr = "ملخص فقه العبادات الميسر (الطهارة والصلاة والصيام)",
                titleEn = "Concise Fiqh of Worship (Purification, Prayer & Fasting)",
                authorAr = "ملخص تعليمي موثق على أصول الفقه الإسلامي",
                authorEn = "Verified Educational Summary",
                summaryAr = "دليل عملي موجز لأحكام الوضوء والصلاة والصيام مع التنبيه لمواضع السعة الفقهية.",
                sourceReference = "مصادر الفقه الميسر المعتمدة",
                contentText = """
أولاً: الطهارة والوضوء
قال تعالى: ﴿يَا أَيُّهَا الَّذِينَ آمَنُوا إِذَا قُمْتُمْ إِلَى الصَّلَاةِ فَاغْسِلُوا وُجُوهَكُمْ وَأَيْدِيَكُمْ إِلَى الْمَرَافِقِ وَامْسَحُوا بِرُءُوسِكُمْ وَأَرْجُلَكُمْ إِلَى الْكَعْبَيْنِ﴾ (المائدة: ٦).
فروض الوضوء المتفق عليها: غسل الوجه، وغسل اليدين إلى المرفقين، ومسح الرأس، وغسل الرجلين إلى الكعبين. ومن سننه: التسمية، والسواك، وغسل الكفين ثلاثاً، والمضمضة والاستنشاق، وتخليل الأصابع.

ثانياً: الخشوع والطمأنينة في الصلاة
الطمأنينة في الركوع والسجود والرفع منهما ركن لا تصح الصلاة بدونه. ويُستحب إحضار القلب وتدبر معاني الفاتحة والأذكار.

ثالثاً: أحكام الصيام
الصيام هو الإمساك بنية التعبد عن المفطرات من طلوع الفجر الصادق إلى غروب الشمس. ويُستحب تعجيل الفطر وتأخير السحور لقوله ﷺ: «تَسَحَّرُوا فَإِنَّ فِي السَّحُورِ بَرَكَةً» (متفق عليه).
تنبيه علمي: للمسائل الفقهية المستجدة أو الخاصة يُرجع دائماً لأهل العلم والفتوى المعتمدين.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_aqeedah_basics",
                category = "AQEEDAH",
                titleAr = "أصول العقيدة الإسلامية الصافية",
                titleEn = "Fundamentals of Islamic Creed (Aqeedah)",
                authorAr = "ملخص تعليمي موثق",
                authorEn = "Verified Educational Summary",
                summaryAr = "عرض مبسط لأركان الإيمان الستة ومعنى التوحيد وأسماء الله الحسنى.",
                sourceReference = "القرآن الكريم وصحيح السنة",
                contentText = """
أساس العقيدة الإسلامية هو توحيد الله تعالى في ربوبيته وألوهيته وأسمائه وصفاته.
١. الإيمان بالله: الاعتقاد الجازم بأن الله هو الخالق الرازق المدبّر وحده، المستحق للعبادة دون سواه.
٢. الإيمان بالملائكة: خلقهم الله من نور، لا يعصون الله ما أمرهم ويفعلون ما يؤمرون؛ ومنهم جبريل أمين الوحي.
٣. الإيمان بالكتب: ومنها التوراة والإنجيل والزبور، وخاتمها المهيمن عليها القرآن الكريم المحفوظ بحفظ الله.
٤. الإيمان بالرسل: أولهم نوح عليه السلام وخاتمهم محمد ﷺ، جاؤوا جميعاً بدعوة التوحيد والرحمة والعدل.
٥. الإيمان باليوم الآخر: البعث والحساب والميزان والجنة والنار.
٦. الإيمان بالقدر: أن الله علم كل شيء وكتبه وشاءه وخلقه، مع تكليف الإنسان بالأخذ بالأسباب والعمل الصالح.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_seerah_highlights",
                category = "SEERAH",
                titleAr = "دروس وعبر من السيرة النبوية الشريفة",
                titleEn = "Lessons from the Noble Prophetic Seerah",
                authorAr = "مختصر السيرة التعليمية",
                authorEn = "Educational Seerah Summary",
                summaryAr = "محطات مضيئة من حياة النبي ﷺ في مكة والمدينة وأخلاقه في الرحمة والعدل والشورى.",
                sourceReference = "كتب السيرة النبوية المعتمدة",
                contentText = """
١. الرحمة المهداة:
قال تعالى: ﴿وَمَا أَرْسَلْنَاكَ إِلَّا رَحْمَةً لِّلْعَالَمِينَ﴾. تجلّت رحمته ﷺ في معاملته للأطفال والضعفاء والخدم بل وحتى الحيوانات.

٢. بناء المجتمع في المدينة:
أول عمل قام به النبي ﷺ عند وصوله المدينة المنورة كان بناء المسجد النبوي ليكون مركزاً للعبادة والتعليم والتآخي، ثم المؤاخاة بين المهاجرين والأنصار، ثم كتابة وثيقة المدينة لتنظيم التعايش والعدل.

٣. العفو عند المقدرة في فتح مكة:
حين دخل النبي ﷺ مكة منتصراً في السنة الثامنة للهجرة تواضع لله وخفض رأسه شاكراً، وقال لأهلها: «اذهبوا فأنتم الطلقاء»، فدخل الناس في دين الله أفواجاً.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_prophets_stories",
                category = "PROPHETS",
                titleAr = "قبسات من قصص الأنبياء عليهم السلام",
                titleEn = "Highlights from Stories of the Prophets",
                authorAr = "مقتطفات من قصص القرآن الكريم",
                authorEn = "Quranic Prophetic Narratives",
                summaryAr = "مواقف الصبر والشكر والتوكل في قصص يوسف وأيوب وموسى وسليمان عليهم السلام.",
                sourceReference = "القرآن الكريم وتفسير ابن كثير",
                contentText = """
١. يوسف عليه السلام — العفة وحسن الظن بالله:
تدرّج يوسف عليه السلام من البئر إلى السجن إلى خزائن الأرض بفضل صبره وتقواه وعفته، ثم عفا عن إخوته وقال: ﴿لَا تَثْرِيبَ عَلَيْكُمُ الْيَوْمَ يَغْفِرُ اللَّهُ لَكُمْ﴾.

٢. أيوب عليه السلام — الصبر الجميل:
ابتُلي أيوب عليه السلام في ماله وجسده فصبر ورضي ودعا ربه بأدب رفيع: ﴿أَنِّي مَسَّنِيَ الضُّرُّ وَأَنتَ أَرْحَمُ الرَّاحِمِينَ﴾، فكشف الله ضره وأثنى عليه في القرآن: ﴿إِنَّا وَجَدْنَاهُ صَابِرًا نِّعْمَ الْعَبْدُ إِنَّهُ أَوَّابٌ﴾.

٣. سليمان عليه السلام — شكر النعمة:
مع ما آتاه الله من ملك عظيم لم ينشغل عن شكر الله بل قال: ﴿هَٰذَا مِن فَضْلِ رَبِّي لِيَبْلُوَنِي أَأَشْكُرُ أَمْ أَكْفُرُ﴾.
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_companions_stories",
                category = "COMPANIONS",
                titleAr = "صور مشرقة من حياة الصحابة الكرام",
                titleEn = "Radiant Portraits of the Companions",
                authorAr = "مقتطفات من سير أعلام الصحابة",
                authorEn = "Biographies of the Sahabah",
                summaryAr = "مواقف خالدة في الصدق والعدل والإنفاق وحب القرآن من حياة الخلفاء الراشدين والصحابة.",
                sourceReference = "صحيح البخاري ومسلم وسير أعلام النبلاء",
                contentText = """
١. عمر بن الخطاب رضي الله عنه والعدل:
كان يتفقد أحوال الرعية ليلاً بنفسه ليطمئن على الفقراء والأيتام ويحمل الطعام على ظهره للمحتاجين خشية أن يُسأل عنهم يوم القيامة.

٢. عثمان بن عفان رضي الله عنه والإنفاق وجمع المصحف:
جهّز جيش العسرة واشترى بئر رومة للمسلمين، وكان له الفضل العظيم في جمع الأمة على مصحف واحد بالرسم العثماني حفظاً لكتاب الله.

٣. زيد بن ثابت وعبد الله بن مسعود رضي الله عنهما مع القرآن:
كرّس الصحابة حياتهم لحفظ كتاب الله وتعلمه وتعليمه؛ وكان ابن مسعود يقول: «كان الرجل منا إذا تعلم عشر آيات لم يجاوزهن حتى يعرف معانيهن والعمل بهن».
                """.trimIndent()
            ),
            LibraryBookEntity(
                id = "lib_lessons_hifz",
                category = "LESSONS",
                titleAr = "دروس في أدب حامل القرآن ومنهجية الحلقات",
                titleEn = "Etiquette of the Quran Carrier & Halaqat Method",
                authorAr = "ملخص من التبيان في آداب حملة القرآن للنووي",
                authorEn = "Inspired by Imam An-Nawawi's At-Tibyan",
                summaryAr = "آداب المعلم والطالب في حلقات القرآن الكريم وكيفية بناء عادة المراجعة اليومية.",
                sourceReference = "التبيان في آداب حملة القرآن",
                contentText = """
١. إخلاص النية:
ينبغي لمعلم القرآن وطالبه أن يقصدا وجه الله تعالى، لا رياءً ولا سمعة.

٢. أدب الطالب مع معلمه:
التواضع والإنصات وتحضير الدرس قبل الحضور إلى الحلقة، وتدوين ملاحظات التجويد والأخطاء لتلافيها في الجلسة القادمة.

٣. قاعدة الثلثين في المراجعة:
من أنجح القواعد في حلقات التحفيظ: خصص ثلث وقتك للحفظ الجديد وثلثيه لمراجعة المحفوظ القريب والبعيد؛ فالمراجعة هي التي تُثبت القرآن في الصدر.
                """.trimIndent()
            )
        )
        return books.map { it.copy(totalLength = it.contentText.length) }
    }

    fun seedQuizQuestions(): List<CustomQuizQuestionEntity> = listOf(
        // QURAN
        CustomQuizQuestionEntity("qz_q_1", "QURAN", "EASY", "كم عدد سور القرآن الكريم؟", "How many Surahs are in the Holy Quran?", "١١٠ سور", "١١٤ سورة", "١٢٠ سورة", "٩٩ سورة", 1, "يتألف القرآن الكريم من ١١٤ سورة و٣٠ جزءاً.", "المصحف الشريف"),
        CustomQuizQuestionEntity("qz_q_2", "QURAN", "EASY", "ما هي السورة التي تُسمى أم الكتاب؟", "Which Surah is known as Umm Al-Kitab?", "سورة البقرة", "سورة يس", "سورة الفاتحة", "سورة الإخلاص", 2, "سورة الفاتحة هي أم الكتاب والسبع المثاني.", "صحيح البخاري (٤٤٧٤)"),
        CustomQuizQuestionEntity("qz_q_3", "QURAN", "MEDIUM", "ما هي أطول سورة في القرآن الكريم؟", "What is the longest Surah in the Quran?", "سورة آل عمران", "سورة النساء", "سورة الأعراف", "سورة البقرة", 3, "سورة البقرة أطول سورة وعدد آياتها ٢٨٦ آية.", "المصحف الشريف"),
        CustomQuizQuestionEntity("qz_q_4", "QURAN", "MEDIUM", "ما هي السورة التي تعدل ثلث القرآن في الأجر؟", "Which Surah equals a third of the Quran in reward?", "سورة الإخلاص", "سورة الكوثر", "سورة العصر", "سورة الفلق", 0, "ثبت في الصحيح أن «قل هو الله أحد» تعدل ثلث القرآن.", "صحيح البخاري (٥٠١٣)"),
        CustomQuizQuestionEntity("qz_q_5", "QURAN", "HARD", "ما هي السورة الوحيدة التي لا تبدأ بالبسملة؟", "Which Surah does not begin with Bismillah?", "سورة الأنفال", "سورة التوبة", "سورة النمل", "سورة الحج", 1, "سورة التوبة (براءة) لا تبدأ بالبسملة، بينما وردت البسملة مرتين في سورة النمل.", "علوم القرآن"),
        // HADITH
        CustomQuizQuestionEntity("qz_h_1", "HADITH", "EASY", "من هو راوي حديث «إنما الأعمال بالنيات»؟", "Who narrated 'Actions are judged by intentions'?", "أبو هريرة رضي الله عنه", "عمر بن الخطاب رضي الله عنه", "أنس بن مالك رضي الله عنه", "بلال بن رباح رضي الله عنه", 1, "رواه أمير المؤمنين عمر بن الخطاب رضي الله عنه.", "صحيح البخاري (١)"),
        CustomQuizQuestionEntity("qz_h_2", "HADITH", "EASY", "أكمل الحديث الشريف: «خيركم من تعلم القرآن...»", "Complete: 'The best of you are those who learn the Quran...'", "وحفظه فقط", "وعلّمه", "وكتبه", "واستمع إليه", 1, "«خيركم من تعلم القرآن وعلّمه» رواه عثمان بن عفان رضي الله عنه.", "صحيح البخاري (٥٠٢٧)"),
        CustomQuizQuestionEntity("qz_h_3", "HADITH", "MEDIUM", "في حديث جبريل، ما هو تعريف الإحسان؟", "How did Hadith Jibril define Ihsan?", "إخراج الزكاة كل عام", "أن تعبد الله كأنك تراه فإن لم تكن تراه فإنه يراك", "صيام يومي الإثنين والخميس", "قراءة سورة الكهف يوم الجمعة", 1, "الإحسان هو استشعار مراقبة الله في كل عبادة وعمل.", "صحيح مسلم (٨)"),
        CustomQuizQuestionEntity("qz_h_4", "HADITH", "HARD", "من مؤلف كتاب الأربعين النووية؟", "Who authored the 40 Hadith of An-Nawawi?", "الإمام مالك", "الإمام البخاري", "الإمام يحيى بن شرف النووي", "الإمام ابن حجر", 2, "ألّفه الإمام محيي الدين يحيى بن شرف النووي الدمشقي رحمه الله.", "مصادر الحديث"),
        // TAJWEED
        CustomQuizQuestionEntity("qz_t_1", "TAJWEED", "EASY", "ما هي حروف القلقلة المجموعة؟", "Which letters belong to Qalqalah?", "يرملون", "قطب جد", "أحرف الحلق", "الميم والنون", 1, "حروف القلقلة خمسة: القاف والطاء والباء والجيم والدال (قطب جد).", "أحكام التجويد"),
        CustomQuizQuestionEntity("qz_t_2", "TAJWEED", "MEDIUM", "ما الحكم في قوله تعالى ﴿مِن شَرِّ﴾؟", "What is the Tajweed rule in 'Min Sharri'?", "الإظهار الحلقي", "الإقلاب", "الإخفاء الحقيقي", "الإدغام بغير غنة", 2, "الشين من حروف الإخفاء الحقيقي الخمسة عشر.", "أحكام التجويد"),
        CustomQuizQuestionEntity("qz_t_3", "TAJWEED", "HARD", "كم مقدار المد اللازم الكلمي المثقل في ﴿الضَّالِّينَ﴾؟", "How many counts is Madd Lazim in 'Ad-Daaalleen'?", "حركتان", "أربع حركات", "ست حركات", "ثلاث حركات", 2, "يمد المد اللازم ست حركات لزوماً عند جميع القراء.", "أحكام المدود"),
        // KNOWLEDGE
        CustomQuizQuestionEntity("qz_k_1", "KNOWLEDGE", "EASY", "كم عدد أركان الإيمان؟", "How many Articles of Faith (Iman) are there?", "أربعة", "خمسة", "ستة", "سبعة", 2, "أركان الإيمان ستة: بالله وملائكته وكتبه ورسله واليوم الآخر والقدر.", "صحيح مسلم (٨)"),
        CustomQuizQuestionEntity("qz_k_2", "KNOWLEDGE", "MEDIUM", "ما هي الأيام البيض المستحب صيامها كل شهر هجري؟", "Which Hijri dates are the White Days?", "١ و٢ و٣", "١٠ و١١ و١٢", "١٣ و١٤ و١٥", "٢٧ و٢٨ و٢٩", 2, "الأيام البيض هي الثالث عشر والرابع عشر والخامس عشر من كل شهر قمري.", "سنن النسائي (٢٤٢٤)"),
        CustomQuizQuestionEntity("qz_k_3", "KNOWLEDGE", "HARD", "في أي سنة هجرية وقع فتح مكة المكرمة؟", "In which Hijri year did the Conquest of Makkah take place?", "السنة الثانية للهجرة", "السنة الخامسة للهجرة", "السنة الثامنة للهجرة", "السنة العاشرة للهجرة", 2, "وقع فتح مكة في شهر رمضان من السنة الثامنة للهجرة.", "السيرة النبوية"),
        // MEMORIZATION
        CustomQuizQuestionEntity("qz_m_1", "MEMORIZATION", "EASY", "ما الآية التي تلي قوله تعالى ﴿إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ﴾؟", "Which Ayah follows 'Inna a'taynakal Kawthar'?", "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "لَكُمْ دِينُكُمْ وَلِيَ دِينِ", "وَرَفَعْنَا لَكَ ذِكْرَكَ", 1, "الآية الثانية من سورة الكوثر: ﴿فَصَلِّ لِرَبِّكَ وَانْحَرْ﴾.", "سورة الكوثر: آية ٢"),
        CustomQuizQuestionEntity("qz_m_2", "MEMORIZATION", "MEDIUM", "ما الآية التي تلي قوله تعالى ﴿وَوَضَعْنَا عَنكَ وِزْرَكَ﴾؟", "Which Ayah follows Surah Ash-Sharh 94:2?", "الَّذِي أَنقَضَ ظَهْرَكَ", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "وَرَفَعْنَا لَكَ ذِكْرَكَ", "فَإِذَا فَرَغْتَ فَانصَبْ", 0, "الآية الثالثة من سورة الشرح: ﴿الَّذِي أَنقَضَ ظَهْرَكَ﴾.", "سورة الشرح: آية ٣"),
        // KIDS
        CustomQuizQuestionEntity("qz_kids_1", "KIDS", "EASY", "ماذا نقول قبل أن نبدأ تناول الطعام؟", "What do we say before eating?", "بِسْمِ اللَّهِ", "الحمد لله بعد الأكل فقط", "سبحان الله", "الله أكبر", 0, "علّمنا النبي ﷺ أن نقول: «بِسْمِ اللَّهِ» قبل الطعام.", "صحيح البخاري (٥٣٧٦)"),
        CustomQuizQuestionEntity("qz_kids_2", "KIDS", "EASY", "كم عدد الصلوات المفروضة في اليوم والليلة؟", "How many daily obligatory prayers are there?", "ثلاث صلوات", "أربع صلوات", "خمس صلوات", "ست صلوات", 2, "الصلوات المفروضة خمس: الفجر، الظهر، العصر، المغرب، والعشاء.", "أركان الإسلام"),
        CustomQuizQuestionEntity("qz_kids_3", "KIDS", "EASY", "ما هو اللقب الذي عُرف به نبينا محمد ﷺ منذ صغره؟", "What title was Prophet Muhammad (PBUH) known by?", "الصادق الأمين", "الشجاع الكبير", "التاجر الغني", "المسافر الحكيم", 0, "عُرف ﷺ بالصادق الأمين لشدة صدقه وأمانته.", "السيرة النبوية")
    )
}
