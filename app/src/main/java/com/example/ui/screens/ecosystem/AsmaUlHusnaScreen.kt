package com.example.ui.screens.ecosystem

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.catalog.AsmaUlHusnaCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.OrnamentalSurahBadge
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalNoorColors

@Composable
fun AsmaUlHusnaScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) AsmaUlHusnaCatalog.names
        else AsmaUlHusnaCatalog.names.filter { entry ->
            entry.arabic.contains(q) ||
                entry.transliteration.lowercase().contains(q) ||
                entry.meaningAr.contains(q) ||
                entry.meaningEn.lowercase().contains(q) ||
                entry.meaningUz.lowercase().contains(q) ||
                entry.number.toString() == q
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("asma_ul_husna_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "أسماء الله الحسنى", "99 Names of Allah", "Allohning 99 go'zal ismlari"),
            subtitle = L10n.pick(
                lang,
                "إنَّ لِلَّهِ تِسْعَةً وَتِسْعِينَ اسْمًا مَنْ أَحْصَاهَا دَخَلَ الْجَنَّةَ",
                "99 Beautiful Names with meanings in Arabic, English & Uzbek",
                "Allohning 99 ismi va ularning ma'nolari"
            ),
            onBack = onBack
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = {
                Text(L10n.pick(lang, "ابحث في أسماء الله الحسنى...", "Search Names of Allah...", "Allohning ismlarini qidiring..."))
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("asma_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.number }) { entry ->
                LiquidGlassCard(
                    goldBorder = entry.number == 1 || entry.number == 93 || entry.number == 99,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrnamentalSurahBadge(number = entry.number, size = 46.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.arabic,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = entry.transliteration,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = L10n.pick(lang, entry.meaningAr, entry.meaningEn, entry.meaningUz),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                val text = "${entry.arabic} (${entry.transliteration})\n${L10n.pick(lang, entry.meaningAr, entry.meaningEn, entry.meaningUz)}\n— NOOR"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, entry.arabic))
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = L10n.pick(lang, "مشاركة", "Share", "Ulashish"),
                                tint = noorColors.goldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
