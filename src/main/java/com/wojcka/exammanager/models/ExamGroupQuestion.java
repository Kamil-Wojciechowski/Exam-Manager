package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


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

    private String answer;

    private Integer points;

    @Enumerated(EnumType.STRING)
    private CorrectType correct;
}
