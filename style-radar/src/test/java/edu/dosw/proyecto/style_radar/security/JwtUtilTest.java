package edu.dosw.proyecto.style_radar.security;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class JwtUtilTest {
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtUtil jwt = new JwtUtil(SECRET, Duration.ofMinutes(60), clock);

    @Test
    void firmaValidaFechasYSubjectSinDatosSensibles() {
        // Act
        String token = jwt.generate(42L);
        var claims = Jwts.parser().verifyWith(key()).clock(() -> Date.from(NOW))
                .build().parseSignedClaims(token).getPayload();
        // Assert
        assertThat(jwt.validateAndGetUserId(token)).isEqualTo(42L);
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iat", "exp");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(jwt.expiresIn()).isEqualTo(3600);
        assertThat(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("password", "passwordHash", "email", "roles");
    }

    @Test
    void firmaManipuladaNoEsValida() {
        // Arrange
        String token = jwt.generate(1L);
        String[] pieces = token.split("\\.");
        byte[] signature = Base64.getUrlDecoder().decode(pieces[2]);
        signature[0] ^= 1;
        String modified = pieces[0] + "." + pieces[1] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        // Act & Assert
        assertThatThrownBy(() -> jwt.validateAndGetUserId(modified)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiracionDeterministaInclusoEnElLimiteExacto() {
        // Arrange
        String token = jwt.generate(1L);
        var expired = new JwtUtil(SECRET, Duration.ofHours(1), Clock.fixed(NOW.plusSeconds(3600), ZoneOffset.UTC));
        // Act & Assert
        assertThatThrownBy(() -> expired.validateAndGetUserId(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "a.b.c", "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0."})
    void rechazaEstructuraInvalidaYAlgoritmoNone(String token) {
        // Act & Assert
        assertThatThrownBy(() -> jwt.validateAndGetUserId(token))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "0", "-1", "+1", "01", "9223372036854775808"})
    void rechazaSubjectInvalidoAunqueLaFirmaSeaValida(String subject) {
        // Arrange
        String token = builder().subject(subject).signWith(key(), Jwts.SIG.HS256).compact();
        // Act & Assert
        assertThatThrownBy(() -> jwt.validateAndGetUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaAlgoritmoDiferenteConClaveCompatible() {
        // Arrange
        byte[] bytes = new byte[64];
        var configured = new JwtUtil(Base64.getEncoder().encodeToString(bytes), Duration.ofHours(1), clock);
        String token = builder().subject("1").signWith(new SecretKeySpec(bytes, "HmacSHA512"), Jwts.SIG.HS512).compact();
        // Act & Assert
        assertThatThrownBy(() -> configured.validateAndGetUserId(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not base64!", "c2hvcnQ="})
    void arranqueFallaSinClaveValidaDe256Bits(String secret) {
        // Act & Assert
        assertThatThrownBy(() -> new JwtUtil(secret, Duration.ofHours(1), clock))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET_BASE64")
                .hasMessageNotContaining("not base64!");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void rechazaDuracionNoPositiva(long seconds) {
        // Act & Assert
        assertThatThrownBy(() -> new JwtUtil(SECRET, Duration.ofSeconds(seconds), clock))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaDuracionNulaOInferiorAlSegundo() {
        assertThatThrownBy(() -> new JwtUtil(SECRET, null, clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil(SECRET, Duration.ofMillis(1), clock)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaFechasAusentesOFuturas() {
        // Arrange
        List<String> tokens = List.of(
                Jwts.builder().subject("1").signWith(key()).compact(),
                Jwts.builder().subject("1").expiration(Date.from(NOW.plusSeconds(60))).signWith(key()).compact(),
                Jwts.builder().subject("1").issuedAt(Date.from(NOW)).signWith(key()).compact(),
                Jwts.builder().subject("1").issuedAt(Date.from(NOW.plusSeconds(60)))
                        .expiration(Date.from(NOW.plusSeconds(120))).signWith(key()).compact(),
                Jwts.builder().subject("1").issuedAt(Date.from(NOW))
                        .expiration(Date.from(NOW.minusSeconds(1))).signWith(key()).compact());
        // Act & Assert
        tokens.forEach(token -> assertThatThrownBy(() -> jwt.validateAndGetUserId(token)).isInstanceOf(JwtException.class));
    }

    @Test
    void rechazaIdInvalidoAlEmitir() {
        assertThatThrownBy(() -> jwt.generate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.generate(0L)).isInstanceOf(IllegalArgumentException.class);
    }

    private JwtBuilder builder() {
        return Jwts.builder().issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(3600)));
    }
    private SecretKeySpec key() { return new SecretKeySpec(Base64.getDecoder().decode(SECRET), "HmacSHA256"); }
}
