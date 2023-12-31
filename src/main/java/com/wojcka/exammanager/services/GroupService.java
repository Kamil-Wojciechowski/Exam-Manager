package com.wojcka.exammanager.services;

import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse getGroups() {
        log.info("Getting groups");

        return GenericResponse.ok(groupRepository.findAll());
    }
}
