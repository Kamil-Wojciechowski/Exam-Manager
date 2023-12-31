package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_exam_group_question_answers")
public class ExamGroupQuestionAnswer {
    @Id
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @ManyToOne
    private ExamGroupQuestion examGroupQuestion;


    @ManyToOne
    private QuestionAnswer questionAnswer;

    @Column(columnDefinition = "TEXT")
    private String manualAnswer;
}
