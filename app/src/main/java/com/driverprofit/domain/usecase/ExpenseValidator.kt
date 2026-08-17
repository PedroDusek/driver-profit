package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.DateRange
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

    fun validate(
        draft: ExpenseDraft,
        vehicle: Vehicle?,
        history: OdometerHistory = OdometerHistory.EMPTY,
    ): List<ExpenseFieldError> = buildList {
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

        addAll(validateAccrual(draft))

        if (category != null) {
            addAll(validateVehicle(category, draft, vehicle))
            addAll(validateOdometer(category, draft, history))
            addAll(validateDetail(category, draft, vehicle))
        }
    }

    /**
     * A leitura do painel é **obrigatória** nas categorias ligadas ao veículo
     * — abastecimento, recarga e manutenção (PRD §23).
     *
     * Obrigatória, e não opcional, pelo mesmo motivo da v0.3.1: um campo em
     * branco que o cálculo trata como ausente produz indicador incompleto sem
     * avisar ninguém. Aqui o preço de deixar passar é maior ainda — o odômetro
     * é a fundação do consumo estimado, do uso pessoal e do alerta de
     * manutenção, e um alerta de troca de óleo atrasado desgasta motor.
     *
     * Pedágio, estacionamento, seguro e IPVA não pedem: não há veículo em jogo
     * e cobrar a leitura ali só criaria atrito.
     */
    /**
     * A competência é opcional, mas não pela metade.
     *
     * Uma ponta só não define intervalo, e aceitar isso obrigaria o cálculo a
     * inventar a outra — que é exatamente o tipo de suposição silenciosa que o
     * projeto evita. O fim antes do início é barrado pelo próprio `DateRange`.
     */
    private fun validateAccrual(draft: ExpenseDraft): List<ExpenseFieldError> {
        val start = draft.accrualStart
        val end = draft.accrualEnd

        return when {
            start == null && end == null -> emptyList()
            start == null || end == null ->
                listOf(error(ExpenseField.ACCRUAL, ExpenseValidationError.ACCRUAL_INCOMPLETE))
            end.isBefore(start) ->
                listOf(error(ExpenseField.ACCRUAL, ExpenseValidationError.ACCRUAL_INCOMPLETE))
            else -> emptyList()
        }
    }

    private fun validateOdometer(
        category: com.driverprofit.domain.model.ExpenseCategory,
        draft: ExpenseDraft,
        history: OdometerHistory,
    ): List<ExpenseFieldError> {
        if (!category.requiresVehicle) return emptyList()

        // "Não sei a leitura" só vale para trás. No lançamento do dia o painel
        // está à mão, e oferecer a saída ali a transformaria em rotina — a
        // leitura por lançamento deixaria de existir na prática, e com ela o
        // consumo, o quilômetro pessoal e o alerta de manutenção.
        if (draft.odometerUnknown) {
            val isRetroactive = draft.date?.isBefore(LocalDate.now(clock)) == true
            return if (isRetroactive) {
                emptyList()
            } else {
                listOf(error(ExpenseField.ODOMETER, ExpenseValidationError.REQUIRED))
            }
        }

        val odometer = draft.odometerKm
            ?: return listOf(error(ExpenseField.ODOMETER, ExpenseValidationError.REQUIRED))

        if (odometer <= 0L || odometer > Expense.MAX_ODOMETER_KM) {
            return listOf(error(ExpenseField.ODOMETER, ExpenseValidationError.ODOMETER_OUT_OF_RANGE))
        }

        // Odômetro só cresce. Uma leitura que contradiz as vizinhas por data é
        // dígito trocado, e aceitá-la calada envenena consumo, marco de
        // manutenção e conciliação de uma vez — o marco produzindo um alvo
        // falso exibido com confiança, que é o pior dos três.
        return if (history.contradicts(draft.date, odometer)) {
            listOf(error(ExpenseField.ODOMETER, ExpenseValidationError.ODOMETER_INCONSISTENT))
        } else {
            emptyList()
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
        // Guardada só quando a categoria a exige: uma leitura digitada num
        // pedágio e depois abandonada não deve virar dado do veículo.
        // "Não sei" grava ausência, e não zero: NULL diz a verdade, e todo o
        // domínio já sabe lidar com ela.
        odometerKm = draft.odometerKm
            .takeIf { draft.category.requiresVehicle && !draft.odometerUnknown },
        accrual = draft.accrualStart?.let { start ->
            draft.accrualEnd?.let { end -> DateRange(start, end) }
        },
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
