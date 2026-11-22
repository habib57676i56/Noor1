package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Cloud repository connected strictly to the provisioned named Firestore database
 * (`R.string.firestore_database_id`) and Firebase Auth.
 */
class NoorCloudRepository(private val appContext: Context) {

    private val databaseId: String = appContext.getString(R.string.firestore_database_id)
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance(databaseId) }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // -------------------------------------------------------------------------
    // Current User Profile & Role Resolution
    // -------------------------------------------------------------------------

    suspend fun syncCurrentUserProfile(
        preferredLanguage: String,
        cityName: String
    ): Result<UserAccountDoc> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snap = docRef.get().await()
            val isOwner = FirebaseAuthHelper.isBootstrappedOwner(user)

            if (!snap.exists()) {
                val initialRole = if (isOwner) AdminRole.SUPER_ADMIN.key else AdminRole.USER.key
                val payload = mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName?.take(80) ?: user.email?.substringBefore("@") ?: "NOOR Member"),
                    "role" to initialRole,
                    "status" to "ACTIVE",
                    "verified" to isOwner,
                    "preferredLanguage" to preferredLanguage,
                    "cityName" to cityName,
                    "adhkarCompleted" to 0L,
                    "quranVersesRead" to 0L,
                    "audioPlays" to 0L,
                    "tasbihCount" to 0L,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                if (isOwner) {
                    ensureOwnerAdminRecord(user)
                }
                recordAuditLog("USER_REGISTERED", "AUTH", "INFO", "Account initialized (${initialRole})")
                incrementDailyAnalytics(newRegDelta = 1L, activeUsersDelta = 1L)
            } else {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to preferredLanguage,
                        "cityName" to cityName,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (isOwner) {
                    ensureOwnerAdminRecord(user)
                }
                recordAuditLog("USER_SIGN_IN", "AUTH", "INFO", "Session verified via Google Sign-In")
                incrementDailyAnalytics(activeUsersDelta = 1L)
            }
            val fresh = docRef.get().await().toObject(UserAccountDoc::class.java)
                ?: UserAccountDoc(uid = user.uid, email = user.email ?: "", role = if (isOwner) "SUPER_ADMIN" else "USER")
            Result.success(fresh)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
            Result.failure(e)
        }
    }

    private suspend fun ensureOwnerAdminRecord(user: FirebaseUser) {
        try {
            val adminRef = db.collection("admins").document(user.uid)
            val snap = adminRef.get().await()
            if (!snap.exists()) {
                adminRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: NoorPlatformConstants.PRIMARY_OWNER_EMAIL),
                        "role" to AdminRole.SUPER_ADMIN.key,
                        "permissions" to listOf("ALL", "GRANT_SUPER_ADMIN", "CMS_ALL", "USERS_ALL", "SYSTEM_ALL"),
                        "active" to true,
                        "grantedBy" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "admins/${user.uid}")
        }
    }

    fun observeCurrentUserProfile(uid: String): Flow<UserAccountDoc?> = callbackFlow {
        val reg = db.collection("users").document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                FirestoreErrorHandler.handleFirestoreError(error, OperationType.GET, "users/$uid")
                return@addSnapshotListener
            }
            trySend(snap?.toObject(UserAccountDoc::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun observeMyAdminRecord(uid: String): Flow<AdminRoleDoc?> = callbackFlow {
        val reg = db.collection("admins").document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                // Non-admins get null quietly without breaking UI
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject(AdminRoleDoc::class.java))
        }
        awaitClose { reg.remove() }
    }

    // -------------------------------------------------------------------------
    // Live Global App Config (App Name, Theme, Founder Profile, Contact, Flags)
    // -------------------------------------------------------------------------

    fun observeGlobalAppConfig(): Flow<GlobalAppConfigDoc> = callbackFlow {
        val reg = db.collection("app_config").document("global").addSnapshotListener { snap, error ->
            if (error != null) {
                FirestoreErrorHandler.handleFirestoreError(error, OperationType.GET, "app_config/global")
                trySend(GlobalAppConfigDoc())
                return@addSnapshotListener
            }
            trySend(snap?.toObject(GlobalAppConfigDoc::class.java) ?: GlobalAppConfigDoc())
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveGlobalAppConfig(config: GlobalAppConfigDoc): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            val payload = mapOf(
                "appNameAr" to config.appNameAr.trim().ifBlank { "نور" }.take(60),
                "appNameEn" to config.appNameEn.trim().ifBlank { "NOOR" }.take(60),
                "themePreset" to config.themePreset,
                "defaultLanguage" to config.defaultLanguage,
                "supportedLanguages" to config.supportedLanguages,
                "maintenanceMode" to config.maintenanceMode,
                "maintenanceMessageAr" to config.maintenanceMessageAr.take(400),
                "maintenanceMessageEn" to config.maintenanceMessageEn.take(400),
                "minSupportedVersion" to config.minSupportedVersion.take(20),
                "founderName" to config.founderName.trim().ifBlank { NoorPlatformConstants.FOUNDER_NAME },
                "founderTitleAr" to config.founderTitleAr.trim(),
                "founderTitleEn" to config.founderTitleEn.trim(),
                "founderProjectAr" to config.founderProjectAr.trim(),
                "founderProjectEn" to config.founderProjectEn.trim(),
                "founderBioAr" to config.founderBioAr.trim(),
                "founderBioEn" to config.founderBioEn.trim(),
                "supportEmail" to config.supportEmail.trim().ifBlank { NoorPlatformConstants.SUPPORT_EMAIL },
                "supportWhatsApp" to config.supportWhatsApp.trim().ifBlank { NoorPlatformConstants.SUPPORT_WHATSAPP },
                "supportTelegram" to config.supportTelegram.trim().ifBlank { NoorPlatformConstants.SUPPORT_TELEGRAM },
                "featureQuranEnabled" to config.featureQuranEnabled,
                "featureAudioEnabled" to config.featureAudioEnabled,
                "featureMosquesEnabled" to config.featureMosquesEnabled,
                "featureQiblaEnabled" to config.featureQiblaEnabled,
                "featureCalendarEnabled" to config.featureCalendarEnabled,
                "defaultCalcMethod" to config.defaultCalcMethod,
                "defaultReciterId" to config.defaultReciterId,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("global").set(payload).await()
            recordAuditLog("UPDATE_GLOBAL_CONFIG", "SETTINGS", "WARNING", "Updated live global settings (maintenance=${config.maintenanceMode})")
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "app_config/global")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Announcements & Notification Center
    // -------------------------------------------------------------------------

    fun observeActiveAnnouncements(): Flow<List<AppAnnouncementDoc>> = callbackFlow {
        val reg = db.collection("announcements")
            .whereEqualTo("active", true)
            .limit(30)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "announcements")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(AppAnnouncementDoc::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAllAnnouncementsAdmin(): Flow<List<AppAnnouncementDoc>> = callbackFlow {
        val reg = db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "announcements")
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toObject(AppAnnouncementDoc::class.java) }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun publishAnnouncement(
        id: String?,
        titleAr: String,
        titleEn: String,
        bodyAr: String,
        bodyEn: String,
        kind: String,
        active: Boolean,
        scheduledAtMillis: Long = 0L
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docId = id?.takeIf { it.isNotBlank() } ?: "ann_${UUID.randomUUID().toString().take(10)}"
        return try {
            val payload = mapOf(
                "titleAr" to titleAr.trim().take(200),
                "titleEn" to titleEn.trim().take(200),
                "bodyAr" to bodyAr.trim().take(2000),
                "bodyEn" to bodyEn.trim().take(2000),
                "kind" to kind,
                "active" to active,
                "scheduledAtMillis" to scheduledAtMillis,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("announcements").document(docId).set(payload).await()
            recordAuditLog("PUBLISH_ANNOUNCEMENT", "SETTINGS", "INFO", "Published announcement $docId ($kind)")
            Result.success(docId)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "announcements/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> = try {
        db.collection("announcements").document(id).delete().await()
        recordAuditLog("DELETE_ANNOUNCEMENT", "SETTINGS", "WARNING", "Deleted announcement $id")
        Result.success(Unit)
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "announcements/$id")
        Result.failure(e)
    }

    // -------------------------------------------------------------------------
    // Islamic Content CMS (All 16 Categories + Workflow + Revisions)
    // -------------------------------------------------------------------------

    fun observePublishedCmsContent(): Flow<List<CmsContentDoc>> = callbackFlow {
        val reg = db.collection("cms_content")
            .whereEqualTo("status", CmsWorkflowStatus.PUBLISHED.key)
            .limit(200)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "cms_content")
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toObject(CmsContentDoc::class.java) }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeAllCmsContentAdmin(): Flow<List<CmsContentDoc>> = callbackFlow {
        val reg = db.collection("cms_content")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(250)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "cms_content")
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toObject(CmsContentDoc::class.java) }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeCmsRevisionsAdmin(contentId: String): Flow<List<CmsRevisionDoc>> = callbackFlow {
        val reg = db.collection("cms_revisions")
            .whereEqualTo("contentId", contentId)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "cms_revisions")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(CmsRevisionDoc::class.java) }
                    ?.sortedByDescending { it.version }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun upsertCmsContent(
        item: CmsContentDoc,
        changeSummary: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docId = item.id.ifBlank { "cms_${UUID.randomUUID().toString().take(10)}" }
        val nextVersion = if (item.id.isBlank()) 1 else (item.version + 1).coerceAtLeast(1)
        return try {
            val payload = mapOf(
                "category" to item.category,
                "subCategory" to item.subCategory.take(64),
                "titleAr" to item.titleAr.trim().take(300),
                "titleEn" to item.titleEn.trim().take(300),
                "bodyAr" to item.bodyAr.trim().take(12000),
                "bodyEn" to item.bodyEn.trim().take(12000),
                "transliteration" to item.transliteration.trim().take(4000),
                "sourceAr" to item.sourceAr.trim().take(400),
                "sourceEn" to item.sourceEn.trim().take(400),
                "gradingAr" to item.gradingAr.trim().take(120),
                "gradingEn" to item.gradingEn.trim().take(120),
                "repetitionCount" to item.repetitionCount.coerceIn(1, 1000),
                "audioUrl" to item.audioUrl.trim().take(500),
                "status" to item.status,
                "version" to nextVersion,
                "scheduledAtMillis" to item.scheduledAtMillis.coerceAtLeast(0L),
                "updatedBy" to uid,
                "createdAt" to (item.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("cms_content").document(docId).set(payload).await()

            val revId = "rev_${docId}_v${nextVersion}_${System.currentTimeMillis() % 10000}"
            db.collection("cms_revisions").document(revId).set(
                mapOf(
                    "contentId" to docId,
                    "version" to nextVersion,
                    "status" to item.status,
                    "bodyAr" to item.bodyAr.trim().take(12000),
                    "bodyEn" to item.bodyEn.trim().take(12000),
                    "sourceAr" to item.sourceAr.trim().take(400),
                    "repetitionCount" to item.repetitionCount.coerceIn(1, 1000),
                    "changeSummary" to changeSummary.take(500),
                    "editorUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordAuditLog("CMS_UPSERT", "CMS", "INFO", "Saved $docId v$nextVersion [${item.category}] -> ${item.status}")
            Result.success(docId)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "cms_content/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteCmsContent(contentId: String): Result<Unit> = try {
        db.collection("cms_content").document(contentId).delete().await()
        recordAuditLog("CMS_DELETE", "CMS", "WARNING", "Deleted CMS item $contentId")
        Result.success(Unit)
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "cms_content/$contentId")
        Result.failure(e)
    }

    // -------------------------------------------------------------------------
    // User Management & RBAC (Admin Dashboard)
    // -------------------------------------------------------------------------

    fun observeAllUsersAdmin(): Flow<List<UserAccountDoc>> = callbackFlow {
        val reg = db.collection("users")
            .limit(200)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "users")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(UserAccountDoc::class.java) }
                    ?.sortedByDescending { it.lastActiveAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateUserByAdmin(
        targetUid: String,
        displayName: String,
        status: String,
        verified: Boolean,
        role: String,
        targetEmail: String
    ): Result<Unit> {
        val actorUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (targetEmail.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true) &&
            (role != AdminRole.SUPER_ADMIN.key || status != "ACTIVE")
        ) {
            return Result.failure(IllegalStateException("Cannot demote or suspend the primary verified Owner."))
        }
        return try {
            db.collection("users").document(targetUid).update(
                mapOf(
                    "displayName" to displayName.trim().take(100),
                    "status" to status,
                    "verified" to verified,
                    "role" to role,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val adminRole = AdminRole.fromKey(role)
            if (adminRole.isPrivileged && status == "ACTIVE") {
                db.collection("admins").document(targetUid).set(
                    mapOf(
                        "uid" to targetUid,
                        "email" to targetEmail,
                        "role" to adminRole.key,
                        "permissions" to permissionsForRole(adminRole),
                        "active" to true,
                        "grantedBy" to actorUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (!targetEmail.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true)) {
                db.collection("admins").document(targetUid).delete().await()
            }

            recordAuditLog("USER_ADMIN_UPDATE", "USER_MGMT", "WARNING", "Updated $targetUid -> role=$role, status=$status, verified=$verified")
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountAdmin(targetUid: String, targetEmail: String): Result<Unit> {
        if (targetEmail.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true)) {
            return Result.failure(IllegalStateException("Primary Owner account is protected from deletion."))
        }
        return try {
            db.collection("users").document(targetUid).delete().await()
            try {
                db.collection("admins").document(targetUid).delete().await()
            } catch (_: Exception) {
            }
            recordAuditLog("USER_DELETE", "USER_MGMT", "CRITICAL", "Deleted user account $targetUid ($targetEmail)")
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "users/$targetUid")
            Result.failure(e)
        }
    }

    private fun permissionsForRole(role: AdminRole): List<String> = when (role) {
        AdminRole.SUPER_ADMIN -> listOf("ALL", "GRANT_SUPER_ADMIN", "CMS_ALL", "USERS_ALL", "SYSTEM_ALL")
        AdminRole.ADMIN -> listOf("CMS_EDIT", "CMS_PUBLISH", "USERS_MANAGE", "SUPPORT_REPLY", "ANNOUNCEMENTS")
        AdminRole.CONTENT_MODERATOR -> listOf("CMS_EDIT", "CMS_VERIFY", "CMS_PUBLISH")
        AdminRole.SUPPORT_AGENT -> listOf("SUPPORT_VIEW", "SUPPORT_REPLY")
        AdminRole.USER -> emptyList()
    }

    // -------------------------------------------------------------------------
    // Support Tickets & User Reports
    // -------------------------------------------------------------------------

    suspend fun submitSupportTicket(
        ticketType: String,
        subject: String,
        message: String,
        userNameFallback: String
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Sign in required"))
        val docId = "tkt_${UUID.randomUUID().toString().take(10)}"
        return try {
            db.collection("support_tickets").document(docId).set(
                mapOf(
                    "userId" to user.uid,
                    "userEmail" to (user.email ?: ""),
                    "userName" to (user.displayName ?: userNameFallback).take(100),
                    "ticketType" to ticketType,
                    "subject" to subject.trim().take(200),
                    "message" to message.trim().take(4000),
                    "status" to "OPEN",
                    "adminReply" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docId)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "support_tickets/$docId")
            Result.failure(e)
        }
    }

    fun observeMySupportTickets(uid: String): Flow<List<SupportTicketDoc>> = callbackFlow {
        val reg = db.collection("support_tickets")
            .whereEqualTo("userId", uid)
            .limit(40)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "support_tickets")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(SupportTicketDoc::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAllSupportTicketsAdmin(): Flow<List<SupportTicketDoc>> = callbackFlow {
        val reg = db.collection("support_tickets")
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "support_tickets")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(SupportTicketDoc::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun replyToSupportTicket(ticketId: String, status: String, reply: String): Result<Unit> = try {
        db.collection("support_tickets").document(ticketId).update(
            mapOf(
                "status" to status,
                "adminReply" to reply.trim().take(4000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        recordAuditLog("SUPPORT_TICKET_UPDATE", "SYSTEM", "INFO", "Updated ticket $ticketId -> $status")
        Result.success(Unit)
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, "support_tickets/$ticketId")
        Result.failure(e)
    }

    fun observeUserReportsAdmin(): Flow<List<UserReportDoc>> = callbackFlow {
        val reg = db.collection("user_reports")
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "user_reports")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(UserReportDoc::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitUserReport(targetType: String, targetId: String, reason: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required"))
        val docId = "rep_${UUID.randomUUID().toString().take(10)}"
        return try {
            db.collection("user_reports").document(docId).set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType.take(64),
                    "targetId" to targetId.take(128),
                    "reason" to reason.trim().take(1500),
                    "status" to "PENDING",
                    "resolutionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docId)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "user_reports/$docId")
            Result.failure(e)
        }
    }

    suspend fun resolveUserReport(reportId: String, status: String, note: String): Result<Unit> = try {
        db.collection("user_reports").document(reportId).update(
            mapOf(
                "status" to status,
                "resolutionNote" to note.trim().take(1000)
            )
        ).await()
        recordAuditLog("RESOLVE_USER_REPORT", "USER_MGMT", "INFO", "Report $reportId -> $status")
        Result.success(Unit)
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, "user_reports/$reportId")
        Result.failure(e)
    }

    // -------------------------------------------------------------------------
    // Audit Logs & Real Daily Analytics Telemetry
    // -------------------------------------------------------------------------

    suspend fun recordAuditLog(
        action: String,
        category: String,
        severity: String,
        details: String
    ) {
        val user = auth.currentUser ?: return
        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        try {
            db.collection("audit_logs").document(logId).set(
                mapOf(
                    "actorUid" to user.uid,
                    "actorEmail" to (user.email ?: ""),
                    "action" to action.take(150),
                    "category" to category,
                    "severity" to severity,
                    "details" to details.take(2000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "audit_logs/$logId")
        }
    }

    fun observeAuditLogsAdmin(): Flow<List<AuditLogDoc>> = callbackFlow {
        val reg = db.collection("audit_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "audit_logs")
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toObject(AuditLogDoc::class.java) }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun incrementDailyAnalytics(
        activeUsersDelta: Long = 0L,
        newRegDelta: Long = 0L,
        quranVersesDelta: Long = 0L,
        adhkarDelta: Long = 0L,
        audioPlaysDelta: Long = 0L,
        tasbihDelta: Long = 0L,
        section: String? = null
    ) {
        if (auth.currentUser == null) return
        val key = todayDateKey()
        val updates = mutableMapOf<String, Any>(
            "dateKey" to key,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (activeUsersDelta > 0) updates["activeUsers"] = FieldValue.increment(activeUsersDelta)
        if (newRegDelta > 0) updates["newRegistrations"] = FieldValue.increment(newRegDelta)
        if (quranVersesDelta > 0) updates["quranVersesRead"] = FieldValue.increment(quranVersesDelta)
        if (adhkarDelta > 0) updates["adhkarCompleted"] = FieldValue.increment(adhkarDelta)
        if (audioPlaysDelta > 0) updates["audioPlays"] = FieldValue.increment(audioPlaysDelta)
        if (tasbihDelta > 0) updates["tasbihTaps"] = FieldValue.increment(tasbihDelta)
        when (section) {
            "home" -> updates["sectionHomeHits"] = FieldValue.increment(1L)
            "quran" -> updates["sectionQuranHits"] = FieldValue.increment(1L)
            "adhkar" -> updates["sectionAdhkarHits"] = FieldValue.increment(1L)
            "audio" -> updates["sectionAudioHits"] = FieldValue.increment(1L)
        }
        try {
            db.collection("analytics_daily").document(key).set(updates, SetOptions.merge()).await()
            val uid = auth.currentUser?.uid
            if (uid != null && (quranVersesDelta > 0 || adhkarDelta > 0 || audioPlaysDelta > 0 || tasbihDelta > 0)) {
                val userUpdates = mutableMapOf<String, Any>(
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (quranVersesDelta > 0) userUpdates["quranVersesRead"] = FieldValue.increment(quranVersesDelta)
                if (adhkarDelta > 0) userUpdates["adhkarCompleted"] = FieldValue.increment(adhkarDelta)
                if (audioPlaysDelta > 0) userUpdates["audioPlays"] = FieldValue.increment(audioPlaysDelta)
                if (tasbihDelta > 0) userUpdates["tasbihCount"] = FieldValue.increment(tasbihDelta)
                db.collection("users").document(uid).update(userUpdates).await()
            }
        } catch (_: Exception) {
            // Non-blocking telemetry
        }
    }

    fun observeAnalyticsDailyAdmin(): Flow<List<DailyAnalyticsDoc>> = callbackFlow {
        val reg = db.collection("analytics_daily")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, "analytics_daily")
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(DailyAnalyticsDoc::class.java) }
                    ?.sortedBy { it.dateKey }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}
