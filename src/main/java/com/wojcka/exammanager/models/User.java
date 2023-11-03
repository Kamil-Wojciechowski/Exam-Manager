package com.wojcka.exammanager.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_users")
public class User implements UserDetails
{
    @Id
    @Valid
    @NotNull
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 30)
    private String firstname;

    @Column(length = 30)
    private String lastname;

    @JsonIgnore
    private String password;

    @JsonIgnore
    private String googleAccessToken;

    @JsonIgnore
    private String googleRefreshToken;

    @JsonIgnore
    private LocalDateTime googleExpiration;

    @JsonIgnore
    private String googleUserId;

    @Column(unique = true)
    private String email;

    private boolean expired;
    private boolean locked;
    private boolean enabled;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                '}';
    }

    @JsonIgnore
    @OneToMany(
            targetEntity = UserGroup.class,
            mappedBy = "user"
    )
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private List<UserGroup> userRoleGroups;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> result = new ArrayList<>();

        for (UserGroup roleGroup : userRoleGroups) {
            String name = roleGroup.getGroup().getKey();
            result.add(new SimpleGrantedAuthority(name));
        }

        return result;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return this.email;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return !this.expired;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return !this.locked;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @JsonIgnore
    public boolean isGoogleExpired() {
        return LocalDateTime.now().minusSeconds(10).isAfter(this.googleExpiration);
    }
}
