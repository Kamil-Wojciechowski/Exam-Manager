package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query(value = "SELECT q.* from _questions q where q.question_metadata_id = :questionMetadataId and q.archived = :archived", nativeQuery = true)
    Page<Question> findAllByMetadataId(@Param("questionMetadataId") Integer questionMetadataId, @Param("archived") Boolean archived, Pageable pageable);

    @Query(value = "SELECT q.* from _questions q where q.question_metadata_id = :questionMetadataId and q.id = :id", nativeQuery = true)
    Optional<Question> findByMetadataIdAndId(@Param("questionMetadataId") Integer questionId, @Param("id") Integer id);

}
