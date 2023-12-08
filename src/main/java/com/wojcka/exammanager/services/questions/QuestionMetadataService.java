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
        Pageable pageable = PageRequest.of(page,size);

        User user = getUserFromAuth();

        Page questionMetadata = questionMetadataRepository.findAllByUser(user.getId(), pageable);

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

        QuestionMetadataOwnership ownership = QuestionMetadataOwnership.builder()
                .questionMetadata(request)
                .ownership(Ownership.OWNER)
                .user(user)
                .build();

        ownership = qmOwnerRepository.save(ownership);

        log.info(ObjectToJson.toJson(ownership));
        log.info("Owner = " + ownership.getUser().toString());
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse create(QuestionMetadata request) {
        request = questionMetadataRepository.save(request);

        log.info(ObjectToJson.toJson(request));

        createOwnership(request);

        return GenericResponse.builder().code(201).status("CREATED").data(request).build();
    }

    public GenericResponse getById(Integer id) {
        QuestionMetadata questionMetadata = questionMetadataRepository.findById(id).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(questionMetadata)
                .build();
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public Void update(QuestionMetadata request) {
        User user = getUserFromAuth();

        if(request.isIdEmpty()) {
            request = questionMetadataRepository.save(request);

            createOwnership(request);
        } else {
            QuestionMetadataOwnership ownership = qmOwnerRepository.findByUserAndQM(request.getId(), user.getId()).orElseThrow(() -> {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
            });

            if(ownership.isEnoughToAccess()) {
                request = questionMetadataRepository.save(request);
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
            }
        }

        log.info(ObjectToJson.toJson(request));

        return null;
    }

    @PreAuthorize("hasRole('TEACHER')")
    public Void deleteById(Long id) {
        User user = getUserFromAuth();

        QuestionMetadata questionMetadata = questionMetadataRepository.findById(id.intValue()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        QuestionMetadataOwnership ownership = qmOwnerRepository.findByUserAndQM(questionMetadata.getId(), user.getId()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(ownership.getOwnership().equals(Ownership.OWNER)) {
            try {
                questionMetadataRepository.delete(questionMetadata);
            } catch (RuntimeException exception) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("can_not_delete"));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        return null;
    }
}
