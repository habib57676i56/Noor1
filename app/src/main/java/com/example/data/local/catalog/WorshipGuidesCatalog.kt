package com.example.data.local.catalog

data class WorshipGuideStep(
    val stepNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val detailAr: String,
    val detailEn: String,
    val detailUz: String,
    val arabicText: String = ""
)

/**
 * Curated offline Islamic guides:
 * 1. Step-by-Step Wudu (الوضوء)
 * 2. Step-by-Step Prayer (صفة الصلاة)
 * 3. Umrah & Hajj Rituals (مناسك العمرة والحج)
 * 4. Authentic Ruqyah Shar'iyyah (الرقية الشرعية من القرآن والسنة)
 */
object WorshipGuidesCatalog {

    val wuduSteps: List<WorshipGuideStep> = listOf(
        WorshipGuideStep(
            1, "النية والتسمية", "Intention & Bismillah", "Niyat va Bismilloh",
            "انْوِ الوضوء بقلبك ثم قل: بسم الله.",
            "Make sincere intention in your heart, then say Bismillah.",
            "Qalbingizda tahoratni niyat qiling va 'Bismilloh' deng.",
            "بِسْمِ اللَّهِ"
        ),
        WorshipGuideStep(
            2, "غسل الكفين ثلاثاً", "Wash Hands (3x)", "Qo'llarni yuvish (3 marta)",
            "اغسل كفيك إلى الرسغين ثلاث مرات مع تخليل الأصابع.",
            "Wash both hands up to the wrists three times, interlacing fingers.",
            "Ikki qo'lingizni bilaklargacha barmoqlar orasini ishqalab 3 marta yuving."
        ),
        WorshipGuideStep(
            3, "المضمضة والاستنشاق ثلاثاً", "Rinse Mouth & Nose (3x)", "Og'iz va burunni chayish (3 marta)",
            "تمضمض بالماء ثم استنشق واستنثر بيدك اليسرى ثلاث مرات.",
            "Rinse your mouth and gently sniff water into the nostrils, blowing it out with the left hand (3x).",
            "Og'zingizni va burningizni 3 marta suv bilan chaying."
        ),
        WorshipGuideStep(
            4, "غسل الوجه ثلاثاً", "Wash the Face (3x)", "Yuzni yuvish (3 marta)",
            "اغسل وجهك كاملاً من منابت الشعر إلى أسفل الذقن ومن الأذن إلى الأذن ثلاثاً.",
            "Wash the entire face from hairline to chin and ear to ear three times.",
            "Yuzingizni soch chiqqan joydan iyakkacha va quloqdan quloqqacha 3 marta yuving."
        ),
        WorshipGuideStep(
            5, "غسل اليدين إلى المرفقين ثلاثاً", "Wash Arms to Elbows (3x)", "Qo'llarni tirsakkacha yuvish (3 marta)",
            "اغسل يدك اليمنى من أطراف الأصابع إلى المرفق ثلاثاً، ثم اليسرى مثلها.",
            "Wash the right arm from fingertips to elbows three times, then the left arm.",
            "Avval o'ng qo'lingizni, so'ng chap qo'lingizni tirsaklar bilan qo'shib 3 marta yuving."
        ),
        WorshipGuideStep(
            6, "مسح الرأس والأذنين مرة", "Wipe Head & Ears (1x)", "Bosh va quloqlarga mash tortish (1 marta)",
            "امسح رأسك بيديك المبلولتين مقبلاً ومدبراً، ثم امسح أذنيك ظاهراً وباطناً مرة واحدة.",
            "Wipe your wet hands over your head front to back, then wipe inside and behind both ears once.",
            "Nam qo'llaringiz bilan boshingizga va quloqlaringizga bir marta mash torting."
        ),
        WorshipGuideStep(
            7, "غسل الرجلين إلى الكعبين ثلاثاً", "Wash Feet to Ankles (3x)", "Oyoqlarni to'piqqacha yuvish (3 marta)",
            "اغسل رجلك اليمنى إلى الكعبين مع تخليل الأصابع ثلاثاً، ثم اليسرى ثلاثاً.",
            "Wash the right foot up to and including the ankle three times, then the left foot.",
            "Avval o'ng, so'ng chap oyog'ingizni to'piqlari bilan qo'shib 3 marta yuving."
        ),
        WorshipGuideStep(
            8, "دعاء بعد الوضوء", "Supplication After Wudu", "Tahoratdan keyingi duo",
            "قل الشهادتين والدعاء المأثور بعد الفراغ من الوضوء لتُفتح لك أبواب الجنة الثمانية.",
            "Recite the Shahadah and prophetic dua after completing Wudu.",
            "Tahoratdan so'ng shahodat va sunnat duosini o'qing.",
            "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ"
        )
    )

    val umrahSteps: List<WorshipGuideStep> = listOf(
        WorshipGuideStep(
            1, "الإحرام من الميقات والتلبية", "Ihram & Talbiyah", "Ehrоm va Talbiya",
            "اغتسل وتطيّب والبس ثياب الإحرام، ثم انْوِ العمرة وابدأ بالتلبية حتى تصل الكعبة.",
            "Perform Ghusl, wear Ihram garments, make intention (Labbayka Umratan), and recite Talbiyah.",
            "G'usl qilib, ehrom kiyib, umrani niyat qiling va Ka'bagacha Talbiya ayting.",
            "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ"
        ),
        WorshipGuideStep(
            2, "الطواف حول الكعبة (٧ أشواط)", "Tawaf Around the Kaaba (7 Circuits)", "Ka'ba atrofida Tavof (7 aylanish)",
            "ابدأ من الحجر الأسود جاعلاً الكعبة عن يسارك، وطف سبعة أشواط مع الذكر والدعاء.",
            "Begin at the Black Stone keeping the Kaaba to your left, completing 7 counter-clockwise circuits with Dua.",
            "Qora toshdan boshlab Ka'bani chap tomoningizda qoldirib 7 marta tavof qiling.",
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ"
        ),
        WorshipGuideStep(
            3, "ركعتا الطواف وماء زمزم", "2 Rak'ahs at Maqam Ibrahim & Zamzam", "Maqomi Ibrohimda 2 rakat va Zamzam",
            "صلِّ ركعتين خلف مقام إبراهيم أو في أي مكان بالمسجد الحرام، ثم اشرب من ماء زمزم وادعُ الله.",
            "Pray 2 Rak'ahs behind Maqam Ibrahim (or anywhere in the Haram), then drink Zamzam water.",
            "Maqomi Ibrohim ortida 2 rakat namoz o'qing va Zamzam suvidan ichib duo qiling."
        ),
        WorshipGuideStep(
            4, "السعي بين الصفا والمروة (٧ أشواط)", "Sa'i Between Safa & Marwah (7 Laps)", "Safo va Marva orasida Sa'y (7 marta)",
            "اصعد الصفا واستقبل القبلة وادعُ، ثم اسعَ سبعة أشواط تبدأ بالصفا وتنتهي بالمروة.",
            "Begin at Safa facing the Qibla with Dua, then walk 7 laps between Safa and Marwah.",
            "Safodan boshlab Marvagacha jami 7 marta Sa'y qiling va zikr-duolar o'qing.",
            "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ"
        ),
        WorshipGuideStep(
            5, "الحلق أو التقصير والتحلل", "Halq / Taqsir & Exiting Ihram", "Soch oldirish yoki qisqartirish",
            "يحلق الرجل رأسه أو يقصّر منه بالكامل (والمرأة تقصّر قدر أنملة)، وبذلك تتم العمرة بحمد الله.",
            "Men shave or trim their hair evenly (women trim a fingertip's length), completing Umrah.",
            "Erkaklar sochini oldiradi yoki qisqartiradi (ayollar bir barmoq uchi qadar), shu bilan Umra tamomlanadi."
        )
    )

    val ruqyahItems: List<WorshipGuideStep> = listOf(
        WorshipGuideStep(
            1, "سورة الفاتحة (أم الكتاب والشفاء)", "Surah Al-Fatiha", "Fotiha surasi (Shifo surasi)",
            "تُقرأ بيقين وتدبّر وهي أعظم رقية في كتاب الله.",
            "Recited with conviction and reflection; the greatest Quranic Ruqyah.",
            "Ixlos va ishonch bilan o'qiladi — Qur'ondagi eng ulug' ruqya.",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
        ),
        WorshipGuideStep(
            2, "آية الكرسي (البقرة: ٢٥٥)", "Ayat al-Kursi (2:255)", "Oyatul Kursiy (Baqara: 255)",
            "حصن عظيم وحفظ من كل سوء بإذن الله.",
            "A mighty shield and divine protection by Allah's permission.",
            "Allohning izni bilan barcha yomonliklardan buyuk qo'rg'on.",
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ"
        ),
        WorshipGuideStep(
            3, "المعوذات (الإخلاص والفلق والناس — ٣ مرات)", "Al-Ikhlas, Al-Falaq & An-Nas (3x)", "Ixlos, Falaq va Nos suralari (3 marta)",
            "تُقرأ ثلاث مرات مع النفث والمسح على الجسد كما كان يفعل النبي ﷺ.",
            "Recite each three times and gently wipe over the body as taught by the Prophet ﷺ.",
            "Payg'ambarimiz ﷺ sunnatlariga muvofiq har birini 3 martadan o'qib dam solinadi.",
            "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ قُلْ أَعُوذُ بِرَبِّ النَّاسِ"
        ),
        WorshipGuideStep(
            4, "دعاء الشفاء النبوي", "Prophetic Dua for Healing", "Shifo so'rash nabaviy duosi",
            "ضع يدك على موضع الألم وادعُ بهذا الدعاء النبوي الصحيح (رواه البخاري ومسلم).",
            "Place your hand over the area of pain and recite this authentic prophetic healing prayer.",
            "Og'riq joyga qo'lingizni qo'yib, ushbu sahih nabaviy duoni o'qing.",
            "اللَّهُمَّ رَبَّ النَّاسِ، أَذْهِبِ الْبَاسَ، اشْفِهِ وَأَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا"
        ),
        WorshipGuideStep(
            5, "رقية جبريل عليه السلام للنبي ﷺ", "Ruqyah of Jibril (AS)", "Jabroil (a.s.) ruqyasi",
            "من أجمع الرقى النبوية الصحيحة (رواه مسلم).",
            "One of the most comprehensive authentic prophetic prayers of protection (Sahih Muslim).",
            "Eng jamlovchi sahih nabaviy himoya duolaridan (Sahih Muslim).",
            "بِسْمِ اللَّهِ أَرْقِيكَ، مِنْ كُلِّ شَيْءٍ يُؤْذِيكَ، مِنْ شَرِّ كُلِّ نَفْسٍ أَوْ عَيْنِ حَاسِدٍ، اللَّهُ يَشْفِيكَ، بِسْمِ اللَّهِ أَرْقِيكَ"
        )
    )
}
