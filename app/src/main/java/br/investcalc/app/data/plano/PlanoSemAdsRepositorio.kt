package br.investcalc.app.data.plano

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import br.investcalc.app.domain.plano.EstadoPlanoSemAds
import br.investcalc.app.domain.plano.PlanoSemAds
import br.investcalc.app.domain.plano.PlanoSemAdsPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ciclo de vida do plano "Remover anúncios" no aparelho (T4/P7):
 * billing Play (PBL 9) + persistência local ([PlanoSemAdsPersistence]).
 *
 *  - [inicializar] no cold start: conecta ao Billing, carrega o preço do
 *    produto e confere compras pendentes/ativas;
 *  - [comprar] abre o fluxo de compra (o direito é concedido quando o Play
 *    confirma via [purchasesUpdated]);
 *  - [restaurar] re-consulta o Play (troca de aparelho): compra legítima
 *    nunca se perde.
 *
 * Sem Play no aparelho (emulador) o estado vira [EstadoPlanoSemAds.INDISPONIVEL]
 * e o card de compra some — sem jamais lançar exceção.
 */
class PlanoSemAdsRepositorio(
    context: Context,
    private val persistencia: PlanoSemAdsPersistence,
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _estado = MutableStateFlow(EstadoPlanoSemAds.INDISPONIVEL)
    val estado: StateFlow<EstadoPlanoSemAds> = _estado.asStateFlow()

    private val _resolvido = MutableStateFlow(false)
    val resolvido: StateFlow<Boolean> = _resolvido.asStateFlow()

    private val _preco = MutableStateFlow<String?>(null)
    val preco: StateFlow<String?> = _preco.asStateFlow()

    private var billingClient: BillingClient? = null
    private var produtoDetails: ProductDetails? = null

    /** Cold start: conecta ao Billing e restaura o estado persistido. */
    fun inicializar() {
        val cliente = BillingClient.newBuilder(appContext)
            .setListener { resultado, compras -> tratarResultado(resultado, compras) }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .enableAutoServiceReconnection()
            .build()
        billingClient = cliente
        scope.launch {
            _estado.value = PlanoSemAds.estado(
                billingDisponivel = false,
                compradoPersistido = persistencia.ler(),
            )
        }
        cliente.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() tenta reconectar sozinho.
            }

            override fun onBillingSetupFinished(resultado: BillingResult) {
                val disponivel = resultado.responseCode == BillingResponseCode.OK
                scope.launch {
                    _estado.value = PlanoSemAds.estado(
                        billingDisponivel = disponivel,
                        compradoPersistido = persistencia.ler(),
                    )
                    // Só depois desta marca o UMP/Ajustes decidem — comprador a
                    // frio não é incomodado com formulário de consentimento.
                    _resolvido.value = true
                }
                if (disponivel) {
                    consultarDetalhes()
                    verificarCompras()
                }
            }
        })
    }

    /** Abre o fluxo de compra no Play (direito concedido em [tratarResultado]). */
    fun comprar(activity: Activity) {
        val cliente = billingClient
        val detalhes = produtoDetails
        if (cliente == null || detalhes == null) return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detalhes)
                        .build(),
                ),
            )
            .build()
        cliente.launchBillingFlow(activity, params)
    }

    /** Re-consulta o Play (troca de aparelho / reinstalação). */
    fun restaurar() = verificarCompras()

    private fun consultarDetalhes() {
        val cliente = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PlanoSemAds.PRODUTO_ID)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        cliente.queryProductDetailsAsync(params) { resultado, detalhes ->
            if (resultado.responseCode == BillingResponseCode.OK) {
                produtoDetails = detalhes.productDetailsList.firstOrNull { it.productId == PlanoSemAds.PRODUTO_ID }
                _preco.value = produtoDetails
                    ?.oneTimePurchaseOfferDetails
                    ?.formattedPrice
            }
        }
    }

    private fun verificarCompras() {
        val cliente = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        cliente.queryPurchasesAsync(params) { resultado, compras ->
            tratarResultado(resultado, compras)
        }
    }

    /** Entrada única de compras (listener do Play + query): só PURCHASED vale. */
    private fun tratarResultado(resultado: BillingResult, compras: List<Purchase>?) {
        val compradoNoPlay = resultado.responseCode == BillingResponseCode.OK &&
            compras.orEmpty().any { compra ->
                compra.products.contains(PlanoSemAds.PRODUTO_ID) &&
                    compra.purchaseState == Purchase.PurchaseState.PURCHASED
            }
        val novas = compras.orEmpty().filter {
            it.products.contains(PlanoSemAds.PRODUTO_ID) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                !it.isAcknowledged
        }
        novas.forEach { reconhecer(it) }
        scope.launch {
            persistencia.gravar(compradoNoPlay)
            _estado.value = PlanoSemAds.estado(
                billingDisponivel = true,
                compradoPersistido = compradoNoPlay,
            )
        }
    }

    /**
     * Produto não-consumível (o direito permanece): reconhece a compra para o
     * Play não a estornar. [consumeAsync] ficaria para itens consumíveis (no
     * nosso caso o direito é permanente e reaparece em cada query).
     */
    private fun reconhecer(compra: Purchase) {
        billingClient?.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(compra.purchaseToken)
                .build(),
            AcknowledgePurchaseResponseListener { },
        )
    }
}