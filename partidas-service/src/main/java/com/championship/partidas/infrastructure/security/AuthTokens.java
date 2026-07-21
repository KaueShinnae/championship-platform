package com.championship.partidas.infrastructure.security;

import com.championship.partidas.application.NaoAutenticadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthTokens {

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

    public record Sessao(UUID id, String nome) {
    }

    public UUID exigirUsuario(HttpServletRequest request) {
        return sessao(request).id();
    }

    public Sessao sessao(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NaoAutenticadoException("entre na sua conta para realizar esta acao");
        }
        String token = header.substring("Bearer ".length());
        try {
            String[] partes = token.split("\\.");
            byte[] esperado = hmac(partes[0]);
            byte[] recebido = Base64.getUrlDecoder().decode(partes[1]);
            if (!MessageDigest.isEqual(esperado, recebido)) {
                throw new NaoAutenticadoException("sessao invalida — entre novamente");
            }
            Map<?, ?> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(partes[0]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new NaoAutenticadoException("sessao expirada — entre novamente");
            }
            String nome = payload.get("nome") == null ? "—" : payload.get("nome").toString();
            return new Sessao(UUID.fromString((String) payload.get("sub")), nome);
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
}
