package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.QuestionMetadataOwnership;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.questions.QMOwnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("api/v1/question/metadata")
public class QMOwnershipController {
    @Autowired
    private QMOwnerService qmOwnerService;

    @GetMapping("/{metadataId}/ownership")
    public ResponseEntity<GenericResponsePageable> getQMOwnership(@PathVariable("metadataId") Integer metadataId, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page, @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size ) {
        return ResponseEntity.ok(qmOwnerService.get(metadataId, page, size));
    }

    @PostMapping("/{metadataId}/ownership")
    public ResponseEntity<GenericResponse> createQMOwnership(@PathVariable("metadataId") Integer metadataId, @Valid @RequestBody QuestionMetadataOwnership request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qmOwnerService.post(metadataId, request));
    }

    @GetMapping("/{metadataId}/ownership/{id}")
    public ResponseEntity<GenericResponse> getByIdQMOwnership(@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id) {
        return ResponseEntity.ok(qmOwnerService.getById(metadataId, id));
    }

    @PatchMapping("/{metadataId}/ownership/{id}")
    public ResponseEntity<Void> updateQMOwnership(@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id, @Valid @RequestBody QuestionMetadataOwnership request) {
        qmOwnerService.patch(metadataId, id, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{metadataId}/ownership/{id}")
    public ResponseEntity<Void> deleteQMOwnership(@PathVariable("metadataId") Integer metadataId, @PathVariable("id") Integer id) {
        qmOwnerService.delete(metadataId, id);

        return ResponseEntity.noContent().build();
    }

}
