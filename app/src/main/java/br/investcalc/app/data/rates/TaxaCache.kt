package br.investcalc.app.data.rates

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Cache persistido das taxas de referência (DataStore/Preferences) com o
 * TTL de 12h resolvido pelo consumidor ([BcbRatesRepository] via
 * `atualizadaEm`, nunca por este cache).
 */
interface TaxaCache {
    suspend fun carregar(): RatesSnapshot?

    suspend fun salvar(snapshot: RatesSnapshot)
}

private val Context.taxasDataStore by preferencesDataStore(name = "taxas_bcb")

/**
 * Implementação em Preferences DataStore do [TaxaCache] — a produção usa esta,
 * o cold start serve o valor persistido da última sessão sem nunca bloquear.
 */
class DataStoreTaxaCache(context: Context) : TaxaCache {

    private val appContext = context.applicationContext

    override suspend fun carregar(): RatesSnapshot? {
        val prefs = appContext.taxasDataStore.data.first()
        val cdi = prefs[KEY_CDI] ?: return null
        val dia = prefs[KEY_DIA] ?: return null
        val base = RatesFallback
        return RatesSnapshot(
            taxaCdiAnualPercentual = cdi.toDouble(),
            taxaSelicAnualPercentual = prefs[KEY_SELIC]?.toDouble() ?: base.TAXA_SELIC_ANUAL_PERCENTUAL,
            variacaoIpca12MesesPercentual = prefs[KEY_IPCA]?.toDouble() ?: base.IPCA_12_MESES_PERCENTUAL,
            atualizadaEm = LocalDate.ofEpochDay(dia),
        )
    }

    override suspend fun salvar(snapshot: RatesSnapshot) {
        appContext.taxasDataStore.edit { prefs ->
            prefs[KEY_CDI] = snapshot.taxaCdiAnualPercentual.toFloat()
            prefs[KEY_SELIC] = snapshot.taxaSelicAnualPercentual.toFloat()
            prefs[KEY_IPCA] = snapshot.variacaoIpca12MesesPercentual.toFloat()
            prefs[KEY_DIA] = (snapshot.atualizadaEm ?: LocalDate.now()).toEpochDay()
        }
    }

    private companion object {
        val KEY_CDI = floatPreferencesKey("cdi_anual_percentual")
        val KEY_SELIC = floatPreferencesKey("selic_anual_percentual")
        val KEY_IPCA = floatPreferencesKey("ipca_12_meses_percentual")
        val KEY_DIA = longPreferencesKey("atualizada_em_epoch_day")
    }
}