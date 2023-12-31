package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.models.Ownership;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OwnershipService {
    public GenericResponse getOwnership() {
        log.info("Getting ownerships");

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(Ownership.values())
                .build();
    }
}
