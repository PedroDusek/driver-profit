package com.driverpro.domain.model

import com.driverpro.core.domain.FuelType
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.Quantity
import java.time.LocalDate

/**
 * Despesa em preenchimento, ainda não validada.
 *
 * Ao contrário de [Expense], os campos específicos ficam soltos e anuláveis em
 * vez de agrupados num `sealed`: enquanto o motorista preenche, a categoria
 * pode mudar e os campos ainda não formam um detalhe coerente. Montar o
 * `sealed` é justamente o trabalho de `ExpenseValidator`.
 */
data class ExpenseDraft(
    val id: Long = Expense.UNSAVED_ID,
    val vehicleId: Long? = null,
    val date: LocalDate? = null,
    val category: ExpenseCategory? = null,
    val amount: Money? = null,
    val description: String = "",

    /** Leitura do painel. Obrigatória para as categorias ligadas ao veículo. */
    val odometerKm: Long? = null,

    /**
     * Declaração explícita de que a leitura é desconhecida.
     *
     * Só vale em **lançamento retroativo** — data anterior à última leitura já
     * registrada daquele veículo. É o caso de quem instala o app e preenche o
     * histórico do mês passado: nota de posto não traz odômetro, e o número não
     * existe para ser lembrado.
     *
     * Não afrouxa a regra do campo obrigatório, que proíbe branco tratado como
     * zero em silêncio (PRD §23). Uma declaração explícita é o oposto de um
     * branco: ausência já significa "não sei" em todo o cálculo — o consumo
     * pula o par, o alerta de manutenção não usa como marco, a conciliação não
     * fecha a janela.
     */
    val odometerUnknown: Boolean = false,

    /** Início da competência do custo fixo (PRD §22). Nulo é o caso comum. */
    val accrualStart: LocalDate? = null,

    /** Fim da competência, inclusive. Preenchido junto com [accrualStart]. */
    val accrualEnd: LocalDate? = null,

    // --- Abastecimento ---
    val fuelType: FuelType? = null,
    val quantity: Quantity? = null,
    val station: String = "",

    // --- Recarga ---
    val chargingLocation: ChargingLocation? = null,
    val place: String = "",

    // --- Manutenção ---
    val maintenanceCategory: MaintenanceCategory? = null,
    val workshop: String = "",
) {
    /** `true` quando o rascunho representa a edição de uma despesa já salva. */
    val isEditing: Boolean get() = id != Expense.UNSAVED_ID
}

/** Constrói um rascunho a partir de uma despesa já persistida, para edição. */
fun Expense.toDraft(): ExpenseDraft {
    val base = ExpenseDraft(
        id = id,
        vehicleId = vehicleId,
        date = date,
        category = category,
        amount = amount,
        description = description,
        odometerKm = odometerKm,
    )
    return when (val detail = detail) {
        is ExpenseDetail.Refuel -> base.copy(
            fuelType = detail.fuelType,
            quantity = detail.quantity,
            station = detail.station,
        )
        is ExpenseDetail.Charging -> base.copy(
            quantity = detail.energy,
            chargingLocation = detail.location,
            place = detail.place,
        )
        is ExpenseDetail.Maintenance -> base.copy(
            maintenanceCategory = detail.category,
            workshop = detail.workshop,
        )
        null -> base
    }
}
