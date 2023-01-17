package com.wojcka.exammanager.models.user.group;

import com.wojcka.exammanager.models.user.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_group_roles")
public class GroupRole {
    @Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    private Group groupRoleId;

    @ManyToOne
    private Role roleId;

    private boolean system;
}
