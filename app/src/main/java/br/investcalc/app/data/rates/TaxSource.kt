package br.investcalc.app.data.rates

/**
 * Origem efetiva das taxas servidas pela cadeia de resiliência do T1-v2
 * (cache fresco → BCB → cache stale → Remote Config → XML → fallback).
 *
 * O token [analytics] é o payload do evento de Analytics registrado quando o
 * repositório resolve uma origem — a UI e o suporte sabem, a posteriori, de
 * onde vieram os números exibidos.
 */
enum class TaxSource(val analytics: String) {
    /** Cache DataStore com < 24h — servido sem consultar o BCB. */
    CACHE_FRESCA("cache_fresca"),

    /** BCB (SGS): uma tentativa + retry com backoff, máx. 1x por processo. */
    BCB("bcb"),

    /** Cache DataStore de qualquer idade, servido após falha do BCB. */
    CACHE_STALE("cache_stale"),

    /** Remote Config (Firebase), consultada quando não há cache stale. */
    REMOTE_CONFIG("remote_config"),

    /** Defaults em XML (recurso cru), último degrau antes do fallback. */
    XML("xml"),

    /** Valores canônicos embutidos ([RatesFallback]). */
    FALLBACK("fallback"),
}