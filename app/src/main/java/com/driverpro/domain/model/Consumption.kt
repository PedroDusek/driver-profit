package com.driverpro.domain.model

import com.driverpro.core.domain.FuelType
import com.driverpro.core.domain.MeasurementUnit
import com.driverpro.core.domain.Quantity
import java.time.LocalDate

/**
 * Consumo em **milésimos de quilômetro por unidade** (PRD §23).
 *
 * `8,75 km/L` é `8750`. Milésimos pelo mesmo motivo de [Quantity]: manter o
 * número inteiro até a hora de exibir, em vez de carregar erro de ponto
 * flutuante por toda a cadeia.
 *
 * A unidade não vive aqui — ela vem do `FuelType` do lançamento. É isso que
 * impede comparar km/L com km/kWh por acidente.
 */
@JvmInline
value class Consumption(val thousandths: Long) : Comparable<Consumption> {

    override fun compareTo(other: Consumption): Int = thousandths.compareTo(other.thousandths)

    /** Parte inteira — `8750` → `8`. */
    val whole: Long get() = thousandths / THOUSAND

    /** Casas decimais — `8750` → `750`. */
    val fraction: Long get() = thousandths % THOUSAND

    companion object {
        private const val THOUSAND = 1_000L

        /**
         * Quilômetros por unidade consumida.
         *
         * Devolve `null` quando não há como dividir — mesma regra de
         * `Money.per` (PRD §21). Um trecho sem distância ou sem quantidade não
         * tem consumo igual a zero: ele simplesmente não tem consumo.
         */
        fun of(distanceKm: Long, quantity: Quantity): Consumption? {
            if (distanceKm <= 0L || quantity.isZero) return null
            val units = quantity.toUnits()
            if (units <= 0.0) return null
            return Consumption(Math.round(distanceKm * THOUSAND / units))
        }
    }
}

/**
 * Consumo estimado de um abastecimento (PRD §23).
 *
 * @param distanceKm quilômetros percorridos desde o abastecimento anterior.
 * @param quantity o que entrou no tanque **neste** abastecimento — que é o que
 *   repõe o consumido no trecho.
 */
data class ConsumptionEstimate(
    val expenseId: Long,
    val date: LocalDate,
    val fuelType: FuelType,
    val distanceKm: Long,
    val quantity: Quantity,
    val consumption: Consumption,
) {
    val unit: MeasurementUnit get() = fuelType.unit
}

/**
 * Calcula consumo pelo método tanque-a-tanque (PRD §23).
 *
 * A distância vem da diferença de odômetro entre dois abastecimentos
 * consecutivos, e o divisor é a quantidade do **segundo** — é ele que repõe o
 * que foi queimado no trecho.
 *
 * O resultado é sempre **estimado**, e o PRD é explícito quanto a rotulá-lo
 * assim: o número só seria exato se os dois abastecimentos tivessem enchido o
 * tanque nas mesmas condições, e ninguém garante isso na vida real.
 */
object ConsumptionEstimator {

    /**
     * Estimativas a partir dos abastecimentos de **um veículo**.
     *
     * Três coisas são exigidas de cada par, e todas descartam o par em
     * silêncio quando faltam:
     *
     * 1. **Odômetro nos dois.** Obrigatório desde a v0.6.0, mas o histórico
     *    anterior não tem.
     * 2. **Quantidade no segundo.** Opcional desde a v0.4.1 — sem ela não há
     *    divisor.
     * 3. **Mesmo combustível nos dois.** Num flex, alternar gasolina e etanol
     *    muda o consumo em cerca de 30%; misturar os dois num número só
     *    produziria uma média que não descreve nenhum dos dois. Comparar
     *    consumo por combustível é assunto pós-MVP (PRD §9).
     */
    fun estimate(refuels: List<Expense>): List<ConsumptionEstimate> {
        val usable = refuels
            .filter { it.category == ExpenseCategory.FUEL || it.category == ExpenseCategory.CHARGING }
            .filter { it.odometerKm != null }
            // Odômetro, e não data: lançar hoje o abastecimento da semana
            // passada é comum, e a ordem física do carro é a do painel.
            .sortedBy { it.odometerKm }

        return usable.zipWithNext().mapNotNull { (previous, current) ->
            val previousOdometer = previous.odometerKm ?: return@mapNotNull null
            val currentOdometer = current.odometerKm ?: return@mapNotNull null
            val quantity = current.quantity ?: return@mapNotNull null

            val fuelType = current.fuelTypeOrNull ?: return@mapNotNull null
            if (previous.fuelTypeOrNull != fuelType) return@mapNotNull null

            val distance = currentOdometer - previousOdometer
            val consumption = Consumption.of(distance, quantity) ?: return@mapNotNull null

            ConsumptionEstimate(
                expenseId = current.id,
                date = current.date,
                fuelType = fuelType,
                distanceKm = distance,
                quantity = quantity,
                consumption = consumption,
            )
        }
    }
}

/**
 * Combustível efetivo do lançamento — energia elétrica no caso da recarga.
 *
 * `internal` porque o piso de distância dos alertas de manutenção (v0.9.0)
 * precisa da mesma regra: um lançamento só entra na conta de combustível
 * comprado se for possível dizer **qual** combustível era.
 */
internal val Expense.fuelTypeOrNull: FuelType?
    get() = when (val detail = detail) {
        is ExpenseDetail.Refuel -> detail.fuelType
        is ExpenseDetail.Charging -> FuelType.ELECTRICITY
        else -> null
    }
