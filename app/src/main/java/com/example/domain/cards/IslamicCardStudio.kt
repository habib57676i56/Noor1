package com.example.domain.cards

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

enum class IslamicCardCategory(val key: String, val labelAr: String, val labelEn: String, val labelUz: String) {
    QURAN("quran", "آيات قرآنية", "Quran Verses", "Qur'on oyatlari"),
    DUA("dua", "أدعية وأذكار", "Duas & Adhkar", "Duolar va zikrlar"),
    HADITH("hadith", "أحاديث نبوية", "Hadith Gems", "Hadislar"),
    GREETING("greeting", "تهاني ومناسبات", "Greetings", "Tabriklar")
}

enum class CardAspectRatio(val widthPx: Int, val heightPx: Int, val labelAr: String, val labelEn: String, val labelUz: String) {
    SQUARE(1080, 1080, "مربع (1:1)", "Square (1:1)", "Kvadrat (1:1)"),
    PORTRAIT(1080, 1350, "طولي (4:5)", "Portrait (4:5)", "Vertikal (4:5)")
}

data class IslamicCardTemplate(
    val id: String,
    val category: IslamicCardCategory,
    val titleAr: String,
    val bodyAr: String,
    val referenceAr: String,
    val subtitleEn: String,
    val subtitleUz: String
)

data class CardVisualTheme(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val bgTopColor: Int,
    val bgMidColor: Int,
    val bgBottomColor: Int,
    val glowColor: Int,
    val accentGoldColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int
)

object IslamicCardStudio {

    val themes: List<CardVisualTheme> = listOf(
        CardVisualTheme(
            id = "royal_emerald",
            nameAr = "الزمرد الملكي",
            nameEn = "Royal Emerald",
            nameUz = "Shohona zumrad",
            bgTopColor = 0xFF0A3A2B.toInt(),
            bgMidColor = 0xFF07281E.toInt(),
            bgBottomColor = 0xFF041610.toInt(),
            glowColor = 0x44E3BD69.toInt(),
            accentGoldColor = 0xFFE3BD69.toInt(),
            primaryTextColor = 0xFFFDFBF4.toInt(),
            secondaryTextColor = 0xFFD9C79E.toInt()
        ),
        CardVisualTheme(
            id = "midnight_sapphire",
            nameAr = "سماء الفجر",
            nameEn = "Fajr Sapphire",
            nameUz = "Tonggi safir",
            bgTopColor = 0xFF0E2447.toInt(),
            bgMidColor = 0xFF091832.toInt(),
            bgBottomColor = 0xFF040B1A.toInt(),
            glowColor = 0x4475B8FF.toInt(),
            accentGoldColor = 0xFFF3D27A.toInt(),
            primaryTextColor = 0xFFF6F9FF.toInt(),
            secondaryTextColor = 0xFFB8CCE6.toInt()
        ),
        CardVisualTheme(
            id = "mushaf_parchment",
            nameAr = "رقّ المصحف",
            nameEn = "Warm Parchment",
            nameUz = "Mushaf qog'ozi",
            bgTopColor = 0xFFFBF5E7.toInt(),
            bgMidColor = 0xFFF3E6CA.toInt(),
            bgBottomColor = 0xFFE7D3AD.toInt(),
            glowColor = 0x38D4A64A.toInt(),
            accentGoldColor = 0xFF9C7228.toInt(),
            primaryTextColor = 0xFF1A2E26.toInt(),
            secondaryTextColor = 0xFF5A4A2E.toInt()
        ),
        CardVisualTheme(
            id = "madinah_amber",
            nameAr = "غسق المدينة",
            nameEn = "Madinah Amber",
            nameUz = "Madina shomi",
            bgTopColor = 0xFF422214.toInt(),
            bgMidColor = 0xFF2B140C.toInt(),
            bgBottomColor = 0xFF170905.toInt(),
            glowColor = 0x44F4B465.toInt(),
            accentGoldColor = 0xFFF2BF6D.toInt(),
            primaryTextColor = 0xFFFFF7EC.toInt(),
            secondaryTextColor = 0xFFE5C49E.toInt()
        ),
        CardVisualTheme(
            id = "pearl_rose",
            nameAr = "لؤلؤ الورد",
            nameEn = "Pearl Velvet",
            nameUz = "Marvarid atirgul",
            bgTopColor = 0xFF3E1C2E.toInt(),
            bgMidColor = 0xFF291120.toInt(),
            bgBottomColor = 0xFF150812.toInt(),
            glowColor = 0x40F0A6C4.toInt(),
            accentGoldColor = 0xFFEAC282.toInt(),
            primaryTextColor = 0xFFFFF5F9.toInt(),
            secondaryTextColor = 0xFFE5BFD0.toInt()
        ),
        CardVisualTheme(
            id = "obsidian_gold",
            nameAr = "الأوبسيديان الذهبي",
            nameEn = "Obsidian Gold",
            nameUz = "Oltin obsidian",
            bgTopColor = 0xFF1C1F22.toInt(),
            bgMidColor = 0xFF101215.toInt(),
            bgBottomColor = 0xFF07080A.toInt(),
            glowColor = 0x44E3B857.toInt(),
            accentGoldColor = 0xFFE7C167.toInt(),
            primaryTextColor = 0xFFFDFBF5.toInt(),
            secondaryTextColor = 0xFFCFC3A6.toInt()
        )
    )

    val templates: List<IslamicCardTemplate> = listOf(
        // Quran Verses
        IslamicCardTemplate(
            "q1", IslamicCardCategory.QURAN, "إن مع العسر يسرا",
            "﴿ فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴾",
            "سورة الشرح · ٥–٦",
            "For indeed, with hardship will be ease.",
            "Albatta, qiyinchilik bilan birga yengillik bordir."
        ),
        IslamicCardTemplate(
            "q2", IslamicCardCategory.QURAN, "طمأنينة القلوب",
            "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
            "سورة الرعد · ٢٨",
            "Unquestionably, by the remembrance of Allah hearts are assured.",
            "Ogoh bo'lingizkim, Allohni zikr etish bilan qalblar orom olur."
        ),
        IslamicCardTemplate(
            "q3", IslamicCardCategory.QURAN, "مخرجاً ورزقاً",
            "﴿ وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا ۝ وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ﴾",
            "سورة الطلاق · ٢–٣",
            "Whoever fears Allah — He will make for him a way out and provide from where he does not expect.",
            "Kim Allohga taqvo qilsa, U zot unga chiqish yo'lini ochib berur."
        ),
        IslamicCardTemplate(
            "q4", IslamicCardCategory.QURAN, "الدعاء الجامع",
            "﴿ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ ﴾",
            "سورة البقرة · ٢٠١",
            "Our Lord, give us in this world that which is good and in the Hereafter that which is good.",
            "Parvardigoro, bizga bu dunyoda ham, oxiratda ham yaxshilik ato etgin."
        ),
        IslamicCardTemplate(
            "q5", IslamicCardCategory.QURAN, "معية الله",
            "﴿ لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا ﴾",
            "سورة التوبة · ٤٠",
            "Do not grieve; indeed Allah is with us.",
            "G'amgin bo'lma, albatta Alloh biz bilan birgadir."
        ),
        IslamicCardTemplate(
            "q6", IslamicCardCategory.QURAN, "قريب مجيب",
            "﴿ وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ ﴾",
            "سورة البقرة · ١٨٦",
            "Indeed I am near. I respond to the invocation of the supplicant when he calls upon Me.",
            "Albatta Men yaqinman. Menga duo qilganning duosini ijobat qilurman."
        ),

        // Duas
        IslamicCardTemplate(
            "d1", IslamicCardCategory.DUA, "دعاء الثبات",
            "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            "دعاء نبوي شريف",
            "O Turner of hearts, keep my heart steadfast upon Your religion.",
            "Ey qalblarni aylantiruvchi Zot, qalbimni O'z diningda sobit qilgin."
        ),
        IslamicCardTemplate(
            "d2", IslamicCardCategory.DUA, "دعاء العفو",
            "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            "رواه الترمذي وابن ماجه",
            "O Allah, You are Pardoning and love to pardon, so pardon me.",
            "Allohim, albatta Sen afv qiluvchisan, afv etishni yaxshi ko'rasan, meni afv etgin."
        ),
        IslamicCardTemplate(
            "d3", IslamicCardCategory.DUA, "دعاء العلم والرزق",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْماً نَافِعاً، وَرِزْقاً طَيِّباً، وَعَمَلاً مُتَقَبَّلاً",
            "أذكار الصباح",
            "O Allah, I ask You for beneficial knowledge, pure provision, and accepted deeds.",
            "Allohim, Sendan foydali ilm, halol rizq va maqbul amal so'rayman."
        ),
        IslamicCardTemplate(
            "d4", IslamicCardCategory.DUA, "دعاء الوالدين",
            "﴿ رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا ﴾",
            "سورة الإسراء · ٢٤",
            "My Lord, have mercy upon them as they brought me up when I was small.",
            "Parvardigorim, meni go'dakligimda tarbiyalaganlaridek ularga rahm qilgin."
        ),

        // Hadith
        IslamicCardTemplate(
            "h1", IslamicCardCategory.HADITH, "خيركم من تعلم القرآن",
            "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            "رواه البخاري",
            "The best among you are those who learn the Quran and teach it.",
            "Sizlarning eng yaxshingiz Qur'onni o'rgangan va uni o'rgatgandir."
        ),
        IslamicCardTemplate(
            "h2", IslamicCardCategory.HADITH, "أحب الأعمال إلى الله",
            "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            "متفق عليه",
            "The most beloved of deeds to Allah are the most consistent, even if small.",
            "Allohga eng suyukli amal oz bo'lsa ham davomli bo'lganidir."
        ),
        IslamicCardTemplate(
            "h3", IslamicCardCategory.HADITH, "التبسم صدقة",
            "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ",
            "رواه الترمذي",
            "Your smiling in the face of your brother is charity.",
            "Birodaringiz yuziga tabassum bilan qarashingiz ham sadaqadir."
        ),

        // Greetings
        IslamicCardTemplate(
            "g1", IslamicCardCategory.GREETING, "جمعة مباركة",
            "جُمُعَةٌ مُبَارَكَةٌ طَيِّبَةٌ\nاللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            "طابت جمعتكم بالخير والبركة",
            "Blessed Friday — May Allah send peace and blessings upon Prophet Muhammad.",
            "Juma muborak — Payg'ambarimiz Muhammadga salovot va salomlar bo'lsin."
        ),
        IslamicCardTemplate(
            "g2", IslamicCardCategory.GREETING, "رمضان مبارك",
            "رَمَضَانُ مُبَارَكٌ\nتَقَبَّلَ اللَّهُ مِنَّا وَمِنْكُمْ صَالِحَ الأَعْمَالِ",
            "كل عام وأنتم إلى الله أقرب",
            "Ramadan Mubarak — May Allah accept our righteous deeds.",
            "Ramazon muborak — Alloh barcha solih amallarimizni qabul qilsin."
        ),
        IslamicCardTemplate(
            "g3", IslamicCardCategory.GREETING, "عيد مبارك",
            "عِيدٌ سَعِيدٌ مُبَارَكٌ\nأَعَادَهُ اللَّهُ عَلَيْكُمْ بِالْيُمْنِ وَالْبَرَكَاتِ",
            "تقبل الله طاعاتكم",
            "Eid Mubarak — May Allah bring joy and blessings to your home.",
            "Hayit muborak — Xonadoningizga tinchlik va baraka tilaymiz."
        )
    )

    /**
     * Renders a crisp, print-quality high-resolution Bitmap with Islamic geometric filigree,
     * radial celestial light, bundled Amiri Arabic calligraphy, citation badge, and optional watermark.
     */
    fun renderCardBitmap(
        context: Context,
        mainText: String,
        referenceText: String,
        subtitleText: String,
        theme: CardVisualTheme,
        aspectRatio: CardAspectRatio,
        fontScale: Float = 1.0f,
        showWatermark: Boolean = true
    ): Bitmap {
        val w = aspectRatio.widthPx
        val h = aspectRatio.heightPx
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Rich Multi-Stop Vertical Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w * 0.25f, h.toFloat(),
                intArrayOf(theme.bgTopColor, theme.bgMidColor, theme.bgBottomColor),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // 2. Celestial Radial Glow Aura behind calligraphy
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h * 0.44f, w * 0.55f,
                intArrayOf(theme.glowColor, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // 3. Subtle Geometric Star Medallion Watermark in Background
        val geoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentGoldColor
            alpha = 26
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        drawEightPointStar(canvas, w / 2f, h * 0.46f, w * 0.34f, geoPaint)
        drawEightPointStar(canvas, w / 2f, h * 0.46f, w * 0.22f, geoPaint)

        // 4. Luxury Double Golden Ornamental Frame
        val outerMargin = 54f
        val innerMargin = 74f
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentGoldColor
            alpha = 185
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(
            RectF(outerMargin, outerMargin, w - outerMargin, h - outerMargin),
            48f, 48f, framePaint
        )
        framePaint.alpha = 95
        framePaint.strokeWidth = 2f
        canvas.drawRoundRect(
            RectF(innerMargin, innerMargin, w - innerMargin, h - innerMargin),
            36f, 36f, framePaint
        )

        // Corner golden rosette medallions
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentGoldColor
            alpha = 155
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
        }
        val corners = listOf(
            outerMargin + 44f to outerMargin + 44f,
            w - outerMargin - 44f to outerMargin + 44f,
            outerMargin + 44f to h - outerMargin - 44f,
            w - outerMargin - 44f to h - outerMargin - 44f
        )
        corners.forEach { (cx, cy) ->
            drawEightPointStar(canvas, cx, cy, 22f, cornerPaint)
        }

        // Top ornamental Bismillah / Crescent emblem
        val amiriTypeface = runCatching { ResourcesCompat.getFont(context, R.font.amiri) }.getOrNull() ?: Typeface.SERIF
        val cairoTypeface = runCatching { ResourcesCompat.getFont(context, R.font.cairo) }.getOrNull() ?: Typeface.SANS_SERIF

        val topOrnamentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentGoldColor
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = amiriTypeface
        }
        canvas.drawText("✦  ۞  ✦", w / 2f, outerMargin + 108f, topOrnamentPaint)

        // 5. Main Arabic Calligraphy Text (auto-wrapped StaticLayout)
        val textPadding = 136f
        val textWidth = (w - textPadding * 2).toInt().coerceAtLeast(200)
        val clampedScale = fontScale.coerceIn(0.75f, 1.35f)
        val mainPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextColor
            textSize = 68f * clampedScale
            typeface = amiriTypeface
            setShadowLayer(12f, 0f, 4f, Color.argb(90, 0, 0, 0))
        }
        val displayBody = mainText.trim().ifBlank { "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾" }
        val mainLayout = StaticLayout.Builder.obtain(displayBody, 0, displayBody.length, mainPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(16f, 1.15f)
            .build()

        // Optional translation/subtitle
        val cleanSubtitle = subtitleText.trim()
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextColor
            textSize = 31f * clampedScale.coerceAtMost(1.15f)
            typeface = cairoTypeface
        }
        val subLayout = if (cleanSubtitle.isNotEmpty()) {
            StaticLayout.Builder.obtain(cleanSubtitle, 0, cleanSubtitle.length, subPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(8f, 1.15f)
                .build()
        } else null

        val combinedHeight = mainLayout.height + (subLayout?.let { it.height + 42 } ?: 0)
        val startY = ((h - combinedHeight) / 2f).coerceIn(220f, h - 380f)

        canvas.save()
        canvas.translate(textPadding, startY)
        mainLayout.draw(canvas)
        canvas.restore()

        if (subLayout != null) {
            canvas.save()
            canvas.translate(textPadding, startY + mainLayout.height + 42f)
            subLayout.draw(canvas)
            canvas.restore()
        }

        // 6. Golden Divider & Reference Pill
        val refY = (startY + combinedHeight + 76f).coerceAtMost(h - 210f)
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentGoldColor
            alpha = 140
            strokeWidth = 2.5f
        }
        canvas.drawLine(w * 0.32f, refY - 34f, w * 0.68f, refY - 34f, divPaint)

        val cleanRef = referenceText.trim()
        if (cleanRef.isNotEmpty()) {
            val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentGoldColor
                textSize = 34f
                textAlign = Paint.Align.CENTER
                typeface = cairoTypeface
            }
            canvas.drawText(cleanRef, w / 2f, refY + 18f, refPaint)
        }

        // 7. Subtle NOOR Watermark
        if (showWatermark) {
            val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryTextColor
                alpha = 175
                textSize = 26f
                textAlign = Paint.Align.CENTER
                typeface = cairoTypeface
            }
            canvas.drawText("تطبيق نور · NOOR", w / 2f, h - outerMargin - 50f, brandPaint)
        }

        return bitmap
    }

    private fun drawEightPointStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val square1 = Path()
        val square2 = Path()
        for (i in 0 until 4) {
            val a1 = Math.toRadians(i * 90.0 + 45.0)
            val a2 = Math.toRadians(i * 90.0)
            val x1 = cx + radius * cos(a1).toFloat()
            val y1 = cy + radius * sin(a1).toFloat()
            val x2 = cx + radius * cos(a2).toFloat()
            val y2 = cy + radius * sin(a2).toFloat()
            if (i == 0) {
                square1.moveTo(x1, y1)
                square2.moveTo(x2, y2)
            } else {
                square1.lineTo(x1, y1)
                square2.lineTo(x2, y2)
            }
        }
        square1.close()
        square2.close()
        canvas.drawPath(square1, paint)
        canvas.drawPath(square2, paint)
    }

    fun shareCardBitmap(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "noor_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "NOOR"))
    }

    /**
     * Saves PNG card to device Gallery via MediaStore (zero storage permission needed on Android 10+).
     */
    fun saveCardToGallery(context: Context, bitmap: Bitmap): Result<String> = runCatching {
        val fileName = "NOOR_Card_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NOOR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create MediaStore record")
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("Unable to open output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            fileName
        } else {
            val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NOOR").apply { mkdirs() }
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile.name
        }
    }
}
