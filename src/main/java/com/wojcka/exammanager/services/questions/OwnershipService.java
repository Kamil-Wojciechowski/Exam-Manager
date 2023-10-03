package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.models.Ownership;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import org.springframework.stereotype.Service;

@Service
public class OwnershipService {
    public GenericResponse getOwnership() {
        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(Ownership.values())
                .build();
    }
}
