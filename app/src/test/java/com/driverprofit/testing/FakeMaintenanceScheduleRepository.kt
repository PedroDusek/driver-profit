package com.driverprofit.testing

import com.driverprofit.domain.model.MaintenanceItem
import com.driverprofit.domain.model.MaintenanceSchedule
import com.driverprofit.domain.repository.MaintenanceScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositorio em memoria para testes de use case e ViewModel.
 *
 * Reproduz o que importa do DAO: a sobrescrita por (veiculo, item), que no
 * banco vem do indice unico, e o fato de apagar a linha significar "voltar ao
 * padrao do app".
 */
class FakeMaintenanceScheduleRepository(
    initialSchedules: List<MaintenanceSchedule> = emptyList(),
) : MaintenanceScheduleRepository {

    private val schedules = MutableStateFlow(initialSchedules)
    private var nextId = (initialSchedules.maxOfOrNull { it.id } ?: 0L) + 1

    /** Instantaneo do estado atual, para assercoes diretas nos testes. */
    val current: List<MaintenanceSchedule> get() = schedules.value

    override fun observeAll(): Flow<List<MaintenanceSchedule>> = schedules

    override fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>> =
        schedules.map { list -> list.filter { it.vehicleId == vehicleId } }

    override suspend fun save(schedule: MaintenanceSchedule) {
        val existing = schedules.value.firstOrNull {
            it.vehicleId == schedule.vehicleId && it.item == schedule.item
        }
        schedules.value = if (existing == null) {
            schedules.value + schedule.copy(id = nextId++)
        } else {
            schedules.value.map {
                if (it.id == existing.id) schedule.copy(id = existing.id) else it
            }
        }
    }

    override suspend fun resetToDefault(vehicleId: Long, item: MaintenanceItem) {
        schedules.value = schedules.value.filterNot {
            it.vehicleId == vehicleId && it.item == item
        }
    }
}
