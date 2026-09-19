package br.investcalc.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InvestCalcApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // SDK de anúncios inicializado de forma NÃO bloqueante (T2/P4); o
        // consentimento do UMP usa a Activity (AppNavHost cold start) e o
        // Firebase Crashlytics inicializa via ContentProvider do BoM.
        MobileAds.initialize(this) { }
    }
}