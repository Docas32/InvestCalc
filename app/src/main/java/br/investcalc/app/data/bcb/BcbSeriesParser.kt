package br.investcalc.app.data.bcb

import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ponto de uma série da API de dados abertos do Banco Central (BCB/SGS).
 *
 * Nota: o BCB publica os valores com VÍRGULA decimal ("0,68") e data em
 * formato "dd/mm/aaaa". O parser herônico ([BcbSeriesParser]) converte para
 * tipos JVM puros para manter a camada de dados independente de Gson/JSON.
 */
data class BcbSeriesPoint(
    val data: LocalDate,
    val valor: Double,
)

/**
 * Parser herônico (JVM puro, sem dependência JSON) do formato
 * `[{"data":"dd/mm/aaaa","valor":"0,68"}, ...]` retornado pela API SGS do BCB.
 *
 * Converte o valor decimal com vírgula para [Double] e a data para [LocalDate].
 */
object BcbSeriesParser {

    private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun parse(json: String): List<BcbSeriesPoint> {
        if (json.isBlank()) return emptyList()

        val pontos = mutableListOf<BcbSeriesPoint>()
        val itens = extrairItens(json)

        for (item in itens) {
            val data = extrairCampo(item, "data") ?: continue
            val valor = extrairCampo(item, "valor") ?: continue

            val localDate = try {
                LocalDate.parse(data, FORMATO_DATA)
            } catch (_: DateTimeException) {
                continue
            }
            val numero = parseValor(valor) ?: continue

            pontos += BcbSeriesPoint(data = localDate, valor = numero)
        }

        return pontos
    }

    /**
     * Extrai os blocos `{...}` individuais do texto JSON sem depender de um
     * parser JSON externo (celibato: nem Gson, nem org.json, nem kotlinx).
     */
    private fun extrairItens(json: String): List<String> {
        if (json.trim().let { !it.startsWith("[") }) return emptyList()

        val itens = mutableListOf<String>()
        var inicio = -1
        var profundidade = 0
        var emString = false
        var escapado = false

        for (i in json.indices) {
            val ch = json[i]
            if (emString) {
                if (escapado) {
                    escapado = false
                } else if (ch == '\\') {
                    escapado = true
                } else if (ch == '"') {
                    emString = false
                }
                continue
            }

            when (ch) {
                '"' -> emString = true
                '{' -> {
                    if (inicio < 0) inicio = i
                    profundidade++
                }
                '}' -> {
                    profundidade--
                    if (profundidade == 0 && inicio >= 0) {
                        itens += json.substring(inicio, i + 1)
                        inicio = -1
                    }
                }
            }
        }

        return itens
    }

    /** Extrai o valor textual de um campo JSON simples ("campo":"valor"). */
    private fun extrairCampo(item: String, campo: String): String? {
        val padrao = "\"$campo\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return padrao.find(item)?.groupValues?.getOrNull(1)
    }

    /** Converte "0,68" → 0.68 tolerando ponto como separador. */
    private fun parseValor(texto: String): Double? {
        val normalizado = texto.trim()
            .replace('.', ' ')
            .replace(',', '.')
            .replace(" ", "")
            .takeIf { it.matches(Regex("-?\\d+(\\.\\d+)?")) }
            ?: return null
        return normalizado.toDoubleOrNull()
    }
}
