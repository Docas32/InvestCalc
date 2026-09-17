package br.investcalc.app.data.rates

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato da camada de taxas de referência usado pela UI e pelos testes
 * herméticos (JVM). As prioridades de frescor são resolvidas pela
 * implementação em `data.rates.remote` e `data.rates.cache`:
 *
 * 1. Cache no disco com TTL de 12 horas (DataStore/Preferences) — nunca
 *    bloqueia a UI no cold start;
 * 2. CDI anual via API do BCB (série 12, últimos 12 pontos → anualização);
 * 3. Remote Config (default_cdi_annual, default_selic_annual,
 *    default_ipca_annual) com fallbacks no XML;
 * 4. Valores herdados hermeticamente: { cdi = 12.6825, selic = 11.25,
 *    ipca = 4.50 } a.a..
 *
 * Contrato **nunca lança** exceção: toda entrega é via [RatesSnapshot] com
 * [TaxSource] explícito (para a badge "atualizada em dd/mm").
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
