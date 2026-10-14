package edu.dosw.proyecto.style_radar.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final Clock clock;
    private final Duration duration;
    private final JwtParser parser;

    public JwtUtil(@Value("${styleradar.security.jwt.secret-base64:}") String secret,
                   @Value("${styleradar.security.jwt.duration:PT60M}") Duration duration, Clock clock) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 debe contener una clave Base64 válida");
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 es requerida y debe contener al menos 256 bits");
        }
        if (duration == null || duration.isNegative() || duration.isZero() || duration.toSeconds() < 1) {
            throw new IllegalStateException("La duración JWT debe ser al menos un segundo");
        }
        this.key = new SecretKeySpec(decoded, "HmacSHA256");
        this.clock = clock;
        this.duration = duration;
        // Solo HS256: no aceptar algoritmos elegidos por el emisor del token.
        this.parser = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant()))
                .sig().clear().add(Jwts.SIG.HS256).and().build();
    }

    public String generate(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Identificador de usuario inválido");
        }
        var now = clock.instant();
        return Jwts.builder().subject(usuarioId.toString()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration))).signWith(key, Jwts.SIG.HS256).compact();
    }

    public Long validateAndGetUserId(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null || claims.getIssuedAt() == null
                || !claims.getExpiration().after(Date.from(clock.instant()))
                || claims.getIssuedAt().after(Date.from(clock.instant()))
                || !claims.getExpiration().after(claims.getIssuedAt())) {
            throw new JwtException("Fechas JWT inválidas");
        }
        try {
            String subject = claims.getSubject();
            if (subject == null || !subject.matches("[1-9][0-9]*")) {
                throw new NumberFormatException();
            }
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new JwtException("Subject JWT inválido");
        }
    }

    public long expiresIn() { return duration.toSeconds(); }
}
