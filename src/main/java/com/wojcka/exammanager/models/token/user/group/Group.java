package com.wojcka.exammanager.models.token.user.group;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_groups")
public class Group {
    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String description;

    @OneToMany(
            targetEntity = GroupRole.class,
            mappedBy = "group",
            fetch = FetchType.LAZY
    )
    private List<GroupRole> groupRole;
}
