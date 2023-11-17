package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.ExamGroupQuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamGroupQuestionAnswerRepository extends JpaRepository<ExamGroupQuestionAnswer, Long> {

}
