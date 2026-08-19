package com.driverpro.domain.model

/**
 * Veículo em preenchimento, ainda não validado.
 *
 * [fuel] é anulável porque "o motorista ainda não escolheu" é um estado
 * legítimo do formulário. A conversão para [Vehicle] só acontece depois que
 * `VehicleValidator` aprova o rascunho.
 *
 * O nome chega sem tratamento, como o usuário digitou — o `trim` é
 * responsabilidade da validação, não da UI.
 */
data class VehicleDraft(
    val id: Long = Vehicle.UNSAVED_ID,
    val name: String = "",
    val fuel: VehicleFuel? = null,
) {
    /** `true` quando o rascunho representa a edição de um veículo já salvo. */
    val isEditing: Boolean get() = id != Vehicle.UNSAVED_ID
}

/** Constrói um rascunho a partir de um veículo já persistido, para edição. */
fun Vehicle.toDraft(): VehicleDraft = VehicleDraft(
    id = id,
    name = name,
    fuel = fuel,
)
