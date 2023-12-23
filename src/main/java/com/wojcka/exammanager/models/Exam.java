package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
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
@Entity(name = "_exams")
public class Exam {
    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @OneToOne(
            cascade = CascadeType.DETACH
    )
    private QuestionMetadata questionMetadata;

    @ManyToOne(
            cascade = CascadeType.DETACH
    )
    private Studies studies;

    @JsonIgnore
    @OneToMany(
            targetEntity = ExamGroup.class,
            mappedBy = "exam",
            cascade = CascadeType.ALL
    )
    private List<ExamGroup> examGroupList;

    private String courseWorkId;

    @NotEmpty
    private String name;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    private Integer questionPerUser;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean showResults = false;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer points = null;

    @Column(nullable = false, columnDefinition="tinyint(1) default 0")
    private Boolean archived = false;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
