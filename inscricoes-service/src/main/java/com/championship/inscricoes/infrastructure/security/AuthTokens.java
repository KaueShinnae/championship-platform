package com.championship.inscricoes.infrastructure.security;

import com.championship.inscricoes.application.NaoAutenticadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Token de sessão compacto assinado com HMAC-SHA256 e segredo compartilhado
 * entre os serviços (env AUTH_SECRET): {@code base64url(json)}.{@code base64url(hmac)}.
 * Grau de segurança de desenvolvimento — sem refresh/revogação; expiração de 7 dias.
 */
@Component
public class AuthTokens {

    private static final Duration VALIDADE = Duration.ofDays(7);

    private final byte[] segredo;
    private final ObjectMapper objectMapper;

    public AuthTokens(@Value("${app.auth.secret:}") String segredo, ObjectMapper objectMapper) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "AUTH_SECRET nao definido — o servico nao sobe sem o segredo de sessao. "
                            + "Defina a variavel de ambiente AUTH_SECRET (ver .env.example); "
                            + "os scripts de dev (scripts/dev-up.ps1) definem um valor de desenvolvimento.");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public String emitir(UUID usuarioId, String nome) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "sub", usuarioId.toString(),
                    "nome", nome,
                    "exp", Instant.now().plus(VALIDADE).getEpochSecond()));
            String corpo = base64Url(payload.getBytes(StandardCharsets.UTF_8));
            return corpo + "." + base64Url(hmac(corpo));
        } catch (Exception e) {
            throw new IllegalStateException("falha ao emitir token", e);
        }
    }

    /** Id do usuário autenticado; lança 401 se o token faltar, expirar ou for inválido. */
    public UUID exigirUsuario(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NaoAutenticadoException("entre na sua conta para realizar esta acao");
        }
        return validar(header.substring("Bearer ".length()));
    }

    /** Como {@link #exigirUsuario}, mas retorna null em vez de falhar (rotas de leitura). */
    public UUID usuarioOpcional(HttpServletRequest request) {
        try {
            return exigirUsuario(request);
        } catch (NaoAutenticadoException e) {
            return null;
        }
    }

    private UUID validar(String token) {
        try {
            String[] partes = token.split("\\.");
            byte[] esperado = hmac(partes[0]);
            byte[] recebido = Base64.getUrlDecoder().decode(partes[1]);
            if (!MessageDigest.isEqual(esperado, recebido)) {
                throw new NaoAutenticadoException("sessao invalida — entre novamente");
            }
            Map<?, ?> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(partes[0]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new NaoAutenticadoException("sessao expirada — entre novamente");
            }
            return UUID.fromString((String) payload.get("sub"));
        } catch (NaoAutenticadoException e) {
            throw e;
        } catch (Exception e) {
            throw new NaoAutenticadoException("sessao invalida — entre novamente");
        }
    }

    private byte[] hmac(String corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
        return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
