package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.components.ObjectToJson;
import com.wojcka.exammanager.models.QuestionMetadata;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.questions.QuestionMetadataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("api/v1/question/metadata")
public class QuestionMetadataController {
    @Autowired
    private QuestionMetadataService questionMetadataService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getQuestionMetadata(@RequestParam(defaultValue = "0", required = false) @Min(0) Integer page,
                                                                       @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size ) {
        return ResponseEntity.ok(questionMetadataService.get(page, size));
    }

    @PostMapping
    public ResponseEntity<GenericResponse> createQuestionMetadata(@Valid @RequestBody QuestionMetadata request) {
        log.info(ObjectToJson.toJson(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(questionMetadataService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> getQuestiomMetadataById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(questionMetadataService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> createQuestionMetadata(@PathVariable("id") @Min(1) Long id, @Valid @RequestBody QuestionMetadata request) {
        request.setId(id);

        log.info(ObjectToJson.toJson(request));

        questionMetadataService.update(request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionMetadata(@PathVariable("id") Long id) {
        questionMetadataService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

}
