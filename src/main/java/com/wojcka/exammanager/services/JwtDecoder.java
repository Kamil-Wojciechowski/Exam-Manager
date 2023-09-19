package com.wojcka.exammanager.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

public class JwtDecoder {
    private JwtData jwtData;

    private String token;

    @Getter
    private String email;

    @Getter
    private Boolean expired;

    public JwtDecoder(String token) {
        this.token = token;
        extractEmail();
        isTokenExpired();
    }

    private void extractEmail() {
        email = extractClaim(this.token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private void isTokenExpired() {
        expired = extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtData.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
