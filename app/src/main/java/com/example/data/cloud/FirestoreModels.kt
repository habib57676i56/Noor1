package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Verified initial application owner email (bootstrapped Super Admin).
 */
object NoorPlatformConstants {
    const val PRIMARY_OWNER_EMAIL = "mustafa20090520@gmail.com"
    const val APP_VERSION_NAME = "3.0.0"

    // Part 4 — Official Founder Profile (exact verified biography, no invented achievements)
    const val FOUNDER_NAME = "Osama"
    const val FOUNDER_TITLE_EN = "Founder & Creator of NOOR"
    const val FOUNDER_TITLE_AR = "مؤسس ومُبتكر تطبيق نور"
    const val FOUNDER_PROJECT_EN = "NOOR — The Complete Islamic Companion"
    const val FOUNDER_PROJECT_AR = "نور — الرفيق الإسلامي المتكامل"
    const val FOUNDER_BIO_EN =
        "Osama is the founder and creator of NOOR, an Islamic digital platform built with a vision of making authentic Islamic knowledge, daily worship, and spiritual resources more accessible through modern technology.\n\n" +
            "His vision is to combine the timeless values of Islam with thoughtful digital innovation, creating a beautiful and reliable experience for Muslims around the world.\n\n" +
            "NOOR represents a commitment to authenticity, simplicity, privacy, and continuous improvement."
    const val FOUNDER_BIO_AR =
        "أسامة هو مؤسس ومُبتكر منصة نور (NOOR)، وهي منصة إسلامية رقمية بُنيت برؤية تهدف إلى جعل المعرفة الإسلامية الأصيلة والعبادة اليومية والموارد الروحية أكثر سهولة ويسراً عبر التقنية الحديثة.\n\n" +
            "تتمثل رؤيته في الجمع بين القيم الإسلامية الخالدة والابتكار الرقمي المدروس، لتقديم تجربة جميلة وموثوقة للمسلمين حول العالم.\n\n" +
            "يمثل نور التزاماً بالأصالة والبساطة والخصوصية والتطوير المستمر."

    // Part 5 — Official Contact & Support Channels (NO invented Telegram username)
    const val SUPPORT_EMAIL = "habiybullohy@gmail.com"
    const val SUPPORT_WHATSAPP = "+963 984 215 420"
    const val SUPPORT_TELEGRAM = "+963 984 215 420"
    const val SUPPORT_WHATSAPP_URL = "https://wa.me/963984215420"
    const val SUPPORT_TELEGRAM_URL = "https://t.me/+963984215420"
}

enum class AdminRole(val key: String, val labelAr: String, val labelEn: String) {
    USER("USER", "مستخدم", "User"),
    SUPPORT_AGENT("SUPPORT_AGENT", "فريق الدعم", "Support Agent"),
    CONTENT_MODERATOR("CONTENT_MODERATOR", "مراجع المحتوى", "Content Moderator"),
    ADMIN("ADMIN", "مشرف", "Administrator"),
    SUPER_ADMIN("SUPER_ADMIN", "المالك / المشرف الأعلى", "Super Admin (Owner)");

    val isPrivileged: Boolean get() = this != USER
    val canManageContent: Boolean get() = this == SUPER_ADMIN || this == ADMIN || this == CONTENT_MODERATOR
    val canManageSupport: Boolean get() = this == SUPER_ADMIN || this == ADMIN || this == SUPPORT_AGENT
    val canManageUsers: Boolean get() = this == SUPER_ADMIN || this == ADMIN
    val canAssignRoles: Boolean get() = this == SUPER_ADMIN

    companion object {
        fun fromKey(key: String?): AdminRole =
            entries.firstOrNull { it.key == key } ?: USER
    }
}

enum class CmsWorkflowStatus(val key: String, val labelAr: String, val labelEn: String) {
    DRAFT("DRAFT", "مسودة", "Draft"),
    REVIEW("REVIEW", "قيد المراجعة", "In Review"),
    VERIFIED("VERIFIED", "موثّق", "Verified"),
    PUBLISHED("PUBLISHED", "منشور", "Published");

    companion object {
        fun fromKey(key: String?): CmsWorkflowStatus =
            entries.firstOrNull { it.key == key } ?: DRAFT
    }
}

/**
 * All 16 CMS Content Categories manageable in the Super Admin Control Center.
 */
enum class CmsContentCategory(val key: String, val labelAr: String, val labelEn: String) {
    QURAN_RESOURCES("quran_resources", "موارد القرآن الكريم", "Quran Resources"),
    TAFSIR("tafsir", "التفسير", "Tafsir"),
    MORNING_ADHKAR("morning_adhkar", "أذكار الصباح", "Morning Adhkar"),
    EVENING_ADHKAR("evening_adhkar", "أذكار المساء", "Evening Adhkar"),
    DAILY_ADHKAR("daily_adhkar", "الأذكار اليومية", "Daily Adhkar"),
    ISLAMIC_DUAS("islamic_duas", "الأدعية الإسلامية", "Islamic Duas"),
    HADITH("hadith", "الأحاديث النبوية", "Hadith"),
    AUDIO_RECITATIONS("audio_recitations", "التلاوات الصوتية", "Audio Recitations"),
    ADHAN_RECORDINGS("adhan_recordings", "تسجيلات الأذان", "Adhan Recordings"),
    ISLAMIC_CALENDAR("islamic_calendar", "التقويم الهجري", "Islamic Calendar"),
    DAILY_VERSES("daily_verses", "آيات اليوم", "Daily Verses"),
    DAILY_HADITH("daily_hadith", "أحاديث اليوم", "Daily Hadith"),
    NOTIFICATIONS("notifications", "قوالب الإشعارات", "Notifications"),
    EDUCATIONAL_ARTICLES("educational_articles", "المقالات التعليمية", "Educational Articles"),
    ISLAMIC_EVENTS("islamic_events", "المناسبات الإسلامية", "Islamic Events"),
    MOSQUE_INFO("mosque_info", "بيانات المساجد", "Mosque Information");

    companion object {
        fun fromKey(key: String?): CmsContentCategory =
            entries.firstOrNull { it.key == key } ?: MORNING_ADHKAR
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccountDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val verified: Boolean = false,
    val preferredLanguage: String = "ar",
    val cityName: String = "Makkah",
    val adhkarCompleted: Long = 0L,
    val quranVersesRead: Long = 0L,
    val audioPlays: Long = 0L,
    val tasbihCount: Long = 0L,
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRoleDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val role: String = "ADMIN",
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    val grantedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CmsContentDoc(
    @DocumentId val id: String = "",
    val category: String = CmsContentCategory.MORNING_ADHKAR.key,
    val subCategory: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyAr: String = "",
    val bodyEn: String = "",
    val transliteration: String = "",
    val sourceAr: String = "",
    val sourceEn: String = "",
    val gradingAr: String = "صحيح",
    val gradingEn: String = "Sahih",
    val repetitionCount: Int = 1,
    val audioUrl: String = "",
    val status: String = CmsWorkflowStatus.DRAFT.key,
    val version: Int = 1,
    val scheduledAtMillis: Long = 0L,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CmsRevisionDoc(
    @DocumentId val id: String = "",
    val contentId: String = "",
    val version: Int = 1,
    val status: String = CmsWorkflowStatus.DRAFT.key,
    val bodyAr: String = "",
    val bodyEn: String = "",
    val sourceAr: String = "",
    val repetitionCount: Int = 1,
    val changeSummary: String = "",
    val editorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GlobalAppConfigDoc(
    @DocumentId val id: String = "global",
    val appNameAr: String = "نور",
    val appNameEn: String = "NOOR",
    val themePreset: String = "emerald_gold",
    val defaultLanguage: String = "ar",
    val supportedLanguages: List<String> = listOf("ar", "en"),
    val maintenanceMode: Boolean = false,
    val maintenanceMessageAr: String = "نقوم حالياً بتحديثات مجدولة لتحسين تجربة نور. سنعود قريباً بإذن الله.",
    val maintenanceMessageEn: String = "NOOR is undergoing scheduled maintenance to serve you better. We will be back shortly.",
    val minSupportedVersion: String = "3.0.0",
    val founderName: String = NoorPlatformConstants.FOUNDER_NAME,
    val founderTitleAr: String = NoorPlatformConstants.FOUNDER_TITLE_AR,
    val founderTitleEn: String = NoorPlatformConstants.FOUNDER_TITLE_EN,
    val founderProjectAr: String = NoorPlatformConstants.FOUNDER_PROJECT_AR,
    val founderProjectEn: String = NoorPlatformConstants.FOUNDER_PROJECT_EN,
    val founderBioAr: String = NoorPlatformConstants.FOUNDER_BIO_AR,
    val founderBioEn: String = NoorPlatformConstants.FOUNDER_BIO_EN,
    val supportEmail: String = NoorPlatformConstants.SUPPORT_EMAIL,
    val supportWhatsApp: String = NoorPlatformConstants.SUPPORT_WHATSAPP,
    val supportTelegram: String = NoorPlatformConstants.SUPPORT_TELEGRAM,
    val featureQuranEnabled: Boolean = true,
    val featureAudioEnabled: Boolean = true,
    val featureMosquesEnabled: Boolean = true,
    val featureQiblaEnabled: Boolean = true,
    val featureCalendarEnabled: Boolean = true,
    val defaultCalcMethod: String = "umm_al_qura",
    val defaultReciterId: String = "yasser_dosari",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppAnnouncementDoc(
    @DocumentId val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyAr: String = "",
    val bodyEn: String = "",
    val kind: String = "GENERAL",
    val active: Boolean = true,
    val scheduledAtMillis: Long = 0L,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicketDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val ticketType: String = "SUPPORT",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserReportDoc(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "content",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogDoc(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val category: String = "SYSTEM",
    val severity: String = "INFO",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyAnalyticsDoc(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val activeUsers: Long = 0L,
    val newRegistrations: Long = 0L,
    val quranVersesRead: Long = 0L,
    val adhkarCompleted: Long = 0L,
    val audioPlays: Long = 0L,
    val tasbihTaps: Long = 0L,
    val sectionHomeHits: Long = 0L,
    val sectionQuranHits: Long = 0L,
    val sectionAdhkarHits: Long = 0L,
    val sectionAudioHits: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
