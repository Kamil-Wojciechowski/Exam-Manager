package com.wojcka.exammanager;

import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.repositories.GroupRepository;
import com.wojcka.exammanager.schemas.responses.GenericResponse;
import com.wojcka.exammanager.services.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {
    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void testGetRoles() {
        List<Group> groupList = new ArrayList<>();
        groupList.add(Group.builder().id(1).name("Role1").key("ROLE_1").description("Role 1 description").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        groupList.add(Group.builder().id(2).name("Role2").key("ROLE_2").description("Role 2 description").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        when(groupRepository.findAll()).thenReturn(groupList);

        GenericResponse response = roleService.getRoles();

        assertEquals(200, response.getCode());
        assertEquals("OK", response.getStatus());
        assertEquals(groupList, response.getData());

        verify(groupRepository).findAll();
        verifyNoMoreInteractions(groupRepository);
    }
}
