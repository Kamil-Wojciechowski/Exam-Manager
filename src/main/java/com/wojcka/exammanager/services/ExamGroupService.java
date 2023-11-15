package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.Exam;
import com.wojcka.exammanager.models.ExamGroup;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.ExamGroupRepository;
import com.wojcka.exammanager.repositories.ExamRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamGroupService {
    @Autowired
    private ExamGroupRepository examGroupRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudiesUserRepository studiesUserRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateOwner(Exam exam) {
        studiesUserRepository.findByUserAndStudiesAndOwner(getUserFromAuth(), exam.getStudies(), true).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_forbidden"));
        });
    }

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponsePageable get(Integer studiesId, Integer examId, Integer page, Integer size) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        validateOwner(exam);

        if(!exam.getStudies().getId().equals(studiesId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_forbidden"));
        }

        Page<ExamGroup> result =  examGroupRepository.findAllByExam(exam, PageRequest.of(page, size));

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
}
