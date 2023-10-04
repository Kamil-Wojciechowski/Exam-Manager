package com.wojcka.exammanager.models;

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
    public String question;

    @Valid
    @NotNull
    @Enumerated(EnumType.STRING)
    public QuestionType questionType;

    @Valid
    @NotNull
    public Long points;

    @Column(nullable = false)
    public Boolean archived = false;

    @OneToOne
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public QuestionMetadata questionMetadata;

    @OneToMany(targetEntity = QuestionAnswer.class,
            mappedBy = "question",
            cascade = CascadeType.ALL)
    private List<QuestionAnswer> answers;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
