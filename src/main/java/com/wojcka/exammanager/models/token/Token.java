package com.wojcka.exammanager.models.token;

import com.wojcka.exammanager.models.token.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "_tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hashedToken;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime expirationDate;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
