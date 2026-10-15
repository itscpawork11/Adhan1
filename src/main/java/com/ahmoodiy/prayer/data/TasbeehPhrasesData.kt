package com.ahmoodiy.prayer.data

import com.ahmoodiy.prayer.domain.model.TasbeehPhrase

/** The ten default tasbeeh formulas of the digital rosary. */
object TasbeehPhrasesData {

    val defaultPhrases: List<TasbeehPhrase> = listOf(
        TasbeehPhrase(
            id = "tp1",
            arabicText = "سُبْحَانَ اللَّهِ",
            virtue = "تُغرس له نخلة في الجنة وتُحط عنه مائة خطيئة",
            defaultTarget = 33,
        ),
        TasbeehPhrase(
            id = "tp2",
            arabicText = "الْحَمْدُ لِلَّهِ",
            virtue = "أفضل الدعاء الحمد لله، وتملأ ميزان العبد بالحسنات",
            defaultTarget = 33,
        ),
        TasbeehPhrase(
            id = "tp3",
            arabicText = "اللَّهُ أَكْبَرُ",
            virtue = "أحب الكلام إلى الله تعالى، ولها أجر عظيم وتثقيل للموازين",
            defaultTarget = 34,
        ),
        TasbeehPhrase(
            id = "tp4",
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            virtue = "كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن",
            defaultTarget = 100,
        ),
        TasbeehPhrase(
            id = "tp5",
            arabicText = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            virtue = "كانت له عدل عشر رقاب، وكُتبت له مائة حسنة ومُحيت عنه مائة سيئة وكانت له حرزاً من الشيطان",
            defaultTarget = 100,
        ),
        TasbeehPhrase(
            id = "tp6",
            arabicText = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            virtue = "يفتح الله به الأبواب المغلقة ويشرح الصدور وينزل به الغيث والبركات",
            defaultTarget = 100,
        ),
        TasbeehPhrase(
            id = "tp7",
            arabicText = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
            virtue = "كنز من كنوز الجنة، ودواء لتسعة وتسعين داء أيسرها الهم",
            defaultTarget = 100,
        ),
        TasbeehPhrase(
            id = "tp8",
            arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            virtue = "من صلى عليّ صلاة صلى الله عليه بها عشراً، وحُطت عنه عشر خطيئات، ورُفع عشر درجات",
            defaultTarget = 100,
        ),
        TasbeehPhrase(
            id = "tp9",
            arabicText = "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            virtue = "دعوة ذي النون في بطن الحوت؛ لم يدعُ بها رجل مسلم في شيء قط إلا استجاب الله له",
            defaultTarget = 40,
        ),
        TasbeehPhrase(
            id = "tp10",
            arabicText = "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ",
            virtue = "أحب الكلام إلى الله، والباقيات الصالحات التي تنفض الذنوب كما تنفض الشجرة ورقها",
            defaultTarget = 33,
        ),
    )

    /** Target presets offered under the rosary. */
    val targetPresets: List<Int> = listOf(33, 99, 100, 1000)
}
