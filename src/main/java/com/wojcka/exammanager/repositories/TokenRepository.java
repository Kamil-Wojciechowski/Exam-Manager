package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    public Optional<Token> findByHashedToken(String hashedToken);
}
