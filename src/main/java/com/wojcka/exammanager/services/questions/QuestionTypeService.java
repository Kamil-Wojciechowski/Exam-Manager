package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.models.QuestionType;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuestionTypeService {
    public GenericResponse getQuestionTypes() {
        log.info("Getting question types");
        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(QuestionType.values())
                .build();
    }
}
