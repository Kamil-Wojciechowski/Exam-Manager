package com.wojcka.exammanager.services.auth;

import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.RecoveryRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.components.language.PolishLanguage;
import com.wojcka.exammanager.models.Token;
import com.wojcka.exammanager.models.TokenType;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${spring.jpa.auth.expiration.recovery}")
    private Long recoveryExpiration;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final TokenRepository tokenRepository;

    private final StrongTextEncryptor textEncryptor = new StrongTextEncryptor();

    @Autowired
    private EmailService emailService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PolishLanguage.email_already_used.label);
        });
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private String encryptString(String key, String token) {
        textEncryptor.setPassword(key);
        return textEncryptor.encrypt(token);
    }

    public GenericResponse recovery(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, PolishLanguage.email_not_found.label);
        });

        String secretUUID = UUID.randomUUID().toString();

        tokenRepository.save(Token.builder()
                .tokenType(TokenType.RECOVERY)
                .user(user)
                .hashedToken(secretUUID)
                .createdAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(recoveryExpiration))
                .build());

        emailService.sendEmail(user.getEmail(), "Recovery",  secretUUID);

        return GenericResponse.builder().code(HttpStatus.CREATED.value()).status(HttpStatus.CREATED.toString()).data("Email has been send").build();
    }

    public GenericResponse recovery(String token, RecoveryRequest request) {

        Token tokenObj = tokenRepository.findByHashedToken(token).orElseThrow(() -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, PolishLanguage.token_not_found.label);
        });

        User user = tokenObj.getUser();

        user.setPassword(request.getPassword());

        userRepository.save(user);

        return GenericResponse.builder().code(HttpStatus.OK.value()).status(HttpStatus.OK.toString()).data("Password has been updated!").build();
    }
}
