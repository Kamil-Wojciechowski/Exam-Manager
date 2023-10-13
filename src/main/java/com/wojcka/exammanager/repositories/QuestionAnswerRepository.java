package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Question;
import com.wojcka.exammanager.models.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Integer> {

    @Query(value = "select qa.* from _question_answers qa where qa.id = :id and qa.question_id = :questionId", nativeQuery = true)
    Optional<QuestionAnswer> getByIdAndQuestionId(@Param("id") Integer id, @Param("questionId") Integer questionId);
}
