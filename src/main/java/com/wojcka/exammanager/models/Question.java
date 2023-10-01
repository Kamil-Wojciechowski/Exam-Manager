package com.wojcka.exammanager.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    public String question;

    @Enumerated(EnumType.STRING)
    public QuestionType questionType;

    public Long points;

    public Boolean archived;

    @OneToOne
    public QuestionMetadata questionMetadata;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
