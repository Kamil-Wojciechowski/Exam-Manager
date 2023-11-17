package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Token;
import com.wojcka.exammanager.models.TokenType;
import com.wojcka.exammanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    Optional<Token> findByHashedToken(String hashedToken);

    void deleteByTokenTypeAndUser(TokenType tokenType, User user);
}
