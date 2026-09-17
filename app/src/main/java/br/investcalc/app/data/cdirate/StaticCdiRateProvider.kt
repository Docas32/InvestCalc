package br.investcalc.app.data.cdirate

/**
 * Implementação estática de [CdiRateProvider] com taxa fixa — útil como
 * fallback determinístico e para os testes assumem o valor canônico
 * 12,6825% a.a. (que equivale a 1,00% a.m. efetivo).
 */
class StaticCdiRateProvider(
    private val taxaCdiAnualPercentual: Double,
) : CdiRateProvider {

    override suspend fun taxaCdiAnualPercentual(): Double? = taxaCdiAnualPercentual
}
