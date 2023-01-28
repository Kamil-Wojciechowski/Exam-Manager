package com.wojcka.exammanager.controllers.auth;


import com.wojcka.exammanager.controllers.auth.requests.AuthenticationRequest;
import com.wojcka.exammanager.controllers.auth.responses.AuthenticationResponse;
import com.wojcka.exammanager.controllers.responses.GenericResponse;
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
    public ResponseEntity<GenericResponse> startRecovery(@RequestParam("email") String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recovery(email));
    }
}
