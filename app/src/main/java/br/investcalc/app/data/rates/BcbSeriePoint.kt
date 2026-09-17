package br.investcalc.app.data.rates

/**
 * Ponto de uma série temporal do BCB (SGS), tal como entregue pela API
 * `https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados/ultimos/13?formato=json`.
 *
 * Campos canônicos da API BCB (formato = json):
 * ```
 * [{"data":"02/01/2026","valor":"0.68"},{"data":"03/01/2026","valor":"0.68"}]
 * ```
 *
 * `data` vem em `dd/mm/aaaa` e `valor` em texto com VÍRGULA decimal.
 * O consumo anualiza os últimos 12 pontos da série CDI (12) para chegar à
 * taxa anual efetiva — tabelado no [BcbSeriesAnnualizer].
 */
data class BcbSeriePoint(
    val data: String,
    val valor: String,
)
