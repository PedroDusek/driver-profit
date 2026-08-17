package com.driverprofit.testing

import com.driverprofit.domain.model.ReconciliationDismissal
import com.driverprofit.domain.repository.ReconciliationDismissalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio em memoria para testes.
 *
 * Reproduz a sobrescrita por janela, que no banco vem do indice unico.
 */
class FakeReconciliationDismissalRepository(
    initial: List<ReconciliationDismissal> = emptyList(),
) : ReconciliationDismissalRepository {

    private val dismissals = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val current: List<ReconciliationDismissal> get() = dismissals.value

    override fun observeAll(): Flow<List<ReconciliationDismissal>> = dismissals

    override suspend fun save(dismissal: ReconciliationDismissal) {
        val existing = dismissals.value.firstOrNull {
            it.vehicleId == dismissal.vehicleId && it.window == dismissal.window
        }
        dismissals.value = if (existing == null) {
            dismissals.value + dismissal.copy(id = nextId++)
        } else {
            dismissals.value.map {
                if (it.id == existing.id) dismissal.copy(id = existing.id) else it
            }
        }
    }
}
