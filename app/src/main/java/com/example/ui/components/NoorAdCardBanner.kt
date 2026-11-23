package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.cloud.AdCardDoc
import com.example.data.cloud.AdPlacement
import com.example.data.cloud.NoorSecurityValidators
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean, respectful, Super-Admin-controlled Advertisement Card Banner.
 *
 * Strictly enforced rules:
 * - Zero third-party ad networks or external tracking SDKs.
 * - Only renders in explicitly approved placements (HOME, ECOSYSTEM_HUB, AUDIO_LIBRARY).
 * - Never shown inside Quran reading, recitation, memorization (Hifz), or Prayer Times.
 * - Optional destination links are strictly validated for https:// before launching.
 */
@Composable
fun NoorAdCardBannerSection(
    adCards: List<AdCardDoc>,
    placement: AdPlacement,
    onImpression: (String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!AdPlacement.isAllowedPlacement(placement.key)) return

    val matchingCards = remember(adCards, placement) {
        val now = System.currentTimeMillis()
        adCards
            .filter { it.placement.equals(placement.key, ignoreCase = true) && it.isCurrentlyActive(now) }
            .sortedWith(compareBy<AdCardDoc> { it.displayOrder }.thenBy { it.titleAr })
            .take(3)
    }

    if (matchingCards.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("noor_ad_banner_${placement.key.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        matchingCards.forEach { card ->
            LaunchedEffect(card.id) {
                onImpression(card.id)
            }
            NoorAdCardItem(
                card = card,
                onClick = { onClick(card.id) }
            )
        }
    }
}

@Composable
fun NoorAdCardItem(
    card: AdCardDoc,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val title = L10n.pick(lang, card.titleAr.ifBlank { card.titleEn }, card.titleEn.ifBlank { card.titleAr })
    val description = L10n.pick(
        lang,
        card.descriptionAr.ifBlank { card.descriptionEn },
        card.descriptionEn.ifBlank { card.descriptionAr }
    )
    val hasValidLink = card.destinationUrl.isNotBlank() &&
        NoorSecurityValidators.isSafeOptionalHttpsUrl(card.destinationUrl)

    val handleTap: () -> Unit = {
        onClick()
        if (!isPreview && hasValidLink) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.destinationUrl.trim()))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                // Gracefully ignore if no browser handles the intent
            }
        }
    }

    LiquidGlassCard(
        goldBorder = true,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasValidLink && !isPreview) Modifier.clickable(onClick = handleTap) else Modifier)
            .testTag("noor_ad_card_${card.id.ifBlank { "preview" }}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldPrimary.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = noorColors.goldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = L10n.pick(lang, "إعلان إداري", "NOOR Notice"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                    }
                }
                if (hasValidLink) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, "فتح الرابط", "Open"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = noorColors.goldAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open link",
                            tint = noorColors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (card.imageUrl.isNotBlank() && NoorSecurityValidators.isSafeOptionalImageUri(card.imageUrl)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    NoorUriImage(
                        uriString = card.imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title.ifBlank { L10n.pick(lang, "عنوان الإعلان", "Announcement Title") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private object NoorUriBitmapCache {
    val cache = LruCache<String, ImageBitmap>(16)
}

/**
 * Lightweight, zero-dependency, offline-safe image loader supporting:
 * - Android Photo Picker content:// URIs
 * - Local file:// URIs
 * - Verified https:// image URLs
 * Decodes off the main thread with memory-safe RGB_565 downsampling for low-RAM devices.
 */
@Composable
fun NoorUriImage(
    uriString: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf(NoorUriBitmapCache.cache.get(uriString)) }

    LaunchedEffect(uriString) {
        if (bitmap != null || uriString.isBlank()) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = uriString.trim()
                val rawBytes: ByteArray? = when {
                    trimmed.startsWith("content://", ignoreCase = true) -> {
                        context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { it.readBytes() }
                    }
                    trimmed.startsWith("file://", ignoreCase = true) -> {
                        val path = Uri.parse(trimmed).path ?: return@runCatching null
                        val file = File(path)
                        if (file.exists() && file.canRead()) file.readBytes() else null
                    }
                    trimmed.startsWith("https://", ignoreCase = true) -> {
                        val conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6000
                            readTimeout = 8000
                            instanceFollowRedirects = true
                        }
                        try {
                            if (conn.responseCode in 200..299) {
                                conn.inputStream.use { it.readBytes() }
                            } else {
                                null
                            }
                        } finally {
                            conn.disconnect()
                        }
                    }
                    else -> null
                }
                if (rawBytes == null || rawBytes.size < 16) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1080 || bounds.outHeight / sample > 1080) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) {
            NoorUriBitmapCache.cache.put(uriString, loaded)
            bitmap = loaded
        }
    }

    bitmap?.let { img ->
        Image(
            bitmap = img,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

