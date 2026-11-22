package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EcosystemDao {

    // ======================== HALAQAT ========================

    @Query("SELECT * FROM halaqah_circles ORDER BY createdAtMillis DESC")
    fun observeCircles(): Flow<List<HalaqahCircleEntity>>

    @Query("SELECT * FROM halaqah_circles ORDER BY createdAtMillis DESC")
    suspend fun getAllCirclesOnce(): List<HalaqahCircleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCircle(circle: HalaqahCircleEntity): Long

    @Query("DELETE FROM halaqah_circles WHERE id = :id")
    suspend fun deleteCircle(id: Long)

    @Query("SELECT * FROM halaqah_teachers ORDER BY createdAtMillis DESC")
    fun observeTeachers(): Flow<List<HalaqahTeacherEntity>>

    @Query("SELECT * FROM halaqah_teachers ORDER BY createdAtMillis DESC")
    suspend fun getAllTeachersOnce(): List<HalaqahTeacherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeacher(teacher: HalaqahTeacherEntity): Long

    @Query("DELETE FROM halaqah_teachers WHERE id = :id")
    suspend fun deleteTeacher(id: Long)

    @Query("SELECT * FROM halaqah_students ORDER BY createdAtMillis DESC")
    fun observeStudents(): Flow<List<HalaqahStudentEntity>>

    @Query("SELECT * FROM halaqah_students ORDER BY createdAtMillis DESC")
    suspend fun getAllStudentsOnce(): List<HalaqahStudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudent(student: HalaqahStudentEntity): Long

    @Query("DELETE FROM halaqah_students WHERE id = :id")
    suspend fun deleteStudent(id: Long)

    @Query("SELECT * FROM halaqah_attendance ORDER BY recordedAtMillis DESC LIMIT 300")
    fun observeAttendance(): Flow<List<HalaqahAttendanceEntity>>

    @Query("SELECT * FROM halaqah_attendance ORDER BY recordedAtMillis DESC")
    suspend fun getAllAttendanceOnce(): List<HalaqahAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: HalaqahAttendanceEntity): Long

    @Query("SELECT * FROM recitation_assessments ORDER BY createdAtMillis DESC LIMIT 300")
    fun observeAssessments(): Flow<List<RecitationAssessmentEntity>>

    @Query("SELECT * FROM recitation_assessments ORDER BY createdAtMillis DESC")
    suspend fun getAllAssessmentsOnce(): List<RecitationAssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssessment(assessment: RecitationAssessmentEntity): Long

    @Query("UPDATE recitation_assessments SET approvedByTeacher = :approved WHERE id = :id")
    suspend fun setAssessmentApproval(id: Long, approved: Boolean)

    @Query("DELETE FROM recitation_assessments WHERE id = :id")
    suspend fun deleteAssessment(id: Long)

    // ======================== HIFZ AI ========================

    @Query("SELECT * FROM hifz_plans ORDER BY updatedAtMillis DESC")
    fun observeHifzPlans(): Flow<List<HifzPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHifzPlan(plan: HifzPlanEntity): Long

    @Query("DELETE FROM hifz_plans WHERE id = :id")
    suspend fun deleteHifzPlan(id: Long)

    @Query("SELECT * FROM hifz_revision_logs ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeHifzRevisionLogs(): Flow<List<HifzRevisionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHifzRevisionLog(log: HifzRevisionLogEntity): Long

    @Query("UPDATE hifz_revision_logs SET teacherApproved = :approved WHERE id = :id")
    suspend fun setHifzLogApproval(id: Long, approved: Boolean)

    @Query("SELECT * FROM hifz_weak_verses ORDER BY resolved ASC, missCount DESC, updatedAtMillis DESC")
    fun observeWeakVerses(): Flow<List<HifzWeakVerseEntity>>

    @Query("SELECT * FROM hifz_weak_verses WHERE surahNumber = :surah AND ayahNumber = :ayah LIMIT 1")
    suspend fun findWeakVerse(surah: Int, ayah: Int): HifzWeakVerseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeakVerse(item: HifzWeakVerseEntity): Long

    @Query("UPDATE hifz_weak_verses SET resolved = :resolved, updatedAtMillis = :now WHERE id = :id")
    suspend fun setWeakVerseResolved(id: Long, resolved: Boolean, now: Long = System.currentTimeMillis())

    // ======================== LOCAL NOOR AI KNOWLEDGE ========================

    @Query("SELECT * FROM local_ai_knowledge ORDER BY createdAtMillis DESC")
    fun observeAllAiKnowledge(): Flow<List<LocalAiKnowledgeEntity>>

    @Query("SELECT * FROM local_ai_knowledge WHERE approvalStatus = 'APPROVED' ORDER BY createdAtMillis DESC")
    fun observeApprovedAiKnowledge(): Flow<List<LocalAiKnowledgeEntity>>

    @Query("SELECT * FROM local_ai_knowledge")
    suspend fun getAllAiKnowledgeOnce(): List<LocalAiKnowledgeEntity>

    @Query("SELECT COUNT(*) FROM local_ai_knowledge")
    suspend fun countAiKnowledge(): Int

    @Query("SELECT * FROM local_ai_knowledge WHERE contentHash = :hash LIMIT 1")
    suspend fun findAiKnowledgeByHash(hash: String): LocalAiKnowledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiKnowledge(item: LocalAiKnowledgeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAiKnowledgeIgnore(items: List<LocalAiKnowledgeEntity>)

    @Query("UPDATE local_ai_knowledge SET approvalStatus = :status WHERE id = :id")
    suspend fun setAiKnowledgeApproval(id: String, status: String)

    @Query("DELETE FROM local_ai_knowledge WHERE id = :id")
    suspend fun deleteAiKnowledge(id: String)

    // ======================== ISLAMIC LIBRARY ========================

    @Query("SELECT * FROM library_books ORDER BY isBookmarked DESC, updatedAtMillis DESC")
    fun observeLibraryBooks(): Flow<List<LibraryBookEntity>>

    @Query("SELECT * FROM library_books")
    suspend fun getAllLibraryBooksOnce(): List<LibraryBookEntity>

    @Query("SELECT COUNT(*) FROM library_books")
    suspend fun countLibraryBooks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryBook(book: LibraryBookEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLibraryBooksIgnore(books: List<LibraryBookEntity>)

    @Query("UPDATE library_books SET isBookmarked = :bookmarked, updatedAtMillis = :now WHERE id = :bookId")
    suspend fun setBookBookmark(bookId: String, bookmarked: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE library_books SET lastReadCharOffset = :offset, updatedAtMillis = :now WHERE id = :bookId")
    suspend fun updateBookProgress(bookId: String, offset: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE library_books SET approvalStatus = :status WHERE id = :bookId")
    suspend fun setBookApproval(bookId: String, status: String)

    @Query("DELETE FROM library_books WHERE id = :bookId")
    suspend fun deleteLibraryBook(bookId: String)

    @Query("SELECT * FROM library_notes WHERE bookId = :bookId ORDER BY createdAtMillis DESC")
    fun observeNotesForBook(bookId: String): Flow<List<LibraryNoteEntity>>

    @Query("SELECT * FROM library_notes ORDER BY createdAtMillis DESC")
    fun observeAllLibraryNotes(): Flow<List<LibraryNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryNote(note: LibraryNoteEntity): Long

    @Query("DELETE FROM library_notes WHERE id = :id")
    suspend fun deleteLibraryNote(id: Long)

    // ======================== TAJWEED & KIDS ========================

    @Query("SELECT * FROM tajweed_progress")
    fun observeTajweedProgress(): Flow<List<TajweedProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTajweedProgress(progress: TajweedProgressEntity)

    @Query("SELECT * FROM kids_progress")
    fun observeKidsProgress(): Flow<List<KidsProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKidsProgress(progress: KidsProgressEntity)

    // ======================== FASTING & KHATM ========================

    @Query("SELECT * FROM fasting_logs ORDER BY dateKey DESC")
    fun observeFastingLogs(): Flow<List<FastingLogEntity>>

    @Query("SELECT * FROM fasting_logs ORDER BY dateKey DESC")
    suspend fun getAllFastingLogsOnce(): List<FastingLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFastingLog(log: FastingLogEntity)

    @Query("DELETE FROM fasting_logs WHERE dateKey = :dateKey")
    suspend fun deleteFastingLog(dateKey: String)

    @Query("SELECT * FROM khatm_plans ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun observeKhatmPlans(): Flow<List<KhatmPlanEntity>>

    @Query("SELECT * FROM khatm_plans")
    suspend fun getAllKhatmPlansOnce(): List<KhatmPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKhatmPlan(plan: KhatmPlanEntity): Long

    @Update
    suspend fun updateKhatmPlan(plan: KhatmPlanEntity)

    @Query("DELETE FROM khatm_plans WHERE id = :id")
    suspend fun deleteKhatmPlan(id: Long)

    @Query("SELECT * FROM khatm_reading_logs ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeKhatmLogs(): Flow<List<KhatmReadingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhatmLog(log: KhatmReadingLogEntity): Long

    // ======================== QUIZ ========================

    @Query("SELECT * FROM custom_quiz_questions ORDER BY createdAtMillis DESC")
    fun observeQuizQuestions(): Flow<List<CustomQuizQuestionEntity>>

    @Query("SELECT * FROM custom_quiz_questions")
    suspend fun getAllQuizQuestionsOnce(): List<CustomQuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM custom_quiz_questions")
    suspend fun countQuizQuestions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizQuestion(q: CustomQuizQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuizQuestionsIgnore(questions: List<CustomQuizQuestionEntity>)

    @Query("DELETE FROM custom_quiz_questions WHERE id = :id")
    suspend fun deleteQuizQuestion(id: String)

    @Query("SELECT * FROM quiz_attempts ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity): Long

    // ======================== PERSONAL QURAN NOTES ========================

    @Query("SELECT * FROM quran_note_collections ORDER BY createdAtMillis ASC")
    fun observeNoteCollections(): Flow<List<QuranNoteCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNoteCollection(collection: QuranNoteCollectionEntity): Long

    @Query("DELETE FROM quran_note_collections WHERE id = :id")
    suspend fun deleteNoteCollection(id: Long)

    @Query("SELECT * FROM quran_personal_notes ORDER BY isFavorite DESC, updatedAtMillis DESC")
    fun observePersonalNotes(): Flow<List<QuranPersonalNoteEntity>>

    @Query("SELECT * FROM quran_personal_notes")
    suspend fun getAllPersonalNotesOnce(): List<QuranPersonalNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonalNote(note: QuranPersonalNoteEntity): Long

    @Query("DELETE FROM quran_personal_notes WHERE id = :id")
    suspend fun deletePersonalNote(id: Long)

    // ======================== LOCAL OFFLINE SUPER ADMIN ACCOUNTS ========================

    @Query("SELECT * FROM local_admin_accounts ORDER BY createdAtMillis ASC")
    fun observeLocalAdmins(): Flow<List<LocalAdminAccountEntity>>

    @Query("SELECT * FROM local_admin_accounts")
    suspend fun getAllLocalAdminsOnce(): List<LocalAdminAccountEntity>

    @Query("SELECT * FROM local_admin_accounts WHERE LOWER(username) = LOWER(:username) AND active = 1 LIMIT 1")
    suspend fun findActiveLocalAdminByUsername(username: String): LocalAdminAccountEntity?

    @Query("SELECT COUNT(*) FROM local_admin_accounts WHERE role = 'SUPER_ADMIN' AND active = 1")
    suspend fun countActiveSuperAdmins(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalAdmin(account: LocalAdminAccountEntity): Long

    @Query("DELETE FROM local_admin_accounts WHERE id = :id")
    suspend fun deleteLocalAdmin(id: Long)
}
