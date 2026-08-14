package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.model.PersonalUsageSource
import com.driverprofit.domain.repository.ExpenseRepository
import com.driverprofit.domain.repository.PersonalUsageRepository
import com.driverprofit.domain.repository.WorkSessionRepository
import kotlinx.coroutines.flow.first
import java.time.Clock

/**
 * O que a leitura do painel explica, e o que ela não explica.
 *
 * @param odometerKilometers distância segundo o odômetro. `null` quando não há
 *   leituras suficientes — e aí não há conciliação possível.
 * @param unexplainedKilometers sobra depois de descontar jornadas e uso
 *   pessoal já declarado. Zero ou negativo significa que está tudo explicado.
 */
data class OdometerReconciliation(
    val period: DateRange,
    val vehicleId: Long,
    val odometerKilometers: Long?,
    val workKilometers: Long,
    val declaredPersonalKilometers: Long,
) {
    val unexplainedKilometers: Long?
        get() = odometerKilometers?.let {
            (it - workKilometers - declaredPersonalKilometers).coerceAtLeast(0L)
        }

    /** `true` quando há sobra a resolver com o motorista. */
    val hasUnexplained: Boolean get() = (unexplainedKilometers ?: 0L) > 0L
}

/**
 * Confere o painel contra o que foi lançado (PRD §22).
 *
 * Este é o mecanismo que faz o uso pessoal funcionar para quem esquece de
 * declarar viagem. Ele **abate** o que já foi declarado, e é isso que impede a
 * dupla contagem: sem esse desconto, a viagem de 1.200 km lançada à mão
 * apareceria de novo dentro da sobra.
 */
class ReconcileOdometerUseCase(
    private val expenseRepository: ExpenseRepository,
    private val workSessionRepository: WorkSessionRepository,
    private val personalUsageRepository: PersonalUsageRepository,
) {

    suspend operator fun invoke(vehicleId: Long, period: DateRange): OdometerReconciliation {
        val odometer = expenseRepository.odometerDistanceIn(vehicleId, period)

        val work = workSessionRepository
            .observeSessionsBetween(period.start, period.end)
            .first()
            .sumOf { it.distanceKm }

        val declared = personalUsageRepository
            .findOverlappingForVehicle(vehicleId, period)
            .sumOf { it.kilometersWithin(period) }

        return OdometerReconciliation(
            period = period,
            vehicleId = vehicleId,
            odometerKilometers = odometer,
            workKilometers = work,
            declaredPersonalKilometers = declared,
        )
    }
}

/**
 * Grava a sobra da conciliação.
 *
 * O motorista escolhe o que ela é: uso pessoal — o caso comum — ou jornada que
 * ele esqueceu de lançar. Os dois têm sinais **opostos** no custo/km, então
 * essa pergunta não pode ser presumida.
 *
 * Quando é jornada esquecida, nada é gravado aqui: o lugar de corrigir isso é
 * o formulário de ganhos, com plataforma, corridas e faturamento. Registrar a
 * distância sozinha inflaria o R$/km, que é exatamente o defeito que a v0.3.1
 * corrigiu.
 */
class SaveReconciledPersonalUsageUseCase(
    private val repository: PersonalUsageRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke(reconciliation: OdometerReconciliation): Long? {
        val unexplained = reconciliation.unexplainedKilometers
        if (unexplained == null || unexplained <= 0L) return null

        return repository.addUsage(
            PersonalUsage(
                vehicleId = reconciliation.vehicleId,
                range = reconciliation.period,
                distanceKm = unexplained,
                source = PersonalUsageSource.RECONCILED,
                createdAt = clock.instant(),
            ),
        )
    }
}
