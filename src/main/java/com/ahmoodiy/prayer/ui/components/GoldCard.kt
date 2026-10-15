package com.ahmoodiy.prayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahmoodiy.prayer.ui.theme.EmeraldCompleted
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing

/**
 * The app's signature surface: deep emerald background, hairline gold border, soft rounding.
 *
 * @param highlighted draws the stronger "completed / active" treatment.
 * @param elevation soft drop shadow used to lift the card off the canvas.
 */
@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: Dp = Spacing.lg,
    containerColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = borderColor ?: if (highlighted) GoldPrimary.copy(alpha = 0.6f) else EmeraldOutline
    val container = containerColor
        ?: if (highlighted) EmeraldCompleted else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.card),
        color = container,
        border = BorderStroke(Spacing.hairline, border),
        shadowElevation = elevation,
    ) {
        Column(
            modifier = Modifier
                .background(container)
                .padding(contentPadding),
            content = content,
        )
    }
}

/** A subtler flat surface used inside cards / list rows. */
@Composable
fun SubtleSurface(
    modifier: Modifier = Modifier,
    contentPadding: Dp = Spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Spacing.hairline, EmeraldOutline),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Solid emerald chip with gold text, used for metadata badges. */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = EmeraldSurface,
    contentColor: Color = GoldPrimary,
    borderColor: Color = EmeraldOutline,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.sm),
        color = containerColor,
        border = BorderStroke(Spacing.hairline, borderColor),
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
        )
    }
}
