package io.github.meko123456.dro.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.time.LocalTime

/** Choose a home-zone time to preview — the keyboard/screen-reader alternative to scrubbing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewTimeDialog(
    initial: LocalTime,
    twentyFourHour: Boolean,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = twentyFourHour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview a time") },
        text = { TimeInput(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("Preview") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
