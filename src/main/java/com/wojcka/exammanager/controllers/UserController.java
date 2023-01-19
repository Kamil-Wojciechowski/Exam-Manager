package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.user.User;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    private User user() {
        return userRepository.findById(UUID.fromString("15cc2df4-c84a-4cb7-9793-7176294d91c4")).orElseThrow(
                () -> new RestClientException(null, null));
    }
}
