package com.driverpro.personal.domain

import com.driverpro.core.domain.DateRange
import com.driverpro.personal.domain.PersonalUsage
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso aos lançamentos de uso pessoal (PRD §22).
 *
 * A interface vive no domínio e a implementação em `data.repository`.
 */
interface PersonalUsageRepository {

    fun observeAll(): Flow<List<PersonalUsage>>

    /**
     * Lançamentos que **encostam** no período, não apenas os contidos nele.
     *
     * Uma viagem de 28/07 a 03/08 precisa aparecer em julho e em agosto, cada
     * mês com a fatia de quilômetros que lhe cabe. O recorte proporcional é do
     * domínio, via `PersonalUsage.kilometersWithin`.
     */
    fun observeOverlapping(period: DateRange): Flow<List<PersonalUsage>>

    /** Uso pessoal já declarado de um veículo num intervalo — base da conciliação. */
    suspend fun findOverlappingForVehicle(vehicleId: Long, period: DateRange): List<PersonalUsage>

    suspend fun getUsage(id: Long): PersonalUsage?

    suspend fun addUsage(usage: PersonalUsage): Long

    suspend fun updateUsage(usage: PersonalUsage)

    suspend fun deleteUsage(id: Long)
}
