package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.LearningStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningStyleRepository extends JpaRepository<LearningStyle, Integer> {

    LearningStyle findLearningStyleById(Integer id);

    LearningStyle findLearningStyleByStudentId(Integer studentId);
}
