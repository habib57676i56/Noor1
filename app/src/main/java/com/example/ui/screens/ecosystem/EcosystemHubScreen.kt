package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdCardDoc
import com.example.data.cloud.AdPlacement
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorAdCardBannerSection
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.EcosystemViewModel

private data class EcosystemModuleItem(
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun EcosystemHubScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    onOpenHalaqat: () -> Unit,
    onOpenHifzAi: () -> Unit,
    onOpenLocalAi: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTajweed: () -> Unit,
    onOpenKids: () -> Unit,
    onOpenVideos: () -> Unit = {},
    onOpenKhatm: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenHadith: () -> Unit,
    onOpenTafseer: () -> Unit,
    onOpenDuaa: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenOfflineAdmin: () -> Unit,
    onOpenZakat: () -> Unit = {},
    onOpenAsmaUlHusna: () -> Unit = {},
    onOpenWorshipGuides: () -> Unit = {},
    onOpenQadaTracker: () -> Unit = {},
    onOpenMirath: () -> Unit = {},
    onOpenCardDesigner: () -> Unit = {},
    onOpenHaramainLive: () -> Unit = {},
    onOpenGlobalQuranSearch: () -> Unit = {},
    onOpenLiveHadith: () -> Unit = {},
    onOpenQuranIrabSarf: () -> Unit = {},
    onOpenCrescentObservatory: () -> Unit = {},
    onOpenGlobalUmmahPulse: () -> Unit = {},
    onOpenMutashabihat: () -> Unit = {},
    onOpenSeerahAtlas: () -> Unit = {},
    onOpenQuranUniverse: () -> Unit = {},
    onOpenCosmicGlobe: () -> Unit = {},
    onOpenSoulRemedy: () -> Unit = {},
    onOpenMiraclesProphets: () -> Unit = {},
    onOpenCalligraphyStudio: () -> Unit = {},
    onOpenSanctuaryAudio: () -> Unit = {},
    onOpenAstrolabe: () -> Unit = {},
    onOpenMakharijLab: () -> Unit = {},
    onOpenMuhasabah: () -> Unit = {},
    onOpenAsbabAlNuzul: () -> Unit = {},
    onOpenVoiceAnalyzer: () -> Unit = {},
    onOpenMaqamatAcademy: () -> Unit = {},
    activeAdCards: List<AdCardDoc> = emptyList(),
    onAdImpression: (String) -> Unit = {},
    onAdClick: (String) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val circles by viewModel.circles.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val books by viewModel.libraryBooks.collectAsStateWithLifecycle()
    val aiKb by viewModel.approvedAiKnowledge.collectAsStateWithLifecycle()
    val khatmPlans by viewModel.khatmPlans.collectAsStateWithLifecycle()

    val coreLearning = listOf(
        EcosystemModuleItem(
            "حلقات نور", "NOOR Halaqat",
            "إدارة الحلقات والطلاب والمعلمين والتسميع", "Circles, Students, Teachers & Assessments",
            Icons.Rounded.Groups, EmeraldPrimary, "hub_halaqat", onOpenHalaqat
        ),
        EcosystemModuleItem(
            "الحفظ الذكي", "NOOR Hifz AI",
            "تسميع مباشر واختبارات الحفظ والآيات الضعيفة", "Live Mic Check, Hidden Tests & Schedules",
            Icons.Rounded.Mic, GoldPrimary, "hub_hifz_ai", onOpenHifzAi
        ),
        EcosystemModuleItem(
            "نور الذكي المحلي", "NOOR Local AI",
            "إجابات إسلامية موثّقة ١٠٠٪ بدون إنترنت", "100% Offline Cited Islamic Q&A",
            Icons.Rounded.Psychology, Color(0xFF1A9A6F), "hub_local_ai", onOpenLocalAi
        ),
        EcosystemModuleItem(
            "مكتبة نور الإسلامية", "NOOR Library",
            "تفسير، حديث، فقه، سيرة واستيراد كتب محلية", "Tafseer, Hadith, Fiqh, Seerah & Import",
            Icons.Rounded.LocalLibrary, Color(0xFFD9B464), "hub_library", onOpenLibrary
        )
    )

    val quranAndEducation = listOf(
        EcosystemModuleItem(
            "تجويد نور", "NOOR Tajweed",
            "أحكام التجويد بأمثلة قرآنية وصوتية واختبارات", "Rules, Audio Ayahs & Quizzes",
            Icons.Rounded.RecordVoiceOver, EmeraldPrimary, "hub_tajweed", onOpenTajweed
        ),
        EcosystemModuleItem(
            "ختمة نور والصيام", "Khatm & Fasting",
            "خطط ختم القرآن وتتبع صيام السنة والقضاء", "Multiple Khatm Plans & Sunnah Fasting",
            Icons.Rounded.NightsStay, GoldPrimary, "hub_khatm", onOpenKhatm
        ),
        EcosystemModuleItem(
            "مسابقات نور", "NOOR Quiz",
            "اختبارات إسلامية وقرآنية بثلاثة مستويات", "Offline Quizzes (Easy/Medium/Hard)",
            Icons.Rounded.Quiz, Color(0xFF2FC291), "hub_quiz", onOpenQuiz
        ),
        EcosystemModuleItem(
            "ملاحظات القرآن", "NOOR Notes",
            "خواطرك ومفضلاتك على الآيات دون تعديل المصحف", "Personal Ayah Notes & Collections",
            Icons.Rounded.EditNote, GoldPrimary, "hub_notes", onOpenNotes
        ),
        EcosystemModuleItem(
            "نور للأطفال", "NOOR Kids",
            "قصار السور والأدعية والقصص بإشراف الوالدين", "Short Surahs, Stories & Parent Controls",
            Icons.Rounded.ChildCare, Color(0xFF26B37E), "hub_kids", onOpenKids
        ),
        EcosystemModuleItem(
            "فيديوهات نور", "NOOR Videos",
            "دروس قرآنية وتجويد وكرتون إسلامي وقصص الأنبياء", "Quran Lessons, Tajweed & Islamic Cartoons",
            Icons.Rounded.OndemandVideo, GoldPrimary, "hub_videos", onOpenVideos
        )
    )

    val referencesAndAdmin = listOf(
        EcosystemModuleItem(
            "أحاديث نور", "NOOR Hadith",
            "مختارات الأحاديث الصحيحة وشرحها", "Verified Hadith & Explanations",
            Icons.Rounded.Book, EmeraldPrimary, "hub_hadith", onOpenHadith
        ),
        EcosystemModuleItem(
            "تفسير نور", "NOOR Tafseer",
            "تفسير ميسر للآيات وأسباب النزول", "Concise Tafseer & Asbab al-Nuzul",
            Icons.Rounded.MenuBook, GoldPrimary, "hub_tafseer", onOpenTafseer
        ),
        EcosystemModuleItem(
            "أدعية نور", "NOOR Duaa",
            "أدعية قرآنية ونبوية صحيحة وموثّقة", "Authentic Quranic & Prophetic Duas",
            Icons.Rounded.VolunteerActivism, Color(0xFF1A9A6F), "hub_duaa", onOpenDuaa
        ),
        EcosystemModuleItem(
            "إذاعة نور القرآنية", "NOOR Radio",
            "بث مباشر لإذاعات القرآن الكريم والتفسير", "Live Quran & Tafseer Radio Streams",
            Icons.Rounded.Radio, GoldPrimary, "hub_radio", onOpenRadio
        ),
        EcosystemModuleItem(
            "الإدارة العامة المحلية", "Offline Super Admin",
            "إدارة محلية كاملة وصلاحيات ونسخ احتياطي JSON", "Local RBAC Admin, KB & JSON Backup",
            Icons.Rounded.AdminPanelSettings, Color(0xFFE4AA44), "hub_offline_admin", onOpenOfflineAdmin
        )
    )

    val essentialIslamicTools = listOf(
        EcosystemModuleItem(
            "حاسبة الزكاة", "Zakat Calculator",
            "زكاة المال والذهب والفضة والتجارة وزكاة الفطر", "Wealth, Gold, Silver, Business & Zakat al-Fitr",
            Icons.Rounded.Calculate, GoldPrimary, "hub_zakat", onOpenZakat
        ),
        EcosystemModuleItem(
            "أسماء الله الحسنى", "99 Names of Allah",
            "٩٩ اسماً مع المعاني والبحث والمشاركة", "All 99 Beautiful Names with Meanings & Search",
            Icons.Rounded.AutoAwesome, EmeraldPrimary, "hub_asma_ul_husna", onOpenAsmaUlHusna
        ),
        EcosystemModuleItem(
            "دليل الوضوء والعمرة والرقية", "Wudu, Umrah & Ruqyah",
            "خطوات الوضوء ومناسك العمرة والرقية الشرعية", "Step-by-Step Wudu, Umrah Rituals & Ruqyah",
            Icons.Rounded.Mosque, Color(0xFF1A9A6F), "hub_worship_guides", onOpenWorshipGuides
        ),
        EcosystemModuleItem(
            "متابع قضاء الصلوات", "Missed Prayers (Qada)",
            "حساب الفوائت وتتبع القضاء اليومي بخطة منظمة", "Track & Complete Missed Prayers Daily",
            Icons.Rounded.EventRepeat, EmeraldPrimary, "hub_qada_tracker", onOpenQadaTracker
        ),
        EcosystemModuleItem(
            "حاسبة المواريث الشرعية", "Inheritance (Mirath)",
            "توزيع التركة وفق الفروض والعصبة والحجب والعول", "Shariah Estate Shares, Awl, Radd & Blocking",
            Icons.Rounded.Gavel, GoldPrimary, "hub_mirath", onOpenMirath
        ),
        EcosystemModuleItem(
            "مصمم البطاقات الإسلامية", "Islamic Card Designer",
            "صمّم بطاقات آيات وأدعية وتهاني وشاركها كصورة", "Create & Share Luxury Ayah & Dua Cards",
            Icons.Rounded.Palette, Color(0xFF2FC291), "hub_card_designer", onOpenCardDesigner
        )
    )

    val liveOnlineFeatures = listOf(
        EcosystemModuleItem(
            "ميزان المدود والتجويد الصوتي الحي", "Live Mic Tajweed & Madd",
            "تحليل حقيقي بالميكروفون لحركات المدّ (٢، ٤–٥، ٦) والطيف الصوتي والنَّفَس", "Real Hardware Mic DSP for Madd Harakaat, Live Spectrum & Breath",
            Icons.Rounded.Mic, Color(0xFF38BDF8), "hub_tajweed_voice_analyzer", onOpenVoiceAnalyzer
        ),
        EcosystemModuleItem(
            "أكاديمية المقامات القرآنية الـ٧", "7 Quranic Maqamat Academy",
            "سلم صوتي ميكروتوني حقيقي بربع التون + تلاوات قرآنية حية + اختبار الأذن", "Real Microtonal Scale Synth (Quarter-Tone), Live Recitations & Quiz",
            Icons.Rounded.GraphicEq, GoldBright, "hub_quran_maqamat_academy", onOpenMaqamatAcademy
        ),
        EcosystemModuleItem(
            "مختبر مخارج الحروف الحقيقي", "Real Anatomy Makharij Lab",
            "صورة تشريحية حقيقية واضحة للأنف والفم واللسان والحلق لـ ٢٨ حرفاً + تجويد", "Real Anatomical Vocal Tract Photo (28 Letters), Color Tajweed & Quiz",
            Icons.Rounded.RecordVoiceOver, Color(0xFF34D399), "hub_tajweed_makharij_lab", onOpenMakharijLab
        ),
        EcosystemModuleItem(
            "محاسبة النفس ومؤشر نور القلب", "Heart Noor Muhasabah Radar",
            "رادار إيماني سداسي الأبعاد + سجل ٧ أيام ووصفة قرآنية يومية", "6-Axis Golden Spiritual Radar, 7-Day Trend & Quranic Remedy",
            Icons.Rounded.Favorite, Color(0xFFF472B6), "hub_spiritual_muhasabah", onOpenMuhasabah
        ),
        EcosystemModuleItem(
            "أطلس أسباب النزول الصحيحة", "Authentic Asbab al-Nuzul",
            "قصص نزول الآيات من الصحيحين مع العِبر والفتح الفوري للمصحف", "Verified Revelation Occasions from Bukhari & Muslim + Mushaf",
            Icons.Rounded.MenuBook, GoldBright, "hub_asbab_al_nuzul", onOpenAsbabAlNuzul
        ),
        EcosystemModuleItem(
            "مرسم التذهيب والخط العربي ٣D", "Golden Calligraphy Studio",
            "تأثيرات شرارات النور باللمس + ٥ لوحات تذهيب + تلاوة وحفظ HD", "Touch Noor Sparks, 5 Gold-Leaf Themes, Live Audio & HD Art",
            Icons.Rounded.Brush, GoldBright, "hub_quran_calligraphy_studio", onOpenCalligraphyStudio
        ),
        EcosystemModuleItem(
            "واحة السكينة والصوتيات الحية", "Islamic Acoustic Sanctuary",
            "مازج مطر الحرم ونسيم الفجر والماء + تلاوات السكينة ومؤقت النوم", "4-Layer Live Rain/Breeze/Stream Mixer + Sakinah Quran & Timer",
            Icons.Rounded.GraphicEq, Color(0xFF2DD4BF), "hub_islamic_sanctuary_audio", onOpenSanctuaryAudio
        ),
        EcosystemModuleItem(
            "الأسطرلاب الإسلامي الذهبي", "Golden Islamic Astrolabe",
            "خريطة السماء الحية و١٥ نجماً عربياً أصيلاً بحساب الزمن النجمي", "Interactive Brass Astrolabe & 15 Historic Arabic Stars Live",
            Icons.Rounded.Explore, Color(0xFFF59E0B), "hub_islamic_astrolabe", onOpenAstrolabe
        ),
        EcosystemModuleItem(
            "الكرة الأرضية ٣D والقبلة بالأقمار الصناعية", "Real 3D Satellite Earth & Qibla",
            "كرة أرضية زرقاء حقيقية مع تقريب فائق للأماكن والكعبة وقوس القبلة الذهبي", "Real Blue Satellite 3D Earth, Deep Street Zoom & Golden Qibla Arc",
            Icons.Rounded.Language, Color(0xFF38BDF8), "hub_cosmic_qibla_globe", onOpenCosmicGlobe
        ),
        EcosystemModuleItem(
            "دواء القلوب من القرآن", "Quranic Soul Remedy",
            "وصفات إيمانية بالصوت الحي وجلسة السكينة والتنفس بالتسبيح", "Emotional Quranic Prescriptions, Live Audio & Dhikr Halo",
            Icons.Rounded.Favorite, Color(0xFFF472B6), "hub_quran_soul_remedy", onOpenSoulRemedy
        ),
        EcosystemModuleItem(
            "الآيات الكونية والأنبياء ٢٥", "Cosmic Signs & 25 Prophets",
            "الإعجاز الكوني والأمثال القرآنية وأطلس الأنبياء الـ٢٥ بالصوت", "Cosmic Signs, Parables & Complete 25 Prophets Atlas",
            Icons.Rounded.Science, GoldBright, "hub_quran_miracles_prophets", onOpenMiraclesProphets
        ),
        EcosystemModuleItem(
            "مجرة القرآن والقراءات العشر", "Quran Root & Qira'at Universe",
            "مجرة الجذور الحية (٦٢٣٦ آية) + القراءات المتواترة صوتاً + السند الذهبي", "Living Root Galaxy (6,236 Ayahs) + 10 Qira'at + Sanad",
            Icons.Rounded.Hub, GoldBright, "hub_quran_universe", onOpenQuranUniverse
        ),
        EcosystemModuleItem(
            "البث المباشر للحرمين ٢٤/٧", "Live Makkah & Madinah",
            "بث مباشر للكعبة والمسجد النبوي مع الطقس الحي", "24/7 Live Kaaba & Nabawi Streams + Weather",
            Icons.Rounded.LiveTv, Color(0xFFE53935), "hub_haramain_live", onOpenHaramainLive
        ),
        EcosystemModuleItem(
            "الباحث القرآني ومقارنة التفاسير", "Global Quran & Tafsir",
            "بحث حي في ٦٢٣٦ آية مع الميسر والجلالين والأوزبكية", "Search All 6,236 Ayahs + Multi-Tafsir & Audio",
            Icons.Rounded.TravelExplore, GoldPrimary, "hub_global_quran_search", onOpenGlobalQuranSearch
        ),
        EcosystemModuleItem(
            "موسوعة الحديث النبوي الحية", "Live Hadith Encyclopedia",
            "آلاف الأحاديث الصحيحة بالدرجة والشرح والفوائد", "Verified Hadiths with Grading, Notes (AR/EN/UZ)",
            Icons.Rounded.MenuBook, EmeraldPrimary, "hub_live_hadith", onOpenLiveHadith
        ),
        EcosystemModuleItem(
            "إعراب وصرف القرآن", "Quran I'rab & Sarf",
            "تحليل إعرابي وصرفي كلمة بكلمة لـ ١١٤ سورة", "Word-by-Word Grammatical I'rab & Morphology",
            Icons.Rounded.Spellcheck, GoldBright, "hub_quran_irab_sarf", onOpenQuranIrabSarf
        ),
        EcosystemModuleItem(
            "مرصد الهلال والقمر الحي", "Crescent Observatory",
            "عمر الهلال والمنازل الـ٢٨ ورؤية السماء وصلاة الكسوف", "Live 3D Moon, 28 Lunar Mansions & Sighting",
            Icons.Rounded.NightsStay, Color(0xFF38BDF8), "hub_crescent_observatory", onOpenCrescentObservatory
        ),
        EcosystemModuleItem(
            "نبض الأذان والقراءات النادرة", "Global Adhan & Qira'at",
            "مواقيت ١٢ عاصمة إسلامية حية + ورش وقالون والدوري والمجوّد", "12 Islamic Cities Live Adhan + Rare Qira'at Vault",
            Icons.Rounded.Public, Color(0xFF2FC291), "hub_global_ummah_pulse", onOpenGlobalUmmahPulse
        ),
        EcosystemModuleItem(
            "متشابهات القرآن والضوابط", "Quran Mutashabihat Lab",
            "فروق الآيات المتشابهة + الضوابط الذهبية + ماسح ٦٢٣٦ آية", "Word-Level Verse Diffs, Hifz Rules & Live Scanner",
            Icons.Rounded.CompareArrows, GoldPrimary, "hub_quran_mutashabihat", onOpenMutashabihat
        ),
        EcosystemModuleItem(
            "أطلس السيرة النبوية والشمائل", "Seerah Atlas & Shama'il",
            "خريطة الهجرة بالمسافات الحقيقية + الشمائل وإحياء السنن", "Interactive Hijrah GPS Map, Seerah & Sunnah Revival",
            Icons.Rounded.HistoryEdu, EmeraldPrimary, "hub_seerah_atlas", onOpenSeerahAtlas
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "منظومة نور المتكاملة", "NOOR Islamic Ecosystem"),
            subtitle = L10n.pick(
                lang,
                "منصة إسلامية شاملة تعمل دون إنترنت بالكامل ومجانية دون اشتراكات",
                "Complete Offline-First Islamic Platform — Zero Subscriptions"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldPrimary, EmeraldDeep))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "جاهز للعمل المحلي الكامل", "100% Offline-Ready Ecosystem"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "حلقات: ${circles.size} · طلاب: ${students.size} · كتب: ${books.size} · معرفة ذكية: ${aiKb.size} · ختمات: ${khatmPlans.size}",
                                    "Circles: ${circles.size} · Students: ${students.size} · Books: ${books.size} · AI KB: ${aiKb.size} · Khatm: ${khatmPlans.size}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (activeAdCards.any { it.placement == AdPlacement.ECOSYSTEM_HUB.key }) {
                item(key = "ecosystem_hub_ad_cards") {
                    NoorAdCardBannerSection(
                        adCards = activeAdCards,
                        placement = AdPlacement.ECOSYSTEM_HUB,
                        onImpression = onAdImpression,
                        onClick = onAdClick
                    )
                }
            }

            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "الميزات الحية بالإنترنت (الحرمين، الباحث القرآني، الموسوعة)", "Live Online Super-Features")
                )
            }
            item {
                ModuleGrid(items = liveOnlineFeatures)
            }

            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "الأدوات الإسلامية الأساسية (الزكاة، الأسماء، الأدلة)", "Essential Daily Islamic Tools")
                )
            }
            item {
                ModuleGrid(items = essentialIslamicTools)
            }

            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "الأنظمة الأساسية (الحلقات والحفظ والذكاء والمكتبة)", "Core Learning Systems")
                )
            }
            item {
                ModuleGrid(items = coreLearning)
            }

            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "التعليم والعبادات والأسرة", "Quran Education, Worship & Family")
                )
            }
            item {
                ModuleGrid(items = quranAndEducation)
            }

            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "المراجع الإسلامية والإدارة المحلية", "Islamic References & Local Admin")
                )
            }
            item {
                ModuleGrid(items = referencesAndAdmin)
            }
        }
    }
}

@Composable
private fun ModuleGrid(items: List<EcosystemModuleItem>) {
    val lang = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(item.testTag),
                        contentPadding = PaddingValues(14.dp),
                        onClick = item.onClick
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.accent.copy(alpha = 0.18f))
                                .border(1.dp, item.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.titleEn, tint = item.accent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = L10n.pick(lang, item.titleAr, item.titleEn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.pick(lang, item.subtitleAr, item.subtitleEn),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
