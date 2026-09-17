# Publicacao (InvestCalc)

Checklist documentado para levar o investcalc para a Google Play.
Camada Android de publicacao e ALTA (ads/consent/splash/keystore); este
documento e a fatia hermética JVM (nao compila, nao quebra os 41 testes).

## 1. Keystore e assinatura (release)

- Nunca commitar `InvestCalcRelease.jks` nem `keystore.properties` no git.
- Criar: `keytool -genkeypair -v -keystore InvestCalcRelease.jks
  -alias InvestCalcCerto -keyalg RSA -keysize 2048 -validity 10950`.
- Manter offline um backup do jks + alias + senhas (posto no cofre, nunca
  no repo).
- `keystore.properties` no `~/.gradle` (fora do repo):
  `storeFile=...  storePassword=...  keyAlias=...  keyPassword=...`.
- Gradle: fatia de script usa essas props em `signingConfigs.release` e o
  `assembleRelease` gera APK assinado (verificado com
  `apksigner verify --print-certs`).

## 2. Privacidade e adesao (Google)

- Politica de privacidade (PT-BR) hospedada em URL estatica HTTPS publica.
  Texto necessario: qudados processados (calculos, idioma, preferencias,
  evento de uso de grafico/tabela e consentimento); nenhuma venda; SHARE
  (Sharing) = ads (banner + intersticial) quando aprovado via UMP; sem
  coleta no modo puro local.
- Data safety form (Play Console): colecao opcional de dados de uso de
  `ads`/`analytics`; sem dados de conta, sem contatos, sem localizacao;
  criptografia TLS em todo envio (BCB e ad requests).
- UMP (User Messaging Platform): SDK de consentimento carregado antes de
  qualquer ad; estados `FORMS_AVAILABLE`, `OBTAINED`, `REQUIRED` tratados;
  se nao consentido, sem banner e sem intersticial (so aba "Ajustes" +
  eco local). Nunca mostrar anuncip no primeiro frame nem em sequencia.

## 3. Anuncios (ids e teste)

- Banner adaptativo no rodape da tela de patrimonio; intersticial na
  frequencia via Remote Config (padrao `interstitial_frequency = 4`:
  a cada 4 tabelas abertas, nunca a primeira, nunca em sequencia).
- Durante dev: assert `BuildConfig.DEBUG` e usar as IDs de teste oficiais
  Google (ca-app-pub-3940256099942544/6300978111 banner,
  /1033173712 intersticial) para nao banir a conta real.
- Remote Config keys: `ads_enabled` (bool), `interstitial_frequency`
  (int), `default_cdi_aa`, `default_selic_aa`, `default_ipca_aa`
  (double, usado como fallback determinístico quando o BCB nao responde).

## 4. Ajustes (tela)

- Idioma (pt / en), toggle "eventos de uso" (analytics on/off), "limpar
  cache de taxas" (forca novo fetch BCB), versao e assinatura SHA-256 do
  apk instalado.
- Sem login, sem conta: anuncios dependem so do consentimento UMP.

## 5. Checklist final (antes de subir app bundle)

- [ ] PRIVACIDADE_URL ativa e sem 404 no console
- [ ] App bundle release assinado (bundleRelease)
- [ ] Screenshots 6.7" e 8" (PT-BR), texto de listagem de 80-500 chars
  (BR), categoria Financas
- [ ] Data safety preenchido de verdade (nao "nao coleta nada" se ads)
- [ ] Teste de consentimento com estado FORMS_REQUIRED e sem obter tudo
- [ ] analytics `chart_tooltip_used` / `table_opened` chegando (sample)
- [ ] keystore no cofre, PRIVACIDADE_URL atualizada na listagem
- [ ] `assembleRelease` verde (R8 + shrink + keep ProfilingTrigger)

Cada item virara uma fatia Android dedicada em sincronia com o oraculo de
compilacao (1 write hermetico por fatia, oraculo isolado entre elas) e um
modelo `domain/rates` que JAMAIS lanca nem bloqueia a UI (contracto que a
sessao selou com RateAnnualizer e os 41 testes JVM).
