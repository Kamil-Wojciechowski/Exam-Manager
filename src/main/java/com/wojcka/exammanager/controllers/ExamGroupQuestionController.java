package com.wojcka.exammanager.controllers;


import com.wojcka.exammanager.schemas.requests.ExamGroupQuestionRequest;
import com.wojcka.exammanager.services.ExamGroupQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/studies/{studiesId}/exams/{examId}/groups/{examGroupId}/questions")
public class ExamGroupQuestionController {

    @Autowired
    private ExamGroupQuestionService examGroupQuestionService;

    @PatchMapping("/{questionId}")
    public ResponseEntity<Void> updateQuestion(@PathVariable("studiesId") Integer studiesId, @PathVariable("examId") Integer examId, @PathVariable("examGroupId") Integer examGroupId, @PathVariable("questionId") Long questionId, @RequestBody ExamGroupQuestionRequest examGroupQuestionRequest) {
        examGroupQuestionService.updateQuestion(studiesId, examId, examGroupId, questionId, examGroupQuestionRequest);

        return ResponseEntity.noContent().build();
    }

}
