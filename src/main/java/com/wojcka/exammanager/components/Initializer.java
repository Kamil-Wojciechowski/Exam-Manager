package com.wojcka.exammanager.components;

import com.wojcka.exammanager.models.token.user.User;
import com.wojcka.exammanager.models.token.user.UserGroup;
import com.wojcka.exammanager.models.token.user.group.Group;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.repositories.UserGroupRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        adminInitializer();
    }

    private void adminInitializer() throws ClassNotFoundException {
        if(initialize && userRepository.findByEmail(email).isEmpty()) {
            User user = userRepository.save(
                    User.builder()
                            .email(email)
                            .firstname(firstname)
                            .lastname(lastname)
                            .password(passwordEncoder.encode(password))
                            .enabled(true)
                            .locked(false)
                            .locked(false)
                            .build()
            );
            if(!groupRepository.findByName("Admin").isEmpty()) {
                Group group = groupRepository.findByName("Admin")
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

}
