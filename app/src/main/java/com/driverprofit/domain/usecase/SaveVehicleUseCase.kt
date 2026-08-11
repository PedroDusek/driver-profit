package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.repository.VehicleRepository

/** Resultado de uma tentativa de salvar veículo. */
sealed interface SaveVehicleResult {

    /** Persistido com sucesso. [id] é o identificador final do veículo. */
    data class Success(val id: Long) : SaveVehicleResult

    /** Rejeitado pela validação. Nada foi gravado. */
    data class Invalid(val errors: List<VehicleFieldError>) : SaveVehicleResult
}

/**
 * Cadastra ou atualiza um veículo (PRD §5).
 *
 * Insert e update ficam no mesmo use case porque a regra de negócio é a mesma
 * — muda apenas o destino. Quem chama não precisa decidir qual operação usar:
 * o id do rascunho já carrega essa informação.
 *
 * Validação e persistência acontecem juntas aqui, e não na ViewModel, para que
 * seja impossível gravar um veículo incoerente passando por outro caminho.
 */
class SaveVehicleUseCase(
    private val repository: VehicleRepository,
    private val validator: VehicleValidator,
) {

    suspend operator fun invoke(draft: VehicleDraft): SaveVehicleResult {
        val errors = validator.validate(draft)
        if (errors.isNotEmpty()) return SaveVehicleResult.Invalid(errors)

        return if (draft.isEditing) {
            // Preserva o createdAt original: editar o modelo do carro não muda
            // a data em que ele entrou no app.
            val existing = repository.getVehicle(draft.id)
                ?: return SaveVehicleResult.Success(repository.addVehicle(validator.toVehicle(draft)))

            repository.updateVehicle(validator.toVehicle(draft, createdAt = existing.createdAt))
            SaveVehicleResult.Success(draft.id)
        } else {
            SaveVehicleResult.Success(repository.addVehicle(validator.toVehicle(draft)))
        }
    }
}
