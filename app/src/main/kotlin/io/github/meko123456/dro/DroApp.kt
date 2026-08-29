package io.github.meko123456.dro

import android.app.Application
import android.content.Context
import io.github.meko123456.dro.data.SettingsRepository
import io.github.meko123456.dro.widget.DroWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Application-scoped object graph. Small app, no DI framework needed. */
class DroApp : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Re-render placed widgets whenever the city list changes. The first emission is the
        // current value, which the widgets already show; only changes matter.
        scope.launch {
            settings.settings.drop(1).collect { DroWidget.updateAll(this@DroApp, it) }
        }
    }
}

val Context.droApp: DroApp
    get() = applicationContext as DroApp
