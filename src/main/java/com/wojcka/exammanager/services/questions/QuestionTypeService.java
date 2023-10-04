package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.models.QuestionType;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import org.springframework.stereotype.Service;

@Service
public class QuestionTypeService {
    public GenericResponse getQuestionTypes() {
        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(QuestionType.values())
                .build();
    }
}
