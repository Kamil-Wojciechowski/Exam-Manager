package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Studies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudiesRepository extends JpaRepository<Studies, Integer> {

    @Query(value = "select s.* from _studies s inner join _studies_user su on s.id = su.studies_id where su.user_id = :userId order by s.id DESC", nativeQuery = true)
    Page<Studies> getByUser(UUID userId, Pageable pageable);

}

