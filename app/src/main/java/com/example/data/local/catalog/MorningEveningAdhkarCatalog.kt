package com.example.data.local.catalog

import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem

/**
 * NOOR 3.0 — Comprehensive, Verified Morning & Evening Adhkar Collection.
 *
 * Every entry contains:
 * - Complete vocalized Arabic text (with diacritics / Tashkeel)
 * - Authentic repetition count per established Prophetic narrations
 * - Explicit scholarly source reference & Hadith grading (Arabic + English)
 * - Clear explanation / virtue and transliteration
 */
object MorningEveningAdhkarCatalog {

    val morningAdhkar: List<DhikrItem> = listOf(
        // 1. Ayat al-Kursi
        DhikrItem(
            id = "m_1",
            category = AdhkarCategory.MORNING,
            textAr = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ.",
            targetCount = 1,
            sourceAr = "سورة البقرة: ٢٥٥ · النسائي في السنن الكبرى (٩٨٤٨) والحاكم وصححه",
            sourceEn = "Surah Al-Baqarah 2:255 · Sunan al-Kubra al-Nasa'i 9848 & Al-Hakim",
            gradingAr = "قرآن كريم · صحيح",
            gradingEn = "Holy Quran · Sahih",
            meaningAr = "من قرأ آية الكرسي حين يصبح أُجير من الجن حتى يمسي بإذن الله.",
            meaningEn = "Whoever recites Ayat al-Kursi in the morning is granted Allah's protection from harm until the evening.",
            transliteration = "Allahu la ilaha illa Huwal-Hayyul-Qayyum, la ta'khudhuhu sinatun wa la nawm..."
        ),
        // 2. Surah Al-Ikhlas (3x)
        DhikrItem(
            id = "m_ikhlas",
            category = AdhkarCategory.MORNING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ.",
            targetCount = 3,
            sourceAr = "سورة الإخلاص · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah Al-Ikhlas (112) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "من قرأ الإخلاص والمعوذتين ثلاث مرات حين يصبح وحين يمسي كفته من كل شيء.",
            meaningEn = "Reciting Al-Ikhlas, Al-Falaq, and An-Nas three times in the morning suffices as protection from everything.",
            transliteration = "Qul Huwallahu Ahad, Allahus-Samad, lam yalid wa lam yulad, wa lam yakun lahu kufuwan ahad."
        ),
        // 3. Surah Al-Falaq (3x)
        DhikrItem(
            id = "m_falaq",
            category = AdhkarCategory.MORNING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ.",
            targetCount = 3,
            sourceAr = "سورة الفلق · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah Al-Falaq (113) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "الاستعاذة برب الفلق من كل شر وحسد وسحر، وتُقرأ ثلاثاً في الصباح.",
            meaningEn = "Seeking refuge with the Lord of Daybreak from all evil, darkness, envy, and harm — recited 3 times in the morning.",
            transliteration = "Qul a'udhu bi-Rabbil-falaq, min sharri ma khalaq, wa min sharri ghasiqin idha waqab, wa min sharrin-naffathati fil-'uqad, wa min sharri hasidin idha hasad."
        ),
        // 4. Surah An-Nas (3x)
        DhikrItem(
            id = "m_nas",
            category = AdhkarCategory.MORNING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ.",
            targetCount = 3,
            sourceAr = "سورة الناس · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah An-Nas (114) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "الاستعاذة برب الناس وملكهم وإلههم من وسوسة الشيطان من الجنة والناس، وتُقرأ ثلاثاً.",
            meaningEn = "Seeking refuge with the Lord, Sovereign, and God of mankind from the retreating whisperer — recited 3 times in the morning.",
            transliteration = "Qul a'udhu bi-Rabbin-nas, Malikin-nas, Ilahin-nas, min sharril-waswasil-khannas..."
        ),
        // 5. Asbahna wa asbahal-mulku lillah
        DhikrItem(
            id = "m_2",
            category = AdhkarCategory.MORNING,
            textAr = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧٢٣)",
            sourceEn = "Sahih Muslim 2723",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "إقرار بملك الله وحده وحمده، وسؤاله خير هذا اليوم وما بعده، والاستعاذة من الكسل وسوء الكبر وعذاب النار والقبر.",
            meaningEn = "We have reached the morning and at this very time unto Allah belongs all sovereignty; asking for the goodness of today and seeking refuge from laziness, senility, and punishment.",
            transliteration = "Asbahna wa asbahal-mulku lillah, walhamdulillahi, la ilaha illAllahu wahdahu la sharika lah..."
        ),
        // 6. Allahumma bika asbahna
        DhikrItem(
            id = "m_bika_asbahna",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٣٩١) وسنن أبي داود (٥٠٦٨) وسنن ابن ماجه (٣٨٦٨)",
            sourceEn = "Jami' at-Tirmidhi 3391, Sunan Abi Dawud 5068 & Ibn Majah 3868",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "اعتراف بأن الصباح والمساء والحياة والموت والبعث كلها بقدرة الله وتدبيره وحده.",
            meaningEn = "O Allah, by Your leave we have reached the morning and by Your leave we have reached the evening, by Your leave we live and die, and unto You is our resurrection.",
            transliteration = "Allahumma bika asbahna, wa bika amsayna, wa bika nahya, wa bika namutu, wa ilaykan-nushur."
        ),
        // 7. Sayyid al-Istighfar
        DhikrItem(
            id = "m_3",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٠٦) — سيد الاستغفار",
            sourceEn = "Sahih al-Bukhari 6306 — Sayyid al-Istighfar",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "سيد الاستغفار؛ من قاله من النهار موقناً به فمات من يومه قبل أن يمسي فهو من أهل الجنة.",
            meaningEn = "The master supplication for forgiveness: whoever recites it with conviction in the morning and dies before evening enters Paradise.",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu..."
        ),
        // 8. Allahumma inni asbahtu ushhiduka (4x)
        DhikrItem(
            id = "m_ushhiduka",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ إِنِّي أَصْبَحْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ.",
            targetCount = 4,
            sourceAr = "سنن أبي داود (٥٠٦٩) والبخاري في الأدب المفرد (١٢٠١)",
            sourceEn = "Sunan Abi Dawud 5069 & Al-Adab al-Mufrad 1201",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها حين يصبح أربع مرات أعتقه الله من النار.",
            meaningEn = "O Allah, I have reached the morning calling You, the bearers of Your Throne, Your angels, and all creation to witness that You alone are Allah and Muhammad is Your servant and Messenger. (4 times)",
            transliteration = "Allahumma inni asbahtu ushhiduka, wa ushhidu hamalata 'arshika, wa mala'ikataka, wa jami'a khalqika..."
        ),
        // 9. Allahumma ma asbaha bi min ni'mah (1x)
        DhikrItem(
            id = "m_nimah",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ مَا أَصْبَحَ بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٧٣) والنسائي في عمل اليوم والليلة",
            sourceEn = "Sunan Abi Dawud 5073 & Al-Nasa'i",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها حين يصبح فقد أدى شكر يومه، بإقراره أن كل نعمة من الله وحده.",
            meaningEn = "O Allah, whatever blessing I or any of Your creation have risen upon this morning is from You alone without partner; so to You belongs all praise and gratitude.",
            transliteration = "Allahumma ma asbaha bi min ni'matin aw bi-ahadin min khalqika fa-minka wahdaka la sharika laka, fa-lakal-hamdu wa lakash-shukr."
        ),
        // 10. Allahumma 'afini fi badani (3x)
        DhikrItem(
            id = "m_afini",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ. اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٩٠) ومسند أحمد (٢٠٤٣٠)",
            sourceEn = "Sunan Abi Dawud 5090 & Musnad Ahmad 20430",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "سؤال الله العافية في البدن والسمع والبصر، والاستعاذة من الكفر والفقر وعذاب القبر ثلاث مرات.",
            meaningEn = "O Allah, grant me health in my body, hearing, and sight; I seek refuge in You from disbelief, poverty, and punishment of the grave. (3 times)",
            transliteration = "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari, la ilaha illa Anta..."
        ),
        // 11. Allahumma inni as'alukal-'afwa wal-'afiyah (1x)
        DhikrItem(
            id = "m_afiyah",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٧٤) وسنن ابن ماجه (٣٨٧١)",
            sourceEn = "Sunan Abi Dawud 5074 & Sunan Ibn Majah 3871",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "لم يكن النبي ﷺ يدع هؤلاء الدعوات حين يمسي وحين يصبح؛ فيها سؤال العفو والعافية والستر والحفظ من كل الجهات.",
            meaningEn = "The Prophet ﷺ never left this supplication in morning or evening: asking for pardon, well-being in religion, worldly affairs, family, wealth, and protection from every direction.",
            transliteration = "Allahumma inni as'alukal-'afwa wal-'afiyata fid-dunya wal-akhirah..."
        ),
        // 12. Allahumma 'Alimal-ghaybi wash-shahadah (1x)
        DhikrItem(
            id = "m_alimal_ghayb",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَى نَفْسِي سُوءًا، أَوْ أَجُرَّهُ إِلَى مُسْلِمٍ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٣٩٢) وسنن أبي داود (٥٠٦٧)",
            sourceEn = "Jami' at-Tirmidhi 3392 & Sunan Abi Dawud 5067",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "علمه النبي ﷺ لأبي بكر الصديق ليقوله إذا أصبح وإذا أمسى وإذا أخذ مضجعه؛ استعاذة من شر النفس والشيطان.",
            meaningEn = "Taught by the Prophet ﷺ to Abu Bakr (RA): seeking refuge in the Knower of the unseen and seen from the evil of oneself and Satan.",
            transliteration = "Allahumma 'Alimal-ghaybi wash-shahadah, Fatiras-samawati wal-ard, Rabba kulli shay'in wa malikahu..."
        ),
        // 13. Bismillahilladhi la yadurru (3x)
        DhikrItem(
            id = "m_4",
            category = AdhkarCategory.MORNING,
            textAr = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٨٨) وسنن الترمذي (٣٣٨٨)",
            sourceEn = "Sunan Abi Dawud 5088 & Jami' at-Tirmidhi 3388",
            gradingAr = "حسن صحيح",
            gradingEn = "Hasan Sahih",
            meaningAr = "من قالها ثلاثاً حين يصبح لم تصبه فجأة بلاء ولم يضره شيء حتى يمسي بإذن الله.",
            meaningEn = "In the name of Allah, with whose name nothing on earth or in heaven can cause harm, and He is the All-Hearing, All-Knowing. (3 times)",
            transliteration = "Bismillahil-ladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa Huwas-Sami'ul-'Alim."
        ),
        // 14. Raditu billahi Rabban (3x)
        DhikrItem(
            id = "m_5",
            category = AdhkarCategory.MORNING,
            textAr = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٧٢) ومسند أحمد (١٨٩٦٧)",
            sourceEn = "Sunan Abi Dawud 5072 & Musnad Ahmad 18967",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها ثلاثاً حين يصبح وحين يمسي كان حقاً على الله أن يرضيه يوم القيامة.",
            meaningEn = "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet. (3 times)",
            transliteration = "Raditu billahi Rabban, wa bil-Islami dinan, wa bi-Muhammadin nabiyya."
        ),
        // 15. Ya Hayyu ya Qayyum (1x)
        DhikrItem(
            id = "m_6",
            category = AdhkarCategory.MORNING,
            textAr = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
            targetCount = 1,
            sourceAr = "النسائي في السنن الكبرى (١٠٣٣٠) والمستدرك للحاكم (٢٠٠٠)",
            sourceEn = "Al-Nasa'i al-Kubra 10330 & Al-Hakim 2000",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "أوصى به النبي ﷺ ابنته فاطمة رضي الله عنها أن تقوله إذا أصبحت وإذا أمست.",
            meaningEn = "O Ever-Living, O Sustainer, in Your mercy I seek relief; rectify all my affairs and do not leave me to myself even for the blink of an eye.",
            transliteration = "Ya Hayyu ya Qayyumu bi-rahmatika astaghithu, aslih li sha'ni kullahu wa la takilni ila nafsi tarfata 'ayn."
        ),
        // 16. Asbahna 'ala fitratil-Islam (1x)
        DhikrItem(
            id = "m_fitrah",
            category = AdhkarCategory.MORNING,
            textAr = "أَصْبَحْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.",
            targetCount = 1,
            sourceAr = "مسند أحمد (١٥٣٦٠) والنسائي في عمل اليوم والليلة",
            sourceEn = "Musnad Ahmad 15360 & Al-Nasa'i",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "تجديد العهد على الفطرة السليمة وكلمة التوحيد وسنة النبي ﷺ وملة إبراهيم الخليل عليه السلام.",
            meaningEn = "We have risen upon the natural disposition of Islam, the word of sincere devotion, the religion of our Prophet Muhammad ﷺ, and the faith of our father Ibrahim.",
            transliteration = "Asbahna 'ala fitratil-Islami, wa 'ala kalimatil-ikhlasi, wa 'ala dini Nabiyyina Muhammadin ﷺ..."
        ),
        // 17. SubhanAllahi wa bihamdihi 'adada khalqihi (3x — Morning specific)
        DhikrItem(
            id = "m_adada_khalqihi",
            category = AdhkarCategory.MORNING,
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            targetCount = 3,
            sourceAr = "صحيح مسلم (٢٧٢٦)",
            sourceEn = "Sahih Muslim 2726",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "أربع كلمات تقال ثلاث مرات في الصباح، ترجح في الأجر بما يُذكر الله به ساعات طويلة.",
            meaningEn = "Glory and praise be to Allah: as many times as the number of His creation, to the extent of His pleasure, the weight of His Throne, and the ink of His words. (3 times, Morning)",
            transliteration = "SubhanAllahi wa bihamdihi: 'adada khalqihi, wa rida nafsihi, wa zinata 'arshihi, wa midada kalimatihi."
        ),
        // 18. Allahumma inni as'aluka 'ilman nafi'an (1x — Morning after Fajr)
        DhikrItem(
            id = "m_ilman_nafian",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.",
            targetCount = 1,
            sourceAr = "سنن ابن ماجه (٩٢٥) ومسند أحمد (٢٦٦٠٢)",
            sourceEn = "Sunan Ibn Majah 925 & Musnad Ahmad 26602",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان النبي ﷺ يقولها إذا صلى الصبح حين يسلم؛ تجمع خير العلم والرزق والعمل الصالح.",
            meaningEn = "O Allah, I ask You for beneficial knowledge, pure provision, and deeds that are accepted. (Recited in the morning)",
            transliteration = "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbalan."
        ),
        // 19. Hasbiyallahu la ilaha illa Huwa (7x — attributed accurately)
        DhikrItem(
            id = "m_hasbi",
            category = AdhkarCategory.MORNING,
            textAr = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
            targetCount = 7,
            sourceAr = "سنن أبي داود (٥٠٨١) — موقوف على أبي الدرداء بسند صحيح",
            sourceEn = "Sunan Abi Dawud 5081 — Authentic Mawquf narration from Abu al-Darda (RA)",
            gradingAr = "موقوف صحيح الإسناد",
            gradingEn = "Sahih Isnad (Mawquf — Companion statement)",
            meaningAr = "مقتبس من سورة التوبة (١٢٩)؛ ورد عن أبي الدرداء رضي الله عنه: من قالها سبعاً حين يصبح وحين يمسي كفاه الله ما أهمه.",
            meaningEn = "Allah is sufficient for me; there is no deity except Him. Upon Him I rely, and He is the Lord of the Mighty Throne. (7 times — authentically attributed to Abu al-Darda)",
            transliteration = "Hasbiyallahu la ilaha illa Huwa, 'alayhi tawakkaltu wa Huwa Rabbul-'Arshil-'Azim."
        ),
        // 20. La ilaha illAllahu wahdahu (10x narration)
        DhikrItem(
            id = "m_tahlil_10",
            category = AdhkarCategory.MORNING,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 10,
            sourceAr = "سنن أبي داود (٥٠٧٧) وسنن ابن ماجه (٣٨٦٧) — رواية العشر",
            sourceEn = "Sunan Abi Dawud 5077 & Ibn Majah 3867 (10x narration)",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها عشراً حين يصبح كانت له عدل رقبة من ولد إسماعيل، وكُتب له عشر حسنات وحُط عنه عشر سيئات وحُفظ من الشيطان حتى يمسي.",
            meaningEn = "Reciting this Tahlil 10 times in the morning grants immense reward, forgiveness, elevation in rank, and protection until evening.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in qadir."
        ),
        // 21. La ilaha illAllahu wahdahu (100x narration)
        DhikrItem(
            id = "m_8",
            category = AdhkarCategory.MORNING,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، يُحْيِي وَيُمِيتُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 100,
            sourceAr = "صحيح البخاري (٣٢٩٣) وصحيح مسلم (٢٦٩١) — رواية المائة في اليوم",
            sourceEn = "Sahih al-Bukhari 3293 & Sahih Muslim 2691 (100x daily narration)",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "من قالها مائة مرة في يوم كانت له عدل عشر رقاب، وكتبت له مائة حسنة، ومحيت عنه مائة سيئة، وكانت له حرزاً من الشيطان يومه ذلك حتى يمسي.",
            meaningEn = "Whoever recites this 100 times in a day receives the reward of freeing ten slaves, 100 good deeds recorded, 100 sins erased, and a shield from Satan all day.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in qadir."
        ),
        // 22. SubhanAllahi wa bihamdihi (100x)
        DhikrItem(
            id = "m_7",
            category = AdhkarCategory.MORNING,
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
            targetCount = 100,
            sourceAr = "صحيح مسلم (٢٦٩٢)",
            sourceEn = "Sahih Muslim 2692",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قال حين يصبح وحين يمسي: سبحان الله وبحمده مائة مرة لم يأت أحد يوم القيامة بأفضل مما جاء به إلا أحد قال مثل ما قال أو زاد عليه.",
            meaningEn = "Whoever recites 'SubhanAllahi wa bihamdihi' 100 times in the morning and evening will not be surpassed in virtue on the Day of Resurrection except by one who says the same or more.",
            transliteration = "SubhanAllahi wa bihamdihi."
        ),
        // 23. Astaghfirullaha wa atubu ilayh (100x)
        DhikrItem(
            id = "m_istighfar_100",
            category = AdhkarCategory.MORNING,
            textAr = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ.",
            targetCount = 100,
            sourceAr = "صحيح البخاري (٦٣٠٧) وصحيح مسلم (٢٧٠٢)",
            sourceEn = "Sahih al-Bukhari 6307 & Sahih Muslim 2702",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "كان النبي ﷺ يستغفر الله ويتوب إليه في اليوم أكثر من سبعين مرة، وفي رواية مسلم مائة مرة.",
            meaningEn = "I seek Allah's forgiveness and turn to Him in repentance — the Prophet ﷺ sought forgiveness up to 100 times daily.",
            transliteration = "Astaghfirullaha wa atubu ilayh."
        ),
        // 24. Salawat upon Prophet Muhammad ﷺ (10x)
        DhikrItem(
            id = "m_salawat_10",
            category = AdhkarCategory.MORNING,
            textAr = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ، وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ.",
            targetCount = 10,
            sourceAr = "المعجم الكبير للطبراني (حسن لغيره عند الألباني) وفضل الصلاة في صحيح مسلم (٤٠٨)",
            sourceEn = "At-Tabarani (10x morning/evening, Hasan) & Sahih Muslim 408",
            gradingAr = "حسن · وأصل الصلاة صحيح",
            gradingEn = "Hasan (10x) · Core virtue Sahih",
            meaningAr = "من صلى على النبي ﷺ حين يصبح عشراً وحين يمسي عشراً أدركته شفاعته يوم القيامة، ومن صلى عليه واحدة صلى الله عليه بها عشراً.",
            meaningEn = "Sending blessings and peace upon Prophet Muhammad ﷺ ten times in the morning earns Allah's tenfold blessings and the Prophet's intercession.",
            transliteration = "Allahumma salli wa sallim 'ala Nabiyyina Muhammadin wa 'ala ali Muhammad..."
        )
    )

    val eveningAdhkar: List<DhikrItem> = listOf(
        // 1. Ayat al-Kursi
        DhikrItem(
            id = "e_kursi",
            category = AdhkarCategory.EVENING,
            textAr = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ.",
            targetCount = 1,
            sourceAr = "سورة البقرة: ٢٥٥ · النسائي في السنن الكبرى (٩٨٤٨) والحاكم",
            sourceEn = "Surah Al-Baqarah 2:255 · Sunan al-Kubra al-Nasa'i 9848 & Al-Hakim",
            gradingAr = "قرآن كريم · صحيح",
            gradingEn = "Holy Quran · Sahih",
            meaningAr = "من قرأ آية الكرسي حين يمسي أُجير من الجن حتى يصبح بإذن الله.",
            meaningEn = "Whoever recites Ayat al-Kursi in the evening is protected until the morning.",
            transliteration = "Allahu la ilaha illa Huwal-Hayyul-Qayyum, la ta'khudhuhu sinatun wa la nawm..."
        ),
        // 2. Last two verses of Surah Al-Baqarah (2:285-286 — established night/evening recitation)
        DhikrItem(
            id = "e_baqarah_last2",
            category = AdhkarCategory.EVENING,
            textAr = "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ لَا نُفَرِّقُ بَيْنَ أَحَدٍ مِّن رُّسُلِهِ ۚ وَقَالُوا سَمِعْنَا وَأَطَعْنَا ۖ غُفْرَانَكَ رَبَّنَا وَإِلَيْكَ الْمَصِيرُ ۝ لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ ۗ رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا ۚ رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا ۚ رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ ۖ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا ۚ أَنتَ مَوْلَانَا فَانصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ.",
            targetCount = 1,
            sourceAr = "سورة البقرة: ٢٨٥-٢٨٦ · صحيح البخاري (٥٠٠٩) وصحيح مسلم (٨٠٧)",
            sourceEn = "Surah Al-Baqarah 2:285-286 · Sahih al-Bukhari 5009 & Sahih Muslim 807",
            gradingAr = "قرآن كريم · متفق عليه",
            gradingEn = "Holy Quran · Agreed Upon (Sahih)",
            meaningAr = "من قرأ بالآيتين من آخر سورة البقرة في ليلة كفتاه؛ من كل سوء وشر.",
            meaningEn = "Whoever recites the last two verses of Surah Al-Baqarah at night, they will suffice him as protection and blessing.",
            transliteration = "Amanar-Rasulu bima unzila ilayhi min Rabbihi wal-mu'minun..."
        ),
        // 3. Surah Al-Ikhlas (3x)
        DhikrItem(
            id = "e_ikhlas",
            category = AdhkarCategory.EVENING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ.",
            targetCount = 3,
            sourceAr = "سورة الإخلاص · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah Al-Ikhlas (112) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "من قرأها مع المعوذتين ثلاثاً حين يمسي كفته من كل شيء.",
            meaningEn = "Reciting Surah Al-Ikhlas with the Mu'awwidhatayn three times in the evening suffices against every harm.",
            transliteration = "Qul Huwallahu Ahad, Allahus-Samad, lam yalid wa lam yulad, wa lam yakun lahu kufuwan ahad."
        ),
        // 4. Surah Al-Falaq (3x)
        DhikrItem(
            id = "e_falaq",
            category = AdhkarCategory.EVENING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ.",
            targetCount = 3,
            sourceAr = "سورة الفلق · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah Al-Falaq (113) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "التحصن بسورة الفلق ثلاث مرات في المساء من شر الليل والحسد وكل أذى.",
            meaningEn = "Seeking evening refuge in the Lord of Daybreak from the darkness of night and all harm — 3 times.",
            transliteration = "Qul a'udhu bi-Rabbil-falaq, min sharri ma khalaq, wa min sharri ghasiqin idha waqab..."
        ),
        // 5. Surah An-Nas (3x)
        DhikrItem(
            id = "e_nas",
            category = AdhkarCategory.EVENING,
            textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ.",
            targetCount = 3,
            sourceAr = "سورة الناس · سنن أبي داود (٥٠٨٢) وسنن الترمذي (٣٥٧٥)",
            sourceEn = "Surah An-Nas (114) · Sunan Abi Dawud 5082 & Jami' at-Tirmidhi 3575",
            gradingAr = "قرآن كريم · حسن صحيح",
            gradingEn = "Holy Quran · Hasan Sahih",
            meaningAr = "التحصن بسورة الناس ثلاث مرات في المساء من وساوس الشيطان من الجنة والناس.",
            meaningEn = "Seeking evening refuge in the Lord of mankind from all whispering evil — 3 times.",
            transliteration = "Qul a'udhu bi-Rabbin-nas, Malikin-nas, Ilahin-nas, min sharril-waswasil-khannas..."
        ),
        // 6. Amsayna wa amsal-mulku lillah
        DhikrItem(
            id = "e_1",
            category = AdhkarCategory.EVENING,
            textAr = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧٢٣)",
            sourceEn = "Sahih Muslim 2723",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء المساء النبوي بسؤال خير هذه الليلة وما بعدها والاستعاذة من شرها والكسل وسوء الكبر وعذاب النار والقبر.",
            meaningEn = "We have reached the evening and with it all dominion belongs to Allah; asking for the goodness of tonight and seeking refuge from evil, laziness, and punishment.",
            transliteration = "Amsayna wa amsal-mulku lillah, walhamdulillahi, la ilaha illAllahu wahdahu la sharika lah..."
        ),
        // 7. Allahumma bika amsayna
        DhikrItem(
            id = "e_2",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٣٩١) وسنن أبي داود (٥٠٦٨)",
            sourceEn = "Jami' at-Tirmidhi 3391 & Sunan Abi Dawud 5068",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "إقرار كامل بقدرة الله في المساء والصباح والحياة والموت والمرجع إليه سبحانه.",
            meaningEn = "O Allah, by Your leave we have reached the evening and by Your leave we have reached the morning, by You we live and die, and unto You is our final return.",
            transliteration = "Allahumma bika amsayna, wa bika asbahna, wa bika nahya, wa bika namutu, wa ilaykal-masir."
        ),
        // 8. Sayyid al-Istighfar (Evening)
        DhikrItem(
            id = "e_sayyid_istighfar",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٠٦) — سيد الاستغفار",
            sourceEn = "Sahih al-Bukhari 6306 — Sayyid al-Istighfar",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قاله من الليل وهو موقن به فمات قبل أن يصبح فهو من أهل الجنة.",
            meaningEn = "Whoever recites Sayyid al-Istighfar with conviction in the evening and passes away before morning is among the people of Paradise.",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka..."
        ),
        // 9. Allahumma inni amsaytu ushhiduka (4x)
        DhikrItem(
            id = "e_ushhiduka",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ.",
            targetCount = 4,
            sourceAr = "سنن أبي داود (٥٠٦٩) والأدب المفرد (١٢٠١)",
            sourceEn = "Sunan Abi Dawud 5069 & Al-Adab al-Mufrad 1201",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها أربع مرات حين يمسي أعتقه الله من النار.",
            meaningEn = "O Allah, I have reached the evening calling You, the bearers of Your Throne, Your angels, and all creation to witness Your Oneness and the messengership of Muhammad ﷺ. (4 times)",
            transliteration = "Allahumma inni amsaytu ushhiduka, wa ushhidu hamalata 'arshika, wa mala'ikataka..."
        ),
        // 10. Allahumma ma amsa bi min ni'mah (1x)
        DhikrItem(
            id = "e_nimah",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٧٣) والنسائي",
            sourceEn = "Sunan Abi Dawud 5073 & Al-Nasa'i",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها حين يمسي فقد أدى شكر ليلته.",
            meaningEn = "O Allah, whatever blessing I or any of Your creation have reached this evening with is from You alone without partner; to You belongs all praise and thanks.",
            transliteration = "Allahumma ma amsa bi min ni'matin aw bi-ahadin min khalqika fa-minka wahdaka la sharika laka..."
        ),
        // 11. Allahumma 'afini fi badani (3x)
        DhikrItem(
            id = "e_5",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ. اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٩٠) ومسند أحمد (٢٠٤٣٠)",
            sourceEn = "Sunan Abi Dawud 5090 & Musnad Ahmad 20430",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "سؤال العافية في البدن والسمع والبصر والاستعاذة من الكفر والفقر وعذاب القبر ثلاثاً في المساء.",
            meaningEn = "O Allah, grant me well-being in my body, hearing, and sight; I seek refuge in You from disbelief, poverty, and the punishment of the grave. (3 times)",
            transliteration = "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari..."
        ),
        // 12. Allahumma inni as'alukal-'afwa wal-'afiyah (1x)
        DhikrItem(
            id = "e_4",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٥٠٧٤) وسنن ابن ماجه (٣٨٧١)",
            sourceEn = "Sunan Abi Dawud 5074 & Sunan Ibn Majah 3871",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء جامع للعافية والستر والأمان والحفظ من جميع الجهات في المساء.",
            meaningEn = "A comprehensive evening supplication for forgiveness, well-being, concealment of faults, security, and protection from all directions.",
            transliteration = "Allahumma inni as'alukal-'afwa wal-'afiyata fid-dunya wal-akhirah..."
        ),
        // 13. Allahumma 'Alimal-ghaybi wash-shahadah (1x)
        DhikrItem(
            id = "e_6",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَى نَفْسِي سُوءًا، أَوْ أَجُرَّهُ إِلَى مُسْلِمٍ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٣٩٢) وسنن أبي داود (٥٠٦٧)",
            sourceEn = "Jami' at-Tirmidhi 3392 & Sunan Abi Dawud 5067",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "الاستعاذة بالله فاطر السماوات والأرض من شر النفس وشر الشيطان وشركه وسوء العمل.",
            meaningEn = "O Allah, Knower of the unseen and the seen, Creator of the heavens and earth, I seek refuge in You from the evil of my soul and Satan.",
            transliteration = "Allahumma 'Alimal-ghaybi wash-shahadah, Fatiras-samawati wal-ard..."
        ),
        // 14. Bismillahilladhi la yadurru (3x)
        DhikrItem(
            id = "e_bismillah",
            category = AdhkarCategory.EVENING,
            textAr = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٨٨) وسنن الترمذي (٣٣٨٨)",
            sourceEn = "Sunan Abi Dawud 5088 & Jami' at-Tirmidhi 3388",
            gradingAr = "حسن صحيح",
            gradingEn = "Hasan Sahih",
            meaningAr = "من قالها ثلاثاً حين يمسي لم تصبه فجأة بلاء ولم يضره شيء حتى يصبح بإذن الله.",
            meaningEn = "In the name of Allah, with whose name nothing on earth or in heaven can cause harm — recited 3 times in the evening.",
            transliteration = "Bismillahil-ladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa Huwas-Sami'ul-'Alim."
        ),
        // 15. Raditu billahi Rabban (3x)
        DhikrItem(
            id = "e_raditu",
            category = AdhkarCategory.EVENING,
            textAr = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (٥٠٧٢) ومسند أحمد (١٨٩٦٧)",
            sourceEn = "Sunan Abi Dawud 5072 & Musnad Ahmad 18967",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "من قالها ثلاثاً حين يمسي كان حقاً على الله أن يرضيه يوم القيامة.",
            meaningEn = "I am pleased with Allah as my Lord, Islam as my religion, and Muhammad ﷺ as my Prophet. (3 times)",
            transliteration = "Raditu billahi Rabban, wa bil-Islami dinan, wa bi-Muhammadin nabiyya."
        ),
        // 16. Ya Hayyu ya Qayyum (1x)
        DhikrItem(
            id = "e_yahayyu",
            category = AdhkarCategory.EVENING,
            textAr = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
            targetCount = 1,
            sourceAr = "النسائي في السنن الكبرى (١٠٣٣٠) والمستدرك للحاكم (٢٠٠٠)",
            sourceEn = "Al-Nasa'i al-Kubra 10330 & Al-Hakim 2000",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "التوسل باسمي الله الحي القيوم لإصلاح الشأن كله وتفويض الأمر إليه سبحانه في المساء.",
            meaningEn = "O Ever-Living, O Sustainer, in Your mercy I seek relief; rectify all my affairs and do not leave me to myself even for the blink of an eye.",
            transliteration = "Ya Hayyu ya Qayyumu bi-rahmatika astaghithu, aslih li sha'ni kullahu wa la takilni ila nafsi tarfata 'ayn."
        ),
        // 17. Amsayna 'ala fitratil-Islam (1x)
        DhikrItem(
            id = "e_fitrah",
            category = AdhkarCategory.EVENING,
            textAr = "أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.",
            targetCount = 1,
            sourceAr = "مسند أحمد (١٥٣٦٠) والنسائي في عمل اليوم والليلة",
            sourceEn = "Musnad Ahmad 15360 & Al-Nasa'i",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "إمساء المؤمن على الفطرة والتوحيد وسنة نبيه محمد ﷺ وملة إبراهيم عليه السلام.",
            meaningEn = "We have reached the evening upon the natural disposition of Islam, the word of sincerity, the religion of our Prophet Muhammad ﷺ, and the faith of Ibrahim.",
            transliteration = "Amsayna 'ala fitratil-Islami, wa 'ala kalimatil-ikhlasi, wa 'ala dini Nabiyyina Muhammadin ﷺ..."
        ),
        // 18. A'udhu bi-kalimatillahit-tammat (3x — Evening specific!)
        DhikrItem(
            id = "e_3",
            category = AdhkarCategory.EVENING,
            textAr = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
            targetCount = 3,
            sourceAr = "صحيح مسلم (٢٧٠٩) وسنن الترمذي (٣٦٠٤)",
            sourceEn = "Sahih Muslim 2709 & Jami' at-Tirmidhi 3604",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها ثلاث مرات حين يمسي لم تضره حمة ولا أذى تلك الليلة بإذن الله.",
            meaningEn = "I seek refuge in the perfect words of Allah from the evil of what He has created. (3 times in the evening)",
            transliteration = "A'udhu bi-kalimatillahit-tammati min sharri ma khalaq."
        ),
        // 19. Hasbiyallahu (7x — Evening)
        DhikrItem(
            id = "e_hasbi",
            category = AdhkarCategory.EVENING,
            textAr = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
            targetCount = 7,
            sourceAr = "سنن أبي داود (٥٠٨١) — موقوف على أبي الدرداء بسند صحيح",
            sourceEn = "Sunan Abi Dawud 5081 — Authentic Mawquf narration from Abu al-Darda (RA)",
            gradingAr = "موقوف صحيح الإسناد",
            gradingEn = "Sahih Isnad (Mawquf — Companion statement)",
            meaningAr = "من قالها سبع مرات حين يمسي كفاه الله ما أهمه من أمر الدنيا والآخرة.",
            meaningEn = "Allah is sufficient for me; there is no deity except Him. Upon Him I rely, and He is the Lord of the Mighty Throne. (7 times)",
            transliteration = "Hasbiyallahu la ilaha illa Huwa, 'alayhi tawakkaltu wa Huwa Rabbul-'Arshil-'Azim."
        ),
        // 20. La ilaha illAllahu wahdahu (10x Evening)
        DhikrItem(
            id = "e_tahlil_10",
            category = AdhkarCategory.EVENING,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 10,
            sourceAr = "سنن أبي داود (٥٠٧٧) ومسند أحمد (٢٣٥١٨)",
            sourceEn = "Sunan Abi Dawud 5077 & Musnad Ahmad 23518 (10x evening narration)",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها عشراً حين يمسي كانت له حرزاً من الشيطان حتى يصبح وأجر عظيم.",
            meaningEn = "Reciting this declaration of Allah's Oneness 10 times in the evening brings immense reward and protection until morning.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in qadir."
        ),
        // 21. SubhanAllahi wa bihamdihi (100x Evening)
        DhikrItem(
            id = "e_subhan_100",
            category = AdhkarCategory.EVENING,
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
            targetCount = 100,
            sourceAr = "صحيح مسلم (٢٦٩٢)",
            sourceEn = "Sahih Muslim 2692",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قال حين يصبح وحين يمسي: سبحان الله وبحمده مائة مرة لم يأت أحد يوم القيامة بأفضل مما جاء به إلا أحد قال مثل ما قال أو زاد عليه.",
            meaningEn = "Glory and praise be to Allah — 100 times in the evening.",
            transliteration = "SubhanAllahi wa bihamdihi."
        ),
        // 22. Astaghfirullaha wa atubu ilayh (100x Evening)
        DhikrItem(
            id = "e_istighfar_100",
            category = AdhkarCategory.EVENING,
            textAr = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ.",
            targetCount = 100,
            sourceAr = "صحيح البخاري (٦٣٠٧) وصحيح مسلم (٢٧٠٢)",
            sourceEn = "Sahih al-Bukhari 6307 & Sahih Muslim 2702",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "ختم اليوم بكثرة الاستغفار والتوبة إلى الله اقتداءً بالنبي ﷺ.",
            meaningEn = "Closing the day with abundant seeking of forgiveness and repentance, following the Sunnah of the Prophet ﷺ.",
            transliteration = "Astaghfirullaha wa atubu ilayh."
        ),
        // 23. Salawat upon Prophet Muhammad ﷺ (10x Evening)
        DhikrItem(
            id = "e_salawat_10",
            category = AdhkarCategory.EVENING,
            textAr = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ، وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ.",
            targetCount = 10,
            sourceAr = "المعجم الكبير للطبراني (حسن لغيره) وصحيح مسلم (٤٠٨)",
            sourceEn = "At-Tabarani (10x morning/evening, Hasan) & Sahih Muslim 408",
            gradingAr = "حسن · وأصل الصلاة صحيح",
            gradingEn = "Hasan (10x) · Core virtue Sahih",
            meaningAr = "الإكثار من الصلاة والسلام على رسول الله ﷺ في المساء لنيل شفاعته وصلاة الله على العبد عشراً بكل صلاة.",
            meaningEn = "Sending blessings and peace upon Prophet Muhammad ﷺ ten times in the evening.",
            transliteration = "Allahumma salli wa sallim 'ala Nabiyyina Muhammadin wa 'ala ali Muhammad..."
        )
    )
}
