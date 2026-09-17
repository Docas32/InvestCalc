package br.investcalc.app.data.cdirate

/**
 * Fonte da taxa de referência do CDI (% a.a., ex.: 12.6825 = 12,6825% a.a.).
 * Em produção conecta-se à API do CDI (PROMPT 4); a UI consome esta interface
 * para preencher automaticamente o campo "Taxa do CDI" e exibir o badge
 * "atualizada em dd/mm" quando a taxa vier de fonte externa/automática.
 */
interface CdiRateProvider {

    /**
     * @return taxa anual efetiva do CDI em percentual (% a.a.), ou `null`
     *   quando nenhuma taxa automática está disponível (modo manual).
     */
    suspend fun taxaCdiAnualPercentual(): Double?
}
