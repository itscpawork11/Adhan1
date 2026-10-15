package com.ahmoodiy.prayer.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.data.GovernoratesData
import com.ahmoodiy.prayer.data.TasbeehPhrasesData
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ArabicFont
import com.ahmoodiy.prayer.domain.model.CustomAudioMeta
import com.ahmoodiy.prayer.domain.model.FontScale
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import com.ahmoodiy.prayer.ui.components.CaptionText
import com.ahmoodiy.prayer.ui.components.GoldCard
import com.ahmoodiy.prayer.ui.components.GoldConfirmDialog
import com.ahmoodiy.prayer.ui.components.GoldInfoDialog
import com.ahmoodiy.prayer.ui.components.MetaChip
import com.ahmoodiy.prayer.ui.components.PillRow
import com.ahmoodiy.prayer.ui.components.ScreenHeader
import com.ahmoodiy.prayer.ui.components.SectionHeader
import com.ahmoodiy.prayer.ui.components.SubtleSurface
import com.ahmoodiy.prayer.ui.components.ToggleRow
import com.ahmoodiy.prayer.ui.theme.DangerRose
import com.ahmoodiy.prayer.ui.theme.EmeraldCompleted
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.BackupManager
import com.ahmoodiy.prayer.util.resolveFontFamily
import java.util.Locale

/**
 * The Settings (الإعدادات) screen.
 *
 * A vertical scroll of gold cards covering appearance, the rosary, prayer alerts & adhan,
 * duas / sharing, backup & restore, and an "about" panel. Everything the user changes is written
 * straight through to [com.ahmoodiy.prayer.data.prefs.SettingsStore] by [SettingsViewModel], and any
 * change that affects alarm scheduling re-registers the prayer alarms.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current

    val version: String = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    // --- Storage Access Framework launchers ------------------------------------------------

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importBackup(uri) }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val displayName = queryDisplayName(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            viewModel.importCustomAdhan(uri, displayName, mimeType)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "الإعدادات",
            subtitle = "دار النور الرقمية · إنشاء أبو الريش",
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            AppearanceSection(settings = settings, viewModel = viewModel)
            Spacer(Modifier.height(Spacing.md))

            RosarySection(settings = settings, viewModel = viewModel)
            Spacer(Modifier.height(Spacing.md))

            AlertsSection(
                settings = settings,
                state = state,
                viewModel = viewModel,
                onPickCustomAudio = { customAudioLauncher.launch(arrayOf("audio/*")) },
            )
            Spacer(Modifier.height(Spacing.md))

            DuasSection(settings = settings, viewModel = viewModel)
            Spacer(Modifier.height(Spacing.md))

            BackupSection(
                onExport = { exportLauncher.launch(BackupManager.FILE_NAME) },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onResetStats = viewModel::resetLifetimeStats,
                onResetAll = viewModel::requestResetAll,
            )
            Spacer(Modifier.height(Spacing.md))

            AboutSection(version = version)
            Spacer(Modifier.height(Spacing.lg))
        }
    }

    state.message?.let { message ->
        GoldInfoDialog(
            title = if (state.messageIsError) "تعذر إكمال العملية" else "تم بنجاح",
            onDismiss = viewModel::dismissMessage,
        ) {
            Text(text = message, color = TextPrimary, fontSize = 13.sp)
        }
    }

    if (state.showResetDialog) {
        GoldConfirmDialog(
            title = "مسح كل البيانات",
            message = "سيتم حذف جميع الإعدادات والإحصائيات والأذكار المخصصة وملف الأذان نهائياً، ولا يمكن التراجع عن ذلك. هل تريد المتابعة؟",
            confirmLabel = "مسح الكل",
            destructive = true,
            onConfirm = viewModel::confirmResetAll,
            onDismiss = viewModel::dismissResetDialog,
        )
    }
}

// ---------------------------------------------------------------------------- appearance

@Composable
private fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel) {
    GoldCard {
        SectionHeader(title = "المظهر والخط", subtitle = "اختر الخط وحجم القراءة")
        Spacer(Modifier.height(Spacing.sm))

        CaptionText("نوع الخط")
        PillRow(
            items = ArabicFont.ordered,
            selectedItem = settings.font,
            labelOf = { it.displayName },
            onSelect = viewModel::setFont,
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${settings.font.badge} · ${settings.font.description}",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            text = "سبحان الله وبحمده",
            fontFamily = settings.font.resolveFontFamily(),
            fontSize = 20.sp,
            color = GoldShimmer,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        Spacer(Modifier.height(Spacing.md))
        CaptionText("حجم الخط")
        PillRow(
            items = FontScale.ordered,
            selectedItem = settings.fontScale,
            labelOf = { it.label },
            onSelect = viewModel::setFontScale,
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "معاينة حجم القراءة المختار",
            color = TextPrimary,
            fontSize = 18.sp * settings.fontScale.multiplier,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        Spacer(Modifier.height(Spacing.sm))
        ToggleRow(
            title = "إظهار التشكيل",
            description = "عرض حركات التشكيل على نصوص الأذكار والأدعية",
            checked = settings.showTashkeel,
            onCheckedChange = viewModel::setShowTashkeel,
        )
        ToggleRow(
            title = "إظهار الفضل والسند",
            description = "عرض فضل كل ذكر ومرجعه أسفل النص",
            checked = settings.showSanadAndVirtue,
            onCheckedChange = viewModel::setShowSanadAndVirtue,
        )
        ToggleRow(
            title = "إخفاء الأذكار المكتملة تلقائياً",
            description = "إخفاء الذكر من القائمة بعد إتمام عدده",
            checked = settings.autoHideCompletedThikr,
            onCheckedChange = viewModel::setAutoHideCompletedThikr,
        )
    }
}

// ---------------------------------------------------------------------------- rosary

@Composable
private fun RosarySection(settings: AppSettings, viewModel: SettingsViewModel) {
    GoldCard {
        SectionHeader(title = "المسبحة", subtitle = "الاهتزاز والصوت والهدف الافتراضي")
        Spacer(Modifier.height(Spacing.xs))

        ToggleRow(
            title = "اهتزاز عند كل تسبيحة",
            checked = settings.tasbeehHapticOnTap,
            onCheckedChange = viewModel::setHapticOnTap,
        )
        ToggleRow(
            title = "اهتزاز مميز عند إتمام الدور",
            checked = settings.tasbeehLongHapticOnCycle,
            onCheckedChange = viewModel::setLongHapticOnCycle,
        )
        ToggleRow(
            title = "صوت النقر",
            checked = settings.tasbeehSoundEnabled,
            onCheckedChange = viewModel::setSoundEnabled,
        )

        Spacer(Modifier.height(Spacing.sm))
        CaptionText("نوع صوت النقر")
        PillRow(
            items = TasbeehSoundType.ordered,
            selectedItem = settings.tasbeehSoundType,
            labelOf = { it.displayName },
            onSelect = viewModel::setSoundType,
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.sm))
        ToggleRow(
            title = "حماية من التصفير بالخطأ",
            description = "طلب تأكيد قبل تصفير العدّاد الحالي",
            checked = settings.tasbeehResetProtection,
            onCheckedChange = viewModel::setResetProtection,
        )

        Spacer(Modifier.height(Spacing.sm))
        CaptionText("الهدف الافتراضي للجولة الجديدة")
        PillRow(
            items = TasbeehPhrasesData.targetPresets,
            selectedItem = settings.tasbeehDefaultTarget,
            labelOf = { it.toString() },
            onSelect = viewModel::setDefaultTarget,
            keyOf = { it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------- alerts & adhan

@Composable
private fun AlertsSection(
    settings: AppSettings,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onPickCustomAudio: () -> Unit,
) {
    GoldCard {
        SectionHeader(title = "التنبيهات والأذان", subtitle = "المواقيت والمحافظة وصوت الأذان")
        Spacer(Modifier.height(Spacing.xs))

        ToggleRow(
            title = "تنبيه الفجر",
            description = "تشغيل تنبيه صلاة الفجر في موعده",
            checked = settings.fajrAlertEnabled,
            onCheckedChange = viewModel::setFajrAlertEnabled,
        )

        Spacer(Modifier.height(Spacing.sm))
        CaptionText("المحافظة")
        PillRow(
            items = GovernoratesData.all,
            selectedItem = GovernoratesData.byId(settings.governorateId),
            labelOf = { it.arabicName },
            onSelect = { viewModel.setGovernorate(it.id) },
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))
        TimingStepper(
            minutes = settings.prayerAlertTiming,
            onChange = viewModel::setAlertTiming,
        )

        Spacer(Modifier.height(Spacing.md))
        CaptionText("الأذانات المشهورة · مكتبة داخل التطبيق وتعمل بدون إنترنت")
        AdhanVoice.withBundledAudio.forEach { voice ->
            VoiceRow(
                voice = voice,
                selected = settings.adhanVoice == voice,
                playing = state.playingVoice == voice,
                previewEnabled = true,
                onSelect = { viewModel.selectVoice(voice) },
                onPreview = { viewModel.toggleVoicePreview(voice) },
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        CaptionText("خيارات أخرى")
        AdhanVoice.others.forEach { voice ->
            VoiceRow(
                voice = voice,
                selected = settings.adhanVoice == voice,
                playing = state.playingVoice == voice,
                previewEnabled = voice != AdhanVoice.CUSTOM || settings.customAudioMeta != null,
                onSelect = { viewModel.selectVoice(voice) },
                onPreview = { viewModel.toggleVoicePreview(voice) },
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        CustomAudioRow(
            meta = settings.customAudioMeta,
            onPick = onPickCustomAudio,
            onRemove = viewModel::removeCustomAdhan,
        )

        Spacer(Modifier.height(Spacing.sm))
        ToggleRow(
            title = "تنبيه يومي للأذكار",
            description = "تذكير صباحي ومسائي بأذكار الصباح والمساء",
            checked = settings.dailyReminderEnabled,
            onCheckedChange = viewModel::setDailyReminderEnabled,
        )
        ReminderTimeRow(
            title = "تنبيه أذكار الصباح",
            time = settings.morningReminderTime,
            enabled = settings.dailyReminderEnabled,
            onPick = viewModel::setMorningReminderTime,
        )
        ReminderTimeRow(
            title = "تنبيه أذكار المساء",
            time = settings.eveningReminderTime,
            enabled = settings.dailyReminderEnabled,
            onPick = viewModel::setEveningReminderTime,
        )
    }
}

/** A selectable adhan-voice row with name, description, maqam badge and an inline preview button. */
@Composable
private fun VoiceRow(
    voice: AdhanVoice,
    selected: Boolean,
    playing: Boolean,
    previewEnabled: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(Radius.md),
        color = if (selected) EmeraldCompleted else EmeraldSurface,
        border = BorderStroke(Spacing.hairline, if (selected) GoldPrimary else EmeraldOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voice.displayName,
                    color = if (selected) GoldShimmer else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = voice.description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                MetaChip(text = voice.maqam, modifier = Modifier.padding(top = Spacing.xs))
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "الصوت المحدد",
                    tint = GoldPrimary,
                    modifier = Modifier
                        .padding(start = Spacing.xs)
                        .size(18.dp),
                )
            }
            IconButton(onClick = onPreview, enabled = previewEnabled) {
                Icon(
                    imageVector = if (playing) Icons.Filled.VolumeOff else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "إيقاف المعاينة" else "معاينة الصوت",
                    tint = if (previewEnabled) GoldPrimary else TextMuted,
                )
            }
        }
    }
}

/** Pre-adhan reminder window control: − / value / + with the bound enforced by the ViewModel. */
@Composable
private fun TimingStepper(minutes: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "التنبيه المسبق",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "ينبّهك قبل الأذان بهذا القدر من الدقائق",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val canDecrease = minutes > SettingsViewModel.MIN_ALERT_MINUTES
        val canIncrease = minutes < SettingsViewModel.MAX_ALERT_MINUTES
        IconButton(onClick = { onChange(minutes - 1) }, enabled = canDecrease) {
            Text(
                text = "−",
                color = if (canDecrease) GoldPrimary else TextMuted,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "$minutes دقيقة",
            color = GoldShimmer,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(66.dp),
        )
        IconButton(onClick = { onChange(minutes + 1) }, enabled = canIncrease) {
            Text(
                text = "+",
                color = if (canIncrease) GoldPrimary else TextMuted,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Shows the imported custom adhan file (or an empty hint) with pick / replace / delete actions. */
@Composable
private fun CustomAudioRow(
    meta: CustomAudioMeta?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(
            text = "ملف أذان مخصص من الهاتف",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.xs))

        if (meta == null) {
            Text(
                text = "لم يتم اختيار ملف بعد · الصيغ المدعومة: MP3 أو M4A أو WAV",
                color = TextMuted,
                fontSize = 11.sp,
            )
        } else {
            SubtleSurface(modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.sm) {
                Text(
                    text = meta.name,
                    color = GoldShimmer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = buildString {
                        append(formatBytes(meta.size))
                        meta.durationMs?.let { append(" · ${it / 1000} ثانية") }
                        append(" · ${meta.type}")
                    },
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPick) {
                Text(
                    text = if (meta == null) "اختيار ملف" else "استبدال الملف",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            if (meta != null) {
                TextButton(onClick = onRemove) {
                    Text(
                        text = "حذف الملف",
                        color = DangerRose,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/** A daily-reminder time row backed by the platform [TimePickerDialog]. */
@Composable
private fun ReminderTimeRow(
    title: String,
    time: String,
    enabled: Boolean,
    onPick: (String) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = time,
                color = if (enabled) GoldShimmer else TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TextButton(
            onClick = {
                val hour = time.substringBefore(':').toIntOrNull() ?: 0
                val minute = time.substringAfter(':').toIntOrNull() ?: 0
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        onPick(formatTime(selectedHour, selectedMinute))
                    },
                    hour,
                    minute,
                    true,
                ).show()
            },
            enabled = enabled,
        ) {
            Text(
                text = "تعديل الوقت",
                color = if (enabled) GoldPrimary else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------- duas

@Composable
private fun DuasSection(settings: AppSettings, viewModel: SettingsViewModel) {
    GoldCard {
        SectionHeader(title = "الأدعية والمشاركة", subtitle = "تنسيق النصوص عند المشاركة")
        Spacer(Modifier.height(Spacing.xs))
        ToggleRow(
            title = "إضافة اسم التطبيق عند المشاركة",
            description = "يُضاف اسم التطبيق ودار النشر أسفل الدعاء عند مشاركته",
            checked = settings.shareFormatWithAppName,
            onCheckedChange = viewModel::setShareFormatWithAppName,
        )
    }
}

// ---------------------------------------------------------------------------- backup & restore

@Composable
private fun BackupSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetStats: () -> Unit,
    onResetAll: () -> Unit,
) {
    GoldCard {
        SectionHeader(
            title = "النسخ الاحتياطي والاستعادة",
            subtitle = "احفظ إعداداتك وإحصائياتك في ملف JSON واستعدها لاحقاً",
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(
                onClick = onExport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = EmeraldDeep,
                ),
            ) {
                Text(text = "تصدير نسخة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                border = BorderStroke(Spacing.hairline, GoldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
            ) {
                Text(text = "استعادة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        SectionDivider()

        ActionRow(
            title = "إعادة تعيين إحصائيات المسبحة",
            description = "تصفير العدّاد اليومي والإجمالي والأدوار مع الإبقاء على الإعدادات",
            icon = Icons.Filled.RestartAlt,
            onClick = onResetStats,
        )
        ActionRow(
            title = "مسح كل البيانات",
            description = "حذف جميع الإعدادات والإحصائيات والأذكار المخصصة وملف الأذان نهائياً",
            icon = Icons.Filled.Delete,
            danger = true,
            onClick = onResetAll,
        )
    }
}

/** Tappable row used for the destructive / maintenance actions. */
@Composable
private fun ActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) DangerRose else GoldPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text(
                text = title,
                color = if (danger) DangerRose else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** A hairline rule used to separate the maintenance actions inside the backup card. */
@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md)
            .height(Spacing.hairline)
            .background(EmeraldOutline),
    )
}

// ---------------------------------------------------------------------------- about

@Composable
private fun AboutSection(version: String) {
    SubtleSurface(modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.lg) {
        Text(
            text = "المسبحة الذكية ومواقيت صلاة مصر",
            color = GoldShimmer,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "الإصدار $version",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "يعمل التطبيق دون إنترنت · لا يجمع أي بيانات",
            color = GoldPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "طريقة الحساب: الهيئة المصرية العامة للمساحة — الفجر 19.5° والعشاء 17.5°، مع مراعاة التوقيت الصيفي.",
            color = TextPrimary,
            fontSize = 11.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "دار النور الرقمية · إنشاء أبو الريش",
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}

// ---------------------------------------------------------------------------- helpers

/**
 * Resolves a human-readable file name for a Storage-Access-Framework [uri] by querying
 * [OpenableColumns.DISPLAY_NAME]. Falls back to "custom_adhan" when the provider cannot answer.
 */
private fun queryDisplayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    }
}.getOrNull() ?: "custom_adhan"

/** Formats a byte count as KB / MB for the custom-audio summary line. */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format(Locale.US, "%.0f KB", kb)
    } else {
        String.format(Locale.US, "%.1f MB", kb / 1024.0)
    }
}

/** Formats a 24-hour time as a zero-padded "HH:mm" string. */
private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)
