package io.github.meko123456.dro.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.meko123456.dro.domain.OverlapFinder
import io.github.meko123456.dro.domain.Segment
import io.github.meko123456.dro.domain.TimeFormat
import java.time.LocalDate
import java.time.ZoneId

/** One bar of the strip: a zone's working hours on the home-day axis. */
data class OverlapBar(val row: ClockRow, val segments: List<Segment>)

/**
 * A 24-hour bar per zone on the home zone's axis, working hours filled, with the window
 * where everyone is at work drawn across all bars and a marker at the current minute.
 * Tapping a bar edits that zone's working hours.
 */
@Composable
fun OverlapStrip(
    date: LocalDate,
    home: ZoneId,
    dayLengthMinutes: Int,
    nowMinute: Int,
    bars: List<OverlapBar>,
    shared: List<Segment>,
    twentyFourHour: Boolean,
    onEditHours: (ClockRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedLabel = axisLabel(date, home, shared, twentyFourHour)
    val caption = if (sharedLabel != null) {
        "Everyone's working $sharedLabel your time"
    } else {
        "No shared working hours today"
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Working hours today", style = MaterialTheme.typography.titleMedium)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = if (sharedLabel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            bars.forEach { bar ->
                StripRow(bar, date, home, dayLengthMinutes, nowMinute, shared, twentyFourHour, onClick = { onEditHours(bar.row) })
            }
            HourAxis(date, home, dayLengthMinutes, twentyFourHour)
        }
    }
}

@Composable
private fun StripRow(
    bar: OverlapBar,
    date: LocalDate,
    home: ZoneId,
    dayLengthMinutes: Int,
    nowMinute: Int,
    shared: List<Segment>,
    twentyFourHour: Boolean,
    onClick: () -> Unit,
) {
    val hours = bar.row.hours
    val local = "${TimeFormat.compact(hours.start, twentyFourHour)}–${TimeFormat.compact(hours.end, twentyFourHour)}"
    val spokenLocal = "${TimeFormat.spoken(hours.start, twentyFourHour)}–${TimeFormat.spoken(hours.end, twentyFourHour)}"
    val onAxis = axisLabel(date, home, bar.segments, twentyFourHour)
    val description = buildString {
        append(bar.row.entry.city)
        append(" works ")
        append(spokenLocal)
        append(" local")
        if (onAxis != null) append(", $onAxis your time") else append(", not today")
    }
    val fill = if (bar.row.zone == home) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val overlay = SharedWindowColor(isSystemInDarkTheme()).copy(alpha = 0.8f)
    val marker = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit working hours", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(100.dp)) {
            Text(bar.row.entry.city, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(local, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Canvas(modifier = Modifier.weight(1f).height(22.dp)) {
            val perMinute = size.width / dayLengthMinutes
            drawRoundRect(color = track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            bar.segments.forEach { drawSegment(it, perMinute, fill) }
            shared.forEach { drawSegment(it, perMinute, overlay) }
            if (nowMinute in 0 until dayLengthMinutes) {
                val x = nowMinute * perMinute
                drawLine(marker, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegment(segment: Segment, perMinute: Float, color: Color) {
    val left = segment.startMinute * perMinute
    val width = (segment.endMinute - segment.startMinute) * perMinute
    drawRect(color = color, topLeft = Offset(left, 0f), size = Size(width, size.height))
}

/** Hour labels under the bars, in home wall-clock time (so a DST day labels honestly). */
@Composable
private fun HourAxis(date: LocalDate, home: ZoneId, dayLengthMinutes: Int, twentyFourHour: Boolean) {
    val ticks = listOf(0, 360, 720, 1080, dayLengthMinutes)
    Row(modifier = Modifier.fillMaxWidth().padding(start = 108.dp)) {
        ticks.forEachIndexed { index, minute ->
            val label = when {
                minute == dayLengthMinutes -> if (twentyFourHour) "24" else "12am"
                else -> hourLabel(OverlapFinder.wallTime(date, home, minute).hour, twentyFourHour)
            }
            Box(modifier = if (index < ticks.lastIndex) Modifier.weight(1f) else Modifier) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * The shared-window band must read against both the primary (home) and secondary (city) bars
 * under any dynamic-color scheme, so it is a fixed amber rather than a theme role.
 */
@Suppress("FunctionName")
private fun SharedWindowColor(dark: Boolean): Color = if (dark) Color(0xFFFFC46B) else Color(0xFFE08A00)

/** `9:00 am–4:30 pm` / `09:00–16:30` per segment, joined with ", "; null when empty. */
private fun axisLabel(date: LocalDate, home: ZoneId, segments: List<Segment>, twentyFourHour: Boolean): String? {
    if (segments.isEmpty()) return null
    return segments.joinToString(", ") {
        val from = OverlapFinder.wallTime(date, home, it.startMinute)
        val to = OverlapFinder.wallTime(date, home, it.endMinute)
        "${TimeFormat.spoken(from, twentyFourHour)}–${TimeFormat.spoken(to, twentyFourHour)}"
    }
}

private fun hourLabel(hour: Int, twentyFourHour: Boolean): String = when {
    twentyFourHour -> hour.toString().padStart(2, '0')
    hour == 0 -> "12am"
    hour < 12 -> "${hour}am"
    hour == 12 -> "12pm"
    else -> "${hour - 12}pm"
}
