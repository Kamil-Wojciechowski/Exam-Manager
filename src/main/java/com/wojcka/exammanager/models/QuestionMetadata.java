package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_questions_metadata")
public class QuestionMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @Valid
    @NotBlank(message = "validation_message_not_blank")
    public String name;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(
            targetEntity = QuestionMetadataOwnership.class,
            mappedBy = "questionMetadata",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<QuestionMetadataOwnership> questionMetadataOwnership;

    @JsonIgnore
    @OneToMany( targetEntity = Question.class,
            mappedBy = "questionMetadata",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    public List<Question> question;

    @JsonIgnore
    public boolean isIdEmpty() {
        return id.equals(null);
    }


}
