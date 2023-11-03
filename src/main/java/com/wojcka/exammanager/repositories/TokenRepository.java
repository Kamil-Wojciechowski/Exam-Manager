package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Token;
import com.wojcka.exammanager.models.TokenType;
import com.wojcka.exammanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    public Optional<Token> findByHashedToken(String hashedToken);

    public boolean deleteByTokenTypeAndUser(TokenType tokenType, User user);
}
