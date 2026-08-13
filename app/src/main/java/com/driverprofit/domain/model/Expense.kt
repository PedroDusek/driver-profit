package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import java.time.Instant
import java.time.LocalDate

/**
 * Campos específicos de cada natureza de despesa.
 *
 * Modelado como `sealed` para que seja impossível construir um abastecimento
 * sem combustível ou uma recarga com posto de gasolina. No banco tudo isso
 * vira colunas anuláveis de uma tabela só (PRD §17: nada de estrutura rígida
 * que impeça categorias novas), mas o domínio não precisa herdar essa
 * frouxidão.
 */
sealed interface ExpenseDetail {

    /**
     * Abastecimento — combustível líquido ou gasoso (PRD §7 a §10).
     *
     * [quantity] é opcional: o indicador principal do produto é **custo/km**,
     * que sai do valor pago e dos quilômetros rodados, sem depender de quantos
     * litros entraram no tanque. Exigir a quantidade só para calcular R$/litro
     * cobraria um dado a cada abastecimento em troca de um número secundário.
     *
     * Quando informada, ela habilita o R$/litro (ou R$/m³) daquele
     * abastecimento.
     */
    data class Refuel(
        val fuelType: FuelType,
        val quantity: Quantity? = null,
        val station: String = "",
    ) : ExpenseDetail

    /** Recarga elétrica (PRD §11). [energy] é opcional, como em [Refuel]. */
    data class Charging(
        val energy: Quantity? = null,
        val location: ChargingLocation,
        val place: String = "",
    ) : ExpenseDetail

    /** Manutenção (PRD §18). */
    data class Maintenance(
        val category: MaintenanceCategory,
        val workshop: String = "",
    ) : ExpenseDetail
}

/**
 * Uma despesa lançada pelo motorista (PRD §17).
 *
 * @param vehicleId veículo a que a despesa se refere. Anulável porque pedágio
 *   e estacionamento não dependem de veículo, e porque excluir um veículo não
 *   pode apagar o histórico financeiro — a despesa fica, órfã.
 * @param detail `null` para categorias que são só valor e descrição.
 * @param odometerKm leitura do painel no momento do lançamento (PRD §23).
 *   Fica fora de [ExpenseDetail] de propósito: ela vale para abastecimento,
 *   recarga e manutenção igualmente, e dentro do `sealed` estaria repetida nas
 *   três variantes. Anulável porque pedágio e estacionamento não a exigem, e
 *   porque as despesas gravadas antes da v0.6.0 não têm leitura nenhuma.
 */
data class Expense(
    val id: Long = UNSAVED_ID,
    val vehicleId: Long? = null,
    val date: LocalDate,
    val category: ExpenseCategory,
    val amount: Money,
    val description: String = "",
    val detail: ExpenseDetail? = null,
    val odometerKm: Long? = null,
    val createdAt: Instant,
) {
    /**
     * Quantidade abastecida ou carregada, quando houver.
     *
     * A unidade vem de [unit] — nunca assuma litros.
     */
    val quantity: Quantity?
        get() = when (detail) {
            is ExpenseDetail.Refuel -> detail.quantity
            is ExpenseDetail.Charging -> detail.energy
            else -> null
        }

    /** Unidade de medida da [quantity]. */
    val unit: MeasurementUnit?
        get() = when (detail) {
            is ExpenseDetail.Refuel -> detail.fuelType.unit
            is ExpenseDetail.Charging -> MeasurementUnit.KILOWATT_HOUR
            else -> null
        }

    /**
     * Preço por unidade: R$/litro, R$/m³ ou R$/kWh (PRD §7, §10, §11).
     *
     * `null` quando a quantidade não foi informada — o que é comum, já que ela
     * é opcional. Indicador secundário: o custo/km, que é o principal, não
     * depende dele.
     *
     * Uma recarga gratuita **com** kWh informado tem preço `R$ 0,00` por
     * unidade, que é informação válida e diferente de indisponível.
     */
    val pricePerUnit: Money?
        get() = quantity?.let { amount.per(it.toUnits()) }

    companion object {
        /** Id atribuído a uma despesa que ainda não foi persistida. */
        const val UNSAVED_ID: Long = 0L

        /** Acima disso a descrição vira outra coisa. */
        const val MAX_DESCRIPTION_LENGTH: Int = 200

        /** Nome de posto, oficina ou local de recarga. */
        const val MAX_PLACE_LENGTH: Int = 80

        /**
         * Teto do odômetro, para pegar dígito a mais na digitação.
         *
         * Um carro de aplicativo roda muito, mas não dez milhões de
         * quilômetros. O limite não protege o banco — protege a conciliação de
         * uso pessoal da v0.7.0, onde uma leitura absurda viraria centenas de
         * milhares de quilômetros "pessoais".
         */
        const val MAX_ODOMETER_KM: Long = 9_999_999L
    }
}
