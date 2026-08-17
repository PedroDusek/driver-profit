package com.driverpro.domain.model

import com.driverpro.core.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AccrualTest {

    private fun expense(
        amount: Money,
        date: LocalDate,
        category: ExpenseCategory = ExpenseCategory.VEHICLE_TAX,
        accrual: DateRange? = null,
    ) = Expense(
        id = 1,
        date = date,
        category = category,
        amount = amount,
        accrual = accrual,
        createdAt = Instant.EPOCH,
    )

    // --- Sem competência: o comportamento que já existia ---

    @Test
    fun `sem competencia o valor conta no dia do pagamento`() {
        val despesa = expense(Money.of(150, 0), date = LocalDate.of(2026, 4, 12))

        assertEquals(
            Money.of(150, 0),
            despesa.amountWithin(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))),
        )
    }

    @Test
    fun `sem competencia o valor some fora do dia do pagamento`() {
        val despesa = expense(Money.of(150, 0), date = LocalDate.of(2026, 4, 12))

        assertEquals(
            Money.ZERO,
            despesa.amountWithin(DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))),
        )
    }

    // --- O caso do PRD 22 ---

    @Test
    fun `o IPVA anual entrega a cada mes a fatia de dias que lhe cabe`() {
        // Exemplo do PRD: R$ 1.200 pagos em 15/01, competencia do ano inteiro.
        // Sem isso, janeiro pareceria catastrofico e os outros onze meses
        // pareceriam isentos.
        val ipva = expense(
            amount = Money.of(1_200, 0),
            date = LocalDate.of(2026, 1, 15),
            accrual = DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
        )

        // 31 de 365 dias: 1200 x 31 / 365 = 101,92
        assertEquals(
            Money.of(101, 92),
            ipva.amountWithin(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))),
        )
        // Agosto tambem tem 31 dias, e o valor e o mesmo — mesmo o pagamento
        // tendo acontecido sete meses antes.
        assertEquals(
            Money.of(101, 92),
            ipva.amountWithin(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))),
        )
    }

    @Test
    fun `periodo que cobre a competencia inteira recebe o valor cheio`() {
        val seguro = expense(
            amount = Money.of(300, 0),
            date = LocalDate.of(2026, 3, 5),
            accrual = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
        )

        assertEquals(
            Money.of(300, 0),
            seguro.amountWithin(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))),
        )
    }

    @Test
    fun `periodo sem sobreposicao com a competencia recebe zero`() {
        val seguro = expense(
            amount = Money.of(300, 0),
            date = LocalDate.of(2026, 3, 5),
            accrual = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
        )

        assertEquals(
            Money.ZERO,
            seguro.amountWithin(DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))),
        )
    }

    @Test
    fun `um dia da competencia vale um dia de custo`() {
        val parcela = expense(
            amount = Money.of(1_200, 0),
            date = LocalDate.of(2026, 4, 10),
            accrual = DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
        )

        // 1200 / 30 = 40,00 por dia — o exemplo do PRD.
        assertEquals(
            Money.of(40, 0),
            parcela.amountWithin(DateRange(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 15))),
        )
    }

    @Test
    fun `a competencia atravessando a virada do mes divide entre os dois`() {
        val seguro = expense(
            amount = Money.of(600, 0),
            date = LocalDate.of(2026, 3, 20),
            accrual = DateRange(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 4, 18)),
        )

        // 12 dias em marco, 18 em abril, de 30 no total.
        val marco = seguro.amountWithin(
            DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
        )
        val abril = seguro.amountWithin(
            DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
        )

        assertEquals(Money.of(240, 0), marco)
        assertEquals(Money.of(360, 0), abril)
        // As duas fatias somam o valor pago: e isso que impede o custo fixo de
        // sumir ou dobrar na virada.
        assertEquals(seguro.amount, marco + abril)
    }

    // --- O indicador ---

    @Test
    fun `custo fixo por km divide pelo quilometro trabalhado`() {
        // Financiamento, seguro e IPVA existem pela decisao de ter o carro para
        // trabalhar. Levar o carro ao mercado nao gera parcela, entao o
        // quilometro pessoal nao entra no divisor (PRD 22).
        val metrics = DashboardMetrics(
            totalRevenue = Money.of(1_000, 0),
            totalExpenses = Money.of(400, 0),
            expensesByCategory = emptyMap(),
            totalRides = 50,
            workKilometers = 800,
            personalKilometers = 200,
            totalOnlineTime = com.driverpro.core.common.WorkDuration.of(40, 0),
            accruedFixedCost = Money.of(240, 0),
        )

        // 240 / 800 km trabalhados = 0,30 — e nao 240 / 1000.
        assertEquals(Money.of(0, 30), metrics.fixedCostPerKm)
    }

    @Test
    fun `sem quilometro trabalhado o custo fixo por km e indisponivel`() {
        val metrics = DashboardMetrics.EMPTY.copy(accruedFixedCost = Money.of(240, 0))

        // Um mes parado tem custo fixo e nao tem custo fixo por km: a divisao
        // devolve null, e a tela exibe um traco.
        assertNull(metrics.fixedCostPerKm)
    }
}
