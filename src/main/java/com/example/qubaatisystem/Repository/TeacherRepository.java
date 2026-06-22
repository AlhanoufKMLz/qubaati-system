package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    Teacher findTeacherById(Integer id);

    // Resolve the teacher profile of the authenticated user (entity or null — no Optional).
    Teacher findTeacherByUserId(Integer userId);
}
