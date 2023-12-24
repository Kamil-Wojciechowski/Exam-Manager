package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.questions.CorrectTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/public/questions/answers/types")
public class CorrectTypeController {
    @Autowired
    private CorrectTypeService correctTypeService;
    @GetMapping
    public ResponseEntity<GenericResponse> getQuestionTypes() {
        return ResponseEntity.ok(correctTypeService.getCorrectTypes());
    }
}
