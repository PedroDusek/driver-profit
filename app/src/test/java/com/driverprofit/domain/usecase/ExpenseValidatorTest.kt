package com.driverprofit.domain.usecase

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import com.driverprofit.domain.model.ChargingLocation
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.ExpenseDetail
import com.driverprofit.domain.model.ExpenseDraft
import com.driverprofit.domain.model.ExpenseField
import com.driverprofit.domain.model.ExpenseFieldError
import com.driverprofit.domain.model.ExpenseValidationError
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.MaintenanceCategory
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExpenseValidatorTest {

    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val validator = ExpenseValidator(clock)

    private val flexCar = Vehicle(
        id = 1,
        name = "Onix branco",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.EPOCH,
    )
    private val electricCar = flexCar.copy(id = 2, name = "Dolphin", fuel = VehicleFuel.ELECTRIC)
    private val cngCar = flexCar.copy(id = 3, name = "Kwid GNV", fuel = VehicleFuel.CNG)

    private val refuelDraft = ExpenseDraft(
        vehicleId = 1,
        date = hoje,
        category = ExpenseCategory.FUEL,
        amount = Money.of(210, 0),
        odometerKm = 45_200,
        fuelType = FuelType.ETHANOL,
        quantity = Quantity.of(35, 400),
        station = "Posto Shell",
    )

    private val tollDraft = ExpenseDraft(
        date = hoje,
        category = ExpenseCategory.TOLL,
        amount = Money.of(12, 50),
    )

    // --- Regras gerais ---

    @Test
    fun `abastecimento completo e valido`() {
        assertEquals(
            emptyList<ExpenseFieldError>(),
            validator.validate(refuelDraft, flexCar),
        )
    }

    @Test
    fun `rascunho vazio acusa data, categoria e valor`() {
        val errors = validator.validate(ExpenseDraft(), null)

        assertEquals(
            setOf(ExpenseField.DATE, ExpenseField.CATEGORY, ExpenseField.AMOUNT),
            errors.map { it.field }.toSet(),
        )
    }

    @Test
    fun `data no futuro e rejeitada`() {
        assertTrue(
            validator.validate(refuelDraft.copy(date = hoje.plusDays(1)), flexCar).contains(
                ExpenseFieldError(ExpenseField.DATE, ExpenseValidationError.DATE_IN_FUTURE),
            ),
        )
    }

    @Test
    fun `valor negativo e rejeitado`() {
        assertTrue(
            validator.validate(refuelDraft.copy(amount = Money(-1)), flexCar).contains(
                ExpenseFieldError(ExpenseField.AMOUNT, ExpenseValidationError.NEGATIVE),
            ),
        )
    }

    // --- Odômetro (v0.6.0) ---

    @Test
    fun `abastecimento exige odometro`() {
        assertTrue(
            validator.validate(refuelDraft.copy(odometerKm = null), flexCar).contains(
                ExpenseFieldError(ExpenseField.ODOMETER, ExpenseValidationError.REQUIRED),
            ),
        )
    }

    @Test
    fun `recarga e manutencao tambem exigem odometro`() {
        val recarga = chargingDraft.copy(odometerKm = null)
        val manutencao = ExpenseDraft(
            vehicleId = 1,
            date = hoje,
            category = ExpenseCategory.MAINTENANCE,
            amount = Money.of(320, 0),
            maintenanceCategory = MaintenanceCategory.OIL,
        )

        listOf(recarga to electricCar, manutencao to flexCar).forEach { (draft, car) ->
            assertTrue(
                "sem odometro deveria ser rejeitado: ${draft.category}",
                validator.validate(draft, car).contains(
                    ExpenseFieldError(ExpenseField.ODOMETER, ExpenseValidationError.REQUIRED),
                ),
            )
        }
    }

    @Test
    fun `pedagio nao exige odometro`() {
        // Nao ha veiculo em jogo; cobrar a leitura ali so criaria atrito.
        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(tollDraft, null))
    }

    @Test
    fun `odometro zero e rejeitado`() {
        // Diferente de ausente: aqui ele respondeu, e a resposta nao pode ser
        // uma leitura de painel.
        assertTrue(
            validator.validate(refuelDraft.copy(odometerKm = 0), flexCar).contains(
                ExpenseFieldError(
                    ExpenseField.ODOMETER,
                    ExpenseValidationError.ODOMETER_OUT_OF_RANGE,
                ),
            ),
        )
    }

    @Test
    fun `odometro absurdamente alto e rejeitado`() {
        // Pega digito a mais na digitacao, que envenenaria a conciliacao de
        // uso pessoal da v0.7.0.
        assertTrue(
            validator.validate(
                refuelDraft.copy(odometerKm = Expense.MAX_ODOMETER_KM + 1),
                flexCar,
            ).contains(
                ExpenseFieldError(
                    ExpenseField.ODOMETER,
                    ExpenseValidationError.ODOMETER_OUT_OF_RANGE,
                ),
            ),
        )
    }

    @Test
    fun `odometro no teto ainda e aceito`() {
        assertEquals(
            emptyList<ExpenseFieldError>(),
            validator.validate(refuelDraft.copy(odometerKm = Expense.MAX_ODOMETER_KM), flexCar),
        )
    }

    @Test
    fun `odometro chega na despesa gravada`() {
        assertEquals(45_200L, validator.toExpense(refuelDraft).odometerKm)
    }

    @Test
    fun `odometro digitado num pedagio nao e gravado`() {
        // Trocar de categoria depois de digitar nao pode deixar leitura
        // pendurada numa despesa que nao tem veiculo.
        val draft = tollDraft.copy(odometerKm = 45_200)

        assertNull(validator.toExpense(draft).odometerKm)
    }

    // --- Pedágio e afins: sem veículo, sem detalhe ---

    @Test
    fun `pedagio nao exige veiculo`() {
        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(tollDraft, null))
    }

    @Test
    fun `pedagio nao gera detalhe`() {
        assertEquals(null, validator.toExpense(tollDraft).detail)
    }

    // --- Abastecimento ---

    @Test
    fun `abastecimento exige veiculo`() {
        assertTrue(
            validator.validate(refuelDraft.copy(vehicleId = null), null).contains(
                ExpenseFieldError(ExpenseField.VEHICLE, ExpenseValidationError.REQUIRED),
            ),
        )
    }

    @Test
    fun `abastecimento exige combustivel`() {
        assertTrue(
            validator.validate(refuelDraft.copy(fuelType = null), flexCar).contains(
                ExpenseFieldError(ExpenseField.FUEL_TYPE, ExpenseValidationError.REQUIRED),
            ),
        )
    }

    @Test
    fun `combustivel incompativel com o veiculo e rejeitado`() {
        // Etanol num carro que so aceita GNV contaminaria o custo por unidade.
        assertTrue(
            validator.validate(refuelDraft.copy(vehicleId = 3), cngCar).contains(
                ExpenseFieldError(
                    ExpenseField.FUEL_TYPE,
                    ExpenseValidationError.FUEL_NOT_SUPPORTED_BY_VEHICLE,
                ),
            ),
        )
    }

    @Test
    fun `gasolina e etanol sao aceitos num flex`() {
        listOf(FuelType.GASOLINE, FuelType.ETHANOL).forEach { fuel ->
            assertEquals(
                "combustivel rejeitado: $fuel",
                emptyList<ExpenseFieldError>(),
                validator.validate(refuelDraft.copy(fuelType = fuel), flexCar),
            )
        }
    }

    @Test
    fun `abastecimento sem quantidade e valido`() {
        // A quantidade e opcional: o indicador principal e custo/km, que sai
        // do valor pago e dos km rodados, sem depender de quantos litros
        // entraram no tanque.
        assertEquals(
            emptyList<ExpenseFieldError>(),
            validator.validate(refuelDraft.copy(quantity = null), flexCar),
        )
    }

    @Test
    fun `abastecimento sem quantidade nao tem preco por unidade`() {
        val expense = validator.toExpense(refuelDraft.copy(quantity = null))

        assertNull(expense.pricePerUnit)
        assertEquals(FuelType.ETHANOL.unit, expense.unit)
    }

    @Test
    fun `abastecimento com quantidade zero e rejeitado`() {
        assertTrue(
            validator.validate(refuelDraft.copy(quantity = Quantity.ZERO), flexCar).contains(
                ExpenseFieldError(ExpenseField.QUANTITY, ExpenseValidationError.QUANTITY_ZERO),
            ),
        )
    }

    @Test
    fun `abastecimento vira detalhe de refuel`() {
        val expense = validator.toExpense(refuelDraft)

        assertEquals(
            ExpenseDetail.Refuel(FuelType.ETHANOL, Quantity.of(35, 400), "Posto Shell"),
            expense.detail,
        )
        assertEquals(FuelType.ETHANOL.unit, expense.unit)
    }

    @Test
    fun `gnv e medido em metros cubicos no lancamento`() {
        val draft = refuelDraft.copy(
            vehicleId = 3,
            fuelType = FuelType.CNG,
            quantity = Quantity.of(12, 500),
        )

        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(draft, cngCar))
        assertEquals(
            com.driverprofit.domain.model.MeasurementUnit.CUBIC_METER,
            validator.toExpense(draft).unit,
        )
    }

    // --- Recarga ---

    private val chargingDraft = ExpenseDraft(
        vehicleId = 2,
        date = hoje,
        category = ExpenseCategory.CHARGING,
        amount = Money.of(45, 0),
        odometerKm = 12_800,
        quantity = Quantity.of(42),
        chargingLocation = ChargingLocation.PUBLIC,
        place = "Eletroposto",
    )

    @Test
    fun `recarga em veiculo eletrico e valida`() {
        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(chargingDraft, electricCar))
    }

    @Test
    fun `recarga em veiculo que nao recarrega e rejeitada`() {
        assertTrue(
            validator.validate(chargingDraft.copy(vehicleId = 1), flexCar).contains(
                ExpenseFieldError(
                    ExpenseField.VEHICLE,
                    ExpenseValidationError.VEHICLE_CANNOT_CHARGE,
                ),
            ),
        )
    }

    @Test
    fun `recarga exige tipo de carregamento`() {
        assertTrue(
            validator.validate(chargingDraft.copy(chargingLocation = null), electricCar).contains(
                ExpenseFieldError(
                    ExpenseField.CHARGING_LOCATION,
                    ExpenseValidationError.REQUIRED,
                ),
            ),
        )
    }

    @Test
    fun `recarga gratuita e valida com valor zero e kWh preenchido`() {
        // Exigencia explicita do PRD 11.
        val gratis = chargingDraft.copy(amount = Money.ZERO)

        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(gratis, electricCar))
        assertEquals(Money.ZERO, validator.toExpense(gratis).pricePerUnit)
    }

    @Test
    fun `recarga sem kWh e valida`() {
        val gratis = chargingDraft.copy(amount = Money.ZERO, quantity = null)

        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(gratis, electricCar))
        assertNull(validator.toExpense(gratis).pricePerUnit)
    }

    // --- Manutenção ---

    @Test
    fun `manutencao exige o item`() {
        val draft = ExpenseDraft(
            vehicleId = 1,
            date = hoje,
            category = ExpenseCategory.MAINTENANCE,
            amount = Money.of(320, 0),
            odometerKm = 45_200,
        )

        assertTrue(
            validator.validate(draft, flexCar).contains(
                ExpenseFieldError(
                    ExpenseField.MAINTENANCE_CATEGORY,
                    ExpenseValidationError.REQUIRED,
                ),
            ),
        )
    }

    @Test
    fun `manutencao completa vira detalhe de maintenance`() {
        val draft = ExpenseDraft(
            vehicleId = 1,
            date = hoje,
            category = ExpenseCategory.MAINTENANCE,
            amount = Money.of(320, 0),
            odometerKm = 45_200,
            maintenanceCategory = MaintenanceCategory.OIL,
            workshop = "Oficina do Zé",
        )

        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(draft, flexCar))
        assertEquals(
            ExpenseDetail.Maintenance(MaintenanceCategory.OIL, "Oficina do Zé"),
            validator.toExpense(draft).detail,
        )
    }

    // --- Competência (PRD §22, v0.11.0) ---

    private val vehicleTaxDraft = ExpenseDraft(
        date = hoje,
        category = ExpenseCategory.VEHICLE_TAX,
        amount = Money.of(1200, 0),
    )

    @Test
    fun `ipva sem competencia e valido`() {
        assertEquals(emptyList<ExpenseFieldError>(), validator.validate(vehicleTaxDraft, null))
    }

    @Test
    fun `ipva com competencia e rejeitado`() {
        // O valor entra inteiro no mes do lancamento a partir da v0.11.0 - nao
        // ha mais intervalo pra diluir.
        val draft = vehicleTaxDraft.copy(
            accrualStart = LocalDate.of(2026, 1, 1),
            accrualEnd = LocalDate.of(2026, 12, 31),
        )

        assertTrue(
            validator.validate(draft, null).contains(
                ExpenseFieldError(ExpenseField.ACCRUAL, ExpenseValidationError.ACCRUAL_NOT_ALLOWED),
            ),
        )
    }

    @Test
    fun `seguro e financiamento continuam aceitando competencia`() {
        listOf(ExpenseCategory.INSURANCE, ExpenseCategory.FINANCING).forEach { categoria ->
            val draft = ExpenseDraft(
                date = hoje,
                category = categoria,
                amount = Money.of(300, 0),
                accrualStart = hoje.withDayOfMonth(1),
                accrualEnd = hoje,
            )

            assertEquals(
                "competencia deveria ser aceita: $categoria",
                emptyList<ExpenseFieldError>(),
                validator.validate(draft, null),
            )
        }
    }

    @Test
    fun `competencia incompleta continua rejeitada`() {
        val draft = ExpenseDraft(
            date = hoje,
            category = ExpenseCategory.INSURANCE,
            amount = Money.of(300, 0),
            accrualStart = hoje.withDayOfMonth(1),
        )

        assertTrue(
            validator.validate(draft, null).contains(
                ExpenseFieldError(ExpenseField.ACCRUAL, ExpenseValidationError.ACCRUAL_INCOMPLETE),
            ),
        )
    }

    // --- Texto ---

    @Test
    fun `descricao longa demais e rejeitada`() {
        val texto = "a".repeat(Expense.MAX_DESCRIPTION_LENGTH + 1)

        assertTrue(
            validator.validate(tollDraft.copy(description = texto), null).contains(
                ExpenseFieldError(
                    ExpenseField.DESCRIPTION,
                    ExpenseValidationError.TEXT_TOO_LONG,
                ),
            ),
        )
    }

    @Test
    fun `toExpense remove espacos das bordas`() {
        val expense = validator.toExpense(
            refuelDraft.copy(description = "  cheio  ", station = "  Shell  "),
        )

        assertEquals("cheio", expense.description)
        assertEquals("Shell", (expense.detail as ExpenseDetail.Refuel).station)
    }

    @Test
    fun `toExpense usa o relogio injetado`() {
        assertEquals(
            Instant.parse("2026-08-11T12:00:00Z"),
            validator.toExpense(tollDraft).createdAt,
        )
    }
}
