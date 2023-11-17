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