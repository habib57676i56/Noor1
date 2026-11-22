package com.example.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.db.HalaqahCircleEntity
import com.example.data.local.db.HalaqahStudentEntity
import com.example.data.local.db.HalaqahTeacherEntity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.EcosystemViewModel

@Composable
fun HalaqatScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val circles by viewModel.circles.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val assessments by viewModel.assessments.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCircleDialog by remember { mutableStateOf(false) }
    var showTeacherDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var showAssessmentDialog by remember { mutableStateOf(false) }
    var reportStudent by remember { mutableStateOf<HalaqahStudentEntity?>(null) }

    val tabs = listOf(
        L10n.pick(lang, "لوحة الحلقات", "Dashboard"),
        L10n.pick(lang, "الحلقات (${circles.size})", "Circles (${circles.size})"),
        L10n.pick(lang, "الطلاب (${students.size})", "Students (${students.size})"),
        L10n.pick(lang, "المعلمون (${teachers.size})", "Teachers (${teachers.size})"),
        L10n.pick(lang, "التسميع (${assessments.size})", "Assessments (${assessments.size})")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "حلقات نور للقرآن الكريم", "NOOR Halaqat"),
            subtitle = L10n.pick(lang, "إدارة محلية متكاملة للحلقات والطلاب والمعلمين وتقييم التسميع", "Offline Quran Learning Circles Management"),
            onBack = onBack
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { index ->
                LiquidGlassChip(
                    label = tabs[index],
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("halaqat_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // 0: Halaqat Dashboard
                0 -> {
                    item {
                        val presentCount = attendance.count { it.status == "PRESENT" }
                        val attendanceRate = if (attendance.isEmpty()) 0 else (presentCount * 100) / attendance.size
                        val avgAccuracy = if (assessments.isEmpty()) 0 else assessments.map { it.accuracyPercent }.average().toInt()

                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Text(
                                text = L10n.pick(lang, "ملخص أداء الحلقات", "Halaqat Overview"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                KpiBadge(L10n.pick(lang, "الحلقات", "Circles"), circles.size.toString())
                                KpiBadge(L10n.pick(lang, "الطلاب", "Students"), students.size.toString())
                                KpiBadge(L10n.pick(lang, "المعلمون", "Teachers"), teachers.size.toString())
                                KpiBadge(L10n.pick(lang, "الدقة", "Accuracy"), "$avgAccuracy%")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "نسبة الحضور المسجلة: $attendanceRate% · إجمالي جلسات التسميع: ${assessments.size}",
                                    "Attendance Rate: $attendanceRate% · Total Assessments: ${assessments.size}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item {
                        NoorSectionHeader(title = L10n.pick(lang, "إجراءات سريعة", "Quick Actions"))
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCircleDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_circle_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "حلقة جديدة", "New Circle"))
                            }
                            OutlinedButton(
                                onClick = { showStudentDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_student_btn")
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "تسجيل طالب", "Add Student"))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showTeacherDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_teacher_btn")
                            ) {
                                Icon(Icons.Rounded.School, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "إضافة معلم", "Add Teacher"))
                            }
                            OutlinedButton(
                                onClick = { showAssessmentDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_assessment_btn"),
                                enabled = students.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.Assessment, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(L10n.pick(lang, "تقييم تسميع", "Assess"))
                            }
                        }
                    }
                    if (assessments.isNotEmpty()) {
                        item { NoorSectionHeader(title = L10n.pick(lang, "آخر التقييمات", "Recent Assessments")) }
                        items(assessments.take(5), key = { it.id }) { a ->
                            val stName = students.firstOrNull { it.id == a.studentId }?.fullName ?: "طالب #${a.studentId}"
                            val surah = SurahCatalog.findSurah(a.surahNumber)
                            LiquidGlassCard {
                                Text("$stName — سورة ${surah.nameAr} (${a.fromAyah}–${a.toAyah})", fontWeight = FontWeight.Bold)
                                Text(
                                    "الدقة: ${a.accuracyPercent}% · التاريخ: ${a.assessmentDateKey}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = noorColors.goldAccent
                                )
                                if (a.tajweedObservations.isNotBlank()) {
                                    Text("ملاحظات التجويد: ${a.tajweedObservations}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 1: Circles CRUD + Quick Attendance
                1 -> {
                    item {
                        Button(
                            onClick = { showCircleDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Groups, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إنشاء حلقة قرآنية جديدة", "Create New Quran Circle"))
                        }
                    }
                    if (circles.isEmpty()) {
                        item {
                            LiquidGlassCard {
                                Text(L10n.pick(lang, "لا توجد حلقات بعد. ابدأ بإنشاء أول حلقة حفظ أو مراجعة.", "No circles yet. Create your first memorization or revision circle."))
                            }
                        }
                    }
                    items(circles, key = { it.id }) { circle ->
                        val teacherName = teachers.firstOrNull { it.id == circle.teacherId }?.fullName ?: L10n.pick(lang, "غير محدد", "Unassigned")
                        val circleStudents = students.filter { it.circleId == circle.id }
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(circle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${circle.circleType} · ${circle.level} · المعلم: $teacherName",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = noorColors.goldAccent
                                    )
                                    if (circle.scheduleText.isNotBlank()) {
                                        Text("الجدول: ${circle.scheduleText}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (circle.description.isNotBlank()) {
                                        Text(circle.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteCircle(circle.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Circle", tint = SemanticError)
                                }
                            }
                            if (circleStudents.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    L10n.pick(lang, "تسجيل حضور اليوم (${viewModel.todayKey()}):", "Mark Today's Attendance:"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                circleStudents.forEach { st ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(st.fullName, style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(onClick = { viewModel.markAttendance(circle.id, st.id, "PRESENT") }) {
                                                Text(L10n.pick(lang, "حاضر", "Present"), color = SemanticSuccess)
                                            }
                                            TextButton(onClick = { viewModel.markAttendance(circle.id, st.id, "ABSENT") }) {
                                                Text(L10n.pick(lang, "غائب", "Absent"), color = SemanticError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2: Students CRUD & Progress Reports
                2 -> {
                    item {
                        Button(
                            onClick = { showStudentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "تسجيل طالب جديد", "Register New Student"))
                        }
                    }
                    items(students, key = { it.id }) { st ->
                        val circleName = circles.firstOrNull { it.id == st.circleId }?.name ?: "بدون حلقة"
                        val surah = SurahCatalog.findSurah(st.currentSurahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${st.fullName} (${st.age} سنة)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("الحلقة: $circleName · السورة الحالية: ${surah.nameAr}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                                    Text("الحفظ: ${st.memorizedJuz} جزء (${st.memorizedPages} صفحة) · المراجعة: ${st.revisionPages} صفحة", style = MaterialTheme.typography.bodySmall)
                                    if (st.teacherFeedback.isNotBlank()) {
                                        Text("توجيه المعلم: ${st.teacherFeedback}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { reportStudent = st }) {
                                    Icon(Icons.Rounded.Assessment, contentDescription = "Student Report", tint = noorColors.goldAccent)
                                }
                                IconButton(onClick = { viewModel.deleteStudent(st.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Student", tint = SemanticError)
                                }
                            }
                        }
                    }
                }

                // 3: Teachers CRUD & Assigned Students
                3 -> {
                    item {
                        Button(
                            onClick = { showTeacherDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إضافة ملف معلم", "Create Teacher Profile"))
                        }
                    }
                    items(teachers, key = { it.id }) { t ->
                        val tCircles = circles.filter { it.teacherId == t.id }
                        val tStudents = students.filter { it.teacherId == t.id || tCircles.any { c -> c.id == it.circleId } }
                        val tAssessments = assessments.filter { it.teacherId == t.id }
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("التخصص: ${t.specialization}", style = MaterialTheme.typography.bodySmall, color = noorColors.goldAccent)
                                    Text("الحلقات: ${tCircles.size} · الطلاب المسندون: ${tStudents.size} · التقييمات: ${tAssessments.size}", style = MaterialTheme.typography.bodySmall)
                                    if (t.scheduleNotes.isNotBlank()) {
                                        Text("الجدول: ${t.scheduleNotes}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteTeacher(t.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Teacher", tint = SemanticError)
                                }
                            }
                        }
                    }
                }

                // 4: Recitation Assessments
                4 -> {
                    item {
                        Button(
                            onClick = { showAssessmentDialog = true },
                            enabled = students.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.pick(lang, "تسجيل تقييم تسميع جديد", "Record Recitation Assessment"))
                        }
                    }
                    items(assessments, key = { it.id }) { a ->
                        val stName = students.firstOrNull { it.id == a.studentId }?.fullName ?: "طالب #${a.studentId}"
                        val surah = SurahCatalog.findSurah(a.surahNumber)
                        LiquidGlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("$stName — ${surah.nameAr} (${a.fromAyah}–${a.toAyah})", fontWeight = FontWeight.Bold)
                                    Text("الدقة: ${a.accuracyPercent}% · التاريخ: ${a.assessmentDateKey} · التالي: ${a.nextAssessmentDateKey.ifBlank { "غير محدد" }}", style = MaterialTheme.typography.labelMedium, color = noorColors.goldAccent)
                                    if (a.newMemorizationSummary.isNotBlank()) Text("الحفظ الجديد: ${a.newMemorizationSummary}", style = MaterialTheme.typography.bodySmall)
                                    if (a.previousRevisionSummary.isNotBlank()) Text("المراجعة: ${a.previousRevisionSummary}", style = MaterialTheme.typography.bodySmall)
                                    if (a.tajweedObservations.isNotBlank()) Text("التجويد: ${a.tajweedObservations}", style = MaterialTheme.typography.bodySmall)
                                    if (a.mistakesToRevise.isNotBlank()) Text("مواضع تحتاج مراجعة: ${a.mistakesToRevise}", style = MaterialTheme.typography.bodySmall, color = SemanticError)
                                    if (a.teacherComments.isNotBlank()) Text("ملاحظة المعلم: ${a.teacherComments}", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = { viewModel.toggleAssessmentApproval(a.id, !a.approvedByTeacher) }) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Approve",
                                            tint = if (a.approvedByTeacher) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteAssessment(a.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = SemanticError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCircleDialog) {
        CircleCreateDialog(
            teachers = teachers,
            onDismiss = { showCircleDialog = false },
            onSave = { name, desc, level, type, schedule, teacherId ->
                viewModel.saveCircle(name, desc, level, type, schedule, teacherId)
                showCircleDialog = false
            }
        )
    }
    if (showTeacherDialog) {
        TeacherCreateDialog(
            onDismiss = { showTeacherDialog = false },
            onSave = { name, phone, spec, sched ->
                viewModel.saveTeacher(name, phone, spec, sched)
                showTeacherDialog = false
            }
        )
    }
    if (showStudentDialog) {
        StudentCreateDialog(
            circles = circles,
            teachers = teachers,
            onDismiss = { showStudentDialog = false },
            onSave = { name, age, contact, circleId, teacherId, juz, pages, revPages, surah, feedback ->
                viewModel.saveStudent(name, age, contact, circleId, teacherId, juz, pages, revPages, surah, feedback)
                showStudentDialog = false
            }
        )
    }
    if (showAssessmentDialog && students.isNotEmpty()) {
        AssessmentCreateDialog(
            students = students,
            onDismiss = { showAssessmentDialog = false },
            onSave = { student, surah, fromA, toA, newMem, prevRev, acc, tajweed, mistakes, comments, nextDate ->
                viewModel.recordRecitationAssessment(
                    studentId = student.id,
                    teacherId = student.teacherId,
                    circleId = student.circleId,
                    surahNumber = surah,
                    fromAyah = fromA,
                    toAyah = toA,
                    newMemSummary = newMem,
                    prevRevSummary = prevRev,
                    accuracyPercent = acc,
                    tajweedObservations = tajweed,
                    mistakesToRevise = mistakes,
                    teacherComments = comments,
                    nextDateKey = nextDate
                )
                showAssessmentDialog = false
            }
        )
    }
    reportStudent?.let { st ->
        val stAssessments = assessments.filter { it.studentId == st.id }
        val stAttendance = attendance.filter { it.studentId == st.id }
        val present = stAttendance.count { it.status == "PRESENT" }
        val avgAcc = if (stAssessments.isEmpty()) 0 else stAssessments.map { it.accuracyPercent }.average().toInt()
        AlertDialog(
            onDismissRequest = { reportStudent = null },
            title = { Text("تقرير تقدم الطالب: ${st.fullName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("المحفوظ: ${st.memorizedJuz} جزء (${st.memorizedPages} صفحة)")
                    Text("صفحات المراجعة: ${st.revisionPages} صفحة")
                    Text("عدد جلسات التسميع: ${stAssessments.size} · متوسط الدقة: $avgAcc%")
                    Text("الحضور: $present من أصل ${stAttendance.size} جلسة")
                    if (st.teacherFeedback.isNotBlank()) Text("توجيه المعلم: ${st.teacherFeedback}")
                }
            },
            confirmButton = {
                TextButton(onClick = { reportStudent = null }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun KpiBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = GoldPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CircleCreateDialog(
    teachers: List<HalaqahTeacherEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("السبت والإثنين والأربعاء بعد العصر") }
    var circleType by remember { mutableStateOf("MEMORIZATION") }
    var level by remember { mutableStateOf("BEGINNER") }
    var selectedTeacherId by remember { mutableStateOf(teachers.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء حلقة قرآنية") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الحلقة") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("وصف الحلقة") })
                OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("أوقات الحلقة") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiquidGlassChip("حفظ", circleType == "MEMORIZATION", { circleType = "MEMORIZATION" })
                    LiquidGlassChip("مراجعة", circleType == "REVISION", { circleType = "REVISION" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiquidGlassChip("مبتدئ", level == "BEGINNER", { level = "BEGINNER" })
                    LiquidGlassChip("متوسط", level == "INTERMEDIATE", { level = "INTERMEDIATE" })
                    LiquidGlassChip("متقدم", level == "ADVANCED", { level = "ADVANCED" })
                }
                if (teachers.isNotEmpty()) {
                    Text("المعلم المسؤول:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teachers) { t ->
                            LiquidGlassChip(t.fullName, selectedTeacherId == t.id, { selectedTeacherId = t.id })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, desc, level, circleType, schedule, selectedTeacherId) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TeacherCreateDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var spec by remember { mutableStateOf("حفظ وتجويد برواية حفص") }
    var sched by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة معلم قرآن") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المعلم الكامل") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم التواصل (اختياري)") }, singleLine = true)
                OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("التخصص / الإجازة") }, singleLine = true)
                OutlinedTextField(value = sched, onValueChange = { sched = it }, label = { Text("ملاحظات الجدول") })
            }
        },
        confirmButton = { Button(onClick = { onSave(name, phone, spec, sched) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun StudentCreateDialog(
    circles: List<HalaqahCircleEntity>,
    teachers: List<HalaqahTeacherEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, Long?, Long?, Int, Int, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("12") }
    var guardian by remember { mutableStateOf("") }
    var juzText by remember { mutableStateOf("1") }
    var pagesText by remember { mutableStateOf("20") }
    var revText by remember { mutableStateOf("10") }
    var surahText by remember { mutableStateOf("78") }
    var feedback by remember { mutableStateOf("") }
    var selectedCircleId by remember { mutableStateOf(circles.firstOrNull()?.id) }
    var selectedTeacherId by remember { mutableStateOf(teachers.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل طالب جديد") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ageText, onValueChange = { ageText = it.filter(Char::isDigit) }, label = { Text("العمر") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = juzText, onValueChange = { juzText = it.filter(Char::isDigit) }, label = { Text("الأجزاء") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pagesText, onValueChange = { pagesText = it.filter(Char::isDigit) }, label = { Text("صفحات الحفظ") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = revText, onValueChange = { revText = it.filter(Char::isDigit) }, label = { Text("صفحات المراجعة") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = surahText, onValueChange = { surahText = it.filter(Char::isDigit) }, label = { Text("رقم السورة الحالية (1-114)") }, singleLine = true)
                OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("تواصل ولي الأمر") }, singleLine = true)
                OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("ملاحظات المعلم") })
                if (circles.isNotEmpty()) {
                    Text("الحلقة:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(circles) { c ->
                            LiquidGlassChip(c.name, selectedCircleId == c.id, { selectedCircleId = c.id })
                        }
                    }
                }
                if (teachers.isNotEmpty()) {
                    Text("المعلم:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teachers) { t ->
                            LiquidGlassChip(t.fullName, selectedTeacherId == t.id, { selectedTeacherId = t.id })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    name,
                    ageText.toIntOrNull() ?: 12,
                    guardian,
                    selectedCircleId,
                    selectedTeacherId,
                    juzText.toIntOrNull() ?: 0,
                    pagesText.toIntOrNull() ?: 0,
                    revText.toIntOrNull() ?: 0,
                    surahText.toIntOrNull() ?: 1,
                    feedback
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AssessmentCreateDialog(
    students: List<HalaqahStudentEntity>,
    onDismiss: () -> Unit,
    onSave: (HalaqahStudentEntity, Int, Int, Int, String, String, Int, String, String, String, String) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.first()) }
    var surahText by remember { mutableStateOf(selectedStudent.currentSurahNumber.toString()) }
    var fromText by remember { mutableStateOf("1") }
    var toText by remember { mutableStateOf("10") }
    var newMem by remember { mutableStateOf("") }
    var prevRev by remember { mutableStateOf("") }
    var accText by remember { mutableStateOf("92") }
    var tajweed by remember { mutableStateOf("") }
    var mistakes by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقييم تسميع القرآن") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اختر الطالب:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(students) { st ->
                        LiquidGlassChip(st.fullName, selectedStudent.id == st.id, { selectedStudent = st })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = surahText, onValueChange = { surahText = it.filter(Char::isDigit) }, label = { Text("السورة") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = fromText, onValueChange = { fromText = it.filter(Char::isDigit) }, label = { Text("من آية") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = toText, onValueChange = { toText = it.filter(Char::isDigit) }, label = { Text("إلى آية") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = accText, onValueChange = { accText = it.filter(Char::isDigit) }, label = { Text("دقة التسميع % (0-100)") }, singleLine = true)
                OutlinedTextField(value = newMem, onValueChange = { newMem = it }, label = { Text("الحفظ الجديد") })
                OutlinedTextField(value = prevRev, onValueChange = { prevRev = it }, label = { Text("المراجعة السابقة") })
                OutlinedTextField(value = tajweed, onValueChange = { tajweed = it }, label = { Text("ملاحظات التجويد") })
                OutlinedTextField(value = mistakes, onValueChange = { mistakes = it }, label = { Text("الأخطاء المطلوب مراجعتها") })
                OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("تعليقات المعلم") })
                OutlinedTextField(value = nextDate, onValueChange = { nextDate = it }, label = { Text("تاريخ التقييم القادم (YYYY-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    selectedStudent,
                    surahText.toIntOrNull() ?: 1,
                    fromText.toIntOrNull() ?: 1,
                    toText.toIntOrNull() ?: 5,
                    newMem,
                    prevRev,
                    accText.toIntOrNull() ?: 90,
                    tajweed,
                    mistakes,
                    comments,
                    nextDate
                )
            }) { Text("حفظ التقييم") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
