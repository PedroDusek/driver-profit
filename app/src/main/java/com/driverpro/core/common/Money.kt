package com.driverpro.core.common

/**
 * Valor monetário em **centavos** (PRD §26).
 *
 * Dinheiro nunca é representado por `Double` neste projeto: `0.1 + 0.2` em
 * ponto flutuante não é `0.3`, e erros de arredondamento se acumulam em somas
 * de centenas de lançamentos. `R$ 286,40` é armazenado como `28640`.
 *
 * A conversão para texto é responsabilidade exclusiva da camada de
 * apresentação — ver `core.ui.format.BrazilianFormatter`.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun times(factor: Int): Money = Money(cents * factor)

    operator fun unaryMinus(): Money = Money(-cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L
    val isNegative: Boolean get() = cents < 0L

    /**
     * Divide este valor por uma quantidade — a operação por trás de R$/km,
     * R$/hora e R$/corrida (PRD §21).
     *
     * Retorna `null` quando [quantity] é zero, negativa ou não-finita, em vez
     * de estourar ou devolver infinito. A UI deve exibir "—" nesse caso: um
     * período sem quilômetros rodados não tem R$/km igual a zero, ele
     * simplesmente não tem R$/km.
     *
     * O arredondamento é *half-up* para o centavo mais próximo.
     */
    fun per(quantity: Double): Money? {
        if (!quantity.isFinite() || quantity <= 0.0) return null
        val result = cents / quantity
        if (!result.isFinite()) return null
        return Money(Math.round(result))
    }

    /** Sobrecarga de [per] para quantidades inteiras (corridas, por exemplo). */
    fun per(quantity: Long): Money? = per(quantity.toDouble())

    companion object {
        val ZERO: Money = Money(0L)

        /** Constrói a partir de reais e centavos: `Money.of(286, 40)` = R$ 286,40. */
        fun of(reais: Long, cents: Int): Money {
            require(cents in 0..99) { "centavos deve estar entre 0 e 99, recebido: $cents" }
            val sign = if (reais < 0) -1 else 1
            return Money(reais * 100 + sign * cents)
        }

        /** Soma uma coleção de valores sem risco de overflow silencioso de tipos. */
        fun sum(values: Iterable<Money>): Money =
            Money(values.sumOf { it.cents })
    }
}
