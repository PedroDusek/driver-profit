package com.driverprofit.core.di

import android.content.Context
import androidx.room.Room
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.repository.OfflineVehicleRepository
import com.driverprofit.domain.repository.VehicleRepository

/**
 * Injeção de dependências manual.
 *
 * Decisão registrada (PRD §55): o projeto **não** usa Hilt/Koin no MVP. Com um
 * único módulo Gradle e um punhado de dependências, um container manual
 * resolve o problema sem custo de build (processamento de anotações) nem de
 * aprendizado. Se o grafo crescer a ponto de este arquivo ficar difícil de
 * ler, reavaliar — e documentar a troca em docs/ARCHITECTURE.md.
 */
interface AppContainer {
    val vehicleRepository: VehicleRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DriverProfitDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            DriverProfitDatabase::class.java,
            DriverProfitDatabase.NAME,
        ).build()
        // Sem fallbackToDestructiveMigration: ver DriverProfitDatabase.
    }

    override val vehicleRepository: VehicleRepository by lazy {
        OfflineVehicleRepository(database.vehicleDao())
    }
}
