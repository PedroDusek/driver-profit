package com.driverprofit

import android.app.Application
import com.driverprofit.core.di.AppContainer
import com.driverprofit.core.di.DefaultAppContainer

/**
 * Ponto de entrada do processo. Guarda o [AppContainer] usado para construir
 * ViewModels.
 */
class DriverProfitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
