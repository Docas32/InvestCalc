package br.investcalc.app.data.ads

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Contador global de simulações concluídas (T2/P4), persistido em DataStore.
 * O intersticial é decidido por [br.investcalc.app.domain.ads.InterstitialGate]
 * com base no valor devolvido por [incrementar].
 */
interface SimulacaoCounter {
    /** Valor atual do contador. */
    suspend fun valor(): Int

    /** Incrementa e devolve o novo total (≥ 1 após a primeira simulação). */
    suspend fun incrementar(): Int
}

private val Context.simulacoesDataStore by preferencesDataStore(name = "simulacoes")

/** Implementação de produção com Preferences DataStore. */
class DataStoreSimulacaoCounter(context: Context) : SimulacaoCounter {

    private val appContext = context.applicationContext

    override suspend fun valor(): Int =
        appContext.simulacoesDataStore.data.first()[CHAVE] ?: 0

    override suspend fun incrementar(): Int {
        var novo = 0
        appContext.simulacoesDataStore.edit { prefs ->
            novo = (prefs[CHAVE] ?: 0) + 1
            prefs[CHAVE] = novo
        }
        return novo
    }

    private companion object {
        val CHAVE = intPreferencesKey("total_simulacoes")
    }
}

/** Implementação em memória para os testes JVM. */
class MemoriaSimulacaoCounter(
    initial: Int = 0,
) : SimulacaoCounter {
    private var valor = initial
    override suspend fun valor(): Int = valor
    override suspend fun incrementar(): Int {
        valor++
        return valor
    }
}