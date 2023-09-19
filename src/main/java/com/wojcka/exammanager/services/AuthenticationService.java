package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.components.language.Translator;
import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.RecoveryRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.models.Token;
import com.wojcka.exammanager.models.TokenType;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${spring.jpa.auth.expiration.recovery}")
    private Long recoveryExpiration;

    @Value("${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;

    @Value("${spring.jpa.auth.expiration.refresh}")
    private Long refreshExpiration;


    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final TokenRepository tokenRepository;

    private final StrongTextEncryptor textEncryptor = new StrongTextEncryptor();

    @Autowired
    private EmailService emailService;

    private User authenticateUser(String email, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password
                )
        );

        return (User) authentication.getPrincipal();
    }

    private AuthenticationResponse createAuthenticateResponse(UserDetails user, Token refreshToken) {
        JwtEncoder jwtServiceEncoder = new JwtEncoder(user);

        return AuthenticationResponse.builder()
                .token(jwtServiceEncoder.getToken())
                .refreshToken(refreshToken.getHashedToken())
                .issued(jwtServiceEncoder.getIssuedAt())
                .expires(jwtServiceEncoder.getExpiresAt())
                .build();
    }

    private Token buildRefreshToken(User user) {
        Token refreshToken = Token.builder()
                .tokenType(TokenType.REFRESH_TOKEN)
                .hashedToken(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()))
                .expirationDate(LocalDateTime.now().plusDays(refreshExpiration))
                .user(user)
                .build();

        return tokenRepository.save(refreshToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = authenticateUser(request.getEmail(), request.getPassword());

       Token refreshToken = buildRefreshToken(user);

        JwtEncoder jwtServiceEncoder = new JwtEncoder(user);

        return createAuthenticateResponse(user, refreshToken);
    }

    private Token validateRefresh(String token) {
        Token refreshToken = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });

        if(!refreshToken.isTokenRefresh() & refreshToken.isTokenExpired()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        }

        return refreshToken;
    }

    public AuthenticationResponse refresh(String token) {
        Token refreshToken = validateRefresh(token);

        return createAuthenticateResponse(refreshToken.getUser(), refreshToken);
    }

    private String encryptString(String key, String token) {
        textEncryptor.setPassword(key);
        return textEncryptor.encrypt(token);
    }

    public GenericResponse recovery(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("email_not_found"));
        });

        String secretUUID = UUID.randomUUID().toString();

        tokenRepository.save(Token.builder()
                .tokenType(TokenType.RECOVERY)
                .user(user)
                .hashedToken(secretUUID)
                .createdAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(recoveryExpiration))
                .build());

        emailService.sendEmail(user.getEmail(), "Recovery",  secretUUID);

        return GenericResponse.builder().code(HttpStatus.CREATED.value()).status(HttpStatus.CREATED.toString()).data(Translator.toLocale("email_has_been_send")).build();
    }

    private Token validateRecovery(String token) {
        Token recoveryToken = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });

        if(!recoveryToken.isTokenRecover()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_wrong_type"));
        }

        if(recoveryToken.isTokenExpired()) {
            tokenRepository.delete(recoveryToken);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_expired_recovery"));
        }

        return recoveryToken;
    }
    public GenericResponse recovery(String token, RecoveryRequest request) {
        Token recoveryToken = validateRecovery(token);

        User user = recoveryToken.getUser();

        user.setPassword(request.getPassword());

        userRepository.save(user);

        tokenRepository.delete(recoveryToken);

        return GenericResponse.builder().code(HttpStatus.OK.value()).status(HttpStatus.OK.toString()).data("Password has been updated!").build();
    }

    private User validateActivate(String token) {
        Token activationToken = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });

        if(!activationToken.isTokenActivation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_wrong_type"));
        }

        User user = activationToken.getUser();

        if(activationToken.isTokenExpired()) {
            String secretUUID = UUID.randomUUID().toString();

            tokenRepository.save(Token.builder()
                    .tokenType(TokenType.ACTIVATION)
                    .user(user)
                    .hashedToken(secretUUID)
                    .createdAt(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(activationExpiration))
                    .build());

            emailService.sendEmail(user.getEmail(), "Activation",  secretUUID);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_expired_activation"));
        }

        tokenRepository.delete(activationToken);

        return user;
    }

    private void activateUser(User user) {
        user.setEnabled(true);

        userRepository.save(user);
    }

    public GenericResponse activate(String token) {
        User user = validateActivate(token);

        activateUser(user);

        return GenericResponse.builder().code(HttpStatus.OK.value()).status(HttpStatus.OK.toString()).data(Translator.toLocale("account_activated")).build();
    }
}
