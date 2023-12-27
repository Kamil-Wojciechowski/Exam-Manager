package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.GroupService;
import com.wojcka.exammanager.services.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/users/groups")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @GetMapping
    private ResponseEntity<GenericResponse> groups() {
        return ResponseEntity.ok(groupService.getGroups());
    }
}
