package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.domain.model.VehicleValidationError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class VehicleValidatorTest {

    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val validator = VehicleValidator(clock)

    private val validDraft = VehicleDraft(name = "Onix branco", fuel = VehicleFuel.FLEX)

    @Test
    fun `rascunho completo e valido`() {
        assertEquals(emptyList<VehicleFieldError>(), validator.validate(validDraft))
    }

    @Test
    fun `rascunho vazio acusa os dois campos de uma vez`() {
        // Todos juntos, e nao um por vez: corrigir campo a campo, com um erro
        // novo surgindo a cada tentativa, irrita quem preenche formulario.
        assertEquals(
            listOf(
                VehicleFieldError(VehicleField.NAME, VehicleValidationError.REQUIRED),
                VehicleFieldError(VehicleField.FUEL, VehicleValidationError.REQUIRED),
            ),
            validator.validate(VehicleDraft()),
        )
    }

    @Test
    fun `nome so com espacos e rejeitado`() {
        assertEquals(
            listOf(VehicleFieldError(VehicleField.NAME, VehicleValidationError.REQUIRED)),
            validator.validate(validDraft.copy(name = "   ")),
        )
    }

    @Test
    fun `nome no limite de tamanho e aceito`() {
        val nome = "a".repeat(Vehicle.MAX_NAME_LENGTH)

        assertEquals(emptyList<VehicleFieldError>(), validator.validate(validDraft.copy(name = nome)))
    }

    @Test
    fun `nome acima do limite e rejeitado`() {
        val nome = "a".repeat(Vehicle.MAX_NAME_LENGTH + 1)

        assertEquals(
            listOf(VehicleFieldError(VehicleField.NAME, VehicleValidationError.NAME_TOO_LONG)),
            validator.validate(validDraft.copy(name = nome)),
        )
    }

    @Test
    fun `espacos das bordas nao contam para o limite de tamanho`() {
        val nome = "  " + "a".repeat(Vehicle.MAX_NAME_LENGTH) + "  "

        assertEquals(emptyList<VehicleFieldError>(), validator.validate(validDraft.copy(name = nome)))
    }

    @Test
    fun `combustivel nao escolhido e rejeitado`() {
        assertEquals(
            listOf(VehicleFieldError(VehicleField.FUEL, VehicleValidationError.REQUIRED)),
            validator.validate(validDraft.copy(fuel = null)),
        )
    }

    @Test
    fun `todos os combustiveis da lista sao aceitos`() {
        VehicleFuel.entries.forEach { fuel ->
            assertEquals(
                "combustivel rejeitado: $fuel",
                emptyList<VehicleFieldError>(),
                validator.validate(validDraft.copy(fuel = fuel)),
            )
        }
    }

    @Test
    fun `toVehicle remove espacos das bordas do nome`() {
        assertEquals("Onix branco", validator.toVehicle(validDraft.copy(name = "  Onix branco  ")).name)
    }

    @Test
    fun `toVehicle usa o relogio injetado`() {
        assertEquals(Instant.parse("2026-08-11T12:00:00Z"), validator.toVehicle(validDraft).createdAt)
    }

    @Test
    fun `toVehicle nao marca como atual por padrao`() {
        // Editar nome ou combustivel nao pode mexer em quem e o veiculo
        // atual - quem chama precisa passar o valor explicitamente.
        assertEquals(false, validator.toVehicle(validDraft).isCurrent)
    }

    @Test
    fun `toVehicle preserva isCurrent quando informado`() {
        assertEquals(true, validator.toVehicle(validDraft, isCurrent = true).isCurrent)
    }
}
