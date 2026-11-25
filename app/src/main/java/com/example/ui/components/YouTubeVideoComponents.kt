package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoItem
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary

/**
 * YouTube-style 16:9 video cover thumbnail with Coil AsyncImage,
 * rich Islamic emerald/gold gradient fallback, bottom scrim, category pill,
 * duration badge, and centered frosted play indicator.
 */
@Composable
fun YouTubeVideoThumbnailBox(
    thumbnailUrl: String,
    categoryLabel: String,
    durationLabel: String = "",
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanThumb = thumbnailUrl.trim()
    var failed by remember(cleanThumb) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A2E25), EmeraldDeep, Color(0xFF1A3C34))
                )
            )
            .border(
                width = if (isHighlighted) 1.6.dp else 1.dp,
                color = if (isHighlighted) GoldBright else GoldPrimary.copy(alpha = 0.28f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (cleanThumb.isNotBlank() && !failed) {
            AsyncImage(
                model = cleanThumb,
                contentDescription = categoryLabel,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Islamic geometric backdrop when no custom cover image is available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GoldPrimary.copy(alpha = 0.22f),
                                EmeraldPrimary.copy(alpha = 0.14f),
                                EmeraldDeep
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                IslamicRosetteOrnament(size = 86.dp)
            }
        }

        // Bottom dark scrim for legible badges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // Centered Play Button Circle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.56f))
                .border(1.5.dp, GoldBright.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(30.dp)
            )
        }

        // Top-Start Category Pill
        if (categoryLabel.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = EmeraldDeep.copy(alpha = 0.85f),
                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f))
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldBright,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom-End Duration Chip
        if (durationLabel.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.78f)
            ) {
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Reusable dialog for publishing a new YouTube-style video (with cover photo, title,
 * description, channel/speaker name, category & video source) or editing an existing
 * published video from Super Admin panels.
 * Uses Android's zero-permission PickVisualMedia picker (Google Play policy compliant).
 */
@Composable
fun PublishOrEditVideoDialog(
    initial: IslamicVideoItem? = null,
    defaultChannelName: String = "",
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        channelName: String,
        category: IslamicVideoCategory,
        streamUrl: String,
        thumbnailUrl: String,
        durationLabel: String,
        isLocalUri: Boolean
    ) -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current

    var title by remember(initial) { mutableStateOf(initial?.titleAr.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.descriptionAr.orEmpty()) }
    var channelName by remember(initial) {
        mutableStateOf(
            initial?.subtitleAr?.ifBlank { initial.uploaderName } ?: defaultChannelName
        )
    }
    var category by remember(initial) {
        mutableStateOf(
            initial?.category?.takeIf { it != IslamicVideoCategory.ALL }
                ?: IslamicVideoCategory.QURAN_LESSONS
        )
    }
    var streamUrl by remember(initial) { mutableStateOf(initial?.streamUrl.orEmpty()) }
    var isLocalUri by remember(initial) { mutableStateOf(initial?.isLocalUri ?: false) }
    var thumbnailUrl by remember(initial) { mutableStateOf(initial?.thumbnailUrl.orEmpty()) }
    var durationLabel by remember(initial) { mutableStateOf(initial?.durationLabel.orEmpty()) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            streamUrl = uri.toString()
            isLocalUri = true
            if (title.isBlank()) {
                title = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                    ?.replace('_', ' ')
                    ?.ifBlank { "" }
                    .orEmpty()
            }
        }
    }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            thumbnailUrl = uri.toString()
        }
    }

    val publishableCategories = remember {
        IslamicVideoCategory.entries.filter { it != IslamicVideoCategory.ALL }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) {
                    L10n.pick(lang, "نشر فيديو جديد (بغلاف ووصف)", "Publish New Video", "Yangi video nashr qilish")
                } else {
                    L10n.pick(lang, "تعديل بيانات الفيديو", "Edit Video Details", "Video ma'lumotlarini tahrirlash")
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live 16:9 YouTube Cover Preview
                YouTubeVideoThumbnailBox(
                    thumbnailUrl = thumbnailUrl,
                    categoryLabel = L10n.pick(lang, category.labelAr, category.labelEn, category.labelUz),
                    durationLabel = durationLabel,
                    isHighlighted = thumbnailUrl.isNotBlank()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_dialog_pick_cover_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            L10n.pick(lang, "صورة الغلاف", "Cover Photo", "Muqova rasmi"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_dialog_pick_file_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            L10n.pick(lang, "اختيار فيديو", "Pick Video", "Video tanlash"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(L10n.pick(lang, "عنوان الفيديو (الاسم)", "Video Title", "Video nomi")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_dialog_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(L10n.pick(lang, "وصف الفيديو", "Description", "Video tavsifi")) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_dialog_desc_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        label = { Text(L10n.pick(lang, "القناة / الشيخ", "Channel / Speaker", "Kanal / Ustoz")) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = durationLabel,
                        onValueChange = { durationLabel = it },
                        label = { Text(L10n.pick(lang, "المدة", "Duration", "Davomiyligi")) },
                        placeholder = { Text("12:30") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Text(
                    text = L10n.pick(lang, "القسم:", "Category:", "Bo'lim:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(publishableCategories, key = { it.key }) { cat ->
                        LiquidGlassChip(
                            label = L10n.pick(lang, cat.labelAr, cat.labelEn, cat.labelUz),
                            selected = category == cat,
                            onClick = { category = cat }
                        )
                    }
                }

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = {
                        streamUrl = it
                        isLocalUri = it.startsWith("content://", ignoreCase = true) || it.startsWith("file://", ignoreCase = true)
                    },
                    label = { Text(L10n.pick(lang, "رابط الفيديو أو مسار الملف", "Video URL or Selected File", "Video havolasi yoki fayli")) },
                    placeholder = { Text("https://.../video.mp4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_dialog_url_input")
                )

                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text(L10n.pick(lang, "رابط صورة الغلاف (اختياري)", "Cover Image URL (optional)", "Muqova rasm havolasi (ixtiyoriy)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.trim(),
                        description.trim(),
                        channelName.trim(),
                        category,
                        streamUrl.trim(),
                        thumbnailUrl.trim(),
                        durationLabel.trim(),
                        isLocalUri
                    )
                },
                enabled = title.trim().isNotEmpty() && streamUrl.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                modifier = Modifier.testTag("video_dialog_save_btn")
            ) {
                Text(
                    if (initial == null) {
                        L10n.pick(lang, "نشر الفيديو", "Publish Video", "Nashr qilish")
                    } else {
                        L10n.pick(lang, "حفظ التعديلات", "Save Changes", "O'zgarishlarni saqlash")
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel", "Bekor qilish"))
            }
        }
    )
}
