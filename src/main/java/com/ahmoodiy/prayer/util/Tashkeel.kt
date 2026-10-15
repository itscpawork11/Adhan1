package com.ahmoodiy.prayer.util

/**
 * Strips the Arabic diacritics (tashkeel) so the reader can display the text
 * without vocalisation when the user disables it in Settings.
 */
object Tashkeel {

    private val TASHKEEL_AND_TATWEEL = Regex("[\\u064B-\\u0652\\u0670\\u0640]")

    fun strip(text: String?): String = text?.replace(TASHKEEL_AND_TATWEEL, "").orEmpty()

    fun apply(text: String?, showTashkeel: Boolean): String =
        if (showTashkeel) text.orEmpty() else strip(text)

    /** Builds the share/copy payload used by the athkar and duas screens. */
    fun shareText(
        text: String,
        title: String? = null,
        source: String? = null,
        virtue: String? = null,
        withAppName: Boolean = true,
        showTashkeel: Boolean = true,
    ): String {
        val body = apply(text, showTashkeel)
        return buildList {
            if (!title.isNullOrBlank()) add("【 $title 】")
            add(body)
            if (!virtue.isNullOrBlank()) add("• الفضل: $virtue")
            if (!source.isNullOrBlank()) add("• المصدر: $source")
            if (withAppName) add("— تم النسخ من: تطبيق مواقيت الصلاة والمسبحة وحصن المسلم (مصر)")
        }.joinToString("\n\n")
    }
}
