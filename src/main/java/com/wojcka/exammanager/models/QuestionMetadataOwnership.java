package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Entity(name = "_questions_metadata_ownership")
public class QuestionMetadataOwnership {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @ManyToOne
    @JsonIgnore
    private QuestionMetadata questionMetadata;

    @Enumerated(EnumType.STRING)
    private Ownership ownership;

    @ManyToOne
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
