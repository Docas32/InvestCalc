package br.investcalc.app.data.rates

import br.investcalc.app.domain.rates.CdiMonthlyPoint
import br.investcalc.app.domain.rates.CdiMonthlySeries
import br.investcalc.app.domain.rates.RateAnnualizer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Cobertura herônica do [RateAnnualizer] (anualização do CDI — requisito
 * central da camada de taxas: a API do BCB entrega CDI *mensal* e a
 * calculadora trabalha com taxa *anual*).
 *
 * Caso canônico da sessão: 12 pontos de 0,98% a.m. (CDI de referência
 * 12,6825% a.a. implícito) → anualizado ≈ 12,42%…; o teste valida com a
 * fórmula fechada `(1+tm)^12 - 1`, independente de rede.
 */
class RateAnnualizerTest {

    @Test
    fun `anualiza doze pontos mensais de CDI`() {
        val pontos = (1..12).map { mes ->
            CdiMonthlyPoint(
                data = LocalDate.of(2026, mes, 1),
                taxaPercentualMensal = 0.9800,
            )
        }
        val serie = CdiMonthlySeries(pontos = pontos)

        val anual = RateAnnualizer.annualizarCdi(serie)

        val esperado = 100.0 * (Math.pow(1.0 + 0.0098, 12.0) - 1.0)
        assertEquals(esperado, anual, 1e-9)
        assertEquals(12, serie.pontos.size)
        assertEquals(
            LocalDate.of(2026, 12, 1),
            serie.atualizadaEm,
        )
    }

    @Test
    fun `serie vazia anualiza para zero`() {
        val anual = RateAnnualizer.annualizarCdi(CdiMonthlySeries(pontos = emptyList()))
        assertEquals(0.0, anual, 1e-12)
    }
}
