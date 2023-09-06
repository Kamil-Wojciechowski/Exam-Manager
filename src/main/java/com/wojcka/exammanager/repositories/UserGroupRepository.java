package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, Integer> {
}
