package br.investcalc.app.domain.plano

/**
 * Vendidade do plano "Remover anúncios" na UI (T4/P7).
 */
enum class EstadoPlanoSemAds { COMPRADO, NAO_COMPRADO, INDISPONIVEL }

/**
 * Plano "Remover anúncios" — produto único, não-consumível, restaurado via
 * Google Play Billing (T4/P7). Classe pura e hermética (JVM, sem Android);
 * o repositório injeta as observações em [estado].
 */
object PlanoSemAds {
    const val PRODUTO_ID = "remover_anuncios"

    /**
     * Estado a partir das observações do repositório:
     *
     * - sem Play (billing indisponível) → [EstadoPlanoSemAds.INDISPONIVEL]
     *   (o card de compra some; sem crash em emulador sem Play);
     * - com Play e [compradoPersistido] → [EstadoPlanoSemAds.COMPRADO];
     * - com Play e sem compra persistida → [EstadoPlanoSemAds.NAO_COMPRADO].
     */
    fun estado(billingDisponivel: Boolean, compradoPersistido: Boolean): EstadoPlanoSemAds = when {
        !billingDisponivel -> EstadoPlanoSemAds.INDISPONIVEL
        compradoPersistido -> EstadoPlanoSemAds.COMPRADO
        else -> EstadoPlanoSemAds.NAO_COMPRADO
    }

    /**
     * Após "Restaurar compras": se o Play confirmar a compra, grava na
     * persistência (uma compra legítima nunca se perde na troca de aparelho).
     */
    suspend fun aposRestauracao(compradoNoPlay: Boolean, persistencia: PlanoSemAdsPersistence): EstadoPlanoSemAds {
        if (compradoNoPlay) persistencia.gravar(true)
        return estado(billingDisponivel = true, compradoPersistido = compradoNoPlay)
    }
}

/**
 * Persistência da compra no aparelho (DataStore). Tolerante a falhas: se ler
 * der errado, trata como não-comprado em vez de quebrar o app sem rede.
 */
interface PlanoSemAdsPersistence {
    suspend fun ler(): Boolean
    suspend fun gravar(comprado: Boolean)
}