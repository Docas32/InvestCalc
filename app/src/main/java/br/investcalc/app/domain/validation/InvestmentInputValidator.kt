package br.investcalc.app.domain.validation

/**
 * Validação de entrada para a tela de calculadora, separada da UI para ser
 * testável em JVM puro (sem Android/Compose).
 */
object InvestmentInputValidator {

    const val MAX_MESES = 600
    private const val MESES_POR_ANO = 12

    private val unidadesTempoValidas = setOf("meses", "anos")

    /**
     * Valida a duração informada, convertendo para meses.
     *
     * @return `Result.error` com mensagem amigável quando inválido; senão
     *   `Result.success(meses)`.
     */
    fun validarDuracao(
        entrada: String,
        unidadeTempo: String,
    ): Result<Int> {
        if (entrada.isBlank()) {
            return Result.failure(IllegalArgumentException("Informe o prazo da aplicação."))
        }
        val numero = entrada.toIntOrNull()
        if (numero == null) {
            return Result.failure(IllegalArgumentException("Prazo deve ser um número inteiro."))
        }
        if (numero <= 0) {
            return Result.failure(IllegalArgumentException("Prazo deve ser maior que zero."))
        }
        if (unidadeTempo !in unidadesTempoValidas) {
            return Result.failure(IllegalArgumentException("Escolha a unidade de tempo."))
        }
        val meses = if (unidadeTempo == "anos") numero * MESES_POR_ANO else numero
        if (meses > MAX_MESES) {
            return Result.failure(
                IllegalArgumentException("Prazo máximo é de $MAX_MESES meses (${MAX_MESES / MESES_POR_ANO} anos)."),
            )
        }
        return Result.success(meses)
    }

    /**
     * Valida um campo monetário digitado (ex.: "1.000,00").
     *
     * @return `Result.error` quando vazio; senão `Result.success(centavos)`.
     */
    fun validarValorMonetario(
        entradas: String,
        nomeCampo: String,
    ): Result<Long> {
        if (entradas.isBlank()) {
            return Result.failure(IllegalArgumentException("Informe o $nomeCampo."))
        }
        val digitos = entradas.filter(Char::isDigit)
        val centavos = digitos.toLongOrNull() ?: 0L
        if (centavos < 0L) {
            return Result.failure(IllegalArgumentException("$nomeCampo não pode ser negativo."))
        }
        return Result.success(centavos)
    }
}
