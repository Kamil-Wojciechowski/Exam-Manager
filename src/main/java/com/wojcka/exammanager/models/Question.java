package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@Entity(name = "_questions")
public class Question {
    @Id
    @GeneratedValue
    private Integer id;

    @Valid
    @NotBlank
    @Column(columnDefinition = "TEXT")
    public String question;

    @Valid
    @NotNull
    @Enumerated(EnumType.STRING)
    public QuestionType questionType;

    @Column(nullable = false)
    public Boolean archived = false;

    @Column(columnDefinition = "boolean default false")
    public Boolean valid = false;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne
    public QuestionMetadata questionMetadata;

    @OneToMany(cascade = CascadeType.ALL, targetEntity = QuestionAnswer.class, mappedBy = "question", fetch = FetchType.LAZY)
    private List<QuestionAnswer> answers;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    public Boolean isTypeForSingleAnswer() {
        return questionType.equals(QuestionType.SINGLE_ANSWER) ||
//                questionType.equals(QuestionType.FILE) ||
                questionType.equals(QuestionType.OPEN_ANSWER);
    }
}
