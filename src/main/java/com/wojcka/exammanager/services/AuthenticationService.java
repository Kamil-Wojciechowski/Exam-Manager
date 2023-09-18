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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${spring.jpa.auth.expiration.recovery}")
    private Long recoveryExpiration;

    @Value("${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;


    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final TokenRepository tokenRepository;

    private final StrongTextEncryptor textEncryptor = new StrongTextEncryptor();

    @Autowired
    private EmailService emailService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = (UserDetails) authentication.getPrincipal();
        JwtEncoder jwtServiceEncoder = new JwtEncoder(user);

        return AuthenticationResponse.builder()
                .token(jwtServiceEncoder.getToken())
                .issued(jwtServiceEncoder.getIssuedAt())
                .expires(jwtServiceEncoder.getExpiresAt())
                .build();
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

    public GenericResponse recovery(String token, RecoveryRequest request) {

        Token tokenObj = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });

        if(!tokenObj.isTokenRecover()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_wrong_type"));
        }

        if(tokenObj.isTokenExpired()) {
            tokenRepository.delete(tokenObj);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_expired_recovery"));
        }

        User user = tokenObj.getUser();

        user.setPassword(request.getPassword());

        userRepository.save(user);

        tokenRepository.delete(tokenObj);

        return GenericResponse.builder().code(HttpStatus.OK.value()).status(HttpStatus.OK.toString()).data("Password has been updated!").build();
    }

    public GenericResponse activate(String token) {

        Token tokenObj = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });

        if(!tokenObj.isTokenActivation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_wrong_type"));
        }

        User user = tokenObj.getUser();

        if(tokenObj.isTokenExpired()) {
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


        user.setEnabled(true);

        userRepository.save(user);

        tokenRepository.delete(tokenObj);

        return GenericResponse.builder().code(HttpStatus.OK.value()).status(HttpStatus.OK.toString()).data(Translator.toLocale("account_activated")).build();
    }
}
