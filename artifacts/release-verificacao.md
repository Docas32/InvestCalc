# Verificação da release — 09/10/2026

Versão 1.0.0 (versionCode 1), pacote `br.investcalc.app`.
Aparelho físico Xiaomi, modelo `23028RA60L`, tela 1080 × 2400.
Conexão Wi-Fi ADB: `192.168.0.103:41537`.

## Build e assinatura

- `./gradlew :app:bundleRelease :app:assembleRelease :app:testDebugUnitTest`: **BUILD SUCCESSFUL**.
- 136 testes unitários em 26 suites, sem falhas ou erros.
- APK: `app/build/outputs/apk/release/app-release.apk`.
- AAB: `app/build/outputs/bundle/release/app-release.aab`.
- APK validado com `apksigner`; certificado do AAB conferido com `keytool`.
- Certificado SHA-256: `80b5ffee5b5210a69b3aabe3752d6f5ce93747a03d35172b4de5808a0318e756`.
- APK SHA-256: `b6e53399ec3759e0d997fd6f3ae7b37ef82499e2174efdeb93d1a38d2d2dc9bb`.
- AAB SHA-256: `7fc7fb6e6902c22eaffbc81ef53ec4c9a99665ec322460072d44f454823d3c16`.
- O SHA-256 do APK instalado no celular é idêntico ao artefato acima.

## Smoke test no aparelho

- Versão anterior desinstalada; release final instalada com sucesso.
- Dados de teste limpos antes da primeira abertura da release corrigida.
- UMP atualizou o status antes de liberar anúncios. O servidor retornou
  `IABTCF_gdprApplies=0`: não exigiu formulário nesta região.
  **O cenário REQUIRED com formulário não foi validado nesta release/aparelho.**
- Banner identificado como anúncio de teste nas três abas: Calculadora,
  Tesouro e Comparar.
- Simulação: R$ 1.000 iniciais, R$ 200 mensais, 12 meses, 100% do CDI,
  DI de 13,9% a.a. Resultado: bruto **R$ 3.688,31**, IR **R$ 57,66**,
  líquido **R$ 3.630,65**. Conferido também em inglês.
- Ajustes sem recurso de teste de crash.
- Ícone verde confirmado visualmente na gaveta de apps.
- Intersticial de teste exibido após a sequência de simulações e fechado
  antes de continuar as capturas.

## Correções encontradas durante o teste

- Banner adaptativo: largura passada em dp ao SDK; a largura em pixels
  causava `Ad size will not fit on screen` e erro de carregamento 1.
- UMP: carregamento do formulário movido para o callback de sucesso da
  atualização de consentimento, eliminando a chamada antes de obter o status.

## Screenshots oficiais

As oito imagens em `artifacts/screens/` foram recapturadas **depois** da
validação no aparelho, em PNG 1080 × 2400. Cada captura passou pela
verificação de teclado oculto; o conjunto foi revisado visualmente.

| Tela | Português | Inglês |
| --- | --- | --- |
| Formulário preenchido | [PT](screens/01_formulario_cdi.pt.png) | [EN](screens/01_formulario_cdi.en.png) |
| Resultado e gráfico com tooltip | [PT](screens/02_resultado_grafico_tooltip.pt.png) | [EN](screens/02_resultado_grafico_tooltip.en.png) |
| Comparador com vencedor | [PT](screens/03_comparador_vencedor.pt.png) | [EN](screens/03_comparador_vencedor.en.png) |
| Guia educativo | [PT](screens/04_guia.pt.png) | [EN](screens/04_guia.en.png) |

O idioma específico do app foi removido ao concluir as capturas, restaurando
o idioma do aparelho.

## Documentação e keystore

- `https://docas32.github.io/investcalc-docs/`: HTTP **200**, verificado com curl.
- Keystore de upload existente: `/home/libai/keys/investcalc-upload.jks`.
  Certificado gerado em 08/10/2026, válido até 23/02/2054.
- Backup local criado em 09/10/2026:
  `.keystore-backups/investcalc-keystore-2026-10-09.zip`, com keystore,
  credenciais, instruções de restauração e hashes dos arquivos.
  Integridade do ZIP e hashes verificados. Pasta com permissão 0700,
  arquivo com permissão 0600 e excluído do Git.
- ZIP SHA-256: `4944cd4d09b26d71ef450b9d4797ea67b82774b3644ed5c5eb132792e6939a57`.
- Segunda cópia enviada ao Google Drive em 09/10/2026 usando o plugin
  Google Drive. Pasta privada
  [investcalc](https://drive.google.com/drive/folders/1GenUWx-ab6iW9rbXvz69k-c1YW2fHkzb).
- [Backup no Drive](https://drive.google.com/file/d/1zjAF8ElMBaNigaF1dGxR6qX3BTL6a3u8/view):
  arquivo `investcalc-keystore-2026-10-09.zip`, ID
  `1zjAF8ElMBaNigaF1dGxR6qX3BTL6a3u8`, 3.518 bytes.
  Upload concluído; leitura posterior confirmou nome, tamanho, pasta,
  `shared=false` e somente permissão de proprietário.
- Backups em dois destinos: cópia local no disco do projeto e cópia remota
  no Google Drive. Senhas não foram incluídas neste relatório.
