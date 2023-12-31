package com.wojcka.exammanager.services;

import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@PreAuthorize("hasRole('TEACHER')")
public class RoleService {

    @Autowired
    private GroupRepository groupRepository;

    public GenericResponse getRoles() {
        log.info("Getting roles");

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(groupRepository.findAll())
                .build();
    }
}
