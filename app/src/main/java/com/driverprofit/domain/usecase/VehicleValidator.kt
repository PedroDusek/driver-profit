package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehicleValidationError
import java.time.Clock
import java.time.Instant

/**
 * Regras de validação do cadastro de veículo.
 *
 * Classe pura: recebe um [Clock] em vez de consultar o relógio do sistema, o
 * que torna a data de criação determinística nos testes.
 *
 * Devolve **todos** os erros de uma vez, e não o primeiro encontrado. Corrigir
 * um campo por vez, com um novo erro aparecendo a cada tentativa, é uma das
 * formas mais eficientes de irritar quem preenche formulário.
 */
class VehicleValidator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    fun validate(draft: VehicleDraft): List<VehicleFieldError> = buildList {
        val name = draft.name.trim()
        when {
            name.isEmpty() ->
                add(VehicleFieldError(VehicleField.NAME, VehicleValidationError.REQUIRED))
            name.length > Vehicle.MAX_NAME_LENGTH ->
                add(VehicleFieldError(VehicleField.NAME, VehicleValidationError.NAME_TOO_LONG))
        }

        if (draft.fuel == null) {
            add(VehicleFieldError(VehicleField.FUEL, VehicleValidationError.REQUIRED))
        }
    }

    /**
     * Converte um rascunho válido em [Vehicle].
     *
     * Só chame depois de [validate] retornar lista vazia — daí o `!!`, que
     * documenta a pré-condição em vez de escondê-la atrás de um default
     * silencioso.
     */
    fun toVehicle(draft: VehicleDraft, createdAt: Instant = clock.instant()): Vehicle = Vehicle(
        id = draft.id,
        name = draft.name.trim(),
        fuel = draft.fuel!!,
        createdAt = createdAt,
    )
}
