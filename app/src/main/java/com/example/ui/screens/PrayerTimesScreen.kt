package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppLanguage
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.IqamahDelays
import com.example.domain.model.PrayerName
import com.example.domain.prayer.CityCatalog
import com.example.domain.prayer.PrayerTimeGate
import com.example.location.LocationHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.prayerIcon
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalIs24HourFormat
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

@Composable
fun PrayerTimesScreen(
    viewModel: NoorViewModel,
    onOpenQibla: () -> Unit,
    onOpenMosques: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val is24Hour = LocalIs24HourFormat.current
    val isAr = lang == AppLanguage.ARABIC
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboard by viewModel.prayerDashboard.collectAsStateWithLifecycle()
    val todayActivity by viewModel.todayActivity.collectAsStateWithLifecycle()
    val isDetecting by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val noorColors = LocalNoorColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Daily, 1=Weekly, 2=Monthly
    var showCityPicker by remember { mutableStateOf(false) }
    var showCalcDialog by remember { mutableStateOf(false) }
    var showOffsetsDialog by remember { mutableStateOf(false) }
    var showIqamahDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectLocationAutomatically()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer_times_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val madhhabBadge = if (prefs.asrMadhhab == AsrMadhhab.HANAFI) {
                L10n.pick(lang, "العصر: حنفي", "Asr: Hanafi", "Asr: Hanafiy", "Аср: Ханафи")
            } else {
                L10n.pick(lang, "العصر: شافعي", "Asr: Shafi'i", "Asr: Shofe'iy", "Аср: Шафии")
            }
            val syncSuffix = if (dashboard.isOfficialPrayerSyncActive) {
                L10n.pick(lang, " · متزامن رسمياً", " · Official Sync", " · Rasmiy", " · Офиц.")
            } else {
                ""
            }
            NoorScreenHeader(
                title = L10n.pick(lang, "مواقيت الصلاة", "Prayer Times"),
                subtitle = if (isAr) "${prefs.cityNameAr} · ${prefs.calculationMethod.nameAr} · $madhhabBadge$syncSuffix"
                else "${prefs.cityNameEn} · ${prefs.calculationMethod.nameEn} · $madhhabBadge$syncSuffix",
                trailing = {
                    IconButton(
                        onClick = onOpenQibla,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(noorColors.glassSurface)
                            .border(1.dp, noorColors.glassBorderBottom, CircleShape)
                            .testTag("prayer_qibla_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = L10n.pick(lang, "القبلة", "Qibla"),
                            tint = noorColors.goldAccent
                        )
                    }
                }
            )
        }

        // Location & Calculation Controls Bar
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detect_gps_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) {
                                viewModel.detectLocationAutomatically()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            if (isDetecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "موقعي تلقائياً", "Auto GPS"),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choose_city_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = { showCityPicker = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.LocationCity, null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "اختيار المدينة", "Pick City"),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الحساب والمذهب", "Method"),
                        selected = false,
                        icon = Icons.Rounded.Tune,
                        onClick = { showCalcDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الدقائق", "Offsets"),
                        selected = false,
                        onClick = { showOffsetsDialog = true }
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الإقامة", "Iqamah"),
                        selected = prefs.iqamahDelays.enabled,
                        icon = Icons.Rounded.Schedule,
                        onClick = { showIqamahDialog = true }
                    )
                }

                // 1-tap Asr Juristic Madhhab Switcher (Shafi'i/Standard vs Hanafi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(
                            lang,
                            "العصر: الجمهور (الشافعي)",
                            "Asr: Shafi'i (Standard)",
                            "Asr: Shofe'iy (Standart)",
                            "Аср: Шафиитский"
                        ),
                        selected = prefs.asrMadhhab == AsrMadhhab.SHAFI,
                        onClick = { viewModel.setAsrMadhhab(AsrMadhhab.SHAFI) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("asr_madhhab_shafi")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(
                            lang,
                            "العصر: الحنفي",
                            "Asr: Hanafi",
                            "Asr: Hanafiy",
                            "Аср: Ханафитский"
                        ),
                        selected = prefs.asrMadhhab == AsrMadhhab.HANAFI,
                        onClick = { viewModel.setAsrMadhhab(AsrMadhhab.HANAFI) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("asr_madhhab_hanafi")
                    )
                }

                LiquidGlassCard(
                    onClick = onOpenMosques,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mosque, null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (prefs.iqamahDelays.enabled && prefs.iqamahDelays.mosqueName.isNotBlank()) {
                                    L10n.pick(lang, "المساجد القريبة · ${prefs.iqamahDelays.mosqueName}", "Nearby Mosques · ${prefs.iqamahDelays.mosqueName}")
                                } else {
                                    L10n.pick(lang, "استكشف المساجد القريبة من موقعك", "Find Nearby Mosques & Directions")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = L10n.pick(lang, "فتح", "Open"),
                            style = MaterialTheme.typography.labelMedium,
                            color = noorColors.goldAccent
                        )
                    }
                }

                // Period selector: Daily / Weekly / Monthly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "يومي", "Daily"),
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f).testTag("prayer_tab_daily")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "أسبوعي", "Weekly"),
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f).testTag("prayer_tab_weekly")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "شهري", "Monthly"),
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f).testTag("prayer_tab_monthly")
                    )
                }
            }
        }

        // Content by tab
        if (selectedTab == 0) {
            val nowMillis = dashboard.gregorianCal.timeInMillis
            items(dashboard.todaySchedule.allOrdered()) { (prayer, millis) ->
                val isNext = prayer == dashboard.nextPrayerInfo.nextPrayer
                val notifyEnabled = prefs.isPrayerNotificationEnabled(prayer)
                val hasEntered = PrayerTimeGate.hasPrayerEntered(dashboard.todaySchedule, prayer, nowMillis)
                val isPrayed = hasEntered && when (prayer) {
                    PrayerName.FAJR -> todayActivity?.fajrPrayed == true
                    PrayerName.DHUHR -> todayActivity?.dhuhrPrayed == true
                    PrayerName.ASR -> todayActivity?.asrPrayed == true
                    PrayerName.MAGHRIB -> todayActivity?.maghribPrayed == true
                    PrayerName.ISHA -> todayActivity?.ishaPrayed == true
                    PrayerName.SUNRISE -> false
                }
                LiquidGlassCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("prayer_row_${prayer.key}"),
                    strong = isNext || isPrayed,
                    goldBorder = isPrayed,
                    onClick = if (hasEntered) {
                        { viewModel.togglePrayerLogged(prayer, nowMillis) }
                    } else {
                        null
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isPrayed -> Color(0xFF4ADE80).copy(alpha = 0.22f)
                                            isNext -> MaterialTheme.colorScheme.primary
                                            hasEntered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPrayed) Icons.Rounded.CheckCircle else prayerIcon(prayer),
                                    contentDescription = null,
                                    tint = when {
                                        isPrayed -> Color(0xFF4ADE80)
                                        isNext -> MaterialTheme.colorScheme.onPrimary
                                        hasEntered -> noorColors.goldAccent
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = L10n.prayerName(prayer, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isPrayed -> Color(0xFF4ADE80)
                                        isNext -> noorColors.goldAccent
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                when {
                                    isPrayed -> {
                                        Text(
                                            text = L10n.pick(lang, "تمت الصلاة ✓", "Prayed ✓", "O'qildi ✓"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                    hasEntered -> {
                                        Text(
                                            text = L10n.pick(lang, "دخل وقتها — اضغط لتسجيل الصلاة", "Entered — tap to mark prayed", "Vaqti kirdi — belgilash uchun bosing"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = noorColors.goldAccent
                                        )
                                    }
                                    isNext -> {
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "القادمة بعد ${L10n.formatCountdown(dashboard.nextPrayerInfo.remainingMillis, lang)}",
                                                "Next in ${L10n.formatCountdown(dashboard.nextPrayerInfo.remainingMillis, lang)}",
                                                "Keyingi namoz: ${L10n.formatCountdown(dashboard.nextPrayerInfo.remainingMillis, lang)}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    prayer.isFard -> {
                                        Text(
                                            text = L10n.pick(lang, "لم يدخل وقتها بعد", "Not entered yet", "Vaqti hali kirmadi"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = L10n.formatPrayerTime(
                                        millis = millis,
                                        timeZoneId = prefs.timezoneId,
                                        lang = lang,
                                        is24Hour = is24Hour,
                                        includeSeconds = true
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val iqDelay = if (prefs.iqamahDelays.enabled && prayer.isFard) prefs.iqamahDelays.delayFor(prayer) else 0
                                if (iqDelay > 0) {
                                    val iqTime = L10n.formatPrayerTime(
                                        millis = millis + iqDelay * 60_000L,
                                        timeZoneId = prefs.timezoneId,
                                        lang = lang,
                                        is24Hour = is24Hour,
                                        includeSeconds = false
                                    )
                                    Text(
                                        text = L10n.pick(lang, "إقامة $iqTime", "Iqamah $iqTime", "Iqomat $iqTime"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = noorColors.goldAccent
                                    )
                                }
                            }
                            if (prayer.isFard) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (hasEntered) viewModel.togglePrayerLogged(prayer, nowMillis)
                                    },
                                    enabled = hasEntered,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("prayer_check_${prayer.key}")
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isPrayed -> Icons.Rounded.CheckCircle
                                            hasEntered -> Icons.Rounded.RadioButtonUnchecked
                                            else -> Icons.Rounded.LockClock
                                        },
                                        contentDescription = L10n.pick(lang, "تحديد الصلاة كـ مُصلّاة", "Mark prayer as prayed", "Namozni o'qilgan deb belgilash"),
                                        tint = when {
                                            isPrayed -> Color(0xFF4ADE80)
                                            hasEntered -> noorColors.goldAccent
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setPrayerNotification(prayer, !notifyEnabled) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (notifyEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                        contentDescription = L10n.pick(lang, "تبديل تنبيه الصلاة", "Toggle prayer alert", "Namoz bildirishnomasini almashtirish"),
                                        tint = if (notifyEnabled) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val list = if (selectedTab == 1) dashboard.weeklySchedules else dashboard.monthlySchedules
            items(list) { daySchedule ->
                ScheduleCompactCard(
                    schedule = daySchedule,
                    timeZoneId = prefs.timezoneId,
                    lang = lang
                )
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            isAr = isAr,
            currentCityId = prefs.cityId,
            onSelect = {
                viewModel.selectCity(it)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }

    if (showCalcDialog) {
        CalculationMethodDialog(
            isAr = isAr,
            currentMethod = prefs.calculationMethod,
            currentMadhhab = prefs.asrMadhhab,
            onSelectMethod = { viewModel.setCalculationMethod(it) },
            onSelectMadhhab = { viewModel.setAsrMadhhab(it) },
            onDismiss = { showCalcDialog = false }
        )
    }

    if (showOffsetsDialog) {
        PrayerOffsetsDialog(
            lang = lang,
            offsets = prefs.offsets,
            onChangeOffset = { prayer, value -> viewModel.setPrayerOffset(prayer, value) },
            onDismiss = { showOffsetsDialog = false }
        )
    }

    if (showIqamahDialog) {
        IqamahScheduleDialog(
            lang = lang,
            current = prefs.iqamahDelays,
            onSave = { updated ->
                viewModel.setIqamahDelays(updated)
                showIqamahDialog = false
            },
            onDismiss = { showIqamahDialog = false }
        )
    }
}

@Composable
private fun ScheduleCompactCard(
    schedule: DailyPrayerSchedule,
    timeZoneId: String,
    lang: AppLanguage
) {
    LiquidGlassCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = schedule.dateKey,
            style = MaterialTheme.typography.labelLarge,
            color = LocalNoorColors.current.goldAccent,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        val is24Hour = LocalIs24HourFormat.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA).forEach { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = L10n.prayerName(p, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = L10n.formatPrayerTime(
                            millis = schedule.timeFor(p),
                            timeZoneId = timeZoneId,
                            lang = lang,
                            is24Hour = is24Hour,
                            includeSeconds = false,
                            compact = true
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CityPickerDialog(
    isAr: Boolean,
    currentCityId: String,
    onSelect: (com.example.domain.model.CityLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) CityCatalog.cities
        else CityCatalog.cities.filter {
            it.nameAr.contains(query, ignoreCase = true) ||
                it.nameEn.contains(query, ignoreCase = true) ||
                it.countryAr.contains(query, ignoreCase = true) ||
                it.countryEn.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAr) "اختر مدينتك" else "Select City") },
        text = {
            Column(modifier = Modifier.height(380.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (isAr) "ابحث عن مدينة..." else "Search city...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered) { city ->
                        val selected = city.id == currentCityId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { onSelect(city) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) city.nameAr else city.nameEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAr) city.countryAr else city.countryEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إغلاق" else "Close")
            }
        }
    )
}

@Composable
private fun CalculationMethodDialog(
    isAr: Boolean,
    currentMethod: CalculationMethod,
    currentMadhhab: AsrMadhhab,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectMadhhab: (AsrMadhhab) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAr) "طريقة الحساب والمذهب" else "Calculation Method & Madhhab") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = if (isAr) "المذهب الفقهي لصلاة العصر" else "Asr Juristic Madhhab",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalNoorColors.current.goldAccent
                    )
                }
                items(AsrMadhhab.entries) { m ->
                    val selected = m == currentMadhhab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onSelectMadhhab(m) }
                            .padding(12.dp)
                    ) {
                        Text(text = if (isAr) m.nameAr else m.nameEn, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) "طريقة حساب المواقيت" else "Calculation Convention",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalNoorColors.current.goldAccent
                    )
                }
                items(CalculationMethod.entries) { method ->
                    val selected = method == currentMethod
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onSelectMethod(method) }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isAr) method.nameAr else method.nameEn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "تم" else "Done")
            }
        }
    )
}

@Composable
private fun PrayerOffsetsDialog(
    lang: AppLanguage,
    offsets: com.example.domain.model.PrayerMinuteOffsets,
    onChangeOffset: (PrayerName, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val rows = listOf(
        PrayerName.FAJR to offsets.fajr,
        PrayerName.SUNRISE to offsets.sunrise,
        PrayerName.DHUHR to offsets.dhuhr,
        PrayerName.ASR to offsets.asr,
        PrayerName.MAGHRIB to offsets.maghrib,
        PrayerName.ISHA to offsets.isha
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.pick(lang, "تعديل دقائق المواقيت", "Manual Minute Offsets")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (prayer, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.prayerName(prayer, lang), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onChangeOffset(prayer, value - 1) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "${if (value > 0) "+" else ""}$value",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onChangeOffset(prayer, value + 1) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "حفظ", "Done"))
            }
        }
    )
}

@Composable
private fun IqamahScheduleDialog(
    lang: AppLanguage,
    current: IqamahDelays,
    onSave: (IqamahDelays) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.pick(lang, "جدول الإقامة في مسجدك", "Mosque Iqamah Schedule")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(L10n.pick(lang, "عرض أوقات الإقامة", "Show Iqamah Times"), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = draft.enabled,
                        onCheckedChange = { draft = draft.copy(enabled = it) }
                    )
                }
                OutlinedTextField(
                    value = draft.mosqueName,
                    onValueChange = { draft = draft.copy(mosqueName = it) },
                    label = { Text(L10n.pick(lang, "اسم المسجد (اختياري)", "Mosque Name (Optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val rows = listOf(
                    PrayerName.FAJR to draft.fajr,
                    PrayerName.DHUHR to draft.dhuhr,
                    PrayerName.ASR to draft.asr,
                    PrayerName.MAGHRIB to draft.maghrib,
                    PrayerName.ISHA to draft.isha
                )
                rows.forEach { (prayer, mins) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.prayerName(prayer, lang), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val next = (mins - 1).coerceAtLeast(0)
                                draft = when (prayer) {
                                    PrayerName.FAJR -> draft.copy(fajr = next)
                                    PrayerName.DHUHR -> draft.copy(dhuhr = next)
                                    PrayerName.ASR -> draft.copy(asr = next)
                                    PrayerName.MAGHRIB -> draft.copy(maghrib = next)
                                    PrayerName.ISHA -> draft.copy(isha = next)
                                    PrayerName.SUNRISE -> draft
                                }
                            }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                            }
                            Text("+${mins}m", fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val next = (mins + 1).coerceAtMost(60)
                                draft = when (prayer) {
                                    PrayerName.FAJR -> draft.copy(fajr = next)
                                    PrayerName.DHUHR -> draft.copy(dhuhr = next)
                                    PrayerName.ASR -> draft.copy(asr = next)
                                    PrayerName.MAGHRIB -> draft.copy(maghrib = next)
                                    PrayerName.ISHA -> draft.copy(isha = next)
                                    PrayerName.SUNRISE -> draft
                                }
                            }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(L10n.pick(lang, "حفظ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel"))
            }
        }
    )
}
