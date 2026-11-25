package com.example.ui.screens.ecosystem

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VideoCall
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
import com.example.ui.components.NoorUserAvatar
import com.example.ui.components.PublishOrEditVideoDialog
import com.example.ui.components.YouTubeVideoThumbnailBox
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
    initialCategory: IslamicVideoCategory = IslamicVideoCategory.ALL,
    currentUserName: String = "",
    currentUserAvatarUrl: String = ""
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val selectedCollection by viewModel.selectedVideoCollection.collectAsStateWithLifecycle()
    val episodes by viewModel.videoEpisodes.collectAsStateWithLifecycle()
    val publishedVideos by viewModel.userVideos.collectAsStateWithLifecycle()
    val isLoading by viewModel.videosLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.videosError.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideoItem.collectAsStateWithLifecycle()

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var showPublishDialog by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (episodes.isEmpty() && selectedCollection != null) {
            viewModel.loadVideoCollection(selectedCollection!!)
        }
    }

    val filteredPublishedVideos = remember(publishedVideos, selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        publishedVideos.filter { v ->
            val catMatch = selectedCategory == IslamicVideoCategory.ALL ||
                selectedCategory == IslamicVideoCategory.MY_VIDEOS ||
                v.category == selectedCategory
            val qMatch = q.isBlank() ||
                v.titleAr.contains(q, ignoreCase = true) ||
                v.titleEn.contains(q, ignoreCase = true) ||
                v.descriptionAr.contains(q, ignoreCase = true) ||
                v.subtitleAr.contains(q, ignoreCase = true) ||
                v.uploaderName.contains(q, ignoreCase = true)
            catMatch && qMatch
        }
    }

    val filteredCollections = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        IslamicVideoCatalog.curatedCollections.filter { col ->
            val catMatch = selectedCategory == IslamicVideoCategory.ALL || col.category == selectedCategory
            val qMatch = q.isBlank() ||
                col.titleAr.contains(q, ignoreCase = true) ||
                col.titleEn.contains(q, ignoreCase = true) ||
                col.titleUz.contains(q, ignoreCase = true) ||
                col.subtitleAr.contains(q, ignoreCase = true)
            catMatch && qMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "فيديوهات نور الإسلامية", "NOOR Islamic Videos", "NOOR islomiy videolari"),
            subtitle = L10n.pick(
                lang,
                "دروس قرآنية · كرتون إسلامي · انشر فيديوهاتك مع غلافها",
                "Quran Lessons · Islamic Cartoons · Publish with Cover",
                "Qur'on darslari · Islomiy multfilmlar · Muqova bilan nashr qiling"
            ),
            onBack = onBack,
            trailing = {
                Button(
                    onClick = { showPublishDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("videos_publish_btn")
                ) {
                    Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        L10n.pick(lang, "نشر", "Publish", "Nashr"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Active Video Player Card
            activeVideo?.let { playing ->
                item(key = "active_player_${playing.id}") {
                    val playlist = if (playing.isUserPublished || playing.isLocalUri) publishedVideos else episodes
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

            // 2. Search bar + Quick Publish CTA
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = {
                        Text(
                            L10n.pick(
                                lang,
                                "ابحث عن درس قرآني أو كرتون إسلامي أو فيديو منشور...",
                                "Search Quran lessons, cartoons, or published videos...",
                                "Qur'on darsi, multfilm yoki video qidirish..."
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("videos_search_input")
                )
            }

            // 3. YouTube-Style Published Videos Feed (With Cover Thumbnail, Channel Avatar, Title & Description)
            if (filteredPublishedVideos.isNotEmpty() || selectedCategory == IslamicVideoCategory.MY_VIDEOS) {
                item {
                    NoorSectionHeader(
                        title = L10n.pick(
                            lang,
                            "الفيديوهات المنشورة (${filteredPublishedVideos.size})",
                            "Published Videos (${filteredPublishedVideos.size})",
                            "Nashr qilingan videolar (${filteredPublishedVideos.size})"
                        ),
                        actionLabel = L10n.pick(lang, "+ نشر فيديو", "+ Publish", "+ Nashr"),
                        onActionClick = { showPublishDialog = true }
                    )
                }
                if (filteredPublishedVideos.isEmpty()) {
                    item {
                        LiquidGlassCard(goldBorder = true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "انشر أول فيديو إسلامي مع غلافه واسمه ووصفه مثل يوتيوب! سيظهر هنا وفي لوحة السوبر أدمن للتعديل أو الحذف.",
                                        "Publish your first video with a cover thumbnail, title & description just like YouTube. Super Admin can edit or delete it anytime.",
                                        "Muqova rasmi, nomi va tavsifi bilan birinchi videongizni YouTube kabi nashr qiling! Super Admin uni tahrirlashi yoki o'chirishi mumkin."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showPublishDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        L10n.pick(lang, "نشر فيديو مع غلافه", "Publish Video with Cover", "Muqovali video nashr qilish"),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPublishedVideos, key = { "pub_${it.id}" }) { video ->
                        YouTubePublishedVideoCard(
                            item = video,
                            isPlaying = activeVideo?.id == video.id,
                            onPlay = { viewModel.selectVideoToPlay(video) },
                            onDelete = { viewModel.removeUserVideoItem(video.id) }
                        )
                    }
                }
            }

            // 4. Selected Curated Series Episodes (Real MP4 links loaded from Archive.org Metadata JSON)
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
                                    Text(L10n.pick(lang, "إعادة التحميل", "Retry", "Qayta urinish"))
                                }
                            }
                        } else if (episodes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "الحلقات المتاحة (${episodes.size}):",
                                        "Available Episodes (${episodes.size}):",
                                        "Mavjud qismlar (${episodes.size}):"
                                    ),
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

            // 5. YouTube-Style Curated Quran Lessons & Islamic Cartoon Series Cards
            if (selectedCategory != IslamicVideoCategory.MY_VIDEOS) {
                item {
                    NoorSectionHeader(
                        title = L10n.pick(
                            lang,
                            "السلاسل الإسلامية والكرتونية (${filteredCollections.size})",
                            "Islamic & Cartoon Series (${filteredCollections.size})",
                            "Islomiy darslar va multseriallar (${filteredCollections.size})"
                        )
                    )
                }
                items(filteredCollections, key = { it.id }) { col ->
                    val isSelected = selectedCollection?.id == col.id
                    YouTubeCuratedSeriesCard(
                        collection = col,
                        isSelected = isSelected,
                        onClick = { viewModel.loadVideoCollection(col) }
                    )
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishOrEditVideoDialog(
            initial = null,
            defaultChannelName = currentUserName,
            onDismiss = { showPublishDialog = false },
            onSave = { title, desc, channel, cat, url, thumb, duration, isLocal ->
                viewModel.addUserVideoItem(
                    title = title,
                    streamUrl = url,
                    isLocalUri = isLocal,
                    categoryLabelAr = L10n.pick(lang, cat.labelAr, cat.labelEn, cat.labelUz),
                    description = desc,
                    channelName = channel,
                    thumbnailUrl = thumb,
                    durationLabel = duration,
                    category = cat,
                    uploaderName = currentUserName.ifBlank { channel },
                    uploaderAvatarUrl = currentUserAvatarUrl
                )
                showPublishDialog = false
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
private fun YouTubePublishedVideoCard(
    item: IslamicVideoItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val catLabel = L10n.pick(lang, item.category.labelAr, item.category.labelEn, item.category.labelUz)
    val channelDisplay = item.subtitleAr.ifBlank { item.uploaderName }.ifBlank { item.sourceLabel }
    val descDisplay = L10n.pick(lang, item.descriptionAr, item.descriptionEn.ifBlank { item.descriptionAr })

    LiquidGlassCard(
        goldBorder = isPlaying,
        onClick = onPlay,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.testTag("youtube_published_video_${item.id}")
    ) {
        YouTubeVideoThumbnailBox(
            thumbnailUrl = item.thumbnailUrl,
            categoryLabel = catLabel,
            durationLabel = item.durationLabel,
            isHighlighted = isPlaying
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            NoorUserAvatar(
                photoUrl = item.uploaderAvatarUrl,
                displayName = channelDisplay,
                size = 40.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(lang, item.titleAr, item.titleEn.ifBlank { item.titleAr }),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) noorColors.goldAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$channelDisplay · $catLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = noorColors.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (descDisplay.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = descDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_published_video_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = L10n.pick(lang, "حذف الفيديو", "Delete Video", "Videoni o'chirish"),
                    tint = SemanticError
                )
            }
        }
    }
}

@Composable
private fun YouTubeCuratedSeriesCard(
    collection: IslamicVideoCollection,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val catLabel = L10n.pick(lang, collection.category.labelAr, collection.category.labelEn, collection.category.labelUz)
    val studioLabel = L10n.pick(lang, collection.speakerOrStudioAr, collection.speakerOrStudioEn)

    LiquidGlassCard(
        goldBorder = isSelected,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.testTag("video_collection_${collection.id}")
    ) {
        YouTubeVideoThumbnailBox(
            thumbnailUrl = collection.coverThumbnailUrl,
            categoryLabel = catLabel,
            isHighlighted = isSelected
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            NoorUserAvatar(
                photoUrl = null,
                displayName = studioLabel,
                size = 38.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(lang, collection.titleAr, collection.titleEn, collection.titleUz),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) noorColors.goldAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$studioLabel · $catLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = noorColors.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = L10n.pick(lang, collection.subtitleAr, collection.subtitleEn, collection.subtitleUz),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
        if (item.descriptionAr.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = L10n.pick(lang, item.descriptionAr, item.descriptionEn.ifBlank { item.descriptionAr }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
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
                                    "Could not play video stream ($what/$extra). Check connection or try another episode.",
                                    "Videoni ijro etib bo'lmadi ($what/$extra). Internetni tekshiring."
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
                        Text(L10n.pick(lang, "إعادة المحاولة", "Retry", "Qayta urinish"))
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
