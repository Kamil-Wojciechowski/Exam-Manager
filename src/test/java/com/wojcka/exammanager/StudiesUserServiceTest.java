package com.wojcka.exammanager;

import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.StudiesRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.services.StudiesUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class StudiesUserServiceTest {
    @MockBean
    private StudiesUserRepository studiesUserRepository;

    @MockBean
    private StudiesRepository studiesRepository;

    @Autowired
    @InjectMocks
    private StudiesUserService studiesUserService;

    User authenticatedUser;

    @BeforeEach
    void setUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstname("John")
                .lastname("Doe")
                .build();

        authenticatedUser = user;

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                AuthorityUtils.createAuthorityList("ROLE_TEACHER")
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setStudent() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                AuthorityUtils.createAuthorityList("ROLE_STUDENT")
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testGet_Forbidden() {
        Long studiesUserId = 1L;
        Integer studiesId = 1;
        Integer page = 0;
        Integer size = 10;

        Studies studies = Studies.builder().id(studiesId).build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        studiesUserList.add(StudiesUser.builder().id(studiesUserId).build());
        studiesUserList.add(StudiesUser.builder().id(2L).build());

        Page<StudiesUser> studiesUserPage = new org.springframework.data.domain.PageImpl<>(studiesUserList);

        when(studiesRepository.findById(studiesId)).thenReturn(java.util.Optional.of(studies));
        when(studiesUserRepository.findByStudiesOrderByIdAsc(studies, PageRequest.of(page, size))).thenReturn(studiesUserPage);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesUserService.get(studies.getId(), 1));

        verify(studiesUserRepository, times(1)).findByUserAndStudies(authenticatedUser, studies);
        verifyNoMoreInteractions(studiesUserRepository);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void testGet_Success() {
        Long studiesUserId = 1L;
        Integer studiesId = 1;
        Integer page = 0;
        Integer size = 10;

        Studies studies = Studies.builder().id(studiesId).build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        studiesUserList.add(StudiesUser.builder().id(studiesUserId).build());
        studiesUserList.add(StudiesUser.builder().id(2L).build());

        Page<StudiesUser> studiesUserPage = new org.springframework.data.domain.PageImpl<>(studiesUserList);

        when(studiesRepository.findById(studiesId)).thenReturn(java.util.Optional.of(studies));
        when(studiesUserRepository.findByStudiesOrderByIdAsc(studies, PageRequest.of(page, size))).thenReturn(studiesUserPage);
        when(studiesUserRepository.findByUserAndStudies(authenticatedUser, studies)).thenReturn(
                Optional.ofNullable(StudiesUser.builder().user(authenticatedUser).owner(false).build())
        );
        GenericResponsePageable response = studiesUserService.get(studiesId, page, size);

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(studiesUserList, response.getData());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertEquals(studiesUserPage.hasNext(), response.isHasNext());
        assertEquals(studiesUserPage.getTotalPages(), response.getPages());
        assertEquals(studiesUserPage.getTotalElements(), response.getTotal());

        verify(studiesRepository).findById(studiesId);
        verify(studiesUserRepository).findByStudiesOrderByIdAsc(studies, PageRequest.of(page, size));
    }

    @Test
    void testPost_Success() {
        Integer studiesId = 1;
        Studies studies = Studies.builder().id(studiesId).build();
        StudiesUser studiesUser = StudiesUser.builder().build();

        when(studiesRepository.findById(studiesId)).thenReturn(java.util.Optional.of(studies));
        when(studiesUserRepository.save(studiesUser)).thenReturn(studiesUser);
        when(studiesUserRepository.findByUserAndStudies(authenticatedUser, studies)).thenReturn(
                Optional.ofNullable(StudiesUser.builder().user(authenticatedUser).owner(true).build())
        );

        studiesUserService.post(studiesId, studiesUser);

        verify(studiesRepository).findById(studiesId);
        verify(studiesUserRepository).save(studiesUser);
    }

    @Test
    void testPost_Forbidden() {
        Integer studiesId = 1;
        Studies studies = Studies.builder().id(studiesId).build();
        StudiesUser studiesUser = StudiesUser.builder().build();

        setStudent();

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                studiesUserService.post(studiesId, studiesUser));

        verifyNoMoreInteractions(studiesUserRepository);
        assertEquals("Access is denied", exception.getMessage());
    }

    @Test
    void testGetSingleItem_Success() {
        Integer studiesId = 1;
        Integer studiesUserId = 1;
        Studies studies = Studies.builder().id(studiesId).build();
        StudiesUser studiesUser = StudiesUser.builder().build();

        when(studiesRepository.findById(studiesId)).thenReturn(java.util.Optional.of(studies));
        when(studiesUserRepository.findByStudiesAndId(studies, studiesUserId)).thenReturn(Optional.of(studiesUser));
        when(studiesUserRepository.findByUserAndStudies(authenticatedUser, studies)).thenReturn(
                Optional.ofNullable(StudiesUser.builder().user(authenticatedUser).owner(true).build())
        );

        studiesUserService.get(studiesId, studiesUserId);
        verify(studiesRepository).findById(studiesId);
        verify(studiesUserRepository).findByStudiesAndId(studies, studiesUserId);
    }

    @Test
    void testGetSingleItem_Failed() {
        Integer studiesId = 1;
        Integer studiesUserId = 1;
        Studies studies = Studies.builder().id(studiesId).build();
        StudiesUser studiesUser = StudiesUser.builder().build();

        when(studiesRepository.findById(studiesId)).thenReturn(java.util.Optional.of(studies));
        when(studiesUserRepository.findByStudiesAndId(studies, studiesUserId)).thenReturn(Optional.of(studiesUser));
        when(studiesUserRepository.findByUserAndStudies(authenticatedUser, studies)).thenReturn(
                Optional.ofNullable(StudiesUser.builder().user(authenticatedUser).owner(false).build())
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesUserService.get(studies.getId(), 1));

        verify(studiesUserRepository, times(1)).findByUserAndStudies(authenticatedUser, studies);
        verifyNoMoreInteractions(studiesUserRepository);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

}
