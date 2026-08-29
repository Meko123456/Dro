package io.github.meko123456.dro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.github.meko123456.dro.MainActivity
import io.github.meko123456.dro.R
import io.github.meko123456.dro.domain.Settings
import io.github.meko123456.dro.domain.ZoneCatalog
import io.github.meko123456.dro.domain.ZoneClock
import io.github.meko123456.dro.droApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Home + up to three cities as `TextClock`s. The framework keeps each clock current on its
 * own (`updatePeriodMillis` is 0), so the only work here is re-rendering when the user's
 * city list changes or the launcher asks; there are no alarms and nothing runs in the
 * background between updates.
 */
class DroWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = context.droApp.settings.settings.first()
                val views = render(context, settings)
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val ROWS = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4)
        private val CITIES = intArrayOf(R.id.city1, R.id.city2, R.id.city3, R.id.city4)
        private val OFFSETS = intArrayOf(R.id.offset1, R.id.offset2, R.id.offset3, R.id.offset4)
        private val CLOCKS = intArrayOf(R.id.clock1, R.id.clock2, R.id.clock3, R.id.clock4)

        /** Push the current settings to every placed widget; no-op when none is placed. */
        fun updateAll(context: Context, settings: Settings) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DroWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context, settings)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        fun render(context: Context, settings: Settings): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_dro)
            val zones = (listOf(settings.home) + settings.cities).take(ROWS.size)
            val now = Instant.now()
            ROWS.indices.forEach { i ->
                val zone = zones.getOrNull(i)
                if (zone == null) {
                    views.setViewVisibility(ROWS[i], View.GONE)
                    return@forEach
                }
                views.setViewVisibility(ROWS[i], View.VISIBLE)
                val entry = ZoneCatalog.entryFor(zone)
                views.setTextViewText(CITIES[i], if (i == 0) "${entry.city} · home" else entry.city)
                val offset = ZoneClock.read(now, settings.home, zone).offsetMinutes
                views.setTextViewText(OFFSETS[i], if (i == 0) "" else ZoneClock.offsetLabel(offset))
                // TextClock.setTimeZone is @RemotableViewMethod: the launcher's clock ticks in this zone.
                views.setString(CLOCKS[i], "setTimeZone", zone.id)
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        /** Ask the launcher to place a widget (Android 8+ pin flow). False if unsupported. */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(ComponentName(context, DroWidget::class.java), null, null)
        }
    }
}
