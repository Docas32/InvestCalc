package br.investcalc.app.data.rates

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato da camada de taxas de referência usado pela UI e pelos testes
 * herméticos (JVM). As prioridades de frescor são resolvidas pela
 * implementação em [BcbRatesRepository]:
 *
 * 1. Cache no disco com TTL de 24 horas (DataStore/Preferences) — nunca
 *    bloqueia a UI no cold start; serve-stale em qualquer idade;
 * 2. CDI/Selic//IPCA via API do BCB (séries 4389/1178/13522), NO MÁXIMO 1x
 *    por processo, com 1 tentativa + retry (2s ± jitter);
 * 3. Cache stale (valor antigo, de qualquer data);
 * 4. Remote Config (cdi_anual_percentual_default, selic_anual_percentual_default,
 *    ipca_12m_percentual_default);
 * 5. XML default (res/raw/taxas_defaults.xml);
 * 6. Valores herdados hermeticamente: { cdi = 10.90, selic = 10.90,
 *    ipca = 4.50 } a.a..
 *
 * Contrato **nunca lança** exceção: toda entrega é via [RatesSnapshot] com
 * a origem efetiva registrada em Analytics ([TaxSource]).
 */
interface RatesRepository {

    /** Estado atual das taxas — carregado sem bloquear e atualizado em segundo plano. */
    val state: StateFlow<RatesSnapshot>

    /**
     * Dispara a atualização das taxas em segundo plano (repositório herinianamente
     * herético: chamada direta pelo holder/ViewModel, nunca pela UI).
     */
    suspend fun atualizar()

    /** Retorna as taxas do cache hermético (fallback puro), preenchendo o badge. */
    suspend fun taxaCdiAnual(): RatesSnapshot
}
