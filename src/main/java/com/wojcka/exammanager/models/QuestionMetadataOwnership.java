package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_questions_metadata_ownership")
public class QuestionMetadataOwnership {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @ManyToOne
    @JsonIgnore
    private QuestionMetadata questionMetadata;

    @Valid
    @NotNull(message = "validation_message_not_blank")
    @Enumerated(EnumType.STRING)
    private Ownership ownership;

    @Valid
    @NotNull(message = "validation_message_not_blank")
    @ManyToOne
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    public boolean isEnoughToAccess() {
        return ownership.equals(Ownership.OWNER) || ownership.equals(Ownership.ADDITIONAL_OWNER);
    }

}
