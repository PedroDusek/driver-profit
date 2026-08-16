package com.driverprofit.domain.model

/** Campo do formulário de despesa ao qual um erro se refere. */
enum class ExpenseField {
    DATE,
    CATEGORY,
    AMOUNT,
    VEHICLE,
    ODOMETER,
    DESCRIPTION,
    FUEL_TYPE,
    QUANTITY,
    STATION,
    CHARGING_LOCATION,
    PLACE,
    MAINTENANCE_CATEGORY,
    WORKSHOP,
}

/**
 * Motivo pelo qual um campo foi rejeitado.
 *
 * O domínio devolve o motivo, não a frase exibida.
 */
enum class ExpenseValidationError {
    REQUIRED,

    /** Data no futuro — não se registra despesa que ainda não aconteceu. */
    DATE_IN_FUTURE,

    /** Valor negativo. Zero é aceito: recarga gratuita existe (PRD §11). */
    NEGATIVE,

    /**
     * Quantidade informada como zero.
     *
     * A quantidade em si é opcional — deixar em branco é aceito. Mas zero
     * litros não é uma resposta: ou não foi informado, ou é erro de digitação.
     */
    QUANTITY_ZERO,

    /** Combustível escolhido não é aceito pelo veículo selecionado. */
    FUEL_NOT_SUPPORTED_BY_VEHICLE,

    /** Veículo selecionado não aceita recarga elétrica. */
    VEHICLE_CANNOT_CHARGE,

    /**
     * Odômetro zerado, negativo ou absurdamente alto.
     *
     * Diferente de [REQUIRED]: aqui o motorista respondeu, mas a resposta não
     * pode ser uma leitura de painel.
     */
    ODOMETER_OUT_OF_RANGE,

    /**
     * A leitura contradiz as vizinhas por data.
     *
     * Odômetro só cresce: um lançamento de 12/08 tem que ficar entre a leitura
     * de 10/08 e a de 16/08. Fora disso é dígito trocado, e aceitá-lo calado
     * envenena consumo, marco de manutenção e conciliação.
     */
    ODOMETER_INCONSISTENT,

    TEXT_TOO_LONG,
}

/** Um erro de validação associado ao campo que o originou. */
data class ExpenseFieldError(
    val field: ExpenseField,
    val error: ExpenseValidationError,
)
