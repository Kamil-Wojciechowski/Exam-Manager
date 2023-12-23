package com.wojcka.exammanager.services.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.StudentSubmissions;
import com.wojcka.exammanager.models.annocument.Annoucment;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.models.cousework.CourseWork;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class GoogleService {

    private final String tokenEndpointUrl = "https://accounts.google.com/o/oauth2/token";
    private final String classroomUrl = "https://classroom.googleapis.com/v1/courses";
    private final String userUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
    private HttpHeaders headers = new HttpHeaders();
    private RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private UserRepository userRepository;
    private void setHeaderAuthorization(String accessToken) {
        headers.setBearerAuth(accessToken);
    }

    public String generateUrl() {
        String googleClientRegistrationId = "google";

        ClientRegistration clientRegistration = clientRegistrationRepository
                .findByRegistrationId(googleClientRegistrationId);

        String authorizationUri = clientRegistration.getProviderDetails()
                .getAuthorizationUri();

        String redirectUri = clientRegistration.getRedirectUri();

        String authorizationUrl = String.format("%s&response_type=code&client_id=%s&scope=%s&state=%s&redirect_uri=%s",
                authorizationUri,
                clientRegistration.getClientId(),
                String.join(" ", clientRegistration.getScopes()),
                "k9qeIBI6hc5wMphgcrJgnXSkVKxSWuCmBjx6BCWHwso%3D",
                redirectUri);

        return authorizationUrl;
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

        ResponseEntity<String> response;

        LocalDateTime expirationTime = LocalDateTime.now();
        try {
            response = restTemplate.exchange(tokenEndpointUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception ex) {
            deleteConnection();

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }

        if (response.getStatusCode() == HttpStatus.OK) {
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

    public List<User> getUsersByClassroom(String classroomId) {
        validateUserGoogle();

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/students", HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ArrayList<LinkedHashMap> items = (ArrayList) objectMapper.readValue(response.getBody(), Map.class).get("students");

                ArrayList<User> listOfUsersEmails = new ArrayList<>();

                if(items != null) {
                    items.forEach(item -> {
                        LinkedHashMap profile = (LinkedHashMap) item.get("profile");
                        LinkedHashMap profileDetails = (LinkedHashMap) profile.get("name");

                        User googleUser = User.builder()
                                .email(profile.get("emailAddress").toString().toLowerCase())
                                .firstname((String) profileDetails.get("givenName"))
                                .lastname((String) profileDetails.get("familyName"))
                                .googleUserId((String) item.get("userId"))
                                .enabled(false)
                                .locked(false)
                                .expired(false)
                                .build();


                        listOfUsersEmails.add(googleUser);
                    });
                }

                return listOfUsersEmails;
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public Annoucment createAnnocument(String classroomId, Annoucment annoucment) {
        validateUserGoogle();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Annoucment> requestEntity = new HttpEntity<>(annoucment, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/announcements", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                LinkedHashMap<String, String> items = (LinkedHashMap) objectMapper.readValue(response.getBody(), Map.class);

                annoucment.setId(items.get("id"));

                return annoucment;
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public CourseWork createCourseWork(String classroomId, CourseWork courseWork) {
        validateUserGoogle();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CourseWork> requestEntity = new HttpEntity<>(courseWork, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                LinkedHashMap<String, String> items = (LinkedHashMap) objectMapper.readValue(response.getBody(), Map.class);

                courseWork.setId(items.get("id"));

                return courseWork;
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public void deleteCourseWork(String classroomId, String courseWorkId) {
        validateUserGoogle();
        HttpEntity<CourseWork> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String>  response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork/" + courseWorkId, HttpMethod.DELETE, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public List<StudentSubmissions> listAssignments(String classroomId, String courseWorkId) {
        validateUserGoogle();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CourseWork> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response  = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork/" +courseWorkId + "/studentSubmissions", HttpMethod.GET, requestEntity, String.class);


        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                List<LinkedHashMap<String, String>> items = (List<LinkedHashMap<String, String>>) objectMapper.readValue(response.getBody(), Map.class).get("studentSubmissions");

                List<StudentSubmissions> submissionsList = new ArrayList<>();

                items.forEach((submissionItem) -> {
                    submissionsList.add(
                            StudentSubmissions.builder()
                                    .id(submissionItem.get("id"))
                                    .state(submissionItem.get("state"))
                                    .userId(submissionItem.get("userId"))
                                    .build()
                    );
                });

                return submissionsList;
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }

        //TODO: LIST https://developers.google.com/classroom/reference/rest/v1/courses.courseWork.studentSubmissions/list?hl=pl&apix_params=%7B"courseId"%3A"590591522251"%2C"courseWorkId"%3A"637937802917"%7D
        //TODO: TO FETCH USERS INFO AND THEN CONNECT submissionId in EXAM GROUP in previous class
    }

    public void publishResults(String classroomId, String courseWorkId, String submissionId, StudentSubmissions studentSubmissions) {
        validateUserGoogle();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StudentSubmissions> requestEntity = new HttpEntity<>(studentSubmissions, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork/" +courseWorkId + "/studentSubmissions/" + submissionId + "?updateMask=draftGrade", HttpMethod.PATCH, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }

        response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork/" +courseWorkId + "/studentSubmissions/" + submissionId + "?updateMask=assignedGrade", HttpMethod.PATCH, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
        //https://developers.google.com/classroom/reference/rest/v1/courses.courseWork.studentSubmissions/patch?hl=pl&apix_params=%7B"courseId"%3A"590591522251"%2C"courseWorkId"%3A"637937802917"%2C"id"%3A"Cg4I4rrx2soEEKW9ucDIEg"%2C"updateMask"%3A"assignedGrade"%2C"resource"%3A%7B"state"%3A"RETURNED"%2C"assignedGrade"%3A20%7D%7D
        //TODO: Post results to google classroom courseWork
    }
}
