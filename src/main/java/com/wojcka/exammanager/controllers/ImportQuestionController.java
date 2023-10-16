package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.questions.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("api/v1/questions/metadata/{metadataId}/questions/imports")
public class ImportQuestionController {

    @Autowired
    private QuestionService questionService;

    @PostMapping
    public ResponseEntity<GenericResponse> importQuestionsAndAnswersCSV(@PathVariable("metadataId") Integer metadataId, @RequestParam("file") MultipartFile file) {
        questionService.importCSV(metadataId, file);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
