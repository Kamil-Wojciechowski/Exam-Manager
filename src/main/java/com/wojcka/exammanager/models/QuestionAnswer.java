package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @JsonIgnore
    @ManyToOne
    private Question question;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String answer;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean correct = false;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
