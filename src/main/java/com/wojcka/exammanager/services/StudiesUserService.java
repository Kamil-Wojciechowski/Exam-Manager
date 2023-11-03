package com.wojcka.exammanager.services;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.components.email.EmailService;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.StudiesRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserRepository;
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
import java.util.Arrays;
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
    private GoogleService googleService;

    @Autowired
    private EmailService emailService;

    @Value("${spring.jpa.auth.expiration.activation}")
    private Long activationExpiration;

    @Value("spring.secret")
    private String secretKey;

    private StrongTextEncryptor textEncryptor;

    private User getUserFromAuth() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateUser(Boolean action) {
        StudiesUser studiesUser = studiesUserRepository.findByUser(getUserFromAuth()).orElseThrow(() -> {
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
        validateUser(false);

        Studies studies = getStudies(studiesId);

        Page<StudiesUser> result = studiesUserRepository.findByStudies(studies, PageRequest.of(page,size));

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

    @PreAuthorize("hasRole('TEACHER')")
    public GenericResponse post(Integer studiesId, StudiesUser studiesUser) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        studiesUser.setStudies(studies);

        studiesUser = studiesUserRepository.save(studiesUser);

        return GenericResponse.created(studiesUser);
    }

    private StudiesUser getStudiesUsserByStudiesAndId(Studies studies, Integer studiesUserId) {
        return studiesUserRepository.findByStudiesAndId(studies, studiesUserId).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Translator.toLocale("item_not_found"));
        });
    }

    public GenericResponse get(Integer studiesId, Integer studiesUserId) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        return GenericResponse.ok(studiesUser);
    }


    @PreAuthorize("hasRole('TEACHER')")
    public void delete(Integer studiesId, Integer studiesUserId) {
        validateUser(true);

        Studies studies = getStudies(studiesId);

        StudiesUser studiesUser = getStudiesUsserByStudiesAndId(studies, studiesUserId);

        studiesUserRepository.delete(studiesUser);
    }

    private void setTextEncryptor() {
        textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword(secretKey);
    }

    public GenericResponse importUsersCsv(Integer studiesId, MultipartFile file) {
        validateUser(true);

        Studies studies = getStudies(studiesId);
        setTextEncryptor();
        List<StudiesUser> studiesUserList = new ArrayList<>();

        if(file.getContentType().equals("text/csv")) {

            try {
                InputStreamReader reader = new InputStreamReader(file.getInputStream());

                CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);


                for (CSVRecord csvRecord : csvParser) {
                    if (csvRecord.size() >= csvParser.getHeaderMap().size()) {

                        studiesUserList.add(processSingleEmail(User.builder()
                                .email(csvRecord.get("email"))
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

    @Transactional
    public StudiesUser processSingleEmail(User googleUser, Studies studies) {
        User user = userRepository.findByEmail(googleUser.getEmail()).orElse(googleUser);

        if(user.getId() == null) {
            user = userRepository.save(user);

            String secretUUID = UUID.randomUUID().toString();

            tokenRepository.save(Token.builder()
                    .tokenType(TokenType.ACTIVATION)
                    .user(user)
                    .hashedToken(secretUUID)
                    .createdAt(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(activationExpiration))
                    .build());

            emailService.sendEmail(user.getEmail(), "Activation", URLEncoder.encode(textEncryptor.encrypt(secretUUID), Charset.defaultCharset()));
        }

        StudiesUser studiesUser = studiesUserRepository.findByUser(user).orElse(StudiesUser.builder()
                .studies(studies)
                .user(user)
                .owner(false)
                .build());

        return studiesUserRepository.save(studiesUser);
    }
    public GenericResponse importUsersGoogle(Integer studiesId) {
        validateUser(true);
        Studies studies = getStudies(studiesId);

        if(studies.getClassroomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Translator.toLocale("classroom_not_provided"));
        }

        List<User> listOfUsers = googleService.getUsersByClassroom(studies.getClassroomId());

        List<StudiesUser> listOfProccessed = new ArrayList<>();

        setTextEncryptor();

        listOfUsers.forEach(item -> {
            listOfProccessed.add(processSingleEmail(item, studies));
        });

        return GenericResponse.created(listOfProccessed);
    }
}
