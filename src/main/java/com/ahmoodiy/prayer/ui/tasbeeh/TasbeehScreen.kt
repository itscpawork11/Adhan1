package com.ahmoodiy.prayer.ui.tasbeeh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.data.TasbeehPhrasesData
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import com.ahmoodiy.prayer.ui.LocalAppSettings
import com.ahmoodiy.prayer.ui.components.GoldCard
import com.ahmoodiy.prayer.ui.components.GoldConfirmDialog
import com.ahmoodiy.prayer.ui.components.Pill
import com.ahmoodiy.prayer.ui.components.PillRow
import com.ahmoodiy.prayer.ui.components.ScreenHeader
import com.ahmoodiy.prayer.ui.components.SectionHeader
import com.ahmoodiy.prayer.ui.components.SubtleSurface
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldMuted
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.Tashkeel
import kotlinx.coroutines.delay

@Composable
fun TasbeehScreen(modifier: Modifier = Modifier) {
    val viewModel: TasbeehViewModel = viewModel(factory = TasbeehViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scale = state.settings.fontScale.multiplier

    var showManager by remember { mutableStateOf(false) }

    // Transient banner shown when a full cycle completes.
    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(state.celebrationToken) {
        if (state.celebrationToken > 0) {
            celebrating = true
            delay(1_700)
            celebrating = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "المسبحة الإلكترونية المطورة",
                subtitle = "دار النور الرقمية · إنشاء أبو الريش",
                actions = {
                    HeaderActions(
                        soundEnabled = state.settings.tasbeehSoundEnabled,
                        soundType = state.settings.tasbeehSoundType,
                        hapticsEnabled = state.settings.tasbeehHapticOnTap,
                        onToggleSound = viewModel::toggleSound,
                        onCycleSoundType = viewModel::cycleSoundType,
                        onToggleHaptics = viewModel::toggleHaptics,
                        onReset = viewModel::requestReset,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = Tashkeel.apply(state.phrase.arabicText, state.settings.showTashkeel),
                    color = GoldShimmer,
                    fontSize = 15.sp * scale,
                    lineHeight = (26 * scale).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                )

                TasbeehRing(
                    progress = state.progress,
                    target = state.target,
                    diameter = 268.dp,
                    onTap = viewModel::tap,
                    onLongPress = viewModel::requestReset,
                ) {
                    RingContent(state = state, scale = scale)
                }

                Spacer(Modifier.height(Spacing.lg))

                TargetPresets(
                    selectedTarget = state.target,
                    onSelect = viewModel::setTarget,
                )

                Spacer(Modifier.height(Spacing.md))

                StatsGrid(state = state, scale = scale)

                Spacer(Modifier.height(Spacing.md))

                VirtueCard(
                    virtue = state.phrase.virtue,
                    count = state.phrase.defaultTarget,
                    scale = scale,
                )

                Spacer(Modifier.height(Spacing.md))

                GoldCard(contentPadding = Spacing.lg) {
                    SectionHeader(
                        title = "سجل الأذكار",
                        subtitle = "اختر الذكر أو أضف ذكراً مخصصاً",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = viewModel::openAddPhraseDialog) {
                                    Icon(Icons.Filled.Add, contentDescription = "إضافة ذكر", tint = GoldPrimary)
                                }
                                IconButton(onClick = viewModel::togglePhraseManager) {
                                    Icon(Icons.Filled.PlaylistPlay, contentDescription = "إدارة الأذكار", tint = GoldPrimary)
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    PillRow(
                        items = state.phrases,
                        selectedItem = state.phrase,
                        labelOf = { it.arabicText.take(22) },
                        onSelect = viewModel::selectPhrase,
                        keyOf = { it.id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
            }
        }

        // Completion banner
        AnimatedVisibility(
            visible = celebrating,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(Brush.horizontalGradient(listOf(GoldShimmer, GoldPrimary)))
                    .padding(horizontal = 26.dp, vertical = 18.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "أتممت الدور ✦",
                        color = EmeraldDeep,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${state.target} · ${state.currentCount}",
                        color = EmeraldDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (state.showResetDialog) {
        GoldConfirmDialog(
            title = "إعادة تعيين العداد",
            message = "سيتم تصفير عداد الجولة الحالية (${state.currentCount}). هل تريد المتابعة؟",
            destructive = true,
            onConfirm = viewModel::resetCurrentRound,
            onDismiss = viewModel::dismissResetDialog,
        )
    }

    if (state.showAddPhraseDialog) {
        AddPhraseDialog(
            draft = state.customDraft,
            onDraftChange = viewModel::updateCustomDraft,
            onConfirm = viewModel::saveCustomPhrase,
            onDismiss = viewModel::dismissAddPhraseDialog,
        )
    }

    if (showManager) {
        PhraseManagerSheet(
            phrases = state.phrases,
            selectedId = state.phrase.id,
            showTashkeel = state.settings.showTashkeel,
            onSelect = { phrase ->
                viewModel.selectPhrase(phrase)
                showManager = false
            },
            onDelete = viewModel::deletePhrase,
            onDismiss = { showManager = false },
        )
    }
}

@Composable
private fun HeaderActions(
    soundEnabled: Boolean,
    soundType: TasbeehSoundType,
    hapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onCycleSoundType: () -> Unit,
    onToggleHaptics: () -> Unit,
    onReset: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCycleSoundType) {
            Icon(
                imageVector = if (soundEnabled && soundType != TasbeehSoundType.MUTE) {
                    Icons.Filled.VolumeUp
                } else {
                    Icons.Filled.VolumeDown
                },
                contentDescription = "الصوت",
                tint = if (soundEnabled) GoldPrimary else TextMuted,
            )
        }
        IconButton(onClick = onToggleSound) {
            Icon(
                Icons.Filled.VolumeOff,
                contentDescription = "كتم الصوت",
                tint = if (soundEnabled) GoldPrimary else TextMuted,
            )
        }
        IconButton(onClick = onToggleHaptics) {
            Icon(Icons.Filled.Vibration, contentDescription = "الاهتزاز", tint = if (hapticsEnabled) GoldPrimary else TextMuted)
        }
        IconButton(onClick = onReset) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "تصفير", tint = GoldPrimary)
        }
    }
}

@Composable
private fun RingContent(state: TasbeehUiState, scale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 34.dp),
    ) {
        Text(
            text = state.currentCount.toString(),
            color = GoldShimmer,
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 56.sp,
        )
        Text(
            text = "من ${state.target} · المتبقي ${state.remaining}",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.md))
        if (state.isComplete) {
            Text(
                text = "✦ أتممت هذا الدور ✦",
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                text = Tashkeel.apply(state.phrase.arabicText, state.settings.showTashkeel),
                color = TextPrimary,
                fontSize = 12.sp * scale,
                lineHeight = (21 * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TargetPresets(selectedTarget: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        PillRow(
            items = TasbeehPhrasesData.targetPresets,
            selectedItem = selectedTarget,
            labelOf = { it.toString() },
            onSelect = onSelect,
            keyOf = { it },
        )
    }
}

@Composable
private fun StatsGrid(state: TasbeehUiState, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatCell(
            label = "الجولة الحالية",
            value = state.currentCount,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = "مجموع اليوم",
            value = state.stats.dailyTotal,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = "الإجمالي",
            value = state.stats.lifetimeTotal,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = "الأدوار",
            value = state.stats.cyclesCompleted,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(label: String, value: Int, scale: Float, modifier: Modifier = Modifier) {
    SubtleSurface(modifier = modifier, contentPadding = Spacing.sm) {
        Text(
            text = value.toString(),
            color = GoldShimmer,
            fontSize = 16.sp * scale,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VirtueCard(virtue: String, count: Int, scale: Float) {
    GoldCard(contentPadding = Spacing.lg) {
        SectionHeader(title = "فضل الذكر", subtitle = "من فضائل هذا الذكر")
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = virtue,
            color = TextPrimary,
            fontSize = 12.sp * scale,
            lineHeight = (22 * scale).sp,
            textAlign = TextAlign.Right,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "عدد السنّة المطلوبة: $count",
            color = GoldMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AddPhraseDialog(
    draft: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        titleContentColor = GoldShimmer,
        textContentColor = TextPrimary,
        title = { Text(text = "إضافة ذكر مخصص", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("اكتب نص الذكر هنا…", color = TextMuted, fontSize = 13.sp) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(Radius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = com.ahmoodiy.prayer.ui.theme.EmeraldOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GoldPrimary,
                    focusedContainerColor = EmeraldDeep,
                    unfocusedContainerColor = EmeraldDeep,
                ),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
                enabled = draft.isNotBlank(),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.action_confirm),
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.action_cancel),
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
        },
    )
}
