package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Exam;
import com.wojcka.exammanager.models.ExamGroup;
import com.wojcka.exammanager.models.StudiesUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamGroupRepository extends JpaRepository<ExamGroup, Long> {

    Page<ExamGroup> findAllByExam(Exam exam, Pageable pageable);
    Boolean existsExamGroupByExamAndStudiesUser(Exam exam, StudiesUser studiesUser);
    Optional<ExamGroup> findExamGroupByExamAndStudiesUser(Exam exam, StudiesUser studiesUser);

    Optional<ExamGroup> findByIdAndExam(Integer id, Exam exam);

    List<ExamGroup> findByExam(Exam exam);
}
