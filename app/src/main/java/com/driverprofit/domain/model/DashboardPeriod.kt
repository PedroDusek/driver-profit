package com.driverprofit.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Período consultado no dashboard (PRD §20).
 *
 * `sealed` e não `enum` porque "personalizado" carrega um intervalo e os
 * demais não. Com enum, o intervalo personalizado teria que viajar por fora,
 * em um campo anulável que só faz sentido para uma das constantes — e cada
 * leitor do estado precisaria lembrar dessa combinação.
 *
 * O dia de referência é **parâmetro**, nunca `LocalDate.now()` lido aqui
 * dentro: é isso que torna "ontem" e "mês anterior" testáveis sem depender do
 * relógio da máquina que roda o teste.
 *
 * Os presets devolvem a semana e o mês **inteiros**, não até hoje. Como
 * lançamento com data futura é recusado na validação, nenhum dia à frente tem
 * registro, e o número exibido é o mesmo — com a vantagem de "este mês" ter
 * sempre o mesmo começo e fim, independente do dia em que se olha.
 */
sealed interface DashboardPeriod {

    /** Intervalo coberto por este período, tomando [today] como referência. */
    fun rangeAt(today: LocalDate): DateRange

    data object Today : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange = DateRange.of(today)
    }

    data object Yesterday : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange = DateRange.of(today.minusDays(1))
    }

    /**
     * Semana ISO: **segunda a domingo**.
     *
     * Fixada em segunda-feira de propósito, e não deduzida do `Locale`: o
     * primeiro dia da semana muda de país para país, e um indicador que troca
     * de intervalo conforme a configuração do aparelho é impossível de
     * conferir.
     */
    data object ThisWeek : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange {
            val monday = today.with(DayOfWeek.MONDAY)
            return DateRange(monday, monday.plusDays(DAYS_IN_WEEK - 1))
        }
    }

    data object ThisMonth : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange = today.monthRange()
    }

    data object LastMonth : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange = today.minusMonths(1).monthRange()
    }

    /** Intervalo escolhido pelo motorista (PRD §20). */
    data class Custom(val range: DateRange) : DashboardPeriod {
        override fun rangeAt(today: LocalDate): DateRange = range
    }

    companion object {
        private const val DAYS_IN_WEEK = 7L

        /**
         * Períodos fixos, na ordem em que aparecem na tela.
         *
         * `Custom` fica de fora porque não existe sem um intervalo escolhido —
         * a tela o oferece como um botão que abre o seletor de datas.
         */
        val PRESETS: List<DashboardPeriod> = listOf(Today, Yesterday, ThisWeek, ThisMonth, LastMonth)

        private fun LocalDate.monthRange(): DateRange =
            DateRange(withDayOfMonth(1), with(TemporalAdjusters.lastDayOfMonth()))
    }
}
