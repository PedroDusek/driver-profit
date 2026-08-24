package com.driverpro.domain.model

import com.driverpro.core.domain.FuelType

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MaintenanceMonitorTest {

    private var nextId = 1L

    private fun refuel(
        odometerKm: Long?,
        quantity: Quantity?,
        date: LocalDate,
        fuel: FuelType = FuelType.GASOLINE,
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

    private fun service(
        category: MaintenanceCategory,
        odometerKm: Long?,
        date: LocalDate,
    ) = Expense(
        id = nextId++,
        vehicleId = 1,
        date = date,
        category = ExpenseCategory.MAINTENANCE,
        amount = Money.of(300, 0),
        detail = ExpenseDetail.Maintenance(category = category),
        odometerKm = odometerKm,
        createdAt = Instant.EPOCH,
    )

    private fun schedule(
        item: MaintenanceItem,
        intervalKm: Long,
        monitored: Boolean = true,
    ) = MaintenanceSchedule(
        id = 1,
        vehicleId = 1,
        item = item,
        intervalKm = intervalKm,
        monitored = monitored,
        createdAt = Instant.EPOCH,
    )

    private fun oil(expenses: List<Expense>, schedules: List<MaintenanceSchedule> = emptyList()) =
        MaintenanceMonitor.alerts(expenses, schedules).single { it.item == MaintenanceItem.OIL }

    // --- Sem dado, sem afirmacao ---

    @Test
    fun `item sem manutencao lancada nao afirma nada`() {
        // O erro caro desta versao seria dizer "em dia" sobre um oleo que o app
        // nunca viu trocar. Sem marco nao ha de onde contar.
        val alerta = oil(
            listOf(refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_1)),
        )

        assertEquals(MaintenanceStatus.UNKNOWN, alerta.status)
        assertNull(alerta.remainingKm)
        assertNull(alerta.traveledKm)
        assertFalse(alerta.needsAttention)
    }

    @Test
    fun `manutencao lancada sem odometro nao vira marco`() {
        // Uma nota fiscal sem a leitura do painel nao diz em que quilometragem
        // o servico foi feito.
        val alerta = oil(listOf(service(MaintenanceCategory.OIL, odometerKm = null, date = DIA_1)))

        assertEquals(MaintenanceStatus.UNKNOWN, alerta.status)
    }

    @Test
    fun `manutencao de outra categoria nao serve de marco para o oleo`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.TIRES, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 105_000, quantity = Quantity.of(40), date = DIA_2),
            ),
        )

        assertEquals(MaintenanceStatus.UNKNOWN, alerta.status)
    }

    // --- Contagem a partir do marco ---

    @Test
    fun `conta a distancia desde a ultima troca da categoria`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 103_000, quantity = null, date = DIA_2),
            ),
        )

        assertEquals(MaintenanceStatus.OK, alerta.status)
        assertEquals(100_000L, alerta.lastServiceKm)
        assertEquals(3_000L, alerta.traveledKm)
        // Intervalo padrao do oleo: 10.000 km.
        assertEquals(7_000L, alerta.remainingKm)
    }

    @Test
    fun `avisa nos ultimos dez por cento do intervalo`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 109_500, quantity = null, date = DIA_2),
            ),
        )

        assertEquals(MaintenanceStatus.DUE_SOON, alerta.status)
        assertEquals(500L, alerta.remainingKm)
        assertTrue(alerta.needsAttention)
    }

    @Test
    fun `a banda de aviso nunca fica menor que a defasagem possivel do painel`() {
        // Intervalo curto: 10% seriam 500 km de banda, e o painel consegue
        // ficar um tanque atrasado. O lembrete chegaria com margem de cem e
        // poucos quilometros, ou depois do vencimento.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 104_100, quantity = null, date = DIA_2),
            ),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 5_000)),
        )

        // Faltam 900 km: dentro da banda minima de 1.000, fora dos 10%.
        assertEquals(MaintenanceStatus.DUE_SOON, alerta.status)
        assertEquals(105_000L, alerta.nextServiceKm)
    }

    @Test
    fun `intervalo muito curto nao fica em aviso permanente`() {
        // Com o piso de 1.000 km aplicado cegamente, um intervalo de 800 estaria
        // em aviso desde o dia da troca. Alerta que nunca desliga nao e alerta.
        val alerta = oil(
            listOf(service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1)),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 800)),
        )

        assertEquals(MaintenanceStatus.OK, alerta.status)
    }

    @Test
    fun `intervalo longo mantem a banda proporcional`() {
        // Pneus a cada 40.000: a banda e 4.000 km, e nao os 1.000 do piso.
        val alertas = MaintenanceMonitor.alerts(
            listOf(
                service(MaintenanceCategory.TIRES, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 136_500, quantity = null, date = DIA_2),
            ),
            emptyList(),
        )

        val pneus = alertas.single { it.item == MaintenanceItem.TIRES }
        // Faltam 3.500 km de 40.000 — dentro dos 10%.
        assertEquals(MaintenanceStatus.DUE_SOON, pneus.status)
    }

    @Test
    fun `passou do intervalo fica vencido e o que falta fica negativo`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 112_000, quantity = null, date = DIA_2),
            ),
        )

        assertEquals(MaintenanceStatus.OVERDUE, alerta.status)
        assertEquals(-2_000L, alerta.remainingKm)
        assertTrue(alerta.needsAttention)
    }

    @Test
    fun `o marco e a maior leitura e nao o lancamento mais recente`() {
        // Lancar hoje a nota da semana passada e rotina; odometro so cresce.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_3),
                service(MaintenanceCategory.OIL, odometerKm = 108_000, date = DIA_1),
                refuel(odometerKm = 109_000, quantity = null, date = DIA_3),
            ),
        )

        assertEquals(108_000L, alerta.lastServiceKm)
        assertEquals(1_000L, alerta.traveledKm)
    }

    // --- O alvo exibido ---

    @Test
    fun `o alvo e a leitura da troca mais o intervalo`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 103_000, quantity = null, date = DIA_2),
            ),
        )

        assertEquals(110_000L, alerta.nextServiceKm)
    }

    @Test
    fun `o alvo nao muda quando o odometro esta atrasado`() {
        // Este e o motivo de exibir o alvo em vez de contagem regressiva: os
        // dois cenarios abaixo tem quilometragem corrente diferente, e o numero
        // que o motorista le e o mesmo nos dois. Ele confere contra o painel e
        // bate sempre.
        val comPainelEmDia = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 109_400, quantity = null, date = DIA_2),
            ),
        )
        val comPainelAtrasado = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 100_400, quantity = null, date = DIA_2),
            ),
        )

        assertEquals(110_000L, comPainelEmDia.nextServiceKm)
        assertEquals(110_000L, comPainelAtrasado.nextServiceKm)
        // A incerteza nao sumiu: ela migrou do numero exibido para o estado,
        // onde errar custa um lembrete adiantado em vez de um numero falso.
        assertEquals(MaintenanceStatus.DUE_SOON, comPainelEmDia.status)
        assertEquals(MaintenanceStatus.OK, comPainelAtrasado.status)
    }

    @Test
    fun `o alvo acompanha o intervalo que o motorista escolheu`() {
        val alerta = oil(
            listOf(service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1)),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 5_000)),
        )

        assertEquals(105_000L, alerta.nextServiceKm)
    }

    @Test
    fun `sem marco nao ha alvo para exibir`() {
        val alerta = oil(emptyList())

        assertNull(alerta.nextServiceKm)
        assertEquals(MaintenanceStatus.UNKNOWN, alerta.status)
    }

    // --- O piso por combustivel comprado ---

    @Test
    fun `combustivel comprado vence odometro parado`() {
        // O motorista repetiu a leitura anterior. Pelo painel andou 400 km;
        // pelos litros que comprou, 900. Subestimar aqui atrasaria a troca de
        // oleo, entao vale o maior.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                // Par que estabelece o consumo historico: 400 km / 40 L = 10 km/L.
                refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_0),
                refuel(odometerKm = 100_400, quantity = Quantity.of(40), date = DIA_2),
                refuel(odometerKm = 100_400, quantity = Quantity.of(50), date = DIA_3),
            ),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 500)),
        )

        // 90 L comprados depois do servico x 10 km/L = 900 km.
        assertEquals(900L, alerta.traveledKm)
        assertTrue(alerta.distanceIsImplied)
        assertEquals(MaintenanceStatus.OVERDUE, alerta.status)
    }

    @Test
    fun `odometro em dia dispensa o piso`() {
        // O piso e um piso: quando o painel esta a frente, ele nao interfere, e
        // a tela nao precisa pedir leitura nenhuma.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_0),
                refuel(odometerKm = 100_400, quantity = Quantity.of(40), date = DIA_2),
                refuel(odometerKm = 105_000, quantity = Quantity.of(50), date = DIA_3),
            ),
        )

        assertEquals(5_000L, alerta.traveledKm)
        assertFalse(alerta.distanceIsImplied)
    }

    @Test
    fun `sem consumo historico o piso nao inventa distancia`() {
        // Um unico abastecimento nao forma par tanque-a-tanque, entao nao ha
        // km por litro para multiplicar. O piso fica em zero em vez de chutar.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_2),
            ),
        )

        assertEquals(0L, alerta.traveledKm)
        assertFalse(alerta.distanceIsImplied)
        assertEquals(MaintenanceStatus.OK, alerta.status)
    }

    @Test
    fun `abastecimento sem quantidade nao levanta o piso`() {
        // Quantidade e opcional desde a v0.4.1. Sem litros nao ha prova de
        // distancia, e o piso continua sendo so o odometro.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_0),
                refuel(odometerKm = 100_400, quantity = Quantity.of(40), date = DIA_2),
                refuel(odometerKm = 100_400, quantity = null, date = DIA_3),
            ),
        )

        // So os 40 L do DIA_2 contam: 400 km, empatando com o odometro.
        assertEquals(400L, alerta.traveledKm)
        assertFalse(alerta.distanceIsImplied)
    }

    @Test
    fun `combustivel comprado antes do servico fica de fora`() {
        // O tanque que abasteceu antes da troca nao mede distancia depois dela.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_400, date = DIA_3),
                refuel(odometerKm = 100_000, quantity = Quantity.of(40), date = DIA_0),
                refuel(odometerKm = 100_400, quantity = Quantity.of(40), date = DIA_2),
            ),
        )

        assertEquals(0L, alerta.traveledKm)
        assertFalse(alerta.distanceIsImplied)
    }

    // --- Intervalos ---

    @Test
    fun `intervalo do motorista substitui o padrao do app`() {
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 105_000, quantity = null, date = DIA_2),
            ),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 5_000)),
        )

        assertEquals(5_000L, alerta.intervalKm)
        assertEquals(MaintenanceStatus.OVERDUE, alerta.status)
    }

    @Test
    fun `item sem preferencia usa o padrao e ja nasce acompanhado`() {
        val alertas = MaintenanceMonitor.alerts(emptyList(), emptyList())

        assertEquals(MaintenanceItem.entries.size, alertas.size)
        assertTrue(alertas.all { it.monitored })
        assertEquals(
            MaintenanceItem.OIL.defaultIntervalKm,
            alertas.single { it.item == MaintenanceItem.OIL }.intervalKm,
        )
    }

    @Test
    fun `item desligado continua na lista mas nao pede atencao`() {
        // Sumir da lista o tornaria impossivel de religar.
        val alerta = oil(
            listOf(
                service(MaintenanceCategory.OIL, odometerKm = 100_000, date = DIA_1),
                refuel(odometerKm = 120_000, quantity = null, date = DIA_2),
            ),
            listOf(schedule(MaintenanceItem.OIL, intervalKm = 10_000, monitored = false)),
        )

        assertFalse(alerta.monitored)
        assertEquals(MaintenanceStatus.OVERDUE, alerta.status)
        assertFalse(alerta.needsAttention)
    }

    private companion object {
        val DIA_0: LocalDate = LocalDate.of(2026, 1, 1)
        val DIA_1: LocalDate = LocalDate.of(2026, 2, 1)
        val DIA_2: LocalDate = LocalDate.of(2026, 3, 1)
        val DIA_3: LocalDate = LocalDate.of(2026, 4, 1)
    }
}
