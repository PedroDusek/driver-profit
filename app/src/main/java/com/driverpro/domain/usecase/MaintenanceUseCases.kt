package com.driverpro.domain.usecase

import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.MaintenanceMonitor
import com.driverpro.domain.model.MaintenanceSchedule
import com.driverpro.domain.model.VehicleMaintenance
import com.driverpro.expenses.domain.ExpenseRepository
import com.driverpro.domain.repository.MaintenanceScheduleRepository
import com.driverpro.vehicle.domain.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant

/**
 * Situação de manutenção de todos os veículos (ROADMAP v0.9.0).
 *
 * Um use case só para as duas telas que precisam disso — a de manutenção, que
 * mostra tudo, e o aviso do dashboard, que filtra o que pede atenção. Duas
 * consultas separadas para o mesmo cálculo acabariam divergindo justamente no
 * caso em que o motorista compara as duas.
 *
 * Carrega o histórico inteiro de despesas de propósito: o marco de uma troca de
 * pneu pode estar a dois anos e quarenta mil quilômetros, e recortar por
 * período perderia exatamente os itens de intervalo longo.
 */
class ObserveMaintenanceUseCase(
    private val vehicleRepository: VehicleRepository,
    private val expenseRepository: ExpenseRepository,
    private val scheduleRepository: MaintenanceScheduleRepository,
) {
    operator fun invoke(): Flow<List<VehicleMaintenance>> = combine(
        vehicleRepository.observeVehicles(),
        expenseRepository.observeExpenses(),
        scheduleRepository.observeAll(),
    ) { vehicles, expenses, schedules ->
        val expensesByVehicle = expenses.groupBy { it.vehicleId }
        val schedulesByVehicle = schedules.groupBy { it.vehicleId }

        vehicles.map { vehicle ->
            VehicleMaintenance(
                vehicle = vehicle,
                alerts = MaintenanceMonitor.alerts(
                    expenses = expensesByVehicle[vehicle.id].orEmpty(),
                    schedules = schedulesByVehicle[vehicle.id].orEmpty(),
                ),
            )
        }
    }
}

/** Resultado de uma tentativa de alterar o intervalo de um item. */
sealed interface SaveMaintenanceIntervalResult {

    data object Success : SaveMaintenanceIntervalResult

    /** Rejeitado. Nada foi gravado, e o intervalo anterior continua valendo. */
    data class Invalid(val error: MaintenanceValidationError) : SaveMaintenanceIntervalResult
}

/** Motivo pelo qual um intervalo foi recusado. */
enum class MaintenanceValidationError {
    REQUIRED,

    /** Zero, negativo, curto demais para ser real ou longo demais para ser útil. */
    INTERVAL_OUT_OF_RANGE,
}

/**
 * Altera o intervalo de um item para um veículo.
 *
 * A validação vive aqui, e não na tela, pelo mesmo motivo dos outros
 * validadores do projeto: ela devolve o **motivo**, e quem traduz para texto é
 * a camada de apresentação (PRD §25).
 */
class SaveMaintenanceIntervalUseCase(
    private val repository: MaintenanceScheduleRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(
        vehicleId: Long,
        item: MaintenanceItem,
        intervalKm: Long?,
        monitored: Boolean = true,
    ): SaveMaintenanceIntervalResult {
        if (intervalKm == null) {
            return SaveMaintenanceIntervalResult.Invalid(MaintenanceValidationError.REQUIRED)
        }
        if (intervalKm !in MaintenanceItem.MIN_INTERVAL_KM..MaintenanceItem.MAX_INTERVAL_KM) {
            return SaveMaintenanceIntervalResult.Invalid(
                MaintenanceValidationError.INTERVAL_OUT_OF_RANGE,
            )
        }

        repository.save(
            MaintenanceSchedule(
                vehicleId = vehicleId,
                item = item,
                intervalKm = intervalKm,
                monitored = monitored,
                createdAt = Instant.now(clock),
            ),
        )
        return SaveMaintenanceIntervalResult.Success
    }
}

/**
 * Devolve um item ao intervalo padrão do app.
 *
 * Apaga a preferência em vez de gravar o valor padrão: assim, se um padrão for
 * revisado numa versão futura, o veículo acompanha — e continua sendo possível
 * distinguir "ele escolheu 10.000" de "ele nunca mexeu".
 */
class ResetMaintenanceIntervalUseCase(
    private val repository: MaintenanceScheduleRepository,
) {
    suspend operator fun invoke(vehicleId: Long, item: MaintenanceItem) =
        repository.resetToDefault(vehicleId, item)
}
