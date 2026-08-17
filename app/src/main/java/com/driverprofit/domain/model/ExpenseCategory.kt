package com.driverprofit.domain.model

/**
 * Natureza da despesa (PRD §17).
 *
 * Persistida pelo `name`, então acrescentar uma categoria é adicionar uma
 * constante aqui — não mexe no banco.
 *
 * [FUEL], [CHARGING] e [MAINTENANCE] carregam campos próprios no lançamento;
 * as demais são apenas valor e descrição. Essa distinção fica no enum, em
 * [detailKind], para que a UI não precise de uma cadeia de `when` espalhada
 * por Composables.
 */
enum class ExpenseCategory(val detailKind: ExpenseDetailKind) {
    /** Abastecimento com combustível líquido ou gasoso. */
    FUEL(ExpenseDetailKind.REFUEL),

    /** Recarga elétrica. */
    CHARGING(ExpenseDetailKind.CHARGING),

    MAINTENANCE(ExpenseDetailKind.MAINTENANCE),

    CAR_WASH(ExpenseDetailKind.NONE),
    TOLL(ExpenseDetailKind.NONE),
    PARKING(ExpenseDetailKind.NONE),
    INSURANCE(ExpenseDetailKind.NONE),
    VEHICLE_TAX(ExpenseDetailKind.NONE),
    FINANCING(ExpenseDetailKind.NONE),
    OTHER(ExpenseDetailKind.NONE),
    ;

    /** Categorias que só fazem sentido com um veículo cadastrado (PRD §5). */
    val requiresVehicle: Boolean
        get() = detailKind != ExpenseDetailKind.NONE

    /**
     * Indica se a despesa entra no custo por quilômetro de operação.
     *
     * Seguro, IPVA e financiamento são custos fixos: eles não variam com o
     * quanto se roda, e misturá-los no custo/km de um único dia distorceria o
     * indicador. Entram no cálculo próprio da v1.4.
     */
    val isOperationalCost: Boolean
        get() = this !in setOf(INSURANCE, VEHICLE_TAX, FINANCING)

    /**
     * Custos fixos que ainda usam competência (PRD §22).
     *
     * IPVA saiu na v0.11.0: o dinheiro sai à vista ou em poucas parcelas, nunca
     * em doze fatias iguais, então diluí-lo pelo ano não descrevia a realidade
     * do motorista. O valor lançado passa a contar inteiro no mês do
     * lançamento — o comportamento que [Expense.amountWithin] já tem quando
     * [Expense.accrual] é `null`. Seguro e financiamento continuam podendo
     * usar competência exatamente como antes.
     */
    val allowsAccrual: Boolean
        get() = this in setOf(INSURANCE, FINANCING)
}

/** Que conjunto de campos extras a categoria exige no formulário. */
enum class ExpenseDetailKind {
    /** Combustível, quantidade e posto. */
    REFUEL,

    /** kWh, local e tipo de carregamento. */
    CHARGING,

    /** Categoria da manutenção e oficina. */
    MAINTENANCE,

    /** Apenas valor e descrição. */
    NONE,
}

/** Categorias de manutenção (PRD §18). */
enum class MaintenanceCategory {
    OIL,
    FILTERS,
    TIRES,
    BRAKES,
    SUSPENSION,
    BATTERY,
    BELT,
    PARTS,
    ELECTRICAL,
    MECHANICAL,
    BODYWORK,
    INSPECTION,
    OTHER,
}

/** Onde a recarga elétrica foi feita (PRD §11). */
enum class ChargingLocation {
    RESIDENTIAL,
    COMMERCIAL,
    PUBLIC,
    OTHER,
}
