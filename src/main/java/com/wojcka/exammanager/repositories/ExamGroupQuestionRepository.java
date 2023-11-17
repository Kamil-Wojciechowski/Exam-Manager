package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.ExamGroup;
import com.wojcka.exammanager.models.ExamGroupQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamGroupQuestionRepository extends JpaRepository<ExamGroupQuestion, Long> {
    Page<ExamGroupQuestion> findAllByExamGroup(ExamGroup examGroup, Pageable pageable);

    Optional<ExamGroupQuestion> findExamGroupQuestionByExamGroupAndId(ExamGroup examGroup, Long id);

    @Query("select sum(egq.points) from _exam_group_questions egq where egq.examGroup = ?1")
    Integer sumPointsByExamGroup(ExamGroup examGroup);
}
