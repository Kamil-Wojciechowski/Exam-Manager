package com.wojcka.exammanager.models;

import jakarta.persistence.*;
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

    @Column(unique = true)
    private String key;

    private String description;
}
