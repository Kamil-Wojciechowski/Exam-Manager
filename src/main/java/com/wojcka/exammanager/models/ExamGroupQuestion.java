package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_exam_group_questions")
public class ExamGroupQuestion {
    @Id
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @ManyToOne(
            cascade = CascadeType.DETACH
    )
    private ExamGroup examGroup;

    @ManyToOne(
            cascade = CascadeType.DETACH
    )
    private Question question;

    @OneToMany(
            mappedBy = "examGroupQuestion"
    )
    private List<ExamGroupQuestionAnswer> answer;

    private Integer points;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean changedManually;

    @Enumerated(EnumType.STRING)
    private CorrectType correct;
}
