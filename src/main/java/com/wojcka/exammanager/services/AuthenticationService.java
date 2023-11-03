package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.ObjectToJson;
import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.PasswordRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.models.Token;
import com.wojcka.exammanager.models.TokenType;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.services.internal.JwtEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    @Value("${spring.jpa.auth.expiration.recovery}")
    private Long recoveryExpiration;

    @Value("${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;

    @Value("${spring.jpa.auth.expiration.refresh}")
    private Long refreshExpiration;

    @Value("spring.secret")
    private String secretKey;


    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final TokenRepository tokenRepository;
    private StrongTextEncryptor textEncryptor;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;


    private User authenticateUser(String email, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email.toLowerCase(),
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

        log.info(ObjectToJson.toJson("Authentication request appeard for user: " + request.getEmail()));

        User user = authenticateUser(request.getEmail().toLowerCase(), request.getPassword());

        Token refreshToken = buildRefreshToken(user);

        log.info(ObjectToJson.toJson("Token has been created!"));

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

    private void initializeEncrypt() {
        textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword(secretKey);
    }

    public GenericResponse recovery(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("email_not_found"));
        });

        initializeEncrypt();

        String secretUUID = UUID.randomUUID().toString();

        tokenRepository.deleteByTokenTypeAndUser(TokenType.RECOVERY, user);

        tokenRepository.save(Token.builder()
                .tokenType(TokenType.RECOVERY)
                .user(user)
                .hashedToken(secretUUID)
                .createdAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(recoveryExpiration))
                .build());

        emailService.sendEmail(user.getEmail(), "Recovery",  URLEncoder.encode(textEncryptor.encrypt(secretUUID), Charset.defaultCharset()));

        return GenericResponse.created(Translator.toLocale("email_has_been_send"));
    }

    private Token validateRecovery(String token) {
        Token recoveryToken = tokenRepository.findByHashedToken(textEncryptor.decrypt(token)).orElseThrow(() -> {
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

    public void validateRecoveryToken(String token) {
        initializeEncrypt();

        validateRecovery(token);
    }
    private void validatePassword(PasswordRequest request) {
        if(!request.isPasswordEqual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("password_not_equal"));
        }
    }
    public GenericResponse recovery(String token, PasswordRequest request) {
        Token recoveryToken = validateRecovery(token);

        User user = recoveryToken.getUser();

        validatePassword(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        tokenRepository.delete(recoveryToken);

        return GenericResponse.ok("Password has been updated!");
    }

    private Token validateActivate(String token) {
        Token activationToken = tokenRepository.findByHashedToken(textEncryptor.decrypt(token)).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("token_not_found"));
        });


        if (!activationToken.isTokenActivation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_wrong_type"));
        }

        if(activationToken.isTokenExpired()) {
            String secretUUID = UUID.randomUUID().toString();

            tokenRepository.save(Token.builder()
                    .tokenType(TokenType.ACTIVATION)
                    .user(activationToken.getUser())
                    .hashedToken(secretUUID)
                    .createdAt(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(activationExpiration))
                    .build());

            emailService.sendEmail(activationToken.getUser().getEmail(), "Activation",  URLEncoder.encode(textEncryptor.encrypt(secretUUID), Charset.defaultCharset()));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("token_expired_activation"));
        }

        return activationToken;
    }

    public void validateActivationToken(String token) {
        initializeEncrypt();

        validateActivate(token);
    }

    private void activateUser(User user) {
        user.setEnabled(true);

        userRepository.save(user);
    }

    public GenericResponse activate(String token, PasswordRequest passwordRequest) {
        Token tokenObject = validateActivate(token);

        validatePassword(passwordRequest);

        initializeEncrypt();

        tokenObject.getUser().setPassword(passwordEncoder.encode(passwordRequest.getPassword()));

        activateUser(tokenObject.getUser());

        tokenRepository.delete(tokenObject);

        return GenericResponse.ok(Translator.toLocale("account_activated"));
    }
}
