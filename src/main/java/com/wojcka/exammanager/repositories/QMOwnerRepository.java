package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.QuestionMetadataOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QMOwnerRepository extends JpaRepository<QuestionMetadataOwnership, Integer> {
    @Query(value = "select * from _questions_metadata_ownership qmo where qmo.question_metadata_id = :questionMetadataId and qmo.user_id = :userId", nativeQuery = true)
    Optional<QuestionMetadataOwnership> findByUserAndQM(@Param("questionMetadataId") Long questionMetadata, @Param("userId") UUID user);
}
