package com.example.domain.mirath

import kotlin.math.max
import kotlin.math.min

enum class DeceasedGender {
    MALE,
    FEMALE
}

data class MirathInput(
    val totalEstate: Double = 0.0,
    val funeralExpenses: Double = 0.0,
    val debts: Double = 0.0,
    val wasiyyah: Double = 0.0,
    val deceasedGender: DeceasedGender = DeceasedGender.MALE,
    val hasSpouse: Boolean = false,
    val wivesCount: Int = 1, // Used when deceased is MALE and hasSpouse = true (1..4)
    val hasFather: Boolean = false,
    val hasMother: Boolean = false,
    val hasPaternalGrandfather: Boolean = false,
    val hasGrandmother: Boolean = false,
    val sonsCount: Int = 0,
    val daughtersCount: Int = 0,
    val sonSonsCount: Int = 0,
    val sonDaughtersCount: Int = 0,
    val fullBrothersCount: Int = 0,
    val fullSistersCount: Int = 0,
    val paternalBrothersCount: Int = 0,
    val paternalSistersCount: Int = 0,
    val maternalSiblingsCount: Int = 0
)

data class MirathHeirShare(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val count: Int,
    val fractionLabel: String,
    val sharePercentage: Double,
    val totalAmount: Double,
    val perPersonAmount: Double,
    val reasonAr: String,
    val reasonEn: String,
    val reasonUz: String
)

data class MirathBlockedHeir(
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val blockedByAr: String,
    val blockedByEn: String,
    val blockedByUz: String
)

enum class MirathAdjustmentType {
    EXACT,
    AWL,
    RADD,
    NO_HEIRS
}

data class MirathResult(
    val grossEstate: Double,
    val netEstateAfterDebts: Double,
    val allowedWasiyyah: Double,
    val wasiyyahCappedAtOneThird: Boolean,
    val distributableEstate: Double,
    val shares: List<MirathHeirShare>,
    val blockedHeirs: List<MirathBlockedHeir>,
    val adjustmentType: MirathAdjustmentType,
    val isUmariyyataynCase: Boolean = false,
    val remainderToBaytAlMal: Double = 0.0
)

/**
 * Pure Kotlin Islamic Inheritance (المواريث الشرعية) Engine following
 * Surah An-Nisa (4:11-12, 4:176) and Jumhur Sunni Fiqh rules:
 * - Funeral & Debt deduction -> 1/3 capped Wasiyyah
 * - Fixed Shares (أصحاب الفروض), including Umariyyatayn (العمريتان)
 * - Blocking rules (الحجب)
 * - Residuary distribution (العصبة بالنفس وبالغير ومع الغير)
 * - Awl (العول) & Radd (الرد)
 */
object MirathCalculatorEngine {

    private data class MutableShare(
        val key: String,
        val nameAr: String,
        val nameEn: String,
        val nameUz: String,
        val count: Int,
        var fraction: Double,
        var fractionLabel: String,
        val isSpouse: Boolean,
        var reasonAr: String,
        var reasonEn: String,
        var reasonUz: String
    )

    fun calculate(input: MirathInput): MirathResult {
        val gross = max(0.0, input.totalEstate)
        val funeral = max(0.0, input.funeralExpenses)
        val debts = max(0.0, input.debts)
        val afterDebts = max(0.0, gross - funeral - debts)
        val maxWasiyyah = afterDebts / 3.0
        val requestedWasiyyah = max(0.0, input.wasiyyah)
        val wasiyyahCapped = requestedWasiyyah > maxWasiyyah + 1e-6 && afterDebts > 0.0
        val allowedWasiyyah = min(requestedWasiyyah, maxWasiyyah)
        val distributable = max(0.0, afterDebts - allowedWasiyyah)

        val sons = max(0, input.sonsCount)
        val daughters = max(0, input.daughtersCount)
        val sonSons = max(0, input.sonSonsCount)
        val sonDaughters = max(0, input.sonDaughtersCount)
        val fullBros = max(0, input.fullBrothersCount)
        val fullSis = max(0, input.fullSistersCount)
        val patBros = max(0, input.paternalBrothersCount)
        val patSis = max(0, input.paternalSistersCount)
        val matSibs = max(0, input.maternalSiblingsCount)
        val wives = if (input.deceasedGender == DeceasedGender.MALE && input.hasSpouse) {
            input.wivesCount.coerceIn(1, 4)
        } else 0
        val hasHusband = input.deceasedGender == DeceasedGender.FEMALE && input.hasSpouse

        val hasChildren = sons > 0 || daughters > 0
        val hasGrandchildren = sonSons > 0 || sonDaughters > 0
        val hasDescendants = hasChildren || hasGrandchildren
        val hasMaleDescendants = sons > 0 || sonSons > 0
        val hasFemaleDescendants = daughters > 0 || (sons == 0 && sonDaughters > 0)
        val totalSiblings = fullBros + fullSis + patBros + patSis + matSibs
        val hasMaleAscendant = input.hasFather || input.hasPaternalGrandfather

        val activeShares = mutableListOf<MutableShare>()
        val blocked = mutableListOf<MirathBlockedHeir>()

        // 1. Spouse (الزوج / الزوجة)
        var spouseFraction = 0.0
        if (hasHusband) {
            spouseFraction = if (hasDescendants) 1.0 / 4.0 else 1.0 / 2.0
            activeShares += MutableShare(
                key = "husband",
                nameAr = "الزوج", nameEn = "Husband", nameUz = "Er",
                count = 1,
                fraction = spouseFraction,
                fractionLabel = if (hasDescendants) "1/4" else "1/2",
                isSpouse = true,
                reasonAr = if (hasDescendants) "الربع لوجود الفرع الوارث" else "النصف لعدم وجود الفرع الوارث",
                reasonEn = if (hasDescendants) "1/4 due to presence of descendants" else "1/2 as no descendants exist",
                reasonUz = if (hasDescendants) "Farzandlar borligi sababli 1/4" else "Farzandlar yo'qligi sababli 1/2"
            )
        } else if (wives > 0) {
            spouseFraction = if (hasDescendants) 1.0 / 8.0 else 1.0 / 4.0
            activeShares += MutableShare(
                key = "wife",
                nameAr = if (wives == 1) "الزوجة" else "الزوجات ($wives)",
                nameEn = if (wives == 1) "Wife" else "Wives ($wives)",
                nameUz = if (wives == 1) "Xotin" else "Xotinlar ($wives)",
                count = wives,
                fraction = spouseFraction,
                fractionLabel = if (hasDescendants) "1/8" else "1/4",
                isSpouse = true,
                reasonAr = if (hasDescendants) "الثمن لوجود الفرع الوارث" else "الربع لعدم وجود الفرع الوارث",
                reasonEn = if (hasDescendants) "1/8 due to presence of descendants" else "1/4 as no descendants exist",
                reasonUz = if (hasDescendants) "Farzandlar borligi sababli 1/8" else "Farzandlar yo'qligi sababli 1/4"
            )
        }

        // Check Umariyyatayn (العمريتان): Spouse + Mother + Father only (no descendants, <2 siblings)
        val isUmariyyatayn = (hasHusband || wives > 0) &&
            input.hasMother && input.hasFather &&
            !hasDescendants && totalSiblings < 2

        // 2. Mother (الأم)
        if (input.hasMother) {
            val (motherFrac, label, rAr, rEn, rUz) = when {
                isUmariyyatayn -> Quint(
                    (1.0 - spouseFraction) / 3.0,
                    "1/3 الباقي",
                    "ثلث الباقي بعد نصيب الزوجية (المسألة العمرية)",
                    "1/3 of remainder after spouse (Umariyyatayn rule)",
                    "Turmush o'rtog'i ulushidan qolganining 1/3 qismi (Umariyatayn)"
                )
                hasDescendants || totalSiblings >= 2 -> Quint(
                    1.0 / 6.0,
                    "1/6",
                    if (hasDescendants) "السدس لوجود الفرع الوارث" else "السدس لوجود جمع من الإخوة",
                    if (hasDescendants) "1/6 due to descendants" else "1/6 due to two or more siblings",
                    if (hasDescendants) "Farzandlar borligi sababli 1/6" else "Ikki yoki undan ortiq aka-uka/opa-singillar sababli 1/6"
                )
                else -> Quint(
                    1.0 / 3.0,
                    "1/3",
                    "الثلث لعدم وجود فرع وارث أو جمع من الإخوة",
                    "1/3 as no descendants or multiple siblings exist",
                    "Farzandlar va ko'p aka-uka/opa-singillar yo'qligi sababli 1/3"
                )
            }
            activeShares += MutableShare(
                key = "mother",
                nameAr = "الأم", nameEn = "Mother", nameUz = "Ona",
                count = 1,
                fraction = motherFrac,
                fractionLabel = label,
                isSpouse = false,
                reasonAr = rAr, reasonEn = rEn, reasonUz = rUz
            )
        }

        // 3. Grandmother (الجدة)
        if (input.hasGrandmother) {
            if (input.hasMother) {
                blocked += MirathBlockedHeir(
                    "الجدة", "Grandmother", "Buvi",
                    "محجوبة بالأم", "Blocked by Mother", "Ona tomonidan to'silgan"
                )
            } else {
                activeShares += MutableShare(
                    key = "grandmother",
                    nameAr = "الجدة", nameEn = "Grandmother", nameUz = "Buvi",
                    count = 1,
                    fraction = 1.0 / 6.0,
                    fractionLabel = "1/6",
                    isSpouse = false,
                    reasonAr = "السدس لعدم وجود الأم",
                    reasonEn = "1/6 in absence of Mother",
                    reasonUz = "Ona yo'qligi sababli 1/6"
                )
            }
        }

        // 4. Father (الأب) & Paternal Grandfather (الجد)
        var fatherIndex = -1
        if (input.hasFather) {
            if (hasDescendants) {
                activeShares += MutableShare(
                    key = "father",
                    nameAr = "الأب", nameEn = "Father", nameUz = "Ota",
                    count = 1,
                    fraction = 1.0 / 6.0,
                    fractionLabel = if (hasMaleDescendants) "1/6" else "1/6 + عصبة",
                    isSpouse = false,
                    reasonAr = if (hasMaleDescendants) "السدس فرضاً لوجود الفرع الذكر" else "السدس فرضاً + الباقي تعصيباً مع الفرع الأنثى",
                    reasonEn = if (hasMaleDescendants) "1/6 fixed share with male descendants" else "1/6 fixed + residuary with female descendants",
                    reasonUz = if (hasMaleDescendants) "O'g'il farzandlar bilan 1/6" else "Qiz farzandlar bilan 1/6 + qoldiq (asaba)"
                )
                fatherIndex = activeShares.lastIndex
            }
        }

        var grandfatherIndex = -1
        if (input.hasPaternalGrandfather) {
            if (input.hasFather) {
                blocked += MirathBlockedHeir(
                    "الجد لأب", "Paternal Grandfather", "Ota tomonidan bobo",
                    "محجوب بالأب", "Blocked by Father", "Ota tomonidan to'silgan"
                )
            } else if (hasDescendants) {
                activeShares += MutableShare(
                    key = "grandfather",
                    nameAr = "الجد لأب", nameEn = "Paternal Grandfather", nameUz = "Ota tomonidan bobo",
                    count = 1,
                    fraction = 1.0 / 6.0,
                    fractionLabel = if (hasMaleDescendants) "1/6" else "1/6 + عصبة",
                    isSpouse = false,
                    reasonAr = if (hasMaleDescendants) "السدس فرضاً لوجود الفرع الذكر" else "السدس فرضاً + الباقي تعصيباً",
                    reasonEn = if (hasMaleDescendants) "1/6 fixed share with male descendants" else "1/6 fixed + residuary",
                    reasonUz = if (hasMaleDescendants) "O'g'il farzandlar bilan 1/6" else "1/6 + qoldiq"
                )
                grandfatherIndex = activeShares.lastIndex
            }
        }

        // 5. Daughters (البنات) — Fixed share only when no sons exist
        if (daughters > 0 && sons == 0) {
            val frac = if (daughters == 1) 1.0 / 2.0 else 2.0 / 3.0
            activeShares += MutableShare(
                key = "daughters",
                nameAr = if (daughters == 1) "البنت" else "البنات ($daughters)",
                nameEn = if (daughters == 1) "Daughter" else "Daughters ($daughters)",
                nameUz = if (daughters == 1) "Qiz" else "Qizlar ($daughters)",
                count = daughters,
                fraction = frac,
                fractionLabel = if (daughters == 1) "1/2" else "2/3",
                isSpouse = false,
                reasonAr = if (daughters == 1) "النصف لانفرادها وعدم وجود ابن" else "الثلثان لتعددهن وعدم وجود ابن",
                reasonEn = if (daughters == 1) "1/2 as sole daughter with no son" else "2/3 shared among daughters with no son",
                reasonUz = if (daughters == 1) "Yolg'iz qiz (o'g'il yo'q) — 1/2" else "Bir nechta qiz (o'g'il yo'q) — 2/3"
            )
        }

        // 6. Son's Sons & Son's Daughters (أبناء الابن وبنات الابن)
        if (sonSons > 0 && sons > 0) {
            blocked += MirathBlockedHeir(
                "أبناء الابن", "Son's Sons", "O'g'ilning o'g'illari",
                "محجوبون بالابن", "Blocked by Son", "O'g'il tomonidan to'silgan"
            )
        }
        if (sonDaughters > 0) {
            when {
                sons > 0 -> blocked += MirathBlockedHeir(
                    "بنات الابن", "Son's Daughters", "O'g'ilning qizlari",
                    "محجوبات بالابن", "Blocked by Son", "O'g'il tomonidan to'silgan"
                )
                daughters >= 2 && sonSons == 0 -> blocked += MirathBlockedHeir(
                    "بنات الابن", "Son's Daughters", "O'g'ilning qizlari",
                    "محجوبات لاستغراق البنتين للثلثين", "Blocked by 2+ Daughters taking 2/3", "2+ qiz 2/3 ni olgani uchun to'silgan"
                )
                sonSons == 0 -> {
                    val (frac, label, rAr, rEn, rUz) = if (daughters == 1) {
                        Quint(
                            1.0 / 6.0, "1/6",
                            "السدس تكملة للثلثين مع البنت الواحدة",
                            "1/6 completing 2/3 alongside one daughter",
                            "Bitta qiz bilan 2/3 ni to'ldirish uchun 1/6"
                        )
                    } else if (sonDaughters == 1) {
                        Quint(
                            1.0 / 2.0, "1/2",
                            "النصف لانفرادها وعدم وجود فرع أعلى",
                            "1/2 as sole son's daughter",
                            "Yolg'iz o'g'il qizi — 1/2"
                        )
                    } else {
                        Quint(
                            2.0 / 3.0, "2/3",
                            "الثلثان لتعددهن وعدم وجود فرع أعلى",
                            "2/3 shared among son's daughters",
                            "Bir nechta o'g'il qizlari — 2/3"
                        )
                    }
                    activeShares += MutableShare(
                        key = "son_daughters",
                        nameAr = if (sonDaughters == 1) "بنت الابن" else "بنات الابن ($sonDaughters)",
                        nameEn = if (sonDaughters == 1) "Son's Daughter" else "Son's Daughters ($sonDaughters)",
                        nameUz = if (sonDaughters == 1) "O'g'ilning qizi" else "O'g'ilning qizlari ($sonDaughters)",
                        count = sonDaughters,
                        fraction = frac,
                        fractionLabel = label,
                        isSpouse = false,
                        reasonAr = rAr, reasonEn = rEn, reasonUz = rUz
                    )
                }
            }
        }

        // 7. Maternal Siblings (الإخوة لأم)
        if (matSibs > 0) {
            if (hasDescendants || hasMaleAscendant) {
                blocked += MirathBlockedHeir(
                    "الإخوة لأم", "Maternal Siblings", "Ona bir aka-uka/opa-singillar",
                    "محجوبون بالفرع الوارث أو الأصل الذكر",
                    "Blocked by Descendants or Father/Grandfather",
                    "Farzandlar yoki Ota/Bobo tomonidan to'silgan"
                )
            } else {
                val frac = if (matSibs == 1) 1.0 / 6.0 else 1.0 / 3.0
                activeShares += MutableShare(
                    key = "maternal_siblings",
                    nameAr = if (matSibs == 1) "أخ / أخت لأم" else "الإخوة لأم ($matSibs)",
                    nameEn = if (matSibs == 1) "Maternal Sibling" else "Maternal Siblings ($matSibs)",
                    nameUz = if (matSibs == 1) "Ona bir aka/opa" else "Ona bir tug'ishganlar ($matSibs)",
                    count = matSibs,
                    fraction = frac,
                    fractionLabel = if (matSibs == 1) "1/6" else "1/3",
                    isSpouse = false,
                    reasonAr = if (matSibs == 1) "السدس لعدم وجود فرع أو أصل ذكر" else "الثلث بالتساوي بين الذكور والإناث",
                    reasonEn = if (matSibs == 1) "1/6 (Kalalah)" else "1/3 shared equally regardless of gender",
                    reasonUz = if (matSibs == 1) "Kalola — 1/6" else "Erkak va ayol teng ulushda 1/3"
                )
            }
        }

        // 8. Full & Paternal Siblings Blocking + Fixed Shares
        val siblingsBlockedByAscOrMaleDesc = hasMaleDescendants || hasMaleAscendant
        if (siblingsBlockedByAscOrMaleDesc) {
            if (fullBros > 0) blocked += MirathBlockedHeir("الإخوة الأشقاء", "Full Brothers", "Tug'ishgan aka-ukalar", "محجوبون بالفرع الذكر أو الأب/الجد", "Blocked by Male Descendant or Father/Grandfather", "O'g'il farzand yoki Ota/Bobo tomonidan to'silgan")
            if (fullSis > 0) blocked += MirathBlockedHeir("الأخوات الشقيقات", "Full Sisters", "Tug'ishgan opa-singillar", "محجوبات بالفرع الذكر أو الأب/الجد", "Blocked by Male Descendant or Father/Grandfather", "O'g'il farzand yoki Ota/Bobo tomonidan to'silgan")
            if (patBros > 0) blocked += MirathBlockedHeir("الإخوة لأب", "Paternal Brothers", "Ota bir aka-ukalar", "محجوبون بالفرع الذكر أو الأب/الجد", "Blocked by Male Descendant or Father/Grandfather", "O'g'il farzand yoki Ota/Bobo tomonidan to'silgan")
            if (patSis > 0) blocked += MirathBlockedHeir("الأخوات لأب", "Paternal Sisters", "Ota bir opa-singillar", "محجوبات بالفرع الذكر أو الأب/الجد", "Blocked by Male Descendant or Father/Grandfather", "O'g'il farzand yoki Ota/Bobo tomonidan to'silgan")
        } else {
            // Full sisters fixed share only if no full brothers and no female descendants (with female descendants they become Asabah ma'al-ghayr)
            if (fullSis > 0 && fullBros == 0 && !hasFemaleDescendants) {
                val frac = if (fullSis == 1) 1.0 / 2.0 else 2.0 / 3.0
                activeShares += MutableShare(
                    key = "full_sisters",
                    nameAr = if (fullSis == 1) "الأخت الشقيقة" else "الأخوات الشقيقات ($fullSis)",
                    nameEn = if (fullSis == 1) "Full Sister" else "Full Sisters ($fullSis)",
                    nameUz = if (fullSis == 1) "Tug'ishgan opa/singil" else "Tug'ishgan opa-singillar ($fullSis)",
                    count = fullSis,
                    fraction = frac,
                    fractionLabel = if (fullSis == 1) "1/2" else "2/3",
                    isSpouse = false,
                    reasonAr = if (fullSis == 1) "النصف لانفرادها" else "الثلثان لتعددهن",
                    reasonEn = if (fullSis == 1) "1/2 as sole full sister" else "2/3 shared among full sisters",
                    reasonUz = if (fullSis == 1) "Yolg'iz tug'ishgan opa/singil — 1/2" else "Bir nechta tug'ishgan opa-singillar — 2/3"
                )
            }
            // Paternal brothers blocked by full brother OR full sister asabah ma'al-ghayr
            val fullSisAsabahMaalGhayr = fullSis > 0 && fullBros == 0 && hasFemaleDescendants
            if (patBros > 0 && (fullBros > 0 || fullSisAsabahMaalGhayr)) {
                blocked += MirathBlockedHeir(
                    "الإخوة لأب", "Paternal Brothers", "Ota bir aka-ukalar",
                    "محجوبون بالأخ الشقيق أو الشقيقة العصبة مع الغير",
                    "Blocked by Full Brother or Full Sister Asabah",
                    "Tug'ishgan aka/opa tomonidan to'silgan"
                )
            }
            if (patSis > 0) {
                when {
                    fullBros > 0 || fullSisAsabahMaalGhayr -> blocked += MirathBlockedHeir(
                        "الأخوات لأب", "Paternal Sisters", "Ota bir opa-singillar",
                        "محجوبات بالأخ الشقيق أو الشقيقة العصبة",
                        "Blocked by Full Brother or Full Sister Asabah",
                        "Tug'ishgan aka/opa tomonidan to'silgan"
                    )
                    fullSis >= 2 && patBros == 0 -> blocked += MirathBlockedHeir(
                        "الأخوات لأب", "Paternal Sisters", "Ota bir opa-singillar",
                        "محجوبات لاستغراق الشقيقتين للثلثين",
                        "Blocked by 2+ Full Sisters taking 2/3",
                        "2+ tug'ishgan opa-singil 2/3 ni olgani uchun to'silgan"
                    )
                    patBros == 0 && !hasFemaleDescendants -> {
                        val (frac, label, rAr, rEn, rUz) = if (fullSis == 1) {
                            Quint(1.0 / 6.0, "1/6", "السدس تكملة للثلثين مع الأخت الشقيقة", "1/6 completing 2/3 with 1 Full Sister", "Bitta tug'ishgan opa bilan 2/3 ni to'ldirish uchun 1/6")
                        } else if (patSis == 1) {
                            Quint(1.0 / 2.0, "1/2", "النصف لانفرادها", "1/2 as sole paternal sister", "Yolg'iz ota bir opa/singil — 1/2")
                        } else {
                            Quint(2.0 / 3.0, "2/3", "الثلثان لتعددهن", "2/3 shared among paternal sisters", "Bir nechta ota bir opa-singillar — 2/3")
                        }
                        activeShares += MutableShare(
                            key = "paternal_sisters",
                            nameAr = if (patSis == 1) "الأخت لأب" else "الأخوات لأب ($patSis)",
                            nameEn = if (patSis == 1) "Paternal Sister" else "Paternal Sisters ($patSis)",
                            nameUz = if (patSis == 1) "Ota bir opa/singil" else "Ota bir opa-singillar ($patSis)",
                            count = patSis,
                            fraction = frac,
                            fractionLabel = label,
                            isSpouse = false,
                            reasonAr = rAr, reasonEn = rEn, reasonUz = rUz
                        )
                    }
                }
            }
        }

        // 9. Calculate Remainder & Distribute to Residuary (العصبة)
        val sumFixed = activeShares.sumOf { it.fraction }
        val remainder = max(0.0, 1.0 - sumFixed)
        var hasAsabahAssigned = false

        if (remainder > 1e-7) {
            when {
                // Priority 1: Sons (+ Daughters 2:1)
                sons > 0 -> {
                    val units = sons * 2 + daughters
                    activeShares += MutableShare(
                        key = "sons",
                        nameAr = if (sons == 1) "الابن" else "الأبناء ($sons)",
                        nameEn = if (sons == 1) "Son" else "Sons ($sons)",
                        nameUz = if (sons == 1) "O'g'il" else "O'g'illar ($sons)",
                        count = sons,
                        fraction = remainder * (sons * 2.0 / units),
                        fractionLabel = if (daughters > 0) "عصبة (للذكر مثل حظ الأنثيين)" else "عصبة (الباقي)",
                        isSpouse = false,
                        reasonAr = "عصبة بالنفس (الباقي بعد أصحاب الفروض)",
                        reasonEn = "Residuary heir (Asabah) — male gets 2x female share",
                        reasonUz = "Asaba (qoldiq merosxo'r)"
                    )
                    if (daughters > 0) {
                        activeShares += MutableShare(
                            key = "daughters_asabah",
                            nameAr = if (daughters == 1) "البنت" else "البنات ($daughters)",
                            nameEn = if (daughters == 1) "Daughter" else "Daughters ($daughters)",
                            nameUz = if (daughters == 1) "Qiz" else "Qizlar ($daughters)",
                            count = daughters,
                            fraction = remainder * (daughters.toDouble() / units),
                            fractionLabel = "عصبة بالغير",
                            isSpouse = false,
                            reasonAr = "عصبة بالابن للذكر مثل حظ الأنثيين",
                            reasonEn = "Residuary with Son (2:1 ratio)",
                            reasonUz = "O'g'il bilan birga asaba (2:1 nisbat)"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 2: Son's Sons (+ Son's Daughters 2:1 when no sons)
                sonSons > 0 && sons == 0 -> {
                    val units = sonSons * 2 + sonDaughters
                    activeShares += MutableShare(
                        key = "son_sons",
                        nameAr = if (sonSons == 1) "ابن الابن" else "أبناء الابن ($sonSons)",
                        nameEn = if (sonSons == 1) "Son's Son" else "Son's Sons ($sonSons)",
                        nameUz = if (sonSons == 1) "O'g'ilning o'g'li" else "O'g'ilning o'g'illari ($sonSons)",
                        count = sonSons,
                        fraction = remainder * (sonSons * 2.0 / units),
                        fractionLabel = "عصبة",
                        isSpouse = false,
                        reasonAr = "عصبة بالنفس لعدم وجود ابن",
                        reasonEn = "Residuary heir (Asabah) in absence of Son",
                        reasonUz = "O'g'il yo'qligi sababli asaba"
                    )
                    if (sonDaughters > 0) {
                        activeShares += MutableShare(
                            key = "son_daughters_asabah",
                            nameAr = if (sonDaughters == 1) "بنت الابن" else "بنات الابن ($sonDaughters)",
                            nameEn = if (sonDaughters == 1) "Son's Daughter" else "Son's Daughters ($sonDaughters)",
                            nameUz = if (sonDaughters == 1) "O'g'ilning qizi" else "O'g'ilning qizlari ($sonDaughters)",
                            count = sonDaughters,
                            fraction = remainder * (sonDaughters.toDouble() / units),
                            fractionLabel = "عصبة بالغير",
                            isSpouse = false,
                            reasonAr = "عصبة بابن الابن للذكر مثل حظ الأنثيين",
                            reasonEn = "Residuary with Son's Son (2:1 ratio)",
                            reasonUz = "O'g'ilning o'g'li bilan asaba (2:1)"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 3: Father (pure Asabah or 1/6 + Asabah)
                input.hasFather -> {
                    if (fatherIndex >= 0) {
                        activeShares[fatherIndex].fraction += remainder
                    } else {
                        activeShares += MutableShare(
                            key = "father_asabah",
                            nameAr = "الأب", nameEn = "Father", nameUz = "Ota",
                            count = 1,
                            fraction = remainder,
                            fractionLabel = "عصبة (الباقي)",
                            isSpouse = false,
                            reasonAr = "عصبة بالنفس لعدم وجود فرع وارث",
                            reasonEn = "Residuary heir (Asabah) with no descendants",
                            reasonUz = "Farzandlar yo'qligi sababli asaba"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 4: Paternal Grandfather
                input.hasPaternalGrandfather && !input.hasFather -> {
                    if (grandfatherIndex >= 0) {
                        activeShares[grandfatherIndex].fraction += remainder
                    } else {
                        activeShares += MutableShare(
                            key = "grandfather_asabah",
                            nameAr = "الجد لأب", nameEn = "Paternal Grandfather", nameUz = "Ota tomonidan bobo",
                            count = 1,
                            fraction = remainder,
                            fractionLabel = "عصبة (الباقي)",
                            isSpouse = false,
                            reasonAr = "عصبة بالنفس لعدم وجود الأب والفرع",
                            reasonEn = "Residuary heir in absence of Father & descendants",
                            reasonUz = "Ota va farzandlar yo'qligi sababli asaba"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 5: Full Brothers (+ Full Sisters 2:1)
                fullBros > 0 && !siblingsBlockedByAscOrMaleDesc -> {
                    val units = fullBros * 2 + fullSis
                    activeShares += MutableShare(
                        key = "full_brothers",
                        nameAr = if (fullBros == 1) "الأخ الشقيق" else "الإخوة الأشقاء ($fullBros)",
                        nameEn = if (fullBros == 1) "Full Brother" else "Full Brothers ($fullBros)",
                        nameUz = if (fullBros == 1) "Tug'ishgan aka/uka" else "Tug'ishgan aka-ukalar ($fullBros)",
                        count = fullBros,
                        fraction = remainder * (fullBros * 2.0 / units),
                        fractionLabel = "عصبة",
                        isSpouse = false,
                        reasonAr = "عصبة بالنفس",
                        reasonEn = "Residuary heir (Asabah)",
                        reasonUz = "Asaba (qoldiq merosxo'r)"
                    )
                    if (fullSis > 0) {
                        activeShares += MutableShare(
                            key = "full_sisters_asabah",
                            nameAr = if (fullSis == 1) "الأخت الشقيقة" else "الأخوات الشقيقات ($fullSis)",
                            nameEn = if (fullSis == 1) "Full Sister" else "Full Sisters ($fullSis)",
                            nameUz = if (fullSis == 1) "Tug'ishgan opa/singil" else "Tug'ishgan opa-singillar ($fullSis)",
                            count = fullSis,
                            fraction = remainder * (fullSis.toDouble() / units),
                            fractionLabel = "عصبة بالغير",
                            isSpouse = false,
                            reasonAr = "عصبة بالأخ الشقيق للذكر مثل حظ الأنثيين",
                            reasonEn = "Residuary with Full Brother (2:1)",
                            reasonUz = "Tug'ishgan aka bilan asaba (2:1)"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 6: Full Sisters Asabah ma'al-ghayr (with Daughters / Son's Daughters)
                fullSis > 0 && fullBros == 0 && hasFemaleDescendants && !siblingsBlockedByAscOrMaleDesc -> {
                    activeShares += MutableShare(
                        key = "full_sisters_maal_ghayr",
                        nameAr = if (fullSis == 1) "الأخت الشقيقة" else "الأخوات الشقيقات ($fullSis)",
                        nameEn = if (fullSis == 1) "Full Sister" else "Full Sisters ($fullSis)",
                        nameUz = if (fullSis == 1) "Tug'ishgan opa/singil" else "Tug'ishgan opa-singillar ($fullSis)",
                        count = fullSis,
                        fraction = remainder,
                        fractionLabel = "عصبة مع الغير",
                        isSpouse = false,
                        reasonAr = "عصبة مع الفرع المؤنث (الباقي)",
                        reasonEn = "Residuary alongside female descendants",
                        reasonUz = "Qiz farzandlar bilan birga asaba (qoldiq)"
                    )
                    hasAsabahAssigned = true
                }
                // Priority 7: Paternal Brothers (+ Paternal Sisters 2:1)
                patBros > 0 && fullBros == 0 && !siblingsBlockedByAscOrMaleDesc -> {
                    val units = patBros * 2 + patSis
                    activeShares += MutableShare(
                        key = "paternal_brothers",
                        nameAr = if (patBros == 1) "الأخ لأب" else "الإخوة لأب ($patBros)",
                        nameEn = if (patBros == 1) "Paternal Brother" else "Paternal Brothers ($patBros)",
                        nameUz = if (patBros == 1) "Ota bir aka/uka" else "Ota bir aka-ukalar ($patBros)",
                        count = patBros,
                        fraction = remainder * (patBros * 2.0 / units),
                        fractionLabel = "عصبة",
                        isSpouse = false,
                        reasonAr = "عصبة بالنفس",
                        reasonEn = "Residuary heir (Asabah)",
                        reasonUz = "Asaba (qoldiq merosxo'r)"
                    )
                    if (patSis > 0) {
                        activeShares += MutableShare(
                            key = "paternal_sisters_asabah",
                            nameAr = if (patSis == 1) "الأخت لأب" else "الأخوات لأب ($patSis)",
                            nameEn = if (patSis == 1) "Paternal Sister" else "Paternal Sisters ($patSis)",
                            nameUz = if (patSis == 1) "Ota bir opa/singil" else "Ota bir opa-singillar ($patSis)",
                            count = patSis,
                            fraction = remainder * (patSis.toDouble() / units),
                            fractionLabel = "عصبة بالغير",
                            isSpouse = false,
                            reasonAr = "عصبة بالأخ لأب للذكر مثل حظ الأنثيين",
                            reasonEn = "Residuary with Paternal Brother (2:1)",
                            reasonUz = "Ota bir aka bilan asaba (2:1)"
                        )
                    }
                    hasAsabahAssigned = true
                }
                // Priority 8: Paternal Sisters Asabah ma'al-ghayr
                patSis > 0 && patBros == 0 && fullBros == 0 && fullSis == 0 && hasFemaleDescendants && !siblingsBlockedByAscOrMaleDesc -> {
                    activeShares += MutableShare(
                        key = "paternal_sisters_maal_ghayr",
                        nameAr = if (patSis == 1) "الأخت لأب" else "الأخوات لأب ($patSis)",
                        nameEn = if (patSis == 1) "Paternal Sister" else "Paternal Sisters ($patSis)",
                        nameUz = if (patSis == 1) "Ota bir opa/singil" else "Ota bir opa-singillar ($patSis)",
                        count = patSis,
                        fraction = remainder,
                        fractionLabel = "عصبة مع الغير",
                        isSpouse = false,
                        reasonAr = "عصبة مع الفرع المؤنث (الباقي)",
                        reasonEn = "Residuary alongside female descendants",
                        reasonUz = "Qiz farzandlar bilan birga asaba"
                    )
                    hasAsabahAssigned = true
                }
            }
        }

        // 10. Handle Awl (العول) & Radd (الرد)
        val totalAllocated = activeShares.sumOf { it.fraction }
        var adjustment = MirathAdjustmentType.EXACT
        var baytAlMalRemainder = 0.0

        when {
            activeShares.isEmpty() -> {
                adjustment = MirathAdjustmentType.NO_HEIRS
                baytAlMalRemainder = distributable
            }
            totalAllocated > 1.0 + 1e-6 -> {
                // Awl: proportionally scale down all shares so total == 1.0
                adjustment = MirathAdjustmentType.AWL
                activeShares.forEach { share ->
                    share.fraction /= totalAllocated
                }
            }
            totalAllocated < 1.0 - 1e-6 && !hasAsabahAssigned -> {
                // Radd: redistribute remaining fraction proportionally to non-spouse fixed heirs
                val raddPool = 1.0 - totalAllocated
                val nonSpouseShares = activeShares.filter { !it.isSpouse }
                val nonSpouseSum = nonSpouseShares.sumOf { it.fraction }
                if (nonSpouseSum > 1e-6) {
                    adjustment = MirathAdjustmentType.RADD
                    nonSpouseShares.forEach { share ->
                        share.fraction += raddPool * (share.fraction / nonSpouseSum)
                    }
                } else {
                    // Only spouse exists -> remainder stays unallocated (Bayt al-Mal or Radd to spouse in later opinion)
                    baytAlMalRemainder = distributable * raddPool
                }
            }
        }

        val finalShares = activeShares.map { item ->
            val totalAmt = distributable * item.fraction
            val perPerson = if (item.count > 0) totalAmt / item.count else 0.0
            MirathHeirShare(
                key = item.key,
                nameAr = item.nameAr,
                nameEn = item.nameEn,
                nameUz = item.nameUz,
                count = item.count,
                fractionLabel = item.fractionLabel,
                sharePercentage = item.fraction * 100.0,
                totalAmount = totalAmt,
                perPersonAmount = perPerson,
                reasonAr = item.reasonAr,
                reasonEn = item.reasonEn,
                reasonUz = item.reasonUz
            )
        }

        return MirathResult(
            grossEstate = gross,
            netEstateAfterDebts = afterDebts,
            allowedWasiyyah = allowedWasiyyah,
            wasiyyahCappedAtOneThird = wasiyyahCapped,
            distributableEstate = distributable,
            shares = finalShares,
            blockedHeirs = blocked,
            adjustmentType = adjustment,
            isUmariyyataynCase = isUmariyyatayn,
            remainderToBaytAlMal = baytAlMalRemainder
        )
    }

    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
