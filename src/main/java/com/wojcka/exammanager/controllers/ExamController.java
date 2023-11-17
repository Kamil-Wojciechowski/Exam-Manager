package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.annocument.Annoucment;
import com.wojcka.exammanager.models.Exam;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.ExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/studies/{studiesId}/exams")
public class ExamController {
    @Autowired
    private ExamService examService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getExams(@PathVariable("studiesId") Integer studiesId, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page, @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(examService.getStudiesByAuthenticatedUser(studiesId, size, page));
    }

    @PostMapping
    public ResponseEntity<GenericResponse> createExam(@PathVariable("studiesId") Integer studiesId, @Valid @RequestBody Exam request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.post(studiesId, request));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<GenericResponse> getExam(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId) {
        return ResponseEntity.ok(examService.get(studiesId, examId));
    }

    @PostMapping("/{examId}/annoucments")
    public ResponseEntity<GenericResponse> postExamAnnocument(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @RequestBody Annoucment annoucment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.postAnnoucmnet(studiesId, examId, annoucment));
    }

    @PostMapping("/{examId}/submission")
    public ResponseEntity<Void> postExamResults(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId) {
        examService.postResults(studiesId, examId);

        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/{examId}")
    public ResponseEntity<Void> patchExam(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @Valid @RequestBody Exam request) {
        examService.patch(studiesId, examId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId) {
        examService.delete(studiesId, examId);

        return ResponseEntity.noContent().build();
    }
}
