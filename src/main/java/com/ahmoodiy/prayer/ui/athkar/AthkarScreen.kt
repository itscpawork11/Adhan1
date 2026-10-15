package com.ahmoodiy.prayer.ui.athkar

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ThikrCategory
import com.ahmoodiy.prayer.domain.model.ThikrItem
import com.ahmoodiy.prayer.ui.components.ArabicSearchField
import com.ahmoodiy.prayer.ui.components.CaptionText
import com.ahmoodiy.prayer.ui.components.GoldCard
import com.ahmoodiy.prayer.ui.components.MetaChip
import com.ahmoodiy.prayer.ui.components.Pill
import com.ahmoodiy.prayer.ui.components.PillRow
import com.ahmoodiy.prayer.ui.components.ScreenHeader
import com.ahmoodiy.prayer.ui.components.SectionHeader
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.GoldMuted
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.Tashkeel
import kotlinx.coroutines.delay

/**
 * The offline Hisn al-Muslim library: a category carousel, a cross-category search, per-thikr
 * repetition counters, favourites and share/copy — all rendered in the emerald & gold look.
 */
@Composable
fun AthkarScreen(modifier: Modifier = Modifier) {
    val viewModel: AthkarViewModel = viewModel(factory = AthkarViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scale = state.settings.fontScale.multiplier

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // Transient confirmation shown after a copy.
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_600)
            copied = false
        }
    }

    val onCopy: (ThikrItem) -> Unit = { item ->
        clipboard.setText(AnnotatedString(sharePayload(item, state.settings)))
        copied = true
    }
    val onShare: (ThikrItem) -> Unit = { item ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharePayload(item, state.settings))
        }
        context.startActivity(Intent.createChooser(send, "مشاركة"))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "أذكار حصن المسلم",
                subtitle = "مكتبة صحيحة 100% · دون إنترنت",
                actions = {
                    IconButton(onClick = viewModel::toggleFavoritesOnly) {
                        Icon(
                            imageVector = if (state.favoritesOnly) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = "المفضلة فقط",
                            tint = if (state.favoritesOnly) GoldPrimary else TextMuted,
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
                ArabicSearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "ابحث في نص الأذكار...",
                )

                Spacer(Modifier.height(Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CaptionText(
                        text = if (state.searchActive) "بحث في جميع التصنيفات" else "التصنيفات",
                    )
                    Pill(
                        label = "المفضلة فقط",
                        selected = state.favoritesOnly,
                        onClick = viewModel::toggleFavoritesOnly,
                    )
                }

                Spacer(Modifier.height(Spacing.sm))

                PillRow(
                    items = ThikrCategory.entries,
                    selectedItem = state.category,
                    labelOf = { it.displayLabel },
                    onSelect = viewModel::setCategory,
                    keyOf = { it.id },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.md))

                SectionHeader(
                    title = "النتائج: ${state.displayedItems.size} · الإجمالي: ${state.totalCount}",
                    subtitle = state.category.displayLabel,
                    trailing = {
                        IconButton(onClick = viewModel::shuffle) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "خلط", tint = GoldPrimary)
                        }
                    },
                )

                Spacer(Modifier.height(Spacing.sm))

                val items = state.displayedItems
                if (items.isEmpty()) {
                    AthkarEmptyState(favoritesOnly = state.favoritesOnly)
                } else {
                    items.forEach { item ->
                        key(item.id) {
                            ThikrCard(
                                item = item,
                                counter = state.counterOf(item),
                                isFavorite = state.isFavorite(item),
                                showTashkeel = state.settings.showTashkeel,
                                showSanad = state.settings.showSanadAndVirtue,
                                scale = scale,
                                onIncrement = { viewModel.increment(item) },
                                onReset = { viewModel.resetCounter(item) },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                onCopy = { onCopy(item) },
                                onShare = { onShare(item) },
                            )
                            Spacer(Modifier.height(Spacing.md))
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
            }
        }

        AnimatedVisibility(
            visible = copied,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xxl),
        ) {
            Surface(
                shape = RoundedCornerShape(Radius.pill),
                color = GoldPrimary,
            ) {
                Text(
                    text = "تم نسخ الذكر ✓",
                    color = EmeraldDeep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
        }
    }
}

/** Builds the copy/share payload for a single thikr using the shared formatter. */
private fun sharePayload(item: ThikrItem, settings: AppSettings): String = Tashkeel.shareText(
    text = item.text,
    title = item.title,
    source = item.source,
    virtue = item.virtue,
    withAppName = settings.shareFormatWithAppName,
    showTashkeel = settings.showTashkeel,
)

@Composable
private fun ThikrCard(
    item: ThikrItem,
    counter: Int,
    isFavorite: Boolean,
    showTashkeel: Boolean,
    showSanad: Boolean,
    scale: Float,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val complete = item.count > 0 && counter >= item.count
    val fraction = if (item.count <= 0) 0f else (counter.toFloat() / item.count).coerceIn(0f, 1f)

    GoldCard(highlighted = complete) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaChip(item.category.displayLabel)
            MetaChip(if (item.count > 1) "×${item.count}" else "×مرة واحدة")
            if (item.isQuranVerse) MetaChip("آية قرآنية")
            if (complete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "مكتمل",
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (showSanad) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = item.source,
                color = GoldMuted,
                fontSize = 10.sp,
                lineHeight = (16 * scale).sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!item.title.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            SectionHeader(title = item.title)
        }

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = Tashkeel.apply(item.text, showTashkeel),
            color = if (item.isQuranVerse) GoldShimmer else TextPrimary,
            fontSize = 15.sp * scale,
            lineHeight = (27 * scale).sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        if (showSanad && item.virtue.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "✦ الفضل: ${item.virtue}",
                color = TextMuted,
                fontSize = 11.sp * scale,
                lineHeight = (20 * scale).sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$counter / ${item.count}",
                    color = if (complete) GoldPrimary else GoldMuted,
                    fontSize = 12.sp * scale,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(EmeraldOutline),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Brush.horizontalGradient(listOf(GoldShimmer, GoldPrimary))),
                    )
                }
            }

            Spacer(Modifier.width(Spacing.md))

            Pill(
                label = "+1",
                selected = true,
                onClick = onIncrement,
            )
        }

        Spacer(Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) GoldPrimary else TextMuted,
                )
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = TextMuted)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = TextMuted)
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Filled.RestartAlt, contentDescription = "تصفير العداد", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun AthkarEmptyState(favoritesOnly: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = GoldMuted,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = if (favoritesOnly) "لا توجد أذكار في المفضلة" else "لا توجد أذكار مطابقة",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "جرّب تغيير التصنيف أو كلمة البحث",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}
