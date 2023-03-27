package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.token.user.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}
