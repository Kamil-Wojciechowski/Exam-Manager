package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.QMOwnerRepository;
import com.wojcka.exammanager.repositories.QuestionMetadataRepository;
import com.wojcka.exammanager.repositories.QuestionRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class QuestionService {

    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QMOwnerRepository qmOwnershipRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Void validateOwnership(Integer metadataId, Boolean isMethodGet) {
        if(!questionMetadataRepository.existsById(metadataId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        }

        User user = getUserFromAuth();

        QuestionMetadataOwnership qmOwnership = qmOwnershipRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!isMethodGet && !qmOwnership.isEnoughToAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        return null;
    }

    public GenericResponsePageable get(Integer metadataId, Integer page, Integer size, Boolean archived) {
        Pageable pageable = PageRequest.of(page,size);

        validateOwnership(metadataId, true);

        Page<Question> result = questionRepository.findAllByMetadataId(metadataId, archived, pageable);

        result.forEach((item) -> {
            item.questionMetadata = null;
        } );

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

    public GenericResponse create(Question request) {
        validateOwnership(request.getQuestionMetadata().getId(), false);

        request = questionRepository.save(request);

        return GenericResponse.builder()
                .code(201)
                .status("CREATED")
                .data(request)
                .build();
    }

    private Question getQuestion(Integer metadataId, Integer id) {
        return questionRepository.findByMetadataIdAndId(metadataId, id).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    public GenericResponse getById(Integer metadataId, Integer id) {
        validateOwnership(metadataId, true);

        Question question = getQuestion(metadataId, id);

        question.setQuestionMetadata(null);

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(question)
                .build();
    }

    public Void upsert(Integer metadataId, Integer id, Question request) {
        validateOwnership(metadataId, false);

        request.setQuestionMetadata(QuestionMetadata.builder().id(metadataId).build());

        try {
            getQuestion(metadataId, id);

            request.setId(id);
        } catch(Exception ex) {
            log.info("Question was not found with provided data. Craeting new.");
        }

            questionRepository.save(request);

        return null;
    }

    public Void delete(Integer metadataId, Integer id, Boolean permaDelete) {
        validateOwnership(metadataId, false);

        Question question = getQuestion(metadataId, id);

        if(permaDelete) {
            questionRepository.delete(question);
        } else {
            question.archived = true;
            questionRepository.save(question);
        }

        return null;
    }

}
