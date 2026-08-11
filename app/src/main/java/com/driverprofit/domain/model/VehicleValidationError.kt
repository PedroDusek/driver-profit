package com.driverprofit.domain.model

/** Campo do formulário de veículo ao qual um erro se refere. */
enum class VehicleField {
    BRAND,
    MODEL,
    YEAR,
    INITIAL_ODOMETER,
    POWERTRAIN,
    COMBUSTION_FUEL,
    CHARGING_CAPABILITY,
}

/**
 * Motivo pelo qual um campo foi rejeitado.
 *
 * O domínio devolve o *motivo*, não a mensagem: o texto exibido é escolha da
 * camada de apresentação, que traduz cada motivo para um string resource.
 * Isso mantém o domínio livre de `Context` e testável na JVM.
 */
enum class VehicleValidationError {
    /** Campo obrigatório não preenchido. */
    REQUIRED,

    /** Ano fora da faixa aceitável. */
    YEAR_OUT_OF_RANGE,

    /** Odômetro negativo. */
    NEGATIVE_ODOMETER,

    /** Odômetro absurdamente alto — provável erro de digitação. */
    ODOMETER_TOO_HIGH,

    /** Campo preenchido para um veículo em que ele não faz sentido. */
    NOT_APPLICABLE,
}

/** Um erro de validação associado ao campo que o originou. */
data class VehicleFieldError(
    val field: VehicleField,
    val error: VehicleValidationError,
)
