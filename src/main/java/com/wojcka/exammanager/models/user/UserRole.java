package com.wojcka.exammanager.models.user;

import com.wojcka.exammanager.models.user.group.GroupRole;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "_user_roles")
public class UserRole {
    @Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    private User userId;

    @ManyToOne
    private GroupRole groupRole;
}
