package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.AdhkarCatalog
import com.example.domain.model.AdhkarCategory
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

@Composable
fun AdhkarScreen(
    viewModel: NoorViewModel,
    onOpenCategory: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val favoriteIds by viewModel.favoriteDhikrIds.collectAsStateWithLifecycle()
    val progressMap by viewModel.adhkarProgressMap.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val allItems = remember { AdhkarCatalog.allAdhkar }
    val matchingItems = remember(query, showFavoritesOnly, favoriteIds) {
        val base = if (showFavoritesOnly) allItems.filter { favoriteIds.contains(it.id) } else allItems
        val q = query.trim()
        if (q.isEmpty()) base else base.filter {
            it.textAr.contains(q, ignoreCase = true) ||
                it.meaningAr.contains(q, ignoreCase = true) ||
                it.meaningEn.contains(q, ignoreCase = true) ||
                it.transliteration.contains(q, ignoreCase = true) ||
                it.sourceAr.contains(q, ignoreCase = true) ||
                it.category.nameAr.contains(q, ignoreCase = true) ||
                it.category.nameEn.contains(q, ignoreCase = true)
        }
    }

    val totalCompletedToday = progressMap.values.count { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adhkar_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مكتبة الأذكار والأدعية", "Authentic Adhkar Library"),
            subtitle = L10n.pick(
                lang,
                "٢٤ قسماً موثقاً · ${allItems.size} ذكراً ودعاءً · أتممت اليوم $totalCompletedToday",
                "24 Verified Categories · ${allItems.size} Supplications · Completed today: $totalCompletedToday"
            ),
            trailing = {
                IconButton(onClick = { viewModel.resetTodayAdhkar() }) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Reset today",
                        tint = noorColors.goldAccent
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = {
                        Text(
                            L10n.pick(lang, "ابحث في جميع الأذكار والأدعية الموثقة...", "Search across all 24 verified categories..."),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الأقسام (٢٤)", "Categories (24)"),
                        selected = !showFavoritesOnly,
                        onClick = { showFavoritesOnly = false }
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "المفضلة (${favoriteIds.size})", "Favorites (${favoriteIds.size})"),
                        selected = showFavoritesOnly,
                        icon = Icons.Rounded.Favorite,
                        onClick = { showFavoritesOnly = true }
                    )
                }
            }

            if (query.isNotBlank() || showFavoritesOnly) {
                if (matchingItems.isEmpty()) {
                    item {
                        LiquidGlassCard(contentPadding = PaddingValues(24.dp)) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    if (showFavoritesOnly) "لا توجد أذكار في المفضلة بعد." else "لم يتم العثور على نتائج مطابقة.",
                                    if (showFavoritesOnly) "No favorite Adhkar saved yet." else "No matching supplications found."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(matchingItems, key = { it.id }) { dhikr ->
                        val prog = progressMap[dhikr.id]?.currentCount ?: 0
                        val isFav = favoriteIds.contains(dhikr.id)
                        LiquidGlassCard(
                            onClick = { onOpenCategory(dhikr.category.id) },
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = L10n.pick(lang, dhikr.category.nameAr, dhikr.category.nameEn),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = noorColors.goldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.toggleFavoriteDhikr(dhikr.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = dhikr.textAr,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L10n.pick(lang, "${dhikr.sourceAr} · ${dhikr.gradingAr} · ($prog/${dhikr.targetCount})", "${dhikr.sourceEn} · ${dhikr.gradingEn} · ($prog/${dhikr.targetCount})"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(AdhkarCategory.entries, key = { it.id }) { category ->
                    val catItems = AdhkarCatalog.byCategory(category)
                    val completedCount = catItems.count { progressMap[it.id]?.isCompleted == true }
                    val frac = if (catItems.isEmpty()) 0f else completedCount.toFloat() / catItems.size.toFloat()

                    LiquidGlassCard(
                        goldBorder = completedCount == catItems.size && catItems.isNotEmpty(),
                        onClick = { onOpenCategory(category.id) },
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.testTag("adhkar_cat_${category.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category),
                                    contentDescription = null,
                                    tint = noorColors.goldAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, category.nameAr, category.nameEn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "${catItems.size} أذكار موثقة · المكتمل اليوم $completedCount/${catItems.size}",
                                        "${catItems.size} authentic items · Done today $completedCount/${catItems.size}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = noorColors.goldAccent,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: AdhkarCategory): ImageVector = when (category) {
    AdhkarCategory.MORNING -> Icons.Rounded.WbSunny
    AdhkarCategory.EVENING -> Icons.Rounded.NightsStay
    AdhkarCategory.AFTER_PRAYER -> Icons.Rounded.Mosque
    AdhkarCategory.SLEEP -> Icons.Rounded.Bedtime
    AdhkarCategory.WAKING -> Icons.Rounded.WbTwilight
    AdhkarCategory.WUDU -> Icons.Rounded.WaterDrop
    AdhkarCategory.MOSQUE -> Icons.Rounded.Mosque
    AdhkarCategory.HOME -> Icons.Rounded.Home
    AdhkarCategory.FOOD_DRINK -> Icons.Rounded.Restaurant
    AdhkarCategory.TRAVEL -> Icons.Rounded.FlightTakeoff
    AdhkarCategory.RIDING -> Icons.Rounded.DirectionsCar
    AdhkarCategory.ANXIETY -> Icons.Rounded.Healing
    AdhkarCategory.ISTIGHFAR -> Icons.Rounded.SelfImprovement
    AdhkarCategory.TASBEEH_DHIKR -> Icons.Rounded.TouchApp
    AdhkarCategory.QURANIC_DUA -> Icons.Rounded.MenuBook
    AdhkarCategory.PROPHETIC_DUA -> Icons.Rounded.AutoAwesome
    AdhkarCategory.FRIDAY -> Icons.Rounded.LightMode
    AdhkarCategory.HAJJ_UMRAH -> Icons.Rounded.Public
    AdhkarCategory.RAIN_WEATHER -> Icons.Rounded.Air
    AdhkarCategory.VISITING_SICK -> Icons.Rounded.LocalHospital
    AdhkarCategory.CONDOLENCE -> Icons.Rounded.VolunteerActivism
    AdhkarCategory.BATHROOM -> Icons.Rounded.Wc
    AdhkarCategory.ADHAN -> Icons.Rounded.Campaign
    AdhkarCategory.GENERAL_SUPPLICATIONS -> Icons.Rounded.Spa
}
