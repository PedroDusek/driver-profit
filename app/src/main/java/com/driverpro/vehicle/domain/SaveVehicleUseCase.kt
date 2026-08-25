package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleDraft
import com.driverpro.vehicle.domain.VehicleFieldError
import com.driverpro.vehicle.domain.VehicleRepository

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
            // Preserva o createdAt original e o isCurrent: editar nome ou
            // combustível não muda quando o carro entrou no app, nem se ele é
            // o veículo atual.
            val existing = repository.getVehicle(draft.id)
                ?: return SaveVehicleResult.Success(insertAsFirstIfNeeded(validator.toVehicle(draft)))

            repository.updateVehicle(
                validator.toVehicle(
                    draft,
                    createdAt = existing.createdAt,
                    isCurrent = existing.isCurrent,
                ),
            )
            SaveVehicleResult.Success(draft.id)
        } else {
            SaveVehicleResult.Success(insertAsFirstIfNeeded(validator.toVehicle(draft)))
        }
    }

    /**
     * Insere o veículo e, se ele for o primeiro cadastrado, marca como atual
     * (v0.12.0). Com um só veículo, ele é automaticamente o atual — pedir
     * para escolher o único carro que existe seria burocracia. Cadastros
     * seguintes não mexem em quem é o atual.
     */
    private suspend fun insertAsFirstIfNeeded(vehicle: Vehicle): Long {
        val isFirst = repository.countVehicles() == 0
        val id = repository.addVehicle(vehicle)
        if (isFirst) repository.setCurrent(id)
        return id
    }
}
