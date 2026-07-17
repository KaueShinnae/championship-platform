package com.championship.inscricoes.application;

/** Requisição sem sessão válida — vira HTTP 401 no ApiExceptionHandler. */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String message) {
        super(message);
    }
}
