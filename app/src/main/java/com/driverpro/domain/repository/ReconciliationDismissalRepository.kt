package com.driverpro.domain.repository

import com.driverpro.domain.model.ReconciliationDismissal
import kotlinx.coroutines.flow.Flow

/** Contrato de acesso às sobras aceitas fora da conta (PRD §22). */
interface ReconciliationDismissalRepository {

    fun observeAll(): Flow<List<ReconciliationDismissal>>

    suspend fun save(dismissal: ReconciliationDismissal)
}
