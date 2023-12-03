package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    private ResponseEntity<GenericResponsePageable> user(@RequestParam(defaultValue = "TEACHER", required = false) String role,
                                                         @RequestParam(required = false) String firstname,
                                                         @RequestParam(required = false) String lastname,
                                                         @RequestParam(required = false) String email,
                                                         @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page, @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(userService.getUsers(role, firstname, lastname, email, page, size));
    }

    @GetMapping("/me")
    private ResponseEntity<GenericResponse> getUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
}
