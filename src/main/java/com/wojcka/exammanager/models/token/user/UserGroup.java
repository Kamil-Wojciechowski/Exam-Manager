package com.wojcka.exammanager.models.token.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.wojcka.exammanager.models.token.user.group.Group;
import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "user_id", foreignKey=@ForeignKey(name = "FK_user_id"))
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id",  foreignKey=@ForeignKey(name = "FK_group_id"))
    private Group group;
}
