package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.StudiesUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController("api/v1/studies/{studiesId}/users")
public class StudiesUserController {

    @Autowired
    private StudiesUserService studiesUserService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getUsers(@PathVariable("studiesId") Integer studiesId,
                                                            @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page,
                                                            @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(studiesUserService.get(studiesId, page, size));
    }

    @PostMapping
    public ResponseEntity<GenericResponse> createUserStudies (@PathVariable("studiesId") Integer studiesId, @RequestBody @Valid StudiesUser studiesUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studiesUserService.post(studiesId, studiesUser));
    }

    @GetMapping("/{studiesUserId}")
    public ResponseEntity<GenericResponse> getUserStudies (@PathVariable("studiesId") Integer studiesId, @PathVariable Integer studiesUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studiesUserService.get(studiesId, studiesUserId));
    }

    @DeleteMapping(("/{studiesUserId}"))
    public ResponseEntity<Void> deleteUserStudies(@PathVariable("studiesId") Integer studiesId, @PathVariable Integer studiesUserId) {
        studiesUserService.delete(studiesId,studiesUserId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<GenericResponse> importUsersForStudies(@PathVariable("studiesId") Integer studiesId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studiesUserService.importUsers(studiesId, file));
    }
}
