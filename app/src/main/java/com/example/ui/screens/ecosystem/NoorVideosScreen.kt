package com.example.ui.screens.ecosystem

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.IslamicVideoCatalog
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoCollection
import com.example.data.local.catalog.IslamicVideoItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.viewmodel.EcosystemViewModel
import kotlinx.coroutines.delay

@Composable
fun NoorVideosScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    initialCategory: IslamicVideoCategory = IslamicVideoCategory.ALL
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    val selectedCollection by viewModel.selectedVideoCollection.collectAsStateWithLifecycle()
    val episodes by viewModel.videoEpisodes.collectAsStateWithLifecycle()
    val userVideos by viewModel.userVideos.collectAsStateWithLifecycle()
    val isLoading by viewModel.videosLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.videosError.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideoItem.collectAsStateWithLifecycle()

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for picking local MP4/video files safely
    val pickLocalVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "فيديو من الجهاز" } ?: "فيديو من الجهاز"
            viewModel.addUserVideoItem(
                title = displayName,
                streamUrl = uri.toString(),
                isLocalUri = true,
                categoryLabelAr = L10n.pick(lang, "فيديو محلي من الجهاز", "Device Video")
            )
            selectedCategory = IslamicVideoCategory.MY_VIDEOS
        }
    }

    LaunchedEffect(Unit) {
        if (episodes.isEmpty() && selectedCollection != null) {
            viewModel.loadVideoCollection(selectedCollection!!)
        }
    }

    val filteredCollections = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        IslamicVideoCatalog.curatedCollections.filter { col ->
            val catMatch = selectedCategory == IslamicVideoCategory.ALL || col.category == selectedCategory
            val qMatch = q.isBlank() ||
                col.titleAr.contains(q, ignoreCase = true) ||
                col.titleEn.contains(q, ignoreCase = true) ||
                col.subtitleAr.contains(q, ignoreCase = true)
            catMatch && qMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "فيديوهات نور الإسلامية", "NOOR Islamic Videos", "NOOR islomiy videolari"),
            subtitle = L10n.pick(
                lang,
                "دروس قرآنية وتجويد · كرتون إسلامي وقصص الأنبياء",
                "Quran & Tajweed Lessons · Islamic Cartoons & Stories",
                "Qur'on va Tajvid darslari · Islomiy multfilmlar"
            ),
            onBack = onBack,
            trailing = {
                Row {
                    IconButton(
                        onClick = {
                            pickLocalVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("videos_pick_local_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = L10n.pick(lang, "اختيار فيديو من الجهاز", "Pick Local Video"),
                            tint = GoldBright
                        )
                    }
                    IconButton(
                        onClick = { showAddUrlDialog = true },
                        modifier = Modifier.testTag("videos_add_link_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddLink,
                            contentDescription = L10n.pick(lang, "إضافة رابط فيديو", "Add Video Link"),
                            tint = GoldBright
                        )
                    }
                }
            }
        )

        // Category Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(IslamicVideoCategory.entries, key = { it.key }) { cat ->
                val label = L10n.pick(lang, cat.labelAr, cat.labelEn, cat.labelUz)
                LiquidGlassChip(
                    label = label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    modifier = Modifier.testTag("videos_cat_${cat.key.lowercase()}")
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Active Video Player Card
            activeVideo?.let { playing ->
                item(key = "active_player_${playing.id}") {
                    val playlist = if (playing.isLocalUri || playing.category == IslamicVideoCategory.MY_VIDEOS) userVideos else episodes
                    val curIdx = playlist.indexOfFirst { it.id == playing.id }
                    val nextEpisode = if (curIdx >= 0 && curIdx < playlist.lastIndex) playlist[curIdx + 1] else null

                    NoorInlineVideoPlayerCard(
                        item = playing,
                        hasNext = nextEpisode != null,
                        onPlayNext = { nextEpisode?.let { viewModel.selectVideoToPlay(it) } },
                        onFullscreen = { isFullscreen = true }
                    )
                }
            }

            // 2. Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = {
                        Text(
                            L10n.pick(
                                lang,
                                "ابحث عن درس قرآني أو كرتون إسلامي...",
                                "Search Quran lessons or Islamic cartoons...",
                                "Qur'on darsi yoki multfilm qidirish..."
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("videos_search_input")
                )
            }

            // 3. Selected Collection Episodes (Real MP4 links loaded from Archive.org Metadata JSON)
            if (selectedCategory != IslamicVideoCategory.MY_VIDEOS && selectedCollection != null) {
                val col = selectedCollection!!
                item(key = "collection_episodes_header") {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, col.titleAr, col.titleEn, col.titleUz),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = L10n.pick(lang, col.subtitleAr, col.subtitleEn, col.subtitleUz),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.loadVideoCollection(col, forceRefresh = true) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = noorColors.goldAccent)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (isLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GoldPrimary, strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    L10n.pick(lang, "جارٍ جلب الحلقات...", "Loading episodes...", "Qismlar yuklanmoqda..."),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else if (!loadError.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(loadError!!, style = MaterialTheme.typography.bodySmall, color = SemanticError)
                                OutlinedButton(onClick = { viewModel.loadVideoCollection(col, forceRefresh = true) }) {
                                    Text(L10n.pick(lang, "إعادة التحميل", "Retry"))
                                }
                            }
                        } else if (episodes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = L10n.pick(lang, "الحلقات المتاحة (${episodes.size}):", "Available Episodes (${episodes.size}):"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                episodes.take(25).forEach { ep ->
                                    val isPlayingThis = activeVideo?.id == ep.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.selectVideoToPlay(ep) }
                                            .testTag("video_episode_${ep.id}"),
                                        color = if (isPlayingThis) EmeraldPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPlayingThis) GoldPrimary else GoldPrimary.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isPlayingThis) GoldBright else noorColors.goldAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = L10n.pick(lang, ep.titleAr, ep.titleEn),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (ep.durationLabel.isNotBlank()) {
                                                Text(
                                                    text = ep.durationLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. User Local / Custom Added Videos Section
            if (selectedCategory == IslamicVideoCategory.MY_VIDEOS || userVideos.isNotEmpty()) {
                item {
                    NoorSectionHeader(
                        title = L10n.pick(lang, "فيديوهاتك المضافة (${userVideos.size})", "Your Saved Videos (${userVideos.size})")
                    )
                }
                if (userVideos.isEmpty()) {
                    item {
                        LiquidGlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "لم تضف فيديوهات محلية بعد. يمكنك اختيار فيديو من جهازك أو إضافة رابط درس مباشر.",
                                        "No local videos yet. Pick a video from your device or paste a direct stream link."
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = {
                                        pickLocalVideoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                                ) {
                                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(L10n.pick(lang, "اختر فيديو من الجهاز", "Pick Local Video"))
                                }
                            }
                        }
                    }
                } else {
                    items(userVideos, key = { "usr_${it.id}" }) { uv ->
                        LiquidGlassCard(
                            goldBorder = activeVideo?.id == uv.id,
                            onClick = { viewModel.selectVideoToPlay(uv) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = GoldBright)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(uv.titleAr, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(uv.sourceLabel, style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
                                }
                                IconButton(onClick = { viewModel.removeUserVideoItem(uv.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Video", tint = SemanticError)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Curated Quran Lessons & Islamic Cartoon Collections List
            if (selectedCategory != IslamicVideoCategory.MY_VIDEOS) {
                item {
                    NoorSectionHeader(
                        title = L10n.pick(
                            lang,
                            "السلاسل الإسلامية والكرتونية (${filteredCollections.size})",
                            "Islamic & Cartoon Series (${filteredCollections.size})"
                        )
                    )
                }
                items(filteredCollections, key = { it.id }) { col ->
                    val isSelected = selectedCollection?.id == col.id
                    LiquidGlassCard(
                        goldBorder = isSelected,
                        onClick = { viewModel.loadVideoCollection(col) },
                        modifier = Modifier.testTag("video_collection_${col.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (col.category == IslamicVideoCategory.ISLAMIC_CARTOONS) {
                                            GoldPrimary.copy(alpha = 0.2f)
                                        } else {
                                            EmeraldPrimary.copy(alpha = 0.22f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (col.category == IslamicVideoCategory.ISLAMIC_CARTOONS) Icons.Rounded.ChildCare else Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = if (col.category == IslamicVideoCategory.ISLAMIC_CARTOONS) GoldBright else EmeraldPrimary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, col.titleAr, col.titleEn, col.titleUz),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) noorColors.goldAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = L10n.pick(lang, col.subtitleAr, col.subtitleEn, col.subtitleUz),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, col.category.labelAr, col.category.labelEn, col.category.labelUz),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = noorColors.goldAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUrlDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddUrlDialog = false },
            title = { Text(L10n.pick(lang, "إضافة رابط فيديو مباشر (MP4)", "Add Direct Video Stream")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(L10n.pick(lang, "عنوان الدرس أو القصة", "Title")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("https://.../lesson.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addUserVideoItem(title, url, isLocalUri = false)
                        showAddUrlDialog = false
                        selectedCategory = IslamicVideoCategory.MY_VIDEOS
                    },
                    enabled = url.startsWith("http", ignoreCase = true)
                ) { Text(L10n.pick(lang, "تشغيل وحفظ", "Play & Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showAddUrlDialog = false }) { Text(L10n.pick(lang, "إلغاء", "Cancel")) }
            }
        )
    }

    if (isFullscreen && activeVideo != null) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                NoorInlineVideoPlayerCard(
                    item = activeVideo!!,
                    hasNext = false,
                    onPlayNext = {},
                    onFullscreen = { isFullscreen = false },
                    isFullscreen = true
                )
            }
        }
    }
}

@Composable
private fun NoorInlineVideoPlayerCard(
    item: IslamicVideoItem,
    hasNext: Boolean,
    onPlayNext: () -> Unit,
    onFullscreen: () -> Unit,
    isFullscreen: Boolean = false
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var retryNonce by remember(item.id) { mutableIntStateOf(0) }
    var isPreparing by remember(item.id, retryNonce) { mutableStateOf(true) }
    var isPlaying by remember(item.id, retryNonce) { mutableStateOf(false) }
    var durationMs by remember(item.id, retryNonce) { mutableIntStateOf(0) }
    var positionMs by remember(item.id, retryNonce) { mutableIntStateOf(0) }
    var errorMsg by remember(item.id, retryNonce) { mutableStateOf<String?>(null) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isPlaying, videoViewRef) {
        while (isPlaying) {
            val vv = videoViewRef
            if (vv != null) {
                runCatching {
                    positionMs = vv.currentPosition
                    if (durationMs <= 0 && vv.duration > 0) {
                        durationMs = vv.duration
                    }
                }
            }
            delay(500L)
        }
    }

    DisposableEffect(item.id) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            mediaPlayerRef = null
            videoViewRef = null
        }
    }

    LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(12.dp)) {
        Text(
            text = L10n.pick(lang, item.titleAr, item.titleEn),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = noorColors.goldAccent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${L10n.pick(lang, item.subtitleAr, item.subtitleEn)} · ${item.sourceLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            key(item.id, retryNonce) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                isPreparing = false
                                errorMsg = null
                                durationMs = mp.duration.coerceAtLeast(0)
                                applySpeedSafely(mp, speed)
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                if (hasNext) onPlayNext()
                            }
                            setOnErrorListener { _, what, extra ->
                                isPreparing = false
                                isPlaying = false
                                errorMsg = L10n.pick(
                                    lang,
                                    "تعذّر تشغيل الفيديو (رمز $what/$extra). تحقق من الاتصال بالإنترنت أو جرّب حلقة أخرى.",
                                    "Could not play video stream ($what/$extra). Check connection or try another episode."
                                )
                                true
                            }
                            setVideoURI(Uri.parse(item.streamUrl))
                            videoViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isPreparing && errorMsg == null) {
                CircularProgressIndicator(color = GoldBright)
            }

            if (errorMsg != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMsg!!, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            errorMsg = null
                            isPreparing = true
                            retryNonce += 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                    ) {
                        Text(L10n.pick(lang, "إعادة المحاولة", "Retry"))
                    }
                }
            }
        }

        // Progress Slider & Time
        if (durationMs > 0) {
            Spacer(Modifier.height(6.dp))
            Slider(
                value = positionMs.coerceIn(0, durationMs).toFloat(),
                onValueChange = { newPos ->
                    positionMs = newPos.toInt()
                    runCatching { videoViewRef?.seekTo(positionMs) }
                },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = GoldPrimary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }

        // Player Controls Bar
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                val target = (positionMs - 10_000).coerceAtLeast(0)
                positionMs = target
                runCatching { videoViewRef?.seekTo(target) }
            }) {
                Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = noorColors.goldAccent)
            }

            IconButton(
                onClick = {
                    val vv = videoViewRef ?: return@IconButton
                    if (isPlaying) {
                        runCatching { vv.pause() }
                        isPlaying = false
                    } else {
                        runCatching { vv.start() }
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary)
                    .testTag("video_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = EmeraldDeep
                )
            }

            IconButton(onClick = {
                val target = (positionMs + 10_000).coerceAtMost(durationMs.coerceAtLeast(positionMs + 10_000))
                positionMs = target
                runCatching { videoViewRef?.seekTo(target) }
            }) {
                Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = noorColors.goldAccent)
            }

            if (hasNext) {
                IconButton(onClick = onPlayNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next Episode", tint = noorColors.goldAccent)
                }
            }

            TextButton(onClick = {
                speed = when {
                    speed < 1.2f -> 1.25f
                    speed < 1.4f -> 1.5f
                    else -> 1.0f
                }
                mediaPlayerRef?.let { applySpeedSafely(it, speed) }
            }) {
                Text("${speed}x", fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
            }

            IconButton(onClick = onFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = noorColors.goldAccent
                )
            }
        }
    }
}

private fun applySpeedSafely(mp: MediaPlayer, speed: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        runCatching {
            val params = mp.playbackParams
            params.speed = speed
            mp.playbackParams = params
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
