package com.driverpro.domain.model

/**
 * Plataforma de transporte em que a sessão de trabalho foi feita.
 *
 * Guardada em cada registro desde já, mesmo sem análise por plataforma no MVP:
 * é isso que vai permitir comparar R$/hora, R$/km e R$/corrida entre Uber e 99
 * mais tarde (PRD §16) sem precisar de migração.
 *
 * Persistida pelo `name`, então adicionar uma plataforma nova é acrescentar
 * uma constante aqui — não mexe no banco.
 */
enum class Platform {
    UBER,
    NINETY_NINE,
    INDRIVE,
    OTHER,
}
