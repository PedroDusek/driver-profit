package com.driverpro.expenses.domain

import com.driverpro.core.domain.FuelType
import com.driverpro.core.domain.MeasurementUnit

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConsumptionEstimatorTest {

    private var nextId = 1L

    private fun refuel(
        odometerKm: Long?,
        quantity: Quantity?,
        fuel: FuelType = FuelType.GASOLINE,
        date: LocalDate = LocalDate.of(2026, 8, 11),
    ) = Expense(
        id = nextId++,
        vehicleId = 1,
        date = date,
        category = ExpenseCategory.FUEL,
        amount = Money.of(200, 0),
        detail = ExpenseDetail.Refuel(fuelType = fuel, quantity = quantity),
        odometerKm = odometerKm,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `consumo sai da diferenca de odometro dividida pela quantidade do segundo`() {
        // 50.350 - 50.000 = 350 km em 40 L = 8,75 km/L. E o exemplo do PRD 23.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = 50_000, quantity = Quantity.of(38)),
                refuel(odometerKm = 50_350, quantity = Quantity.of(40)),
            ),
        )

        assertEquals(1, estimativas.size)
        assertEquals(Consumption(8_750), estimativas.single().consumption)
        assertEquals(350L, estimativas.single().distanceKm)
    }

    @Test
    fun `o primeiro abastecimento nunca tem consumo`() {
        // Nao ha trecho anterior para medir.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(refuel(odometerKm = 50_000, quantity = Quantity.of(40))),
        )

        assertTrue(estimativas.isEmpty())
    }

    @Test
    fun `abastecimento sem quantidade nao gera estimativa`() {
        // A quantidade e opcional desde a v0.4.1; sem ela nao ha divisor.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = 50_000, quantity = Quantity.of(38)),
                refuel(odometerKm = 50_350, quantity = null),
            ),
        )

        assertTrue(estimativas.isEmpty())
    }

    @Test
    fun `abastecimento sem odometro nao gera estimativa`() {
        // O historico anterior a v0.6.0 nao tem leitura.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = null, quantity = Quantity.of(38)),
                refuel(odometerKm = 50_350, quantity = Quantity.of(40)),
            ),
        )

        assertTrue(estimativas.isEmpty())
    }

    @Test
    fun `trocar de combustivel descarta o par`() {
        // Num flex, alternar gasolina e etanol muda o consumo em cerca de 30%.
        // Um numero so misturando os dois nao descreveria nenhum deles.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = 50_000, quantity = Quantity.of(38), fuel = FuelType.GASOLINE),
                refuel(odometerKm = 50_350, quantity = Quantity.of(40), fuel = FuelType.ETHANOL),
            ),
        )

        assertTrue(estimativas.isEmpty())
    }

    @Test
    fun `sequencia do mesmo combustivel gera uma estimativa por trecho`() {
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = 50_000, quantity = Quantity.of(38)),
                refuel(odometerKm = 50_350, quantity = Quantity.of(40)),
                refuel(odometerKm = 50_750, quantity = Quantity.of(50)),
            ),
        )

        assertEquals(2, estimativas.size)
        assertEquals(Consumption(8_750), estimativas[0].consumption)
        // 400 km / 50 L = 8 km/L
        assertEquals(Consumption(8_000), estimativas[1].consumption)
    }

    @Test
    fun `a ordem e a do odometro e nao a da data`() {
        // Lancar hoje o abastecimento da semana passada e comum; a ordem
        // fisica do carro e a do painel.
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(
                    odometerKm = 50_350,
                    quantity = Quantity.of(40),
                    date = LocalDate.of(2026, 8, 1),
                ),
                refuel(
                    odometerKm = 50_000,
                    quantity = Quantity.of(38),
                    date = LocalDate.of(2026, 8, 11),
                ),
            ),
        )

        assertEquals(1, estimativas.size)
        assertEquals(350L, estimativas.single().distanceKm)
    }

    @Test
    fun `odometro repetido nao gera consumo infinito`() {
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                refuel(odometerKm = 50_000, quantity = Quantity.of(38)),
                refuel(odometerKm = 50_000, quantity = Quantity.of(40)),
            ),
        )

        assertTrue(estimativas.isEmpty())
    }

    @Test
    fun `recarga eletrica e medida em kWh`() {
        val estimativas = ConsumptionEstimator.estimate(
            listOf(
                Expense(
                    id = 1,
                    vehicleId = 1,
                    date = LocalDate.of(2026, 8, 1),
                    category = ExpenseCategory.CHARGING,
                    amount = Money.of(45, 0),
                    detail = ExpenseDetail.Charging(
                        energy = Quantity.of(40),
                        location = ChargingLocation.PUBLIC,
                    ),
                    odometerKm = 10_000,
                    createdAt = Instant.EPOCH,
                ),
                Expense(
                    id = 2,
                    vehicleId = 1,
                    date = LocalDate.of(2026, 8, 11),
                    category = ExpenseCategory.CHARGING,
                    amount = Money.of(45, 0),
                    detail = ExpenseDetail.Charging(
                        energy = Quantity.of(50),
                        location = ChargingLocation.PUBLIC,
                    ),
                    odometerKm = 10_300,
                    createdAt = Instant.EPOCH,
                ),
            ),
        )

        // 300 km / 50 kWh = 6 km/kWh
        assertEquals(Consumption(6_000), estimativas.single().consumption)
        assertEquals(MeasurementUnit.KILOWATT_HOUR, estimativas.single().unit)
    }

    @Test
    fun `consumo sem distancia ou sem quantidade e indisponivel`() {
        assertNull(Consumption.of(0, Quantity.of(40)))
        assertNull(Consumption.of(350, Quantity.ZERO))
        assertNull(Consumption.of(-10, Quantity.of(40)))
    }
}
