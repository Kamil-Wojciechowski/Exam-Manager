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

        log.info("Generating URLs for google");

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

        log.info("Authorization URL for google is generated");


        return authorizationUrl;
    }

    private void validateUserGoogle() {
        User user = getUserFromAuth();

        log.info("Validate User from auth.");

        if(user.getGoogleAccessToken() == null) {
            log.error("Account is not connected to google!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("account_not_connected"));
        }

        if(user.isGoogleExpired()) {
            log.info("Token is no longer valid. Start refreshing.");
            refreshAccessToken();
            user = getUserFromAuth();
        }

        log.info("Setting google access token");
        setHeaderAuthorization(user.getGoogleAccessToken());
    }
    public GenericResponse getCourses() {
        validateUserGoogle();

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        log.info("Creation of request towards google for courses");
        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "?teacherId=me&courseStates=ACTIVE", HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Response is correct!");
            try {
                log.info("Returning courses details");
                return GenericResponse.ok(objectMapper.readValue(response.getBody(), Map.class).get("courses"));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            log.error("Error occurred while getting courses.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private HttpEntity<MultiValueMap<String, String>> buildRequest(String authorizationCode, boolean refresh) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");

        log.info("Building request for Authorization/Refresh");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        body.add("client_id", registration.getClientId());
        body.add("client_secret", registration.getClientSecret());

        if(refresh) {
            log.info("Refresh request is being built.");
            body.add("grant_type", "refresh_token");

            String refreshToken = getUserFromAuth().getGoogleRefreshToken();

            if(refreshToken == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("account_not_connected"));
            }

            body.add("refresh_token", refreshToken);
        } else {
            log.info("Creating authorization callback.");
            body.add("code", authorizationCode);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", registration.getRedirectUri());
        }

        return new HttpEntity<>(body, headers);
    }

    private String getUserIdFromGoogle(String accessToken) {
        setHeaderAuthorization(accessToken);
        log.info("Fetching user from google.");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(userUrl, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("User has been fetched correctly.");
            try {
                return (String) objectMapper.readValue(response.getBody(), Map.class).get("id");
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            log.error("Error occurred while fetching user.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    private void requestOAuth(String authorizationCode, Boolean refresh) {
        HttpEntity<MultiValueMap<String, String>> requestEntity = buildRequest(authorizationCode, refresh);
        log.info("Request OAuth");
        ResponseEntity<String> response;

        LocalDateTime expirationTime = LocalDateTime.now();
        try {
            response = restTemplate.exchange(tokenEndpointUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception ex) {
            deleteConnection();
            log.error(ex.getMessage());
            log.error("Fetching closed with an error. Connection has been deleted! Please create a new one.");

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Request OAuth fetched correctly.");

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
            log.info("Error occurred while authorization/refreshing.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
        }
    }

    private void saveUserAccess(String accessToken, String refreshToken, String userId, LocalDateTime expirationDate) {
        log.info("Saving user details about google.");
        User user = getUserFromAuth();

        user.setGoogleAccessToken(accessToken);
        user.setGoogleRefreshToken(refreshToken);
        user.setGoogleUserId(userId);
        user.setGoogleExpiration(expirationDate);

        userRepository.save(user);
    }

    private void saveUserRefresh(String accessToken, LocalDateTime expirationDate) {
        log.info("Saving user new token.");
        User user = getUserFromAuth();

        user.setGoogleAccessToken(accessToken);
        user.setGoogleExpiration(expirationDate);

        userRepository.save(user);
    }

    public void processOauthCallback(String authorizationCode) {
        log.info("Process of OAuth Callback starts");
        requestOAuth(authorizationCode, false);
        log.info("Process of OAuth Callback ends");
    }

    public void deleteConnection() {
        log.info("Deleting connection with google.");
        User user = getUserFromAuth();

        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleExpiration(null);
        user.setGoogleUserId(null);

        userRepository.save(user);
    }

    private void refreshAccessToken() {
        log.info("Process of refresh access token starts");
        requestOAuth(null, true);
        log.info("Process of refresh access token ends");
    }

    public List<User> getUsersByClassroom(String classroomId) {
        validateUserGoogle();
        log.info("Process of listing users in classroom starts");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/students", HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Users has been fetched.");
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

                log.info("Process of listing users in classroom ends");

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
        log.info("Process of creating announcement starts");

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Annoucment> requestEntity = new HttpEntity<>(annoucment, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/announcements", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Announcement has been created");
            try {
                LinkedHashMap<String, String> items = (LinkedHashMap) objectMapper.readValue(response.getBody(), Map.class);

                annoucment.setId(items.get("id"));

                log.info("Process of creating announcement ends");
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
        log.info("Process of creating course work starts");

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CourseWork> requestEntity = new HttpEntity<>(courseWork, headers);

        ResponseEntity<String> response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Process of creating course work has been created");

            try {
                LinkedHashMap<String, String> items = (LinkedHashMap) objectMapper.readValue(response.getBody(), Map.class);

                courseWork.setId(items.get("id"));

                log.info("Process of deleting course work ends");
                return courseWork;
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            log.error("Error occurred when creating an course work");
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public void deleteCourseWork(String classroomId, String courseWorkId) {
        validateUserGoogle();
        log.info("Process of deleting course work starts");

        HttpEntity<CourseWork> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<String>  response = restTemplate.exchange(classroomUrl + "/" + classroomId + "/courseWork/" + courseWorkId, HttpMethod.DELETE, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }

        log.info("Process of deleting course work ends");
    }

    public List<StudentSubmissions> listAssignments(String classroomId, String courseWorkId) {
        validateUserGoogle();
        log.info("Process of list assignments starts");

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

                log.info("Process of list assignments ends");

                return submissionsList;
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            }
        } else {
            log.error("Error occurred when listing assignments");

            throw new ResponseStatusException(response.getStatusCode(), response.getBody());
        }
    }

    public void publishResults(String classroomId, String courseWorkId, String submissionId, StudentSubmissions studentSubmissions) {
        validateUserGoogle();
        log.info("Process of publishing results to classroom starts.");

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

        log.info("Process of publishing results to classroom ends.");

    }
}
