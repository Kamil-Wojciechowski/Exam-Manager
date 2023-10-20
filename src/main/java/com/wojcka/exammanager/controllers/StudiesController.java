package com.wojcka.exammanager.controllers;

import com.google.api.services.classroom.model.Course;
import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.StudiesService;
import com.wojcka.exammanager.services.internal.GoogleService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/studies")
public class StudiesController {
    @Autowired
    private StudiesService studiesService;

    @Autowired
    private GoogleService googleService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getStudies(@RequestParam(defaultValue = "0", required = false) @Min(0) Integer page, @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(studiesService.get(page, size));
    }

    @PostMapping
    public ResponseEntity<GenericResponse> createStudies(@RequestBody Studies request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studiesService.post(request));
    }

    @GetMapping("{id}")
    public ResponseEntity<GenericResponse> getStudiesById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(studiesService.getById(id));
    }

    @PatchMapping("{id}")
    public ResponseEntity<Void> updateStudiesById(@PathVariable("id") Integer id, @RequestBody Studies request) {
        studiesService.update(id, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteStudiesById(@PathVariable("id") Integer id) {
        studiesService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classrooms")
    public ResponseEntity<List<Course>> getClassrooms() throws IOException {
        return ResponseEntity.ok(googleService.getCourses());
    }



}
