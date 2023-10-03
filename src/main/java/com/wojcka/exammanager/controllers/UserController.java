package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.components.ObjectToJson;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    private ResponseEntity<GenericResponsePageable> user(@RequestParam(defaultValue = "TEACHER", required = false) String role, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page, @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(userService.getUsers(role, page, size));
    }


}
