package com.driverpro.expenses.domain

import com.driverpro.core.domain.DateRange
import com.driverpro.core.domain.FuelType
import com.driverpro.core.domain.MeasurementUnit
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.Quantity
import com.driverpro.maintenance.domain.MaintenanceCategory
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
    /**
     * A que intervalo o valor se refere, quando ele não se esgota no dia do
     * pagamento (PRD §22).
     *
     * `null` é o caso comum e significa "conta no próprio dia" — combustível,
     * pedágio, lavagem. Custo fixo é diferente: o IPVA de R$ 1.200 pago em
     * janeiro serve ao ano inteiro, e sem separar as duas coisas janeiro parece
     * catastrófico e os outros onze meses parecem isentos.
     *
     * **A [date] continua sendo quando o dinheiro saiu.** Histórico, "Despesas"
     * e lucro seguem exibindo caixa, para conferir com o extrato; só os
     * indicadores por quilômetro usam competência.
     */
    val accrual: DateRange? = null,
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

    /**
     * Quanto deste lançamento pertence a [period], pela competência.
     *
     * Sem competência declarada, o valor inteiro cai no dia do pagamento — ou
     * zero, se esse dia estiver fora do período. Com competência, o valor é
     * repartido pelos dias do intervalo e devolvido na proporção que couber.
     *
     * O rateio é por dias iguais, e não por dias trabalhados: seguro e IPVA
     * correm no calendário, não no uso. Um mês parado custa o mesmo que um mês
     * rodando, que é justamente o que faz deles custo fixo.
     */
    fun amountWithin(period: DateRange): Money {
        val accrual = accrual ?: return if (date in period) amount else Money.ZERO

        val start = maxOf(accrual.start, period.start)
        val end = minOf(accrual.end, period.end)
        if (end.isBefore(start)) return Money.ZERO

        val overlapDays = DateRange(start, end).days
        if (overlapDays >= accrual.days) return amount

        return Money(Math.round(amount.cents.toDouble() * overlapDays / accrual.days))
    }

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
