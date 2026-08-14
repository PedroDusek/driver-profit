package com.driverprofit.testing

import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.repository.PersonalUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositório em memória para testes de use case e ViewModel.
 *
 * Reproduz o que importa do `PersonalUsageDao`: ids auto-incrementais,
 * ordenação por data decrescente e — o mais fácil de errar — o filtro de
 * **sobreposição** de intervalos, e não de contenção.
 */
class FakePersonalUsageRepository(
    initialUsages: List<PersonalUsage> = emptyList(),
) : PersonalUsageRepository {

    private val usages = MutableStateFlow(initialUsages)
    private var nextId = (initialUsages.maxOfOrNull { it.id } ?: 0L) + 1

    /** Instantâneo do estado atual, para asserções diretas nos testes. */
    val current: List<PersonalUsage> get() = usages.value

    private fun List<PersonalUsage>.sorted(): List<PersonalUsage> =
        sortedWith(compareByDescending<PersonalUsage> { it.range.start }.thenByDescending { it.id })

    private fun PersonalUsage.overlaps(period: DateRange): Boolean =
        range.start <= period.end && range.end >= period.start

    override fun observeAll(): Flow<List<PersonalUsage>> = usages.map { it.sorted() }

    override fun observeOverlapping(period: DateRange): Flow<List<PersonalUsage>> =
        usages.map { list -> list.filter { it.overlaps(period) }.sorted() }

    override suspend fun findOverlappingForVehicle(
        vehicleId: Long,
        period: DateRange,
    ): List<PersonalUsage> =
        usages.value.filter { it.vehicleId == vehicleId && it.overlaps(period) }

    override suspend fun getUsage(id: Long): PersonalUsage? =
        usages.value.firstOrNull { it.id == id }

    override suspend fun addUsage(usage: PersonalUsage): Long {
        val id = nextId++
        usages.value = usages.value + usage.copy(id = id)
        return id
    }

    override suspend fun updateUsage(usage: PersonalUsage) {
        usages.value = usages.value.map { if (it.id == usage.id) usage else it }
    }

    override suspend fun deleteUsage(id: Long) {
        usages.value = usages.value.filterNot { it.id == id }
    }
}
