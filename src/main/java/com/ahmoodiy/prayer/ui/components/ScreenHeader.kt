package com.ahmoodiy.prayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted

/**
 * The in-app header shown at the top of every section:
 * Arabic title, a small subtitle and a row of quick actions.
 *
 * Finished with a vertical gold accent bar and a fading gold rule so every screen opens with the
 * same, consistent brand lockup.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(EmeraldSurface, EmeraldDeep)))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GoldShimmer, GoldPrimary.copy(alpha = 0.3f)),
                        ),
                        shape = RoundedCornerShape(3.dp),
                    ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = GoldShimmer,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, start = Spacing.sm),
                )
            }

            actions()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.5f),
                            GoldPrimary.copy(alpha = 0.15f),
                            EmeraldOutline,
                        ),
                    ),
                ),
        )
    }
}
