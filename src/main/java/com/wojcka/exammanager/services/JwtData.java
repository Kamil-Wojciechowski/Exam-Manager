package com.wojcka.exammanager.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtData {
    @Getter
    private static String secretKey;

    @Getter
    private static Long expiartionTime;

    @Value("${spring.secret}")
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Value("${token.time}")
    public void setexpiartionTime(Long expiartionTime) {
        this.expiartionTime = expiartionTime;
    }



}
