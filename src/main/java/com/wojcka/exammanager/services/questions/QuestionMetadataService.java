package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.components.ObjectToJson;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class QuestionMetadataService {

    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;

    @Autowired
    private QMOwnerRepository qmOwnerRepository;

    public GenericResponsePageable get(int page, int size) {
        log.info("Getting Questions Metadata starts");
        Pageable pageable = PageRequest.of(page,size);

        User user = getUserFromAuth();

        Page questionMetadata = questionMetadataRepository.findAllByUser(user.getId(), pageable);

        log.info("Getting Questions Metadata ends");

        return GenericResponsePageable.builder()
                .code(200)
                .status("OK")
                .data(questionMetadata.get())
                .page(page)
                .size(size)
                .hasNext(questionMetadata.hasNext())
                .pages(questionMetadata.getTotalPages())
                .total(questionMetadata.getTotalElements())
                .build();

    }

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void createOwnership(QuestionMetadata request) {
        User user = getUserFromAuth();
        log.info("Creating ownership for user: " + user.getId() + " for metadata: " + request.getId());

        QuestionMetadataOwnership ownership = QuestionMetadataOwnership.builder()
                .questionMetadata(request)
                .ownership(Ownership.OWNER)
                .user(user)
                .build();

        qmOwnerRepository.save(ownership);

        log.info("Creating ownership for user ends");

    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse create(QuestionMetadata request) {
        log.info("Creating Question Metadata starts");
        request = questionMetadataRepository.save(request);

        createOwnership(request);

        log.info("Creating Question Metadata ends");
        return GenericResponse.builder().code(201).status("CREATED").data(request).build();
    }

    public GenericResponse getById(Integer id) {
        log.info("Getting Question Metadata by id starts: " + id);

        QuestionMetadata questionMetadata = questionMetadataRepository.findById(id).orElseThrow(() -> {
            log.warn("Item could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        log.info("Getting Question Metadata by id ends");
        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(questionMetadata)
                .build();
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public Void update(QuestionMetadata request) {
        log.info("Update Question Metadata starts: " + request.getId());

        User user = getUserFromAuth();

        if(request.isIdEmpty()) {
            request = questionMetadataRepository.save(request);

            createOwnership(request);
        } else {
            QuestionMetadataOwnership ownership = qmOwnerRepository.findByUserAndQM(request.getId(), user.getId()).orElseThrow(() -> {
                log.warn("Ownership not found");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
            });

            if(ownership.isEnoughToAccess()) {
                request = questionMetadataRepository.save(request);
            } else {
                log.warn("User does not contain permissions");
                log.warn(user.getId().toString());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
            }
        }

        log.info("Update Question Metadata ends");

        return null;
    }

    @PreAuthorize("hasRole('TEACHER')")
    public Void deleteById(Long id) {
        log.info("Delete Question Metadata starts: " + id);

        User user = getUserFromAuth();

        QuestionMetadata questionMetadata = questionMetadataRepository.findById(id.intValue()).orElseThrow(() -> {
            log.warn("Item could not found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        QuestionMetadataOwnership ownership = qmOwnerRepository.findByUserAndQM(questionMetadata.getId(), user.getId()).orElseThrow(() -> {
            log.warn("Ownership not found");

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(ownership.getOwnership().equals(Ownership.OWNER)) {
            try {
                questionMetadataRepository.delete(questionMetadata);
            } catch (RuntimeException exception) {
                log.warn("User is not an owner of this item.");
                log.warn(user.getId().toString());

                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("can_not_delete"));
            }
        } else {
            log.warn("User with no access try to delete database");
            log.warn(user.getId().toString());

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        log.info("Delete Question Metadata ends");


        return null;
    }
}
