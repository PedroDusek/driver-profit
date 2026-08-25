package com.driverpro.maintenance.domain

/**
 * Categorias de manutenção (PRD §18).
 *
 * Movida de `expenses/domain/ExpenseCategory.kt` na reorganização
 * feature-first: é o conceito de manutenção que uma despesa de categoria
 * MANUTENÇÃO referencia, não o contrário — `expenses` importa daqui, e não o
 * inverso.
 */
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
