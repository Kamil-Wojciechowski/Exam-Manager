package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.PasswordRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/v1/auth", produces = {"application/json;charset=UTF-8"})
public class AuthenticationController {

    @Autowired
    private AuthenticationService service;

    @PostMapping("login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request, @RequestHeader(value = "X-Forwarded-For", required = false, defaultValue = "0.0.0.0") String addressIp) {
        return ResponseEntity.ok(service.authenticate(request, addressIp));
    }

    @PostMapping("refresh/{token}")
    public ResponseEntity<AuthenticationResponse> refreshToken(@PathVariable("token") String token) {
        return ResponseEntity.ok(service.refresh(token));
    }


    @PostMapping("recovery/{email}")
    public ResponseEntity<GenericResponse> startRecovery(@PathVariable("email") String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recovery(email));
    }

    @GetMapping("recovery")
    public ResponseEntity<Void> checkRecoveryToken(@Param("token") String token) {
        service.validateRecoveryToken(token);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("recovered")
    public ResponseEntity<GenericResponse> completeRecovery(@Param("token") String token, @Valid @RequestBody PasswordRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(service.recovery(token, request));
    }

    @GetMapping("activation")
    public ResponseEntity<Void> checkActivationToken(@Param("token") String token) {
        service.validateActivationToken(token);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("activation")
    public ResponseEntity<GenericResponse> completeActivation(@Param("token") String token, @Valid @RequestBody PasswordRequest passwordRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(service.activate(token, passwordRequest));
    }
}
