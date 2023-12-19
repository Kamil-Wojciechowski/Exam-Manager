package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.QMOwnerRepository;
import com.wojcka.exammanager.repositories.QuestionAnswerRepository;
import com.wojcka.exammanager.repositories.QuestionMetadataRepository;
import com.wojcka.exammanager.repositories.QuestionRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Autowired
    private QuestionAnswerRepository questionAnswerRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private QuestionMetadata validateOwnership(Integer metadataId, Boolean isMethodGet) {
        QuestionMetadata questionMetadata = questionMetadataRepository.findById(metadataId).orElseThrow(() ->  {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        User user = getUserFromAuth();

        QuestionMetadataOwnership qmOwnership = qmOwnershipRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!isMethodGet && !qmOwnership.isEnoughToAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        return questionMetadata;
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

    @Transactional
    public GenericResponse importCSV(Integer metadataId, MultipartFile file) {
        QuestionMetadata questionMetadata = validateOwnership(metadataId, false);

        if(file.getContentType().equals("text/csv")) {

            try {
                InputStreamReader reader = new InputStreamReader(file.getInputStream());

                CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

                for (CSVRecord csvRecord : csvParser) {
                    if (csvRecord.size() >= csvParser.getHeaderMap().size()) {

                        Question question = Question.builder()
                                .question(csvRecord.get(0))
                                .questionMetadata(questionMetadata)
                                .archived(false)
                                .build();


                        List<String> correctAnswer = Arrays.asList(csvRecord.get("correct").split(","));
                        if (correctAnswer.size() >= 2) {
                            question.setQuestionType(QuestionType.MULTIPLE_ANSWERS);

                        } else {
                            question.setQuestionType(QuestionType.SINGLE_ANSWER);
                        }

                        question = questionRepository.save(question);

                        List<QuestionAnswer> answers = new ArrayList<>();



                        for (String header : csvParser.getHeaderMap().keySet()) {
                            if (header.startsWith("answer")) {
                                Boolean correct = correctAnswer.contains(header.substring(6, header.length()));

                                if (!csvRecord.get(header).isEmpty()) {
                                    answers.add(
                                            QuestionAnswer.builder()
                                                    .question(question)
                                                    .answer(csvRecord.get(header))
                                                    .correct(correct)
                                                    .build()
                                    );
                                }
                            }
                        }


                        questionAnswerRepository.saveAll(answers);

                        question.setValid(!answers.stream().filter(item -> item.getCorrect()).toList().isEmpty());

                        questionRepository.save(question);
                    }
                }

            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
            }

        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("file_not_supported"));
        }

        return GenericResponse.created(null);
    }

}