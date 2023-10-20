package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudiesUserRepository extends JpaRepository<StudiesUser, Long> {
    @Query
    Optional<StudiesUser> findByUser(User user);
}
