package com.wojcka.exammanager.models.user.group;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.wojcka.exammanager.models.user.role.Role;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_group_roles")
public class GroupRole {
    @Id
    @GeneratedValue
    private Integer id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
