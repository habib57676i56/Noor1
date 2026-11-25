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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LiveMetalPrices
import com.example.data.remote.NoorLiveOnlineService
import com.example.domain.zakat.NisabStandard
import com.example.domain.zakat.ZakatCalculationInput
import com.example.domain.zakat.ZakatCalculatorEngine
import kotlinx.coroutines.launch
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NoorZakatScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Wealth Zakat, 1 = Zakat al-Fitr, 2 = 8 Recipients
    var selectedCurrency by remember { mutableStateOf(ZakatCalculatorEngine.currencies.first()) }
    var nisabStandard by remember { mutableStateOf(NisabStandard.GOLD_85G) }

    val scope = rememberCoroutineScope()
    var goldPriceText by remember { mutableStateOf(formatRawNumber(selectedCurrency.defaultGold24kPerGram)) }
    var silverPriceText by remember { mutableStateOf(formatRawNumber(selectedCurrency.defaultSilverPerGram)) }
    var fetchingLiveMetals by remember { mutableStateOf(false) }
    var liveMetalsStatus by remember { mutableStateOf<String?>(null) }
    var latestLiveMetals by remember { mutableStateOf<LiveMetalPrices?>(null) }

    var cashText by remember { mutableStateOf("") }
    var gold24kText by remember { mutableStateOf("") }
    var gold21kText by remember { mutableStateOf("") }
    var gold18kText by remember { mutableStateOf("") }
    var silverGramsText by remember { mutableStateOf("") }
    var investmentsText by remember { mutableStateOf("") }
    var tradeGoodsText by remember { mutableStateOf("") }
    var receivablesText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }

    var fitrMembers by remember { mutableIntStateOf(4) }
    var fitrPerPersonText by remember { mutableStateOf(formatRawNumber(selectedCurrency.defaultFitrPerPerson)) }

    val calcInput = ZakatCalculationInput(
        cashAndBank = parseNumber(cashText),
        gold24kGrams = parseNumber(gold24kText),
        gold21kGrams = parseNumber(gold21kText),
        gold18kGrams = parseNumber(gold18kText),
        silverGrams = parseNumber(silverGramsText),
        investmentsAndStocks = parseNumber(investmentsText),
        tradeInventory = parseNumber(tradeGoodsText),
        receivables = parseNumber(receivablesText),
        immediateDebts = parseNumber(debtsText),
        gold24kPricePerGram = parseNumber(goldPriceText).takeIf { it > 0.0 } ?: selectedCurrency.defaultGold24kPerGram,
        silverPricePerGram = parseNumber(silverPriceText).takeIf { it > 0.0 } ?: selectedCurrency.defaultSilverPerGram,
        nisabStandard = nisabStandard
    )
    val result = remember(calcInput) { ZakatCalculatorEngine.calculateWealthZakat(calcInput) }
    val fitrResult = remember(fitrMembers, fitrPerPersonText, selectedCurrency) {
        ZakatCalculatorEngine.calculateZakatAlFitr(
            familyMembers = fitrMembers,
            amountPerPerson = parseNumber(fitrPerPersonText).takeIf { it > 0.0 } ?: selectedCurrency.defaultFitrPerPerson
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("zakat_calculator_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "حاسبة الزكاة الشاملة", "Zakat Calculator", "Zakot hisoblagichi"),
            subtitle = L10n.pick(
                lang,
                "حساب زكاة المال والذهب والفضة والتجارة وزكاة الفطر بدقة شرعية",
                "Accurate Wealth, Gold, Silver, Trade & Zakat al-Fitr Calculator",
                "Mol, oltin, kumush, savdo va Fitr zakotini aniq hisoblash"
            ),
            onBack = onBack
        )

        // Mode Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "زكاة المال", "Wealth Zakat", "Mol zakoti"),
                selected = selectedTab == 0,
                icon = Icons.Rounded.Calculate,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f).testTag("zakat_tab_wealth")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "زكاة الفطر", "Zakat al-Fitr", "Fitr sadaqasi"),
                selected = selectedTab == 1,
                icon = Icons.Rounded.Groups,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f).testTag("zakat_tab_fitr")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "المصارف", "Recipients", "Haqdorlar"),
                selected = selectedTab == 2,
                icon = Icons.Rounded.VolunteerActivism,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f).testTag("zakat_tab_recipients")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Currency Selector Strip (visible on Wealth & Fitr tabs)
            if (selectedTab != 2) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ZakatCalculatorEngine.currencies) { currency ->
                            LiquidGlassChip(
                                label = "${currency.code} (${currency.symbol})",
                                selected = selectedCurrency.code == currency.code,
                                onClick = {
                                    selectedCurrency = currency
                                    goldPriceText = formatRawNumber(currency.defaultGold24kPerGram)
                                    silverPriceText = formatRawNumber(currency.defaultSilverPerGram)
                                    fitrPerPersonText = formatRawNumber(currency.defaultFitrPerPerson)
                                }
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Live Liquid Glass Result Hero Card
                item {
                    LiquidGlassCard(
                        strong = true,
                        goldBorder = result.isZakatDue,
                        modifier = Modifier.testTag("zakat_result_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "مقدار الزكاة الواجبة (٢.٥٪)", "Zakat Payable (2.5%)", "To'lanadigan zakot (2.5%)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (result.isZakatDue) EmeraldBright.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                    .border(
                                        1.dp,
                                        if (result.isZakatDue) EmeraldSoft else noorColors.glassBorderBottom,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (result.isZakatDue) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (result.isZakatDue) EmeraldSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (result.isZakatDue) {
                                        L10n.pick(lang, "بلغ النصاب", "Above Nisab", "Nisobga yetdi")
                                    } else {
                                        L10n.pick(lang, "دون النصاب", "Below Nisab", "Nisobdan past")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${formatMoney(result.zakatDueAmount)} ${selectedCurrency.symbol}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (result.isZakatDue) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("zakat_due_amount_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ZakatMetricPill(
                                label = L10n.pick(lang, "صافي المال", "Net Wealth", "Sof boylik"),
                                value = "${formatMoney(result.netZakatableWealth)} ${selectedCurrency.symbol}",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ZakatMetricPill(
                                label = L10n.pick(lang, "قيمة النصاب", "Nisab Threshold", "Nisob qiymati"),
                                value = "${formatMoney(result.nisabThreshold)} ${selectedCurrency.symbol}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Nisab Standard & Metal Prices
                item {
                    LiquidGlassCard {
                        NoorSectionHeader(
                            title = L10n.pick(lang, "معيار النصاب وأسعار الذهب والفضة", "Nisab Standard & Metal Prices", "Nisob va oltin-kumush narxlari")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الذهب (٨٥ غرام)", "Gold (85g)", "Oltin (85g)"),
                                selected = nisabStandard == NisabStandard.GOLD_85G,
                                onClick = { nisabStandard = NisabStandard.GOLD_85G },
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الفضة (٥٩٥ غرام)", "Silver (595g)", "Kumush (595g)"),
                                selected = nisabStandard == NisabStandard.SILVER_595G,
                                onClick = { nisabStandard = NisabStandard.SILVER_595G },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZakatNumberField(
                                value = goldPriceText,
                                onValueChange = { goldPriceText = it },
                                label = L10n.pick(lang, "سعر غرام الذهب ٢٤", "24K Gold / gram", "1g Oltin (24K)"),
                                suffix = selectedCurrency.symbol,
                                modifier = Modifier.weight(1f)
                            )
                            ZakatNumberField(
                                value = silverPriceText,
                                onValueChange = { silverPriceText = it },
                                label = L10n.pick(lang, "سعر غرام الفضة", "Silver / gram", "1g Kumush"),
                                suffix = selectedCurrency.symbol,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    fetchingLiveMetals = true
                                    liveMetalsStatus = null
                                    NoorLiveOnlineService.fetchLivePreciousMetalsPrices(selectedCurrency.code)
                                        .onSuccess { live ->
                                            latestLiveMetals = live
                                            goldPriceText = formatRawNumber(live.gold24kPerGram)
                                            silverPriceText = formatRawNumber(live.silverPerGram)
                                            liveMetalsStatus = L10n.pick(
                                                lang,
                                                "تم تحديث الأسعار العالمية الحية: عيار ٢٤ = ${formatMoney(live.gold24kPerGram)} · عيار ٢١ = ${formatMoney(live.gold21kPerGram)} · عيار ١٨ = ${formatMoney(live.gold18kPerGram)} ${selectedCurrency.symbol}",
                                                "Live spot updated: 24K = ${formatMoney(live.gold24kPerGram)} · 21K = ${formatMoney(live.gold21kPerGram)} · 18K = ${formatMoney(live.gold18kPerGram)} ${selectedCurrency.symbol}",
                                                "Jonli bozor narxlari yangilandi: 24K = ${formatMoney(live.gold24kPerGram)} · 21K = ${formatMoney(live.gold21kPerGram)} ${selectedCurrency.symbol}"
                                            )
                                        }
                                        .onFailure {
                                            liveMetalsStatus = L10n.pick(
                                                lang,
                                                "تعذّر جلب السعر الحي حالياً — يمكنك تعديل السعر يدوياً",
                                                "Unable to fetch live spot price — manual entry remains active",
                                                "Jonli narxni olib bo'lmadi — narxni qo'lda kiritishingiz mumkin"
                                            )
                                        }
                                    fetchingLiveMetals = false
                                }
                            },
                            enabled = !fetchingLiveMetals,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("zakat_fetch_live_gold_btn")
                        ) {
                            if (fetchingLiveMetals) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "تحديث أسعار الذهب والفضة العالمية الحية (${selectedCurrency.code})",
                                        "Sync Live World Gold & Silver Prices (${selectedCurrency.code})",
                                        "Jonli jahon oltin va kumush narxini olish (${selectedCurrency.code})"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        liveMetalsStatus?.let { status ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (latestLiveMetals != null) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Cash, Investments & Business Goods
                item {
                    LiquidGlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = noorColors.goldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "النقود والمدخرات والتجارة", "Cash, Savings & Business", "Naqd pul, jamg'arma va savdo"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        ZakatNumberField(
                            value = cashText,
                            onValueChange = { cashText = it },
                            label = L10n.pick(lang, "النقود والأرصدة البنكية", "Cash & Bank Balances", "Naqd va bank hisobidagi pul"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth().testTag("zakat_cash_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ZakatNumberField(
                            value = investmentsText,
                            onValueChange = { investmentsText = it },
                            label = L10n.pick(lang, "الأسهم والاستثمارات الزكوية", "Stocks & Investments", "Aksiya va investitsiyalar"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ZakatNumberField(
                            value = tradeGoodsText,
                            onValueChange = { tradeGoodsText = it },
                            label = L10n.pick(lang, "عروض التجارة والبضائع", "Business Inventory / Trade Goods", "Savdo mollari qiymati"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ZakatNumberField(
                            value = receivablesText,
                            onValueChange = { receivablesText = it },
                            label = L10n.pick(lang, "ديون لك عند الآخرين (مرجوة السداد)", "Money Owed to You", "Boshqalardan olinadigan qarz"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ZakatNumberField(
                            value = debtsText,
                            onValueChange = { debtsText = it },
                            label = L10n.pick(lang, "الديون والالتزامات الحالة عليك (تُخصم)", "Immediate Debts Owed (Deducted)", "To'lanishi kerak qarzlar (ayiriladi)"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Gold & Silver Holdings by Karat
                item {
                    LiquidGlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Diamond, contentDescription = null, tint = noorColors.goldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "الذهب والفضة (بالغرام)", "Gold & Silver (in Grams)", "Oltin va kumush (grammda)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZakatNumberField(
                                value = gold24kText,
                                onValueChange = { gold24kText = it },
                                label = L10n.pick(lang, "ذهب عيار ٢٤", "24K Gold", "Oltin 24K"),
                                suffix = "g",
                                modifier = Modifier.weight(1f)
                            )
                            ZakatNumberField(
                                value = gold21kText,
                                onValueChange = { gold21kText = it },
                                label = L10n.pick(lang, "ذهب عيار ٢١", "21K Gold", "Oltin 21K"),
                                suffix = "g",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZakatNumberField(
                                value = gold18kText,
                                onValueChange = { gold18kText = it },
                                label = L10n.pick(lang, "ذهب عيار ١٨", "18K Gold", "Oltin 18K"),
                                suffix = "g",
                                modifier = Modifier.weight(1f)
                            )
                            ZakatNumberField(
                                value = silverGramsText,
                                onValueChange = { silverGramsText = it },
                                label = L10n.pick(lang, "الفضة", "Silver", "Kumush"),
                                suffix = "g",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                cashText = ""
                                gold24kText = ""
                                gold21kText = ""
                                gold18kText = ""
                                silverGramsText = ""
                                investmentsText = ""
                                tradeGoodsText = ""
                                receivablesText = ""
                                debtsText = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "تصفير الحقول", "Reset Inputs", "Tozalash"))
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Zakat al-Fitr Calculator
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Text(
                            text = L10n.pick(lang, "إجمالي زكاة الفطر المستحقة", "Total Zakat al-Fitr Due", "Jami Fitr sadaqasi"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${formatMoney(fitrResult.totalAmount)} ${selectedCurrency.symbol}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldBright
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "أو ما يعادل ${formatRawNumber(fitrResult.totalFoodKg)} كغ من غالب قوت البلد (صاع ≈ ٢.٥ كغ لكل فرد)",
                                "Or approx. ${formatRawNumber(fitrResult.totalFoodKg)} kg of staple food (~2.5 kg per person)",
                                "Yoki har bir kishi uchun ~2.5 kg dan jami ${formatRawNumber(fitrResult.totalFoodKg)} kg oziq-ovqat"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    LiquidGlassCard {
                        Text(
                            text = L10n.pick(lang, "عدد أفراد الأسرة", "Number of Family Members", "Oila a'zolari soni"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (fitrMembers > 1) fitrMembers-- },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = noorColors.goldAccent)
                            }
                            Text(
                                text = fitrMembers.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = noorColors.goldAccent
                            )
                            IconButton(
                                onClick = { if (fitrMembers < 50) fitrMembers++ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = noorColors.goldAccent)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ZakatNumberField(
                            value = fitrPerPersonText,
                            onValueChange = { fitrPerPersonText = it },
                            label = L10n.pick(lang, "قيمة الفطرة للفرد الواحد", "Fitr Amount per Person", "Bir kishi uchun Fitr miqdori"),
                            suffix = selectedCurrency.symbol,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // 8 Quranic Recipients of Zakat (Surah At-Tawbah 9:60)
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Text(
                            text = "﴿إِنَّمَا الصَّدَقَاتُ لِلْفُقَرَاءِ وَالْمَسَاكِينِ وَالْعَامِلِينَ عَلَيْهَا وَالْمُؤَلَّفَةِ قُلُوبُهُمْ وَفِي الرِّقَابِ وَالْغَارِمِينَ وَفِي سَبِيلِ اللَّهِ وَابْنِ السَّبِيلِ﴾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(lang, "سورة التوبة — الآية ٦٠", "Surah At-Tawbah — Ayah 60", "Tavba surasi — 60-oyat"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val recipients = listOf(
                    Triple("١. الفقراء", "1. The Poor (Al-Fuqara)", "1. Faqirlar — asosiy ehtiyoji qoplanmaydiganlar"),
                    Triple("٢. المساكين", "2. The Needy (Al-Masakin)", "2. Miskinlar — daromadi yetarli bo'lmaganlar"),
                    Triple("٣. العاملون عليها", "3. Zakat Administrators", "3. Zakot yig'uvchi va tarqatuvchilar"),
                    Triple("٤. المؤلفة قلوبهم", "4. Those Whose Hearts Are Reconciled", "4. Qalblari Islomga moyil qilinuvchilar"),
                    Triple("٥. في الرقاب", "5. Freeing Those in Bondage", "5. Qullarni ozod qilish yo'lida"),
                    Triple("٦. الغارمون", "6. Those Burdened by Debt", "6. Qarzini to'lay olmayotgan qarzdorlar"),
                    Triple("٧. في سبيل الله", "7. In the Cause of Allah", "7. Alloh yo'lidagi xayrli ishlar"),
                    Triple("٨. ابن السبيل", "8. The Stranded Traveler", "8. Yo'lda qolib ketgan musofirlar")
                )
                items(recipients) { (ar, en, uz) ->
                    LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = L10n.pick(lang, ar, en, uz),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZakatMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (noorColors.isDark) Color(0x47103329) else Color(0xD9E4F4EE),
                        if (noorColors.isDark) Color(0x36081D17) else Color(0xB8D8EFE6)
                    )
                )
            )
            .border(0.9.dp, noorColors.glassBorderBottom, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ZakatNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() || it == '.' }
            if (cleaned.count { it == '.' } <= 1) {
                onValueChange(cleaned)
            }
        },
        label = { Text(label, maxLines = 1) },
        suffix = { Text(suffix, fontWeight = FontWeight.Bold) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

private fun parseNumber(text: String): Double = text.trim().toDoubleOrNull() ?: 0.0

private fun formatMoney(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (amount > 0 && amount < 1000) 2 else 0
    }
    return nf.format(amount)
}

private fun formatRawNumber(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)
}
