package com.example.data.local.catalog

enum class IslamicVideoCategory(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val labelUz: String
) {
    ALL("ALL", "الكل", "All", "Barchasi"),
    QURAN_LESSONS("QURAN_LESSONS", "دروس قرآنية وتجويد", "Quran & Tajweed Lessons", "Qur'on va Tajvid darslari"),
    ISLAMIC_CARTOONS("ISLAMIC_CARTOONS", "كرتون إسلامي للأطفال", "Islamic Cartoons", "Islomiy multfilmlar"),
    PROPHETS_STORIES("PROPHETS_STORIES", "قصص الأنبياء والقرآن", "Prophets & Quran Stories", "Payg'ambarlar qissalari"),
    MY_VIDEOS("MY_VIDEOS", "فيديوهاتي المحلية", "My Local Videos", "Mening videolarim");

    companion object {
        fun fromKey(key: String?): IslamicVideoCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MY_VIDEOS
    }
}

data class IslamicVideoCollection(
    val id: String,
    val archiveItemId: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val subtitleUz: String,
    val category: IslamicVideoCategory,
    val speakerOrStudioAr: String,
    val speakerOrStudioEn: String,
    val isKidsSafe: Boolean = true
) {
    /** Authentic Archive.org cover thumbnail URL for this Islamic collection. */
    val coverThumbnailUrl: String
        get() = "https://archive.org/services/img/$archiveItemId"
}

data class IslamicVideoItem(
    val id: String,
    val collectionId: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val category: IslamicVideoCategory,
    val streamUrl: String,
    val durationLabel: String = "",
    val sourceLabel: String = "Archive.org",
    val isLocalUri: Boolean = false,
    val thumbnailUrl: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val uploaderName: String = "",
    val uploaderAvatarUrl: String = "",
    val publishedAtMillis: Long = 0L,
    val isUserPublished: Boolean = false
)

/**
 * Curated directory of real open-access Islamic video collections hosted on Archive.org
 * (Quran reading/Nooraniyah, Tajweed lessons, Prophets' stories cartoons, and Quranic stories).
 * Actual direct MP4 CDN URLs are resolved dynamically from Archive.org's JSON Metadata API
 * (`https://archive.org/metadata/{archiveItemId}`) by [com.example.data.quran.IslamicVideoRepository].
 */
object IslamicVideoCatalog {

    val curatedCollections: List<IslamicVideoCollection> = listOf(
        IslamicVideoCollection(
            id = "col_nooraniya",
            archiveItemId = "alnawrania1-17",
            titleAr = "القاعدة النورانية — دروس القراءة والقرآن",
            titleEn = "Al-Qaidah Al-Nooraniyah (Quran Lessons)",
            titleUz = "Qoidai Nuroniya — Qur'on o'qish darslari",
            subtitleAr = "١٧ درساً مرئياً لتعليم مخارج الحروف وتلاوة القرآن للأطفال والكبار",
            subtitleEn = "17 video lessons teaching Arabic letters, Makharij & Quran reading",
            subtitleUz = "Harflar maxraji va Qur'on o'qishni o'rgatuvchi 17 ta videodars",
            category = IslamicVideoCategory.QURAN_LESSONS,
            speakerOrStudioAr = "دروس القاعدة النورانية",
            speakerOrStudioEn = "Nooraniyah Academy",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_tajweed_amer",
            archiveItemId = "11245_201608",
            titleAr = "السلسلة الميسرة لتعليم التجويد — الشيخ أحمد عامر",
            titleEn = "Simplified Tajweed Lessons — Sheikh Ahmad Amer",
            titleUz = "Osonlashtirilgan Tajvid darslari — Shayx Ahmad Omir",
            subtitleAr = "سلسلة دروس عملية في أحكام النون الساكنة والمدود والصفات",
            subtitleEn = "Practical video lessons covering Nun Sakinah, Madd & Tajweed rules",
            subtitleUz = "Nun sokin, mad va tajvid qoidalari bo'yicha amaliy videodarslar",
            category = IslamicVideoCategory.QURAN_LESSONS,
            speakerOrStudioAr = "الشيخ أحمد عامر",
            speakerOrStudioEn = "Sheikh Ahmad Amer",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_clay_prophets",
            archiveItemId = "20220102_20220102_1155",
            titleAr = "قصص الأنبياء للأطفال — كرتون صلصال إسلامي",
            titleEn = "Stories of the Prophets — Islamic Claymation Cartoon",
            titleUz = "Payg'ambarlar qissalari — Islomiy multfilm",
            subtitleAr = "حلقات كرتون تربوية تحكي سير أنبياء الله بأسلوب شيق للأسرة والطفل",
            subtitleEn = "Family-friendly animated Islamic series narrating the Prophets' lives",
            subtitleUz = "Bolalar va oila uchun payg'ambarlar hayoti haqida tarbiyaviy multserial",
            category = IslamicVideoCategory.ISLAMIC_CARTOONS,
            speakerOrStudioAr = "كرتون إسلامي هادف",
            speakerOrStudioEn = "Islamic Kids Animation",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_quran_animals",
            archiveItemId = "360p_20210222",
            titleAr = "قصص الحيوان في القرآن — مسلسل كرتون إسلامي",
            titleEn = "Animal Stories in the Quran — Animated Series",
            titleUz = "Qur'ondagi hayvonlar qissalari — Multserial",
            subtitleAr = "سلسلة كرتون إسلامية ممتعة تعرض قصص هدهد سليمان، ناقة صالح، وحوت يونس",
            subtitleEn = "Animated series exploring Quranic stories like Hudhud, She-Camel & Yunus",
            subtitleUz = "Qur'onda zikr qilingan hudhud, tuya va Yunus alayhissalom qissalari",
            category = IslamicVideoCategory.ISLAMIC_CARTOONS,
            speakerOrStudioAr = "قصص القرآن الكرتونية",
            speakerOrStudioEn = "Quran Animated Series",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_human_namrud",
            archiveItemId = "26-1-human-stories-from-quran-720p",
            titleAr = "قصص الإنسان في القرآن — قصة النمرود وإبراهيم ﷺ",
            titleEn = "Human Stories in the Quran — Prophet Ibrahim & Namrud",
            titleUz = "Qur'ondagi insonlar qissasi — Ibrohim (a.s) va Namrud",
            subtitleAr = "درس قرآني مصوّر في الثبات على التوحيد من قصة خليل الله إبراهيم",
            subtitleEn = "Animated Quranic lesson on Tawhid and steadfastness from Surah Al-Baqarah",
            subtitleUz = "Tavhid va sobitqadamlik haqida Qur'oniy videodars",
            category = IslamicVideoCategory.PROPHETS_STORIES,
            speakerOrStudioAr = "سلسلة قصص الإنسان في القرآن",
            speakerOrStudioEn = "Quranic Stories Animation",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_human_qarun",
            archiveItemId = "7-1-human-stories-from-quran-360p",
            titleAr = "قصص الإنسان في القرآن — قصة قارون وعاقبة الكبر",
            titleEn = "Human Stories in the Quran — Story of Qarun",
            titleUz = "Qur'ondagi insonlar qissasi — Qorun qissasi",
            subtitleAr = "عبرة قرآنية مصورة من سورة القصص حول شكر النعمة وتجنب الغرور",
            subtitleEn = "Animated Quranic story from Surah Al-Qasas about gratitude and humility",
            subtitleUz = "Qasas surasidan shukronalik va kamtarlik haqida videodars",
            category = IslamicVideoCategory.PROPHETS_STORIES,
            speakerOrStudioAr = "سلسلة قصص الإنسان في القرآن",
            speakerOrStudioEn = "Quranic Stories Animation",
            isKidsSafe = true
        ),
        IslamicVideoCollection(
            id = "col_human_gardens",
            archiveItemId = "29-2-human-stories-from-quran-720p",
            titleAr = "قصص الإنسان في القرآن — أصحاب الجنتين (سورة الكهف)",
            titleEn = "Human Stories in the Quran — Owners of the Two Gardens",
            titleUz = "Qur'ondagi insonlar qissasi — Ikki bog' sohibi (Kahf surasi)",
            subtitleAr = "قصة أصحاب الجنتين من سورة الكهف مصورة بأسلوب تربوي مؤثر",
            subtitleEn = "Visual Quranic narration of the Two Gardens parable from Surah Al-Kahf",
            subtitleUz = "Kahf surasidagi ikki bog' egasi haqidagi Qur'oniy videodars",
            category = IslamicVideoCategory.PROPHETS_STORIES,
            speakerOrStudioAr = "سلسلة قصص الإنسان في القرآن",
            speakerOrStudioEn = "Quranic Stories Animation",
            isKidsSafe = true
        )
    )
}
