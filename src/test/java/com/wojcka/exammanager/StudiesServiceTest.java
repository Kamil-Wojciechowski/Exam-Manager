package com.wojcka.exammanager;

import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.StudiesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.models.User;
import com.wojcka.exammanager.repositories.StudiesRepository;
import com.wojcka.exammanager.repositories.StudiesUserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class StudiesServiceTest {
    @MockBean
    private StudiesRepository studiesRepository;

    @MockBean
    private StudiesUserRepository studiesUserRepository;

    @InjectMocks
    @Autowired
    private StudiesService studiesService;

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

    @Test
    void testGet() {
        Integer page = 0;
        Integer size = 10;

        List<StudiesUser> studiesUser = new ArrayList<>();

        studiesUser.add(StudiesUser.builder().user(authenticatedUser).owner(true).build());

        List<Studies> studiesList = new ArrayList<>();
        Studies studies1 = Studies.builder().id(1).build();
        Studies studies2 = Studies.builder().id(2).build();
        studies1.setStudiesUserList(studiesUser);
        studies2.setStudiesUserList(studiesUser);
        studiesList.add(studies1);
        studiesList.add(studies2);

        Page<Studies> pageable = new org.springframework.data.domain.PageImpl<>(studiesList);

        when(studiesRepository.getByUser(authenticatedUser.getId(), PageRequest.of(page, size))).thenReturn(pageable);
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies1, true)).thenReturn(java.util.Optional.of(
                StudiesUser.builder().user(authenticatedUser).owner(true).studies(studies1).build()
        ));
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, studies2, true)).thenReturn(java.util.Optional.empty());

        GenericResponsePageable response = studiesService.get(page, size);

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(studiesList, response.getData());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertEquals(pageable.hasNext(), response.isHasNext());
        assertEquals(pageable.getTotalPages(), response.getPages());
        assertEquals(pageable.getTotalElements(), response.getTotal());

        verify(studiesRepository).getByUser(authenticatedUser.getId(), PageRequest.of(page, size));
        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testPost() {
        Studies studies = Studies.builder().id(1).build();

        when(studiesRepository.save(studies)).thenReturn(studies);
        when(studiesUserRepository.save(any())).thenReturn(
                StudiesUser.builder().user(authenticatedUser).owner(true).studies(studies).build()
        );

        GenericResponse response = studiesService.post(studies);

        assertEquals(201, response.getCode());
        assertEquals("CREATED", response.getStatus());
        assertEquals(studies, response.getData());

        verify(studiesRepository).save(studies);

        verify(studiesUserRepository).save(
                StudiesUser.builder().user(authenticatedUser).owner(true).studies(studies).build()
        );

        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testGetById_Success() {
        Studies studies = Studies.builder().id(1).build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        studies.setStudiesUserList(studiesUserList);

        when(studiesRepository.findById(1)).thenReturn(Optional.of(studies));
        when(studiesUserRepository.findByStudiesAndOwner(studies, true)).thenReturn(studiesUserList);

        GenericResponse result = studiesService.getById(1);

        assertEquals(GenericResponse.ok(studies), result);

        verify(studiesRepository).findById(1);

        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testGetById_NotFound() {
        when(studiesRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesService.getById(1));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(studiesRepository).findById(1);
        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testUpdate_Success() {
        Studies existingStudies = Studies.builder().id(1).build();

        Studies updatedStudies = Studies.builder().id(1).name("Updated Name").build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        StudiesUser studiesUser = StudiesUser.builder().user(authenticatedUser).owner(true).build();
        studiesUserList.add(studiesUser);
        existingStudies.setStudiesUserList(studiesUserList);

        when(studiesRepository.findById(1)).thenReturn(Optional.of(existingStudies));
        when(studiesUserRepository.findByStudiesAndOwner(existingStudies, true)).thenReturn(studiesUserList);
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, true)).thenReturn(java.util.Optional.of(
                studiesUser
        ));
        studiesService.update(1, updatedStudies);

        verify(studiesRepository).findById(1);
        verify(studiesRepository).save(updatedStudies);
        verifyNoMoreInteractions(studiesRepository);
    }

    @Test
    void testUpdate_NotFound() {
        when(studiesRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesService.update(1, Studies.builder().name("Updated Name").build()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(studiesRepository).findById(1);

        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testUpdate_Forbidden() {
        Studies updatedStudies = Studies.builder().id(1).name("Updated Name").build();

        Studies existingStudies = Studies.builder().id(1).build();
        StudiesUser studiesUser = StudiesUser.builder().user(authenticatedUser).owner(false).build();
        List<StudiesUser> studiesUserList = new ArrayList<>();
        studiesUserList.add(studiesUser);
        existingStudies.setStudiesUserList(studiesUserList);

        when(studiesRepository.findById(1)).thenReturn(Optional.of(existingStudies));
        when(studiesUserRepository.findByStudiesAndOwner(existingStudies, true)).thenReturn(new ArrayList<>());
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, false)).thenReturn(java.util.Optional.of(
                studiesUser
        ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesService.update(1, updatedStudies));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(studiesRepository).findById(1);
        verifyNoMoreInteractions(studiesRepository);
    }

    @Test
    void testDelete_Success() {
        Studies existingStudies = Studies.builder().id(1).build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        StudiesUser studiesUser = StudiesUser.builder().user(authenticatedUser).owner(true).build();
        studiesUserList.add(studiesUser);
        existingStudies.setStudiesUserList(studiesUserList);

        when(studiesRepository.findById(1)).thenReturn(Optional.of(existingStudies));
        when(studiesUserRepository.findByStudiesAndOwner(existingStudies, true)).thenReturn(studiesUserList);
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, true)).thenReturn(java.util.Optional.of(
                studiesUser
        ));

        studiesService.delete(1);

        verify(studiesRepository).findById(1);
        verify(studiesUserRepository).findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, true);
        verify(studiesRepository).delete(existingStudies);
        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testDelete_NotFound() {
        when(studiesRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesService.delete(1));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(studiesRepository).findById(1);
        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }

    @Test
    void testDelete_Forbidden() {
        Studies existingStudies = Studies.builder().id(1).build();

        List<StudiesUser> studiesUserList = new ArrayList<>();
        StudiesUser studiesUser = StudiesUser.builder().user(authenticatedUser).owner(false).build();
        studiesUserList.add(studiesUser);
        existingStudies.setStudiesUserList(studiesUserList);

        when(studiesRepository.findById(1)).thenReturn(Optional.of(existingStudies));
        when(studiesUserRepository.findByStudiesAndOwner(existingStudies, true)).thenReturn(new ArrayList<>());
        when(studiesUserRepository.findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, false)).thenReturn(java.util.Optional.of(
                studiesUser
        ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                studiesService.delete(1));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(studiesRepository).findById(1);
        verify(studiesUserRepository).findByUserAndStudiesAndOwner(authenticatedUser, existingStudies, true);
        verifyNoMoreInteractions(studiesRepository, studiesUserRepository);
    }
}
