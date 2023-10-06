package com.wojcka.exammanager.services.questions;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.QMOwnerRepository;
import com.wojcka.exammanager.repositories.QuestionAnswerRepository;
import com.wojcka.exammanager.repositories.QuestionMetadataRepository;
import com.wojcka.exammanager.repositories.QuestionRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")

public class AnswerService {
    @Autowired
    private QuestionAnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;

    @Autowired
    private QMOwnerRepository qmOwnershipRepository;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Void validateOwnership(Integer metadataId) {
        if(!questionMetadataRepository.existsById(metadataId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        }

        User user = getUserFromAuth();

        QuestionMetadataOwnership qmOwnership = qmOwnershipRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!qmOwnership.isEnoughToAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        return null;
    }

    private Question getQuestion(Integer metadataId, Integer id) {
        return questionRepository.findByMetadataIdAndId(metadataId, id).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    private Void validateNumberOfQuestionsAndType(Question question, QuestionAnswer request, Integer answerId) {
        if(request.getCorrect()) {
            List<QuestionAnswer> answers = question.getAnswers();

            answers = answers.stream().filter(item -> item.getCorrect()).collect(Collectors.toList());

            if (question.isTypeForSingleAnswer() && (answers.size() == 1)) {
                if(answerId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("answer_only_one"));
                }

                QuestionAnswer answer = answerRepository.findById(answerId).orElseThrow(() -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
                });

                if(!answer.getCorrect()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("answer_only_one"));
                }

            }
        }

        return null;
    }

    public GenericResponse post(Integer metadataId, Integer questionId, QuestionAnswer request) {
        validateOwnership(metadataId);

        Question question = getQuestion(metadataId, questionId);

        validateNumberOfQuestionsAndType(question, request, null);

        request.setQuestion(Question.builder().id(questionId).build());

        request = answerRepository.save(request);

        return GenericResponse.created(request);
    }

    public Void patch(Integer metadataId, Integer questionId, Integer answerId, QuestionAnswer request) {
        validateOwnership(metadataId);

        Question question = getQuestion(metadataId, questionId);

        validateNumberOfQuestionsAndType(question, request, answerId);

        request.setId(answerId);
        request.setQuestion(Question.builder().id(questionId).build());

        answerRepository.save(request);

        return null;
    }
}
