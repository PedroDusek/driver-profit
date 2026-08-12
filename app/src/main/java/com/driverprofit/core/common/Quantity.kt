package com.driverprofit.core.common

/**
 * Quantidade de combustível ou energia, em **milésimos da unidade**.
 *
 * `35,478 L` é armazenado como `35478`. A unidade em si não vive aqui — ela
 * vem do `FuelType` do lançamento (litro, m³ ou kWh), o que impede somar
 * litros com m³ por acidente (PRD §27).
 *
 * Milésimos e não centésimos porque a bomba de combustível exibe três casas:
 * arredondar na entrada já introduziria erro no preço por litro.
 *
 * Mesmo motivo de [Money] para não usar `Double`: o preço por unidade é uma
 * divisão, e acumular erro de ponto flutuante no divisor contamina o
 * indicador (PRD §26).
 */
@JvmInline
value class Quantity(val thousandths: Long) : Comparable<Quantity> {

    init {
        require(thousandths >= 0) { "quantidade não pode ser negativa: $thousandths" }
    }

    operator fun plus(other: Quantity): Quantity = Quantity(thousandths + other.thousandths)

    override fun compareTo(other: Quantity): Int = thousandths.compareTo(other.thousandths)

    val isZero: Boolean get() = thousandths == 0L

    /** Parte inteira — `35478` → `35`. */
    val whole: Long get() = thousandths / THOUSAND

    /** Casas decimais — `35478` → `478`. */
    val fraction: Long get() = thousandths % THOUSAND

    /**
     * Valor em unidades, usado apenas como divisor no preço por unidade.
     * Não persistir este valor.
     */
    fun toUnits(): Double = thousandths.toDouble() / THOUSAND

    companion object {
        private const val THOUSAND = 1_000L

        val ZERO: Quantity = Quantity(0L)

        /** `of(35, 478)` = 35,478 unidades. */
        fun of(whole: Long, thousandths: Long = 0L): Quantity {
            require(thousandths in 0 until THOUSAND) {
                "milésimos deve estar entre 0 e 999, recebido: $thousandths"
            }
            return Quantity(whole * THOUSAND + thousandths)
        }

        fun sum(values: Iterable<Quantity>): Quantity =
            Quantity(values.sumOf { it.thousandths })
    }
}
