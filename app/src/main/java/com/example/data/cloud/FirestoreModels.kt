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
    val canManageAds: Boolean get() = this == SUPER_ADMIN
    val canControlGlobalFeatures: Boolean get() = this == SUPER_ADMIN
    val canSendAdminMessages: Boolean get() = this == SUPER_ADMIN || this == ADMIN

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
    val featureMushafImagesEnabled: Boolean = true,
    val featureAudioEnabled: Boolean = true,
    val featureMemorizationEnabled: Boolean = true,
    val featureListeningAssessmentEnabled: Boolean = true,
    val featureKhatmahEnabled: Boolean = true,
    val featureFastingEnabled: Boolean = true,
    val featureDailyWirdEnabled: Boolean = true,
    val featureNotificationsEnabled: Boolean = true,
    val featureAdsEnabled: Boolean = true,
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

/**
 * Strictly allowed placements for Administrative Advertisement Cards.
 * Ads are NEVER permitted inside Quran reading, recitation, memorization (Hifz), or Prayer Times.
 */
enum class AdPlacement(val key: String, val labelAr: String, val labelEn: String) {
    HOME("HOME", "الشاشة الرئيسية", "Home Screen"),
    ECOSYSTEM_HUB("ECOSYSTEM_HUB", "مركز منظومة نور", "NOOR Ecosystem Hub"),
    AUDIO_LIBRARY("AUDIO_LIBRARY", "مكتبة الصوتيات", "Audio Library");

    companion object {
        private val FORBIDDEN_ROUTE_TOKENS = setOf(
            "quran", "mushaf", "recitation", "hifz", "memorization", "prayer", "prayers", "adhan"
        )

        fun fromKey(key: String?): AdPlacement =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: HOME

        fun isAllowedPlacement(placementKey: String?): Boolean =
            entries.any { it.key.equals(placementKey, ignoreCase = true) }

        fun isForbiddenScreenRoute(routeOrScreenName: String): Boolean {
            val lower = routeOrScreenName.lowercase()
            return FORBIDDEN_ROUTE_TOKENS.any { token -> lower.contains(token) }
        }
    }
}

object NoorSecurityValidators {
    /**
     * Validates an optional external destination URL for advertisement cards or links.
     * Empty string is allowed (informational card without external link).
     * Non-empty URLs MUST strictly use https:// and must not contain control chars or javascript:.
     */
    fun isSafeOptionalHttpsUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.length > 500) return false
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        val hostPart = trimmed.substringAfter("https://", "").substringBefore("/")
        if (hostPart.isBlank() || !hostPart.contains(".") || hostPart.contains(" ")) return false
        if (trimmed.any { it.isISOControl() }) return false
        return true
    }

    /**
     * Validates an optional image URI/URL (either https:// or local content:// / file:// from Photo Picker).
     */
    fun isSafeOptionalImageUri(uriOrUrl: String): Boolean {
        val trimmed = uriOrUrl.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.length > 600) return false
        if (trimmed.any { it.isISOControl() }) return false
        return trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
    }
}

@Keep
@IgnoreExtraProperties
data class AdCardDoc(
    @DocumentId val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val imageUrl: String = "",
    val destinationUrl: String = "",
    val placement: String = AdPlacement.HOME.key,
    val enabled: Boolean = true,
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L,
    val displayOrder: Int = 1,
    val impressionsCount: Long = 0L,
    val clicksCount: Long = 0L,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isCurrentlyActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!enabled) return false
        if (!AdPlacement.isAllowedPlacement(placement)) return false
        if (startAtMillis > 0L && nowMillis < startAtMillis) return false
        if (endAtMillis > 0L && nowMillis > endAtMillis) return false
        return true
    }
}

enum class AdminMessageType(val key: String, val labelAr: String, val labelEn: String) {
    SYSTEM("SYSTEM", "تنبيه نظام", "System"),
    MAINTENANCE("MAINTENANCE", "صيانة", "Maintenance"),
    FEATURE_UPDATE("FEATURE_UPDATE", "تحديث ميزة", "Feature Update"),
    REMINDER("REMINDER", "تذكير", "Reminder"),
    EDUCATIONAL("EDUCATIONAL", "رسالة تعليمية", "Educational"),
    DIRECT("DIRECT", "رسالة مباشرة", "Direct Message");

    companion object {
        fun fromKey(key: String?): AdminMessageType =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AdminMessagePriority(val key: String, val labelAr: String, val labelEn: String) {
    LOW("LOW", "منخفضة", "Low"),
    NORMAL("NORMAL", "عادية", "Normal"),
    HIGH("HIGH", "مرتفعة", "High"),
    URGENT("URGENT", "عاجلة", "Urgent");

    companion object {
        fun fromKey(key: String?): AdminMessagePriority =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: NORMAL
    }
}

@Keep
@IgnoreExtraProperties
data class AdminMessageDoc(
    @DocumentId val id: String = "",
    val targetType: String = "ALL", // ALL, GROUP, USER
    val targetGroup: String = "ALL", // ALL, ACTIVE, VERIFIED, ADMINS
    val targetUserUid: String = "",
    val targetUserEmail: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyAr: String = "",
    val bodyEn: String = "",
    val messageType: String = AdminMessageType.SYSTEM.key,
    val priority: String = AdminMessagePriority.NORMAL.key,
    val imageUrl: String = "",
    val scheduledAtMillis: Long = 0L,
    val active: Boolean = true,
    val readByUids: List<String> = emptyList(),
    val createdBy: String = "",
    val createdByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

