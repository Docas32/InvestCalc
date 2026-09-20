# InvestCalc

App Android (Kotlin + Jetpack Compose) para simular investimentos em CDI/CDB e
Tesouro Direto, com taxas da API pública do BCB, gráfico de evolução, comparador e
guia educativo.

## Build

```
./gradlew :app:assembleDebug      # APK de debug
./gradlew :app:assembleRelease    # APK de release assinado
./gradlew :app:bundleRelease      # AAB para a Play Store
./gradlew :app:testDebugUnitTest  # testes unitários
```

## Assinatura de release

O APK/AAB de release é assinado com uma **upload key** através do arquivo
`keystore.properties` (na raiz do projeto, ignorado pelo Git — nunca o commite nem
compartilhe as senhas).

1. Gere o keystore (uma única vez; guarde-o com segurança, perdeu = não consegue
   publicar atualizações):

```
keytool -genkeypair -v -keystore investcalc-upload.jks -alias investcalc \
        -keyalg RSA -keysize 2048 -validity 10000
```

2. Preencha o `keystore.properties`:

```
storeFile=investcalc-upload.jks
storePassword=SENHA_DO_KEYSTORE
keyAlias=investcalc
keyPassword=SENHA_DO_ALIAS
```

3. Crie o bundle (a build falha com uma mensagem clara se o `keystore.properties`
   não estiver preenchido):

```
./gradlew :app:bundleRelease
```

> **Play App Signing**: a Google Play re-assina o seu app com a *app signing key*.
> A chave deste keystore é a *upload key* usada apenas para enviar versões; mantenha
> as duas em local seguro.

## Publicação na Play Store

Assets de listagem em `artifacts/`:

- `icone_512.png` — ícone 512×512.
- `screens/01..04_*.{pt,en}.png` — capturas geradas por `capturar_screens.sh`
  (formulário CDI, resultado com gráfico+tooltip, comparador com vencedor, guia).
- `privacy.html` — política de privacidade (host própria, ex.: GitHub Pages).
  A mesma política está em `PRIVACIDADE.md`.

Checklist:

- [ ] Conta de desenvolvedor na Google Play (U$ 25, taxa única).
- [ ] `investcalc-upload.jks` + `keystore.properties` preenchido (item acima).
- [ ] Enviar `app/build/outputs/bundle/release/app-release.aab` como versão inicial.
- [ ] **Data safety** (Ficha de dados do app): preencher informando coleta de
      análises (Firebase Analytics) e anúncios (AdMob); nenhum compartilhamento;
      transmissão criptografada.
- [ ] **Classificação de conteúdo** (IARC) e faixa etária.
- [ ] **URL da política de privacidade**: apontar para o `privacy.html` publicado
      (ex.: `https://SEU-USUARIO.github.io/investcalc/privacy.html`).
- [ ] Países/valores: definir preço e países (inicial pode ser apenas Brasil).
- [ ] Revisão da Google (pode levar alguns dias).

### Teste real do plano sem ads (3 passos)

1. **Track interno** no Play Console (`Internal testing`) com o AAB desta release
   publicado uma vez.
2. **Produto cadastrado** em *Monetização > Produtos*: id `remover_anuncios`,
   tipo produto no app (não-consumível), com preço em R$ definido.
3. **Testers de licença** (licença de teste) adicionados à track: sem pagamento
   real; ao comprar/restaurar, o Billing devolve o preço e confirma a compra.

### Respostas sugeridas do formulário Data safety

- O app coleta dados? **Sim** — Firebase Analytics (eventos anônimos de uso) e
  AdMob (anúncios).
- Compartilha dados com terceiros? **Não** (eventos anônimos de Analytics/AdMob,
  conforme o fluxo UMP de consentimento).
- Transmissão de dados criptografada? **Sim** (HTTPS).
- Coleta exigida/elementar: os eventos de Analytics e a exibição de AdMob são
  opcionais e controlados pelo consentimento UMP.