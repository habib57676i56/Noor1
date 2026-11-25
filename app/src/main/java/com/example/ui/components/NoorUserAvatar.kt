package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary

/**
 * Circular NOOR account profile avatar with gold border ring.
 * Displays the user's Google account photo or custom picked avatar URI via Coil,
 * gracefully falling back to the user's first-letter monogram or a Mosque emblem.
 */
@Composable
fun NoorUserAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 46.dp,
    showEditBadge: Boolean = false,
    contentDescription: String = "Account Profile Photo",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanUrl = photoUrl?.trim().orEmpty()
    var imageFailed by remember(cleanUrl) { mutableStateOf(false) }
    val initialChar = remember(displayName) {
        displayName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    }

    Box(
        modifier = modifier
            .size(size)
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDeep)))
                .border(1.5.dp, GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (cleanUrl.isNotBlank() && !imageFailed) {
                AsyncImage(
                    model = cleanUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (initialChar.isNotBlank()) {
                Text(
                    text = initialChar,
                    style = if (size >= 52.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Mosque,
                    contentDescription = contentDescription,
                    tint = GoldBright,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary)
                    .border(1.5.dp, EmeraldDeep, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = EmeraldDeep,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
