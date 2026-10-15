package com.ahmoodiy.prayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmoodiy.prayer.ui.navigation.AppDestination
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted

/** Custom bottom navigation bar matching the emerald + gold luxury identity. */
@Composable
fun AhmoodiyBottomBar(
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EmeraldDeep)
            .navigationBarsPadding(),
    ) {
        // Fading gold rule replacing the flat separator.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.5f),
                            GoldPrimary.copy(alpha = 0.18f),
                            GoldPrimary.copy(alpha = 0.5f),
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppDestination.entries.forEach { destination ->
                val selected = destination.route == currentRoute
                BottomBarItem(
                    destination = destination,
                    selected = selected,
                    onClick = { if (!selected) onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) GoldShimmer else TextMuted
    val container = if (selected) GoldPrimary.copy(alpha = 0.14f) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Keeps icons aligned while the active tab shows its own gold marker.
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(bottom = Spacing.xs)
                    .width(20.dp)
                    .height(2.dp)
                    .background(GoldPrimary, RoundedCornerShape(2.dp)),
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }

        Icon(
            imageVector = destination.icon,
            contentDescription = stringResource(destination.titleRes),
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(destination.titleRes),
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
