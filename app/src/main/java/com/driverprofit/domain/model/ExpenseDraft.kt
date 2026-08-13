package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
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
