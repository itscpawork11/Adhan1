package com.ahmoodiy.prayer.ui.duas

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.domain.model.ThikrCategory
import com.ahmoodiy.prayer.domain.model.ThikrItem
import com.ahmoodiy.prayer.ui.components.ArabicSearchField
import com.ahmoodiy.prayer.ui.components.GoldCard
import com.ahmoodiy.prayer.ui.components.MetaChip
import com.ahmoodiy.prayer.ui.components.Pill
import com.ahmoodiy.prayer.ui.components.PillRow
import com.ahmoodiy.prayer.ui.components.ScreenHeader
import com.ahmoodiy.prayer.ui.components.SectionHeader
import com.ahmoodiy.prayer.ui.theme.GoldMuted
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.Tashkeel

/** The category pills: "الكل" (null) plus the four dua categories. */
private val categoryFilters: List<ThikrCategory?> =
    listOf(null) + ThikrCategory.duasCategories.toList()

/**
 * The Duas (الأدعية) section: a browsable, offline list of the Quranic, prophetic, general
 * and ruqyah invocations from Hisn al-Muslim.
 *
 * Users can filter by category, search without tashkeel, restrict to favourites, shuffle the
 * order, count each dua, and copy or share its text.
 */
@Composable
fun DuasScreen(modifier: Modifier = Modifier) {
    val viewModel: DuasViewModel = viewModel(factory = DuasViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scale = state.settings.fontScale.multiplier

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun payloadFor(item: ThikrItem): String = Tashkeel.shareText(
        text = item.text,
        title = item.title,
        source = item.source,
        virtue = item.virtue,
        withAppName = state.settings.shareFormatWithAppName,
        showTashkeel = state.settings.showTashkeel,
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "الأدعية القرآنية والنبوية",
            subtitle = "مختارات من حصن المسلم · دون إنترنت",
        )

        FilterSection(
            selectedCategory = state.selectedCategory,
            query = state.query,
            favoritesOnly = state.favoritesOnly,
            resultCount = state.resultCount,
            onSelectCategory = viewModel::selectCategory,
            onQueryChange = viewModel::updateQuery,
            onToggleFavorites = viewModel::toggleFavoritesOnly,
            onShuffle = viewModel::shuffle,
        )

        if (state.items.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items = state.items, key = { it.id }) { item ->
                    DuaCard(
                        item = item,
                        count = state.counters[item.id] ?: 0,
                        isFavorite = item.id in state.favorites,
                        showSanadAndVirtue = state.settings.showSanadAndVirtue,
                        showTashkeel = state.settings.showTashkeel,
                        scale = scale,
                        onIncrement = { viewModel.incrementCounter(item) },
                        onReset = { viewModel.resetCounter(item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(payloadFor(item)))
                            Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, payloadFor(item))
                            }
                            context.startActivity(Intent.createChooser(send, "مشاركة"))
                        },
                    )
                }
            }
        }
    }
}

/** Category pills, search field, favourites filter and the result counter line. */
@Composable
private fun FilterSection(
    selectedCategory: ThikrCategory?,
    query: String,
    favoritesOnly: Boolean,
    resultCount: Int,
    onSelectCategory: (ThikrCategory?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        PillRow(
            items = categoryFilters,
            selectedItem = selectedCategory,
            labelOf = { it?.displayLabel ?: "الكل" },
            onSelect = onSelectCategory,
            keyOf = { it?.id ?: "all" },
        )

        Spacer(Modifier.height(Spacing.sm))

        ArabicSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "ابحث في الأدعية...",
        )

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Pill(
                label = "المفضلة فقط",
                selected = favoritesOnly,
                onClick = onToggleFavorites,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        SectionHeader(
            title = "النتائج: $resultCount",
            titleColor = GoldShimmer,
            trailing = {
                Pill(
                    label = "خلط",
                    selected = false,
                    onClick = onShuffle,
                )
            },
        )
    }
}

/** A single dua card: metadata, text, virtue/source, counter and utility actions. */
@Composable
private fun DuaCard(
    item: ThikrItem,
    count: Int,
    isFavorite: Boolean,
    showSanadAndVirtue: Boolean,
    showTashkeel: Boolean,
    scale: Float,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val reached = item.count > 0 && count >= item.count

    GoldCard(highlighted = reached) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                MetaChip(text = item.categoryArabic)
                MetaChip(text = "×${item.count}")
            }
            if (reached) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "مكتمل",
                    tint = GoldPrimary,
                )
            }
        }

        if (!item.title.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            SectionHeader(title = Tashkeel.apply(item.title, showTashkeel))
        }

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = Tashkeel.apply(item.text, showTashkeel),
            color = if (item.isQuranVerse) GoldShimmer else TextPrimary,
            fontSize = 15.sp * scale,
            lineHeight = (27 * scale).sp,
            textAlign = TextAlign.Right,
        )

        if (showSanadAndVirtue) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "✦ الفضل: ${item.virtue}",
                color = TextMuted,
                fontSize = 11.sp * scale,
                lineHeight = (20 * scale).sp,
                textAlign = TextAlign.Right,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "✦ المصدر: ${item.source}",
                color = TextMuted,
                fontSize = 11.sp * scale,
                lineHeight = (20 * scale).sp,
                textAlign = TextAlign.Right,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count / ${item.count}",
                color = if (reached) GoldPrimary else TextPrimary,
                fontSize = 13.sp * scale,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onIncrement) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "زيادة العداد",
                    tint = GoldPrimary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
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
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "نسخ",
                    tint = TextMuted,
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "مشاركة",
                    tint = TextMuted,
                )
            }
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = "تصفير",
                    tint = TextMuted,
                )
            }
        }
    }
}

/** Friendly placeholder shown when no dua matches the active filters. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = GoldMuted,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "لا توجد أدعية مطابقة",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "جرّب تغيير الفئة أو مسح كلمة البحث",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
