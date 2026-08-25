package com.driverpro.core.ui.format

import com.driverpro.core.domain.Money

/**
 * Entrada monetária digitada em centavos.
 *
 * O motorista tecla `32050` e o campo mostra `R$ 320,50`. Isso evita a briga
 * com vírgula, ponto e teclado numérico no meio de um lançamento rápido.
 *
 * ### Por que existe um cálculo de diferença aqui
 *
 * O campo exibe o valor **já formatado**, então o texto que ele devolve no
 * `onValueChange` contém os dígitos da formatação: `"R$ 0,00"` tem três zeros,
 * não um. Reextrair todos os dígitos do texto formatado faz estado e tela
 * deixarem de ser reversíveis, e produz dois defeitos concretos:
 *
 *  - apagar um caractere de `R$ 0,00` devolve `"00"`, que normaliza de volta
 *    para `"0"` — o campo trava em `R$ 0,00` e nunca esvazia;
 *  - tocar no meio do texto e digitar `3` em `R$ 0,00` produz `R$ 30,00`, e
 *    não `R$ 0,03`.
 *
 * A solução é comparar quantos dígitos o texto exibido tinha com quantos o
 * texto devolvido tem: menos significa que o motorista apagou, mais significa
 * que ele digitou. O estado guarda só os dígitos que ele realmente teclou.
 */
object MoneyInput {

    const val MAX_DIGITS: Int = 9

    /** `""` → `null`; `"32050"` → `R$ 320,50`. */
    fun toMoney(digits: String): Money? =
        digits.takeIf { it.isNotEmpty() }?.toLongOrNull()?.let(::Money)

    /**
     * Texto exibido no campo.
     *
     * Campo vazio fica vazio: mostrar `R$ 0,00` daria aparência de preenchido
     * num campo que ainda não foi respondido.
     */
    fun display(digits: String): String =
        toMoney(digits)?.let(BrazilianFormatter::money).orEmpty()

    /**
     * Novos dígitos após uma edição do campo.
     *
     * @param currentDigits o que o motorista já havia teclado.
     * @param typedText o texto que o campo devolveu, ainda com a formatação.
     */
    fun onTextChanged(
        currentDigits: String,
        typedText: String,
        maxDigits: Int = MAX_DIGITS,
    ): String {
        val displayedDigits = display(currentDigits).filter(Char::isDigit)
        val typedDigits = typedText.filter(Char::isDigit)

        return when {
            // Apagou: some um dígito do que foi teclado, seja qual for a
            // posição do cursor. Apagar tudo esvazia o campo de verdade.
            typedDigits.length < displayedDigits.length -> currentDigits.dropLast(1)

            // Digitou: acrescenta só o que entrou de novo. O trecho inserido é
            // identificado por diferença — o que sobra depois de descontar o
            // prefixo e o sufixo em comum — e não pelo fim do texto. Sem isso,
            // digitar no meio de "R$ 0,00" acrescentaria o zero da formatação
            // em vez do dígito teclado.
            typedDigits.length > displayedDigits.length ->
                (currentDigits + insertedPart(displayedDigits, typedDigits)).take(maxDigits)

            // Mesma quantidade de dígitos: edição que não muda o valor
            // (mexeu na formatação). Mantém o estado.
            else -> currentDigits
        }
    }

    /** Trecho de [typed] que não existia em [displayed]. */
    private fun insertedPart(displayed: String, typed: String): String {
        val prefix = displayed.commonPrefixWith(typed).length
        // O sufixo em comum não pode invadir o prefixo já contado.
        val maxSuffix = minOf(displayed.length, typed.length) - prefix
        val suffix = displayed.commonSuffixWith(typed).length.coerceAtMost(maxSuffix)
        return typed.substring(prefix, typed.length - suffix)
    }
}
