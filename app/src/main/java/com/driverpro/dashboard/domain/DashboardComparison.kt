package com.driverpro.dashboard.domain

import com.driverpro.core.domain.Money
import kotlin.math.abs

/**
 * Se, para um indicador, **subir é bom**.
 *
 * Existe porque a mesma variação de 10% significa coisas opostas em lucro e em
 * custo, e essa é a informação que decide a cor na tela. Deixá-la aqui, junto
 * do cálculo, é o que impede a tela de pintar de verde um custo que aumentou —
 * um erro pior que não mostrar variação nenhuma, porque afirma o contrário do
 * que aconteceu.
 */
enum class MetricPolarity {
    HIGHER_IS_BETTER,
    LOWER_IS_BETTER,
}

/**
 * Variação de um indicador entre o período escolhido e o anterior equivalente.
 *
 * Os três casos são distintos de propósito: "não havia período anterior",
 * "havia, mas o indicador era zero" e "variou tanto" pedem frases diferentes na
 * tela, e reduzir tudo a um número anulável perderia essa diferença.
 */
sealed interface MetricChange {

    /**
     * O período anterior não teve lançamento nenhum.
     *
     * Não é o mesmo que variação zero: ninguém rodou, então não há com o que
     * comparar. A tela diz isso com todas as letras.
     */
    data object NoBaseline : MetricChange

    /**
     * Houve período anterior, mas o indicador valia zero (ou não existia) lá.
     *
     * Variação percentual sobre zero não existe — qualquer valor novo seria
     * "infinito por cento". Segue a mesma regra de `Money.per`: divisão sem
     * denominador devolve ausência, nunca zero.
     */
    data object NoBase : MetricChange

    /**
     * @param percent sempre **não negativo**; o sentido vive em [rising].
     * @param rising `true` quando o valor atual é maior que o anterior.
     * @param better se essa variação é boa para o motorista, já resolvido pela
     *   polaridade do indicador.
     * @param absolute diferença em reais, quando o indicador é dinheiro.
     */
    data class Measured(
        val percent: Int,
        val rising: Boolean,
        val better: Boolean,
        val absolute: Money? = null,
    ) : MetricChange {

        /** `true` quando os dois períodos deram exatamente o mesmo número. */
        val isFlat: Boolean get() = percent == 0
    }
}

/**
 * O período escolhido ao lado do anterior equivalente (PRD §20).
 *
 * Responde à pergunta que um número sozinho não responde: "estou melhorando?".
 * Qual é o período anterior de cada preset é decisão de [DashboardPeriod];
 * aqui só entra a aritmética da variação.
 *
 * Kotlin puro, como [DashboardMetrics] — conta financeira não mora em
 * ViewModel (PRD §54).
 */
data class DashboardComparison(
    val current: DashboardMetrics,
    val previous: DashboardMetrics,
) {

    // --- Dinheiro do período ---

    val netProfit: MetricChange
        get() = compare(current.netProfit, previous.netProfit, MetricPolarity.HIGHER_IS_BETTER)

    val totalRevenue: MetricChange
        get() = compare(current.totalRevenue, previous.totalRevenue, MetricPolarity.HIGHER_IS_BETTER)

    val workExpenses: MetricChange
        get() = compare(current.workExpenses, previous.workExpenses, MetricPolarity.LOWER_IS_BETTER)

    // --- Razões ---

    val revenuePerHour: MetricChange
        get() = compare(current.revenuePerHour, previous.revenuePerHour, MetricPolarity.HIGHER_IS_BETTER)

    val revenuePerKm: MetricChange
        get() = compare(current.revenuePerKm, previous.revenuePerKm, MetricPolarity.HIGHER_IS_BETTER)

    val costPerKm: MetricChange
        get() = compare(current.costPerKm, previous.costPerKm, MetricPolarity.LOWER_IS_BETTER)

    val profitPerKm: MetricChange
        get() = compare(current.profitPerKm, previous.profitPerKm, MetricPolarity.HIGHER_IS_BETTER)

    val profitPerHour: MetricChange
        get() = compare(current.profitPerHour, previous.profitPerHour, MetricPolarity.HIGHER_IS_BETTER)

    /** Razão anulável: sem valor de um dos lados, não há variação a declarar. */
    private fun compare(
        currentValue: Money?,
        previousValue: Money?,
        polarity: MetricPolarity,
    ): MetricChange {
        if (previous.isEmpty) return MetricChange.NoBaseline
        if (currentValue == null || previousValue == null) return MetricChange.NoBase
        return compare(currentValue, previousValue, polarity)
    }

    private fun compare(
        currentValue: Money,
        previousValue: Money,
        polarity: MetricPolarity,
    ): MetricChange {
        if (previous.isEmpty) return MetricChange.NoBaseline
        if (previousValue.isZero) return MetricChange.NoBase

        val difference = currentValue - previousValue
        val rising = difference.cents > 0L

        // O módulo do anterior no denominador é o que faz prejuízo encolhendo
        // (-100 -> -50) ler como melhora de 50%, e não como piora: sem ele, o
        // sinal negativo da base inverteria o resultado.
        val percent = Math.round(
            abs(difference.cents) * PERCENT / abs(previousValue.cents),
        ).toInt()

        return MetricChange.Measured(
            percent = percent,
            rising = rising,
            better = when {
                difference.isZero -> false
                polarity == MetricPolarity.HIGHER_IS_BETTER -> rising
                else -> !rising
            },
            absolute = difference,
        )
    }

    private companion object {
        const val PERCENT = 100.0
    }
}
