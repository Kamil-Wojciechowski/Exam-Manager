package com.wojcka.exammanager.controllers.auth;


import com.wojcka.exammanager.schemas.requests.AuthenticationRequest;
import com.wojcka.exammanager.schemas.requests.RecoveryRequest;
import com.wojcka.exammanager.schemas.responses.AuthenticationResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("recovery/{email}")
    public ResponseEntity<GenericResponse> startRecovery(@PathVariable("email") String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recovery(email));
    }

    @PostMapping("recovery/{token}")
    public ResponseEntity<GenericResponse> completeRecovery(@PathVariable("token") String token, @RequestBody RecoveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recovery(token, request));
    }
}
