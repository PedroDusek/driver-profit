package com.driverpro.earnings.domain

/** Campo do formulário de sessão de trabalho ao qual um erro se refere. */
enum class WorkSessionField {
    DATE,
    PLATFORM,
    RIDES,
    REVENUE,
    ONLINE_TIME,
    DISTANCE,
    NOTE,
}

/**
 * Motivo pelo qual um campo foi rejeitado.
 *
 * O domínio devolve o *motivo*, não a mensagem: o texto exibido é escolha da
 * camada de apresentação. Isso mantém o domínio livre de `Context` e testável
 * na JVM.
 */
enum class WorkSessionValidationError {
    /** Campo obrigatório não preenchido. */
    REQUIRED,

    /** Data no futuro — não se registra um dia que ainda não aconteceu. */
    DATE_IN_FUTURE,

    /** Valor negativo onde só faz sentido zero ou positivo. */
    NEGATIVE,

    /** Jornada acima de 24 horas em um único dia. */
    ONLINE_TIME_TOO_LONG,

    /** Observação longa demais. */
    NOTE_TOO_LONG,

    /** Sessão sem faturamento, sem corridas, sem tempo e sem distância. */
    EMPTY_SESSION,
}

/** Um erro de validação associado ao campo que o originou. */
data class WorkSessionFieldError(
    val field: WorkSessionField,
    val error: WorkSessionValidationError,
)
