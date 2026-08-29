package io.github.meko123456.dro.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.meko123456.dro.domain.ZoneCatalog
import io.github.meko123456.dro.domain.ZoneEntry
import java.time.ZoneId

/**
 * Search the tz catalogue and pick a city. Zones already on screen are listed but marked, so
 * the user learns why a tap did nothing instead of silently hitting a no-op.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCitySheet(
    alreadyShown: Set<ZoneId>,
    onPick: (ZoneId) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { ZoneCatalog.entries() }
    val results = remember(query, all) { ZoneCatalog.search(query, all) }
    val focus = remember { FocusRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                singleLine = true,
                label = { Text("City") },
                placeholder = { Text("Search ${all.size} cities") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(top = 8.dp)) {
                items(results, key = { it.id.id }) { entry ->
                    CatalogRow(entry, shown = entry.id in alreadyShown, onPick = { onPick(entry.id) })
                }
                if (results.isEmpty()) {
                    item { Text("No city matches \"$query\"", modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun CatalogRow(entry: ZoneEntry, shown: Boolean, onPick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.city) },
        supportingContent = {
            Text(if (shown) "${entry.region} · already on your list" else entry.region)
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            headlineColor = if (shown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.clickable(enabled = !shown, onClick = onPick),
    )
}
