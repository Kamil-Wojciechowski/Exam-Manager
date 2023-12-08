package com.wojcka.exammanager.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wojcka.exammanager.services.internal.GoogleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    @Autowired
    private GoogleService googleService;

    @GetMapping("/google")
    public ResponseEntity<Void> generateUrl() {
        String authUrl = googleService.generateUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Google-Url");
        headers.add("Google-Url", authUrl);

        return ResponseEntity.noContent().headers(headers).build();
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