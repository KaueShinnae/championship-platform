package com.championship.partidas.domain;

public enum FormatoTorneio {
    GRUPOS_PLAYOFFS,
    PLAYOFFS,
    PONTOS_CORRIDOS;

    public int minimoDeTimes() {
        return switch (this) {
            case GRUPOS_PLAYOFFS -> 6;
            case PLAYOFFS -> 2;
            case PONTOS_CORRIDOS -> 3;
        };
    }
}
