package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Integer> {
}
