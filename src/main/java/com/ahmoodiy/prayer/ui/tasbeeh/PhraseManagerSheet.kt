package com.ahmoodiy.prayer.ui.tasbeeh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.domain.model.TasbeehPhrase
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.Radius
import com.ahmoodiy.prayer.ui.theme.Spacing
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary
import com.ahmoodiy.prayer.util.Tashkeel

/**
 * Bottom sheet listing every available tasbeeh formula (defaults + user created),
 * with an inline delete affordance for custom entries.
 */
@Composable
fun PhraseManagerSheet(
    phrases: List<TasbeehPhrase>,
    selectedId: String,
    showTashkeel: Boolean,
    onSelect: (TasbeehPhrase) -> Unit,
    onDelete: (TasbeehPhrase) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EmeraldSurface,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .height(4.dp)
                    .width(46.dp)
                    .background(GoldPrimary, RoundedCornerShape(Radius.pill)),
            )
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
            Text(
                text = "سجل الأذكار (${phrases.size})",
                color = GoldPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "اضغط لاختيار الذكر · امسح أي ذكر مخصص بزر الحذف",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.md),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(phrases, key = { it.id }) { phrase ->
                    PhraseRow(
                        phrase = phrase,
                        selected = phrase.id == selectedId,
                        showTashkeel = showTashkeel,
                        onClick = { onSelect(phrase) },
                        onDelete = if (phrase.isCustom) ({ onDelete(phrase) }) else null,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun PhraseRow(
    phrase: TasbeehPhrase,
    selected: Boolean,
    showTashkeel: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) com.ahmoodiy.prayer.ui.theme.EmeraldCompleted else EmeraldDeep,
                shape = RoundedCornerShape(Radius.md),
            )
            .border(
                width = Spacing.hairline,
                color = if (selected) GoldPrimary else EmeraldOutline,
                shape = RoundedCornerShape(Radius.md),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoldPrimary)
            Spacer(Modifier.width(6.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Tashkeel.apply(phrase.arabicText, showTashkeel),
                color = if (selected) GoldPrimary else TextPrimary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${phrase.virtue} · الهدف ${phrase.defaultTarget}",
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = com.ahmoodiy.prayer.ui.theme.DangerRose)
            }
        }
    }
}
