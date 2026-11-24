package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.QuranReciter
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

/**
 * Single unified "Choose Reciter" button + selection menu shared across Quran Reading
 * and Quran Memorization screens.
 *
 * Replaces overcrowded multi-chip reciter rows with ONE clear button that shows the
 * active Sheikh, opens a clean modal list of all available reciters, indicates the
 * current selection, and closes automatically when a reciter is chosen.
 */
@Composable
fun ChooseReciterButton(
    selectedReciter: QuranReciter,
    onSelectReciter: (QuranReciter) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("choose_reciter_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.RecordVoiceOver,
            contentDescription = L10n.pick(lang, "اختر القارئ", "Choose Reciter"),
            tint = noorColors.goldAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = L10n.pick(lang, "اختر القارئ", "Choose Reciter"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = L10n.pick(lang, selectedReciter.nameAr, selectedReciter.nameEn),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = noorColors.goldAccent,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showPicker) {
        ReciterSelectionDialog(
            selectedReciter = selectedReciter,
            onSelectReciter = { reciter ->
                onSelectReciter(reciter)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
fun ReciterSelectionDialog(
    selectedReciter: QuranReciter,
    onSelectReciter: (QuranReciter) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reciter_picker_dialog"),
        title = {
            Column {
                Text(
                    text = L10n.pick(lang, "اختر القارئ", "Choose Reciter"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L10n.pick(
                        lang,
                        "يُطبّق القارئ المختار على القراءة والحفظ معاً",
                        "Applies consistently across Quran reading and memorization"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(QuranReciter.entries, key = { it.id }) { reciter ->
                    val isSelected = reciter == selectedReciter
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectReciter(reciter) }
                            .testTag("reciter_option_${reciter.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            GoldPrimary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.8f))
                        } else {
                            null
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, reciter.nameAr, reciter.nameEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = L10n.pick(lang, reciter.nameEn, reciter.nameAr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = L10n.pick(lang, "القارئ الحالي", "Selected Reciter"),
                                    tint = noorColors.goldAccent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("reciter_selected_indicator_${reciter.id}")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إغلاق", "Close"))
            }
        }
    )
}
