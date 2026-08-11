package com.driverprofit.domain.model

/** Campo do formulário de veículo ao qual um erro se refere. */
enum class VehicleField {
    NAME,
    FUEL,
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

    /** Nome longo demais para caber na lista de forma legível. */
    NAME_TOO_LONG,
}

/** Um erro de validação associado ao campo que o originou. */
data class VehicleFieldError(
    val field: VehicleField,
    val error: VehicleValidationError,
)
