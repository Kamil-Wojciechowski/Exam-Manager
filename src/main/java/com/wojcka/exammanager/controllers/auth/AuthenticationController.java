package com.wojcka.exammanager.controllers.auth;


import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.RecoveryRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = {"application/json;charset=UTF-8"})
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("refresh/{token}")
    public ResponseEntity<AuthenticationResponse> refreshToken(@PathVariable("token") String token) {
        return ResponseEntity.ok(service.refresh(token));
    }

    @PostMapping("recovery/{email}")
    public ResponseEntity<GenericResponse> startRecovery(@PathVariable("email") String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recovery(email));
    }

    @PostMapping("recovered/{token}")
    public ResponseEntity<GenericResponse> completeRecovery(@PathVariable("token") String token, @RequestBody RecoveryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(service.recovery(token, request));
    }

    @PostMapping("activation/{token}")
    public ResponseEntity<GenericResponse> completeActivation(@PathVariable("token") String token) {
        return ResponseEntity.status(HttpStatus.OK).body(service.activate(token));
    }
}
