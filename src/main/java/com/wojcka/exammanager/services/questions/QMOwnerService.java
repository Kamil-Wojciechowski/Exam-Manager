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
        log.info("Validating ownership: " + metadataId);
        User user = getUserFromAuth();

        questionMetadataRepository.findById(metadataId).orElseThrow(() -> {
            log.warn("Question metadata could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        QuestionMetadataOwnership qmOwnership = qmOwnerRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            log.warn("Ownership could not be found");

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!qmOwnership.getOwnership().equals(Ownership.OWNER)) {
            log.warn("User is not owner");

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }
    }

    public GenericResponsePageable get(Integer metadataId, int page, int size) {
        log.info("Getting Ownerships of metadata starts " + metadataId);
        Pageable pageable = PageRequest.of(page,size);

        validateOwnership(metadataId);

        Page qmOwnershipPage = qmOwnerRepository.findByQuestionMetadata(metadataId, pageable);

        log.info("Getting Ownerships of metadata ends");

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
        log.info("Creating Ownerships for metadata starts " + metadataId);

        validateOwnership(metadataId);

        if(qmOwnerRepository.findByUserAndQM(metadataId, request.getUser().getId()).isPresent()) {
            log.warn("User already exists for this metadata");

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("item_already_exits"));
        }

        QuestionMetadata questionMetadata = QuestionMetadata.builder().id(metadataId).build();

        request.setQuestionMetadata(questionMetadata);

        request = qmOwnerRepository.save(request);

        log.info("Creating Ownerships for metadata ends");

        return GenericResponse.builder()
                .code(201)
                .status("CREATED")
                .data(request)
                .build();
    }


    public GenericResponse getById(Integer metadataId, Integer id) {
        log.info("Getting Ownerships for metadata by id starts " + metadataId + " " + id);

        validateOwnership(metadataId);

        QuestionMetadataOwnership qmOwnership = qmOwnerRepository.findByMetadataIdAndId(metadataId, id).orElseThrow(() -> {
            log.warn("Item could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        log.info("Getting Ownerships for metadata by id ends");


        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(qmOwnership)
                .build();
    }

    public Void patch(Integer metadataId, Integer id, QuestionMetadataOwnership request) {
        log.info("Update of Ownerships for metadata by id starts " + metadataId + " " + id);

        validateOwnership(metadataId);

        if(qmOwnerRepository.findByMetadataIdAndId(metadataId, id).isEmpty()) {
            log.warn("Item could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        };

        request.setId(id);
        request.setQuestionMetadata(QuestionMetadata.builder().id(metadataId).build());

        log.info("Update of Ownerships for metadata by id ends");

        qmOwnerRepository.save(request);

        return null;
    }

    public Void delete(Integer metadataId, Integer id) {
        log.info("Delete of Ownerships for metadata by id starts " + metadataId + " " + id);

        validateOwnership(metadataId);

        if(qmOwnerRepository.findByMetadataIdAndId(metadataId, id).isEmpty()) {
            log.warn("Item could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        };

        qmOwnerRepository.deleteById(id);

        log.info("Delete of Ownerships for metadata by id ends");
        return null;
    }
}
