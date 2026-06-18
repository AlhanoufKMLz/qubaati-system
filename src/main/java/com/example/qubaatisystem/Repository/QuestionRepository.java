package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    Question findQuestionById(Integer id);

    List<Question> findQuestionsByActivityId(Integer activityId);
}
