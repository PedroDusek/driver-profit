package com.driverpro.domain.usecase

import com.driverpro.core.domain.DateRange
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageSource
import com.driverpro.domain.model.ReconciliationDismissal
import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.repository.ExpenseRepository
import com.driverpro.domain.repository.PersonalUsageRepository
import com.driverpro.domain.repository.ReconciliationDismissalRepository
import com.driverpro.domain.repository.VehicleRepository
import com.driverpro.domain.repository.WorkSessionRepository
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
     * Conciliação de **todas** as janelas de um veículo, da mais antiga para a
     * mais recente.
     *
     * Todas, e não apenas a última: lançar dois abastecimentos antes de abrir o
     * app fecha duas janelas, e conferir só a mais nova abandonaria a anterior
     * para sempre. Isso vale para quem lança em lote — semanalmente, por
     * exemplo — e para quem preenche histórico depois.
     *
     * As já resolvidas saem da lista sozinhas: o uso pessoal gravado por uma
     * conciliação anterior é descontado como declarado, e a sobra da janela
     * volta a zero.
     *
     * @param expenses despesas **do veículo**.
     * @param sessions jornadas de trabalho. Elas não têm veículo (PRD §15), o
     *   que é simplificação assumida do MVP: com um carro por motorista, toda
     *   jornada da janela é daquele carro.
     */
    fun pending(
        expenses: List<Expense>,
        sessions: List<WorkSession>,
        personalUsage: List<PersonalUsage>,
        dismissals: List<ReconciliationDismissal>,
        vehicleId: Long,
    ): List<OdometerReconciliation> =
        windows(expenses, sessions, personalUsage, vehicleId).filter { it.isPending(dismissals) }

    /**
     * **Todas** as janelas de um veículo, sem filtro de política.
     *
     * Separada de [pending] de propósito: aqui vive o cálculo, lá vive a
     * decisão de o que mostrar. A distinção importa porque a sobra negativa
     * continua existindo e sendo preservada — é ela que faz janelas
     * encadeadas se cancelarem — mesmo tendo deixado de ser exibida.
     */
    fun windows(
        expenses: List<Expense>,
        sessions: List<WorkSession>,
        personalUsage: List<PersonalUsage>,
        vehicleId: Long,
    ): List<OdometerReconciliation> {
        // Ordenado por leitura, e não por data: odômetro só cresce, e lançar
        // hoje a nota da semana passada é rotina.
        val readings = expenses
            .filter { it.vehicleId == vehicleId && it.odometerKm != null }
            .sortedBy { it.odometerKm }

        if (readings.size < 2) return emptyList()

        return readings.zipWithNext { previous, current ->
            val previousOdometer = previous.odometerKm ?: return@zipWithNext null
            val currentOdometer = current.odometerKm ?: return@zipWithNext null

            val window = windowBetween(previous.date, current.date)

            val work = sessions
                .filter { it.date >= window.start && it.date <= window.end }
                .sumOf { it.distanceKm }

            val declared = personalUsage
                .filter { it.range.start <= window.end && it.range.end >= window.start }
                .sumOf { it.kilometersWithin(window) }

            OdometerReconciliation(
                period = window,
                vehicleId = vehicleId,
                odometerKilometers = currentOdometer - previousOdometer,
                workKilometers = work,
                declaredPersonalKilometers = declared,
            )
        }.filterNotNull()
    }

    /**
     * Se esta janela ainda precisa de resposta do motorista.
     *
     * **Sobra negativa nunca pergunta.** Ela significa que os lançamentos somam
     * mais que o painel, e isso não é distância faltando: é inconsistência
     * entre dois números do próprio motorista, sem nada a classificar. Como o
     * app **é** a anotação dele, não existe fonte contra a qual conferir — e
     * alerta sem ação possível vira ruído que ensina a fechar aviso sem ler,
     * gastando a atenção que os alertas úteis precisam ter.
     *
     * O efeito de ignorá-la é misto e pequeno: o ganho/km sai pessimista (mais
     * quilômetros no divisor) e o custo/km sai otimista, ambos na proporção da
     * sobra, e limitados àquela janela porque não há saldo global.
     *
     * **Sobra positiva respeita a dispensa, até o valor dispensado.** Quem
     * aceitou deixar 15 km de fora aceitou aquele fato; se um lançamento
     * retroativo explicar parte deles e a sobra cair para 5, ela cabe no que já
     * foi aceito e o app segue quieto. Se a sobra **crescer** além dos 15,
     * apareceu distância nova sobre a qual ele não opinou, e a pergunta volta.
     */
    private fun OdometerReconciliation.isPending(
        dismissals: List<ReconciliationDismissal>,
    ): Boolean {
        val unexplained = unexplainedKilometers ?: return false
        if (unexplained <= 0L) return false

        val dismissed = dismissals
            .firstOrNull { it.vehicleId == vehicleId && it.window == period }
            ?.dismissedKm
            ?: 0L

        return unexplained > dismissed
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
    private val dismissalRepository: ReconciliationDismissalRepository,
) {
    operator fun invoke(): Flow<List<VehicleReconciliation>> = combine(
        vehicleRepository.observeVehicles(),
        expenseRepository.observeExpenses(),
        workSessionRepository.observeSessions(),
        personalUsageRepository.observeAll(),
        dismissalRepository.observeAll(),
    ) { vehicles, expenses, sessions, personalUsage, dismissals ->
        vehicles.flatMap { vehicle ->
            OdometerWindow.pending(expenses, sessions, personalUsage, dismissals, vehicle.id)
                .map { VehicleReconciliation(vehicle, it) }
        }
    }
}

/**
 * Aceita a sobra fora da conta.
 *
 * Os quilômetros não viram uso pessoal nem trabalho: ficam fora de todos os
 * totais, e o custo por km segue um pouco mais alto que o real. É decisão
 * consciente, e a tela diz isso antes de o motorista tomá-la.
 */
class DismissReconciliationUseCase(
    private val repository: ReconciliationDismissalRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke(reconciliation: OdometerReconciliation) {
        val unexplained = reconciliation.unexplainedKilometers ?: return
        if (unexplained <= 0L) return

        repository.save(
            ReconciliationDismissal(
                vehicleId = reconciliation.vehicleId,
                window = reconciliation.period,
                dismissedKm = unexplained,
                createdAt = clock.instant(),
            ),
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
