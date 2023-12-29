package com.wojcka.exammanager;

import com.wojcka.exammanager.components.Translator;
import com.wojcka.exammanager.models.*;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.repositories.TokenRepository;
import com.wojcka.exammanager.repositories.UserGroupRepository;
import com.wojcka.exammanager.repositories.UserRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.schemas.responses.GenericResponsePageable;
import com.wojcka.exammanager.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@WithMockUser(username = "user", roles = "TEACHER")
@ExtendWith(SpringExtension.class)
public class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserGroupRepository userGroupRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private TokenRepository tokenRepository;


    @Autowired
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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
    void testGetUsers() {
        String role = "TEACHER";
        String firstname = "Test";
        String lastname = null;
        String email = null;
        Integer page = 0;
        Integer size = 10;

        List<User> userList = new ArrayList<>();
        userList.add(User.builder().firstname("Test1").lastname("Test1").email("some@test1.com").id(UUID.randomUUID()).build());
        userList.add(User.builder().firstname("Test2").lastname("Test2").email("some@test2.com").id(UUID.randomUUID()).build());

        Page<User> userPage = new PageImpl<>(userList);

        when(userRepository.getByRoleAndParams("ROLE_TEACHER", "%test%", null, null, PageRequest.of(0, 10)))
                .thenReturn(userPage);

        GenericResponsePageable response = userService.getUsers(role, firstname, lastname, email, page, size);

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(userList, response.getData());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertEquals(userPage.hasNext(), response.isHasNext());
        assertEquals(userPage.getTotalPages(), response.getPages());
        assertEquals(userPage.getTotalElements(), response.getTotal());

        verify(userRepository).getByRoleAndParams("ROLE_TEACHER", "%test%", null, null, PageRequest.of(0, 10));
    }


    @Test
    void testAddUser() {
        User request = new User();
        request.setEmail("test@example.com");
        request.setFirstname("John");
        request.setLastname("Doe");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(groupRepository.findByKey("ROLE_TEACHER")).thenReturn(Optional.of(new Group()));
        when(userRepository.save(any())).thenReturn(new User());
        when(userGroupRepository.save(any())).thenReturn(new UserGroup());
        when(tokenRepository.save(any())).thenReturn(new Token());

        GenericResponse response = userService.addUser(request, true);

        assertEquals(HttpStatus.CREATED.name(), response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    void testChangeRole_UserTryingToEditSelf() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(authenticatedUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.changeRole(userId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(Translator.toLocale("user_not_editable"), exception.getReason());
        verify(userGroupRepository, never()).save(any());
    }

    @Test
    void testChangeRole_Successful() {
        UUID userIdToEdit = UUID.randomUUID();

        List<UserGroup> userGroupList = new ArrayList<>();
        userGroupList.add(UserGroup.builder()
                .group(
                        Group.builder()
                        .key("TEACHER")
                                .build())
                .build());

        User userToEdit = User.builder()
                .id(userIdToEdit)
                .email("edit@example.com")
                .firstname("Edit")
                .lastname("User")
                .userRoleGroups(userGroupList)
                .build();


        when(userRepository.findById(userIdToEdit)).thenReturn(Optional.of(userToEdit));
        when(groupRepository.findByKey("ROLE_TEACHER")).thenReturn(Optional.of(new Group()));
        when(userGroupRepository.save(any())).thenReturn(new UserGroup());

        userService.changeRole(userIdToEdit);

        verify(userGroupRepository).save(any());
    }

    @Test
    void testDeactivateUser_Successful() {
        User userToDeactivate = mock(User.class);
        when(userRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.ofNullable(userToDeactivate));

        userService.deactivateUser(UUID.randomUUID());

        verify(userToDeactivate, times(1)).setLocked(anyBoolean());
        verify(userRepository, times(1)).save(userToDeactivate);
    }
}
