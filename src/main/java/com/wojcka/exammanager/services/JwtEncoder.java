package com.wojcka.exammanager.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;

public class JwtEncoder {
    private JwtData jwtData;

    @Getter
    private String token;

    @Getter
    private Date issuedAt;

    @Getter
    private Date expiresAt;
    public JwtEncoder(UserDetails user) {
        issuedAt = new Date(System.currentTimeMillis());
        expiresAt = new Date(System.currentTimeMillis() + jwtData.getExpiartionTime());
        token= Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtData.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
