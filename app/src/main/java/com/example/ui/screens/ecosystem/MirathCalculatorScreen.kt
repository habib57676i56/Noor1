package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.mirath.DeceasedGender
import com.example.domain.mirath.MirathAdjustmentType
import com.example.domain.mirath.MirathCalculatorEngine
import com.example.domain.mirath.MirathInput
import com.example.domain.zakat.ZakatCalculatorEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MirathCalculatorScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedCurrencyIndex by remember { mutableIntStateOf(0) }
    val currency = ZakatCalculatorEngine.currencies[selectedCurrencyIndex]

    var totalEstateText by remember { mutableStateOf("100000") }
    var funeralText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }
    var wasiyyahText by remember { mutableStateOf("") }

    var gender by remember { mutableStateOf(DeceasedGender.MALE) }
    var hasSpouse by remember { mutableStateOf(true) }
    var wivesCount by remember { mutableIntStateOf(1) }
    var hasFather by remember { mutableStateOf(false) }
    var hasMother by remember { mutableStateOf(true) }
    var hasGrandfather by remember { mutableStateOf(false) }
    var hasGrandmother by remember { mutableStateOf(false) }
    var sons by remember { mutableIntStateOf(1) }
    var daughters by remember { mutableIntStateOf(2) }
    var sonSons by remember { mutableIntStateOf(0) }
    var sonDaughters by remember { mutableIntStateOf(0) }
    var fullBros by remember { mutableIntStateOf(0) }
    var fullSis by remember { mutableIntStateOf(0) }
    var patBros by remember { mutableIntStateOf(0) }
    var patSis by remember { mutableIntStateOf(0) }
    var matSibs by remember { mutableIntStateOf(0) }

    val input = MirathInput(
        totalEstate = totalEstateText.toDoubleOrNull() ?: 0.0,
        funeralExpenses = funeralText.toDoubleOrNull() ?: 0.0,
        debts = debtsText.toDoubleOrNull() ?: 0.0,
        wasiyyah = wasiyyahText.toDoubleOrNull() ?: 0.0,
        deceasedGender = gender,
        hasSpouse = hasSpouse,
        wivesCount = wivesCount,
        hasFather = hasFather,
        hasMother = hasMother,
        hasPaternalGrandfather = hasGrandfather,
        hasGrandmother = hasGrandmother,
        sonsCount = sons,
        daughtersCount = daughters,
        sonSonsCount = sonSons,
        sonDaughtersCount = sonDaughters,
        fullBrothersCount = fullBros,
        fullSistersCount = fullSis,
        paternalBrothersCount = patBros,
        paternalSistersCount = patSis,
        maternalSiblingsCount = matSibs
    )
    val result = remember(input) { MirathCalculatorEngine.calculate(input) }

    fun fmtMoney(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "${nf.format(amount)} ${currency.symbol}"
    }

    fun fmtPct(pct: Double): String = String.format(Locale.US, "%.2f%%", pct)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mirath_calculator_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "حاسبة المواريث الشرعية", "Islamic Inheritance (Mirath)", "Meros taqsimoti kalkulyatori"),
            subtitle = L10n.pick(
                lang,
                "توزيع التركة وفق أحكام الفرائض (الفروض، العصبة، الحجب، العول، الرد)",
                "Shariah estate calculator with fixed shares, residuary, blocking, Awl & Radd",
                "Qur'on va fiqh qoidalari asosida meros ulushlarini aniq hisoblash"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Distribution Hero Summary
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Gavel, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = L10n.pick(lang, "صافي التركة للتوزيع", "Net Distributable Estate", "Taqsimlanadigan meros"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = fmtMoney(result.distributableEstate),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = noorColors.goldAccent
                                    )
                                }
                            }

                            val badge = when {
                                result.isUmariyyataynCase -> L10n.pick(lang, "عمرية", "Umariyyatayn", "Umariyatayn")
                                result.adjustmentType == MirathAdjustmentType.AWL -> L10n.pick(lang, "عَوْل", "Awl", "Avl")
                                result.adjustmentType == MirathAdjustmentType.RADD -> L10n.pick(lang, "رَدّ", "Radd", "Radd")
                                else -> L10n.pick(lang, "توزيع عادل", "Exact", "Aniq")
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(EmeraldPrimary.copy(alpha = 0.18f))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                            }
                        }

                        if (result.wasiyyahCappedAtOneThird) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "تنبيه شرعي: تم تحديد الوصية بحد أقصى الثلث (${fmtMoney(result.allowedWasiyyah)}) وفق الحديث النبوي: «الثلث والثلث كثير».",
                                    "Shariah Note: Bequest (Wasiyyah) was automatically capped at 1/3 (${fmtMoney(result.allowedWasiyyah)}).",
                                    "Shariat qoidasi: Vasiyat merosning ko'pi bilan 1/3 qismi (${fmtMoney(result.allowedWasiyyah)}) bilan cheklandi."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }
            }

            // 2. Results Breakdown (Shares)
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "أنصبة الورثة المستحقين", "Heirs & Calculated Shares", "Merosxo'rlar ulushlari")
                )
            }

            if (result.shares.isEmpty()) {
                item {
                    LiquidGlassCard {
                        Text(
                            text = L10n.pick(
                                lang,
                                "اختر الورثة أدناه لعرض التوزيع الشرعي الفوري للتركة.",
                                "Select surviving heirs below to view the instant inheritance breakdown.",
                                "Meros taqsimotini ko'rish uchun quyida vorislarni tanlang."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(result.shares, key = { it.key }) { share ->
                    LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = L10n.pick(lang, share.nameAr, share.nameEn, share.nameUz),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldPrimary.copy(alpha = 0.16f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = share.fractionLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = L10n.pick(lang, share.reasonAr, share.reasonEn, share.reasonUz),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (share.count > 1) {
                                    Text(
                                        text = L10n.pick(
                                            lang,
                                            "نصيب الفرد الواحد: ${fmtMoney(share.perPersonAmount)}",
                                            "Each person: ${fmtMoney(share.perPersonAmount)}",
                                            "Har biri uchun: ${fmtMoney(share.perPersonAmount)}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = fmtMoney(share.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = fmtPct(share.sharePercentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = noorColors.goldAccent
                                )
                            }
                        }
                    }
                }
            }

            // Blocked Heirs Card (if any)
            if (result.blockedHeirs.isNotEmpty()) {
                item {
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Block, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "المحجوبون من الميراث (الحجب)", "Blocked Heirs (Hijab)", "To'silgan vorislar (Hijob)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            result.blockedHeirs.forEach { b ->
                                Text(
                                    text = "• ${L10n.pick(lang, b.nameAr, b.nameEn, b.nameUz)}: ${L10n.pick(lang, b.blockedByAr, b.blockedByEn, b.blockedByUz)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Estate Value, Currency & Deductions
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "بيانات التركة والخصومات", "Estate Value & Deductions", "Meros qiymati va xarajatlar")
                )
            }
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ZakatCalculatorEngine.currencies.indices.toList()) { idx ->
                                val c = ZakatCalculatorEngine.currencies[idx]
                                LiquidGlassChip(
                                    label = "${c.code} (${c.symbol})",
                                    selected = selectedCurrencyIndex == idx,
                                    onClick = { selectedCurrencyIndex = idx }
                                )
                            }
                        }

                        MirathMoneyField(
                            value = totalEstateText,
                            onValueChange = { totalEstateText = it },
                            label = L10n.pick(lang, "إجمالي قيمة التركة", "Total Estate Value", "Umumiy meros qiymati"),
                            suffix = currency.symbol
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MirathMoneyField(
                                value = debtsText,
                                onValueChange = { debtsText = it },
                                label = L10n.pick(lang, "الديون", "Debts", "Qarzlar"),
                                suffix = currency.symbol,
                                modifier = Modifier.weight(1f)
                            )
                            MirathMoneyField(
                                value = funeralText,
                                onValueChange = { funeralText = it },
                                label = L10n.pick(lang, "التجهيز والدفن", "Funeral", "Dafn xarajati"),
                                suffix = currency.symbol,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MirathMoneyField(
                            value = wasiyyahText,
                            onValueChange = { wasiyyahText = it },
                            label = L10n.pick(lang, "الوصية (بحد أقصى الثلث)", "Wasiyyah / Bequest (max 1/3)", "Vasiyat (ko'pi bilan 1/3)"),
                            suffix = currency.symbol
                        )
                    }
                }
            }

            // 4. Deceased Gender & Heirs Selection
            item {
                NoorSectionHeader(
                    title = L10n.pick(lang, "بيانات المتوفى والورثة", "Deceased & Surviving Heirs", "Marhum va vorislar tarkibi")
                )
            }
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "المتوفى: ذكر", "Deceased: Male", "Marhum: Erkak"),
                                selected = gender == DeceasedGender.MALE,
                                onClick = { gender = DeceasedGender.MALE },
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassChip(
                                label = L10n.pick(lang, "المتوفى: أنثى", "Deceased: Female", "Marhuma: Ayol"),
                                selected = gender == DeceasedGender.FEMALE,
                                onClick = { gender = DeceasedGender.FEMALE },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Spouse
                        if (gender == DeceasedGender.MALE) {
                            MirathToggleRow(
                                label = L10n.pick(lang, "الزوجة / الزوجات", "Wife / Wives", "Xotin / Xotinlar"),
                                checked = hasSpouse,
                                onCheckedChange = { hasSpouse = it }
                            )
                            if (hasSpouse) {
                                MirathStepperRow(
                                    label = L10n.pick(lang, "عدد الزوجات (١–٤)", "Number of Wives (1–4)", "Xotinlar soni (1–4)"),
                                    value = wivesCount,
                                    min = 1,
                                    max = 4,
                                    onChange = { wivesCount = it }
                                )
                            }
                        } else {
                            MirathToggleRow(
                                label = L10n.pick(lang, "الزوج", "Husband", "Er"),
                                checked = hasSpouse,
                                onCheckedChange = { hasSpouse = it }
                            )
                        }

                        // Parents & Grandparents
                        MirathToggleRow(L10n.pick(lang, "الأب", "Father", "Ota"), hasFather) { hasFather = it }
                        MirathToggleRow(L10n.pick(lang, "الأم", "Mother", "Ona"), hasMother) { hasMother = it }
                        MirathToggleRow(L10n.pick(lang, "الجد لأب", "Paternal Grandfather", "Ota tomonidan bobo"), hasGrandfather) { hasGrandfather = it }
                        MirathToggleRow(L10n.pick(lang, "الجدة", "Grandmother", "Buvi"), hasGrandmother) { hasGrandmother = it }

                        // Descendants
                        MirathStepperRow(L10n.pick(lang, "الأبناء (الذكور)", "Sons", "O'g'illar"), sons, 0, 20) { sons = it }
                        MirathStepperRow(L10n.pick(lang, "البنات", "Daughters", "Qizlar"), daughters, 0, 20) { daughters = it }
                        MirathStepperRow(L10n.pick(lang, "أبناء الابن", "Son's Sons", "O'g'ilning o'g'illari"), sonSons, 0, 15) { sonSons = it }
                        MirathStepperRow(L10n.pick(lang, "بنات الابن", "Son's Daughters", "O'g'ilning qizlari"), sonDaughters, 0, 15) { sonDaughters = it }

                        // Siblings
                        MirathStepperRow(L10n.pick(lang, "الإخوة الأشقاء", "Full Brothers", "Tug'ishgan aka-ukalar"), fullBros, 0, 15) { fullBros = it }
                        MirathStepperRow(L10n.pick(lang, "الأخوات الشقيقات", "Full Sisters", "Tug'ishgan opa-singillar"), fullSis, 0, 15) { fullSis = it }
                        MirathStepperRow(L10n.pick(lang, "الإخوة لأب", "Paternal Brothers", "Ota bir aka-ukalar"), patBros, 0, 15) { patBros = it }
                        MirathStepperRow(L10n.pick(lang, "الأخوات لأب", "Paternal Sisters", "Ota bir opa-singillar"), patSis, 0, 15) { patSis = it }
                        MirathStepperRow(L10n.pick(lang, "الإخوة لأم (ذكور وإناث)", "Maternal Siblings", "Ona bir tug'ishganlar"), matSibs, 0, 15) { matSibs = it }

                        OutlinedButton(
                            onClick = {
                                totalEstateText = "100000"
                                funeralText = ""
                                debtsText = ""
                                wasiyyahText = ""
                                hasSpouse = false
                                hasFather = false
                                hasMother = false
                                hasGrandfather = false
                                hasGrandmother = false
                                sons = 0
                                daughters = 0
                                sonSons = 0
                                sonDaughters = 0
                                fullBros = 0
                                fullSis = 0
                                patBros = 0
                                patSis = 0
                                matSibs = 0
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("mirath_reset_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إعادة تعيين الورثة", "Reset All Heirs", "Barchasini tozalash"))
                        }
                    }
                }
            }

            // 5. Scholarly Disclaimer
            item {
                LiquidGlassCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "تنبيه مهم: هذه الحاسبة استرشادية وفق أصول علم الفرائض والقرآن الكريم (سورة النساء ١١–١٢ و١٧٦). في القضايا القانونية أو الحالات المركبة يُنصح بمراجعة عالم شرعي أو المحكمة المختصة.",
                                "Advisory Notice: This calculator provides educational guidance based on Surah An-Nisa (4:11–12, 176) and Jumhur Fiqh. For legal execution or complex estates, consult a qualified scholar or Shariah court.",
                                "Eslatma: Ushbu hisoblagich Qur'on va fiqh asosida ma'lumot berish uchun mo'ljallangan. Rasmiy taqsimot uchun ahli ilm yoki mutaxassisga murojaat qiling."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MirathMoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val clean = raw.filter { it.isDigit() || it == '.' }
            if (clean.count { it == '.' } <= 1) onValueChange(clean)
        },
        label = { Text(label, maxLines = 1) },
        suffix = { Text(suffix, fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun MirathToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MirathStepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    val noorColors = LocalNoorColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { if (value > min) onChange(value - 1) },
                enabled = value > min,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(noorColors.glassSurfaceStrong)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
            }
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value > 0) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { if (value < max) onChange(value + 1) },
                enabled = value < max,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(noorColors.glassSurfaceStrong)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase")
            }
        }
    }
}
