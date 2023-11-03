package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.Question;
import com.wojcka.exammanager.models.QuestionMetadata;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.questions.QuestionService;
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
@RestController
@RequestMapping("api/v1/questions/metadata/{metadataId}/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getQuestions(@PathVariable("metadataId") Integer metadataId, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page,
                                                                @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size, @RequestParam(defaultValue = "false", required = false) Boolean archived) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.get(metadataId, page, size, archived));
    }

    @PostMapping
    public ResponseEntity<GenericResponse> createQuestion(@PathVariable("metadataId") Integer metadataId, @RequestBody @Valid Question request) {
        request.setQuestionMetadata(
                QuestionMetadata.builder()
                        .id((metadataId)).build()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> getQuestionById(@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(questionService.getById(metadataId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateQuestion(@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id, @RequestBody @Valid Question request) {
        questionService.upsert(metadataId, id, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion (@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id, @RequestParam(value = "delete", defaultValue = "false", required = false) Boolean permaDelete) {
        questionService.delete(metadataId, id, permaDelete);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/imports")
    public ResponseEntity<GenericResponse> importQuestionsAndAnswersCSV(@PathVariable("metadataId") Integer metadataId, @RequestParam("file") MultipartFile file) {
        questionService.importCSV(metadataId, file);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

