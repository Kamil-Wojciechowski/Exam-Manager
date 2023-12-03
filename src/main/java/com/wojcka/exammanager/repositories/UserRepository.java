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

    @Query(value = """
select u.* from _users u join _user_groups ug on u.id = ug.user_id join _groups g on ug.group_id = g.id where g.key = :role and (
((lower(u.firstname) like :firstname or :firstname is null) and (lower(u.lastname) like :lastname or :lastname is null) and (lower(u.email) like :email or :email is null)))
""", nativeQuery = true)
    Page<User> getByRoleAndParams(@Param("role") String role, @Param("firstname") String firstname, @Param("lastname") String lastname, @Param("email") String email, Pageable pageable);
}