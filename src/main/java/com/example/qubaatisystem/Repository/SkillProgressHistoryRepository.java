package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.SkillProgressHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillProgressHistoryRepository extends JpaRepository<SkillProgressHistory, Integer> {

    SkillProgressHistory findSkillProgressHistoryById(Integer id);

    List<SkillProgressHistory> findSkillProgressHistoriesByStudentId(Integer studentId);
}
