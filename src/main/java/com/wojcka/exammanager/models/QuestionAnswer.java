package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String answer;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean correct = false;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
