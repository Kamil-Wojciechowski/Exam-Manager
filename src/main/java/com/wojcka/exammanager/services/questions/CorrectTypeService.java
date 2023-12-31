package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.models.CorrectType;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CorrectTypeService {
    public GenericResponse getCorrectTypes() {
        log.info("Getting correct types");

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(CorrectType.values())
                .build();
    }
}

