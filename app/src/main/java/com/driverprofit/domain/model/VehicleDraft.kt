package com.driverprofit.domain.model

/**
 * Veículo em preenchimento, ainda não validado.
 *
 * Diferente de [Vehicle], todo campo obrigatório é anulável: o formulário
 * começa vazio, e "o motorista ainda não escolheu a propulsão" é um estado
 * legítimo que precisa ser representável. A conversão para [Vehicle] só
 * acontece depois que `VehicleValidator` aprova o rascunho.
 *
 * As strings chegam sem tratamento, como o usuário digitou — o `trim` é
 * responsabilidade da validação, não da UI.
 */
data class VehicleDraft(
    val id: Long = Vehicle.UNSAVED_ID,
    val brand: String = "",
    val model: String = "",
    val year: Int? = null,
    val initialOdometerKm: Long? = null,
    val powertrain: VehiclePowertrain? = null,
    val combustionFuel: CombustionFuel? = null,
    val chargingCapability: ChargingCapability? = null,
) {
    /** `true` quando o rascunho representa a edição de um veículo já salvo. */
    val isEditing: Boolean get() = id != Vehicle.UNSAVED_ID

    /**
     * Zera os campos que deixaram de fazer sentido após a troca de propulsão.
     *
     * Sem isso, um motorista que escolhe "elétrico" depois de ter marcado
     * "flex" salvaria um elétrico com combustível — dado incoerente que
     * quebraria o formulário de abastecimento mais tarde.
     */
    fun withPowertrain(newPowertrain: VehiclePowertrain?): VehicleDraft = copy(
        powertrain = newPowertrain,
        combustionFuel = combustionFuel.takeIf { newPowertrain?.usesCombustionFuel == true },
        chargingCapability = chargingCapability.takeIf { newPowertrain?.mayBeCharged == true },
    )
}

/** Constrói um rascunho a partir de um veículo já persistido, para edição. */
fun Vehicle.toDraft(): VehicleDraft = VehicleDraft(
    id = id,
    brand = brand,
    model = model,
    year = year,
    initialOdometerKm = initialOdometerKm,
    powertrain = powertrain,
    combustionFuel = combustionFuel,
    chargingCapability = chargingCapability,
)
