package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    Student findStudentById(Integer id);

    List<Student> findByParentId(Integer parentId);

    List<Student> findByClassroomId(Integer classroomId);

    int countByClassroomTeacherId(Integer teacherId);
}
