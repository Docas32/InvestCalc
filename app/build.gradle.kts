import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Chave de upload para a Play Store (P6). O arquivo vive NA RAIZ fora do git
// (.gitignore) com placeholders; preencher antes de qualquer build de release.
val keystorePropertiesFile: File = rootProject.file("keystore.properties")

fun carregarKeystoreProperties(): Properties {
    val props = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { props.load(it) }
    }
    return props
}

/** Lança com a mensagem canônica quando o release não pode ser assinado. */
fun validarAssinaturaRelease(): Properties {
    val props = carregarKeystoreProperties()
    fun vazio(chave: String): Boolean = props.getProperty(chave).isNullOrBlank()
    val storeFileProp = props.getProperty("storeFile")
    val arquivoOk = !storeFileProp.isNullOrBlank() &&
        rootProject.file(storeFileProp).exists()
    if (vazio("storeFile") || vazio("storePassword") || vazio("keyAlias") ||
        vazio("keyPassword") || !arquivoOk
    ) {
        throw GradleException(
            """
            Build de release exige a assinatura de upload. Crie o keystore e preencha
            'keystore.properties' na raiz do projeto (NUNCA commite o .jks nem esse arquivo):

              keytool -genkeypair -v -keystore investcalc-upload.jks -alias investcalc \
                -keyalg RSA -keysize 2048 -validity 10000

              # keystore.properties (na raiz)
              storeFile=investcalc-upload.jks
              storePassword=SUA_SENHA
              keyAlias=investcalc
              keyPassword=SUA_SENHA
            """.trimIndent(),
        )
    }
    return props
}

android {
    namespace = "br.investcalc.app"
    compileSdk = 35

    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "br.investcalc.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SOMENTE IDs DE TESTE oficiais do Google (P4) — proibido ID real nesta fase.
        buildConfigField("String", "ADMOB_BANNER_AD_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        // Device hash do ConsentDebugSettings (DEBUG). Colhido do logcat ao
        // rodar a primeira sessão de anúncios (regarg 2026-09-19).
        buildConfigField("String", "ADMOB_TEST_DEVICE_HASHED_ID", "\"7036A9582DA6F0A3AA98A4857E163BF5\"")
    }

    signingConfigs {
        create("release") {
            val p = carregarKeystoreProperties()
            storeFile = p.getProperty("storeFile")?.takeIf { it.isNotBlank() }
                ?.let { rootProject.file(it) }
            storePassword = p.getProperty("storePassword")
            keyAlias = p.getProperty("keyAlias")
            keyPassword = p.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Validação lenta (execution time) e restrita às tarefas de release: debug e
// testes continuam rodando sem keystore; assembleRelease/bundleRelease falham
// com a mensagem canônica enquanto keystore.properties não estiver preenchido.

val validarAssinaturaRelease = tasks.register("validarAssinaturaRelease") {
    group = "signing"
    description = "Falha com instruções claras se o keystore de upload estiver ausente/incompleto."
    doLast { validarAssinaturaRelease() }
}

// Gatilho determinístico: as tarefas de release citadas no comando falham NA
// CONFIGURAÇÃO com a mensagem canônica (debug e testes seguem sem keystore).
val releasePedida = gradle.startParameter.taskNames
    .map { it.substringAfterLast(':') }
    .any { it in setOf("assembleRelease", "bundleRelease", "packageRelease") }
if (releasePedida) {
    validarAssinaturaRelease()
}

// Rede de segurança para caminhos indiretos (ex.: "build" dispara o pipeline
// de release sem citar o nome): a task de validação roda antes do empacotamento.
tasks.whenTaskAdded {
    if (name in setOf("packageRelease", "bundleRelease")) {
        dependsOn(validarAssinaturaRelease)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
}
