package br.investcalc.app.domain.plano

/**
 * Vendidade do plano "Remover anúncios" na UI (T4/P7).
 */
enum class EstadoPlanoSemAds { COMPRADO, NAO_COMPRADO, INDISPONIVEL }

/**
 * Desfecho de uma tentativa de compra (P7.3): [ABERTA] inicia o fluxo do
 * Play; [RECUSADA_INDISPONIVEL] significa Billing indisponível ou produto não
 * devolvido — a UI mostra o aviso "disponível na versão da Play Store" em vez
 * de fingir que abriu algo (botão ativo que não faz nada = bug para o
 * usuário).
 */
enum class DesfechoCompra { ABERTA, RECUSADA_INDISPONIVEL }

/**
 * Resultado informado ao usuário após "Restaurar compras" (P7.3): o Play deu
 * a palavra, e a UI mostra um snackbar com o desfecho.
 */
enum class ResultadoRestauracao { COMPRA_ENCONTRADA, NENHUMA_COMPRA }

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
     *   (o card de compra aparece com "Comprar" desabilitado e o subtítulo de
     *   loja — sem crash em emulador sem Play);
     * - com Play e [compradoPersistido] → [EstadoPlanoSemAds.COMPRADO];
     * - com Play e sem compra persistida → [EstadoPlanoSemAds.NAO_COMPRADO].
     */
    fun estado(billingDisponivel: Boolean, compradoPersistido: Boolean): EstadoPlanoSemAds = when {
        !billingDisponivel -> EstadoPlanoSemAds.INDISPONIVEL
        compradoPersistido -> EstadoPlanoSemAds.COMPRADO
        else -> EstadoPlanoSemAds.NAO_COMPRADO
    }

    /**
     * Compra só é possível com o Billing conectado **e** o produto
     * "remover_anuncios" devolvido pelo Play. Sem o produto — o caso de estar
     * conectado sem retorno — o botão fica desabilitado na UI.
     */
    fun compraPossivel(billingDisponivel: Boolean, produtoEncontrado: Boolean): Boolean =
        billingDisponivel && produtoEncontrado

    /**
     * Tentativa de compra (P7.3): recusada quando o Billing não conectou ou o
     * produto não foi devolvido — a UI então mostra a explicação em vez de
     * abrir nada. Comprador já marcado conta como aberta (o card nem aparece).
     */
    fun tentarComprar(
        billingDisponivel: Boolean,
        compradoPersistido: Boolean,
        produtoEncontrado: Boolean,
    ): DesfechoCompra = when {
        compradoPersistido || compraPossivel(billingDisponivel, produtoEncontrado) ->
            DesfechoCompra.ABERTA
        else -> DesfechoCompra.RECUSADA_INDISPONIVEL
    }

    /**
     * Após "Restaurar compras": se o Play confirmar a compra, grava na
     * persistência (uma compra legítima nunca se perde na troca de aparelho).
     */
    suspend fun aposRestauracao(compradoNoPlay: Boolean, persistencia: PlanoSemAdsPersistence): EstadoPlanoSemAds {
        if (compradoNoPlay) persistencia.gravar(true)
        return estado(billingDisponivel = true, compradoPersistido = compradoNoPlay)
    }

    /**
     * Feedback da restauração a partir da palavra do Play (P7.3): a UI mostra
     * ao usuário o que o Play respondeu, sempre — encontrou ou não.
     */
    fun resultadoDaRestauracao(compradoNoPlay: Boolean): ResultadoRestauracao =
        if (compradoNoPlay) ResultadoRestauracao.COMPRA_ENCONTRADA
        else ResultadoRestauracao.NENHUMA_COMPRA
}

/**
 * Persistência da compra no aparelho (DataStore). Tolerante a falhas: se ler
 * der errado, trata como não-comprado em vez de quebrar o app sem rede.
 */
interface PlanoSemAdsPersistence {
    suspend fun ler(): Boolean
    suspend fun gravar(comprado: Boolean)
}