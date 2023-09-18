package com.wojcka.exammanager.components;

import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.models.UserGroup;
import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.repositories.UserGroupRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Initializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${default.email}")
    private String email;

    @Value("${default.password}")
    private String password;

    @Value("${default.firstname}")
    private String firstname;

    @Value("${default.lastname}")
    private String lastname;

    @Value("${default.initialize}")
    private Boolean initialize;

    @Override
    public void run(String...args) throws Exception {
        if(initialize) {
            groupInitializer();
            adminInitializer();
        }
    }

    public void groupInitializer() throws ClassNotFoundException {
        if(groupRepository.findAll().isEmpty()) {
            List<Group> groups = new ArrayList<>();

            groups.add(Group.builder()
                    .key("ROLE_TEACHER")
                    .name("Nauczyciel")
                    .description("""
                            Uprawnienia umożliwiające użytkownikowi zarządzanie aplikacją w wszystkich serwisach.""")
                    .build());

            groups.add(Group.builder()
                    .key("ROLE_STUDENT")
                    .name("Student")
                    .description("""
                            Uprawnienia umożliwiające korzystanie użytkownikowi z konkretnych funkcjonalności""")
                    .build());

            groupRepository.saveAll(groups);
        }
    }

    private void adminInitializer() throws ClassNotFoundException {

        User user;

        try {
            user = userRepository.findByEmail(email).orElseThrow(NullPointerException::new);
        } catch (NullPointerException ex) {
            user = userRepository.save(
                    User.builder()
                            .email(email)
                            .firstname(firstname)
                            .lastname(lastname)
                            .password(passwordEncoder.encode(password))
                            .enabled(true)
                            .locked(false)
                            .build()
            );
        }

            if(!userGroupRepository.existsByUser(user)) {
                Group group = groupRepository.findByKey("ROLE_TEACHER")
                        .orElseThrow(NullPointerException::new);

                userGroupRepository.save(
                        UserGroup
                                .builder()
                                .user(user)
                                .group(group)
                                .build()
                );
        }
    }

}
