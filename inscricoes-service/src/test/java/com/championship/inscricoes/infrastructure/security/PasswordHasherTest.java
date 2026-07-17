package com.championship.inscricoes.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verificaSenhaCorretaERejeitaIncorreta() {
        String hash = hasher.hash("minha-senha-123");

        assertThat(hasher.verificar("minha-senha-123", hash)).isTrue();
        assertThat(hasher.verificar("outra-senha", hash)).isFalse();
        assertThat(hasher.verificar("minha-senha-123", "lixo-invalido")).isFalse();
    }

    @Test
    void mesmoTextoGeraHashesDiferentesPorCausaDoSalt() {
        assertThat(hasher.hash("senha")).isNotEqualTo(hasher.hash("senha"));
    }
}
