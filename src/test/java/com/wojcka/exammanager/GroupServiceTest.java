package com.wojcka.exammanager;

import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @MockBean
    private GroupRepository groupRepository;

    @InjectMocks
    @Autowired
    private GroupService groupService;

    @BeforeEach
    void setAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                null,
                null,
                AuthorityUtils.createAuthorityList("ROLE_TEACHER")
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setStudent() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                null,
                null,
                AuthorityUtils.createAuthorityList("ROLE_STUDENT")
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testGetGroups() {
        List<Group> groupList = new ArrayList<>();
        groupList.add(Group.builder().id(1).name("Group1").key("GROUP_1").description("Group 1 description").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        groupList.add(Group.builder().id(2).name("Group2").key("GROUP_2").description("Group 2 description").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        when(groupRepository.findAll()).thenReturn(groupList);

        GenericResponse response = groupService.getGroups();

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(groupList, response.getData());
    }

    @Test
    void testGetGroups_Forbidden() {
        setStudent();

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                        groupService.getGroups()
        );

        assertEquals("Access is denied", exception.getMessage());
    }
}
