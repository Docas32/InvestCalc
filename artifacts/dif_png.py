#!/usr/bin/env python3
import struct
import sys
import zlib


def decodificar_png(caminho):
    with open(caminho, "rb") as f:
        dados = f.read()
    if dados[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("nao e PNG")
    pos = 8
    largura = altura = None
    bitdepth = colortype = 0
    canais_por_cor = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}
    idat = b""
    while pos < len(dados):
        compr, tipo = struct.unpack(">I4s", dados[pos:pos + 8])
        bloco = dados[pos + 8:pos + 8 + compr]
        if tipo == b"IHDR":
            largura, altura, bitdepth, colortype = struct.unpack(">IIBB", bloco[:10])
        elif tipo == b"IDAT":
            idat += bloco
        elif tipo == b"IEND":
            break
        pos += 12 + compr
    if bitdepth != 8:
        raise ValueError("bitdepth %d nao suportado" % bitdepth)
    nc = canais_por_cor[colortype]
    bpp = nc
    raw = zlib.decompress(idat)
    stride = largura * nc
    img = bytearray(largura * altura * nc)
    prev = bytearray(stride)
    off = 0
    for y in range(altura):
        filtro = raw[off]
        off += 1
        linha = bytearray(raw[off:off + stride])
        off += stride
        if filtro == 1:
            for x in range(bpp, stride):
                linha[x] = (linha[x] + linha[x - bpp]) & 255
        elif filtro == 2:
            for x in range(stride):
                linha[x] = (linha[x] + prev[x]) & 255
        elif filtro == 3:
            for x in range(stride):
                a = linha[x - bpp] if x >= bpp else 0
                linha[x] = (linha[x] + ((a + prev[x]) >> 1)) & 255
        elif filtro == 4:
            for x in range(stride):
                a = linha[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                linha[x] = (linha[x] + pred) & 255
        img[y * stride:(y + 1) * stride] = linha
        prev = linha
    return largura, altura, nc, img


def pixel(img, nc, x, y, largura):
    i = (y * largura + x) * nc
    return tuple(img[i:i + nc])


def diff(a, b, limiar=24, regiao=None):
    wa, ha, na, ia = decodificar_png(a)
    wb, hb, nb, ib = decodificar_png(b)
    if (wa, ha, na) != (wb, hb, nb):
        raise ValueError("dimensoes diferentes")
    if regiao is None:
        regiao = (0, 0, wa, ha)
    x0, y0, x1, y1 = regiao
    n = 0
    for y in range(max(0, y0), min(ha, y1)):
        for x in range(max(0, x0), min(wa, x1)):
            p = pixel(ia, na, x, y, wa)
            q = pixel(ib, nb, x, y, wb)
            if max(abs(p[k] - q[k]) for k in range(na)) > limiar:
                n += 1
    return n


if __name__ == "__main__":
    a = sys.argv[1]
    b = sys.argv[2]
    regiao = tuple(int(v) for v in sys.argv[3:7]) if len(sys.argv) >= 7 else None
    print(diff(a, b, regiao=regiao))