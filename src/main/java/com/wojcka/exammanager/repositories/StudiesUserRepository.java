package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Studies;
import com.wojcka.exammanager.models.StudiesUser;
import com.wojcka.exammanager.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudiesUserRepository extends JpaRepository<StudiesUser, Long> {
    @Query
    Optional<StudiesUser> findByUser(User user);

    @Query
    Optional<StudiesUser> findByUserAndStudiesAndOwner(User user, Studies studies, Boolean owner);

    @Query
    Page<StudiesUser> findByStudies(Studies studies, Pageable pageable);

    @Query
    Optional<StudiesUser> findByStudiesAndId(Studies studies, Integer id);
}
