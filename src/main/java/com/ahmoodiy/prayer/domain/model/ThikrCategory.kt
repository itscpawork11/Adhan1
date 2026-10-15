package com.ahmoodiy.prayer.domain.model

/**
 * Every section of the embedded Hisn al-Muslim dataset.
 * [displayLabel] mirrors the carousel labels of the original web build.
 */
enum class ThikrCategory(
    val id: String,
    val displayLabel: String,
) {
    MORNING("morning", "أذكار الصباح 🌅"),
    EVENING("evening", "أذكار المساء 🌙"),
    TASABEEH("tasabeeh", "تسابيح وفضائل 📿"),
    PRAYER_ATHKAR("prayer_athkar", "أذكار الصلاة 🕌"),
    POST_PRAYER("post_prayer", "بعد الصلاة 🤲"),
    SLEEP("sleep", "النوم والأحلام 🛌"),
    WAKE_UP("wake_up", "الاستيقاظ ☀️"),
    PROPHETS_DUAS("prophets_duas", "أدعية الأنبياء 📜"),
    QURANIC_DUAS("quranic_duas", "الأدعية القرآنية 📖"),
    GENERAL_DUAS("general_duas", "أذكار متفرقة وحياتية 🌿"),
    MOSQUE_ADHAN("mosque_adhan", "المسجد والأذان 🕋"),
    WUDU_HOME_FOOD("wudu_home_food", "الوضوء والمنزل والطعام 🏡"),
    RUQYAH_SHIFA("ruqyah_shifa", "الرقية والشفاء 🛡️");

    companion object {
        fun fromId(id: String?): ThikrCategory? = entries.firstOrNull { it.id.equals(id, true) }

        /** Categories surfaced by the Duas section. */
        val duasCategories: Set<ThikrCategory> = setOf(
            PROPHETS_DUAS,
            QURANIC_DUAS,
            GENERAL_DUAS,
            RUQYAH_SHIFA,
        )
    }
}
