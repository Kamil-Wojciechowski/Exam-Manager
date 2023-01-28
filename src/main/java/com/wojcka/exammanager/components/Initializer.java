package com.wojcka.exammanager.components;

import com.wojcka.exammanager.models.user.User;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Initializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

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

    @Override
    public void run(String...args) throws Exception {

        if(userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(
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
        }



    }



}
