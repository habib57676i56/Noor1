package com.example.ui.admin

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.DailyAnalyticsDoc
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Part 1A — Global Dashboard with real-time statistics from live Firestore & Room records
 * and interactive Compose Canvas visualizations.
 */
@Composable
fun AdminDashboardTab(adminViewModel: AdminViewModel) {
    val lang = LocalAppLanguage.current
    val kpis by adminViewModel.dashboardKpis.collectAsStateWithLifecycle()
    val analytics by adminViewModel.dailyAnalytics.collectAsStateWithLifecycle()
    val storage by adminViewModel.storageMetrics.collectAsStateWithLifecycle()
    val verificationReport by adminViewModel.adhkarVerificationReport.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "لوحة القيادة العامة — إحصائيات حية", "Global Executive Dashboard"),
                subtitle = L10n.pick(lang, "جميع الأرقام مستخرجة مباشرة من قاعدة البيانات الحقيقية", "All metrics derived from real Firestore & local database records"),
                trailing = {
                    IconButton(onClick = { adminViewModel.refreshStorageMetrics() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = AdminPalette.AccentCyan)
                    }
                }
            )
        }

        // Row 1: Users & Growth KPIs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpiTile(
                    label = L10n.pick(lang, "إجمالي المستخدمين", "Total Users"),
                    value = "${kpis.totalUsers}",
                    sublabel = L10n.pick(lang, "نشط: ${kpis.activeUsers} · موقوف: ${kpis.suspendedUsers}", "Active: ${kpis.activeUsers} · Susp: ${kpis.suspendedUsers}"),
                    icon = Icons.Rounded.Group,
                    accent = AdminPalette.AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiTile(
                    label = L10n.pick(lang, "النشطون (يومي / شهري)", "DAU / MAU"),
                    value = "${kpis.dailyActiveUsers} / ${kpis.monthlyActiveUsers}",
                    sublabel = L10n.pick(lang, "تسجيلات اليوم: ${kpis.newRegistrationsToday}", "New today: ${kpis.newRegistrationsToday}"),
                    icon = Icons.Rounded.PersonAdd,
                    accent = AdminPalette.AccentIndigo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Worship Activity KPIs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpiTile(
                    label = L10n.pick(lang, "آيات مقروءة", "Quran Verses Read"),
                    value = "${kpis.totalQuranVersesRead}",
                    sublabel = L10n.pick(lang, "تسبيحات: ${kpis.totalTasbihTaps}", "Tasbih taps: ${kpis.totalTasbihTaps}"),
                    icon = Icons.Rounded.MenuBook,
                    accent = AdminPalette.AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiTile(
                    label = L10n.pick(lang, "الأذكار المكتملة", "Adhkar Completed"),
                    value = "${kpis.totalAdhkarCompleted}",
                    sublabel = L10n.pick(lang, "تشغيلات الصوت: ${kpis.totalAudioPlays}", "Audio plays: ${kpis.totalAudioPlays}"),
                    icon = Icons.Rounded.AutoAwesome,
                    accent = AdminPalette.AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Content, Support, Reports & Verified Users
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpiTile(
                    label = L10n.pick(lang, "عناصر المحتوى (CMS)", "CMS Content Items"),
                    value = "${kpis.totalCmsItems}",
                    sublabel = L10n.pick(lang, "منشور: ${kpis.publishedCmsItems} · مسودة: ${kpis.draftCmsItems}", "Live: ${kpis.publishedCmsItems} · Draft: ${kpis.draftCmsItems}"),
                    icon = Icons.Rounded.LibraryBooks,
                    accent = AdminPalette.AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiTile(
                    label = L10n.pick(lang, "تذاكر الدعم والبلاغات", "Tickets & Reports"),
                    value = "${kpis.openSupportTickets} / ${kpis.pendingUserReports}",
                    sublabel = L10n.pick(lang, "حسابات موثقة: ${kpis.verifiedUsers}", "Verified accounts: ${kpis.verifiedUsers}"),
                    icon = Icons.Rounded.SupportAgent,
                    accent = if (kpis.openSupportTickets > 0 || kpis.pendingUserReports > 0) AdminPalette.AccentRose else AdminPalette.AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Chart 1: Daily User & Activity Growth Line Chart
        item {
            AdminPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = L10n.pick(lang, "نمو النشاط اليومي (المستخدمون والعبادات)", "Daily Growth & Worship Activity"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AdminPalette.TextPrimary
                        )
                        Text(
                            text = L10n.pick(lang, "آخر ${analytics.size.coerceAtLeast(1)} أيام من سجلات analytics_daily", "Latest ${analytics.size} real daily records"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                    AdminStatusBadge(
                        label = L10n.pick(lang, "حي", "LIVE"),
                        color = AdminPalette.AccentEmerald
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                DailyGrowthLineChart(analytics = analytics)
            }
        }

        // Interactive Chart 2: Most Accessed Sections Bar Chart
        item {
            AdminPanelCard {
                Text(
                    text = L10n.pick(lang, "الأقسام الأكثر استخداماً", "Most Accessed Sections"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdminPalette.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                SectionHitsBarChart(sectionHits = kpis.sectionHits)
            }
        }

        // System Health, Verification Quality & Storage Usage Card
        item {
            AdminPanelCard(accentBorder = AdminPalette.AccentEmerald.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HealthDonutGauge(score = kpis.systemHealthScore, modifier = Modifier.size(90.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.HealthAndSafety, null, tint = AdminPalette.AccentEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "صحة النظام والأداء", "System Health & Integrity"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AdminPalette.TextPrimary
                            )
                        }
                        Text(
                            text = L10n.pick(
                                lang,
                                "فحص الأذكار الموثقة: ${verificationReport.morningCount} صباح · ${verificationReport.eveningCount} مساء · ${verificationReport.errorCount} أخطاء",
                                "Adhkar Verification: ${verificationReport.morningCount} Morning · ${verificationReport.eveningCount} Evening · ${verificationReport.errorCount} errors"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (verificationReport.isClean) AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                        )
                        val totalKb = storage.totalBytes / 1024L
                        Text(
                            text = L10n.pick(
                                lang,
                                "التخزين المحلي: $totalKb ك.ب (${storage.downloadedAudioCount} ملف صوتي محفوظ)",
                                "Local Storage: $totalKb KB (${storage.downloadedAudioCount} cached audio files)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.TextSecondary
                        )
                        LinearProgressIndicator(
                            progress = { (kpis.systemHealthScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = AdminPalette.AccentEmerald,
                            trackColor = AdminPalette.SurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGrowthLineChart(analytics: List<DailyAnalyticsDoc>) {
    val points = if (analytics.isEmpty()) {
        listOf(0L, 0L)
    } else if (analytics.size == 1) {
        val v = analytics.first().let { it.activeUsers + it.adhkarCompleted + it.quranVersesRead + it.audioPlays }
        listOf(0L, v)
    } else {
        analytics.map { it.activeUsers + it.adhkarCompleted + it.quranVersesRead + it.audioPlays }
    }
    val maxVal = (points.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 14.dp.toPx()
            val padBottom = 18.dp.toPx()
            val usableH = (h - padTop - padBottom).coerceAtLeast(1f)

            // Horizontal subtle grid lines
            for (i in 0..3) {
                val y = padTop + (usableH * i / 3f)
                drawLine(
                    color = AdminPalette.BorderSubtle.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val stepX = if (points.size > 1) w / (points.size - 1) else w
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = padTop + usableH * (1f - (value / maxVal).coerceIn(0f, 1f))
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h - padBottom)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h - padBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(AdminPalette.AccentCyan.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = AdminPalette.AccentCyan,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = padTop + usableH * (1f - (value / maxVal).coerceIn(0f, 1f))
                drawCircle(color = AdminPalette.BgCanvas, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = AdminPalette.AccentCyan, radius = 3.5.dp.toPx(), center = Offset(x, y))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = analytics.firstOrNull()?.dateKey ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = AdminPalette.TextMuted
            )
            Text(
                text = "Peak: ${points.maxOrNull() ?: 0}",
                style = MaterialTheme.typography.labelSmall,
                color = AdminPalette.AccentCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = analytics.lastOrNull()?.dateKey ?: "Today",
                style = MaterialTheme.typography.labelSmall,
                color = AdminPalette.TextMuted
            )
        }
    }
}

@Composable
private fun SectionHitsBarChart(sectionHits: Map<String, Long>) {
    val entries = sectionHits.entries.toList()
    val maxHit = (entries.maxOfOrNull { it.value } ?: 1L).coerceAtLeast(1L)
    val colors = listOf(
        AdminPalette.AccentCyan,
        AdminPalette.AccentEmerald,
        AdminPalette.AccentAmber,
        AdminPalette.AccentIndigo
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEachIndexed { idx, (label, hits) ->
            val accent = colors[idx % colors.size]
            val ratio = (hits.toFloat() / maxHit.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = AdminPalette.TextPrimary)
                    Text("$hits", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    drawRoundRect(
                        color = AdminPalette.SurfaceElevated,
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        size = size
                    )
                    if (ratio > 0f) {
                        drawRoundRect(
                            color = accent,
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            size = Size(size.width * ratio, size.height)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthDonutGauge(score: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = AdminPalette.SurfaceElevated,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = when {
                    score >= 90 -> AdminPalette.AccentEmerald
                    score >= 75 -> AdminPalette.AccentAmber
                    else -> AdminPalette.AccentRose
                },
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f).coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
        }
        Text(
            text = "$score%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AdminPalette.TextPrimary
        )
    }
}
