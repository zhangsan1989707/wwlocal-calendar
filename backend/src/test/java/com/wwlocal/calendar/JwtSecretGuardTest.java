package com.wwlocal.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wwlocal.calendar.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Guards against re-introducing a hardcoded JWT signing secret. If the production
 * source ever regresses to a literal default secret, anyone with read access to
 * the repository can forge admin tokens. These tests fail loudly in that case.
 */
class JwtSecretGuardTest {

  @Test
  void missingSecretFailsFast() {
    assertThatThrownBy(() -> new JwtUtil("", 86_400_000L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app.jwt.secret");
  }

  @Test
  void shortSecretIsRejected() {
    assertThatThrownBy(() -> new JwtUtil("too-short", 86_400_000L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  @Test
  void previouslyHardcodedSecretIsNoLongerAccepted() {
    // This literal was committed in JwtUtil.java prior to the fix and is in git
    // history. The application must never sign or verify with it as a default.
    var leaked = "wwlocal-calendar-jwt-secret-key-2026-min-256bits";
    var leakedKey = Keys.hmacShaKeyFor(leaked.getBytes(StandardCharsets.UTF_8));
    var forgedToken = Jwts.builder()
        .subject("attacker-user-id")
        .claims(java.util.Map.of("username", "attacker", "role", "admin"))
        .issuedAt(new java.util.Date())
        .expiration(new java.util.Date(System.currentTimeMillis() + 3_600_000L))
        .signWith(leakedKey)
        .compact();

    var util = new JwtUtil("a-different-secret-that-is-long-enough-for-hs256-32+", 3_600_000L);
    assertThat(util.validateToken(forgedToken))
        .as("token signed with the old hardcoded secret must not be valid under a new key")
        .isFalse();
  }
}
