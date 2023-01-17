package com.wojcka.exammanager.models.user.group;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

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
}
