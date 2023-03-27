package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.token.user.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Integer> {
    public Optional<Group> findByName(String name);
}
