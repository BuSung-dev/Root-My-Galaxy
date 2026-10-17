package dev.busung.s25uroot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BootDelayDialog(
    currentDelay: Int,
    minDelay: Int,
    maxDelay: Int,
    presets: List<Int> = AppPreferences.BOOT_DELAY_PRESETS.toList(),
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var delayValue by remember { mutableStateOf(currentDelay.toString()) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
        title = {
            Text(stringResource(R.string.boot_delay))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.boot_delay_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = delayValue,
                    onValueChange = {
                        delayValue = it
                        error = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error,
                    label = { Text(stringResource(R.string.boot_delay)) },
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.boot_delay_range,
                                minDelay,
                                maxDelay,
                            )
                        )
                    },
                )
                if (error) {
                    Text(
                        stringResource(R.string.boot_delay_range, minDelay, maxDelay),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        QuickDelayButton(
                            delay = preset,
                            selected = preset == currentDelay,
                            onClick = { delayValue = preset.toString(); error = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = delayValue.toIntOrNull()
                    if (parsed != null && parsed in minDelay..maxDelay) {
                        onConfirm(parsed)
                    } else {
                        error = true
                    }
                },
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun QuickDelayButton(
    delay: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.boot_delay_seconds, delay)) },
    )
}
