package com.wojcka.exammanager.services.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleService {

    private final String tokenEndpointUrl = "https://accounts.google.com/o/oauth2/token";
    private final String classroomUrl = "https://classroom.googleapis.com/v1/courses";
    private final String userUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
    private HttpHeaders headers = new HttpHeaders();
    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    private void setHeaderAuthorization(String accessToken) {
        headers.setBearerAuth(accessToken);
    }

    private void validateUserGoogle() {
        User user = getUserFromAuth();

        if(user.getGoogleAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("account_not_connected"));
        }

        if(user.isGoogleExpired()) {
            refreshAccessToken();
            user = getUserFromAuth();
        }

        setHeaderAuthorization(user.getGoogleAccessToken());
    }
    public GenericResponse getCourses() {
        validateUserGoogle();

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "?teacherId=me&courseStates=ACTIVE", HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {

            try {
                return GenericResponse.ok(objectMapper.readValue(response.getBody(), Map.class).get("courses"));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    public String createAuthorizationURL() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri()) // Replace with your actual redirect URI
                .scope(registration.getScopes().toArray(new String[0]))
                .build();

        return authorizationRequest.getAuthorizationRequestUri();
    }

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private HttpEntity<MultiValueMap<String, String>> buildRequest(String authorizationCode, boolean refresh) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");
        // Create a request entity with the authorization code and other parameters
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        body.add("client_id", registration.getClientId());
        body.add("client_secret", registration.getClientSecret());


        if(refresh) {
            body.add("grant_type", "refresh_token");

            String refreshToken = getUserFromAuth().getGoogleRefreshToken();

            if(refreshToken == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("account_not_connected"));
            }

            body.add("refresh_token", refreshToken);
        } else {
            body.add("code", authorizationCode);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", registration.getRedirectUri());
        }

        return new HttpEntity<>(body, headers);
    }

    private String getUserIdFromGoogle(String accessToken) {
        setHeaderAuthorization(accessToken);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(userUrl, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                return (String) objectMapper.readValue(response.getBody(), Map.class).get("id");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    private void requestOAuth(String authorizationCode, Boolean refresh) {
        HttpEntity<MultiValueMap<String, String>> requestEntity = buildRequest(authorizationCode, refresh);

        LocalDateTime expirationTime = LocalDateTime.now();
        ResponseEntity<String> response = restTemplate.exchange(tokenEndpointUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            // You can extract the access token from the response body

            Map<String, String> jsonMap = null;
            try {
                jsonMap = objectMapper.readValue(response.getBody(), Map.class);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }

            Object expiration = jsonMap.get("expires_in");

            if (expiration instanceof Integer) {
                Integer expirationInt = (Integer) expiration;

                expirationTime = expirationTime.plusSeconds(expirationInt);
            }

            String userId = getUserIdFromGoogle(jsonMap.get("access_token"));

            if(refresh) {
                saveUserRefresh(jsonMap.get("access_token"), expirationTime);
            } else {
                saveUserAccess(jsonMap.get("access_token"), jsonMap.get("refresh_token"), userId, expirationTime);
            }

        } else {
            // Handle errors or invalid responses
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    private void saveUserAccess(String accessToken, String refreshToken, String userId, LocalDateTime expirationDate) {
        User user = getUserFromAuth();

        user.setGoogleAccessToken(accessToken);
        user.setGoogleRefreshToken(refreshToken);
        user.setGoogleUserId(userId);
        user.setGoogleExpiration(expirationDate);

        userRepository.save(user);
    }

    private void saveUserRefresh(String accessToken, LocalDateTime expirationDate) {
        User user = getUserFromAuth();

        user.setGoogleAccessToken(accessToken);
        user.setGoogleExpiration(expirationDate);

        userRepository.save(user);
    }

    public void processOauthCallback(String authorizationCode) {
        requestOAuth(authorizationCode, false);
    }

    public void deleteConnection() {
        User user = getUserFromAuth();

        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleExpiration(null);
        user.setGoogleUserId(null);

        userRepository.save(user);
    }

    private void refreshAccessToken() {
        requestOAuth(null, true);
    }
}
