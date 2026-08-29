package io.github.meko123456.dro.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.dro.domain.TimeFormat
import io.github.meko123456.dro.domain.ZoneClock
import io.github.meko123456.dro.droApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(context.droApp.settings) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twentyFourHour = DateFormat.is24HourFormat(context)
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var editingZone: String? by rememberSaveable { mutableStateOf(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add city")
            }
        },
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Dro", style = MaterialTheme.typography.titleLarge)
                    Text("დრო", style = MaterialTheme.typography.labelMedium)
                }
            })
        },
    ) { padding ->
        val ui = state ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "home") { HomeHeader(ui.home, twentyFourHour) }
            item(key = "divider") { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            items(ui.cities, key = { it.zone.id }) { row ->
                val index = ui.cities.indexOf(row)
                CityRow(
                    row = row,
                    twentyFourHour = twentyFourHour,
                    canMoveUp = index > 0,
                    canMoveDown = index < ui.cities.lastIndex,
                    onMakeHome = { viewModel.makeHome(row.zone) },
                    onMoveUp = { viewModel.moveCity(index, index - 1) },
                    onMoveDown = { viewModel.moveCity(index, index + 1) },
                    onRemove = { viewModel.removeCity(row.zone) },
                )
            }
            item(key = "overlap") {
                OverlapStrip(
                    date = ui.date,
                    home = ui.home.zone,
                    dayLengthMinutes = ui.dayLengthMinutes,
                    nowMinute = ui.nowMinute,
                    bars = ui.bars,
                    shared = ui.shared,
                    twentyFourHour = twentyFourHour,
                    onEditHours = { editingZone = it.zone.id },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
        }
        val editing = ui.bars.firstOrNull { it.row.zone.id == editingZone }
        if (editing != null) {
            WorkingHoursDialog(
                entry = editing.row.entry,
                hours = editing.row.hours,
                twentyFourHour = twentyFourHour,
                onSave = { viewModel.setHours(editing.row.zone, it); editingZone = null },
                onDismiss = { editingZone = null },
            )
        }
        if (showAdd) {
            AddCitySheet(
                alreadyShown = (ui.cities.map { it.zone } + ui.home.zone).toSet(),
                onPick = { viewModel.addCity(it); showAdd = false },
                onDismiss = { showAdd = false },
            )
        }
    }
}

/** The home zone: big clock, date, city. One semantics node. */
@Composable
private fun HomeHeader(row: ClockRow, twentyFourHour: Boolean) {
    val reading = row.reading
    val description = "Home, ${row.entry.city}, ${TimeFormat.spoken(reading.localTime, twentyFourHour)}, " +
        TimeFormat.date(reading.localDate)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(
            text = "Home · ${row.entry.city}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ClockText(reading.localTime, twentyFourHour, large = true)
        Text(TimeFormat.date(reading.localDate), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * One city: name + region on the left, clock + offset on the right, and a menu with the row
 * actions. The text merges into one semantics node; the menu button stays separate so a
 * screen reader can reach "Make home" / "Remove" without a gesture.
 */
@Composable
private fun CityRow(
    row: ClockRow,
    twentyFourHour: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMakeHome: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val reading = row.reading
    val shift = ZoneClock.dayShiftLabel(reading.dayShift)
    val description = listOfNotNull(
        row.entry.city,
        TimeFormat.spoken(reading.localTime, twentyFourHour),
        ZoneClock.spokenOffset(reading.offsetMinutes),
        shift,
    ).joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.entry.city, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = row.entry.region,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                ClockText(reading.localTime, twentyFourHour, large = false)
                Text(
                    text = listOfNotNull(shift, ZoneClock.offsetLabel(reading.offsetMinutes)).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.clearAndSetSemantics { contentDescription = "Options for ${row.entry.city}" },
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Make home") }, onClick = { menuOpen = false; onMakeHome() })
            DropdownMenuItem(text = { Text("Move up") }, enabled = canMoveUp, onClick = { menuOpen = false; onMoveUp() })
            DropdownMenuItem(text = { Text("Move down") }, enabled = canMoveDown, onClick = { menuOpen = false; onMoveDown() })
            DropdownMenuItem(text = { Text("Remove") }, onClick = { menuOpen = false; onRemove() })
        }
    }
}

/** `14:05`, or `2:05` with a small `pm` when the device uses a 12-hour clock. */
@Composable
private fun ClockText(time: java.time.LocalTime, twentyFourHour: Boolean, large: Boolean) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = TimeFormat.time(time, twentyFourHour),
            style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineSmall,
        )
        TimeFormat.amPm(time, twentyFourHour)?.let { marker ->
            Text(
                text = marker,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = if (large) 12.dp else 4.dp),
            )
        }
    }
}
