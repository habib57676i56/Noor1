package com.example.data.local.catalog

import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem

/**
 * Categories 13..24 of the NOOR 2.0 Complete Authentic Islamic Adhkar Library.
 * Every entry uses verified Quranic verses or authentic Hadith references (Bukhari, Muslim,
 * Abu Dawud, At-Tirmidhi, Al-Nasa'i, Ibn Majah, Ahmad, Hisn al-Muslim).
 */
object AdhkarCatalogExtended {

    val adhkarPart2: List<DhikrItem> = listOf(
        // === 13. الاستغفار والتوبة (ISTIGHFAR) ===
        DhikrItem(
            id = "is_1",
            category = AdhkarCategory.ISTIGHFAR,
            textAr = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ.",
            targetCount = 3,
            sourceAr = "سنن أبي داود (١٥١٧) والترمذي (٣٥٧٧)",
            sourceEn = "Sunan Abi Dawud 1517 & At-Tirmidhi 3577",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها غفر الله له ذنوبه؛ وهي من أعظم صيغ الاستغفار والتوبة.",
            meaningEn = "I seek forgiveness from Allah the Almighty, whom there is no deity except Him, the Ever-Living, the Sustainer, and I repent to Him.",
            transliteration = "Astaghfirullahal-'Azimalladhi la ilaha illa Huwal-Hayyul-Qayyumu wa atubu ilayh."
        ),
        DhikrItem(
            id = "is_2",
            category = AdhkarCategory.ISTIGHFAR,
            textAr = "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ.",
            targetCount = 100,
            sourceAr = "سنن أبي داود (١٥١٦) والترمذي (٣٤٣٤)",
            sourceEn = "Sunan Abi Dawud 1516 & At-Tirmidhi 3434",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان الصحابة يعدّون للنبي ﷺ في المجلس الواحد مائة مرة يقولها.",
            meaningEn = "My Lord, forgive me and accept my repentance; indeed You are the Accepting of Repentance, the Most Merciful.",
            transliteration = "Rabbighfir li wa tub 'alayya innaka Antat-Tawwabur-Rahim."
        ),
        DhikrItem(
            id = "is_3",
            category = AdhkarCategory.ISTIGHFAR,
            textAr = "اللَّهُمَّ اغْفِرْ لِي خَطِيئَتِي وَجَهْلِي، وَإِسْرَافِي فِي أَمْرِي، وَمَا أَنْتَ أَعْلَمُ بِهِ مِنِّي، اللَّهُمَّ اغْفِرْ لِي جِدِّي وَهَزْلِي، وَخَطَئِي وَعَمْدِي، وَكُلُّ ذَلِكَ عِنْدِي.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦٣٩٨) ومسلم (٢٧١٩)",
            sourceEn = "Sahih al-Bukhari 6398 & Muslim 2719",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "دعاء جامع لطلب المغفرة من جميع الذنوب والزلات الظاهرة والخفية.",
            meaningEn = "O Allah, forgive my sins, my ignorance, my excess in my affairs, and whatever You know better than me.",
            transliteration = "Allahummaghfir li khati'ati wa jahli wa israfi fi amri..."
        ),
        DhikrItem(
            id = "is_4",
            category = AdhkarCategory.ISTIGHFAR,
            textAr = "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا، وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ وَارْحَمْنِي، إِنَّكَ أَنْتَ الْغَفُورُ الرَّحِيمُ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٨٣٤) ومسلم (٢٧٠٥)",
            sourceEn = "Sahih al-Bukhari 834 & Muslim 2705",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "الدعاء الذي علّمه النبي ﷺ لأبي بكر الصديق رضي الله عنه ليدعو به في صلاته.",
            meaningEn = "O Allah, I have greatly wronged myself, and none forgives sins except You; so grant me forgiveness from Yourself and have mercy on me.",
            transliteration = "Allahumma inni zalamtu nafsi zulman kathiran wa la yaghfirudh-dhunuba illa Anta..."
        ),

        // === 14. التسبيح والتحميد والتكبير والتهليل (TASBEEH_DHIKR) ===
        DhikrItem(
            id = "td_1",
            category = AdhkarCategory.TASBEEH_DHIKR,
            textAr = "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَٰهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ.",
            targetCount = 33,
            sourceAr = "صحيح مسلم (٢٦٩٥)",
            sourceEn = "Sahih Muslim 2695",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "الباقيات الصالحات؛ أحب الكلام إلى الله وأفضل مما طلعت عليه الشمس.",
            meaningEn = "Glory be to Allah, praise be to Allah, there is no deity except Allah, and Allah is the Greatest — the most beloved words to Allah.",
            transliteration = "SubhanAllah, walhamdulillah, wa la ilaha illAllah, wallahu Akbar."
        ),
        DhikrItem(
            id = "td_2",
            category = AdhkarCategory.TASBEEH_DHIKR,
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ.",
            targetCount = 33,
            sourceAr = "صحيح البخاري (٦٤٠٦) ومسلم (٢٦٩٤)",
            sourceEn = "Sahih al-Bukhari 6406 & Muslim 2694",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "كلمتان خفيفتان على اللسان ثقيلتان في الميزان حبيبتان إلى الرحمن.",
            meaningEn = "Light on the tongue, heavy on the Scale, and beloved to the Most Merciful.",
            transliteration = "SubhanAllahi wa bihamdihi, SubhanAllahil-'Azim."
        ),
        DhikrItem(
            id = "td_3",
            category = AdhkarCategory.TASBEEH_DHIKR,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 100,
            sourceAr = "صحيح البخاري (٣٢٩٣) ومسلم (٢٦٩١)",
            sourceEn = "Sahih al-Bukhari 3293 & Muslim 2691",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "من قالها في يوم مائة مرة كانت له عدل عشر رقاب وكُتبت له مائة حسنة ومُحيت عنه مائة سيئة وكانت له حرزاً من الشيطان.",
            meaningEn = "Reciting this 100 times in a day brings immense reward, forgiveness, and protection throughout the day.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in Qadir."
        ),
        DhikrItem(
            id = "td_4",
            category = AdhkarCategory.TASBEEH_DHIKR,
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            targetCount = 3,
            sourceAr = "صحيح مسلم (٢٧٢٦)",
            sourceEn = "Sahih Muslim 2726",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "أربع كلمات تقال ثلاثاً تعدل الساعات الطوال من الذكر.",
            meaningEn = "Glory be to Allah and praise Him, by the multitude of His creation, His own pleasure, the weight of His Throne, and the ink of His words.",
            transliteration = "SubhanAllahi wa bihamdihi, 'adada khalqihi, wa rida nafsihi, wa zinata 'arshihi, wa midada kalimatih."
        ),
        DhikrItem(
            id = "td_5",
            category = AdhkarCategory.TASBEEH_DHIKR,
            textAr = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ.",
            targetCount = 33,
            sourceAr = "صحيح البخاري (٤٢٠٥) ومسلم (٢٧٠٤)",
            sourceEn = "Sahih al-Bukhari 4205 & Muslim 2704",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "كنز من كنوز الجنة.",
            meaningEn = "There is no might nor power except through Allah — a treasure from the treasures of Paradise.",
            transliteration = "La hawla wa la quwwata illa billah."
        ),

        // === 15. الأدعية القرآنية (QURANIC_DUA) ===
        DhikrItem(
            id = "qd_1",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ.",
            targetCount = 1,
            sourceAr = "سورة البقرة: ٢٠١",
            sourceEn = "Surah Al-Baqarah 2:201",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "أجمع دعاء لخير الدنيا والآخرة وكان أكثر دعاء النبي ﷺ.",
            meaningEn = "Our Lord, give us in this world that which is good and in the Hereafter that which is good, and protect us from the punishment of the Fire.",
            transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar."
        ),
        DhikrItem(
            id = "qd_2",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ.",
            targetCount = 1,
            sourceAr = "سورة آل عمران: ٨",
            sourceEn = "Surah Aal-E-Imran 3:8",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء الراسخين في العلم بطلب الثبات على الهداية والرحمة.",
            meaningEn = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy. Indeed, You are the Bestower.",
            transliteration = "Rabbana la tuzigh qulubana ba'da idh hadaytana wa hab lana min ladunka rahmah."
        ),
        DhikrItem(
            id = "qd_3",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي ۝ وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي ۝ يَفْقَهُوا قَوْلِي.",
            targetCount = 1,
            sourceAr = "سورة طه: ٢٥–٢٨",
            sourceEn = "Surah Ta-Ha 20:25-28",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء نبي الله موسى عليه السلام لانشراح الصدر وتيسير الأمور.",
            meaningEn = "My Lord, expand for me my chest, ease for me my task, and untie the knot from my tongue that they may understand my speech.",
            transliteration = "Rabbish-rah li sadri, wa yassir li amri, wahlul 'uqdatan min lisani yafqahu qawli."
        ),
        DhikrItem(
            id = "qd_4",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا.",
            targetCount = 1,
            sourceAr = "سورة الفرقان: ٧٤",
            sourceEn = "Surah Al-Furqan 25:74",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء عباد الرحمن بصلاح الأهل والذرية والإمامة في الخير.",
            meaningEn = "Our Lord, grant us from among our spouses and offspring comfort to our eyes and make us an example for the righteous.",
            transliteration = "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yunin waj-'alna lil-muttaqina imama."
        ),
        DhikrItem(
            id = "qd_5",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبِّ زِدْنِي عِلْمًا.",
            targetCount = 1,
            sourceAr = "سورة طه: ١١٤",
            sourceEn = "Surah Ta-Ha 20:114",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء قرآني مبارك لطلب الزيادة في العلم النافع.",
            meaningEn = "My Lord, increase me in knowledge.",
            transliteration = "Rabbi zidni 'ilma."
        ),
        DhikrItem(
            id = "qd_6",
            category = AdhkarCategory.QURANIC_DUA,
            textAr = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ ۝ رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ.",
            targetCount = 1,
            sourceAr = "سورة إبراهيم: ٤٠–٤١",
            sourceEn = "Surah Ibrahim 14:40-41",
            gradingAr = "قرآن كريم",
            gradingEn = "Holy Quran",
            meaningAr = "دعاء الخليل إبراهيم عليه السلام بالمحافظة على الصلاة والمغفرة للوالدين والمؤمنين.",
            meaningEn = "My Lord, make me an establisher of prayer, and from my descendants; our Lord, accept my supplication and forgive me, my parents, and the believers.",
            transliteration = "Rabbij-'alni muqimas-salati wa min dhurriyyati, Rabbana wa taqabbal du'a."
        ),

        // === 16. الأدعية النبوية الصحيحة (PROPHETIC_DUA) ===
        DhikrItem(
            id = "pd_1",
            category = AdhkarCategory.PROPHETIC_DUA,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧٢١)",
            sourceEn = "Sahih Muslim 2721",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء نبوي جامع لأصول الخير: الهداية، والتقوى، والعفة، وغنى النفس.",
            meaningEn = "O Allah, I ask You for guidance, piety, chastity, and self-sufficiency.",
            transliteration = "Allahumma inni as'alukal-huda wat-tuqa wal-'afafa wal-ghina."
        ),
        DhikrItem(
            id = "pd_2",
            category = AdhkarCategory.PROPHETIC_DUA,
            textAr = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ.",
            targetCount = 3,
            sourceAr = "سنن الترمذي (٢١٤٠)",
            sourceEn = "Sunan At-Tirmidhi 2140",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان من أكثر دعاء النبي ﷺ طلباً للثبات على الحق والإيمان.",
            meaningEn = "O Turner of hearts, keep my heart firm upon Your religion.",
            transliteration = "Ya Muqallibal-qulubi thabbit qalbi 'ala dinik."
        ),
        DhikrItem(
            id = "pd_3",
            category = AdhkarCategory.PROPHETIC_DUA,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.",
            targetCount = 1,
            sourceAr = "سنن ابن ماجه (٩٢٥)",
            sourceEn = "Sunan Ibn Majah 925",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان النبي ﷺ يقولها إذا صلى الصبح حين يسلم.",
            meaningEn = "O Allah, I ask You for beneficial knowledge, pure provision, and accepted deeds.",
            transliteration = "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbala."
        ),
        DhikrItem(
            id = "pd_4",
            category = AdhkarCategory.PROPHETIC_DUA,
            textAr = "اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧٢٠)",
            sourceEn = "Sahih Muslim 2720",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من جوامع الدعاء النبوي لصلاح الدين والدنيا والآخرة.",
            meaningEn = "O Allah, rectify for me my religion which is the safeguard of my affairs, rectify my worldly life, and rectify my Hereafter.",
            transliteration = "Allahumma aslih li dinil-ladhi huwa 'ismatu amri..."
        ),

        // === 17. أذكار وسنن يوم الجمعة (FRIDAY) ===
        DhikrItem(
            id = "fr_1",
            category = AdhkarCategory.FRIDAY,
            textAr = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ، اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ.",
            targetCount = 10,
            sourceAr = "صحيح البخاري (٣٣٧٠) · الإكثار يوم الجمعة: سنن أبي داود (١٠٤٧)",
            sourceEn = "Sahih al-Bukhari 3370 · Sunan Abi Dawud 1047",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "الإكثار من الصلاة الإبراهيمية على النبي ﷺ ليلة الجمعة ويومها فإن صلاتكم معروضة عليه.",
            meaningEn = "Sending abundant Salawat upon Prophet Muhammad (PBUH) on Friday is among the greatest Sunnahs of the day.",
            transliteration = "Allahumma salli 'ala Muhammadin wa 'ala ali Muhammad..."
        ),
        DhikrItem(
            id = "fr_2",
            category = AdhkarCategory.FRIDAY,
            textAr = "قراءة سورة الكهف يوم الجمعة — ﴿الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجَا﴾.",
            targetCount = 1,
            sourceAr = "المستدرك للحاكم (٣٣٩٢) والبيهقي",
            sourceEn = "Al-Hakim 3392 & Al-Bayhaqi",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قرأ سورة الكهف في يوم الجمعة أضاء له من النور ما بين الجمعتين.",
            meaningEn = "Whoever recites Surah Al-Kahf on Friday will have a light shining for him between the two Fridays.",
            transliteration = "Recite Surah Al-Kahf (Surah 18) on Friday."
        ),
        DhikrItem(
            id = "fr_3",
            category = AdhkarCategory.FRIDAY,
            textAr = "تحرّي ساعة الاستجابة يوم الجمعة (خاصة آخر ساعة بعد العصر) والإكثار من الدعاء الصالح.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٩٣٥) ومسلم (٨٥٢) وسنن أبي داود (١٠٤٨)",
            sourceEn = "Sahih al-Bukhari 935, Muslim 852 & Abi Dawud 1048",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "في يوم الجمعة ساعة لا يوافقها عبد مسلم وهو قائم يصلي يسأل الله شيئاً إلا أعطاه إياه.",
            meaningEn = "On Friday there is an hour during which a Muslim asking Allah for good is granted it — seek it especially in the last hour after Asr.",
            transliteration = "Seek the accepted hour of Dua on Friday, especially after Asr before Maghrib."
        ),

        // === 18. أدعية الحج والعمرة (HAJJ_UMRAH) ===
        DhikrItem(
            id = "hj_1",
            category = AdhkarCategory.HAJJ_UMRAH,
            textAr = "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ.",
            targetCount = 3,
            sourceAr = "صحيح البخاري (١٥٤٩) ومسلم (١١٨٤)",
            sourceEn = "Sahih al-Bukhari 1549 & Muslim 1184",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "التلبية الشرعية في الإحرام بالحج والعمرة.",
            meaningEn = "Here I am, O Allah, here I am. Here I am, You have no partner, here I am. Verily all praise, grace, and sovereignty belong to You.",
            transliteration = "LabbaykAllahumma labbayk, labbayka la sharika laka labbayk..."
        ),
        DhikrItem(
            id = "hj_2",
            category = AdhkarCategory.HAJJ_UMRAH,
            textAr = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (١٨٩٢) — بين الركن اليماني والحجر الأسود",
            sourceEn = "Sunan Abi Dawud 1892 (Between Yemeni Corner & Black Stone)",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "كان النبي ﷺ يقولها في الطواف بين الركن اليماني والحجر الأسود.",
            meaningEn = "Recited during Tawaf between the Yemeni Corner and the Black Stone.",
            transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar."
        ),
        DhikrItem(
            id = "hj_3",
            category = AdhkarCategory.HAJJ_UMRAH,
            textAr = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ، أَنْجَزَ وَعْدَهُ، وَنَصَرَ عَبْدَهُ، وَهَزَمَ الْأَحْزَابَ وَحْدَهُ.",
            targetCount = 3,
            sourceAr = "صحيح مسلم (١٢١٨) — على الصفا والمروة",
            sourceEn = "Sahih Muslim 1218 (At Safa and Marwah)",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال ثلاث مرات على الصفا والمروة مع الدعاء بينها.",
            meaningEn = "Recited three times atop Safa and Marwah facing the Qibla with personal supplication in between.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah... anjaza wa'dahu wa nasara 'abdahu..."
        ),
        DhikrItem(
            id = "hj_4",
            category = AdhkarCategory.HAJJ_UMRAH,
            textAr = "خَيْرُ الدُّعَاءِ دُعَاءُ يَوْمِ عَرَفَةَ: «لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ».",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٥٨٥)",
            sourceEn = "Sunan At-Tirmidhi 3585 (Day of Arafah)",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "خير ما قال النبي ﷺ والنبيون من قبله في يوم عرفة.",
            meaningEn = "The best supplication is that of the Day of Arafah, testifying to Allah's Oneness, Sovereignty, and Power.",
            transliteration = "La ilaha illAllahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in Qadir."
        ),

        // === 19. أذكار المطر والريح والرعد (RAIN_WEATHER) ===
        DhikrItem(
            id = "rw_1",
            category = AdhkarCategory.RAIN_WEATHER,
            textAr = "اللَّهُمَّ صَيِّبًا نَافِعًا.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (١٠٣٢)",
            sourceEn = "Sahih al-Bukhari 1032",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال عند نزول المطر طلباً لأن يكون غيثاً نافعاً مباركاً.",
            meaningEn = "O Allah, make it a beneficial downpour (said when rain falls).",
            transliteration = "Allahumma sayyiban nafi'a."
        ),
        DhikrItem(
            id = "rw_2",
            category = AdhkarCategory.RAIN_WEATHER,
            textAr = "مُطِرْنَا بِفَضْلِ اللَّهِ وَرَحْمَتِهِ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٨٤٦) ومسلم (٧١)",
            sourceEn = "Sahih al-Bukhari 846 & Muslim 71",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "يقال بعد نزول المطر نسبةً للنعمة إلى الله وحده.",
            meaningEn = "We have been given rain by the grace and mercy of Allah.",
            transliteration = "Mutirna bi-fadlillahi wa rahmatih."
        ),
        DhikrItem(
            id = "rw_3",
            category = AdhkarCategory.RAIN_WEATHER,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَهَا، وَخَيْرَ مَا فِيهَا، وَخَيْرَ مَا أُرْسِلَتْ بِهِ، وَأَعُوذُ بِكَ مِنْ شَرِّهَا، وَشَرِّ مَا فِيهَا، وَشَرِّ مَا أُرْسِلَتْ بِهِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٨٩٩)",
            sourceEn = "Sahih Muslim 899 (When wind blows)",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء هبوب الريح الشديدة بسؤال خيرها والاستعاذة من شرها.",
            meaningEn = "O Allah, I ask You for its good and the good of what it was sent with, and I seek refuge in You from its evil.",
            transliteration = "Allahumma inni as'aluka khayraha wa khayra ma fiha..."
        ),
        DhikrItem(
            id = "rw_4",
            category = AdhkarCategory.RAIN_WEATHER,
            textAr = "سُبْحَانَ الَّذِي يُسَبِّحُ الرَّعْدُ بِحَمْدِهِ وَالْمَلَائِكَةُ مِنْ خِيفَتِهِ.",
            targetCount = 1,
            sourceAr = "موطأ مالك (٣٦٤١) والأدب المفرد للبخاري (٧٢٣)",
            sourceEn = "Muwatta Malik & Al-Adab al-Mufrad 723",
            gradingAr = "صحيح الإسناد (موقوف)",
            gradingEn = "Authentic Chain (Athari)",
            meaningAr = "كان عبد الله بن الزبير رضي الله عنه إذا سمع الرعد ترك الحديث وقالها.",
            meaningEn = "Glory be to Him whom the thunder glorifies with His praise and the angels in awe of Him.",
            transliteration = "Subhanal-ladhi yusabbihur-ra'du bi-hamdihi wal-mala'ikatu min khifatih."
        ),

        // === 20. أذكار عيادة المريض (VISITING_SICK) ===
        DhikrItem(
            id = "vs_1",
            category = AdhkarCategory.VISITING_SICK,
            textAr = "لَا بَأْسَ طَهُورٌ إِنْ شَاءَ اللَّهُ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٣٦١٦)",
            sourceEn = "Sahih al-Bukhari 3616",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "كان النبي ﷺ إذا دخل على مريض يعوده قالها تطييباً لنفسه وبشارةً بالتطهير.",
            meaningEn = "No harm; it will be a purification (from sins), if Allah wills.",
            transliteration = "La ba'sa tahurun in sha'Allah."
        ),
        DhikrItem(
            id = "vs_2",
            category = AdhkarCategory.VISITING_SICK,
            textAr = "أَسْأَلُ اللَّهَ الْعَظِيمَ رَبَّ الْعَرْشِ الْعَظِيمِ أَنْ يَشْفِيَكَ.",
            targetCount = 7,
            sourceAr = "سنن أبي داود (٣١٠٦) والترمذي (٢٠٨٣)",
            sourceEn = "Sunan Abi Dawud 3106 & At-Tirmidhi 2083",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "ما من عبد مسلم يعود مريضاً لم يحضر أجله فيقولها سبع مرات إلا عوفي بإذن الله.",
            meaningEn = "I ask Allah the Almighty, Lord of the Mighty Throne, to heal you (recited 7 times).",
            transliteration = "As'alullahal-'Azima Rabbal-'Arshil-'Azimi an yashfiyak."
        ),
        DhikrItem(
            id = "vs_3",
            category = AdhkarCategory.VISITING_SICK,
            textAr = "اللَّهُمَّ رَبَّ النَّاسِ، أَذْهِبِ الْبَأْسَ، اشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٥٦٧٥) ومسلم (٢١٩١)",
            sourceEn = "Sahih al-Bukhari 5675 & Muslim 2191",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "رقية النبي ﷺ ودعاؤه للمريض بالشفاء التام.",
            meaningEn = "O Allah, Lord of mankind, remove the affliction and heal, for You are the Healer; there is no healing except Your healing, a healing that leaves no illness behind.",
            transliteration = "Allahumma Rabban-nasi adh-hibil-ba's, ishfi Antash-Shafi, la shifa'a illa shifa'uk..."
        ),

        // === 21. أدعية التعزية والجنازة (CONDOLENCE) ===
        DhikrItem(
            id = "cn_1",
            category = AdhkarCategory.CONDOLENCE,
            textAr = "إِنَّ لِلَّهِ مَا أَخَذَ، وَلَهُ مَا أَعْطَى، وَكُلُّ شَيْءٍ عِنْدَهُ بِأَجَلٍ مُسَمًّى، فَلْتَصْبِرْ وَلْتَحْتَسِبْ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (١٢٨٤) ومسلم (٩٢٣)",
            sourceEn = "Sahih al-Bukhari 1284 & Muslim 923",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "أصح وأعظم صيغة للتعزية النبوية في المصائب وفقد الأحبة.",
            meaningEn = "Indeed, to Allah belongs what He took and to Him belongs what He gave, and everything with Him has an appointed time; so be patient and seek reward.",
            transliteration = "Inna lillahi ma akhadha, wa lahu ma a'ta, wa kullu shay'in 'indahu bi-ajalin musamma, fal-tasbir wal-tahtasib."
        ),
        DhikrItem(
            id = "cn_2",
            category = AdhkarCategory.CONDOLENCE,
            textAr = "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٩١٨)",
            sourceEn = "Sahih Muslim 918",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء من أصابته مصيبة؛ يأجره الله فيها ويخلف له خيراً منها.",
            meaningEn = "Indeed we belong to Allah and to Him we shall return. O Allah, reward me in my affliction and replace it for me with something better.",
            transliteration = "Inna lillahi wa inna ilayhi raji'un. Allahumma'jurni fi musibati wa akhlif li khayran minha."
        ),
        DhikrItem(
            id = "cn_3",
            category = AdhkarCategory.CONDOLENCE,
            textAr = "اللَّهُمَّ اغْفِرْ لَهُ وَارْحَمْهُ، وَعَافِهِ وَاعْفُ عَنْهُ، وَأَكْرِمْ نُزُلَهُ، وَوَسِّعْ مُدْخَلَهُ، وَاغْسِلْهُ بِالْمَاءِ وَالثَّلْجِ وَالْبَرَدِ، وَنَقِّهِ مِنَ الْخَطَايَا كَمَا نَقَّيْتَ الثَّوْبَ الْأَبْيَضَ مِنَ الدَّنَسِ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٩٦٣)",
            sourceEn = "Sahih Muslim 963 (Funeral Prayer Dua)",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "الدعاء للميت في صلاة الجنازة وعند الترحم عليه.",
            meaningEn = "O Allah, forgive him and have mercy on him, grant him safety and pardon him, honor his abode, and widen his entrance.",
            transliteration = "Allahummaghfir lahu warhamhu, wa 'afihi wa'fu 'anhu, wa akrim nuzulahu..."
        ),

        // === 22. دخول وخروج الخلاء (BATHROOM) ===
        DhikrItem(
            id = "bt_1",
            category = AdhkarCategory.BATHROOM,
            textAr = "بِسْمِ اللَّهِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (١٤٢) ومسلم (٣٧٥) والترمذي (٦٠٦)",
            sourceEn = "Sahih al-Bukhari 142, Muslim 375 & At-Tirmidhi 606",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "يقال قبل دخول الخلاء مع تقديم القدم اليسرى.",
            meaningEn = "In the name of Allah. O Allah, I seek refuge in You from all evil and impure spirits (said before entering with the left foot).",
            transliteration = "Bismillah, Allahumma inni a'udhu bika minal-khubuthi wal-khaba'ith."
        ),
        DhikrItem(
            id = "bt_2",
            category = AdhkarCategory.BATHROOM,
            textAr = "غُفْرَانَكَ.",
            targetCount = 1,
            sourceAr = "سنن أبي داود (٣٠) والترمذي (٧) وابن ماجه (٣٠٠)",
            sourceEn = "Sunan Abi Dawud 30, At-Tirmidhi 7 & Ibn Majah 300",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال عند الخروج من الخلاء بالقدم اليمنى طلباً لمغفرة الله.",
            meaningEn = "I seek Your forgiveness (said upon exiting with the right foot).",
            transliteration = "Ghufranak."
        ),

        // === 23. أذكار سماع الأذان (ADHAN) ===
        DhikrItem(
            id = "ad_1",
            category = AdhkarCategory.ADHAN,
            textAr = "يقول مثل ما يقول المؤذن، إلا في «حَيَّ عَلَى الصَّلَاةِ» و«حَيَّ عَلَى الْفَلَاحِ» فيقول: «لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ».",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦١١) ومسلم (٣٨٥)",
            sourceEn = "Sahih al-Bukhari 611 & Muslim 385",
            gradingAr = "متفق عليه",
            gradingEn = "Agreed Upon (Sahih)",
            meaningAr = "متابعة المؤذن بخشوع ويقين سبب لدخول الجنة.",
            meaningEn = "Repeat after the Mu'adhdhin word for word, except upon 'Hayya 'alas-Salah' and 'Hayya 'alal-Falah', say: La hawla wa la quwwata illa billah.",
            transliteration = "Repeat the Adhan; reply 'La hawla wa la quwwata illa billah' to the calls to prayer and success."
        ),
        DhikrItem(
            id = "ad_2",
            category = AdhkarCategory.ADHAN,
            textAr = "وَأَنَا أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، رَضِيتُ بِاللَّهِ رَبًّا، وَبِمُحَمَّدٍ رَسُولًا، وَبِالْإِسْلَامِ دِينًا.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٣٨٦)",
            sourceEn = "Sahih Muslim 386",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من قالها حين يسمع تشهد المؤذن غُفر له ذنبه.",
            meaningEn = "Said upon hearing the Mu'adhdhin's testimony of faith; brings forgiveness of sins.",
            transliteration = "Wa ana ash-hadu an la ilaha illAllahu wahdahu la sharika lah... Raditu billahi Rabban..."
        ),
        DhikrItem(
            id = "ad_3",
            category = AdhkarCategory.ADHAN,
            textAr = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ.",
            targetCount = 1,
            sourceAr = "صحيح البخاري (٦١٤)",
            sourceEn = "Sahih al-Bukhari 614",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "يقال بعد الصلاة على النبي ﷺ عقب الأذان؛ حلت لقائله شفاعة النبي ﷺ يوم القيامة.",
            meaningEn = "O Allah, Lord of this perfect call and established prayer, grant Muhammad al-Wasilah and excellence, and raise him to the praised station You promised him.",
            transliteration = "Allahumma Rabba hadhihid-da'watit-tammah, was-salatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah..."
        ),

        // === 24. أدعية إسلامية جامعة (GENERAL_SUPPLICATIONS) ===
        DhikrItem(
            id = "gs_1",
            category = AdhkarCategory.GENERAL_SUPPLICATIONS,
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنَ الْخَيْرِ كُلِّهِ عَاجِلِهِ وَآجِلِهِ، مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ، وَأَعُوذُ بِكَ مِنَ الشَّرِّ كُلِّهِ عَاجِلِهِ وَآجِلِهِ، مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ خَيْرِ مَا سَأَلَكَ عَبْدُكَ وَنَبِيُّكَ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا عَاذَ بِهِ عَبْدُكَ وَنَبِيُّكَ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ وَمَا قَرَّبَ إِلَيْهَا مِنْ قَوْلٍ أَوْ عَمَلٍ، وَأَعُوذُ بِكَ مِنَ النَّارِ وَمَا قَرَّبَ إِلَيْهَا مِنْ قَوْلٍ أَوْ عَمَلٍ، وَأَسْأَلُكَ أَنْ تَجْعَلَ كُلَّ قَضَاءٍ قَضَيْتَهُ لِي خَيْرًا.",
            targetCount = 1,
            sourceAr = "سنن ابن ماجه (٣٨٤٦) ومسند أحمد",
            sourceEn = "Sunan Ibn Majah 3846 & Musnad Ahmad",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "من أجمع الجوامع التي علّمها النبي ﷺ لأم المؤمنين عائشة رضي الله عنها.",
            meaningEn = "Comprehensive Prophetic supplication asking for all good in this world and the next, Paradise, and goodness in every decree.",
            transliteration = "Allahumma inni as'aluka minal-khayri kullihi 'ajilihi wa ajilih..."
        ),
        DhikrItem(
            id = "gs_2",
            category = AdhkarCategory.GENERAL_SUPPLICATIONS,
            textAr = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ.",
            targetCount = 1,
            sourceAr = "صحيح مسلم (٢٧٣٩)",
            sourceEn = "Sahih Muslim 2739",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "استعاذة لحفظ النعم والعافية والسلامة من سخط الله.",
            meaningEn = "O Allah, I seek refuge in You from the decline of Your blessings, the passing of Your well-being, Your sudden wrath, and all of Your displeasure.",
            transliteration = "Allahumma inni a'udhu bika min zawali ni'matik, wa tahawwuli 'afiyatik..."
        ),
        DhikrItem(
            id = "gs_3",
            category = AdhkarCategory.GENERAL_SUPPLICATIONS,
            textAr = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي.",
            targetCount = 3,
            sourceAr = "سنن الترمذي (٣٥١٣) وابن ماجه (٣٨٥٠)",
            sourceEn = "Sunan At-Tirmidhi 3513 & Ibn Majah 3850",
            gradingAr = "صحيح",
            gradingEn = "Sahih",
            meaningAr = "دعاء ليلة القدر وسائر الأوقات بطلب العفو والصفح من الله العفو الكريم.",
            meaningEn = "O Allah, You are Most Forgiving and You love forgiveness, so forgive me.",
            transliteration = "Allahumma innaka 'Afuwwun tuhibbul-'afwa fa'fu 'anni."
        ),
        DhikrItem(
            id = "gs_4",
            category = AdhkarCategory.GENERAL_SUPPLICATIONS,
            textAr = "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ.",
            targetCount = 1,
            sourceAr = "سنن الترمذي (٣٥٦٣)",
            sourceEn = "Sunan At-Tirmidhi 3563",
            gradingAr = "حسن",
            gradingEn = "Hasan",
            meaningAr = "دعاء للرزق الحلال والغنى عن الخلق وقضاء الدين.",
            meaningEn = "O Allah, suffice me with what You have made lawful over what You have forbidden, and enrich me by Your bounty so that I need none besides You.",
            transliteration = "Allahummak-fini bi-halalika 'an haramik, wa aghnini bi-fadlika 'amman siwak."
        )
    )
}
