package com.wojcka.exammanager.controllers;

import com.wojcka.exammanager.models.ExamGroup;
import com.wojcka.exammanager.models.ExamGroupQuestion;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.ExamGroupQuestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/studies/{studiesId}/exams/{examId}/groups/questions")
public class ExamParticipateController {

    @Autowired
    private ExamGroupQuestionService examGroupQuestionService;


    @GetMapping("/participate")
    public ResponseEntity<GenericResponsePageable> getQuestionsParticipate(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @RequestParam(defaultValue = "0", required = false) @Min(0) Integer page,
                                                                           @RequestParam(defaultValue = "1", required = false) @Min(1) @Max(100) Integer size) {

        return ResponseEntity.ok(examGroupQuestionService.getQuestions(studiesId, examId, page, size));
    }

    @PostMapping("/participate")
    public ResponseEntity<GenericResponse> postAnswers(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @RequestBody List<ExamGroupQuestion> examGroupQuestions) {
        ExamGroup examGroup = examGroupQuestionService.markAsSent(studiesId, examId, examGroupQuestions);

        examGroupQuestionService.processQuestions(examGroup, examGroupQuestions);

        return ResponseEntity.status(HttpStatus.CREATED).body(GenericResponse.created(examGroup));
    }
}
