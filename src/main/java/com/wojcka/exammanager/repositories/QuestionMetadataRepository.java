package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.QuestionMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionMetadataRepository extends JpaRepository<QuestionMetadata, Integer> {

    @Query("SELECT qm from _questions_metadata qm order by qm.id DESC")
    Page<QuestionMetadata> findAll(Pageable pageable);

    @Query(value = "SELECT qm.* from _questions_metadata qm join _questions_metadata_ownership qmo on qmo.question_metadata_id = qm.id where qmo.user_id = :userId order by qm.id DESC", nativeQuery = true)
    Page<QuestionMetadata> findAllByUser(@Param("userId") UUID userId, Pageable pageable);
}
