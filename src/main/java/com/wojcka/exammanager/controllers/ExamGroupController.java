package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.ExamGroupService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/studies/{studiesId}/exams/{examId}/groups")
public class ExamGroupController {
    @Autowired
    private ExamGroupService examGroupService;

    @GetMapping
    public ResponseEntity<GenericResponsePageable> getExamGroup(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page,
                                                                @RequestParam(defaultValue = "50", required = false) @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(examGroupService.get(studiesId, examId, page, size));
    }


}
