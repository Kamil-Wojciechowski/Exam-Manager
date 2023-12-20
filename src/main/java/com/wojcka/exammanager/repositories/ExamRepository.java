package com.wojcka.exammanager.repositories;

import com.wojcka.exammanager.models.Exam;
import com.wojcka.exammanager.models.Studies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamRepository  extends JpaRepository<Exam, Integer> {

    @Query
    Page<Exam> findAllByStudies(Studies studise, Pageable pageable);

    @Query
    List<Exam> findAllByStudies(Studies studies);

    @Query
    Optional<Exam> findByIdAndStudies(Integer id, Studies studies);

}
