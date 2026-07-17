package com.championship.inscricoes.infrastructure.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha com PBKDF2 (JDK puro, sem dependência nova). Formato
 * armazenado: {@code pbkdf2$<iteracoes>$<saltBase64>$<hashBase64>}.
 * Suficiente para o estágio atual do produto; trocar por biblioteca dedicada
 * se a plataforma sair do escopo de demo.
 */
@Component
public class PasswordHasher {

    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    public String hash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(senha, salt, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean verificar(String senha, String armazenado) {
        try {
            String[] partes = armazenado.split("\\$");
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(senha, salt, iteracoes);
            return java.security.MessageDigest.isEqual(esperado, calculado);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String senha, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("falha ao calcular hash de senha", e);
        }
    }
}
