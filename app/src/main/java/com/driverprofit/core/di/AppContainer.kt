package com.driverprofit.core.di

import android.content.Context
import androidx.room.Room
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.database.Migrations
import com.driverprofit.data.repository.OfflineVehicleRepository
import com.driverprofit.domain.repository.VehicleRepository
import com.driverprofit.domain.usecase.DeleteVehicleUseCase
import com.driverprofit.domain.usecase.GetVehicleUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.domain.usecase.SaveVehicleUseCase
import com.driverprofit.domain.usecase.VehicleValidator

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

    val saveVehicle: SaveVehicleUseCase
    val observeVehicles: ObserveVehiclesUseCase
    val getVehicle: GetVehicleUseCase
    val deleteVehicle: DeleteVehicleUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DriverProfitDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            DriverProfitDatabase::class.java,
            DriverProfitDatabase.NAME,
        )
            .addMigrations(*Migrations.ALL)
            // Sem fallbackToDestructiveMigration: ver DriverProfitDatabase.
            .build()
    }

    override val vehicleRepository: VehicleRepository by lazy {
        OfflineVehicleRepository(database.vehicleDao())
    }

    private val vehicleValidator = VehicleValidator()

    override val saveVehicle: SaveVehicleUseCase by lazy {
        SaveVehicleUseCase(vehicleRepository, vehicleValidator)
    }

    override val observeVehicles: ObserveVehiclesUseCase by lazy {
        ObserveVehiclesUseCase(vehicleRepository)
    }

    override val getVehicle: GetVehicleUseCase by lazy {
        GetVehicleUseCase(vehicleRepository)
    }

    override val deleteVehicle: DeleteVehicleUseCase by lazy {
        DeleteVehicleUseCase(vehicleRepository)
    }
}
