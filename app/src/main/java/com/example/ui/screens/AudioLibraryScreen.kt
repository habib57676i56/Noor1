package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackState
import com.example.data.local.catalog.AudioLibraryCatalog
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.QuranReciter
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.MosqueHorizonSilhouette
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.OrnamentalSurahBadge
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldForest
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import java.util.Locale

@Composable
fun AudioLibraryScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val favoriteAudioIds by viewModel.favoriteAudioIds.collectAsStateWithLifecycle()
    val recentAudio by viewModel.recentAudioTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.audioPlaylists.collectAsStateWithLifecycle()
    val playlistItems by viewModel.audioPlaylistItems.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedAudioIds.collectAsStateWithLifecycle()
    val downloadingIds by viewModel.downloadingTrackIds.collectAsStateWithLifecycle()

    // 0: Yasser Al-Dosari, 1: All Reciters, 2: Adhan & Takbeer, 3: Supplications, 4: Favorites, 5: Recent, 6: Playlists
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedReciter by remember { mutableStateOf(QuranReciter.ALAFASY) }
    var searchQuery by remember { mutableStateOf("") }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackForPlaylistDialog by remember { mutableStateOf<AudioTrackItem?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }

    val yasserTracks = remember { AudioLibraryCatalog.yasserAlDosariTracks() }
    val reciterTracks = remember(selectedReciter) { AudioLibraryCatalog.surahTracksForReciter(selectedReciter) }
    val adhanTracks = remember { AudioLibraryCatalog.adhanAndTakbeeratTracks }
    val duaTracks = remember { AudioLibraryCatalog.curatedSupplicationRecitations }

    val allKnownTracksById = remember {
        val map = LinkedHashMap<String, AudioTrackItem>()
        QuranReciter.entries.forEach { rec ->
            AudioLibraryCatalog.surahTracksForReciter(rec).forEach { map[it.id] = it }
        }
        adhanTracks.forEach { map[it.id] = it }
        duaTracks.forEach { map[it.id] = it }
        map
    }

    val displayedTracks: List<AudioTrackItem> = remember(
        selectedTab, selectedReciter, searchQuery, favoriteAudioIds, selectedPlaylistId, playlistItems
    ) {
        val base = when (selectedTab) {
            0 -> yasserTracks
            1 -> reciterTracks
            2 -> adhanTracks
            3 -> duaTracks
            4 -> favoriteAudioIds.mapNotNull { allKnownTracksById[it] }
            6 -> {
                val pid = selectedPlaylistId
                if (pid != null) {
                    playlistItems.filter { it.playlistId == pid }.mapNotNull { allKnownTracksById[it.trackId] }
                } else emptyList()
            }
            else -> emptyList()
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) base else base.filter {
            it.titleAr.contains(q, ignoreCase = true) ||
                it.titleEn.contains(q, ignoreCase = true) ||
                it.subtitleAr.contains(q, ignoreCase = true) ||
                it.subtitleEn.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_library_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "المكتبة الصوتية الإسلامية", "Islamic Audio Library"),
            subtitle = L10n.pick(lang, "ياسر الدوسري — تلاوات القرآن الكريم", "YASSER AL-DOSARI — QURAN RECITATIONS"),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Featured Yasser Al-Dosari & Live Player Hero Card
            item {
                FeaturedAudioPlayerCard(
                    audioState = audioState,
                    isFavorite = audioState.currentTrack?.id?.let { favoriteAudioIds.contains(it) } == true,
                    onPlayDefaultYasser = {
                        viewModel.playAudioTrack(yasserTracks.first(), yasserTracks)
                    },
                    onTogglePlayPause = { viewModel.toggleAudioPauseResume() },
                    onNext = { viewModel.playNextAudio() },
                    onPrevious = { viewModel.playPreviousAudio() },
                    onSeek = { viewModel.seekAudioTo(it) },
                    onCycleSpeed = {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                        val idx = speeds.indexOfFirst { kotlin.math.abs(it - audioState.playbackSpeed) < 0.05f }
                        val next = speeds[(idx + 1) % speeds.size]
                        viewModel.setAudioPlaybackSpeed(next)
                    },
                    onCycleRepeat = { viewModel.cycleAudioRepeatMode() },
                    onOpenSleepTimer = { showSleepDialog = true },
                    onToggleFavorite = {
                        audioState.currentTrack?.let { viewModel.toggleFavoriteAudio(it.id) }
                    }
                )
            }

            // Audio Quality & Background Playback Strip (NOOR 3.0)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, "الجودة:", "Quality:"),
                        style = MaterialTheme.typography.labelMedium,
                        color = noorColors.goldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    listOf("64 kbps", "128 kbps", "192 kbps").forEach { q ->
                        LiquidGlassChip(
                            label = q,
                            selected = prefs.audioQualityLabel == q,
                            onClick = { viewModel.setAudioQualityLabel(q) }
                        )
                    }
                }
            }

            // Category Tabs
            item {
                val tabs = listOf(
                    0 to L10n.pick(lang, "ياسر الدوسري", "Yasser Al-Dosari"),
                    1 to L10n.pick(lang, "القراء", "Reciters"),
                    2 to L10n.pick(lang, "الأذان والتكبير", "Adhan"),
                    3 to L10n.pick(lang, "الأدعية", "Duas"),
                    4 to L10n.pick(lang, "المفضلة", "Favorites"),
                    5 to L10n.pick(lang, "الأخيرة", "Recent"),
                    6 to L10n.pick(lang, "قوائم التشغيل", "Playlists")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs) { (idx, label) ->
                        LiquidGlassChip(
                            label = label,
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx }
                        )
                    }
                }
            }

            // Reciter Picker when on "All Reciters" tab
            if (selectedTab == 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuranReciter.entries) { reciter ->
                            LiquidGlassChip(
                                label = L10n.pick(lang, reciter.nameAr, reciter.nameEn),
                                selected = selectedReciter == reciter,
                                onClick = { selectedReciter = reciter }
                            )
                        }
                    }
                }
            }

            // Search bar for track lists
            if (selectedTab in listOf(0, 1, 2, 3, 4)) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = {
                            Text(
                                L10n.pick(lang, "ابحث عن سورة أو تلاوة...", "Search Surah or recitation..."),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }

            // Section Title for Yasser Al-Dosari
            if (selectedTab == 0) {
                item {
                    LiquidGlassCard(
                        goldBorder = true,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = noorColors.goldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = L10n.pick(lang, "الشيخ ياسر الدوسري — المصحف المرتل الكامل (١١٤ سورة)", "YASSER AL-DOSARI — COMPLETE 114 SURAH RECITATIONS"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "بث مباشر موثوق مع إمكانية الحفظ المؤقت للاستماع دون إنترنت (MP3Quran Open Archive).",
                                        "Authorized streaming with optional private offline caching (MP3Quran Open Archive)."
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recently Played Tab
            if (selectedTab == 5) {
                if (recentAudio.isEmpty()) {
                    item {
                        EmptyAudioStateCard(
                            title = L10n.pick(lang, "لا توجد تلاوات مشغلة مؤخراً", "No Recently Played Audio"),
                            subtitle = L10n.pick(lang, "ابدأ بتشغيل أي سورة من تلاوات الشيخ ياسر الدوسري لتظهر هنا.", "Play any recitation to see your recent listening history here.")
                        )
                    }
                } else {
                    items(recentAudio, key = { it.trackId }) { recent ->
                        LiquidGlassCard(
                            onClick = { viewModel.playRecentAudioEntity(recent) },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = noorColors.goldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, recent.titleAr, recent.titleEn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(lang, recent.reciterNameAr, recent.reciterNameEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = noorColors.goldAccent
                                )
                            }
                        }
                    }
                }
            }

            // Playlists Tab
            if (selectedTab == 6) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(lang, "قوائم التشغيل الخاصة بك", "Your Audio Playlists"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LiquidGlassChip(
                            label = L10n.pick(lang, "قائمة جديدة", "New Playlist"),
                            selected = true,
                            icon = Icons.Rounded.Add,
                            onClick = { showCreatePlaylistDialog = true }
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    item {
                        EmptyAudioStateCard(
                            title = L10n.pick(lang, "لم تنشئ قوائم تشغيل بعد", "No Playlists Yet"),
                            subtitle = L10n.pick(lang, "أنشئ قائمة مثل (سور النوم) أو (تلاوات ياسر الدوسري) وأضف إليها السور المفضلة.", "Create playlists like 'Before Sleep' or 'Yasser Favorites' and add your favorite Surahs.")
                        )
                    }
                } else {
                    items(playlists, key = { it.id }) { pl ->
                        val count = playlistItems.count { it.playlistId == pl.id }
                        val isSelected = selectedPlaylistId == pl.id
                        LiquidGlassCard(
                            goldBorder = isSelected,
                            onClick = { selectedPlaylistId = if (isSelected) null else pl.id },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = noorColors.goldAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, pl.nameAr, pl.nameEn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(lang, "$count مقاطع صوتية", "$count tracks"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val plTracks = playlistItems
                                            .filter { it.playlistId == pl.id }
                                            .mapNotNull { allKnownTracksById[it.trackId] }
                                        if (plTracks.isNotEmpty()) {
                                            viewModel.playAudioTrack(plTracks.first(), plTracks)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Playlist", tint = noorColors.goldAccent)
                                }
                                IconButton(onClick = { viewModel.deleteAudioPlaylist(pl.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Track List (for Yasser, Reciters, Adhan, Duas, Favorites, or selected Playlist)
            if (selectedTab != 5 && (selectedTab != 6 || selectedPlaylistId != null)) {
                if (displayedTracks.isEmpty() && selectedTab == 4) {
                    item {
                        EmptyAudioStateCard(
                            title = L10n.pick(lang, "لا توجد تلاوات في المفضلة", "No Favorite Recitations"),
                            subtitle = L10n.pick(lang, "اضغط على أيقونة القلب بجانب أي سورة لحفظها هنا.", "Tap the heart icon next to any Surah to save it here.")
                        )
                    }
                } else {
                    items(displayedTracks, key = { it.id }) { track ->
                        val isCurrent = audioState.currentTrack?.id == track.id
                        val isFav = favoriteAudioIds.contains(track.id)
                        val isDownloaded = downloadedIds.contains(track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_"))
                        val isDownloading = downloadingIds.contains(track.id)

                        AudioTrackRowCard(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && audioState.isPlaying,
                            isFavorite = isFav,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            onPlay = {
                                if (isCurrent) {
                                    viewModel.toggleAudioPauseResume()
                                } else {
                                    viewModel.playAudioTrack(track, displayedTracks)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavoriteAudio(track.id) },
                            onAddToPlaylist = { trackForPlaylistDialog = track },
                            onToggleDownload = {
                                if (isDownloaded) {
                                    viewModel.deleteOfflineTrack(track.id)
                                } else {
                                    viewModel.downloadTrackOffline(track)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text(L10n.pick(lang, "مؤقت النوم الصوتي", "Audio Sleep Timer")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(null to L10n.pick(lang, "إيقاف المؤقت", "Turn Off"), 10 to "10 min", 15 to "15 min", 30 to "30 min", 60 to "60 min")
                    options.forEach { (mins, label) ->
                        LiquidGlassCard(
                            onClick = {
                                viewModel.setAudioSleepTimer(mins)
                                showSleepDialog = false
                            },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(L10n.pick(lang, "إنشاء قائمة تشغيل جديدة", "Create Audio Playlist")) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = { Text(L10n.pick(lang, "مثلاً: تلاوات المساء", "e.g. Evening Recitations")) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createAudioPlaylist(newName)
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text(L10n.pick(lang, "إنشاء", "Create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Add Track to Playlist Dialog
    trackForPlaylistDialog?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForPlaylistDialog = null },
            title = { Text(L10n.pick(lang, "إضافة إلى قائمة تشغيل", "Add to Playlist")) },
            text = {
                if (playlists.isEmpty()) {
                    Text(L10n.pick(lang, "لا توجد قوائم تشغيل بعد. أنشئ قائمة من تبويب قوائم التشغيل.", "No playlists yet. Create one in the Playlists tab."))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { pl ->
                            LiquidGlassCard(
                                onClick = {
                                    viewModel.addTrackToPlaylist(pl.id, track.id)
                                    trackForPlaylistDialog = null
                                },
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text(L10n.pick(lang, pl.nameAr, pl.nameEn), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    trackForPlaylistDialog = null
                    showCreatePlaylistDialog = true
                }) {
                    Text(L10n.pick(lang, "قائمة جديدة", "New Playlist"))
                }
            },
            dismissButton = {
                TextButton(onClick = { trackForPlaylistDialog = null }) {
                    Text(L10n.pick(lang, "إغلاق", "Close"))
                }
            }
        )
    }
}

@Composable
private fun FeaturedAudioPlayerCard(
    audioState: AudioPlaybackState,
    isFavorite: Boolean,
    onPlayDefaultYasser: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A3224), Color(0xFF051812), Color(0xFF040E0B))
                )
            )
            .border(
                1.2.dp,
                Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.7f), GoldPrimary.copy(alpha = 0.2f))),
                RoundedCornerShape(28.dp)
            )
    ) {
        MosqueHorizonSilhouette(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .align(Alignment.BottomCenter),
            tint = GoldPrimary.copy(alpha = 0.16f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Golden Islamic Arch Emblem
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val arch = Path().apply {
                        moveTo(w * 0.14f, h * 0.92f)
                        lineTo(w * 0.14f, h * 0.45f)
                        cubicTo(w * 0.14f, h * 0.16f, w * 0.5f, h * 0.04f, w * 0.5f, h * 0.04f)
                        cubicTo(w * 0.5f, h * 0.04f, w * 0.86f, h * 0.16f, w * 0.86f, h * 0.45f)
                        lineTo(w * 0.86f, h * 0.92f)
                        close()
                    }
                    drawPath(
                        path = arch,
                        brush = Brush.verticalGradient(listOf(Color(0xFF165B42), Color(0xFF08241A)))
                    )
                    drawPath(
                        path = arch,
                        color = GoldBright,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = GoldPrimary.copy(alpha = 0.25f),
                        radius = w * 0.18f,
                        center = Offset(w * 0.5f, h * 0.54f)
                    )
                }
                Icon(
                    imageVector = if (audioState.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Mosque,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val displayTitle = if (audioState.hasActiveSession) {
                L10n.pick(lang, audioState.titleAr, audioState.titleEn)
            } else {
                L10n.pick(lang, "الشيخ ياسر الدوسري", "Sheikh Yasser Al-Dosari")
            }
            val displaySub = if (audioState.hasActiveSession) {
                L10n.pick(lang, audioState.subtitleAr, audioState.subtitleEn)
            } else {
                L10n.pick(lang, "تلاوات القرآن الكريم · اضغط للتشغيل", "Quran Recitations · Tap to Listen")
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = displaySub,
                style = MaterialTheme.typography.bodySmall,
                color = GoldBright.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (audioState.isOfflineFile) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L10n.pick(lang, "يعمل من الذاكرة المحلية دون إنترنت", "Playing from offline cache"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7BE8B4)
                )
            }

            audioState.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFB4A9),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seek Slider
            val duration = audioState.durationMs.coerceAtLeast(1L)
            val progress = (audioState.currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Slider(
                value = if (audioState.durationMs > 0) progress else 0f,
                onValueChange = { frac ->
                    if (audioState.durationMs > 0) {
                        onSeek((frac * audioState.durationMs).toLong())
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = Color(0xFF39D98A),
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMillis(audioState.currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = if (audioState.durationMs > 0) formatMillis(audioState.durationMs) else "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (audioState.repeatMode == AudioRepeatMode.REPEAT_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (audioState.repeatMode != AudioRepeatMode.OFF) GoldBright else Color.White.copy(alpha = 0.55f)
                    )
                }
                IconButton(onClick = onPrevious, enabled = audioState.hasPrevious || audioState.currentPositionMs > 4000L, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GoldBright, GoldPrimary)))
                        .clickable {
                            if (audioState.hasActiveSession) onTogglePlayPause() else onPlayDefaultYasser()
                        }
                        .testTag("audio_main_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (audioState.isBuffering) {
                        CircularProgressIndicator(
                            color = EmeraldDeep,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = EmeraldDeep,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = audioState.hasNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) GoldBright else Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Utility Pills: Speed + Sleep Timer
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassChip(
                    label = "${audioState.playbackSpeed}x",
                    selected = audioState.playbackSpeed != 1.0f,
                    icon = Icons.Rounded.Speed,
                    onClick = onCycleSpeed
                )
                val sleepLabel = audioState.sleepTimerRemainingSec?.let { sec ->
                    "${sec / 60}:${String.format(Locale.US, "%02d", sec % 60)}"
                } ?: L10n.pick(lang, "مؤقت النوم", "Sleep Timer")
                LiquidGlassChip(
                    label = sleepLabel,
                    selected = audioState.sleepTimerRemainingSec != null,
                    icon = Icons.Rounded.Bedtime,
                    onClick = onOpenSleepTimer
                )
            }
        }
    }
}

@Composable
private fun AudioTrackRowCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleDownload: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    LiquidGlassCard(
        goldBorder = isCurrent,
        onClick = onPlay,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track.surahNumber != null) {
                OrnamentalSurahBadge(number = track.surahNumber, size = 42.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, tint = noorColors.goldAccent)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(lang, track.titleAr, track.titleEn),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) noorColors.goldAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = L10n.pick(lang, track.subtitleAr, track.subtitleEn),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (track.allowOfflineCache) {
                IconButton(onClick = onToggleDownload, modifier = Modifier.size(40.dp)) {
                    if (isDownloading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Rounded.CloudDone else Icons.Rounded.Download,
                            contentDescription = "Offline Cache",
                            tint = if (isDownloaded) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) noorColors.goldAccent else EmeraldForest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isCurrent) EmeraldDeep else GoldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAudioStateCard(title: String, subtitle: String) {
    LiquidGlassCard(contentPadding = PaddingValues(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Headphones,
                contentDescription = null,
                tint = LocalNoorColors.current.goldAccent,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
