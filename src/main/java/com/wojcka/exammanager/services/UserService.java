package com.wojcka.exammanager.services;


import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.components.email.EmailBodyBuilder;
import com.wojcka.exammanager.components.email.EmailBodyType;
import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserGroupRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    private StrongTextEncryptor textEncryptor;

    @Value(value = "${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;

    @Value("${spring.mail.frontendUrl.activation")
    private String activationUrl;

    @Value("${spring.secret}")
    private String secretKey;

    public GenericResponsePageable getUsers(String role, String firstname, String lastname, String email, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page,size);

        role = "ROLE_" + role.toUpperCase();

        if(firstname != null) {
            firstname = '%' + firstname.toLowerCase() + '%';
        }
        if(lastname != null) {
            lastname = '%' + lastname.toLowerCase() + '%';
        }
        if(email != null) {
            email = '%' + email.toLowerCase() + '%';
        }

        Page result = userRepository.getByRoleAndParams(role, firstname, lastname, email, pageable);

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(result.get().toList())
                .page(page)
                .size(size)
                .hasNext(result.hasNext())
                .pages(result.getTotalPages())
                .total(result.getTotalElements())
                .build();
    }


    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT')" )
    public GenericResponse getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return GenericResponse.ok(user);
    }

    private void setTextEncryptor() {
        textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword(secretKey);
    }

    private Group getGroup(Boolean isTeacher) {
        if(isTeacher) {
            return groupRepository.findByKey("ROLE_TEACHER").orElseThrow(() -> {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            });
        } else {
            return groupRepository.findByKey("ROLE_STUDENT").orElseThrow(() -> {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            });
        }
    }

    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public GenericResponse addUser(User request, Boolean teacher) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(User.builder()
                        .email(request.getEmail().toLowerCase())
                        .firstname(request.getFirstname())
                        .lastname(request.getLastname())
                        .expired(false)
                        .locked(false)
                        .enabled(false)
                .build());

        if(user.getId() != null ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("email_already_exists"));
        }

        setTextEncryptor();

        user = userRepository.save(user);

        Group group = getGroup(teacher);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .build();

        userGroup = userGroupRepository.save(userGroup);

        String secretUUID = UUID.randomUUID().toString();

        tokenRepository.save(Token.builder()
                .tokenType(TokenType.ACTIVATION)
                .user(user)
                .hashedToken(secretUUID)
                .createdAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(activationExpiration))
                .build());

        HashMap<String, String> items = new HashMap<>();
        items.put("{url}", activationUrl.replace("{token}", URLEncoder.encode(textEncryptor.encrypt(secretUUID), Charset.defaultCharset())));

        String emailBody = EmailBodyBuilder.buildEmail(EmailBodyType.ACTIVATION, items);

        emailService.sendEmail(user.getEmail(), "Activation", emailBody);

        List<UserGroup> userGroups = new ArrayList<>();
        userGroups.add(userGroup);

        user.setUserRoleGroups(userGroups);

        return GenericResponse.created(user);
    }

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private User validateUser(UUID uuid) {
        User user = userRepository.findById(uuid).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("user_not_found"));
        });

        if(user.equals(getUserFromAuth())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("user_not_editable"));
        }

        return user;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public void changeRole(UUID uuid) {
        User user = validateUser(uuid);

        Boolean isStudent = user.getUserRoleGroups().stream().filter(item -> item.getGroup().getKey().contains("TEACHER")).toList().isEmpty();

        UserGroup userGroup = user.getUserRoleGroups().get(0);

        userGroup.setGroup(getGroup(isStudent));

        userGroupRepository.save(userGroup);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public void deactivateUser(UUID uuid) {
        User user = validateUser(uuid);

        user.setLocked(!user.isLocked());

        userRepository.save(user);
    }

}
