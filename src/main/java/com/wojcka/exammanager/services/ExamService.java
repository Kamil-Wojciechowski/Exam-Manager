package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.models.annocument.Annoucment;
import com.wojcka.exammanager.models.annocument.State;
import com.wojcka.exammanager.models.cousework.AssigneeMode;
import com.wojcka.exammanager.models.cousework.CourseWork;
import com.wojcka.exammanager.models.cousework.WorkType;
import com.wojcka.exammanager.repositories.*;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.internal.GoogleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
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

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamGroupRepository examGroupRepository;

    @Autowired
    private ExamGroupQuestionRepository examGroupQuestionRepository;

    @Autowired
    private GoogleService googleService;

    public GenericResponsePageable getStudiesByAuthenticatedUser(Integer studiesId, String order, String orderBy, Boolean archived, int size, int page) {

        Studies studies = fetchStudies(studiesId);

        studiesUserRepository.findByUserAndStudies(getUserFromAuth(), studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        Sort sort;

        if(order.equals("asc")) {
            sort = Sort.by(Sort.Order.asc(orderBy));
        } else {
            sort = Sort.by(Sort.Order.desc(orderBy));
        }

        Page<Exam> result = examRepository.findAllByStudiesAndArchived(studies, archived, PageRequest.of(page, size, sort));

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

        if(exam.getStartAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("can_not_update"));

        }
    }

    private void addUsersToExamGroup(Studies studies, QuestionMetadata questionMetadata ,Exam exam) {
        List<StudiesUser> studiesUserList = studiesUserRepository.findByStudiesAndOwner(studies, false);

        List<StudentSubmissions> studentSubmissions;

        if(studies.getClassroomId() != null && exam.getCourseWorkId() != null) {
            studentSubmissions = googleService.listAssignments(studies.getClassroomId(), exam.getCourseWorkId());
        } else {
            studentSubmissions = null;
        }

        studiesUserList.forEach(studiesUser -> {
            List<Question> questionsPerUser = questionRepository.findAllByQuestionMetadataAndRandomWhereValid(questionMetadata.getId(), exam.getQuestionPerUser());

            List<ExamGroupQuestion> examGroupQuestionList = new ArrayList<>();
            String submmisionId = null;

            try {
                submmisionId = studentSubmissions.stream().filter((item) -> item.getUserId().equals(studiesUser.getUser().getGoogleUserId())).toList().get(0).getId();
            } catch (Exception ex) {
                log.info("Student submission not found in this classroom");
            }
            ExamGroup examGroup = examGroupRepository.save(ExamGroup.builder()
                    .exam(exam)
                    .studiesUser(studiesUser)
                    .submissionId(submmisionId)
                    .sent(false)
                    .build());

            questionsPerUser.forEach(question -> {
               examGroupQuestionList.add(
                       ExamGroupQuestion.builder()
                               .examGroup(examGroup)
                               .question(question)
                               .build()
               );
            });

            examGroupQuestionRepository.saveAll(examGroupQuestionList);
        });
    }

    private void validateQuestionsNumber(Exam exam) {
        Integer countValid = questionRepository.countAllByQuestionMetadataAndValidAndArchived(exam.getQuestionMetadata(), true, false);

        if(exam.getQuestionPerUser() > countValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("to_many_count"));
        }
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse post(Integer studiesId, Exam exam) {
        Studies studies = validateStudies(studiesId);
        QuestionMetadata questionMetadata = validateQuestionMetadata(exam.getQuestionMetadata().getId());

        validateDateRange(exam);
        validateQuestionsNumber(exam);

        exam.setStudies(studies);
        exam.setQuestionMetadata(questionMetadata);

        exam = examRepository.save(exam);

        if(studies.getClassroomId() != null && !studies.getClassroomId().isEmpty()) {
            CourseWork courseWork = CourseWork.builder()
                    .title(exam.getName())
                    .description(Translator.toLocale("exam_description"))
                    .assigneeMode(AssigneeMode.ALL_STUDENTS)
                    .workType(WorkType.ASSIGNMENT)
                    .maxPoints(exam.getQuestionPerUser() * 2)
                    .state(State.PUBLISHED)
                    .build();

            courseWork = googleService.createCourseWork(studies.getClassroomId(), courseWork);

            exam.setCourseWorkId(courseWork.getId());
            examRepository.save(exam);
        }

        addUsersToExamGroup(studies, questionMetadata, exam);

        return GenericResponse.created(exam);
    }

    private Exam getExam(Integer examId, Studies studies) {
        return examRepository.findByIdAndStudies(examId, studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    public GenericResponse get(Integer studiesId, Integer examId) {
        Studies studies = fetchStudies(studiesId);

        StudiesUser studiesUser = studiesUserRepository.findByUserAndStudies(getUserFromAuth(), studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        Exam exam = getExam(examId, studies);

        if(!studiesUser.getOwner() & exam.getShowResults()) {
            ExamGroup examGroup = examGroupRepository.findExamGroupByExamAndStudiesUser(exam, studiesUser).orElseThrow(() -> {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
            });

            exam.setPoints(examGroup.getPoints());
        }

        return GenericResponse.ok(exam);
    }

    private void manageExamQuestions(Exam exam) {
        List<StudiesUser> studiesUserList = studiesUserRepository.findByStudiesAndOwner(exam.getStudies(), false);

        studiesUserList.forEach(studiesUser -> {
            ExamGroup examGroup = examGroupRepository.findExamGroupByExamAndStudiesUser(exam, studiesUser).orElseThrow(() -> {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Translator.toLocale("internal_server_error"));
            });

            Integer countMissing = exam.getQuestionPerUser() - examGroup.getExamGroupQuestionList().size();

            if(countMissing > 0) {

                List<Question> additonalQuestions = questionRepository.findAdditonalQuestion(exam.getQuestionMetadata().getId(), countMissing, examGroup.getId());

                List<ExamGroupQuestion> additional = new ArrayList<>();

                additonalQuestions.forEach((question -> {
                    additional.add(
                            ExamGroupQuestion.builder()
                                    .examGroup(examGroup)
                                    .question(question)
                                    .build()
                    );
                }));

                examGroupQuestionRepository.saveAll(additional);
            } else if (countMissing < 0) {
                countMissing = Math.abs(countMissing);

                examGroupQuestionRepository.deleteAll(examGroupQuestionRepository.findAllByExamGroup(examGroup ,PageRequest.of(0, countMissing)).getContent());
            }
        });
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public void patch(Integer studiesId, Integer examId, Exam request) {
        validateDateRange(request);

        Studies studies = validateStudies(studiesId);

        Exam exam = getExam(examId, studies);
        QuestionMetadata questionMetadata = validateQuestionMetadata(request.getQuestionMetadata().getId());

        validateQuestionsNumber(request);

        request.setId(exam.getId());
        request.setStudies(studies);
        request.setQuestionMetadata(questionMetadata);
        request.setCourseWorkId(exam.getCourseWorkId());

        if(exam.getQuestionPerUser() != request.getQuestionPerUser()) {
            manageExamQuestions(request);

            if(studies.getClassroomId() != null) {
                if(exam.getCourseWorkId() != null) {
                    googleService.deleteCourseWork(studies.getClassroomId(), exam.getCourseWorkId());
                }

                CourseWork courseWork = CourseWork.builder()
                        .id(exam.getCourseWorkId())
                        .title(exam.getName())
                        .description(Translator.toLocale("exam_description"))
                        .assigneeMode(AssigneeMode.ALL_STUDENTS)
                        .workType(WorkType.ASSIGNMENT)
                        .maxPoints(exam.getQuestionPerUser() * 2)
                        .state(State.PUBLISHED)
                        .build();

                courseWork = googleService.createCourseWork(studies.getClassroomId(), courseWork);

                request.setCourseWorkId(courseWork.getId());
            }

        }

        examRepository.save(request);
    }

    @PreAuthorize("hasRole('TEACHER')")
    public void delete(Integer studiesId, Integer examId) {
        Studies studies = validateStudies(studiesId);

        Exam exam = getExam(examId, studies);

        validateQuestionMetadata(exam.getQuestionMetadata().getId());

        if(exam.getArchived()) {
            if(studies.getClassroomId() != null && exam.getCourseWorkId() != null) {
                googleService.deleteCourseWork(studies.getClassroomId(), exam.getCourseWorkId());
            }

            examRepository.delete(exam);
        } else {
            exam.setArchived(true);

            examRepository.save(exam);
        }

    }

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse postAnnoucmnet(Integer studiesId, Integer examId, Annoucment annoucment) {

        Studies studies = validateStudies(studiesId);
        Exam exam = getExam(examId, studies);

        googleService.createAnnocument(studies.getClassroomId(), annoucment);

        return GenericResponse.created(annoucment);
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public void postResults(Integer studiesId, Integer examId) {
        Studies studies = validateStudies(studiesId);
        Exam exam = getExam(examId, studies);

        if(exam.getShowResults()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("exam_already_published"));
        }

        if(exam.getEndAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("exam_results_not_ready"));
        }

        exam.setShowResults(true);
        examRepository.save(exam);

        if(studies.getClassroomId() != null && exam.getCourseWorkId() != null) {
            List<ExamGroup> examGroups = examGroupRepository.findByExam(exam);

            examGroups.forEach((item) -> {
                if(item.getSubmissionId() != null) {
                    Integer points =  item.getPoints() == null ? 0 : item.getPoints();

                    StudentSubmissions studentSubmissions = StudentSubmissions.builder().assignedGrade(points).draftGrade(points).state("TURNED_IN").build();

                    googleService.publishResults(studies.getClassroomId(), exam.getCourseWorkId(), item.getSubmissionId(), studentSubmissions);
                }
            });
        }
    }
}
