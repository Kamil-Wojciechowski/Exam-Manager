package com.wojcka.exammanager.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_question_answers")
public class QuestionAnswer {
    @Id
    @GeneratedValue
    private Integer id;

    @OneToOne
    private Question question;

    private String answer;

    private Boolean correct;
}
