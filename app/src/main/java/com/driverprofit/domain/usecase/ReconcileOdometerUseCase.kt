package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.model.PersonalUsageSource
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.repository.ExpenseRepository
import com.driverprofit.domain.repository.PersonalUsageRepository
import com.driverprofit.domain.repository.VehicleRepository
import com.driverprofit.domain.repository.WorkSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock

/**
 * O que a leitura do painel explica, e o que ela não explica.
 *
 * @param odometerKilometers distância segundo o painel na janela. `null` quando
 *   não há duas leituras para comparar — e aí não há conciliação possível.
 * @param unexplainedKilometers sobra depois de descontar jornadas e uso pessoal
 *   já declarado. **Pode ser negativa**, e isso é informação, não erro a
 *   esconder.
 */
data class OdometerReconciliation(
    val period: DateRange,
    val vehicleId: Long,
    val odometerKilometers: Long?,
    val workKilometers: Long,
    val declaredPersonalKilometers: Long,
) {
    val unexplainedKilometers: Long?
        get() = odometerKilometers?.let { it - workKilometers - declaredPersonalKilometers }

    /** `true` quando há sobra a transformar em uso pessoal. */
    val hasUnexplained: Boolean get() = (unexplainedKilometers ?: 0L) > 0L

    /**
     * `true` quando o lançado é **maior** que o painel.
     *
     * Não é uso pessoal negativo: é sinal de que um número foi lançado errado —
     * km de jornada inflado, ou leitura digitada baixa. Antes da v0.9.1 isso era
     * zerado em silêncio, o que além de esconder o erro quebrava o cancelamento
     * entre janelas encadeadas: uma jornada alocada na janela errada inflava uma
     * sobra e devia desinflar a seguinte, e o piso em zero transformava o
     * desequilíbrio passageiro em quilometragem pessoal gravada para sempre.
     */
    val hasDivergence: Boolean get() = (unexplainedKilometers ?: 0L) < 0L
}

/** Um veículo e a conciliação pendente dele. */
data class VehicleReconciliation(
    val vehicle: Vehicle,
    val reconciliation: OdometerReconciliation,
)

/**
 * Confere o painel contra o lançado, na janela **entre duas leituras**
 * (PRD §22).
 *
 * O calendário não sabe nada sobre o carro. O único intervalo em que a
 * diferença de odômetro é um fato é o que vai de uma leitura à seguinte —
 * amarrar a conciliação ao mês obrigaria a estimar o que aconteceu quando o mês
 * termina no meio de dois abastecimentos.
 *
 * Como o odômetro é obrigatório em abastecimento, recarga e manutenção, a
 * leitura chega sozinha, no ritmo em que o motorista usa o carro. Isso torna a
 * cadência de lançamento irrelevante: diariamente, semanalmente ou depois de um
 * mês sumido produzem a mesma conta, sem caso especial.
 *
 * Kotlin puro para poder ser exercitado sem banco.
 */
object OdometerWindow {

    /**
     * Conciliação da última janela de um veículo, ou `null` quando não há duas
     * leituras para comparar.
     *
     * @param expenses despesas **do veículo**.
     * @param sessions jornadas de trabalho. Elas não têm veículo (PRD §15), o
     *   que é simplificação assumida do MVP: com um carro por motorista, toda
     *   jornada da janela é daquele carro.
     */
    fun latest(
        expenses: List<Expense>,
        sessions: List<WorkSession>,
        personalUsage: List<PersonalUsage>,
        vehicleId: Long,
    ): OdometerReconciliation? {
        // Ordenado por leitura, e não por data: odômetro só cresce, e lançar
        // hoje a nota da semana passada é rotina.
        val readings = expenses
            .filter { it.vehicleId == vehicleId && it.odometerKm != null }
            .sortedBy { it.odometerKm }

        if (readings.size < 2) return null

        val previous = readings[readings.size - 2]
        val current = readings.last()

        val previousOdometer = previous.odometerKm ?: return null
        val currentOdometer = current.odometerKm ?: return null

        val window = windowBetween(previous.date, current.date)

        val work = sessions
            .filter { it.date >= window.start && it.date <= window.end }
            .sumOf { it.distanceKm }

        val declared = personalUsage
            .filter { it.range.start <= window.end && it.range.end >= window.start }
            .sumOf { it.kilometersWithin(window) }

        return OdometerReconciliation(
            period = window,
            vehicleId = vehicleId,
            odometerKilometers = currentOdometer - previousOdometer,
            workKilometers = work,
            declaredPersonalKilometers = declared,
        )
    }

    /**
     * A janela vai do dia **seguinte** à leitura anterior até o dia da leitura
     * nova.
     *
     * Começar no dia seguinte é o que impede janelas consecutivas de contarem a
     * mesma jornada duas vezes: a jornada do dia da leitura anterior pertence à
     * janela que terminou nela.
     *
     * Duas leituras no mesmo dia caem no caso degenerado, e aí a janela é esse
     * único dia. A jornada daquele dia pode ser contada nas duas janelas, o que
     * **encolhe** a sobra — errar para menos devolve o comportamento antigo, em
     * vez de inventar quilometragem pessoal que ninguém rodou.
     */
    private fun windowBetween(
        previousDate: java.time.LocalDate,
        currentDate: java.time.LocalDate,
    ): DateRange {
        val start = if (currentDate.isAfter(previousDate)) previousDate.plusDays(1) else currentDate
        return DateRange(start, currentDate)
    }
}

/**
 * Conciliação pendente de cada veículo, reagindo ao banco.
 *
 * É isso que faz o ciclo girar sozinho: quando o motorista lança o
 * abastecimento com a leitura nova, a divergência aparece na tela sem ele
 * precisar procurar um botão em outro lugar — que era o defeito da v0.7.0,
 * onde a conciliação existia mas dependia de alguém saber que existia.
 */
class ObserveOdometerReconciliationUseCase(
    private val vehicleRepository: VehicleRepository,
    private val expenseRepository: ExpenseRepository,
    private val workSessionRepository: WorkSessionRepository,
    private val personalUsageRepository: PersonalUsageRepository,
) {
    operator fun invoke(): Flow<List<VehicleReconciliation>> = combine(
        vehicleRepository.observeVehicles(),
        expenseRepository.observeExpenses(),
        workSessionRepository.observeSessions(),
        personalUsageRepository.observeAll(),
    ) { vehicles, expenses, sessions, personalUsage ->
        vehicles.mapNotNull { vehicle ->
            OdometerWindow.latest(expenses, sessions, personalUsage, vehicle.id)
                ?.takeIf { it.hasUnexplained || it.hasDivergence }
                ?.let { VehicleReconciliation(vehicle, it) }
        }
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
