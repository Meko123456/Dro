package io.github.meko123456.dro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.meko123456.dro.domain.WorkingHours
import io.github.meko123456.dro.domain.ZoneEntry
import java.time.LocalTime

/** Edit one zone's working hours in its local time. An end at or before the start crosses midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHoursDialog(
    entry: ZoneEntry,
    hours: WorkingHours,
    twentyFourHour: Boolean,
    onSave: (WorkingHours) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = rememberTimePickerState(hours.start.hour, hours.start.minute, is24Hour = twentyFourHour)
    val end = rememberTimePickerState(hours.end.hour, hours.end.minute, is24Hour = twentyFourHour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Working hours · ${entry.city}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("In ${entry.city}'s local time. Ending before the start means an overnight shift.")
                Text("Start")
                TimeInput(state = start)
                Text("End")
                TimeInput(state = end)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(WorkingHours(LocalTime.of(start.hour, start.minute), LocalTime.of(end.hour, end.minute)))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onSave(WorkingHours.DEFAULT) }) { Text("Reset to 09:00–18:00") }
        },
    )
}
