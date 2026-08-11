package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehiclePowertrain
import com.driverprofit.domain.model.VehicleValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class VehicleValidatorTest {

    // Relógio fixo em 11/08/2026: a validação de ano precisa ser determinística.
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val validator = VehicleValidator(clock)

    private val validFlexDraft = VehicleDraft(
        brand = "Chevrolet",
        model = "Onix",
        year = 2020,
        initialOdometerKm = 50_000,
        powertrain = VehiclePowertrain.COMBUSTION,
        combustionFuel = CombustionFuel.FLEX,
        chargingCapability = null,
    )

    @Test
    fun `rascunho completo e valido`() {
        assertEquals(emptyList<VehicleFieldError>(), validator.validate(validFlexDraft))
    }

    @Test
    fun `rascunho vazio acusa todos os campos obrigatorios de uma vez`() {
        val errors = validator.validate(VehicleDraft())

        // Todos juntos, e nao um por vez: corrigir campo a campo, com um erro
        // novo surgindo a cada tentativa, e a forma mais rapida de irritar
        // quem preenche formulario.
        assertEquals(
            setOf(
                VehicleField.BRAND,
                VehicleField.MODEL,
                VehicleField.YEAR,
                VehicleField.INITIAL_ODOMETER,
                VehicleField.POWERTRAIN,
            ),
            errors.map { it.field }.toSet(),
        )
        assertTrue(errors.all { it.error == VehicleValidationError.REQUIRED })
    }

    @Test
    fun `marca e modelo em branco sao rejeitados`() {
        val errors = validator.validate(validFlexDraft.copy(brand = "   ", model = ""))

        assertTrue(
            errors.containsAll(
                listOf(
                    VehicleFieldError(VehicleField.BRAND, VehicleValidationError.REQUIRED),
                    VehicleFieldError(VehicleField.MODEL, VehicleValidationError.REQUIRED),
                ),
            ),
        )
    }

    @Test
    fun `ano do proximo ano e aceito`() {
        // Montadoras lancam o modelo com o ano seguinte.
        assertEquals(emptyList<VehicleFieldError>(), validator.validate(validFlexDraft.copy(year = 2027)))
    }

    @Test
    fun `ano dois anos a frente e rejeitado`() {
        assertEquals(
            listOf(VehicleFieldError(VehicleField.YEAR, VehicleValidationError.YEAR_OUT_OF_RANGE)),
            validator.validate(validFlexDraft.copy(year = 2028)),
        )
    }

    @Test
    fun `ano anterior ao minimo e rejeitado`() {
        assertEquals(
            listOf(VehicleFieldError(VehicleField.YEAR, VehicleValidationError.YEAR_OUT_OF_RANGE)),
            validator.validate(validFlexDraft.copy(year = 1949)),
        )
    }

    @Test
    fun `odometro zero e aceito`() {
        // Carro zero km e um caso real, nao erro de digitacao.
        assertEquals(
            emptyList<VehicleFieldError>(),
            validator.validate(validFlexDraft.copy(initialOdometerKm = 0)),
        )
    }

    @Test
    fun `odometro negativo e rejeitado`() {
        assertEquals(
            listOf(
                VehicleFieldError(
                    VehicleField.INITIAL_ODOMETER,
                    VehicleValidationError.NEGATIVE_ODOMETER,
                ),
            ),
            validator.validate(validFlexDraft.copy(initialOdometerKm = -1)),
        )
    }

    @Test
    fun `odometro absurdo e rejeitado`() {
        assertEquals(
            listOf(
                VehicleFieldError(
                    VehicleField.INITIAL_ODOMETER,
                    VehicleValidationError.ODOMETER_TOO_HIGH,
                ),
            ),
            validator.validate(validFlexDraft.copy(initialOdometerKm = 2_000_001)),
        )
    }

    @Test
    fun `combustao exige combustivel`() {
        assertEquals(
            listOf(
                VehicleFieldError(
                    VehicleField.COMBUSTION_FUEL,
                    VehicleValidationError.REQUIRED,
                ),
            ),
            validator.validate(validFlexDraft.copy(combustionFuel = null)),
        )
    }

    @Test
    fun `combustao nao aceita capacidade de recarga`() {
        val errors = validator.validate(
            validFlexDraft.copy(chargingCapability = ChargingCapability.PLUG_IN),
        )

        assertEquals(
            listOf(
                VehicleFieldError(
                    VehicleField.CHARGING_CAPABILITY,
                    VehicleValidationError.NOT_APPLICABLE,
                ),
            ),
            errors,
        )
    }

    @Test
    fun `eletrico exige recarga e recusa combustivel`() {
        val errors = validator.validate(
            validFlexDraft.copy(
                powertrain = VehiclePowertrain.ELECTRIC,
                combustionFuel = CombustionFuel.FLEX,
                chargingCapability = null,
            ),
        )

        assertEquals(
            setOf(
                VehicleFieldError(
                    VehicleField.COMBUSTION_FUEL,
                    VehicleValidationError.NOT_APPLICABLE,
                ),
                VehicleFieldError(
                    VehicleField.CHARGING_CAPABILITY,
                    VehicleValidationError.REQUIRED,
                ),
            ),
            errors.toSet(),
        )
    }

    @Test
    fun `eletrico puro e valido`() {
        assertEquals(
            emptyList<VehicleFieldError>(),
            validator.validate(
                validFlexDraft.copy(
                    powertrain = VehiclePowertrain.ELECTRIC,
                    combustionFuel = null,
                    chargingCapability = ChargingCapability.PLUG_IN,
                ),
            ),
        )
    }

    @Test
    fun `hibrido exige combustivel e recarga`() {
        val errors = validator.validate(
            validFlexDraft.copy(
                powertrain = VehiclePowertrain.HYBRID,
                combustionFuel = null,
                chargingCapability = null,
            ),
        )

        assertEquals(
            setOf(VehicleField.COMBUSTION_FUEL, VehicleField.CHARGING_CAPABILITY),
            errors.map { it.field }.toSet(),
        )
    }

    @Test
    fun `hibrido convencional e valido com recarga NONE`() {
        assertEquals(
            emptyList<VehicleFieldError>(),
            validator.validate(
                validFlexDraft.copy(
                    powertrain = VehiclePowertrain.HYBRID,
                    combustionFuel = CombustionFuel.GASOLINE,
                    chargingCapability = ChargingCapability.NONE,
                ),
            ),
        )
    }

    @Test
    fun `hibrido flex plug-in e valido`() {
        // A combinacao que o PRD 13 exige que continue possivel.
        assertEquals(
            emptyList<VehicleFieldError>(),
            validator.validate(
                validFlexDraft.copy(
                    powertrain = VehiclePowertrain.HYBRID,
                    combustionFuel = CombustionFuel.FLEX,
                    chargingCapability = ChargingCapability.PLUG_IN,
                ),
            ),
        )
    }

    @Test
    fun `toVehicle remove espacos das bordas`() {
        val vehicle = validator.toVehicle(
            validFlexDraft.copy(brand = "  Chevrolet  ", model = " Onix "),
        )

        assertEquals("Chevrolet", vehicle.brand)
        assertEquals("Onix", vehicle.model)
    }

    @Test
    fun `toVehicle usa o relogio injetado`() {
        assertEquals(Instant.parse("2026-08-11T12:00:00Z"), validator.toVehicle(validFlexDraft).createdAt)
    }
}
