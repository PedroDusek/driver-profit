package com.driverpro

import android.app.Application
import com.driverpro.core.di.AppContainer
import com.driverpro.core.di.DefaultAppContainer

/**
 * Ponto de entrada do processo. Guarda o [AppContainer] usado para construir
 * ViewModels.
 */
class DriverProApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
