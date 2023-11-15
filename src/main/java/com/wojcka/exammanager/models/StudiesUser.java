package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_studies_user")
public class StudiesUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Valid
    @ManyToOne
    @NotNull
    private User user;

    @Column(columnDefinition = "boolean default false")
    private Boolean owner = false;

    @JsonIgnore
    @ManyToOne
    private Studies studies;

    @JsonIgnore
    @OneToMany(
            targetEntity = ExamGroup.class,
            mappedBy = "studiesUser",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ExamGroup> examGroupList;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
