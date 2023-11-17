package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Override
    Optional<User> findById(UUID uuid);

    Optional<User> findByEmail(String email);

    @Query(value = "select u.* from _users u join _user_groups ug on u.id = ug.user_id join _groups g on ug.group_id = g.id where g.key = :role", nativeQuery = true)

    Page<User> getUsersByRole(@Param("role") String role, Pageable pageable);
}