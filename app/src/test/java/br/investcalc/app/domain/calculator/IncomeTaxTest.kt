package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomeTaxTest {

    @Test
    fun `aliquota ate 180 dias e 22 por cento`() {
        assertEquals(0.225, IncomeTax.aliquotaPorDias(180), 1e-9)
        assertEquals(0.225, IncomeTax.aliquotaPorDias(90), 1e-9)
    }

    @Test
    fun `aliquota de 181 a 360 dias e 20 por cento`() {
        assertEquals(0.20, IncomeTax.aliquotaPorDias(181), 1e-9)
        assertEquals(0.20, IncomeTax.aliquotaPorDias(360), 1e-9)
    }

    @Test
    fun `aliquota de 361 a 720 dias e 17 5 por cento`() {
        assertEquals(0.175, IncomeTax.aliquotaPorDias(361), 1e-9)
        assertEquals(0.175, IncomeTax.aliquotaPorDias(720), 1e-9)
    }

    @Test
    fun `aliquota acima de 720 dias e 15 por cento`() {
        assertEquals(0.15, IncomeTax.aliquotaPorDias(721), 1e-9)
        assertEquals(0.15, IncomeTax.aliquotaPorDias(1_000), 1e-9)
    }

    @Test
    fun `calcularIrSobreAliquota aplica aliquota explícita`() {
        val rendimento = br.investcalc.app.domain.model.MonetaryValue(12_683L)
        assertEquals(2_537L, IncomeTax.calcularIrSobreAliquota(rendimento, 0.20).cents)
    }

    @Test
    fun `calcularIrSobreDias deriva aliquota pela tabela regressiva`() {
        val rendimento = br.investcalc.app.domain.model.MonetaryValue(12_683L)
        // 365 dias -> faixa 361-720 -> 17,5% -> 12.683 * 0,175 = 2.219,525 -> 2.220
        assertEquals(2_220L, IncomeTax.calcularIrSobreDias(rendimento, 365).cents)
    }

    @Test
    fun `prazo isento nao gera imposto`() {
        val rendimento = br.investcalc.app.domain.model.MonetaryValue(12_683L)
        assertEquals(0L, IncomeTax.calcularIrSobreDias(rendimento, 0).cents)
    }
}
