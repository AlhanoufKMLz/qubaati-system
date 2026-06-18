package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Integer> {

    StudentAnswer findStudentAnswerById(Integer id);

    List<StudentAnswer> findStudentAnswersByActivitySubmissionId(Integer activitySubmissionId);

    StudentAnswer findStudentAnswerByActivitySubmissionIdAndQuestionId(Integer activitySubmissionId, Integer questionId);
}
