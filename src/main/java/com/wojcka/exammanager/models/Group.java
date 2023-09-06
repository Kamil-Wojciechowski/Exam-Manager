package com.wojcka.exammanager.models;

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

    @Column(unique = true)
    private String key;

    private String description;
}
