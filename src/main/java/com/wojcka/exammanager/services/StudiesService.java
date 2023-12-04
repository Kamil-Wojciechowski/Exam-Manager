package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.*;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class StudiesService {
    @Autowired
    private StudiesRepository studiesRepository;

    @Autowired
    private StudiesUserRepository studiesUserRepository;

    private List<StudiesUser> filterOwner(List<StudiesUser> studiesUserList, User user) {
        return studiesUserList.stream().filter((studiesUser ->
                studiesUser.getUser().getId().equals(user.getId()) & studiesUser.getOwner()
        )).toList();
    }

    public GenericResponsePageable get(Integer page, Integer size) {
        User user = getUserFromAuth();

        Page<Studies> pageable = studiesRepository.getByUser(user.getId() ,PageRequest.of(page, size));

        pageable.getContent().forEach((item) -> {
            List<StudiesUser> studiesUserList = filterOwner(item.getStudiesUserList(), user);

            item.setOwner(!studiesUserList.isEmpty());
        });

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(pageable.get())
                .page(page)
                .size(size)
                .hasNext(pageable.hasNext())
                .pages(pageable.getTotalPages())
                .total(pageable.getTotalElements())
                .build();
    }



    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse post(Studies studies) {
        studies = studiesRepository.save(studies);

        StudiesUser studiesUser = StudiesUser.builder()
                .user(getUserFromAuth())
                .owner(true)
                .studies(studies)
                .build();

        studiesUserRepository.save(studiesUser);

        return GenericResponse.created(studies);
    }

    private Studies getStudiesById(Integer id) {
        Studies studies =  studiesRepository.findById(id).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        List<StudiesUser> studiesUserList = filterOwner(studies.getStudiesUserList(), getUserFromAuth());

        studies.setOwner(!studiesUserList.isEmpty());

        return studies;
    }

    public GenericResponse getById(Integer id) {
        Studies studies = getStudiesById(id);

        return GenericResponse.ok(studies);
    }

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateOwnership(Studies studies) {
        studiesUserRepository.findByUserAndStudiesAndOwner(getUserFromAuth(), studies, true).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });
    }

    @PreAuthorize("hasRole('TEACHER')")
    public void update(Integer id, Studies request) {
        Studies studies = getStudiesById(id);

        validateOwnership(studies);

        request.setId(id);

        studiesRepository.save(request);
    }

    @PreAuthorize("hasRole('TEACHER')")
    public void delete(Integer id) {

        Studies studies = getStudiesById(id);

        validateOwnership(studies);

        try {
            studiesRepository.delete(studies);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("can_not_delete"));
        }
    }
}
