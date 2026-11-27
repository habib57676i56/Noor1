package com.example.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.SurahCatalog
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Super Admin Reciters Studio:
 * Add custom reciters and upload audio files directly from the phone (zero-permission SAF OpenDocument)
 * or link HTTPS audio streams. Published reciters immediately appear inside the app's Reciters list
 * and Audio Library with full offline/local playback.
 */
@Composable
fun AdminRecitersTab(
    adminViewModel: AdminViewModel
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val customReciters by adminViewModel.customReciters.collectAsStateWithLifecycle()
    val audioState by adminViewModel.audioState.collectAsStateWithLifecycle()

    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var selectedStyleIdx by remember { mutableIntStateOf(0) }
    var surahTitleAr by remember { mutableStateOf("سورة الفاتحة") }
    var surahNumberText by remember { mutableStateOf("1") }
    var remoteAudioUrl by remember { mutableStateOf("") }
    var pickedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf("") }

    // Dedicated Yasser Al-Dosari (Iraqi Maqam) MP3 Upload / Direct Link Studio State
    var yasserTitleAr by remember { mutableStateOf("سورة غافر — تلاوة عراقية خاشعة") }
    var yasserSurahNumberText by remember { mutableStateOf("40") }
    var yasserAudioUrl by remember { mutableStateOf("") }
    var yasserPickedUri by remember { mutableStateOf<Uri?>(null) }
    var yasserPickedFileName by remember { mutableStateOf("") }

    val styleOptions = remember {
        listOf(
            "مرتّل (حفص عن عاصم)" to "Murattal (Hafs)",
            "مجوّد" to "Mujawwad",
            "ورش عن نافع" to "Warsh",
            "مقام العراق · ياسر الدوسري" to "Yasser Al-Dosari · Iraqi Maqam",
            "تلاوة خاشعة خاصة" to "Special Khashia"
        )
    }

    val yasserAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            yasserPickedUri = uri
            val resolved = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) c.getString(idx) else null
                    } else null
                }
            }.getOrNull()
            yasserPickedFileName = resolved ?: uri.lastPathSegment.orEmpty().ifBlank { "yasser_iraqi.mp3" }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedAudioUri = uri
            val resolved = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) c.getString(idx) else null
                    } else null
                }
            }.getOrNull()
            pickedFileName = resolved ?: uri.lastPathSegment.orEmpty().ifBlank { "audio_file.mp3" }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_reciters_tab"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "استوديو القراء ورفع الصوتيات للمصحف", "Mushaf Reciters Studio & Audio Uploader"),
                subtitle = L10n.pick(
                    lang,
                    "ارفع صوت القارئ من الهاتف ليظهر فوراً داخل قائمة قراء المصحف الشريف مع تحديد وتتبع الآيات تلقائياً",
                    "Upload phone audio — appears directly inside the Quran Mushaf reciters picker with automatic Ayah sync"
                )
            )
        }

        // Dedicated Sheikh Yasser Al-Dosari — Iraqi Maqam MP3 Upload & Direct Link Card
        item {
            AdminPanelCard(accentBorder = AdminPalette.AccentEmerald.copy(alpha = 0.75f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AdminPalette.AccentEmerald.copy(alpha = 0.18f))
                            .border(1.dp, AdminPalette.AccentEmerald.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AdminPalette.AccentEmerald)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "تلاوات ياسر الدوسري · المقام العراقي (رفع MP3 أو رابط)",
                                "Yasser Al-Dosari · Iraqi Maqam (Upload MP3 / URL)"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdminPalette.TextPrimary
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "ارفع ملف MP3 من جهازك أو أضف رابط تلاوة مباشر ليظهر فوراً في قسم ياسر الدوسري بالمقام العراقي",
                                "Upload an MP3 from your phone or paste a direct audio link to publish directly into the Iraqi Maqam studio"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yasserTitleAr,
                        onValueChange = { yasserTitleAr = it },
                        label = { Text(L10n.pick(lang, "عنوان التلاوة *", "Recitation Title *")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.75f)
                            .testTag("admin_yasser_iraqi_title")
                    )
                    OutlinedTextField(
                        value = yasserSurahNumberText,
                        onValueChange = { txt ->
                            val digits = txt.filter { it.isDigit() }.take(3)
                            yasserSurahNumberText = digits
                            digits.toIntOrNull()?.takeIf { it in 1..114 }?.let { num ->
                                SurahCatalog.findSurah(num)?.let { s ->
                                    yasserTitleAr = "سورة ${s.nameAr} — مقام العراق"
                                }
                            }
                        },
                        label = { Text(L10n.pick(lang, "السورة", "Surah #")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { yasserAudioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_yasser_pick_mp3_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (yasserPickedUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = if (yasserPickedUri != null) AdminPalette.AccentEmerald else AdminPalette.GoldLuxe
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (yasserPickedUri != null) {
                            L10n.pick(lang, "تم اختيار: $yasserPickedFileName", "Selected: $yasserPickedFileName")
                        } else {
                            L10n.pick(lang, "رفع ملف تلاوة MP3 من الهاتف", "Upload MP3 Recitation from Phone")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = yasserAudioUrl,
                    onValueChange = { yasserAudioUrl = it },
                    label = { Text(L10n.pick(lang, "أو ضع رابط التلاوة المباشر (https://...mp3)", "Or paste direct audio link (https://...mp3)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_yasser_iraqi_url")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val surahNum = yasserSurahNumberText.toIntOrNull()?.coerceIn(1, 114) ?: 40
                        val surahMeta = SurahCatalog.findSurah(surahNum)
                        val titleClean = yasserTitleAr.trim().ifBlank { "سورة ${surahMeta.nameAr} — مقام العراق" }
                        adminViewModel.addCustomReciterFromPhone(
                            nameAr = "الشيخ ياسر الدوسري (مقام العراق)",
                            nameEn = "Sheikh Yasser Al-Dosari (Iraqi Maqam)",
                            styleAr = "مقام العراق · مضافة من الإدارة",
                            styleEn = "Yasser Al-Dosari · Iraqi Maqam",
                            surahTitleAr = titleClean,
                            surahTitleEn = "Surah ${surahMeta.nameEn} — Iraqi Maqam",
                            surahNumber = surahNum,
                            pickedAudioUri = yasserPickedUri,
                            remoteAudioUrl = yasserAudioUrl
                        )
                        if (yasserPickedUri != null || yasserAudioUrl.isNotBlank()) {
                            yasserAudioUrl = ""
                            yasserPickedUri = null
                            yasserPickedFileName = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_publish_yasser_iraqi_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminPalette.GoldLuxe,
                        contentColor = Color(0xFF0A1713)
                    )
                ) {
                    Icon(Icons.Rounded.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "نشر في قسم ياسر الدوسري · المقام العراقي", "Publish to Yasser Iraqi Maqam Studio"),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Add New Reciter & Upload Audio Glass Card
        item {
            AdminPanelCard(accentBorder = AdminPalette.GoldLuxe.copy(alpha = 0.65f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AdminPalette.GoldLuxe.copy(alpha = 0.18f))
                            .border(1.dp, AdminPalette.GoldLuxe.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = AdminPalette.GoldLuxe)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(lang, "إضافة قارئ جديد ورفع التلاوة", "Add Custom Reciter & Audio"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdminPalette.TextPrimary
                        )
                        Text(
                            text = L10n.pick(lang, "يدعم MP3 / M4A / WAV / OGG من ذاكرة الهاتف", "Supports MP3 / M4A / WAV / OGG from phone storage"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(L10n.pick(lang, "اسم القارئ بالعربية *", "Reciter Name (Arabic) *")) },
                    placeholder = { Text(L10n.pick(lang, "مثال: الشيخ عبد الله المقرئ", "e.g. Sheikh Abdullah")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("admin_reciter_name_ar")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(L10n.pick(lang, "اسم القارئ بالإنجليزية (اختياري)", "Reciter Name (English, optional)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = L10n.pick(lang, "نوع التلاوة والرواية", "Recitation Style / Riwayah"),
                    style = MaterialTheme.typography.labelMedium,
                    color = AdminPalette.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(styleOptions.indices.toList()) { idx ->
                        val (ar, en) = styleOptions[idx]
                        AdminFilterPill(
                            label = L10n.pick(lang, ar, en),
                            selected = selectedStyleIdx == idx,
                            onClick = { selectedStyleIdx = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = surahTitleAr,
                        onValueChange = { surahTitleAr = it },
                        label = { Text(L10n.pick(lang, "عنوان التلاوة / السورة", "Surah / Track Title")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.7f)
                    )
                    OutlinedTextField(
                        value = surahNumberText,
                        onValueChange = { txt ->
                            val digits = txt.filter { it.isDigit() }.take(3)
                            surahNumberText = digits
                            digits.toIntOrNull()?.takeIf { it in 1..114 }?.let { num ->
                                SurahCatalog.findSurah(num)?.let { s ->
                                    surahTitleAr = "سورة ${s.nameAr}"
                                }
                            }
                        },
                        label = { Text(L10n.pick(lang, "رقم السورة", "Surah #")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Audio File Picker Button
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_pick_audio_file_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (pickedAudioUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = if (pickedAudioUri != null) AdminPalette.AccentEmerald else AdminPalette.AccentCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pickedAudioUri != null) {
                            L10n.pick(lang, "تم اختيار الملف: $pickedFileName", "Selected: $pickedFileName")
                        } else {
                            L10n.pick(lang, "رفع ملف صوتي من الهاتف (MP3 / M4A / WAV)", "Upload Audio File from Phone")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remoteAudioUrl,
                    onValueChange = { remoteAudioUrl = it },
                    label = { Text(L10n.pick(lang, "أو رابط صوتي مباشر https:// (اختياري)", "Or direct https:// audio URL (optional)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val (styleAr, styleEn) = styleOptions[selectedStyleIdx]
                        val surahNum = surahNumberText.toIntOrNull()?.coerceIn(1, 114) ?: 1
                        val surahEn = SurahCatalog.findSurah(surahNum)?.let { "Surah ${it.nameEn}" } ?: surahTitleAr
                        adminViewModel.addCustomReciterFromPhone(
                            nameAr = nameAr,
                            nameEn = nameEn,
                            styleAr = styleAr,
                            styleEn = styleEn,
                            surahTitleAr = surahTitleAr,
                            surahTitleEn = surahEn,
                            surahNumber = surahNum,
                            pickedAudioUri = pickedAudioUri,
                            remoteAudioUrl = remoteAudioUrl
                        )
                        if (nameAr.isNotBlank() && (pickedAudioUri != null || remoteAudioUrl.isNotBlank())) {
                            nameAr = ""
                            nameEn = ""
                            remoteAudioUrl = ""
                            pickedAudioUri = null
                            pickedFileName = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_publish_reciter_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminPalette.AccentEmerald,
                        contentColor = Color(0xFF021610)
                    )
                ) {
                    Icon(Icons.Rounded.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "حفظ وإضافة إلى قائمة القراء فوراً", "Save & Publish to Reciters List"),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Published Custom Reciters Header
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "القراء المضافون (${customReciters.size})", "Published Custom Reciters (${customReciters.size})"),
                subtitle = L10n.pick(lang, "يظهر جميع هؤلاء القراء مباشرة داخل مكتبة الصوتيات وقائمة القراء", "All reciters below appear live in the Audio Library & Reciters picker")
            )
        }

        if (customReciters.isEmpty()) {
            item {
                AdminPanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AdminPalette.TextMuted, modifier = Modifier.size(28.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "لم تتم إضافة قراء مخصصين بعد. اختر ملفاً صوتياً من هاتفك أعلاه ليظهر القارئ فوراً للمستخدمين.",
                                "No custom reciters added yet. Pick an audio file from your phone above to publish immediately."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(customReciters, key = { it.id }) { reciter ->
                val trackId = "custom_reciter_${reciter.id}"
                val isPlayingThis = audioState.currentTrack?.id == trackId && audioState.isPlaying
                AdminPanelCard(
                    accentBorder = if (isPlayingThis) AdminPalette.AccentEmerald else AdminPalette.BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AdminPalette.AccentCyan.copy(alpha = 0.25f), AdminPalette.AccentEmerald.copy(alpha = 0.25f))
                                    )
                                )
                                .border(1.dp, AdminPalette.AccentCyan.copy(alpha = 0.55f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { adminViewModel.previewCustomReciter(reciter) }) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlayingThis) "Pause" else "Play Preview",
                                    tint = if (isPlayingThis) AdminPalette.AccentEmerald else Color.White
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = L10n.pick(lang, reciter.nameAr, reciter.nameEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminPalette.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AdminStatusBadge(
                                    label = L10n.pick(lang, "سورة ${reciter.surahNumber}", "Surah ${reciter.surahNumber}"),
                                    color = AdminPalette.GoldLuxe
                                )
                            }
                            Text(
                                text = "${L10n.pick(lang, reciter.surahTitleAr, reciter.surahTitleEn)} · ${L10n.pick(lang, reciter.styleAr, reciter.styleEn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (reciter.fileName.isNotBlank() || reciter.localAudioPath.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.AudioFile, null, tint = AdminPalette.AccentEmerald, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = reciter.fileName.ifBlank { L10n.pick(lang, "ملف صوتي مرفوع من الهاتف", "Uploaded Phone Audio") },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminPalette.AccentEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { adminViewModel.deleteCustomReciter(reciter.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Reciter",
                                tint = AdminPalette.AccentRose
                            )
                        }
                    }
                }
            }
        }
    }
}
