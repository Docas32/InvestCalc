package br.investcalc.app.data.analytics

/**
 * Contrato de rastreamento de eventos de produto (Firebase Analytics em
 * produção). Mantida como interface para que os testes JVM injetem um
 * `FakeAnalyticsRepository` que grava eventos em memória, sem depender de
 * bibliotecas de rede/Android.
 */
interface AnalyticsRepository {

    /**
     * Registra um cálculo de simulação concluído.
     *
     * @param type tipo da simulação (ex.: "cdi").
     * @param months prazo em meses usado no cálculo.
     * @param hasMonthlyContribution indica que o usuário usou aporte mensal > 0.
     */
    fun calculationPerformed(
        type: String,
        months: Int,
        hasMonthlyContribution: Boolean,
    )

    /**
     * Registra a origem das taxas servidas pela cadeia de resiliência
     * (T1-v2): "cache_fresca", "bcb", "cache_stale", "remote_config", "xml"
     * ou "fallback" — ver [br.investcalc.app.data.rates.TaxSource].
     */
    fun taxaOrigemUsada(origem: String)
}
