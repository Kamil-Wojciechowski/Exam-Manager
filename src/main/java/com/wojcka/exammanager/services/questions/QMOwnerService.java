package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.Ownership;
import com.wojcka.exammanager.models.QuestionMetadata;
import com.wojcka.exammanager.models.QuestionMetadataOwnership;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.QMOwnerRepository;
import com.wojcka.exammanager.repositories.QuestionMetadataRepository;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class QMOwnerService {

    @Autowired
    private QMOwnerRepository qmOwnerRepository;

    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateOwnership(Integer metadataId) {
        User user = getUserFromAuth();

        questionMetadataRepository.findById(metadataId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        QuestionMetadataOwnership qmOwnership = qmOwnerRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!qmOwnership.getOwnership().equals(Ownership.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }
    }

    public GenericResponsePageable get(Integer metadataId, int page, int size) {
        Pageable pageable = PageRequest.of(page,size);

        validateOwnership(metadataId);

        Page qmOwnershipPage = qmOwnerRepository.findByQuestionMetadata(metadataId, pageable);

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(qmOwnershipPage.get())
                .page(page)
                .size(size)
                .hasNext(qmOwnershipPage.hasNext())
                .pages(qmOwnershipPage.getTotalPages())
                .total(qmOwnershipPage.getTotalElements())
                .build();

    }

    public GenericResponse post(Integer metadataId, QuestionMetadataOwnership request) {
        validateOwnership(metadataId);

        if(qmOwnerRepository.findByUserAndQM(metadataId, request.getUser().getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("item_already_exits"));
        }

        QuestionMetadata questionMetadata = QuestionMetadata.builder().id(metadataId).build();

        request.setQuestionMetadata(questionMetadata);

        request = qmOwnerRepository.save(request);

        return GenericResponse.builder()
                .code(201)
                .status("CREATED")
                .data(request)
                .build();
    }


    public GenericResponse getById(Integer metadataId, Integer id) {
        validateOwnership(metadataId);

        QuestionMetadataOwnership qmOwnership = qmOwnerRepository.findByMetadataIdAndId(metadataId, id).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(qmOwnership)
                .build();
    }

    public Void patch(Integer metadataId, Integer id, QuestionMetadataOwnership request) {
        validateOwnership(metadataId);

        if(qmOwnerRepository.findByMetadataIdAndId(metadataId, id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        };

        request.setId(id);
        request.setQuestionMetadata(QuestionMetadata.builder().id(metadataId).build());

        qmOwnerRepository.save(request);

        return null;
    }

    public Void delete(Integer metadataId, Integer id) {
        validateOwnership(metadataId);

        if(qmOwnerRepository.findByMetadataIdAndId(metadataId, id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        };

        qmOwnerRepository.deleteById(id);

        return null;
    }
}
