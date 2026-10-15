package com.ahmoodiy.prayer.data

import com.ahmoodiy.prayer.domain.model.Governorate

/** Coordinates transcribed 1:1 from the original web build. */
object GovernoratesData {

    val all: List<Governorate> = listOf(
        Governorate("cairo", "القاهرة", "Cairo", 30.0444, 31.2357),
        Governorate("alexandria", "الإسكندرية", "Alexandria", 31.2001, 29.9187),
        Governorate("giza", "الجيزة", "Giza", 30.0131, 31.2089),
        Governorate("dakahlia", "الدقهلية (المنصورة)", "Dakahlia", 31.0409, 31.3785),
        Governorate("sharqia", "الشرقية (الزقازيق)", "Sharqia", 30.5877, 31.5020),
        Governorate("gharbia", "الغربية (طنطا)", "Gharbia", 30.7865, 31.0004),
        Governorate("qalyubia", "القليوبية (بنها)", "Qalyubia", 30.4660, 31.1853),
        Governorate("monufia", "المنوفية (شبين الكوم)", "Monufia", 30.5593, 30.9996),
        Governorate("beheira", "البحيرة (دمنهور)", "Beheira", 31.0364, 30.4689),
        Governorate("port_said", "بورسعيد", "Port Said", 31.2653, 32.3019),
        Governorate("suez", "السويس", "Suez", 29.9668, 32.5498),
        Governorate("ismailia", "الإسماعيلية", "Ismailia", 30.5965, 32.2715),
        Governorate("damietta", "دمياط", "Damietta", 31.4175, 31.8144),
        Governorate("kafr_el_sheikh", "كفر الشيخ", "Kafr El Sheikh", 31.1107, 30.9388),
        Governorate("faiyum", "الفيوم", "Faiyum", 29.3084, 30.8428),
        Governorate("beni_suef", "بني سويف", "Beni Suef", 29.0661, 31.0994),
        Governorate("minya", "المنيا", "Minya", 28.0871, 30.7618),
        Governorate("asyut", "أسيوط", "Asyut", 27.1783, 31.1859),
        Governorate("sohag", "سوهاج", "Sohag", 26.5590, 31.6957),
        Governorate("qena", "قنا", "Qena", 26.1551, 32.7160),
        Governorate("luxor", "الأقصر", "Luxor", 25.6872, 32.6396),
        Governorate("aswan", "أسوان", "Aswan", 24.0889, 32.8998),
        Governorate("red_sea", "البحر الأحمر (الغردقة)", "Red Sea", 27.2579, 33.8116),
        Governorate("south_sinai", "جنوب سيناء (شرم الشيخ)", "South Sinai", 27.9158, 34.3299),
        Governorate("north_sinai", "شمال سيناء (العريش)", "North Sinai", 31.1316, 33.7984),
        Governorate("matrouh", "مطروح", "Matrouh", 31.3543, 27.2373),
        Governorate("new_valley", "الوادي الجديد (الخارجة)", "New Valley", 25.4514, 30.5464),
    )

    val default: Governorate = all.first()

    fun byId(id: String?): Governorate = all.firstOrNull { it.id == id } ?: default
}
