package com.ahmoodiy.prayer.domain.model

/**
 * Selectable adhan recitation / notification tone.
 *
 * Most voices ship as real, bundled recordings (see `res/raw`); [TAKBEERAT] is synthesised on
 * device and [CUSTOM] plays a file the user imports from the phone. [shortLabel] is the compact
 * chip label, [displayName] the full title and [maqam] a short musical hint.
 */
enum class AdhanVoice(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val maqam: String,
) {
    MAKKAH(
        id = "makkah",
        displayName = "أذان الحرم المكي الشريف",
        shortLabel = "الحرم المكي",
        description = "صوت مؤذني المسجد الحرام الشريف المهيب بنبرة حجازية روحانية",
        maqam = "مقام الحجاز",
    ),
    MADINAH(
        id = "madinah",
        displayName = "أذان الحرم المدني (المسجد النبوي)",
        shortLabel = "الحرم المدني",
        description = "أذان المسجد النبوي الشريف بنبرة هادئة خاشعة تطمئن لها القلوب",
        maqam = "مقام البياتي",
    ),
    AQSA(
        id = "aqsa",
        displayName = "أذان المسجد الأقصى المبارك",
        shortLabel = "المسجد الأقصى",
        description = "أذان من رحاب المسجد الأقصى المبارك بقدسية ونبرة مؤثرة",
        maqam = "مقام الراست",
    ),
    EGYPT(
        id = "egypt",
        displayName = "أذان مصر (إذاعة القرآن الكريم)",
        shortLabel = "أذان مصر",
        description = "الأذان المصري التاريخي الأصيل المحبب لجميع محافظات مصر",
        maqam = "مقام الراست المصري",
    ),
    ABDULBASIT(
        id = "abdulbasit",
        displayName = "الشيخ عبد الباسط عبد الصمد",
        shortLabel = "عبد الباسط",
        description = "صوت الشيخ عبد الباسط عبد الصمد الخاشع المهيب الذي يأسر القلوب",
        maqam = "مقام البياتي",
    ),
    MINSHAWI(
        id = "minshawi",
        displayName = "الشيخ محمد صديق المنشاوي",
        shortLabel = "المنشاوي",
        description = "أذان خاشع بنبرة الشيخ المنشاوي المتأنية النافذة إلى القلب",
        maqam = "مقام البياتي",
    ),
    HOSSARY(
        id = "hossary",
        displayName = "الشيخ محمود خليل الحصري",
        shortLabel = "الحصري",
        description = "أذان متميّز بترتيل الشيخ الحصري الأصيل الوقور",
        maqam = "مقام البياتي",
    ),
    ALAFASY(
        id = "alafasy",
        displayName = "الشيخ مشاري راشد العفاسي",
        shortLabel = "مشاري العفاسي",
        description = "أذان عذب نديّ بصوت الشيخ مشاري العفاسي",
        maqam = "مقام الحجاز",
    ),
    QATAMI(
        id = "qatami",
        displayName = "الشيخ ناصر القطامي",
        shortLabel = "ناصر القطامي",
        description = "أذان مؤثّر خاشع بصوت الشيخ ناصر القطامي",
        maqam = "مقام البياتي",
    ),
    YUSUF_ISLAM(
        id = "yusuf_islam",
        displayName = "يوسف إسلام (كات ستيفنز)",
        shortLabel = "يوسف إسلام",
        description = "أذان بصوت الفنان العالمي يوسف إسلام بنكهة عالمية مميزة",
        maqam = "نغمة عالمية",
    ),
    TAKBEERAT(
        id = "takbeerat",
        displayName = "تكبيرات الأذان فقط",
        shortLabel = "تكبيرات",
        description = "نغمة تنبيه لطيفة تقتصر على التكبيرات الأولى للأذان (مولّدة داخل التطبيق)",
        maqam = "نغم مختصر",
    ),
    CUSTOM(
        id = "custom",
        displayName = "ملف صوتي يدوي من الهاتف",
        shortLabel = "ملف مخصص",
        description = "اختر أي ملف أذان مخصص بصيغة MP3 أو M4A أو WAV من ملفات هاتفك",
        maqam = "ملف محلي مخصص",
    );

    companion object {
        val ordered: List<AdhanVoice> = entries.toList()

        /** Voices that play a real bundled recording (no synthesis, fully offline). */
        val withBundledAudio: List<AdhanVoice> = entries.filter {
            it != AdhanVoice.TAKBEERAT && it != AdhanVoice.CUSTOM
        }

        /** Synthesised and user-provided alternatives, shown after the bundled recordings. */
        val others: List<AdhanVoice> = entries.filter {
            it == AdhanVoice.TAKBEERAT || it == AdhanVoice.CUSTOM
        }

        fun fromId(id: String?): AdhanVoice = entries.firstOrNull { it.id.equals(id, true) } ?: ABDULBASIT
    }
}
