package com.driverpro.maintenance.domain

import com.driverpro.core.domain.FuelType
import com.driverpro.expenses.domain.Consumption
import com.driverpro.expenses.domain.ConsumptionEstimator
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.ExpenseDetail
import com.driverpro.expenses.domain.fuelTypeOrNull

/**
 * Decide a situação de cada item de manutenção a partir do que foi lançado.
 *
 * Kotlin puro, como todo cálculo que o produto não pode errar (PRD §29): dá
 * para exercitar cada regra abaixo com JUnit, sem aparelho.
 *
 * ### A assimetria que rege esta versão
 *
 * As outras funcionalidades degradam com elegância — sem uso pessoal, o
 * custo/km sai pessimista e a tela avisa. Aqui não dá para fazer o mesmo:
 * subestimar quilometragem **atrasa** o alerta de troca de óleo, e óleo velho
 * desgasta motor. Errar para menos custa dinheiro de verdade; errar para mais
 * custa uma troca antecipada.
 *
 * Daí as duas regras que diferenciam este cálculo de todos os outros do app:
 *
 * 1. **Piso por combustível comprado.** Litros multiplicados pelo consumo
 *    histórico dão uma distância que o carro necessariamente percorreu,
 *    independente de o motorista atualizar o painel. Quando esse piso passa da
 *    diferença de odômetro, é ele que vale, e o alerta se declara estimado.
 * 2. **Sem marco, sem afirmação.** Nunca se supõe que um item está em dia
 *    porque não há registro dele. A ausência vira
 *    [MaintenanceStatus.UNKNOWN], que a tela mostra como pendência de dado —
 *    não como tranquilidade.
 */
object MaintenanceMonitor {

    /** Fração final do intervalo em que o item passa a avisar. */
    private const val DUE_SOON_FRACTION = 10L

    /**
     * Piso da banda de aviso, em quilômetros.
     *
     * Existe porque a banda precisa ser maior que a defasagem que o painel
     * consegue acumular. Como o odômetro é obrigatório em todo abastecimento, a
     * defasagem máxima é um tanque — algo entre 300 e 500 km. Com 10% de um
     * intervalo de 5.000 km, a banda seria 500 km e a margem real cairia para
     * cem e poucos; num intervalo mais curto, o lembrete chegaria **depois** do
     * vencimento, que é exatamente o que esta versão existe para evitar.
     */
    private const val MIN_DUE_SOON_KM = 1_000L

    /**
     * Teto da banda, como fração do intervalo.
     *
     * Sem ele, um intervalo curto ficaria permanentemente em aviso, e um alerta
     * que está sempre ligado não é alerta.
     */
    private const val MAX_DUE_SOON_FRACTION = 2L

    private const val THOUSAND = 1_000L

    /**
     * Situação de cada item acompanhado de **um** veículo.
     *
     * @param expenses todas as despesas do veículo — manutenções dão o marco,
     *   abastecimentos dão o piso, e qualquer lançamento com leitura serve de
     *   quilometragem corrente.
     * @param schedules o que o motorista alterou. Item sem registro usa
     *   [MaintenanceItem.defaultIntervalKm]; item com `monitored = false` fica
     *   de fora do resultado.
     */
    fun alerts(
        expenses: List<Expense>,
        schedules: List<MaintenanceSchedule>,
    ): List<MaintenanceAlert> {
        val overrides = schedules.associateBy { it.item }
        val currentOdometer = expenses.mapNotNull { it.odometerKm }.maxOrNull()
        val consumptionByFuel = historicConsumption(expenses)

        return MaintenanceItem.entries.map { item ->
            val override = overrides[item]

            alertFor(
                item = item,
                intervalKm = override?.intervalKm ?: item.defaultIntervalKm,
                monitored = override?.monitored ?: true,
                expenses = expenses,
                currentOdometer = currentOdometer,
                consumptionByFuel = consumptionByFuel,
            )
        }
    }

    private fun alertFor(
        item: MaintenanceItem,
        intervalKm: Long,
        monitored: Boolean,
        expenses: List<Expense>,
        currentOdometer: Long?,
        consumptionByFuel: Map<FuelType, Consumption>,
    ): MaintenanceAlert {
        val marker = lastService(expenses, item)
        val markerOdometer = marker?.odometerKm

        if (marker == null || markerOdometer == null || currentOdometer == null) {
            return MaintenanceAlert(
                item = item,
                intervalKm = intervalKm,
                lastServiceKm = null,
                lastServiceDate = marker?.date,
                traveledKm = null,
                status = MaintenanceStatus.UNKNOWN,
                monitored = monitored,
            )
        }

        // Nunca negativo: o marco é a maior leitura da categoria, e o corrente
        // é a maior de todas, então o corrente é no mínimo igual a ele.
        val byOdometer = currentOdometer - markerOdometer
        val byFuel = impliedDistance(expenses, marker, consumptionByFuel)

        val traveled = maxOf(byOdometer, byFuel)

        return MaintenanceAlert(
            item = item,
            intervalKm = intervalKm,
            lastServiceKm = markerOdometer,
            lastServiceDate = marker.date,
            traveledKm = traveled,
            status = statusFor(traveled, intervalKm),
            distanceIsImplied = byFuel > byOdometer,
            monitored = monitored,
        )
    }

    private fun statusFor(traveledKm: Long, intervalKm: Long): MaintenanceStatus {
        val remaining = intervalKm - traveledKm
        return when {
            remaining <= 0L -> MaintenanceStatus.OVERDUE
            remaining <= dueSoonBand(intervalKm) -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.OK
        }
    }

    /** Com quantos quilômetros de folga o lembrete começa a aparecer. */
    private fun dueSoonBand(intervalKm: Long): Long = minOf(
        maxOf(intervalKm / DUE_SOON_FRACTION, MIN_DUE_SOON_KM),
        intervalKm / MAX_DUE_SOON_FRACTION,
    )

    /**
     * Último serviço lançado na categoria do item.
     *
     * Escolhido pela **maior leitura**, e não pela data mais recente, pelo
     * mesmo motivo de `ExpenseDao.observeLatestOdometer`: odômetro só cresce, e
     * lançar hoje a nota da semana passada é rotina. Ordenar por data devolveria
     * um marco atrás do real, o que adiantaria o alerta em vez de atrasá-lo —
     * mas ainda assim seria um número que não corresponde ao carro.
     */
    private fun lastService(expenses: List<Expense>, item: MaintenanceItem): Expense? =
        expenses
            .filter { it.category == ExpenseCategory.MAINTENANCE && it.odometerKm != null }
            .filter { (it.detail as? ExpenseDetail.Maintenance)?.category == item.category }
            .maxByOrNull { it.odometerKm ?: 0L }

    /**
     * Distância mínima que o combustível comprado desde o serviço comprova
     * (PRD §23).
     *
     * Os abastecimentos são recortados por **data**, e não por odômetro, de
     * propósito: o piso existe justamente para o caso em que a leitura do
     * painel está desatualizada, e filtrar por ela devolveria o defeito para
     * dentro da correção.
     *
     * Duas fontes de folga, ambas empurrando o alerta para **antes** e não para
     * depois, que é o lado seguro desta versão:
     *
     * - um abastecimento no mesmo dia do serviço, porém anterior a ele, entra
     *   na conta;
     * - o que está no tanque agora ainda não virou distância.
     *
     * Abastecimento sem quantidade, ou de um combustível sem histórico de
     * consumo, contribui com zero. O piso é um piso: dado que falta o abaixa,
     * nunca o inventa.
     */
    private fun impliedDistance(
        expenses: List<Expense>,
        marker: Expense,
        consumptionByFuel: Map<FuelType, Consumption>,
    ): Long = expenses
        .filter { it.id != marker.id && !it.date.isBefore(marker.date) }
        .sumOf { expense ->
            val quantity = expense.quantity ?: return@sumOf 0L
            val fuelType = expense.fuelTypeOrNull ?: return@sumOf 0L
            val consumption = consumptionByFuel[fuelType] ?: return@sumOf 0L
            Math.round(quantity.toUnits() * consumption.thousandths / THOUSAND)
        }

    /**
     * Consumo típico do veículo por combustível, vindo do estimador da v0.8.0.
     *
     * **Mediana, e não média nem máximo.** Um par tanque-a-tanque estragado —
     * abastecimento parcial, odômetro digitado errado — desloca a média e
     * domina o máximo; a mediana o ignora. E como o resultado só é usado dentro
     * de um `max` contra o odômetro, subestimar aqui devolve o comportamento
     * antigo em vez de produzir número errado.
     */
    private fun historicConsumption(expenses: List<Expense>): Map<FuelType, Consumption> =
        ConsumptionEstimator.estimate(expenses)
            .groupBy { it.fuelType }
            .mapValues { (_, estimates) -> median(estimates.map { it.consumption }) }

    private fun median(values: List<Consumption>): Consumption {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            Consumption((sorted[middle - 1].thousandths + sorted[middle].thousandths) / 2)
        }
    }
}
