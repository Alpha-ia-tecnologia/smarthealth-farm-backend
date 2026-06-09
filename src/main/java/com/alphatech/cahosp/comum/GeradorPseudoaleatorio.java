package com.alphatech.cahosp.comum;

/**
 * Gerador pseudoaleatorio determinístico (mulberry32 + FNV-1a 32 bits), espelhando o PRNG
 * usado em {@code ../smarthealth-farm/src/data} do front. Mesma semente => mesma sequencia,
 * garantindo dados de seed reprodutiveis entre execucoes e fieis a distribuicao do mock.
 *
 * <p>Uso: {@code var r = GeradorPseudoaleatorio.comSemente("est" + codigo + sigla); r.proximo();}
 */
public final class GeradorPseudoaleatorio {

    private int estado;

    private GeradorPseudoaleatorio(int semente) {
        this.estado = semente;
    }

    /** Cria um gerador a partir de uma semente textual (hash FNV-1a). */
    public static GeradorPseudoaleatorio comSemente(String semente) {
        return new GeradorPseudoaleatorio(fnv1a(semente));
    }

    /** Proximo valor pseudoaleatorio no intervalo [0, 1). */
    public double proximo() {
        estado += 0x6d2b79f5;
        int t = estado;
        t = (t ^ (t >>> 15)) * (1 | t);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        int u = t ^ (t >>> 14);
        return Integer.toUnsignedLong(u) / 4294967296.0;
    }

    private static int fnv1a(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        return h;
    }
}
