package com.wojcka.exammanager.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wojcka.exammanager.services.internal.GoogleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    @Autowired
    private GoogleService googleService;

    @PostMapping("/connectLocalAccount")
    public ResponseEntity<Void> connectLocalAccount(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        headers.setLocation(URI.create(googleService.createAuthorizationURL()));

        return ResponseEntity.status(HttpStatus.SEE_OTHER).headers(headers).build();
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> oauthCallback(@RequestParam("code") String authorizationCode) throws JsonProcessingException {
        googleService.processOauthCallback(authorizationCode);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disconnectLocalAccount")
    public ResponseEntity<Void> disconnectLocalAccount() {
       googleService.deleteConnection();

        return ResponseEntity.noContent().build();
    }
}