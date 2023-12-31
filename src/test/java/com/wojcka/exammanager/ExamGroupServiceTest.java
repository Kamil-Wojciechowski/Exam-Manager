package com.wojcka.exammanager;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.ExamGroupRepository;
import com.wojcka.exammanager.repositories.ExamRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.ExamGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ExamGroupServiceTest {

    @MockBean
    private ExamGroupRepository examGroupRepository;

    @MockBean
    private ExamRepository examRepository;

    @MockBean
    private StudiesUserRepository studiesUserRepository;

    @InjectMocks
    @Autowired
    private ExamGroupService examGroupService;

    private User authenticatedUser;
    private Studies studies;
    private Exam exam;
    private List<StudiesUser> studiesUserList;
    private List<ExamGroupQuestion> examGroupQuestionList;

    @BeforeEach
    void setUp() {
        authenticatedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstname("John")
                .lastname("Doe")
                .build();

        studies = Studies.builder().id(1).build();
        exam = Exam.builder().id(Long.getLong("1")).studies(studies).build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                AuthorityUtils.createAuthorityList("ROLE_TEACHER")
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        studiesUserList = new ArrayList<>();
        studiesUserList.add(StudiesUser.builder().user(authenticatedUser).owner(true).studies(studies).build());

        examGroupQuestionList = new ArrayList<>();

        examGroupQuestionList.add(
                ExamGroupQuestion.builder()
                        .id(1L)
                        .question(
                                Question.builder()
                                        .id(1)
                                        .build()
                        )
                        .build()
        );
    }

    void setStudent() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                AuthorityUtils.createAuthorityList("ROLE_STUDENT")
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testGet() {
        Integer studiesId = 1;
        Integer examId = 1;
        Integer page = 0;
        Integer size = 10;

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies, true)).thenReturn(Optional.of(studiesUserList.get(0)));

        Page<ExamGroup> examGroupPage = mock(Page.class);
        when(examGroupRepository.findAllByExam(exam, PageRequest.of(page, size))).thenReturn(examGroupPage);

        GenericResponsePageable response = examGroupService.get(studiesId, examId, page, size);

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(0, ((List<ExamGroup>) response.getData()).size());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertFalse(response.isHasNext());
        assertEquals(0, response.getPages());
        assertEquals(0, response.getTotal());

        verify(examRepository).findById(examId);
        verify(studiesUserRepository).findByUserAndStudiesAndOwner(authenticatedUser, studies, true);
        verify(examGroupRepository).findAllByExam(exam, PageRequest.of(page, size));
    }

    @Test
    void testGetDetailsExamGroup() {
        Integer studiesId = 1;
        Integer examId = 1;
        Long examGroupId = 1L;

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies, true)).thenReturn(Optional.of(studiesUserList.get(0)));

        ExamGroup examGroup = ExamGroup.builder().id(examGroupId).exam(exam).build();
        examGroup.setExamGroupQuestionList(examGroupQuestionList);

        when(examGroupRepository.findByIdAndExam(examGroupId.intValue(), exam)).thenReturn(Optional.of(examGroup));

        GenericResponse response = examGroupService.getDetailsExamGroup(studiesId, examId, examGroupId.intValue());

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());

        verify(examRepository).findById(examId);
        verify(studiesUserRepository).findByUserAndStudiesAndOwner(authenticatedUser, studies, true);
        verify(examGroupRepository).findByIdAndExam(examGroupId.intValue(), exam);
    }

    @Test
    void testGetDetailsExamGroup_NotFound() {
        Integer studiesId = 1;
        Integer examId = 1;
        Integer examGroupId = 1;

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies, true)).thenReturn(Optional.of(studiesUserList.get(0)));
        when(examGroupRepository.findByIdAndExam(examGroupId, exam)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                examGroupService.getDetailsExamGroup(studiesId, examId, examGroupId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(examRepository).findById(examId);
        verify(studiesUserRepository).findByUserAndStudiesAndOwner(authenticatedUser, studies, true);
        verify(examGroupRepository).findByIdAndExam(examGroupId, exam);
    }

    @Test
    void testGetDetailsExamGroup_Forbidden() {
        Integer studiesId = 1;
        Integer examId = 1;
        Integer examGroupId = 1;

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies, true)).thenReturn(Optional.empty());

        setStudent();

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                examGroupService.getDetailsExamGroup(studiesId, examId, examGroupId));

        assertEquals("Access is denied", exception.getMessage());

        verifyNoMoreInteractions(examGroupRepository);
    }
}
