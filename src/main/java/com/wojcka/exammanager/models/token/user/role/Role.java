package com.wojcka.exammanager.models.token.user.role;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_roles")
public class Role {
    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String key;

    private String description;
}
