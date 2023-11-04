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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;

    @Autowired
    private QMOwnerRepository qmOwnerRepository;

    @Autowired
    private StudiesRepository studiesRepository;

    @Autowired
    private StudiesUserRepository studiesUserRepository;

    public GenericResponsePageable getStudiesByAuthenticatedUser(Integer studiesId, int size, int page) {

        Studies studies = fetchStudies(studiesId);

        studiesUserRepository.findByUserAndStudies(getUserFromAuth(), studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        Page<Exam> result = examRepository.findAllByStudies(studies, PageRequest.of(page,size));

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

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Studies fetchStudies(Integer studiesId) {
        return studiesRepository.findById(studiesId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    private Studies validateStudies(Integer studiesId) {
        Studies studies = fetchStudies(studiesId);

        studiesUserRepository.findByUserAndStudiesAndOwner(getUserFromAuth(), studies, true).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        return studies;
    }

    private QuestionMetadata validateQuestionMetadata(Integer questionMetadataId) {
        QuestionMetadata questionMetadata =  questionMetadataRepository.findById(questionMetadataId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        QuestionMetadataOwnership questionMetadataOwnership = qmOwnerRepository.findByUserAndAndQuestionMetadata(getUserFromAuth(), questionMetadata).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        if(!questionMetadataOwnership.isEnoughToAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        }

        return questionMetadata;
    }

    private void validateDateRange(Exam exam) {
        if(exam.getStartAt().isAfter(exam.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("dates_wrong_order"));
        }
    }
    public GenericResponse post(Integer studiesId, Exam exam) {
        Studies studies = validateStudies(studiesId);
        QuestionMetadata questionMetadata = validateQuestionMetadata(exam.getQuestionMetadata().getId());

        validateDateRange(exam);

        exam.setStudies(studies);
        exam.setQuestionMetadata(questionMetadata);

        exam = examRepository.save(exam);

        return GenericResponse.created(exam);
    }

    private Exam getExam(Integer examId, Studies studies) {
        return examRepository.findByIdAndStudies(examId, studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }
    public GenericResponse get(Integer studiesId, Integer examId) {
        Studies studies = validateStudies(studiesId);

        Exam exam = getExam(examId, studies);

        validateQuestionMetadata(exam.getQuestionMetadata().getId());

        return GenericResponse.ok(exam);
    }

    public void patch(Integer studiesId, Integer examId, Exam request) {
        validateDateRange(request);

        Studies studies = validateStudies(studiesId);

        Exam exam = getExam(examId, studies);
        QuestionMetadata questionMetadata = validateQuestionMetadata(request.getQuestionMetadata().getId());

        request.setId(exam.getId());
        request.setStudies(studies);
        request.setQuestionMetadata(questionMetadata);

        examRepository.save(request);
    }

    public void delete(Integer studiesId, Integer examId) {
        Studies studies = validateStudies(studiesId);

        Exam exam = getExam(examId, studies);

        validateQuestionMetadata(exam.getQuestionMetadata().getId());

        examRepository.delete(exam);
    }
}
