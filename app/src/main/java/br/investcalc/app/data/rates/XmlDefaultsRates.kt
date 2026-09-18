package br.investcalc.app.data.rates

import android.content.Context
import android.content.res.XmlResourceParser
import br.investcalc.app.R
import org.xmlpull.v1.XmlPullParser

/**
 * Camada "XML default" da cadeia de resiliência (T1-v2): valores canônicos
 * versionados em um XML de recurso cru, consultados quando não há cache
 * stale nem remote config. Nunca lança: retorna `null` se o recurso não
 * existir ou não puder ser lido.
 */
interface XmlDefaultsRates {
    /** Leitura síncrona e barata (recurso local); `null` quando indisponível. */
    fun carregar(): RatesSnapshot?
}

/**
 * Implementação de produção: lê `res/raw/taxas_defaults.xml`, no formato
 *
 * ```
 * <taxas cdi="10.9" selic="10.9" ipca="4.5"/>
 * ```
 */
class RawXmlDefaultsRates(
    private val context: Context,
    private val recurso: Int = R.raw.taxas_defaults,
) : XmlDefaultsRates {

    override fun carregar(): RatesSnapshot? {
        return try {
            val parser = context.resources.getXml(recurso)
            parser.use { p ->
                var cdi = -1.0
                var selic = -1.0
                var ipca = -1.0
                var event = p.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && p.name == "taxas") {
                        cdi = p.getAttributeValue(null, "cdi").lerPercentual()
                        selic = p.getAttributeValue(null, "selic").lerPercentual()
                        ipca = p.getAttributeValue(null, "ipca").lerPercentual()
                    }
                    event = p.next()
                }
                if (cdi > 0.0 && selic > 0.0 && ipca > 0.0) {
                    RatesSnapshot(cdi, selic, ipca, atualizadaEm = null)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** "10.9" ou "10,9" → 10.9; qualquer texto inválido → 0.0. */
    private fun String?.lerPercentual(): Double {
        val texto = this?.trim() ?: return 0.0
        return texto
            .replace(",", ".")
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?: 0.0
    }
}