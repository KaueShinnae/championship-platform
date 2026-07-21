package com.championship.partidas.application;

public class SemPermissaoException extends RuntimeException {

    public SemPermissaoException(String message) {
        super(message);
    }
}
