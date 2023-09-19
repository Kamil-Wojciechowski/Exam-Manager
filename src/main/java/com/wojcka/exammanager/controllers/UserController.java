package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;


    @GetMapping
    private ResponseEntity<User> user() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(user);
    }
}
