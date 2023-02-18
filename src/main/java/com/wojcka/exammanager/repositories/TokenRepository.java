package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.token.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    public Optional<Token> findByHashedToken(String hashedToken);
}
