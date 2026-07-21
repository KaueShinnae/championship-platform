package com.championship.inscricoes.application;

public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String message) {
        super(message);
    }
}
