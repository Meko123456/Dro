package io.github.meko123456.dro

import android.app.Application
import android.content.Context
import io.github.meko123456.dro.data.SettingsRepository

/** Application-scoped object graph. Small app, no DI framework needed. */
class DroApp : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}

val Context.droApp: DroApp
    get() = applicationContext as DroApp
