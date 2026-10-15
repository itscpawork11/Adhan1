package com.ahmoodiy.prayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextPrimary

/** A single selectable pill / chip. */
@Composable
fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = GoldPrimary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = if (selected) accentColor else EmeraldSurface,
        border = BorderStroke(
            Spacing.hairline,
            if (selected) GoldShimmer else EmeraldOutline,
        ),
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = if (selected) EmeraldDeep else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/** Horizontally scrollable row of pills. */
@Composable
fun <T> PillRow(
    items: List<T>,
    selectedItem: T?,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    keyOf: (T) -> Any = { it as Any },
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            androidx.compose.runtime.key(keyOf(item)) {
                Pill(
                    label = labelOf(item),
                    selected = item == selectedItem,
                    onClick = { onSelect(item) },
                )
            }
        }
    }
}
