package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.ExamGroup;
import com.wojcka.exammanager.models.ExamGroupQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamGroupQuestionRepository extends JpaRepository<ExamGroupQuestion, Long> {
    List<ExamGroupQuestion> findAllByExamGroup(ExamGroup examGroup, Pageable pageable);
}
