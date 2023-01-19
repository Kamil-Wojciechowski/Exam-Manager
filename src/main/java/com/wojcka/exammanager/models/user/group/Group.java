package com.wojcka.exammanager.models.user.group;

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
@Entity(name = "_groups")
public class Group {
    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String description;

    @OneToMany
    @JoinTable(
            name = "_group_roles",
            joinColumns = {
                    @JoinColumn(name = "group_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "id")
            }
    )
    private List<GroupRole> groupRole;
}
