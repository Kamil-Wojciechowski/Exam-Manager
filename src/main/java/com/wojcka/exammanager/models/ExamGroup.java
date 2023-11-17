package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_exam_groups")
public class ExamGroup {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(
            cascade = CascadeType.DETACH
    )
    private StudiesUser studiesUser;

    @JsonIgnore
    @ManyToOne(
            cascade = CascadeType.DETACH
    )
    private Exam exam;

    private String submissionId;

    @OneToMany(
            targetEntity = ExamGroupQuestion.class,
            mappedBy = "examGroup",
            orphanRemoval = true)
    private List<ExamGroupQuestion> examGroupQuestionList;

    private Boolean sent;

    private Integer points;
}