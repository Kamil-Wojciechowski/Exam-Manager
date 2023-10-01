package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.QuestionMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionMetadataRepository extends JpaRepository<QuestionMetadata, Integer> {

    @Query("SELECT qm from _questions_metadata qm")
    Page<QuestionMetadata> findAll(Pageable pageable);
}
