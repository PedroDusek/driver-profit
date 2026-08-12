package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseDetail
import com.driverprofit.domain.model.ExpenseDetailKind
import com.driverprofit.domain.model.ExpenseDraft
import com.driverprofit.domain.model.ExpenseField
import com.driverprofit.domain.model.ExpenseFieldError
import com.driverprofit.domain.model.ExpenseValidationError
import com.driverprofit.domain.model.Vehicle
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Regras de validação da despesa.
 *
 * Recebe o [Vehicle] selecionado — e não só o id — porque parte das regras
 * depende do que aquele veículo aceita: não faz sentido registrar etanol num
 * carro a diesel nem recarga num carro que não é plug-in (PRD §7).
 *
 * Devolve todos os erros de uma vez.
 */
class ExpenseValidator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    fun validate(draft: ExpenseDraft, vehicle: Vehicle?): List<ExpenseFieldError> = buildList {
        addAll(validateDate(draft.date))

        val category = draft.category
        if (category == null) {
            add(error(ExpenseField.CATEGORY, ExpenseValidationError.REQUIRED))
        }

        when {
            draft.amount == null ->
                add(error(ExpenseField.AMOUNT, ExpenseValidationError.REQUIRED))
            draft.amount.isNegative ->
                add(error(ExpenseField.AMOUNT, ExpenseValidationError.NEGATIVE))
            // Zero é aceito de propósito: recarga gratuita em shopping é
            // despesa de R$ 0,00 com kWh > 0, e o PRD §11 exige que caiba.
        }

        if (draft.description.length > Expense.MAX_DESCRIPTION_LENGTH) {
            add(error(ExpenseField.DESCRIPTION, ExpenseValidationError.TEXT_TOO_LONG))
        }

        if (category != null) {
            addAll(validateVehicle(category, draft, vehicle))
            addAll(validateDetail(category, draft, vehicle))
        }
    }

    /**
     * Converte um rascunho válido em [Expense].
     *
     * Só chame depois de [validate] retornar lista vazia.
     */
    fun toExpense(draft: ExpenseDraft, createdAt: Instant = clock.instant()): Expense = Expense(
        id = draft.id,
        vehicleId = draft.vehicleId,
        date = draft.date!!,
        category = draft.category!!,
        amount = draft.amount!!,
        description = draft.description.trim(),
        detail = toDetail(draft),
        createdAt = createdAt,
    )

    private fun toDetail(draft: ExpenseDraft): ExpenseDetail? =
        when (draft.category!!.detailKind) {
            ExpenseDetailKind.REFUEL -> ExpenseDetail.Refuel(
                fuelType = draft.fuelType!!,
                quantity = draft.quantity,
                station = draft.station.trim(),
            )
            ExpenseDetailKind.CHARGING -> ExpenseDetail.Charging(
                energy = draft.quantity,
                location = draft.chargingLocation!!,
                place = draft.place.trim(),
            )
            ExpenseDetailKind.MAINTENANCE -> ExpenseDetail.Maintenance(
                category = draft.maintenanceCategory!!,
                workshop = draft.workshop.trim(),
            )
            ExpenseDetailKind.NONE -> null
        }

    private fun validateDate(date: LocalDate?): List<ExpenseFieldError> = when {
        date == null ->
            listOf(error(ExpenseField.DATE, ExpenseValidationError.REQUIRED))
        date.isAfter(LocalDate.now(clock)) ->
            listOf(error(ExpenseField.DATE, ExpenseValidationError.DATE_IN_FUTURE))
        else -> emptyList()
    }

    private fun validateVehicle(
        category: com.driverprofit.domain.model.ExpenseCategory,
        draft: ExpenseDraft,
        vehicle: Vehicle?,
    ): List<ExpenseFieldError> {
        // Pedágio e estacionamento não dependem de veículo; abastecimento,
        // recarga e manutenção sim (PRD §5).
        if (!category.requiresVehicle) return emptyList()

        if (draft.vehicleId == null || vehicle == null) {
            return listOf(error(ExpenseField.VEHICLE, ExpenseValidationError.REQUIRED))
        }
        return emptyList()
    }

    private fun validateDetail(
        category: com.driverprofit.domain.model.ExpenseCategory,
        draft: ExpenseDraft,
        vehicle: Vehicle?,
    ): List<ExpenseFieldError> = buildList {
        when (category.detailKind) {
            ExpenseDetailKind.REFUEL -> {
                when {
                    draft.fuelType == null ->
                        add(error(ExpenseField.FUEL_TYPE, ExpenseValidationError.REQUIRED))
                    // O combustível precisa ser um dos que o veículo aceita:
                    // etanol num carro a GNV puro é dado impossível, e
                    // contaminaria o custo por unidade depois.
                    vehicle != null && draft.fuelType !in vehicle.refuelOptions ->
                        add(
                            error(
                                ExpenseField.FUEL_TYPE,
                                ExpenseValidationError.FUEL_NOT_SUPPORTED_BY_VEHICLE,
                            ),
                        )
                }
                addAll(validateQuantity(draft))
                if (draft.station.length > Expense.MAX_PLACE_LENGTH) {
                    add(error(ExpenseField.STATION, ExpenseValidationError.TEXT_TOO_LONG))
                }
            }

            ExpenseDetailKind.CHARGING -> {
                if (vehicle != null && !vehicle.supportsChargingRecords) {
                    add(error(ExpenseField.VEHICLE, ExpenseValidationError.VEHICLE_CANNOT_CHARGE))
                }
                if (draft.chargingLocation == null) {
                    add(error(ExpenseField.CHARGING_LOCATION, ExpenseValidationError.REQUIRED))
                }
                addAll(validateQuantity(draft))
                if (draft.place.length > Expense.MAX_PLACE_LENGTH) {
                    add(error(ExpenseField.PLACE, ExpenseValidationError.TEXT_TOO_LONG))
                }
            }

            ExpenseDetailKind.MAINTENANCE -> {
                if (draft.maintenanceCategory == null) {
                    add(error(ExpenseField.MAINTENANCE_CATEGORY, ExpenseValidationError.REQUIRED))
                }
                if (draft.workshop.length > Expense.MAX_PLACE_LENGTH) {
                    add(error(ExpenseField.WORKSHOP, ExpenseValidationError.TEXT_TOO_LONG))
                }
            }

            ExpenseDetailKind.NONE -> Unit
        }
    }

    /**
     * A quantidade é **opcional**.
     *
     * O indicador principal do produto é custo/km, que sai do valor pago e dos
     * quilômetros rodados — não de quantos litros entraram no tanque. Exigir a
     * quantidade a cada abastecimento cobraria um dado toda vez em troca de um
     * número secundário (R$/litro), e um campo obrigatório a mais é uma
     * barreira entre o motorista e o lançamento.
     *
     * Zero, porém, não é resposta: ou não foi informado — e aí fica em branco —
     * ou é erro de digitação.
     */
    private fun validateQuantity(draft: ExpenseDraft): List<ExpenseFieldError> =
        if (draft.quantity?.isZero == true) {
            listOf(error(ExpenseField.QUANTITY, ExpenseValidationError.QUANTITY_ZERO))
        } else {
            emptyList()
        }

    private fun error(field: ExpenseField, error: ExpenseValidationError) =
        ExpenseFieldError(field, error)
}
