package br.investcalc.app.core.format

/**
 * Máscara monetária **pura** (camada `core`, sem Android/Compose) para campos
 * de dinheiro — a fatia T1 do teclado numérico + máscara de entrada.
 *
 * O estado interno é sempre **apenas os dígitos** (centavos): `"100000"` →
 * exibe `"R$ 1.000,00"` e vale `1000.00` reais. Qualquer texto digitado
 * (inclusive no meio ou com símbolos colados) é reduzido a dígitos antes de
 * reformatar — nada de duplicar ou crashear na edição.
 *
 * Regras:
 *  - `apply("5")` → `"R$ 0,05"`; `apply("12")` → `"R$ 0,12"`;
 *  - `apply("100000")` → `"R$ 1.000,00"`; `apply("1000")` → `"R$ 10,00"`;
 *  - `apply("")` → `""` (vazio, sem preso de cursor);
 *  - `digitsOf("12a34")` → `"1234"` → `apply` → `"R$ 12,34"`;
 *  - limite de [MAX_DIGITS] dígitos (overflow de [Long] ignorado com segurança);
 *  - o prefixo `"R$"` é parte da formatação do campo (como o placeholder
 *    "R$ 0,00").
 */
/** Texto mascarado + posição do cursor devolvidos por [CurrencyInputMask.reaplicar]. */
data class ResultadoMascara(val texto: String, val posicaoCursor: Int)

object CurrencyInputMask {

    const val MAX_DIGITS = 13
    private const val CENTS_PER_UNIT = 100.0

    /** Mantém apenas dígitos (0-9), limitando aos últimos [MAX_DIGITS]. */
    fun digitsOf(input: String): String =
        input.filter(Char::isDigit).takeLast(MAX_DIGITS)

    /**
     * Formata os dígitos como moeda brasileira: `"100000"` → `"R$ 1.000,00"`.
     * O prefixo "R$" faz parte da formatação do campo. Vazio quando não há
     * dígitos.
     */
    fun apply(digits: String): String {
        val limpos = digitsOf(digits)
        if (limpos.isEmpty()) return ""
        val cents = limpos.toLongOrNull() ?: 0L
        val reais = cents / 100L
        val centavos = cents % 100L
        return "R$ ${milhar(reais)},${"%02d".format(centavos)}"
    }

    /** Converte o texto da máscara ("R$ 1.101,46") em centavos (`110146L`). */
    fun parseToCents(texto: String): Long {
        val digits = digitsOf(texto)
        if (digits.isEmpty()) return 0L
        return digits.toLongOrNull() ?: 0L
    }

    /** Reais (`Double`) correspondentes ao texto da máscara. */
    fun toReais(texto: String): Double = parseToCents(texto) / CENTS_PER_UNIT

    /**
     * Reaplica a máscara ao texto editado e devolve o resultado com o cursor
     * **sempre no fim**. É a única função de edição dos campos monetários
     * (P3.2-FALHA1): qualquer edição — digitar no meio, backspace, colar — é
     * reduzida a dígitos, reformatada e devolvida já pronta para `TextFieldValue`
     * com `selection = TextRange(posicaoCursor)`.
     */
    fun reaplicar(editado: String): ResultadoMascara {
        val formatado = apply(editado)
        return ResultadoMascara(formatado, formatado.length)
    }

    private fun milhar(v: Long): String {
        val s = v.toString()
        if (s.length <= 3) return s
        return s.reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
    }
}
