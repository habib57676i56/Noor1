package com.example.domain.zakat

import kotlin.math.max

enum class NisabStandard(val key: String) {
    GOLD_85G("gold"),
    SILVER_595G("silver")
}

data class ZakatCurrency(
    val code: String,
    val symbol: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val defaultGold24kPerGram: Double,
    val defaultSilverPerGram: Double,
    val defaultFitrPerPerson: Double
)

data class ZakatCalculationInput(
    val cashAndBank: Double = 0.0,
    val gold24kGrams: Double = 0.0,
    val gold21kGrams: Double = 0.0,
    val gold18kGrams: Double = 0.0,
    val silverGrams: Double = 0.0,
    val investmentsAndStocks: Double = 0.0,
    val tradeInventory: Double = 0.0,
    val receivables: Double = 0.0,
    val immediateDebts: Double = 0.0,
    val gold24kPricePerGram: Double = 85.0,
    val silverPricePerGram: Double = 1.0,
    val nisabStandard: NisabStandard = NisabStandard.GOLD_85G
)

data class ZakatCalculationResult(
    val goldValue: Double,
    val silverValue: Double,
    val totalAssets: Double,
    val netZakatableWealth: Double,
    val nisabThreshold: Double,
    val isZakatDue: Boolean,
    val zakatDueAmount: Double,
    val equivalentPureGoldGrams: Double
)

data class ZakatFitrResult(
    val familyMembers: Int,
    val amountPerPerson: Double,
    val totalAmount: Double,
    val totalFoodKg: Double
)

object ZakatCalculatorEngine {

    const val GOLD_NISAB_GRAMS = 85.0
    const val SILVER_NISAB_GRAMS = 595.0
    const val ZAKAT_RATE = 0.025
    const val FITR_KG_PER_PERSON = 2.5

    val currencies: List<ZakatCurrency> = listOf(
        ZakatCurrency("USD", "$", "دولار أمريكي", "US Dollar", "AQSh dollari", 86.0, 1.0, 12.0),
        ZakatCurrency("SAR", "ر.س", "ريال سعودي", "Saudi Riyal", "Saudiya riyoli", 322.0, 3.75, 25.0),
        ZakatCurrency("AED", "د.إ", "درهم إماراتي", "UAE Dirham", "BAA dirhami", 316.0, 3.67, 25.0),
        ZakatCurrency("TRY", "₺", "ليرة تركية", "Turkish Lira", "Turk lirasi", 2950.0, 34.0, 180.0),
        ZakatCurrency("EGP", "ج.م", "جنيه مصري", "Egyptian Pound", "Misr funti", 4200.0, 49.0, 50.0),
        ZakatCurrency("EUR", "€", "يورو", "Euro", "Yevro", 80.0, 0.94, 10.0),
        ZakatCurrency("UZS", "so'm", "سوم أوزبكي", "Uzbekistan Som", "O'zbek so'mi", 1100000.0, 13000.0, 40000.0)
    )

    fun calculateWealthZakat(input: ZakatCalculationInput): ZakatCalculationResult {
        val safeGoldPrice = max(0.0, input.gold24kPricePerGram)
        val safeSilverPrice = max(0.0, input.silverPricePerGram)

        val pureGoldGrams = max(0.0, input.gold24kGrams) +
            max(0.0, input.gold21kGrams) * (21.0 / 24.0) +
            max(0.0, input.gold18kGrams) * (18.0 / 24.0)

        val goldValue = pureGoldGrams * safeGoldPrice
        val silverValue = max(0.0, input.silverGrams) * safeSilverPrice

        val totalAssets = max(0.0, input.cashAndBank) +
            goldValue +
            silverValue +
            max(0.0, input.investmentsAndStocks) +
            max(0.0, input.tradeInventory) +
            max(0.0, input.receivables)

        val netWealth = max(0.0, totalAssets - max(0.0, input.immediateDebts))

        val nisabThreshold = when (input.nisabStandard) {
            NisabStandard.GOLD_85G -> GOLD_NISAB_GRAMS * safeGoldPrice
            NisabStandard.SILVER_595G -> SILVER_NISAB_GRAMS * safeSilverPrice
        }

        val isDue = nisabThreshold > 0.0 && netWealth >= nisabThreshold
        val zakatAmount = if (isDue) netWealth * ZAKAT_RATE else 0.0

        return ZakatCalculationResult(
            goldValue = goldValue,
            silverValue = silverValue,
            totalAssets = totalAssets,
            netZakatableWealth = netWealth,
            nisabThreshold = nisabThreshold,
            isZakatDue = isDue,
            zakatDueAmount = zakatAmount,
            equivalentPureGoldGrams = pureGoldGrams
        )
    }

    fun calculateZakatAlFitr(familyMembers: Int, amountPerPerson: Double): ZakatFitrResult {
        val safeMembers = familyMembers.coerceAtLeast(1)
        val safePerPerson = max(0.0, amountPerPerson)
        return ZakatFitrResult(
            familyMembers = safeMembers,
            amountPerPerson = safePerPerson,
            totalAmount = safeMembers * safePerPerson,
            totalFoodKg = safeMembers * FITR_KG_PER_PERSON
        )
    }
}
