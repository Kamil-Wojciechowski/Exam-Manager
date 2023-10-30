package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.StudiesRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudiesUserService {

    @Autowired
    private StudiesUserRepository studiesUserRepository;

    @Autowired
    private StudiesRepository studiesRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateUser(Boolean action) {
        StudiesUser studiesUser = studiesUserRepository.findByUser(getUserFromAuth()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        if (action && studiesUser.getOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        }
    }

    private Studies getStudies(Integer studiesId) {
        return studiesRepository.findById(studiesId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }
    public GenericResponsePageable get(Integer studiesId, Integer page, Integer size) {
        validateUser(false);

        Studies studies = getStudies(studiesId);

        Page<StudiesUser> result = studiesUserRepository.findByStudies(studies, PageRequest.of(page,size));

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(result.get())
                .page(page)
                .size(size)
                .hasNext(result.hasNext())
                .pages(result.getTotalPages())
                .total(result.getTotalElements())
                .build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse post(Integer studiesId, StudiesUser studiesUser) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        studiesUser.setStudies(studies);

        studiesUser = studiesUserRepository.save(studiesUser);

        return GenericResponse.created(studiesUser);
    }

    private StudiesUser getStudiesUsserByStudiesAndId(Studies studies, Integer studiesUserId) {
        return studiesUserRepository.findByStudiesAndId(studies, studiesUserId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    public GenericResponse get(Integer studiesId, Integer studiesUserId) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        return GenericResponse.ok(studiesUser);
    }


    @PreAuthorize("hasRole('TEACHER')")
    public void delete(Integer studiesId, Integer studiesUserId) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        studiesUserRepository.delete(studiesUser);
    }

    public GenericResponse importUsers(Integer studiesId, MultipartFile file) {

        return GenericResponse.created(new Object());
    }
}
