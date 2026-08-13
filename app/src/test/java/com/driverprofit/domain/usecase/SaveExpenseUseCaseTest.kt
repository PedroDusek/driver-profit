package com.driverprofit.domain.usecase

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.ExpenseDraft
import com.driverprofit.domain.model.ExpenseField
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.testing.FakeExpenseRepository
import com.driverprofit.testing.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SaveExpenseUseCaseTest {

    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))

    private val flexCar = Vehicle(
        id = 1,
        name = "Onix branco",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.EPOCH,
    )

    private val expenses = FakeExpenseRepository()
    private val vehicles = FakeVehicleRepository(listOf(flexCar))
    private val saveExpense = SaveExpenseUseCase(expenses, vehicles, ExpenseValidator(clock))

    private val refuelDraft = ExpenseDraft(
        vehicleId = 1,
        date = hoje,
        category = ExpenseCategory.FUEL,
        amount = Money.of(210, 0),
        odometerKm = 45_200,
        fuelType = FuelType.ETHANOL,
        quantity = Quantity.of(35, 400),
    )

    @Test
    fun `registra abastecimento valido`() = runTest {
        val result = saveExpense(refuelDraft)

        assertTrue(result is SaveExpenseResult.Success)
        assertEquals(1, expenses.current.size)
        assertEquals(Money.of(210, 0), expenses.current.single().amount)
    }

    @Test
    fun `busca o veiculo para validar contra o que ele aceita`() = runTest {
        // Sem carregar o veiculo, esta regra nao teria como existir no dominio.
        val result = saveExpense(refuelDraft.copy(fuelType = FuelType.CNG))

        assertTrue(result is SaveExpenseResult.Invalid)
        assertEquals(
            listOf(ExpenseField.FUEL_TYPE),
            (result as SaveExpenseResult.Invalid).errors.map { it.field },
        )
        assertTrue(expenses.current.isEmpty())
    }

    @Test
    fun `veiculo inexistente e tratado como veiculo ausente`() = runTest {
        val result = saveExpense(refuelDraft.copy(vehicleId = 999))

        assertTrue(result is SaveExpenseResult.Invalid)
        assertTrue(
            (result as SaveExpenseResult.Invalid).errors.any {
                it.field == ExpenseField.VEHICLE
            },
        )
    }

    @Test
    fun `pedagio e salvo sem consultar veiculo`() = runTest {
        val result = saveExpense(
            ExpenseDraft(
                date = hoje,
                category = ExpenseCategory.TOLL,
                amount = Money.of(12, 50),
            ),
        )

        assertTrue(result is SaveExpenseResult.Success)
        assertEquals(null, expenses.current.single().vehicleId)
    }

    @Test
    fun `edicao atualiza em vez de inserir`() = runTest {
        val id = (saveExpense(refuelDraft) as SaveExpenseResult.Success).id

        val result = saveExpense(refuelDraft.copy(id = id, amount = Money.of(250, 0)))

        assertEquals(SaveExpenseResult.Success(id), result)
        assertEquals(1, expenses.current.size)
        assertEquals(Money.of(250, 0), expenses.current.single().amount)
    }

    @Test
    fun `edicao preserva a data de criacao original`() = runTest {
        val original = Expense(
            id = 7,
            vehicleId = 1,
            date = hoje.minusDays(2),
            category = ExpenseCategory.TOLL,
            amount = Money.of(10, 0),
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
        )
        val repo = FakeExpenseRepository(listOf(original))
        val useCase = SaveExpenseUseCase(repo, vehicles, ExpenseValidator(clock))

        useCase(
            ExpenseDraft(
                id = 7,
                date = hoje.minusDays(2),
                category = ExpenseCategory.TOLL,
                amount = Money.of(15, 0),
            ),
        )

        assertEquals(Instant.parse("2024-01-15T08:00:00Z"), repo.current.single().createdAt)
        assertEquals(Money.of(15, 0), repo.current.single().amount)
    }

    @Test
    fun `edicao de despesa ja excluida insere uma nova em vez de perder o dado`() = runTest {
        val result = saveExpense(refuelDraft.copy(id = 999))

        assertTrue(result is SaveExpenseResult.Success)
        assertEquals(1, expenses.current.size)
    }

    @Test
    fun `recarga gratuita e persistida com valor zero`() = runTest {
        val eletrico = flexCar.copy(id = 2, fuel = VehicleFuel.ELECTRIC)
        val vehiclesComEletrico = FakeVehicleRepository(listOf(flexCar, eletrico))
        val useCase = SaveExpenseUseCase(expenses, vehiclesComEletrico, ExpenseValidator(clock))

        val result = useCase(
            ExpenseDraft(
                vehicleId = 2,
                date = hoje,
                category = ExpenseCategory.CHARGING,
                amount = Money.ZERO,
                odometerKm = 12_800,
                quantity = Quantity.of(42),
                chargingLocation = com.driverprofit.domain.model.ChargingLocation.COMMERCIAL,
            ),
        )

        assertTrue(result is SaveExpenseResult.Success)
        assertEquals(Money.ZERO, expenses.current.single().amount)
        assertEquals(Money.ZERO, expenses.current.single().pricePerUnit)
    }
}
