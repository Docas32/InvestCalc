#!/usr/bin/env bash
#
# Captura as screenshots da listagem da Play Store via adb (InvestCalc):
# formulario CDI, resultado com grafico + tooltip, Comparador com vencedor e
# Guia (via Ajustes -> "Guia educativo", desde P8 o Guia nao e aba da barra),
# em pt-BR e em EN. Saida: artifacts/screens/<01..04>_*.{pt,en}.png
#
# Pre-requisitos:
#   - app Installed no aparelho/aparelho adb conectado (debug ou release);
#   - consentimento UMP: na 1a abertura o app mostra o formulario; o script
#     pausa e orienta a responder antes de capturar.
#
# Uso:  ./capturar_screens.sh              # pt-BR + EN
#       ADB_SERIAL=192.168.0.x:PORT ./capturar_screens.sh
#       ./capturar_screens.sh pt           # so pt-BR
#       ./capturar_screens.sh en           # so en
set -u

PKG="br.investcalc.app"
ACT=".MainActivity"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SAIDA="$DIR/screens"
mkdir -p "$SAIDA"

ADB="${ADB:-adb}"
SERIAL="${ADB_SERIAL:-}"
declare -a BASE=( "$ADB" )
[ -n "$SERIAL" ] && BASE=( "$ADB" -s "$SERIAL" )

S() { "${BASE[@]}" shell "$@"; }
P() { "${BASE[@]}" exec-out "$@"; }

# ---- Labels por idioma -----------------------------------------------------
find_text() { # <texto> <preferencia:baixo|alto> -> imprime "cx cy"
  dump_ui | python3 -c '
import sys, re
txt, pref = sys.argv[1], sys.argv[2]
doc = sys.stdin.read()
melhor, my = None, -1
padroes = (r"text=\"([^\"]*)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
           r"content-desc=\"([^\"]*)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"")
for padrao in padroes:
    for m in re.finditer(padrao, doc):
        if m.group(1) != txt:
            continue
        l, t, r, b = (int(g) for g in m.group(2, 3, 4, 5))
        cx, cy = (l + r) // 2, (t + b) // 2
        if pref == "alto":
            if melhor is None or cy < my:
                melhor, my = (cx, cy), cy
        elif cy >= my:
            melhor, my = (cx, cy), cy
if melhor:
    print(melhor[0], melhor[1])
    sys.exit(0)
sys.exit(1)
' "$1" "${2:-baixo}"
}

dump_ui() {
  local i out=""
  for i in $(seq 1 8); do
    S uiautomator dump /sdcard/investcalc_ui.xml >/dev/null 2>&1
    out="$(S cat /sdcard/investcalc_ui.xml 2>/dev/null)"
    case "$out" in *"<hierarchy"*) printf '%s' "$out"; return 0 ;; esac
    sleep 1
  done
  return 1
}

centro() { dump_ui | find_text "$1" "${2:-baixo}"; }

tocar() { # <texto> <preferencia>
  local p
  p="$(centro "$1" "${2:-baixo}")" || return 1
  S input tap $p
  return 0
}

rolar_ate() { # <texto> [max tentativas]
  local txt="$1" n="${2:-8}" p
  while [ "$n" -gt 0 ]; do
    p="$(centro "$txt")" && { S input tap $p; return 0; }
    S input swipe 540 1500 540 1050 200
    sleep 1
    n=$((n - 1))
  done
  echo "AVISO: nao achei '$txt' na tela." >&2
  return 1
}

capturar() {
  P screencap -p > "$SAIDA/$1"
  echo ">>> capturado $SAIDA/$1"
}

TAM="$(S wm size 2>/dev/null | grep -oE '[0-9]+x[0-9]+' | head -1 | tr 'x' ' ')"
[ -z "$TAM" ] && { echo "Sem tamanho de tela; aborte."; exit 1; }
set -- $TAM
W=$1; H=$2
SW_X1=$((W * 45 / 100)); SW_X2=$((W * 58 / 100))

respira() { sleep "$1"; }

ativar_tela() {
  S input keyevent KEYCODE_WAKEUP
  respira 1
  if S dumpsys window 2>/dev/null | grep -q 'isKeyguardShowing=true'; then
    S input keyevent KEYCODE_MENU
    respira 1
    S input swipe $((W / 2)) $((H * 4 / 5)) $((W / 2)) $((H / 5)) 400
    respira 2
  fi
}

conjunto() {
  local locale="$1" lbl_nav_calc="$2" lbl_simular="$3" lbl_nav_compare="$4" lbl_nav_ajustes="$5" lbl_guia_item="$6" sufixo="$7"
  echo "=== idioma $sufixo ($locale) ==="
  ativar_tela
  S cmd locale set-app-locales "$PKG" --locales "$locale" >/dev/null 2>&1
  S am force-stop "$PKG"
  sleep 2
  S am start -W -n "$PKG/$ACT" >/dev/null 2>&1
  respira 6

  if dump_ui | grep -qiE 'consent|privacidade|privacy|personalizar|personalized'; then
    echo ">> Apareceu o formulario UMP de consentimento. Escolha as opcoes no"
    echo ">> aparelho (recomendado: aceitar) e depois digite ENTER aqui."
    read -t 90 -r -p ">> [ENTER para continuar ou 90s] " _ || echo ">> (continuando)"
  fi
  respira 2

  tocar "$lbl_nav_calc" || { echo "Falhou ao abrir a aba Calculadora."; return 1; }
  respira 2
  capturar "01_formulario_cdi.${sufixo}.png"

  rolar_ate "$lbl_simular" || return 1
  respira 3

  local ty achou="" px
  local g0=$((H * 37 / 100)) g1=$((H * 64 / 100))
  P screencap -p > "$SAIDA/.tmp_tooltip.png"
  for ty in 50 44 54 46; do
    S input swipe $SW_X1 $((H * ty / 100)) $SW_X2 $((H * ty / 100)) 400
    respira 2
    P screencap -p > "$SAIDA/.tmp_tooltip2.png"
    px="$(python3 "$DIR/dif_png.py" "$SAIDA/.tmp_tooltip.png" "$SAIDA/.tmp_tooltip2.png" 0 "$g0" "$W" "$g1" 2>/dev/null || echo 0)"
    if [ "${px:-0}" -gt 1200 ]; then achou=1; break; fi
  done
  rm -f "$SAIDA/.tmp_tooltip.png" "$SAIDA/.tmp_tooltip2.png"
  [ -n "$achou" ] || echo "AVISO: nao detectei tooltip (try manual)." >&2
  capturar "02_resultado_grafico_tooltip.${sufixo}.png"

  tocar "$lbl_nav_compare" || { echo "Falhou ao abrir Comparar."; return 1; }
  respira 2
  rolar_ate "$lbl_simular" || return 1
  respira 3
  capturar "03_comparador_vencedor.${sufixo}.png"

  # Guia (P8): nao e mais aba — abrimos Ajustes e tocamos no item "Guia
  # educativo" (mesmo texto do titulo da tela), depois voltamos.
  tocar "$lbl_nav_ajustes" || { echo "Falhou ao abrir Ajustes."; return 1; }
  respira 2
  rolar_ate "$lbl_guia_item" || return 1
  respira 2
  capturar "04_guia.${sufixo}.png"
  S input keyevent KEYCODE_BACK
  respira 1
}

fazer_pt() { conjunto "pt-BR" "Calculadora" "Simular" "Comparar" "Ajustes" "Guia educativo" "pt"; }
fazer_en() { conjunto "en-US" "Calculator" "Simulate" "Compare" "Settings" "Educational guide" "en"; }

case "${1:-}" in
  pt) fazer_pt ;;
  en) fazer_en ;;
  *)  fazer_pt; fazer_en ;;
esac

S am force-stop "$PKG"
S cmd locale set-app-locales "$PKG" >/dev/null 2>&1
LOCALE_PERSIST="$(S getprop persist.sys.locale 2>/dev/null | tr -d '\r')"
if [ -n "$LOCALE_PERSIST" ] && [ "$LOCALE_PERSIST" != "null" ]; then
  S settings put system system_locales "$LOCALE_PERSIST" >/dev/null 2>&1
  echo ">>> idioma do aparelho restaurado para: $LOCALE_PERSIST"
fi
echo ">>> screenshots em $SAIDA"
ls -la "$SAIDA"