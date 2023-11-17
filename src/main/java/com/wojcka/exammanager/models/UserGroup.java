package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "_user_groups")
public class UserGroup {
    @Id
    @GeneratedValue
    private Integer id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey=@ForeignKey(name = "FK_user_id"), unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id",  foreignKey=@ForeignKey(name = "FK_group_id"))
    private Group group;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
