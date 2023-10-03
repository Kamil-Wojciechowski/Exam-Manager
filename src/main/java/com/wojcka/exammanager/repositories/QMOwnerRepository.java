package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.QuestionMetadataOwnership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QMOwnerRepository extends JpaRepository<QuestionMetadataOwnership, Integer> {
    @Query(value = "select * from _questions_metadata_ownership qmo where qmo.question_metadata_id = :questionMetadataId and qmo.user_id = :userId", nativeQuery = true)
    Optional<QuestionMetadataOwnership> findByUserAndQM(@Param("questionMetadataId") Integer questionMetadata, @Param("userId") UUID user);

    @Query(value = "SELECT * from _questions_metadata_ownership qmo where qmo.question_metadata_id = :questionMetadataId", nativeQuery = true)
    Page<QuestionMetadataOwnership> findByQuestionMetadata(@Param("questionMetadataId") Integer questionMetadata, Pageable pageable);

    @Query(value = "select * from _questions_metadata_ownership qmo where qmo.question_metadata_id = :questionMetadataId and qmo.id = :qmoId", nativeQuery = true)
    Optional<QuestionMetadataOwnership> findByMetadataIdAndId(@Param("questionMetadataId") Integer questionMetadata, @Param("qmoId") Integer id);
}
