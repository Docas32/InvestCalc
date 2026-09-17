package br.investcalc.app.domain.ads

/**
 * Portão de intersticial — lógica pura (JVM, sem Android, sem rede) que
 * decide Quando exibir o anúncio intersticial ao tocar em "Calcular".
 *
 * Regras do contrato (PROMPT / REMOTE CONFIG, fatia monetização):
 *  1. `ads_enabled = false` (Remote Config) ⇒ nunca exibe;
 *  2. NUNCA no primeiro cálculo da sessão (o usuário recém-abriu o app);
 *  3. Máximo 1 exibição a cada `N` cálculos, em que `N` = chave
 *     `interstitial_frequency` da Remote Config (default 4);
 *  4. NUNCA em sequência imediata: se o cálculo anterior já mostrou o
 *     intersticial, o atual nunca mostra (proteção anti-golpe de UI).
 *
 * A contagem (`totalCalculosInvocadosEmSessao`) é mantida pelo holder da
 * tela; este objeto só decide, de forma determinística e testável heronet.
 */
object InterstitialGate {

    /**
     * Decide se o intersticial deve aparecer após o cálculo de número
     * [calculosJaInvocados] (contagem já incrementada, ≥ 1).
     *
     * @param adsHabilitado Remote Config `ads_enabled` (false ⇒ nunca).
     * @param frequencia Remote Config `interstitial_frequency` (N ≥ 1).
     * @param calculosJaInvocados total de cálculos já disparados NA SESSÃO.
     * @param intersticialExibidoNoCalculoAnterior flag de sequência imediata.
     */
    fun deveExibir(
        adsHabilitado: Boolean,
        frequencia: Int,
        calculosJaInvocados: Int,
        intersticialExibidoNoCalculoAnterior: Boolean,
    ): Boolean {
        if (!adsHabilitado) return false
        if (frequencia <= 0) return false
        if (calculosJaInvocados <= 1) return false // nunca no 1º cálculo
        if (intersticialExibidoNoCalculoAnterior) return false // nunca em sequência

        return calculosJaInvocados % frequencia == 0
    }
}
