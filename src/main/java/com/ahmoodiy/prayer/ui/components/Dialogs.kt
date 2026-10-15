package com.ahmoodiy.prayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.ui.theme.EmeraldDeep
import com.ahmoodiy.prayer.ui.theme.EmeraldSurface
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import com.ahmoodiy.prayer.ui.theme.TextMuted
import com.ahmoodiy.prayer.ui.theme.TextPrimary

/** Shared emerald/gold confirmation dialog. */
@Composable
fun GoldConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_confirm),
    dismissLabel: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        titleContentColor = GoldShimmer,
        textContentColor = TextPrimary,
        title = {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, fontSize = 13.sp, textAlign = TextAlign.Start)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MaterialTheme.colorScheme.error else GoldPrimary,
                    contentColor = EmeraldDeep,
                ),
            ) {
                Text(text = confirmLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel, color = TextMuted, fontSize = 12.sp)
            }
        },
    )
}

/** Simple informational dialog (used for backup codes / errors). */
@Composable
fun GoldInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.action_ok),
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        titleContentColor = GoldShimmer,
        title = { Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = EmeraldDeep,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = confirmLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
    )
}
