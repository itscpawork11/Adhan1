package com.ahmoodiy.prayer.ui.prayers

import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.data.GovernoratesData
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.Governorate
import com.ahmoodiy.prayer.domain.model.PrayerKey
import com.ahmoodiy.prayer.domain.model.PrayerNotificationMode
import com.ahmoodiy.prayer.ui.components.GoldCard
import com.ahmoodiy.prayer.ui.components.GoldInfoDialog
import com.ahmoodiy.prayer.ui.components.MetaChip
import com.ahmoodiy.prayer.ui.components.PillRow
import com.ahmoodiy.prayer.ui.components.ScreenHeader
import com.ahmoodiy.prayer.ui.components.SectionHeader
import com.ahmoodiy.prayer.ui.components.SubtleSurface
import com.ahmoodiy.prayer.ui.theme.DangerRose
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldMuted
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.ArabicTime
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "مواقيت الصلاة" screen: live countdown to the next prayer, the full daily timetable with
 * per-prayer alert modes and manual offsets, adhan voice selection and alert timing.
 */
@Composable
fun PrayersScreen(modifier: Modifier = Modifier) {
    val viewModel: PrayersViewModel = viewModel(factory = PrayersViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scale = state.settings.fontScale.multiplier

    // Once-per-second ticker so every countdown on the screen stays live.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            viewModel.tick()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "مواقيت الصلاة في مصر",
            subtitle = "الهيئة المصرية العامة للمساحة · فجر 19.5° وعشاء 17.5°",
            actions = {
                IconButton(onClick = viewModel::showMethodInfo) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "طريقة الحساب",
                        tint = GoldPrimary,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            GovernorateSelector(
                selected = state.governorate,
                onSelect = viewModel::selectGovernorate,
            )

            Spacer(Modifier.height(Spacing.md))

            NextPrayerHero(state = state, scale = scale)

            Spacer(Modifier.height(Spacing.md))

            PrayerList(
                state = state,
                onCycleMode = viewModel::cycleMode,
                onAdjustOffset = viewModel::adjustOffset,
            )

            Spacer(Modifier.height(Spacing.md))

            AdhanVoiceSection(
                current = state.settings.adhanVoice,
                onSelect = viewModel::selectVoice,
            )

            Spacer(Modifier.height(Spacing.md))

            AlertFooter(
                alertTiming = state.settings.prayerAlertTiming,
                fajrEnabled = state.settings.fajrAlertEnabled,
                onAlertTimingChange = viewModel::setAlertTiming,
                onToggleFajr = viewModel::toggleFajrAlert,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(Spacing.md))
                NotificationPermissionNote()
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }

    if (state.showMethodInfo) {
        GoldInfoDialog(
            title = "طريقة حساب المواقيت والتوقيت الصيفي",
            onDismiss = viewModel::dismissMethodInfo,
        ) {
            MethodInfoContent()
        }
    }
}

// --------------------------------------------------------------------------- governorate

/** Governorate picker plus the currently selected coordinates. */
@Composable
private fun GovernorateSelector(
    selected: Governorate,
    onSelect: (String) -> Unit,
) {
    GoldCard(contentPadding = Spacing.lg) {
        SectionHeader(
            title = "المحافظة",
            subtitle = "اختر محافظتك لضبط خطوط الطول والعرض",
        )
        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = selected.arabicName,
            color = GoldShimmer,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = selected.englishName,
            color = TextMuted,
            fontSize = 11.sp,
        )
        Text(
            text = String.format(Locale.US, "%.4f, %.4f", selected.lat, selected.lng),
            color = GoldMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Spacing.sm))

        PillRow(
            items = GovernoratesData.all,
            selectedItem = selected,
            labelOf = { it.arabicName },
            onSelect = { onSelect(it.id) },
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --------------------------------------------------------------------------- hero card

/** The highlighted "next prayer" card with the live countdown and the current clock. */
@Composable
private fun NextPrayerHero(state: PrayersUiState, scale: Float) {
    val countdown = ArabicTime.countdownUntil(state.nextMillis, state.nowMillis)

    GoldCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = true,
        contentPadding = Spacing.lg,
    ) {
        SectionHeader(
            title = "الصلاة القادمة",
            subtitle = "الوقت المتبقي حتى الأذان",
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = state.nextKey.arabicName,
                    color = GoldShimmer,
                    fontSize = 26.sp * scale,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = ArabicTime.formatTime(state.nextMillis),
                    color = TextPrimary,
                    fontSize = 15.sp * scale,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (countdown.isExpired) "الآن" else countdown.compact,
                    color = if (countdown.isExpired) DangerRose else GoldShimmer,
                    fontSize = 30.sp * scale,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "ساعات : دقائق : ثوانٍ",
                    color = TextMuted,
                    fontSize = 9.sp,
                )
            }
        }

        if (!countdown.isExpired) {
            val totalMinutes = countdown.totalSeconds / 60
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = friendlyRemaining(totalMinutes / 60, totalMinutes % 60),
                color = GoldMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = "الوقت الآن", color = TextMuted, fontSize = 10.sp)
                Text(
                    text = ArabicTime.formatTime(state.nowMillis),
                    color = GoldPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "التاريخ", color = TextMuted, fontSize = 10.sp)
                Text(
                    text = gregorianDate(state.nowMillis),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                hijriDate(state.nowMillis)?.let { hijri ->
                    Text(
                        text = hijri,
                        color = GoldMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------- timetable

/** The six daily moments, each as its own card (الشروق rendered as a subtle marker). */
@Composable
private fun PrayerList(
    state: PrayersUiState,
    onCycleMode: (PrayerKey) -> Unit,
    onAdjustOffset: (PrayerKey, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.ordered.forEach { key ->
            val time = state.millisFor(key)
            val isCurrent = key == state.currentKey
            val isNext = state.isNext(key)
            val status = statusLabel(key, time, state)
            val offset = state.settings.prayerOffsets.minutesFor(key)

            if (key == PrayerKey.SUNRISE) {
                SunriseRow(
                    time = time,
                    status = status,
                    offset = offset,
                    onAdjustOffset = onAdjustOffset,
                )
            } else {
                PrayerRow(
                    key = key,
                    time = time,
                    status = status,
                    isCurrent = isCurrent,
                    isNext = isNext,
                    offset = offset,
                    mode = state.settings.modeFor(key),
                    showFajrNote = key == PrayerKey.FAJR && !state.settings.fajrAlertEnabled,
                    onCycleMode = onCycleMode,
                    onAdjustOffset = onAdjustOffset,
                )
            }
        }
    }
}

/** One prayer row: name + time, live status, offset stepper and the alert-mode control. */
@Composable
private fun PrayerRow(
    key: PrayerKey,
    time: Long,
    status: String,
    isCurrent: Boolean,
    isNext: Boolean,
    offset: Int,
    mode: PrayerNotificationMode,
    showFajrNote: Boolean,
    onCycleMode: (PrayerKey) -> Unit,
    onAdjustOffset: (PrayerKey, Int) -> Unit,
) {
    val emphasised = isCurrent || isNext

    GoldCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = emphasised,
        contentPadding = Spacing.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = key.arabicName,
                        color = if (emphasised) GoldShimmer else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    MetaChip(text = key.englishName)
                    if (isNext) {
                        Spacer(Modifier.width(Spacing.xs))
                        MetaChip(
                            text = "القادمة",
                            containerColor = GoldPrimary,
                            contentColor = EmeraldDeep,
                            borderColor = GoldShimmer,
                        )
                    }
                }
                Text(
                    text = status,
                    color = if (isCurrent) GoldPrimary else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Text(
                text = ArabicTime.formatTime(time),
                color = if (emphasised) GoldShimmer else TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OffsetStepper(
                minutes = offset,
                onDecrease = { onAdjustOffset(key, -1) },
                onIncrease = { onAdjustOffset(key, 1) },
            )
            ModeControl(mode = mode, onClick = { onCycleMode(key) })
        }

        if (showFajrNote) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "تنبيه الفجر موقوف — فعّله من الإعدادات",
                color = DangerRose,
                fontSize = 10.sp,
            )
        }
    }
}

/** الشروق is a time marker, not a prayer: subtle surface, offset only, never an adhan control. */
@Composable
private fun SunriseRow(
    time: Long,
    status: String,
    offset: Int,
    onAdjustOffset: (PrayerKey, Int) -> Unit,
) {
    SubtleSurface(modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PrayerKey.SUNRISE.arabicName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    MetaChip(text = "علامة وقتية")
                }
                Text(text = status, color = TextMuted, fontSize = 11.sp)
            }
            Text(
                text = ArabicTime.formatTime(time),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OffsetStepper(
                minutes = offset,
                onDecrease = { onAdjustOffset(PrayerKey.SUNRISE, -1) },
                onIncrease = { onAdjustOffset(PrayerKey.SUNRISE, 1) },
            )
            Text(text = "لا يوجد أذان للشروق", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// --------------------------------------------------------------------------- controls

/** Offset value chip flanked by −/+ steppers; persists and re-schedules on every tap. */
@Composable
private fun OffsetStepper(
    minutes: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(label = "−", onClick = onDecrease)
        Spacer(Modifier.width(Spacing.sm))
        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
            MetaChip(text = offsetLabel(minutes))
        }
        Spacer(Modifier.width(Spacing.sm))
        StepperButton(label = "+", onClick = onIncrease)
    }
}

/** Small emerald/gold −/+ button used by the two steppers on this screen. */
@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.sm),
        color = EmeraldSurface,
        border = BorderStroke(Spacing.hairline, EmeraldOutline),
    ) {
        Text(
            text = label,
            color = GoldPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}

/** Clickable per-prayer alert mode: أذان ← تنبيه قبل الأذان ← صامت. */
@Composable
private fun ModeControl(mode: PrayerNotificationMode, onClick: () -> Unit) {
    val (label, accent, icon) = when (mode) {
        PrayerNotificationMode.ADHAN -> Triple(
            "أذان كامل 🔔",
            GoldPrimary,
            Icons.Filled.VolumeUp,
        )
        PrayerNotificationMode.PRE_REMINDER -> Triple(
            "تنبيه قبل الأذان ⏰",
            GoldMuted,
            Icons.Filled.NotificationsActive,
        )
        PrayerNotificationMode.MUTE -> Triple(
            "صامت 🔕",
            TextMuted,
            Icons.Filled.VolumeOff,
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.md),
        color = EmeraldSurface,
        border = BorderStroke(Spacing.hairline, accent),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --------------------------------------------------------------------------- adhan voice

/** Adhan voice picker; the selected voice is persisted straight to settings. */
@Composable
private fun AdhanVoiceSection(
    current: AdhanVoice,
    onSelect: (AdhanVoice) -> Unit,
) {
    GoldCard(contentPadding = Spacing.lg) {
        SectionHeader(
            title = "الأذان",
            subtitle = "اختر صوت المؤذن المفضل لديك",
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = current.displayName,
                    color = GoldShimmer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = current.maqam,
                    color = GoldMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = current.description,
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(Spacing.sm))
        PillRow(
            items = AdhanVoice.ordered,
            selectedItem = current,
            labelOf = { shortVoiceLabel(it) },
            onSelect = onSelect,
            keyOf = { it.id },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --------------------------------------------------------------------------- notifications

/** Alert lead-time stepper, the Fajr master switch and the notification-permission note. */
@Composable
private fun AlertFooter(
    alertTiming: Int,
    fajrEnabled: Boolean,
    onAlertTimingChange: (Int) -> Unit,
    onToggleFajr: () -> Unit,
) {
    SubtleSurface(modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.md) {
        SectionHeader(
            title = "التنبيهات",
            subtitle = "قبل الأذان وضبط تنبيه الفجر",
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "التنبيه قبل الأذان بـ",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(label = "−", onClick = { onAlertTimingChange(alertTiming - 1) })
                Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$alertTiming دقيقة",
                        color = GoldShimmer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StepperButton(label = "+", onClick = { onAlertTimingChange(alertTiming + 1) })
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Surface(
            onClick = onToggleFajr,
            shape = RoundedCornerShape(Radius.md),
            color = EmeraldSurface,
            border = BorderStroke(Spacing.hairline, if (fajrEnabled) GoldPrimary else EmeraldOutline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = if (fajrEnabled) GoldPrimary else TextMuted,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (fajrEnabled) "تنبيه الفجر مُفعّل" else "تنبيه الفجر موقوف",
                    color = if (fajrEnabled) GoldPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "يحتاج عمل الأذان إلى إذن الإشعارات وإذن المنبّهات الدقيقة على هاتفك.",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        )
    }
}

/** Informational hint shown on Android 13+ where notifications are opt-in. */
@Composable
private fun NotificationPermissionNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = EmeraldSurface,
        border = BorderStroke(Spacing.hairline, EmeraldOutline),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = "فعّل الإشعارات",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "لتصلك تنبيهات الأذان، اسمح للتطبيق بإرسال الإشعارات من إعدادات النظام.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

// --------------------------------------------------------------------------- dialogs

/** Body of the calculation-method / daylight-saving explanation dialog. */
@Composable
private fun MethodInfoContent() {
    Text(
        text = "تُحسب المواقيت وفق طريقة الهيئة المصرية العامة للمساحة: زاوية الفجر 19.5°، " +
            "وزاوية العشاء 17.5°، وارتفاع الشمس عند الغروب 0.833°، وبمذهب الشافعي في وقت العصر.",
        color = TextPrimary,
        fontSize = 12.sp,
        lineHeight = 20.sp,
    )
    Text(
        text = "يُطبَّق التوقيت الصيفي المصري تلقائياً من آخر جمعة في أبريل حتى آخر خميس في أكتوبر " +
            "(UTC+3)، ويعود التوقيت إلى (UTC+2) في باقي العام.",
        color = TextPrimary,
        fontSize = 12.sp,
        lineHeight = 20.sp,
    )
    Text(
        text = "الفروق اليدوية لكل صلاة تُضاف إلى الوقت المحسوب قبل جدولة تنبيه الأذان.",
        color = TextMuted,
        fontSize = 11.sp,
        lineHeight = 18.sp,
    )
}

// --------------------------------------------------------------------------- helpers

/** Human-readable live status of a moment: the current window, a countdown, or a past time. */
private fun statusLabel(key: PrayerKey, time: Long, state: PrayersUiState): String = when {
    key == state.currentKey -> "الآن"
    time > state.nowMillis -> "بعد " + ArabicTime.countdownUntil(time, state.nowMillis).compact
    else -> "مضى"
}

/** "بعد ٢ ساعة و١٤ دقيقة" style phrase (digits stay Western, matching [ArabicTime]). */
private fun friendlyRemaining(hours: Long, minutes: Long): String = when {
    hours > 0L && minutes > 0L -> "بعد $hours ساعة و$minutes دقيقة"
    hours > 0L -> "بعد $hours ساعة"
    minutes > 0L -> "بعد $minutes دقيقة"
    else -> "بعد أقل من دقيقة"
}

/** Renders the manual offset as `+3 د` / `-2 د` / `0 د`. */
private fun offsetLabel(minutes: Int): String = when {
    minutes > 0 -> "+$minutes د"
    minutes < 0 -> "$minutes د"
    else -> "0 د"
}

/** Compact Arabic pill label for each adhan voice. */
private fun shortVoiceLabel(voice: AdhanVoice): String = voice.shortLabel

/** Gregorian date in Egyptian Arabic, e.g. `الأربعاء 15 أكتوبر 2026`. */
private fun gregorianDate(millis: Long): String = runCatching {
    SimpleDateFormat("EEEE d MMMM yyyy", Locale("ar", "EG")).format(Date(millis))
}.getOrDefault("")

/** Hijri (Umm al-Qura) date via the platform ICU calendar; `null` when unavailable. */
private fun hijriDate(millis: Long): String? = runCatching {
    val calendar = android.icu.util.IslamicCalendar(android.icu.util.ULocale("ar_EG"))
    calendar.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
    calendar.timeInMillis = millis

    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.getDisplayName(
        java.util.Calendar.MONTH,
        java.util.Calendar.LONG,
        android.icu.util.ULocale("ar_EG"),
    ).orEmpty()

    "$day $month $year هـ"
}.getOrNull()
