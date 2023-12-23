package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.*;
import com.wojcka.exammanager.schemas.requests.ExamGroupQuestionRequest;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.questions.AnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ExamGroupQuestionService {
    @Autowired
    ExamGroupQuestionRepository examGroupQuestionRepository;

    @Autowired
    StudiesRepository studiesRepository;

    @Autowired
    StudiesUserRepository studiesUserRepository;

    @Autowired
    ExamRepository examRepository;

    @Autowired
    ExamGroupRepository examGroupRepository;

    @Autowired
    QuestionAnswerRepository questionAnswerRepository;

    @Autowired
    ExamGroupQuestionAnswerRepository examGroupQuestionAnswerRepository;

    Integer additionalTime = 600;

    private User getUserFromAuth() {
        log.info("getUserFromAuth");
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public GenericResponsePageable getQuestions(Integer studiesId, Integer examId,  Integer page, Integer size) {

        Studies studies = getStudiesById(studiesId);

        StudiesUser studiesUser = getStudiesUserByStudies(studies);

        Exam exam = getExamByIdAndStudies(examId, studies);

        ExamGroup examGroup = getExamGroupByExamAndStudiesUser(exam, studiesUser);

        validateExamTimeframe(exam);

        Page<ExamGroupQuestion> result = examGroupQuestionRepository.findAllByExamGroup(examGroup, PageRequest.of(page, size));

        result.getContent().forEach(item -> {
            item.getQuestion().setQuestionMetadata(null);
            item.getQuestion().getAnswers().forEach(answer -> {
                answer.setCorrect(null);
            });
        });

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

    private void validateExamTimeframe(Exam exam) {
        log.info("validateExamTimeframe");
        LocalDateTime currentTime = LocalDateTime.now();

        if(exam.getStartAt().isAfter(currentTime) | exam.getEndAt().plusSeconds(additionalTime).isBefore(currentTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("can_not_insert"));
        }
    }

    private ExamGroup getExamGroupByExamAndStudiesUser(Exam exam, StudiesUser studiesUser) {
        log.info("getExamGroupByExamAndStudiesUser");
        ExamGroup examGroup = examGroupRepository.findExamGroupByExamAndStudiesUser(exam, studiesUser).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        if(examGroup.getSent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("already_sent"));
        }

        return examGroup;
    }

    private Exam getExamByIdAndStudies(Integer examId, Studies studies) {
        log.info("Exam By Id And studies");
        return examRepository.findByIdAndStudies(examId, studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    private StudiesUser getStudiesUserByStudies(Studies studies) {
        log.info("getStudiesUserByStudies");

        return studiesUserRepository.findByUserAndStudiesAndOwner(getUserFromAuth(), studies, false).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });
    }

    private Studies getStudiesById(Integer studiesId) {
        log.info("getStudiesById");
        return studiesRepository.findById(studiesId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    private ExamGroup validateAll(Integer studiesId, Integer examId) {
        Studies studies = getStudiesById(studiesId);

        StudiesUser studiesUser = getStudiesUserByStudies(studies);

        Exam exam = getExamByIdAndStudies(examId, studies);

        ExamGroup examGroup = getExamGroupByExamAndStudiesUser(exam, studiesUser);

        validateExamTimeframe(exam);

        log.info("Validated - Return object");
        return examGroup;
    }

    public ExamGroup markAsSent(Integer studiesId, Integer examId, List<ExamGroupQuestion> examGroupQuestions) {
        ExamGroup examGroup = validateAll(studiesId, examId);

        examGroup.setSent(true);

        examGroupRepository.save(examGroup);

        return examGroup;
    }

    private Integer validatePoints(ExamGroupQuestion examGroupQuestion, List<ExamGroupQuestionAnswer> answers) {
        List<QuestionAnswer> correctAnswers = questionAnswerRepository.getByQuestionAndCorrect(examGroupQuestion.getQuestion(), true);

        List<QuestionAnswer> answered = new ArrayList<>();

        answers.forEach((item) -> {
            answered.add(item.getQuestionAnswer());
        });

        CorrectType correctType = CorrectType.CORRECT;
        Integer points = 2;

        if(correctAnswers.size() > 1 && answered.size() >= 1) {
            if(correctAnswers.size() < answered.size()) {
                correctType = CorrectType.NOT_CORRECT;
                points = 0;
            } else if(correctAnswers.size() == answered.size()) {
                if(!correctAnswers.equals(answered)) {
                    correctType = CorrectType.NOT_CORRECT;
                    points = 0;
                }
            } else {
                correctType = CorrectType.PARTIAL_CORRECT;
                points = 1;

                for(QuestionAnswer item : answered)  {
                   if(!correctAnswers.contains(item)) {
                       correctType = CorrectType.NOT_CORRECT;
                       points = 0;
                       break;
                   }
                }
            }
        } else if(correctAnswers.size() == 1 && answered.size() == 1) {
            if(!correctAnswers.equals(answered)) {
                correctType = CorrectType.NOT_CORRECT;
                points = 0;
            }
        } else {
            correctType = CorrectType.NOT_CORRECT;
            points = 0;
        }

        examGroupQuestion.setPoints(points);
        examGroupQuestion.setCorrect(correctType);

        examGroupQuestionRepository.save(examGroupQuestion);

        return points;
    }

    @Async
    public CompletableFuture<Void> processQuestions(ExamGroup examGroup, List<ExamGroupQuestion> examGroupQuestions) {
        Integer sumPoints = 0;

        for(ExamGroupQuestion item : examGroupQuestions) {
            ExamGroupQuestion examGroupQuestion = examGroupQuestionRepository.findExamGroupQuestionByExamGroupAndId(examGroup, item.getId()).orElse(new ExamGroupQuestion());

            if (examGroupQuestion != null) {

                if (examGroupQuestion.getQuestion().questionType.equals(QuestionType.SINGLE_ANSWER) || examGroupQuestion.getQuestion().questionType.equals(QuestionType.MULTIPLE_ANSWERS)) {

                        item.getAnswer().forEach((answer) -> {
                            answer.setExamGroupQuestion(examGroupQuestion);
                        });

                        List<ExamGroupQuestionAnswer> answers = examGroupQuestionAnswerRepository.saveAll(item.getAnswer());

                        sumPoints += validatePoints(examGroupQuestion, answers);

                }
            }
        }

        examGroup.setPoints(sumPoints);
        examGroupRepository.save(examGroup);

        return CompletableFuture.completedFuture(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    public void updateQuestion(Integer studiesId, Integer examId, Integer examGroupId, Long questionId, ExamGroupQuestionRequest request) {
        Studies studies = getStudiesById(studiesId);

        StudiesUser studiesUser = studiesUserRepository.findByUserAndStudiesAndOwner(getUserFromAuth(), studies, true).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        Exam exam = getExamByIdAndStudies(examId, studies);

        if(exam.getShowResults()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("can_not_update_published"));

        }

        ExamGroup examGroup = examGroupRepository.findByIdAndExam(examGroupId, exam).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        validateExamTimeframe(exam);

        ExamGroupQuestion examGroupQuestion = examGroupQuestionRepository.findExamGroupQuestionByExamGroupAndId(examGroup, questionId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });

        int points = 0;

        switch(request.getCorrectType()) {
            case CORRECT:
                points = 2;
                break;
            case PARTIAL_CORRECT:
                points = 1;
                break;
        }

        examGroupQuestion.setChangedManually(true);
        examGroupQuestion.setCorrect(request.getCorrectType());
        examGroupQuestion.setPoints(points);

        examGroupQuestionRepository.save(examGroupQuestion);

        Integer sumPoints = examGroupQuestionRepository.sumPointsByExamGroup(examGroup);

        examGroup.setPoints(sumPoints);

        examGroupRepository.save(examGroup);
    }
}
