package com.wojcka.exammanager.services;

import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse getGroups() {
        return GenericResponse.ok(groupRepository.findAll());
    }
}
