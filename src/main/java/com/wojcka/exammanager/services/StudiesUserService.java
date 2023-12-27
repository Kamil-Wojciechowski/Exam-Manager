package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.components.email.EmailBodyBuilder;
import com.wojcka.exammanager.components.email.EmailBodyType;
import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.*;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.internal.GoogleService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class StudiesUserService {

    @Autowired
    private StudiesUserRepository studiesUserRepository;

    @Autowired
    private StudiesRepository studiesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamGroupRepository examGroupRepository;

    @Autowired
    private ExamGroupQuestionRepository examGroupQuestionRepository;

    @Autowired
    private GoogleService googleService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Value("${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;

    @Value("spring.secret")
    private String secretKey;

    @Value("${spring.mail.frontendUrl.activation}")
    private String activationUrl;

    private StrongTextEncryptor textEncryptor;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateUser(Studies studies, Boolean action) {
        StudiesUser studiesUser = studiesUserRepository.findByUserAndStudies(getUserFromAuth(), studies).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        });

        if (action && !studiesUser.getOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Translator.toLocale("item_forbidden"));
        }
    }

    private Studies getStudies(Integer studiesId) {
        return studiesRepository.findById(studiesId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }
    public GenericResponsePageable get(Integer studiesId, Integer page, Integer size) {
        Studies studies = getStudies(studiesId);

        validateUser(studies, false);

        Page<StudiesUser> result = studiesUserRepository.findByStudiesOrderByIdAsc(studies, PageRequest.of(page,size));

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

    private void checkExam(StudiesUser studiesUser, Studies studies) {
        List<Exam> examList = examRepository.findAllByStudies(studies);

        examList.forEach(exam -> {
            if(exam.getStartAt().isAfter(LocalDateTime.now()) && !examGroupRepository.existsExamGroupByExamAndStudiesUser(exam, studiesUser)) {
                List<Question> questionListPerUser = questionRepository.findAllByQuestionMetadataAndRandomWhereValid(exam.getQuestionMetadata().getId(), exam.getQuestionPerUser());

                ExamGroup examGroup = examGroupRepository.save(
                        ExamGroup.builder()
                                .studiesUser(studiesUser)
                                .exam(exam)
                                .sent(false)
                                .build()
                );

                List<ExamGroupQuestion> examGroupQuestionList = new ArrayList<>();

                questionListPerUser.forEach(question -> {
                    examGroupQuestionList.add(
                            ExamGroupQuestion.builder()
                                    .examGroup(examGroup)
                                    .question(question)
                                    .build()
                    );
                });

                examGroupQuestionRepository.saveAll(examGroupQuestionList);
            }
        });
    }

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse post(Integer studiesId, StudiesUser studiesUser) {
        Studies studies = getStudies(studiesId);

        validateUser(studies, true);

        studiesUser.setStudies(studies);

        studiesUser = studiesUserRepository.save(studiesUser);

        checkExam(studiesUser, studies);

        return GenericResponse.created(studiesUser);
    }

    private StudiesUser getStudiesUsserByStudiesAndId(Studies studies, Integer studiesUserId) {
        return studiesUserRepository.findByStudiesAndId(studies, studiesUserId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    public GenericResponse get(Integer studiesId, Integer studiesUserId) {
        Studies studies = getStudies(studiesId);

        validateUser(studies,true);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        return GenericResponse.ok(studiesUser);
    }


    @PreAuthorize("hasRole('TEACHER')")
    public void delete(Integer studiesId, Integer studiesUserId) {
        Studies studies = getStudies(studiesId);

        validateUser(studies ,true);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        studiesUserRepository.delete(studiesUser);
    }

    private void setTextEncryptor() {
        textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword(secretKey);
    }

    @Transactional
    public GenericResponse importUsersCsv(Integer studiesId, MultipartFile file) {
        Studies studies = getStudies(studiesId);

        validateUser(studies, true);
        setTextEncryptor();
        List<StudiesUser> studiesUserList = new ArrayList<>();

        if(file.getContentType().equals("text/csv")) {

            try {
                InputStreamReader reader = new InputStreamReader(file.getInputStream());

                CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);


                for (CSVRecord csvRecord : csvParser) {
                    if (csvRecord.size() >= csvParser.getHeaderMap().size()) {

                        studiesUserList.add(processSingleEmail(User.builder()
                                .email(csvRecord.get("email").toLowerCase())
                                .firstname(csvRecord.get("firstname"))
                                .lastname(csvRecord.get("lastname"))
                                .expired(false)
                                .locked(false)
                                .enabled(false)
                                .build(), studies));
                    }
                }

            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
            }

        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("file_not_supported"));
        }

        return GenericResponse.created(studiesUserList);
    }

    private StudiesUser processSingleEmail(User googleUser, Studies studies) {
        String googleClassroomId = googleUser.getGoogleUserId();

        User user = userRepository.findByEmail(googleUser.getEmail()).orElse(googleUser);

        if(user.getId() == null) {
            user = userRepository.save(user);

            userGroupRepository.save((
                    UserGroup.builder()
                            .user(user)
                            .group(groupRepository.findByKey("ROLE_STUDENT").orElseThrow(() -> {
                                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
                            }))
                            .build()
            ));

            String secretUUID = UUID.randomUUID().toString();

            tokenRepository.save(Token.builder()
                    .tokenType(TokenType.ACTIVATION)
                    .user(user)
                    .hashedToken(secretUUID)
                    .createdAt(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(activationExpiration))
                    .build());

            HashMap<String, String> items = new HashMap<>();
            items.put("{url}", activationUrl.replace("{token}", URLEncoder.encode(textEncryptor.encrypt(secretUUID), Charset.defaultCharset())));

            String emailBody = EmailBodyBuilder.buildEmail(EmailBodyType.ACTIVATION, items);

            emailService.sendEmail(user.getEmail(), "Activation", emailBody);
        } else if (user.getId() != null && user.getGoogleUserId() == null && googleClassroomId != null) {
            user.setGoogleUserId(googleClassroomId);

            userRepository.save(user);
        }

        StudiesUser studiesUser = studiesUserRepository.findByUserAndStudies(user, studies).orElse(StudiesUser.builder()
                .studies(studies)
                .user(user)
                .owner(false)
                .build());

        if(studiesUser.getId() == null) {
            studiesUser = studiesUserRepository.save(studiesUser);

            checkExam(studiesUser, studies);
        }

        return studiesUser;
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse importUsersGoogle(Integer studiesId) {
        Studies studies = getStudies(studiesId);
        validateUser(studies, true);

        if(studies.getClassroomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("classroom_not_provided"));
        }

        List<User> listOfUsers = googleService.getUsersByClassroom(studies.getClassroomId());

        List<StudiesUser> listOfProccessed = new ArrayList<>();

        setTextEncryptor();

        if(!listOfUsers.isEmpty()) {
            listOfUsers.forEach(item -> {
                listOfProccessed.add(processSingleEmail(item, studies));
            });
        }


        return GenericResponse.created(listOfProccessed);
    }
}
