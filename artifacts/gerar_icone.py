#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera artifacts/icone_512.png: glifo do launcher (ic_launcher_foreground.xml)
sobre fundo indigo (#5C6BC0), 512x512, fiel aos pathData do vetor Android:

  - 3 barras brancas crescentes: (34,60..42,78), (50,46..58,78), (66,32..74,78)
  - seta ascendente: traco arredondado (73,33)->(83,22) largura 5 + ponta
    triangular (83,22)-(76,28)-(80,29).

Rasterizacao propria (sem PIL/numpy): supersampling 4x com anti-alias por
bloco. Saida: PNG RGBA de 512x512.
"""

import struct
import sys
import zlib

OUT = 512
SS = 4
WS = OUT * SS
COR_FUNDO = (0x5C, 0x6B, 0xC0)  # ic_launcher_background
COR_GLIFO = (0xFF, 0xFF, 0xFF)

BAR = ((34, 60, 42, 78), (50, 46, 58, 78), (66, 32, 74, 78))
LIN_A, LIN_B, LIN_R = (73.0, 33.0), (83.0, 22.0), 2.5
TRI = ((83, 22), (76, 28), (80, 29))


def dentro_ret(x, y, r):
    x0, y0, x1, y1 = r
    return x0 <= x <= x1 and y0 <= y <= y1


def sinal(p, a, b):
    return (p[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (p[1] - b[1])


def dentro_tri(x, y):
    p = (x, y)
    s1 = sinal(p, TRI[0], TRI[1])
    s2 = sinal(p, TRI[1], TRI[2])
    s3 = sinal(p, TRI[2], TRI[0])
    neg = s1 < 0 or s2 < 0 or s3 < 0
    pos = s1 > 0 or s2 > 0 or s3 > 0
    return not (neg and pos)


_AX, _AY = LIN_A
_BX, _BY = LIN_B
_DX, _DY = _BX - _AX, _BY - _AY
_LEN2 = _DX * _DX + _DY * _DY


def dentro_linha(x, y):
    t = ((x - _AX) * _DX + (y - _AY) * _DY) / _LEN2
    if t < 0.0:
        t = 0.0
    elif t > 1.0:
        t = 1.0
    cx = _AX + t * _DX
    cy = _AY + t * _DY
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= LIN_R * LIN_R


def cobertura(x, y):
    for r in BAR:
        if dentro_ret(x, y, r):
            return 1.0
    if dentro_tri(x, y):
        return 1.0
    if dentro_linha(x, y):
        return 1.0
    return 0.0


def main():
    scale = 108.0 / WS
    grid = bytearray(WS * WS)
    for py in range(WS):
        vy = (py + 0.5) * scale
        base = py * WS
        for px in range(WS):
            vx = (px + 0.5) * scale
            grid[base + px] = 1 if cobertura(vx, vy) > 0.5 else 0
    precision = [sum(grid[(by * SS + i) * WS + (bx * SS + j)]
                     for i in range(SS) for j in range(SS))
                 for by in range(OUT) for bx in range(OUT)]

    def escrever_png(path):
        linhas = []
        for y in range(OUT):
            fila = bytearray()
            for x in range(OUT):
                cov = precision[y * OUT + x] / (SS * SS)
                r = round(COR_FUNDO[0] + (COR_GLIFO[0] - COR_FUNDO[0]) * cov)
                g = round(COR_FUNDO[1] + (COR_GLIFO[1] - COR_FUNDO[1]) * cov)
                b = round(COR_FUNDO[2] + (COR_GLIFO[2] - COR_FUNDO[2]) * cov)
                fila += bytes((r, g, b, 255))
            linhas.append(b'\x00' + fila)
        raw = b''.join(linhas)

        def chunk(tag, dados):
            ch = struct.pack('>I', len(dados)) + tag + dados
            return ch + struct.pack('>I', zlib.crc32(tag + dados) & 0xFFFFFFFF)

        ihdr = struct.pack('>IIBBBBB', OUT, OUT, 8, 6, 0, 0, 0)
        png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) +
               chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))
        with open(path, 'wb') as f:
            f.write(png)

    caminho = sys.argv[1] if len(sys.argv) > 1 else 'artifacts/icone_512.png'
    escrever_png(caminho)
    print('OK %s (512x512)' % caminho)


if __name__ == '__main__':
    main()