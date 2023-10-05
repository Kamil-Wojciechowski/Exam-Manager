package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.QuestionAnswer;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.questions.AnswerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/questions/metadata/{metadataId}/questions/{questionId}/answers")
public class AnswerController {
    @Autowired
    private AnswerService answerService;

    @PostMapping
    public ResponseEntity<GenericResponse> createAnswer(@PathVariable("metadataId") Integer metadataId, @PathVariable("questionId") Integer questionId, @RequestBody @Valid QuestionAnswer request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(answerService.post(metadataId, questionId, request));
    }

}
