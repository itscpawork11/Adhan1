package com.ahmoodiy.prayer.domain.model

/**
 * The five Arabic typefaces selectable by the user.
 * Rendering (and the downloadable Google font name) lives in the UI layer.
 */
enum class ArabicFont(
    val id: String,
    val displayName: String,
    val badge: String,
    val description: String,
) {
    AMIRI(
        id = "amiri",
        displayName = "خط المصحف الشريف (الأميري)",
        badge = "كلاسيكي قرآني",
        description = "خط النسخ الكلاسيكي الفاخر المعتمد في طباعة المصاحف الشريفة ونصوص السنة النبوية",
    ),
    RUQAA(
        id = "ruqaa",
        displayName = "خط رقعة أصيل (Aref Ruqaa)",
        badge = "فن الخط العربي",
        description = "جمال الخط اليدوي العربي الأصيل بحركات رشيقة وانسيابية عالية",
    ),
    KUFI(
        id = "kufi",
        displayName = "خط الكوفي الحديث (Noto Kufi)",
        badge = "هندسي فاخر",
        description = "طابع معماري إسلامي حديث يتسم بالقوة والوضوح والأناقة المعاصرة",
    ),
    TAJAWAL(
        id = "tajawal",
        displayName = "خط تجوال الرقمي (Tajawal)",
        badge = "فائق الوضوح",
        description = "أفضل خط لواجهات الهواتف والشاشات الصغيرة لقراءة سريعة ومريحة للعين",
    ),
    CAIRO(
        id = "cairo",
        displayName = "خط القاهرة الحديث (Cairo)",
        badge = "عصري متزن",
        description = "خط عصري متزن ذو وضوح هندسي ممتاز للأرقام والنصوص اليومية",
    );

    companion object {
        val ordered: List<ArabicFont> = entries.toList()

        fun fromId(id: String?): ArabicFont = entries.firstOrNull { it.id.equals(id, true) } ?: AMIRI
    }
}
