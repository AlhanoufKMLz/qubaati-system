package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.LearningStyleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningStyleHistoryRepository extends JpaRepository<LearningStyleHistory, Integer> {

    LearningStyleHistory findLearningStyleHistoryById(Integer id);

    List<LearningStyleHistory> findLearningStyleHistoriesByStudentId(Integer studentId);
}
