package com.championship.inscricoes.application;

/** Usuário autenticado sem papel para a ação — vira HTTP 403 no ApiExceptionHandler. */
public class SemPermissaoException extends RuntimeException {

    public SemPermissaoException(String message) {
        super(message);
    }
}
