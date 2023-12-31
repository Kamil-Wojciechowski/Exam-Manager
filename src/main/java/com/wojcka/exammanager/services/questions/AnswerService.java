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
        log.info("Validate ownership starts");
        if(!questionMetadataRepository.existsById(metadataId)) {
            log.warn("Item could not be found");

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        }

        User user = getUserFromAuth();

        QuestionMetadataOwnership qmOwnership = qmOwnershipRepository.findByUserAndQM(metadataId, user.getId()).orElseThrow(() -> {
            log.warn("Ownership for user could not be found");

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        });

        if(!qmOwnership.isEnoughToAccess()) {
            log.warn("User does not have enough access.");

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("ownership_not_found"));
        }

        return null;
    }

    private Question getQuestion(Integer metadataId, Integer id) {
        log.info("Getting question " + id);
        return questionRepository.findByMetadataIdAndId(metadataId, id).orElseThrow(() -> {
            log.warn("Item could not be found: " + id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    private void saveQuestionValidation(Question question, Integer sizeOfCorrectAnswers) {
            question.setValid(sizeOfCorrectAnswers >= 1);

            questionRepository.save(question);
    }


    private void validateNumberOfQuestionsAndType(Question question, QuestionAnswer request, QuestionAnswer answer) {
        if(request.getCorrect() || answer != null) {
            log.info("Validation of correct questions and types.");
            List<QuestionAnswer> answers = question.getAnswers();

                answers = answers.stream().filter(item -> item.getCorrect()).toList();

                Integer size = answers.size();

                if(answer != null) {
                    if (!answer.getCorrect() && request.getCorrect()) {
                        size++;
                    } else if (answer.getCorrect() && !request.getCorrect()) {
                        size--;
                    }
                }

                if(request != null && size.equals(0) && request.getCorrect()) {
                    size++;
                }

                if (question.isTypeForSingleAnswer() && (answers.size() == 1) && request.getCorrect()) {
                    if (answer == null) {
                        log.warn("Correct answer can be only one!");
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("answer_only_one"));
                    }

                    if (!answer.getCorrect()) {
                        log.warn("Correct answer can be only one!");
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("answer_only_one"));
                    }
                }

                saveQuestionValidation(question, size);
            }
    }


    public GenericResponse post(Integer metadataId, Integer questionId, QuestionAnswer request) {
        log.info("Creating answer starts");
        validateOwnership(metadataId);

        Question question = getQuestion(metadataId, questionId);

        validateNumberOfQuestionsAndType(question, request, null);

        request.setQuestion(Question.builder().id(questionId).build());

        request = answerRepository.save(request);

        log.info("Creating answer ends");
        return GenericResponse.created(request);
    }

    public Void patch(Integer metadataId, Integer questionId, Integer answerId, QuestionAnswer request) {
        log.info("Updating answer starts: " + answerId);
        validateOwnership(metadataId);

        Question question = getQuestion(metadataId, questionId);

        QuestionAnswer questionAnswer = answerRepository.getByIdAndQuestionId(answerId, questionId).orElseThrow(() -> {
            log.warn("Question answer could not be found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        validateNumberOfQuestionsAndType(question, request, questionAnswer);

        request.setId(answerId);
        request.setQuestion(Question.builder().id(questionId).build());

        answerRepository.save(request);

        log.info("Updating answer ends: " + answerId);
        return null;
    }

    public Void delete(Integer metadataId, Integer questionId, Integer answerId) {
        log.info("Deleting answer starts: " + answerId);

        validateOwnership(metadataId);

        Question question = getQuestion(metadataId, questionId);

        QuestionAnswer questionAnswer = answerRepository.getByIdAndQuestionId(answerId, questionId).orElseThrow(() -> {
            log.warn("Question answer could not be found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        if(question.questionType.equals(QuestionType.SINGLE_ANSWER) && questionAnswer.getCorrect()) {
            saveQuestionValidation(question, 0);
        } else if(question.questionType.equals(QuestionType.MULTIPLE_ANSWERS)) {
            if((question.getAnswers().stream().filter((item) -> item.getCorrect()).toList().size()) == 1 && questionAnswer.getCorrect()) {
                saveQuestionValidation(question, 0);
            }
        }

        answerRepository.deleteById(questionAnswer.getId());

        log.info("Deleting answer ends: " + answerId);
        return null;
    }
}
