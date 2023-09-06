package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Group;
import com.wojcka.exammanager.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    public Optional<Role> findByKey(String key);

    public boolean existsByKey(String name);
}
