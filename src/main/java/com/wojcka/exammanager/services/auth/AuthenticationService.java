package com.wojcka.exammanager.services.auth;

import com.wojcka.exammanager.components.EmailComponent;
import com.wojcka.exammanager.controllers.auth.requests.AuthenticationRequest;
import com.wojcka.exammanager.controllers.auth.responses.AuthenticationResponse;
import com.wojcka.exammanager.controllers.responses.GenericResponse;
import com.wojcka.exammanager.models.token.Token;
import com.wojcka.exammanager.models.token.TokenType;
import com.wojcka.exammanager.models.user.User;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("spring.auth.expiration.recovery")
    private Long recoveryExpiration;

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final TokenRepository tokenRepository;

    private final StrongTextEncryptor textEncryptor;
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public GenericResponse recovery(String email) {
        User user = repository.findByEmail(email).orElseThrow();

        String keyUUID = UUID.randomUUID().toString();
        String secretUUID = UUID.randomUUID().toString();


        textEncryptor.setPassword(secretUUID);
        String encryptedKey = textEncryptor.encrypt(keyUUID);

        tokenRepository.save(Token.builder()
                .tokenType(TokenType.RECOVERY)
                .user(user)
                .hashedToken(encryptedKey)
                .createdAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(recoveryExpiration))
                .build());

        EmailComponent.sendEmail(user.getEmail(), "Recovery", keyUUID + " | " + secretUUID);

        return GenericResponse.builder().code(201).status("CREATED").data("Email has been send").build();
    }
}
