package com.championship.partidas.application;

/** Usuário autenticado mas sem papel de gestão no campeonato (HTTP 403). */
public class SemPermissaoException extends RuntimeException {

    public SemPermissaoException(String message) {
        super(message);
    }
}
