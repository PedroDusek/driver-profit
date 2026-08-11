package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehicleValidationError
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Regras de validação do cadastro de veículo (PRD §5, §12, §14).
 *
 * Classe pura: recebe um [Clock] em vez de consultar o relógio do sistema, o
 * que torna a validação de ano determinística nos testes.
 *
 * Devolve **todos** os erros de uma vez, e não o primeiro encontrado. Corrigir
 * um campo por vez, com um novo erro aparecendo a cada tentativa, é uma das
 * formas mais eficientes de irritar quem preenche formulário.
 */
class VehicleValidator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    fun validate(draft: VehicleDraft): List<VehicleFieldError> = buildList {
        validateText(draft.brand, VehicleField.BRAND)?.let(::add)
        validateText(draft.model, VehicleField.MODEL)?.let(::add)
        addAll(validateYear(draft.year))
        addAll(validateOdometer(draft.initialOdometerKm))
        addAll(validatePropulsion(draft))
    }

    /**
     * Converte um rascunho válido em [Vehicle].
     *
     * Só chame depois de [validate] retornar lista vazia — daí os `!!`, que
     * documentam a pré-condição em vez de escondê-la atrás de defaults
     * silenciosos.
     */
    fun toVehicle(draft: VehicleDraft, createdAt: Instant = clock.instant()): Vehicle = Vehicle(
        id = draft.id,
        brand = draft.brand.trim(),
        model = draft.model.trim(),
        year = draft.year!!,
        initialOdometerKm = draft.initialOdometerKm!!,
        powertrain = draft.powertrain!!,
        combustionFuel = draft.combustionFuel,
        chargingCapability = draft.chargingCapability,
        createdAt = createdAt,
    )

    private fun validateText(value: String, field: VehicleField): VehicleFieldError? =
        if (value.isBlank()) VehicleFieldError(field, VehicleValidationError.REQUIRED) else null

    private fun validateYear(year: Int?): List<VehicleFieldError> {
        if (year == null) {
            return listOf(VehicleFieldError(VehicleField.YEAR, VehicleValidationError.REQUIRED))
        }
        // Modelos são lançados com o ano seguinte, então o limite superior é
        // o ano que vem, não o atual.
        val maxYear = Instant.now(clock).atZone(ZoneId.systemDefault()).year + 1
        return if (year !in MIN_YEAR..maxYear) {
            listOf(VehicleFieldError(VehicleField.YEAR, VehicleValidationError.YEAR_OUT_OF_RANGE))
        } else {
            emptyList()
        }
    }

    private fun validateOdometer(odometer: Long?): List<VehicleFieldError> {
        val field = VehicleField.INITIAL_ODOMETER
        return when {
            odometer == null ->
                listOf(VehicleFieldError(field, VehicleValidationError.REQUIRED))
            odometer < 0 ->
                listOf(VehicleFieldError(field, VehicleValidationError.NEGATIVE_ODOMETER))
            odometer > MAX_ODOMETER_KM ->
                listOf(VehicleFieldError(field, VehicleValidationError.ODOMETER_TOO_HIGH))
            else -> emptyList()
        }
    }

    /**
     * Coerência entre propulsão, combustível e capacidade de recarga.
     *
     * As duas direções importam: falta o que é obrigatório e sobra o que não
     * se aplica. Um elétrico puro com combustível cadastrado geraria um
     * formulário de abastecimento impossível na v0.4.0.
     */
    private fun validatePropulsion(draft: VehicleDraft): List<VehicleFieldError> = buildList {
        val powertrain = draft.powertrain
        if (powertrain == null) {
            add(VehicleFieldError(VehicleField.POWERTRAIN, VehicleValidationError.REQUIRED))
            return@buildList
        }

        if (powertrain.usesCombustionFuel) {
            if (draft.combustionFuel == null) {
                add(
                    VehicleFieldError(
                        VehicleField.COMBUSTION_FUEL,
                        VehicleValidationError.REQUIRED,
                    ),
                )
            }
        } else if (draft.combustionFuel != null) {
            add(
                VehicleFieldError(
                    VehicleField.COMBUSTION_FUEL,
                    VehicleValidationError.NOT_APPLICABLE,
                ),
            )
        }

        if (powertrain.mayBeCharged) {
            if (draft.chargingCapability == null) {
                add(
                    VehicleFieldError(
                        VehicleField.CHARGING_CAPABILITY,
                        VehicleValidationError.REQUIRED,
                    ),
                )
            }
        } else if (draft.chargingCapability != null) {
            add(
                VehicleFieldError(
                    VehicleField.CHARGING_CAPABILITY,
                    VehicleValidationError.NOT_APPLICABLE,
                ),
            )
        }
    }

    companion object {
        /** Antes disso, é digitação errada e não carro de aplicativo. */
        const val MIN_YEAR: Int = 1950

        /** 2 milhões de km: acima disso, quase certamente erro de digitação. */
        const val MAX_ODOMETER_KM: Long = 2_000_000L
    }
}
