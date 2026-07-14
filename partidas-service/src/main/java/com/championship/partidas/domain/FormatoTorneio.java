package com.championship.partidas.domain;

/** Formatos de competição suportados (regras fixas — ver SPEC de formatos). */
public enum FormatoTorneio {
    GRUPOS_PLAYOFFS,
    PLAYOFFS,
    PONTOS_CORRIDOS;

    /** Mínimo de times confirmados para o sorteio ser possível. */
    public int minimoDeTimes() {
        return switch (this) {
            case GRUPOS_PLAYOFFS -> 6;
            case PLAYOFFS -> 2;
            case PONTOS_CORRIDOS -> 3;
        };
    }
}
