package br.investcalc.app.data.plano

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import br.investcalc.app.domain.plano.PlanoSemAdsPersistence
import kotlinx.coroutines.flow.first

private val Context.planoDataStore by preferencesDataStore(name = "plano_sem_ads")

/**
 * Persistência da compra "Remover anúncios" em Preferences DataStore —
 * design tolerante a falhas (T4/P7): erro de leitura = não-comprado; gravação
 * nunca lança para a UI.
 */
class DataStorePlanoSemAdsPersistence(context: Context) : PlanoSemAdsPersistence {

    private val appContext = context.applicationContext

    override suspend fun ler(): Boolean = try {
        val prefs = appContext.planoDataStore.data.first()
        prefs[KEY_COMPRADO] ?: false
    } catch (_: Exception) {
        false
    }

    override suspend fun gravar(comprado: Boolean) {
        try {
            appContext.planoDataStore.edit { prefs ->
                if (comprado) {
                    prefs[KEY_COMPRADO] = true
                } else {
                    prefs.remove(KEY_COMPRADO)
                }
            }
        } catch (_: Exception) {
            // Sem persistência não quebra a sessão; próxima restauração re-tenta.
        }
    }

    private companion object {
        val KEY_COMPRADO = booleanPreferencesKey("comprado")
    }
}