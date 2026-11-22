package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorApplication
import com.example.data.cloud.AdminRole
import com.example.data.cloud.AdminSecurityRateLimiter
import com.example.data.cloud.AppAnnouncementDoc
import com.example.data.cloud.AuditLogDoc
import com.example.data.cloud.CmsContentCategory
import com.example.data.cloud.CmsContentDoc
import com.example.data.cloud.CmsRevisionDoc
import com.example.data.cloud.CmsWorkflowStatus
import com.example.data.cloud.DailyAnalyticsDoc
import com.example.data.cloud.FirebaseAuthHelper
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorCloudRepository
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.cloud.SupportTicketDoc
import com.example.data.cloud.UserAccountDoc
import com.example.data.cloud.UserReportDoc
import com.example.data.local.catalog.AdhkarCatalog
import com.example.data.local.db.BackupSnapshotEntity
import com.example.data.local.db.SystemErrorLogEntity
import com.example.data.repository.NoorRepository
import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem
import com.example.domain.verification.AdhkarVerificationEngine
import com.example.domain.verification.AdhkarVerificationReport
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardKpis(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val suspendedUsers: Int = 0,
    val verifiedUsers: Int = 0,
    val dailyActiveUsers: Long = 0L,
    val monthlyActiveUsers: Long = 0L,
    val newRegistrationsToday: Long = 0L,
    val totalCmsItems: Int = 0,
    val publishedCmsItems: Int = 0,
    val draftCmsItems: Int = 0,
    val openSupportTickets: Int = 0,
    val pendingUserReports: Int = 0,
    val totalQuranVersesRead: Long = 0L,
    val totalAdhkarCompleted: Long = 0L,
    val totalAudioPlays: Long = 0L,
    val totalTasbihTaps: Long = 0L,
    val sectionHits: Map<String, Long> = emptyMap(),
    val systemHealthScore: Int = 100
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo: NoorRepository = (application as NoorApplication).repository
    private val cloudRepo: NoorCloudRepository = (application as NoorApplication).cloudRepository

    val rateLimiter = AdminSecurityRateLimiter()

    private val _currentRole = MutableStateFlow(AdminRole.USER)
    val currentRole: StateFlow<AdminRole> = _currentRole.asStateFlow()

    private val _isPrivilegedUnlocked = MutableStateFlow(false)
    val isPrivilegedUnlocked: StateFlow<Boolean> = _isPrivilegedUnlocked.asStateFlow()

    private val _adminFeedback = MutableStateFlow<String?>(null)
    val adminFeedback: StateFlow<String?> = _adminFeedback.asStateFlow()

    private val _storageMetrics = MutableStateFlow(NoorRepository.StorageMetrics())
    val storageMetrics: StateFlow<NoorRepository.StorageMetrics> = _storageMetrics.asStateFlow()

    private val _selectedRevisionContentId = MutableStateFlow<String?>(null)

    private val isAuthorizedStream: StateFlow<Boolean> = _currentRole
        .map { it.isPrivileged }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val allUsers: StateFlow<List<UserAccountDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAllUsersAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCmsContent: StateFlow<List<CmsContentDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAllCmsContentAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<AppAnnouncementDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAllAnnouncementsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupportTickets: StateFlow<List<SupportTicketDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAllSupportTicketsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserReports: StateFlow<List<UserReportDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeUserReportsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAuditLogsAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAnalytics: StateFlow<List<DailyAnalyticsDoc>> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeAnalyticsDailyAdmin() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalConfig: StateFlow<GlobalAppConfigDoc> = isAuthorizedStream
        .flatMapLatest { ok -> if (ok) cloudRepo.observeGlobalAppConfig() else flowOf(GlobalAppConfigDoc()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalAppConfigDoc())

    val selectedRevisions: StateFlow<List<CmsRevisionDoc>> = combine(
        isAuthorizedStream,
        _selectedRevisionContentId
    ) { ok, contentId -> ok to contentId }
        .flatMapLatest { (ok, contentId) ->
            if (ok && !contentId.isNullOrBlank()) cloudRepo.observeCmsRevisionsAdmin(contentId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backupSnapshots: StateFlow<List<BackupSnapshotEntity>> = localRepo.backupSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemErrorLogs: StateFlow<List<SystemErrorLogEntity>> = localRepo.systemErrorLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adhkarVerificationReport: StateFlow<AdhkarVerificationReport> = allCmsContent
        .map { cmsItems ->
            val cmsAsDhikr = cmsItems
                .filter {
                    it.category == CmsContentCategory.MORNING_ADHKAR.key ||
                        it.category == CmsContentCategory.EVENING_ADHKAR.key
                }
                .map { c ->
                    DhikrItem(
                        id = c.id,
                        category = if (c.category == CmsContentCategory.EVENING_ADHKAR.key) AdhkarCategory.EVENING else AdhkarCategory.MORNING,
                        textAr = c.bodyAr,
                        targetCount = c.repetitionCount,
                        sourceAr = c.sourceAr,
                        sourceEn = c.sourceEn,
                        gradingAr = c.gradingAr,
                        gradingEn = c.gradingEn,
                        meaningAr = c.titleAr,
                        meaningEn = c.bodyEn
                    )
                }
            AdhkarVerificationEngine.verifyCollection(AdhkarCatalog.allAdhkar + cmsAsDhikr)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AdhkarVerificationEngine.verifyCollection(AdhkarCatalog.allAdhkar)
        )

    val dashboardKpis: StateFlow<AdminDashboardKpis> = combine(
        allUsers,
        allCmsContent,
        allSupportTickets,
        allUserReports,
        dailyAnalytics
    ) { users, cms, tickets, reports, analytics ->
        val latest = analytics.lastOrNull()
        val activeUsers = users.count { it.status == "ACTIVE" }
        val suspended = users.count { it.status == "SUSPENDED" }
        val verified = users.count { it.verified }
        val totalQuran = analytics.sumOf { it.quranVersesRead } + users.sumOf { it.quranVersesRead }
        val totalAdhkar = analytics.sumOf { it.adhkarCompleted } + users.sumOf { it.adhkarCompleted }
        val totalAudio = analytics.sumOf { it.audioPlays } + users.sumOf { it.audioPlays }
        val totalTasbih = analytics.sumOf { it.tasbihTaps } + users.sumOf { it.tasbihCount }
        val mau = analytics.sumOf { it.activeUsers }.coerceAtLeast(activeUsers.toLong())
        val dau = (latest?.activeUsers ?: 0L).coerceAtLeast(if (activeUsers > 0) 1L else 0L)
        val sections = mapOf(
            "Home" to analytics.sumOf { it.sectionHomeHits },
            "Quran" to analytics.sumOf { it.sectionQuranHits },
            "Adhkar" to analytics.sumOf { it.sectionAdhkarHits },
            "Audio" to analytics.sumOf { it.sectionAudioHits }
        )
        val openTickets = tickets.count { it.status != "RESOLVED" }
        val pendingReports = reports.count { it.status == "PENDING" }
        val health = (100 - (pendingReports * 3) - (openTickets * 2)).coerceIn(60, 100)

        AdminDashboardKpis(
            totalUsers = users.size,
            activeUsers = activeUsers,
            suspendedUsers = suspended,
            verifiedUsers = verified,
            dailyActiveUsers = dau,
            monthlyActiveUsers = mau,
            newRegistrationsToday = latest?.newRegistrations ?: 0L,
            totalCmsItems = cms.size + AdhkarCatalog.allAdhkar.size,
            publishedCmsItems = cms.count { it.status == CmsWorkflowStatus.PUBLISHED.key } + AdhkarCatalog.allAdhkar.size,
            draftCmsItems = cms.count { it.status != CmsWorkflowStatus.PUBLISHED.key },
            openSupportTickets = openTickets,
            pendingUserReports = pendingReports,
            totalQuranVersesRead = totalQuran,
            totalAdhkarCompleted = totalAdhkar,
            totalAudioPlays = totalAudio,
            totalTasbihTaps = totalTasbih,
            sectionHits = sections,
            systemHealthScore = health
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardKpis())

    init {
        refreshStorageMetrics()
    }

    fun syncAuthorizedRole(role: AdminRole) {
        _currentRole.value = role
        if (!role.isPrivileged) {
            _isPrivilegedUnlocked.value = false
        }
    }

    fun clearFeedback() {
        _adminFeedback.value = null
    }

    /**
     * Secondary security step (MFA / Owner Confirmation) layered on top of verified Google Sign-In
     * before destructive operations or Super Admin elevation. Protected by rate limiter.
     */
    fun verifySecondFactorEmailMatch(enteredEmail: String): Boolean {
        val now = System.currentTimeMillis()
        if (rateLimiter.isLocked(now)) {
            val sec = rateLimiter.remainingLockSeconds(now)
            _adminFeedback.value = "Security lock active. Retry in ${sec}s."
            return false
        }
        val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        return if (enteredEmail.trim().equals(currentEmail, ignoreCase = true) && currentEmail.isNotBlank()) {
            rateLimiter.recordSuccess()
            _isPrivilegedUnlocked.value = true
            viewModelScope.launch {
                cloudRepo.recordAuditLog("MFA_VERIFIED", "SECURITY", "INFO", "Secondary email identity verification passed")
            }
            _adminFeedback.value = "Privileged session verified"
            true
        } else {
            val locked = rateLimiter.recordFailure(now)
            viewModelScope.launch {
                cloudRepo.recordAuditLog(
                    "MFA_FAILED",
                    "SECURITY",
                    if (locked) "CRITICAL" else "WARNING",
                    "Failed admin confirmation attempt (locked=$locked)"
                )
            }
            _adminFeedback.value = if (locked) {
                "Too many failed attempts. Locked for 2 minutes."
            } else {
                "Verification failed (${rateLimiter.failedAttemptCount(now)}/5)"
            }
            false
        }
    }

    fun executeEmergencyOwnerRecovery() = viewModelScope.launch {
        val user = FirebaseAuth.getInstance().currentUser
        if (!FirebaseAuthHelper.isBootstrappedOwner(user)) {
            _adminFeedback.value = "Emergency recovery is strictly restricted to the verified primary Owner account."
            return@launch
        }
        val res = cloudRepo.syncCurrentUserProfile(preferredLanguage = "ar", cityName = "Makkah")
        if (res.isSuccess) {
            cloudRepo.recordAuditLog("OWNER_EMERGENCY_RECOVERY", "SECURITY", "CRITICAL", "Re-verified primary owner Super Admin access")
            _currentRole.value = AdminRole.SUPER_ADMIN
            _isPrivilegedUnlocked.value = true
            _adminFeedback.value = "Owner Super Admin access verified and restored."
        } else {
            _adminFeedback.value = "Recovery error: ${res.exceptionOrNull()?.message}"
        }
    }

    // --- User Management Actions ---

    fun updateUser(
        user: UserAccountDoc,
        newDisplayName: String,
        newStatus: String,
        newVerified: Boolean,
        newRole: AdminRole
    ) = viewModelScope.launch {
        if (!_currentRole.value.canManageUsers) {
            _adminFeedback.value = "Insufficient role permission to edit users."
            return@launch
        }
        if (newRole.key != user.role && !_currentRole.value.canAssignRoles) {
            _adminFeedback.value = "Only the verified Super Admin / Owner may assign or revoke roles."
            return@launch
        }
        val result = cloudRepo.updateUserByAdmin(
            targetUid = user.uid,
            displayName = newDisplayName,
            status = newStatus,
            verified = newVerified,
            role = newRole.key,
            targetEmail = user.email
        )
        _adminFeedback.value = if (result.isSuccess) {
            "User ${user.email} updated"
        } else {
            result.exceptionOrNull()?.message ?: "Update failed"
        }
    }

    fun deleteUser(user: UserAccountDoc) = viewModelScope.launch {
        if (_currentRole.value != AdminRole.SUPER_ADMIN) {
            _adminFeedback.value = "Only the Super Admin / Owner may delete accounts."
            return@launch
        }
        val result = cloudRepo.deleteUserAccountAdmin(user.uid, user.email)
        _adminFeedback.value = if (result.isSuccess) {
            "User account removed with audit log"
        } else {
            result.exceptionOrNull()?.message ?: "Delete failed"
        }
    }

    fun resolveUserReport(reportId: String, status: String, note: String) = viewModelScope.launch {
        val result = cloudRepo.resolveUserReport(reportId, status, note)
        _adminFeedback.value = if (result.isSuccess) "Report marked $status" else "Failed to update report"
    }

    // --- CMS Content Management & Revisions ---

    fun selectContentForRevisions(contentId: String?) {
        _selectedRevisionContentId.value = contentId
    }

    fun saveCmsItem(item: CmsContentDoc, changeSummary: String) = viewModelScope.launch {
        if (!_currentRole.value.canManageContent) {
            _adminFeedback.value = "Insufficient permission to edit CMS content."
            return@launch
        }
        if (item.titleAr.isBlank() || item.bodyAr.isBlank() || item.sourceAr.isBlank()) {
            _adminFeedback.value = "Arabic title, Arabic text, and scholarly source are required."
            return@launch
        }
        val result = cloudRepo.upsertCmsContent(item, changeSummary.ifBlank { "Updated via Admin CMS" })
        _adminFeedback.value = if (result.isSuccess) {
            "CMS content saved (${item.status})"
        } else {
            result.exceptionOrNull()?.message ?: "CMS save failed"
        }
    }

    fun restoreCmsRevision(item: CmsContentDoc, rev: CmsRevisionDoc) = viewModelScope.launch {
        val restored = item.copy(
            bodyAr = rev.bodyAr,
            bodyEn = rev.bodyEn,
            sourceAr = rev.sourceAr.ifBlank { item.sourceAr },
            repetitionCount = rev.repetitionCount.coerceAtLeast(1),
            status = rev.status
        )
        saveCmsItem(restored, "Restored from version v${rev.version}")
    }

    fun deleteCmsItem(contentId: String) = viewModelScope.launch {
        val result = cloudRepo.deleteCmsContent(contentId)
        _adminFeedback.value = if (result.isSuccess) "CMS entry deleted" else "Delete failed"
    }

    // --- Global App Settings, Founder Profile, Contact & Maintenance ---

    fun saveGlobalConfig(config: GlobalAppConfigDoc) = viewModelScope.launch {
        if (_currentRole.value != AdminRole.SUPER_ADMIN) {
            _adminFeedback.value = "Only Super Admin / Owner can modify global settings."
            return@launch
        }
        val result = cloudRepo.saveGlobalAppConfig(config)
        _adminFeedback.value = if (result.isSuccess) {
            "Live global settings published across NOOR"
        } else {
            result.exceptionOrNull()?.message ?: "Settings save failed"
        }
    }

    // --- Announcements & Support Tickets ---

    fun publishAnnouncement(
        id: String? = null,
        titleAr: String,
        titleEn: String,
        bodyAr: String,
        bodyEn: String,
        kind: String,
        active: Boolean = true,
        scheduledAtMillis: Long = 0L
    ) = viewModelScope.launch {
        val res = cloudRepo.publishAnnouncement(id, titleAr, titleEn, bodyAr, bodyEn, kind, active, scheduledAtMillis)
        _adminFeedback.value = if (res.isSuccess) "Announcement published" else "Announcement publish failed"
    }

    fun deleteAnnouncement(id: String) = viewModelScope.launch {
        val res = cloudRepo.deleteAnnouncement(id)
        _adminFeedback.value = if (res.isSuccess) "Announcement removed" else "Delete failed"
    }

    fun replySupportTicket(ticketId: String, status: String, reply: String) = viewModelScope.launch {
        val res = cloudRepo.replyToSupportTicket(ticketId, status, reply)
        _adminFeedback.value = if (res.isSuccess) "Support ticket updated ($status)" else "Ticket update failed"
    }

    // --- System Management: Backups, Snapshots, Content Export/Import, Cache ---

    fun refreshStorageMetrics() = viewModelScope.launch {
        _storageMetrics.value = localRepo.computeStorageMetrics()
    }

    fun createRollbackSnapshot(label: String) = viewModelScope.launch {
        localRepo.createRollbackSnapshot(label)
        cloudRepo.recordAuditLog("CREATE_SNAPSHOT", "SYSTEM", "INFO", "Created local rollback snapshot: $label")
        refreshStorageMetrics()
        _adminFeedback.value = "Rollback snapshot created"
    }

    fun restoreRollbackSnapshot(snapshot: BackupSnapshotEntity) = viewModelScope.launch {
        val ok = localRepo.importBackupJson(snapshot.payloadJson)
        cloudRepo.recordAuditLog("RESTORE_SNAPSHOT", "SYSTEM", "CRITICAL", "Restored snapshot #${snapshot.id} (${snapshot.label}): ok=$ok")
        _adminFeedback.value = if (ok) "Snapshot '${snapshot.label}' restored" else "Snapshot restore failed"
    }

    fun deleteRollbackSnapshot(id: Long) = viewModelScope.launch {
        localRepo.deleteRollbackSnapshot(id)
        refreshStorageMetrics()
    }

    fun clearTemporaryCaches() = viewModelScope.launch {
        localRepo.clearTemporaryCaches()
        cloudRepo.recordAuditLog("CLEAR_CACHE", "SYSTEM", "INFO", "Cleared temporary audio and app caches")
        refreshStorageMetrics()
        _adminFeedback.value = "Temporary caches cleared"
    }

    fun clearSystemErrorLogs() = viewModelScope.launch {
        localRepo.clearSystemErrorLogs()
        _adminFeedback.value = "System error logs cleared"
    }

    fun exportAllCmsJson(): String = localRepo.exportCmsItemsToJson(allCmsContent.value)

    fun importCmsJson(json: String) = viewModelScope.launch {
        val parsed = localRepo.parseCmsItemsFromJson(json)
        if (parsed.isEmpty()) {
            _adminFeedback.value = "No valid NOOR CMS items found in JSON"
            return@launch
        }
        var imported = 0
        parsed.forEach { item ->
            val res = cloudRepo.upsertCmsContent(item.copy(status = CmsWorkflowStatus.DRAFT.key), "Imported via JSON")
            if (res.isSuccess) imported++
        }
        _adminFeedback.value = "Imported $imported CMS entries as DRAFT for verification"
    }
}
